import java.util.jar.JarFile

plugins { application }

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
val verifyDistribution = tasks.register("verifyDistribution") {
    group = "verification"
    description = "Verify essential installed files, runtime inventory and launcher policy"
    dependsOn(tasks.installDist)
    inputs.dir(distributionDirectory)
    inputs.files(runtimeFiles, desktopJar)
    inputs.property("buildHostPath", buildHostPath)
    val rootDirectory = distributionDirectory
    val runtimeArtifacts = runtimeFiles.map { it.toList() }
    val applicationJar = desktopJar
    val hostPath = buildHostPath
    doLast {
        val root = rootDirectory.get().asFile
        val expectedArtifacts = runtimeArtifacts.get() + applicationJar.get().asFile
        val expectedJars = expectedArtifacts.map { it.name }.toSet()
        val notices = setOf("LICENSE", "THIRD_PARTY.md", "VERSION", "licenses/BOUNCY_CASTLE_LICENSE.html", "licenses/TOTIPO_JAVA_LICENSE")
        val expected = expectedJars.map { "lib/$it" }.toSet() + notices + setOf("bin/totipo-desktop", "bin/totipo-desktop.bat")
        val actual = root.walkTopDown().filter { it.isFile }.map { it.relativeTo(root).invariantSeparatorsPath }.toSet()
        check(actual == expected) { "Distribution inventory mismatch: missing ${expected - actual}; extra ${actual - expected}" }
        check(root.resolve("bin/totipo-desktop").canExecute() || System.getProperty("os.name").startsWith("Windows")) {
            "Unix launcher is not executable"
        }
        listOf("bin/totipo-desktop", "bin/totipo-desktop.bat").forEach { name ->
            val script = root.resolve(name).readText()
            check("-XX:+DisableAttachMechanism" in script) { "Missing packaged hardening: $name" }
            check("org.totipo.desktop.TotipoDesktop" in script) { "Wrong application main class: $name" }
            check(hostPath !in script && !Regex("/home/|/Users/|/nix/store/|[A-Za-z]:[\\\\/]Users[\\\\/]").containsMatchIn(script)) {
                "Build-host path in $name"
            }
        }
        JarFile(root.resolve("lib/${applicationJar.get().asFile.name}")).use { jar ->
            check(jar.getJarEntry("org/totipo/desktop/TotipoDesktop.class") != null) { "Missing application entry point" }
            listOf(16, 32, 48, 64, 128, 256).forEach { size ->
                check(jar.getJarEntry("org/totipo/desktop/icons/totipo-$size.png") != null) { "Missing application icon: $size" }
            }
        }
        check(actual.none { Regex("(?i)(junit|test|\\.gradle|\\.git|\\.java$)").containsMatchIn(it) }) {
            "Test/development material in distribution"
        }
        logger.lifecycle("Verified distribution: {}", expectedJars.sorted())
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
tasks.register<JavaExec>("filesystemQualification") {
    group = "verification"
    description = "Real-NIO qualification beneath an explicit -PqualificationRoot"
    classpath = qualification.runtimeClasspath
    mainClass.set("org.totipo.qualification.FilesystemQualification")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
    val root = qualificationRoot
    argumentProviders.add(CommandLineArgumentProvider {
        check(root.isPresent) { "Supply -PqualificationRoot=/explicit/disposable/test/root" }
        listOf(root.get())
    })
}
