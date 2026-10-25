import java.util.jar.JarFile
import java.util.zip.ZipFile
import java.util.zip.CRC32
import java.security.MessageDigest
import javax.imageio.ImageIO

plugins { application }

// Buildship/JDTLS applies Eclipse on import; keep its output separate from Gradle's.
allprojects {
    pluginManager.withPlugin("eclipse") {
        extensions.configure<org.gradle.plugins.ide.eclipse.model.EclipseModel> {
            classpath.defaultOutputDir = layout.buildDirectory.dir("eclipse/default").get().asFile
            classpath.baseSourceOutputDir.set(layout.buildDirectory.dir("eclipse"))
        }
    }
}

group = "org.totipo"
description = "Totipo Swing desktop shell"

// No terminator or one conventional newline; no other whitespace is accepted.
val versionText = providers.fileContents(layout.projectDirectory.file("VERSION")).asText.get()
val applicationVersion = if (versionText.endsWith("\r\n")) versionText.dropLast(2) else versionText.removeSuffix("\n")
check(applicationVersion.isNotEmpty() && applicationVersion.none { it.isWhitespace() }) {
    "VERSION must contain one nonempty value without whitespace (one final newline is allowed)"
}
check(applicationVersion.matches(Regex("[A-Za-z0-9][A-Za-z0-9._+-]*"))) { "Unsafe VERSION filename characters" }
version = applicationVersion.trim()
val releaseBuild = providers.gradleProperty("releaseBuild").map { it.toBooleanStrict() }.orElse(false)
check(!releaseBuild.get() || applicationVersion != "0.0.0-dev") {
    "A release build requires replacing the development VERSION"
}
tasks.register("validateVersion") {
    group = "verification"
    description = "Validate VERSION; -PreleaseBuild=true rejects the development sentinel"
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
application { mainClass.set("org.totipo.desktop.TotipoDesktop") }

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
}

val totipoJavaVersion = "0.2.0"
dependencies {
    implementation("org.totipo:totipo-storage-nio:$totipoJavaVersion")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyLocking {
    lockAllConfigurations()
    lockMode.set(LockMode.STRICT)
}

// Start scripts only; `run` retains normal debugger/Attach API availability.
tasks.startScripts { defaultJvmOpts = listOf("-XX:+DisableAttachMechanism") }
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
distributions.main {
    contents {
        from("LICENSE", "THIRD_PARTY.md", "VERSION")
        from("packaging/licenses") { into("licenses") }
    }
}

val distributionDirectory = layout.buildDirectory.dir("install/totipo-desktop")
val runtimeFiles = configurations.runtimeClasspath
val desktopJar = tasks.jar.flatMap { it.archiveFile }
val buildHostPath = layout.projectDirectory.asFile.absolutePath
val portableZip = tasks.distZip.flatMap { it.archiveFile }
val identityReport = layout.buildDirectory.file("reports/distribution/portable.sha256")
val verifyDistribution = tasks.register("verifyDistribution") {
    group = "verification"
    description = "Verify installed layout and final portable ZIP bytes; write SHA-256 inventory"
    dependsOn(tasks.installDist, tasks.distZip)
    inputs.dir(distributionDirectory)
    inputs.files(runtimeFiles, desktopJar)
    inputs.property("buildHostPath", buildHostPath)
    inputs.file(portableZip)
    inputs.files("LICENSE", "THIRD_PARTY.md", "VERSION", "packaging/licenses")
    outputs.file(identityReport)
    val rootDirectory = distributionDirectory
    val runtimeArtifacts = runtimeFiles.map { it.toList() }
    val applicationJar = desktopJar
    val hostPath = buildHostPath
    val archive = portableZip
    val report = identityReport
    val legalSources = listOf("LICENSE", "THIRD_PARTY.md", "VERSION").associateWith { layout.projectDirectory.file(it).asFile } +
        listOf("BOUNCY_CASTLE_LICENSE.html", "TOTIPO_JAVA_LICENSE").associate { "licenses/$it" to layout.projectDirectory.file("packaging/licenses/$it").asFile }
    doLast {
        fun sha(file: File): String = file.inputStream().use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(65536)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
        val root = rootDirectory.get().asFile
        val expectedArtifacts = runtimeArtifacts.get() + applicationJar.get().asFile
        val expectedJars = expectedArtifacts.map { it.name }.toSet()
        check(expectedJars.size == expectedArtifacts.size) { "Colliding runtime JAR filenames" }
        val notices = setOf("LICENSE", "THIRD_PARTY.md", "VERSION", "licenses/BOUNCY_CASTLE_LICENSE.html", "licenses/TOTIPO_JAVA_LICENSE")
        val expected = expectedJars.map { "lib/$it" }.toSet() + notices + setOf("bin/totipo-desktop", "bin/totipo-desktop.bat")
        val actual = root.walkTopDown().filter { it.isFile }.map { it.relativeTo(root).invariantSeparatorsPath }.toSet()
        check(actual == expected) { "Distribution inventory mismatch: missing ${expected - actual}; extra ${actual - expected}" }
        expectedArtifacts.forEach { check(sha(it) == sha(root.resolve("lib/${it.name}"))) { "Changed original JAR: ${it.name}" } }
        legalSources.forEach { (name, source) -> check(sha(source) == sha(root.resolve(name))) { "Changed notice: $name" } }
        check(root.resolve("bin/totipo-desktop").canExecute() || System.getProperty("os.name").startsWith("Windows")) {
            "Unix launcher is not executable"
        }
        listOf("bin/totipo-desktop", "bin/totipo-desktop.bat").forEach { name ->
            val script = root.resolve(name).readText()
            val windows = name.endsWith(".bat")
            val assignment = if (windows) "set CLASSPATH=" else "CLASSPATH="
            val classpath = script.lineSequence().single { it.startsWith(assignment) }.removePrefix(assignment)
                .split(if (windows) ';' else ':')
            val references = expectedJars.map { if (windows) "%APP_HOME%\\lib\\$it" else "\$APP_HOME/lib/$it" }.toSet()
            check(classpath.size == references.size && classpath.toSet() == references) { "Non-distribution launcher classpath: $name" }
            check("-XX:+DisableAttachMechanism" in script) { "Missing packaged hardening: $name" }
            check("org.totipo.desktop.TotipoDesktop" in script) { "Wrong application main class: $name" }
            check(hostPath !in script && !Regex("/home/|/Users/|/nix/store/|[A-Za-z]:[\\\\/]Users[\\\\/]").containsMatchIn(script)) {
                "Build-host path in $name"
            }
        }
        JarFile(root.resolve("lib/${applicationJar.get().asFile.name}")).use { jar ->
            check(jar.getJarEntry("org/totipo/desktop/TotipoDesktop.class") != null) { "Missing application entry point" }
            listOf(16, 32, 48, 64, 128, 256).forEach { size ->
                val entry = jar.getJarEntry("org/totipo/desktop/icons/totipo-$size.png")
                check(entry != null) { "Missing application icon: $size" }
                val icon = jar.getInputStream(entry).use { ImageIO.read(it) }
                check(icon != null && icon.width == size && icon.height == size) { "Invalid application icon: $size" }
            }
        }
        check(actual.none { Regex("(?i)(junit|test|\\.gradle|\\.git|\\.java$)").containsMatchIn(it) }) {
            "Test/development material in distribution"
        }
        // Read every final ZIP entry, checking CRC, size, exact inventory and extracted bytes.
        val zipFile = archive.get().asFile
        val prefix = zipFile.name.removeSuffix(".zip") + "/"
        val extracted = temporaryDir.resolve("extracted")
        extracted.deleteRecursively()
        check(extracted.mkdirs()) { "Cannot create extraction directory" }
        ZipFile(zipFile).use { zip ->
            val entries = zip.entries().asSequence().toList()
            check(entries.map { it.name }.distinct().size == entries.size) { "Duplicate ZIP entries" }
            val allowedDirectories = expected.flatMap { name ->
                name.indices.filter { name[it] == '/' }.map { prefix + name.substring(0, it + 1) }
            }.toSet() + prefix
            check(entries.filter { it.isDirectory }.all { it.name in allowedDirectories }) { "Unexpected ZIP directory" }
            val files = entries.filterNot { it.isDirectory }
            check(files.map { it.name }.toSet() == expected.map { prefix + it }.toSet()) { "ZIP inventory mismatch" }
            files.forEach { entry ->
                val name = entry.name.removePrefix(prefix)
                val target = extracted.resolve(name)
                check(target.toPath().normalize().startsWith(extracted.toPath())) { "Unsafe ZIP path" }
                target.parentFile.mkdirs()
                val crc = CRC32()
                zip.getInputStream(entry).use { input -> target.outputStream().use { output ->
                    val buffer = ByteArray(65536)
                    while (true) { val count = input.read(buffer); if (count < 0) break; crc.update(buffer, 0, count); output.write(buffer, 0, count) }
                } }
                check(target.length() == entry.size && crc.value == entry.crc) { "ZIP integrity failure: $name" }
                check(sha(target) == sha(root.resolve(name))) { "ZIP/extracted content mismatch: $name" }
            }
        }
        val reportFile = report.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText("# ZIP size=${zipFile.length()} bytes\n${sha(zipFile)}  ${zipFile.name}\n" +
            expected.sorted().joinToString("") { "${sha(extracted.resolve(it))}  $prefix$it\n" })
        logger.lifecycle("Verified distribution: {}", expectedJars.sorted())
        logger.lifecycle("Final ZIP identities: {}", reportFile)
    }
}
tasks.check { dependsOn(verifyDistribution) }

// Compile the separate harness in check; execution remains explicit and it is not distributed.
val qualification = sourceSets.create("qualification")
qualification.compileClasspath = sourceSets.main.get().compileClasspath
qualification.runtimeClasspath = qualification.output + sourceSets.main.get().runtimeClasspath
tasks.named<JavaCompile>(qualification.compileJavaTaskName) {
    options.annotationProcessorPath = configurations.annotationProcessor.get()
}
tasks.check { dependsOn(tasks.named(qualification.classesTaskName)) }
val qualificationRoot = providers.gradleProperty("qualificationRoot")
val qualificationDistribution = providers.gradleProperty("qualificationDistribution")
tasks.register<JavaExec>("filesystemQualification") {
    group = "verification"
    description = "Real-NIO qualification beneath an explicit -PqualificationRoot"
    // With an extracted release, neither main output nor development dependency resolution
    // participates in the execution classpath. The harness remains outside the release.
    classpath = if (qualificationDistribution.isPresent) {
        qualification.output + files(fileTree(qualificationDistribution.get()) { include("lib/*.jar") })
    } else qualification.runtimeClasspath
    mainClass.set("org.totipo.qualification.FilesystemQualification")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
    val root = qualificationRoot
    argumentProviders.add(CommandLineArgumentProvider {
        check(root.isPresent) { "Supply -PqualificationRoot=/explicit/disposable/test/root" }
        listOf(root.get())
    })
}
