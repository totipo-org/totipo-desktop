# M4b-R1: Portable Java distribution and qualification research

## 1. Executive recommendation

**Recommend B: retain the standard Gradle application distribution, with one
canonical portable ZIP for a future 0.1.0 release.** Build its Java payload once;
qualify and publish the same ZIP bytes first on Linux, then qualify those bytes
on Windows/macOS if their native boundaries pass. Keep Nix for development/CI
and as a separately qualified, optional deployment route. Do not require a native
installer or bundled runtime for 0.1.0.

A flattened executable fat JAR is **technically feasible for the current
application on the tested JDK**, including actual encrypted vault operations.
BC signing does not prevent Totipo's lightweight Argon2 use. However, ordinary
flattening with a replacement manifest loses BC's upstream signature, and bare
`java -jar` cannot carry the existing `-XX:+DisableAttachMechanism` default.
Preserving the existing distribution avoids both changes and adds no packaging
plugin or bootstrap code. This recommendation departs from the single-file JAR
preference because a single downloadable ZIP already contains all portable bytes
while retaining original dependency artifacts and generated hardened launchers.
It is an application/security-maintenance choice, not a claim that Java forbids
fat JARs or that stripping signatures breaks Argon2.

C (executable application JAR plus external JARs) is a reasonable future
convenience if `java -jar` is independently valuable. It still needs multiple
files and an explicit JVM option or wrapper for hardening; it offers little over
B for this release. No self-contained bundle is recommended now.

All recommendations below require a later reviewed milestone. **No production,
build, dependency, policy, release-tooling or qualification-status change was made.**
No platform is declared qualified by this research.

## 2. Baseline, scope and provenance

- Starting branch: `main`; HEAD: `2aca8a8a984367aeeb2efc57834b8074bd40ccc5`.
- Starting `git status --short`: empty. No existing changes needed preservation.
- Classification: **DOCS_ONLY**, researching BUILD_DEPENDENCY/PACKAGING_RELEASE
  decisions without implementing either. No application-operation behavior changed.
- Work stayed within this desktop checkout. Downloads, extracted reference
  sources, prototype programs/JARs and logs are under ignored
  `build/research/m4b-r1/`; no sibling repository was modified or used as a source
  substitution. `git check-ignore build/research/m4b-r1` confirms exclusion.
- Local date: 2026-10-24 America/New_York; experimental UTC date: 2026-10-25.
- Environment: Linux `6.18.53`, x86_64/Java `amd64`; no DISPLAY or WAYLAND_DISPLAY;
  distribution not established. Nix-provided OpenJDK `25.0.4.1+1`, runtime label
  `(nix)`, harness `java.vendor=N/A`; Gradle `9.8.0`, revision
  `a927be5e08efe79e0b87ada06c762dde6bb9f8b8`.
- Java executable: `/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1/bin/java`;
  JAVA_HOME ends in that derivation's `/lib/openjdk`. Gradle executable:
  `/nix/store/jma178bc8ijyv2xbb6c142ajylyhvcnc-gradle-9.8.0/bin/gradle`.
  Available toolchain used directly; **agent Nix invocations: zero**.

Read AGENTS.md, build/settings/locks/verification metadata, package/flake/lock,
ARCHITECTURE.md, QUALIFICATION.md, RELEASE_CHECKLIST.md, docs/DEVELOPMENT.md,
THIRD_PARTY.md, dependency provenance, all four manual qualification procedures,
the filesystem harness, representative controller/UI/clipboard/NIO/watcher tests,
SwingSmoke and CI. Historical references include M4a release hardening, M4b
0.1.1/r18 repin, 0.2.0/r19 reconciliation, qualification-ladder and subsequent
Nix-first simplification reports. Their obsolete commands/Java 17 claims are
historical, not instructions to reinstate deleted machinery.

Read the exact [reviewed application guidance](https://github.com/totipo-dev/totipo-java/blob/3b24b54becde0c93479c1fbd80ea0fbd2026e2a8/API_DESIGN.md),
including operation classes/state snapshots, lifecycle, publication and TOTP.
Downloaded blob SHA-256 is
`bd017168bf0dcb103c5880cff9d6d5b2f358c493fbc305e1eea0b0623ca81493`,
matching the repository pin. Experiments reuse the existing single-threaded
filesystem harness: builders stay confined, observation is awaited, historical
update bases deliberately create competing branches, and sessions close before
reopening. New state arrival was not treated as cancellation. No EDT scheduling
or application concurrency policy was modified.

Released implementation is Java 0.2.0 at
`d6310c177ae930df188fd4f5798622c935698b2e`, separately from that guidance pin.
Maven source JARs match existing verification metadata:

| Source JAR | SHA-256 |
| --- | --- |
| core 0.2.0 | `e2661770e0e59ca733959b4931689a9253988352605087c390bfc3a045dd9b6c` |
| storage-nio 0.2.0 | `bb690c98837f938b1cce75cdfc06b44b47b066c4cb937a4ec461f2dc82f5b04b` |

The downloaded KDF source also compares byte-for-byte equal with
[BouncyCastleArgon2idKdf.java at the release commit](https://github.com/totipo-org/totipo-java/blob/d6310c177ae930df188fd4f5798622c935698b2e/core/src/main/java/org/totipo/format/BouncyCastleArgon2idKdf.java).
Research sources never entered the production dependency graph.

## 3. Current packaging architecture

Verified source responsibilities:

| Facility | Present responsibility |
| --- | --- |
| Gradle `jar` | Desktop classes and classpath resources, including six PNG window icons. Thin application JAR; manifest only has `Manifest-Version: 1.0`, no Main-Class/Class-Path. It cannot currently launch alone with `java -jar`. |
| `startScripts` | Unix shell and Windows batch scripts, explicit four-JAR classpath, main `org.totipo.desktop.TotipoDesktop`, default `-XX:+DisableAttachMechanism`. |
| `installDist` | `bin/` scripts, `lib/` application and three dependency JARs, top-level LICENSE/THIRD_PARTY.md/VERSION, two upstream license files under `licenses/`. No JRE, Gradle, source or tests required at runtime. |
| `distZip`, `distTar` | Outer archives of the main distribution. Existing alternatives contain the same application layout; no native installer. All Gradle archive tasks request reproducible order and no preserved file timestamps. |
| `verifyDistribution` | Depends on installDist; compares complete file-name inventory with resolved runtime files plus application JAR/notices/scripts, checks Unix executable bit except on Windows, both launcher main/attach policy and host-path exclusion, main class and six icon entries, and test/development filename exclusion. `check` depends on it. It verifies installed layout, not independently extracted archive bytes. |
| `qualificationClasses` | Separate public-API filesystem harness, compiled through `check`, not distributed. `filesystemQualification` executes it only with an explicit disposable root. |
| Nix package | Filtered production/test/qualification/build inputs; official Gradle dependency cache; strict dependency verification; installDist plus `check`; copies bin/lib, removes BAT, wraps Unix launcher with managed full JDK and shell utilities; installs icons, desktop entry and notices; checks Nix installation transformations. |
| Flake/CI | Shared JDK 25/patched Gradle; Linux-only package/check; `checks.desktop` equals packages.default. CI runs human-authoritative `nix flake check`; it does not produce generic release ZIP/TAR. |

The Unix script selects JAVA_HOME or PATH Java, handles Darwin/Cygwin/MSYS,
uses shell utilities including uname, xargs and sed, then execs Java. BAT uses
JAVA_HOME or java.exe on PATH. Both place DEFAULT_JVM_OPTS before JAVA_OPTS and
TOTIPO_DESKTOP_OPTS; these user-controlled options can override defaults. The
hardening is a default for normal launch, not a protection against an operator
choosing another command. Gradle `run` deliberately retains attach/debug access.

Window icons are classpath PNGs at
`org/totipo/desktop/icons/totipo-{16,32,48,64,128,256}.png`. Nix additionally
installs the hicolor icon tree and a Totipo/Utility/Terminal=false desktop entry.
Generic archives provide window icons, not OS installation/file associations.
Top-level desktop Apache-2.0 LICENSE, THIRD_PARTY.md and VERSION, Totipo Java's
Apache license and the unchanged BC license HTML are explicit distribution
inputs. BC also contains META-INF/LICENSE.md. Generic Java comes from the user;
Nix's runtime legal notices stay in its JDK closure.

Integrity is layered: Maven Central only; strict version locks; reviewed SHA-256
verification of artifacts/metadata; separate fixed Nix download cache. Gradle
PGP verification is **disabled** (`verify-signatures=false`); do not confuse it
with BC's runtime JAR signature. Current verifyDistribution checks names and
selected contents, not per-file byte equality with the resolution cache. There
is no current extracted-archive hash verifier or mandatory independent rebuild
task. The release checklist requires inspection, SHA-256 and deliberate independent
comparisons for reproducibility claims. Historical archive equivalence/rebuild
results retain their original versions, tools and dates.

Linux-specific assumptions reside principally in Nix: platform exposure, glibc
`C.UTF-8`, shell wrapper/utilities, desktop-file/hicolor integration and Linux
install checks. Generic archives have cross-platform scripts but no completed
Windows/macOS qualification. The SHA command examples and colon-separated smoke
classpath are Unix procedures, not portable Windows commands.

If A became canonical, external dependency lib layout, dependency filenames in
launch classpaths, and two generic scripts could become unnecessary **only if**
the hardening decision allowed their removal. Parallel canonical ZIP and TAR
could be unnecessary. Nix could later wrap the JAR directly. Inventory, input
hash verification, legal material, classpath icons, versioning, reproducibility
checks, native qualification and an external Java requirement would still be
necessary. Merging manifests/services/signatures/MR contents becomes new work.

### Background: opvaultfx

Read current master at exact commit
`f0481b10dda2c1c1d02cd0169c719201347ba2b6`:
[build.gradle](https://github.com/ingon/opvaultfx/blob/f0481b10dda2c1c1d02cd0169c719201347ba2b6/build.gradle),
[package.nix](https://github.com/ingon/opvaultfx/blob/f0481b10dda2c1c1d02cd0169c719201347ba2b6/package.nix),
[flake.nix](https://github.com/ingon/opvaultfx/blob/f0481b10dda2c1c1d02cd0169c719201347ba2b6/flake.nix).
It uses application/OpenJFX/Beryx jlink plugins, JavaFX 21, Java 17 compatibility,
a JavaFX-enabled JDK 21/Gradle 8 Nix environment, attach-disabled scripts and a
Nix wrapper around a standard distribution extracted from TAR. Useful lessons:
reuse Gradle's layout, manage Java explicitly in Nix, constrain source inputs,
and preserve launcher hardening. Totipo's direct installDist installation already
avoids the extra TAR extraction. This reference does not establish a single
fat-JAR model or platform qualification. Its JavaFX/modules/plugins/JDK choices
are unnecessary for Swing and should not be copied.

## 4. Exact runtime graph and packaging constraints

Fresh strict dependency report confirms:

```text
totipo-desktop 0.0.0-dev
└── org.totipo:totipo-storage-nio:0.2.0
    └── org.totipo:totipo-core:0.2.0
        └── org.bouncycastle:bcprov-jdk18on:1.86 (runtime)
```

JUnit 6.1.3 and its transitive tree are test-only. No JavaFX, logging framework,
JNI dependency or platform classifier is resolved. Required application launch
payload is the desktop JAR plus all three runtime JARs and a desktop-capable Java
25 runtime. Launch scripts supply the entry point. Legal files must also ship.

| Installed JAR | Bytes | SHA-256 |
| --- | ---: | --- |
| totipo-desktop-0.0.0-dev.jar | 362192 | `82142f8be4eeaf0e9f303dfacac2e9698800f5b75d523bd3cdff35d274d6ec99` |
| totipo-storage-nio-0.2.0.jar | 28471 | `776068249e689e94136748fb8ffd86c837e0af8bbb6cec8ae5e785c4eca4ba93` |
| totipo-core-0.2.0.jar | 178196 | `4f1fb4bb1ab5f0a78c0f2d9a1ed3146413c94f631e7f9b95477968a66968520f` |
| bcprov-jdk18on-1.86.jar | 7224011 | `2af190b300cbb0b35e248ccf5f4a06b6072030aeb3da7a98ec73abe5b4cb371f` |

These are research input identities, not proposed release hashes. Installed JARs
total 7,792,870 bytes. Desktop entry-point class major version is 69 (Java 25);
Totipo dependencies' Java 17 bytecode does not lower desktop's requirement.
No preview flag is used. Newer Java versions require their own evidence; a minimum
bytecode version is not blanket qualification of all future runtimes.

Archive scan found no `.so`, `.dll`, `.dylib` or `.jnilib` entries. NIO/core source
contains no System.load/loadLibrary or native method declaration. BC's jdeps
summary for Java 25 requires java.base, java.logging, java.naming, java.sql;
no external native module. This is evidence about shipped dependencies, not an
assertion that JDK Swing/AWT/clipboard/crypto have no native platform dependencies.

Only cross-JAR file-path collision is `META-INF/MANIFEST.MF` (all four JARs).
No duplicate class path or other resource-path collision was found. Versioned
implementations within BC are intentional multi-release alternatives, not
cross-dependency duplicate classes. BC's manifest has OSGi/package metadata,
Multi-Release=true and per-entry signing digests; the other three are minimal.
BC contains BCRSA204.SF and BCRSA204.RSA. Its sole service descriptor is
`META-INF/services/java.security.Provider`, listing BouncyCastleProvider and
BouncyCastlePQCProvider. No service concatenation is required for today's graph;
a later graph must be re-audited, not silently merged with first-wins behavior.

## 5. Bouncy Castle use, signatures and provider authentication

**Verified released source:** BouncyCastleArgon2idKdf directly constructs
Argon2Parameters and Argon2BytesGenerator. Its Argon2id configuration is version
1.3, 65536 KiB, three iterations, parallelism four, 32 output bytes. No BC JCA
provider is registered or requested anywhere in core. CryptoSupport, Totp,
VaultUnlocker, VaultBootstrapWriter, V1EnvelopeWriter and EnvelopeReader use
ordinary JCA/JCE lookup for SHA-256, HMAC and AES/GCM/NoPadding. EntropySource uses
SecureRandom. Thus core uses **both lightweight BC and JCA/JCE**, but its JCA/JCE
calls do **not specifically use the BC provider**. On this runtime AES-GCM and
HmacSHA1 resolve to SunJCE; merely shipping a Provider service descriptor does not
register BC in the default Security provider list. Probe observed BC=null.

[BC's official documentation](https://www.bouncycastle.org/documentation/documentation-java/)
distinguishes its lightweight API from its JCA provider and says the signed JAR
is required for provider use. Its old policy-file advice should not be copied as
Java 25 setup instructions. [Oracle JDK 25's provider guide](https://docs.oracle.com/en/java/javase/25/security/howtoimplaprovider.html)
requires signing for providers furnishing Cipher, KDF, KEM, KeyAgreement,
KeyGenerator, Mac or SecretKeyFactory. Class discovery/instantiation alone is
therefore insufficient provider qualification.

Observed signature cases:

1. Original BC: `jarsigner -verify` says **jar verified**, with a PKIX certificate
   chain warning (unable to find a certification path in this environment).
   Loaded Argon2 class CodeSource has two certificates. This establishes checked
   JAR signatures here, not globally trusted publisher certification.
2. Flattened JAR with replacement manifest and BC SF/RSA retained: class loading
   fails with **SecurityException: Invalid signature file digest for Manifest main
   attributes**. It is an invalid transformation, not a cryptographic-algorithm
   failure. No elaborate signature workaround was attempted.
3. Flattened JAR with BC signature files and old manifest removed: `jarsigner`
   says **jar is unsigned**; Argon2 class has zero CodeSource certificates.
   Actual encrypted workflow passes. Lightweight Argon2 does not require JCE
   provider authentication. Removing metadata does not change algorithm bytes,
   but it does remove the original runtime signature/integrity boundary.

The probe also instantiated BC explicitly and requested an AES-GCM Cipher:
**both original and unsigned flattened copies succeeded on this Nix OpenJDK**.
This must not be reported as an authentication failure or as universal acceptance.
Installed JDK src.zip explains the result: JceSecurity.verifyProvider constructs
ProviderVerifier with savePerms=false, whose verify immediately returns. The
[OpenJDK 25 source](https://github.com/openjdk/jdk/blob/jdk-25-ga/src/java.base/share/classes/javax/crypto/ProviderVerifier.java)
contains that shortcut. Oracle's provider requirements still constrain a portable
provider packaging decision. No Oracle JDK/other-vendor runtime was tested.
The explicit BC probe checks acquisition only, not a full BC-provider encryption
round trip; Totipo's actual AES operations were exercised through its normal
SunJCE path.

**Conclusion:** flattening is not intrinsically forbidden for this Totipo
lightweight usage. The conventional unsigned transformation sacrifices BC's
upstream signature, and provider portability cannot be inferred from this JDK's
acceptance. Signing the resulting fat JAR with a desktop key would not preserve
BC's original signature or automatically meet JCE signing requirements. Keeping
original signed dependency JARs is the simplest conservative choice. Input
SHA-256 verification and a trusted release checksum remain needed in every model;
a checksum distributed through the same compromised channel is not publisher
authentication.

## 6. Disposable fat-JAR experiment

No Shadow/Spring/custom class loader, plugin, dependency change or tracked init
script was introduced. A Java source-file program using JDK JarFile and
ZipOutputStream built the prototype from verified installDist inputs. Existing
Gradle produced/reused the application classes and separate qualification harness.

Prototype recipe (implemented only under ignored build/research):

- Enumerate four input JARs in filename order; enumerate non-directory entries,
  record owners and compare duplicate bytes. Reject unresolved duplicate content.
- Remove input top-level manifests and SF/RSA/DSA/EC files; exclude root/versioned
  module-info descriptors for a classpath artifact. This is not modular deployment.
- Preserve all other class/resource bytes, including BC's service file, properties,
  META-INF/LICENSE.md and versioned resources. Add desktop LICENSE/THIRD_PARTY.md/
  VERSION and both upstream notices under META-INF/totipo/ with distinct paths.
- Create one manifest with Main-Class=org.totipo.desktop.TotipoDesktop and
  Multi-Release=true. BC contributes 115 retained versioned file entries across
  versions 9/11/15/17/25 after module descriptor exclusion. A JDK 25 JarFile
  versioned view selected a versioned OSGI resource in the probe.
- Sort all output paths lexically and set each ZIP timestamp to
  1980-01-01T00:00 using setTimeLocal, avoiding source timestamps/timezone input.
  Write twice with the same JDK/default deflater, without embedding host paths.

[JDK 25's JAR specification](https://docs.oracle.com/en/java/javase/25/docs/specs/jar/jar.html)
defines Main-Class, Multi-Release and relative Class-Path and the manifest/signature
relationship. It does not provide a generic arbitrary JVM-options manifest key.

| Experiment | Verified result and limit |
| --- | --- |
| Two prototype constructions | Both 7,303,190 bytes; identical SHA-256 `d27b037aee1b3a7e0871eadb74a891b84bb1aa4d55944304ecb2e12f2b9c961e`. Same-host repeatability only; not independent-build or cross-toolchain proof. |
| Ordinary `java -Djava.awt.headless=true -jar .../fat-1.jar` | Main-Class reached actual Swing application, then expected HeadlessException on EDT in ShellFrame. This is entry-point loading evidence, **not GUI PASS**; process exit code is not a reliable GUI-health oracle for an EDT exception. |
| Runtime resources | All six PNGs decoded with ImageIO and dimensions verified from prototype classpath. No filesystem-relative extraction/loading scheme required. |
| ServiceLoader | Both BC provider classes discovered from flattened service descriptor; BC remained unregistered in Security. No conflicting descriptor currently exists. |
| Signed metadata retained | Expected failure above; class load aborted. |
| Signed metadata stripped | Actual KDF/AES/HMAC workflow below passed; upstream signature absent. |
| Existing FilesystemQualification with fat JAR as sole product classpath | PASS at `2026-10-25T03:36:02.344857467Z`, ext4/POSIX=true. Harness classes were separate; no original product/dependency JAR on classpath. |
| Same harness with original four JARs | PASS at `2026-10-25T03:36:04.732886638Z`, same environment. |

Each workflow created an empty disposable vault with test-only credentials,
closed it, reopened, saved/read an ACTIVE SHA1 token, generated a TOTP, reopened,
updated twice from a captured historical base, observed a conflict, merged with
a preserved secret, generated the same code, closed/reopened and checked the
merged state/TOTP again. Resources/builders/subscriptions closed; harness wiped
its input buffers and deleted its unique child without following symlinks. The
explicit parent `build/research/m4b-r1/disposable root` is empty afterward. It
exercises actual 64 MiB Argon2id and authenticated encryption/decryption, not a
mock KDF or superficial class load.

Limits: this is neither the protocol conformance corpus nor full qualification.
No independent TOTP known-answer vector, tampering/wrong-password matrix, crash,
power loss, hostile provider, GUI, native clipboard, watcher-on-fat-JAR or other
OS/runtime was exercised. Preserving an icon path and loading an entry point do
not prove all exception flows. Existing controllers catch operation failures and
sanitize user presentation; TotipoDesktop schedules construction on EDT without
a top-level exception dialog. Packaging alone does not improve uncaught EDT
exception handling. Native application errors need the existing manual checks.

Size decreased by 489,680 bytes (~6.3%) versus the sum of four JARs, principally
with changed metadata/compression; no minimization/relocation was performed.
Six fresh-JVM diagnostic probes per layout took these milliseconds from process
creation through exit:

```text
fat:      263, 263, 276, 259, 261, 255
separate: 473, 504, 506, 473, 485, 485
```

The diagnostic loads Argon2, queries providers/services, acquires Ciphers and
checks VM flags; it does not open a window. File caches were uncontrolled and
layouts were measured sequentially. Signature verification contributes to the
separate-JAR work. These numbers show no observed probe startup penalty from
flattening; they are **not GUI startup measurements or a release performance
promise**. There is no nested-JAR extraction or bootstrap overhead.

## 7. Runtime-hardening implications

**Bare `java -jar totipo-desktop.jar` cannot guarantee the current attach flag.**
The [Java 25 launcher manual](https://docs.oracle.com/en/java/javase/25/docs/specs/man/java.html)
documents DisableAttachMechanism and JDK_JAVA_OPTIONS. The flag is a HotSpot
advanced option, not a Java-SE-wide security guarantee. A manifest cannot request
it, and a system property with that name is not the VM option.

Local HotSpotDiagnosticMXBean probe reports DisableAttachMechanism=false,
writeable=false. Attempting setVMOption fails with IllegalArgumentException.
Starting the same probe with `-XX:+DisableAttachMechanism` reports true. An
application cannot simply switch this startup flag on after main begins.

| Option | Consequence |
| --- | --- |
| `java -XX:+DisableAttachMechanism -jar totipo-desktop.jar` | No wrapper needed, but longer explicit command; protects that invocation. |
| JDK_JAVA_OPTIONS or JAVA_TOOL_OPTIONS configured by operator | Can supply startup flag, but external configuration and effects on other Java processes; JAR cannot require it. Existing script-specific JAVA_OPTS is not interpreted by bare java. |
| Standard Gradle scripts / Nix wrapper | Existing default retained; user can still deliberately override. Least new code and matches current policy. |
| Document bare launch as a weaker route | Requires explicit maintainer policy decision and accurate support/security wording; not adopted here. |
| Inspect flag and refuse unhardened startup | Could enforce admission on supported HotSpot, but changes behavior and bare command fails; new implementation/vendor handling needed. |
| Relaunch self with flag / embedded launcher agent | Adds process lifecycle, argument propagation and failure complexity; an agent attribute itself cannot retroactively change this non-writeable flag. Not recommended as research workaround. |

DisableAttachMechanism restricts Attach API tooling. It is not protection against
all same-user process inspection, memory attacks or OS compromise. A fat JAR
cannot preserve the current *automatic default* with exactly the desired bare
command through standard manifest facilities. C has the same limitation.

## 8. Distribution choices

| Criterion | A: flattened executable fat JAR | B: Gradle application distribution | C: executable JAR + external dependencies |
| --- | --- | --- | --- |
| User installation | One JAR plus Java; attractive download/launch command, but hardening needs extra option | One archive extraction plus Java; launch platform script; keep directory together | One archive extraction plus Java; keep app and lib files together; `java -jar` entry point |
| Dependency/crypto integrity | Verified inputs, but ordinary flattening discards BC signature; direct Argon2 works here | Original signed BC JAR retained byte-for-byte; no merge semantics | Original signed BC JAR retained byte-for-byte |
| Maintenance | New collision/signature/MR/legal merge and verification rules, re-audit on dependency updates | Existing built-in tasks and verifier; smallest change | Small standard manifest addition with exact relative Class-Path and file layout checks; extra launch route |
| Supply chain | No new plugin necessary, but custom merge logic is another trusted transformation | No new plugin; reviewed three-dependency graph | No new plugin; manifest paths must not escape distribution or fetch remote dependencies |
| Portability | Java payload portable; native services/FS still require qualification | Same payload/scripts usable across OS candidates; extraction permissions/utilities need checks | Standard relative URL Class-Path works across OS; no nested JAR loading; URL escaping/wrapping/path spaces must be verified |
| Hardening | Explicit JVM option/environment or new policy/code | Current generated-script default | Bare java has A's limitation; scripts restore B's default |
| Identity/reproducibility | One product hash, harder provenance mapping; deterministic archive possible | Archive hash plus per-file inventory; exact original dependency hashes | App hash alone insufficient; dependency hashes and layout part of runnable identity |
| Later jpackage | Usable classpath input, with signing decision carried forward | Direct lib directory/main JAR input; intact dependencies | Direct app/lib input; simple manifest |

C would add Main-Class and a manifest Class-Path such as
`lib/totipo-core-0.2.0.jar lib/totipo-storage-nio-0.2.0.jar lib/bcprov-jdk18on-1.86.jar`
if the entry JAR sat at the distribution root. Values are relative URLs, not OS
classpath separators or `lib/*`. This layout is a design example, not built or
tested here. A JAR merely containing nested untouched JAR files is not executable
by the ordinary class loader without additional machinery. No such machinery
was prototyped.

[JDK jpackage](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jpackage.html)
can consume an application and runtime to produce native images/packages; each
platform format must be built on its target platform. All three models leave
that future layer possible. Such bundles would have separate platform/runtime/
installer identities and could not be treated as the original portable bytes.
No jpackage/jlink build was run.

## 9. Cross-platform qualification gap matrix

“Portable source” below is a static assessment, not a Windows/macOS PASS. None of
those platforms was available. Linux rows refer to bounded evidence, not blanket
Linux support. Qualification must consume the released runtime bytes; rerunning
a project Gradle test build that resolves another payload is insufficient.

| Category / existing evidence or harness | Linux | Windows adaptation / gap | macOS adaptation / gap |
| --- | --- | --- | --- |
| Platform-independent JVM logic: Base32/URI/password/untrusted text, state/controller fakes, retirement, TOTP presentation, fake clipboard and clock/resume tests | Existing headless JUnit harness; current milestone did not rerun it | Most test source reusable on desktop-capable Java 25; test runtime classpath separator/runner needs adaptation; headless Ctrl fallback does not test native keys | Same; native Command behavior not proven by headless tests |
| Portable artifact verification: inventory, hashes, main/resources, notices, no host paths, signatures/MR | Existing verifyDistribution passes installed layout; outer archives inspected historically | JarFile/MessageDigest checks reusable; replace shell hash/extraction commands and execute BAT, do not require Unix executable bit | Same Java checks reusable; native extraction/Unix launcher permissions/utilities must be checked |
| Native NIO create/open/save/merge/reopen: FilesystemQualification, NioSmokeTest, S3Nio/S4Nio/NioVaultCreation | Historical r19 ext4 PASS and this research's two ext4 workflow passes | Public-API harness itself has no mandatory POSIX calls; default provider hard links, NOFOLLOW and directory-channel force need actual testing; may block support | Same capability tests needed on selected filesystem/provider; cannot infer from Unix ancestry |
| Native watcher: VaultFolderWatcherTest and WatcherSessionIntegrationTest combine fake keys with actual WatchService/external NIO publication | Historical tmpfs/ext4 watcher results; manual synchronized-change smoke reported | Fake source logic reusable; real registration, replacement, bursts, overflow/degradation, close/unblock and event timing require native runs | Same; no guaranteed event timing/provider behavior or remote-change detection |
| GUI/keyboard/accessibility: headless UI tests; optional SwingSmoke; GUI_SMOKE/ACCESSIBILITY_SMOKE | Full native matrix remains incomplete | Native LAF, chooser/hidden directories, Ctrl/menu keys, scaling/fonts/focus/Robot and accessibility require actual display | Native LAF, Command keys, activation/application lifecycle, fonts/scaling/focus/Robot/accessibility require actual display |
| Clipboard/native integration: TotpClipboardTest/probes; CLIPBOARD_SMOKE | Logical fake ownership/expiry tests exist; native Copy reported, full expiry/history/ownership matrix missing | Same checklist concepts; native clipboard contention, marker interoperability, identical external replacement, expiry/clearing and manager/history need real paste targets | Same; record clipboard managers/paste apps; Java ownership callbacks are not proof of platform marker preservation |
| Inactivity/background/session locking: DesktopApplication/InactivityLock/ResumeGuard and lifecycle tests; DesktopEvents hooks | Policy and fake clocks tested historically, native lifecycle matrix missing | Supported Desktop hooks must be observed, including sleep/wake, OS lock, minimize/background and missed-event fallback | Same; actual support is queried at runtime, not assumed from OS name |
| Platform packaging: Nix wrapper/desktop/icon install checks; PACKAGED_LAUNCH | Linux-specific Nix route separate from generic archive; native route still needs evidence | Nix Linux package does not apply. Qualify BAT from exact generic archive outside checkout, path spaces/Unicode/minimal Java env | Nix Linux package does not apply. Qualify Unix script from exact generic archive; no installer/notarization claim |
| Human-operated scenarios: all four manual checklists | Required on actual artifact/environment | Procedures largely reusable with platform terminology/tools; no simulated PASS | Same; screen reader only claimed if actually used |

### Filesystem and harness findings requiring care

- **Create-only installation, not generic atomic replacement.** Released
  NioCanonicalInstaller's ordinary shared-store path uses Files.createLink from
  a complete forced stage into the canonical name. It deliberately does not use
  ATOMIC_MOVE or REPLACE_EXISTING. A private coordinated-move implementation
  requires exclusive application control and is not desktop's ordinary entry
  point. Do not “port” by changing these semantics in packaging.
- NioDurability opens the containing directory with READ/NOFOLLOW_LINKS and calls
  FileChannel.force(true); unsupported operations fail. A Java classpath is
  portable while this runtime filesystem capability may not be. Windows/macOS
  could fail the original artifact's create/save path; this is an **untested risk**,
  not an established failure. If Java implementation changes prove necessary,
  that needs a new artifact/version; later paperwork cannot qualify broken bytes.
- [Files.createLink](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html#createLink(java.nio.file.Path,java.nio.file.Path))
  is optional. [FileChannel.force](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/channels/FileChannel.html)
  has provider/storage limits. Harness success is not crash/power-loss durability,
  network/FUSE/cloud synchronization or every filesystem/mount configuration.
- No FileLock/tryLock use was found in released ordinary NIO sources. Concurrent
  access uses create-only immutable publication and core/session coordination,
  not a claimed cross-process exclusive lock. Historical branch/merge tests and
  independent NIO watcher publication do not exhaust simultaneous-process races.
  [FileLock's platform-dependent semantics](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/channels/FileLock.html)
  are not an unimplemented platform guarantee. Test concurrent publishers and
  external file-handle/deletion interactions on each claimed provider.
- NioFiles enumerates exact child spellings rather than trusting lookup aliases;
  NOFOLLOW checks reject inappropriate namespace nodes. Registry normalization
  deliberately avoids toRealPath/symlink resolution. Case-folding, Unicode naming,
  path spaces, long paths, roots and alias behavior need native scenarios, not an
  assumption that Windows/macOS have one universal case policy.
- **Concrete test portability gaps:** VaultRegistryTest.normalizationDoesNotResolveSymlinks
  creates a real symlink without a capability guard; Windows privileges/provider
  support may affect execution. SwingSmoke's unreadable-bootstrap lifecycle case
  unconditionally calls setPosixFilePermissions and needs an unprivileged POSIX
  environment. Report unavailable capability rather than silently passing it.
- DirectoryPickerTest already handles DOS hidden attributes separately in
  platformHiddenDirectoryIsOmitted. Its `.hidden`/`.git` omission expectation
  also matches DirectoryPicker.visibleDirectory's explicit dot-prefix filter,
  independently of Files.isHidden. That case is **not** evidence of a POSIX-only
  assumption. Native hidden-attribute behavior still needs execution on the
  claimed provider. No tracked test was changed here.
- DEVELOPMENT's SwingSmoke command uses colon-separated classpaths; Windows uses
  semicolons. `sha256sum`, shell/tar examples and Nix install checks are Linux/Unix
  operational instructions. Use standard Java verification or equivalent native
  tools (for example Get-FileHash on Windows) and record tool identities.
- [WatchService documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/WatchService.html)
  makes timing, ordering and detection implementation-specific. Existing
  real-event tests use finite waits and some bounded burst assertions; investigate
  timing/provider failures rather than equating a timeout to a product semantic
  defect or merely increasing it. Manual refresh remains required fallback.
- SwingSmoke is a Robot scenario driver, not native qualification by itself.
  Xvfb/synthetic Metal palettes/scaling do not establish physical-display or
  native-LAF/clipboard behavior. Headless tests set java.awt.headless=true.
- DesktopEvents installs sleep/user-session hooks only if supported. Inactivity
  uses a 15-minute wall-clock deadline; ResumeGuard detects >=30 s processing
  gaps or >5 s wall/monotonic discrepancy. It cannot guarantee every OS lock or
  short suspend, and long EDT stalls can conservatively lock. Preserve those
  limits in platform evidence.
- Runtime qualification must name distribution/build, architecture and security
  configuration. This research's provider-verifier difference is a concrete reason
  that “Java 25” alone is too broad. Require java.desktop and necessary standard
  crypto modules/providers; do not quietly substitute a headless/minimal JRE.

## 10. Proposed progressive qualification and evidence model

This is a policy proposal; QUALIFICATION.md and support claims remain unchanged.

1. Review/freeze candidate source, VERSION=0.1.0, dependencies, build tools and
   qualification definitions. Complete the existing owner automatic gate after
   consumed inputs stabilize. Build the canonical portable payload **once** in
   the supported Nix development environment; do not rebuild on each target OS.
2. Record a release manifest: source commit/tag intent, exact tools/locks, complete
   relative-path inventory, byte sizes, SHA-256 of every JAR/script/legal file,
   and SHA-256/size of the final ZIP. Preserve original Maven dependency bytes.
   Retain the exact files, not only a hash list.
3. Qualify those exact extracted bytes on a narrowly named Linux environment and
   filesystem. Run automatic, filesystem, native GUI/keyboard/clipboard and
   actual package-launch boundaries with separate results. Hash before and after
   testing. Read-only release installation and disposable test vaults are useful
   checks against accidental package writes.
4. Publish immutable 0.1.0 artifacts/checksums only after the required Linux checks
   and owner authorization. Release notes say which Linux tuple is qualified;
   Windows/macOS explicitly UNQUALIFIED. Neither a BAT file nor portable bytecode
   implies support.
5. Later, download the **original immutable ZIP** on Windows/macOS, verify its
   original SHA-256 and all extracted payload hashes, then run the same applicable
   categories and native adaptations. Do not resolve replacement product JARs,
   rebuild, move the release tag or increment the version just to add evidence.
6. Record an append-only dated evidence document for each attempt. A complete
   passing required matrix can extend the support status for those bytes and
   tested tuple. FAIL/UNQUALIFIED stays explicit. Necessary product/dependency
   fixes require a new version and qualification; do not replace 0.1.0 assets.

### Product identity versus transport identity

For A, the executable fat-JAR SHA-256 is the product payload identity. A ZIP/TAR
around it has a separate hash; recompressing/repacking changes transport identity
even if the JAR remains unchanged. Evidence must say which was actually used.

For recommended B, **the desktop JAR hash alone is not the runnable application
identity**: all three dependency JARs and launchers/policy matter. Name the
canonical ZIP hash and the full extracted inventory; the desktop JAR's individual
hash remains stable and separately recorded. ZIP and TAR built from the same
layout have different transport hashes and require their own extraction checks
if both are published/claimed. The same principle applies to C. A later Nix
package/store closure has an additional deployment/runtime identity; it does
not replace the portable manifest or inherit native qualification automatically.

Proposal: publish only the existing ZIP format as the canonical generic download,
rather than maintaining two equal-status generic archives. This needs explicit
checklist/procedure edits in the next milestone because today's policy demands
both ZIP and TAR. Until then both requirements remain in force.

### Evidence record fields and later documentation

Each attempt should record:

- Release version/source commit, canonical artifact filename/size/SHA-256, each
  product JAR's hash, extracted inventory, acquisition source and verification.
- Qualification harness/test-source/runner revision and hashes, commands, actual
  execution versus cache reuse, counts, results and retained non-secret logs.
  Freeze a small separate qualification kit or source revision; do not put JUnit
  or qualification code into the release application. Run it against the original
  release JARs. Compiling test code is not rebuilding the product.
- UTC date, operator/reviewer, OS/distribution/build, architecture, exact Java
  vendor/distribution/version/build, VM flags/security configuration.
- Filesystem/provider/mount context, local versus remote/sync location, tested
  capability/permission assumptions; omit personal vault paths.
- GUI session/display protocol, desktop/window manager, LAF, scaling, keyboard
  layout, clipboard manager/history, paste target, accessibility tools if tested.
- Separate PASS/FAIL/UNQUALIFIED for every required scenario, cleanup/shutdown,
  failures/retries, limitations and exact scope of any resulting support claim.

Commit new evidence documents and update a current support-status index on the
normal documentation branch, each linking original release hashes and earlier
attempts. Add a dated release-note addendum linking those documents if desired;
preserve the original publication statement and historical failures. Do not
rewrite evidence rows as if later runs occurred at release time. No replacement
asset/tag mutation, repack or rebuild is needed for an evidence-only extension.
Tests and documentation changes after release get their own revision; original
product bytes and earlier evidence retain their identities. A later test that
reveals a real defect can narrow support status without erasing the original
observation.

## 11. Minimal path to desktop 0.1.0

### Existing Linux evidence

QUALIFICATION.md records r19 ext4 filesystem PASS on 2026-10-09 with OpenJDK
25.0.4.1+1, plus earlier ext4 evidence of older semantics. Refresh/create/layout
work records real Linux tmpfs/ext4 watcher cases and human-reported focused UI/
external Syncthing smoke. The 0.2.0 reconciliation records 417-test forced normal
and offline executions for that historical source. Subsequent reports preserve
owner Nix PASS and quick GUI/Copy smoke with incomplete environment/artifact
logs. These are useful bounded observations, not evidence that every later
commit or the eventual VERSION=0.1.0 bytes passed. Historical M4a/r18 archive hash
comparisons are similarly not current independent reproducibility proof.

### Genuinely outstanding release boundaries

- Choose reviewed candidate VERSION/source and final artifact identity. Current
  VERSION is still `0.0.0-dev`; no release artifact exists.
- Final automatic checks on frozen inputs via owner `nix flake check`; record
  reuse honestly. No extra duplicate normal/strict/offline full suites by habit.
- Construct/inspect final generic artifact(s), exact hashes/inventory, extracted
  launch outside checkout in paths with spaces/Unicode, external Java 25 and
  documented launcher flag behavior.
- Explicit filesystem workflow for the claimed Linux filesystem/environment,
  using the candidate's original runtime bytes, plus relevant native watcher
  integration. Current harness is a useful sufficient workflow procedure, not a
  comprehensive filesystem durability certification.
- Full native GUI/keyboard/accessibility/clipboard and package launch checklists,
  dated and tied to those bytes. Missing contention/history/sleep/ownership/
  accessibility cases cannot be inferred from “Copy works” or headless PASS.
  Resolve required but impractical scenarios through reviewed scope/policy,
  not by relabeling an unperformed check PASS.
- Independent final-artifact build comparison if reproducibility is claimed;
  current deterministic archive configuration and this prototype's two writes
  do not establish that release claim.

The existing four manual procedures and blocking checklist already cover most
of the necessary Linux behavior. Do not replace them with a larger generic test
framework. Small changes should separate canonical portable identity from Nix
and outer-archive identity, run the public harness against release files, and
record complete evidence. Current release checks explicitly assume ZIP **and**
TAR, generated scripts and a mandatory Nix native route; they do not qualify a
standalone JAR.

**Nix recommendation:** retain the optional deployment package and authoritative
repository check. For the smallest 0.1.0 product release, qualify the portable ZIP
on one Linux architecture/runtime/local-filesystem/desktop tuple. Do not make
native Nix package publication a second prerequisite unless the maintainer wants
to claim that route in 0.1.0. Its existing install checks continue through the
repository gate; native Nix launcher/desktop-entry qualification remains separate
and pending if unclaimed. Today's checklist still requires the Nix route: this
research does not waive it. A policy amendment must precede release.

Defer Windows/macOS runs, additional Linux tuples/filesystems, self-contained
runtime images, installers, OS signing/notarization, file associations and
updating. Unqualified platforms/filesystems remain explicit. Linux-first must
not become “all Linux” or “all Java 25 distributions.”

### Concise next milestone proposal (not implemented)

**M4b-R2: Freeze the portable-distribution contract and Linux evidence procedure.**
Keep B, the current dependency graph and launcher hardening. Review whether one
existing ZIP replaces dual ZIP/TAR publication and whether Nix deployment is
optional for 0.1.0. Add final-payload inventory/hash and extracted-artifact checks,
plus a minimal way to run the existing qualification harness against exact
release JARs; update affected procedures/support-status evidence format. Preserve
all native check boundaries. No UI/crypto change, new packaging plugin, bundled
runtime or installer. Finish that implementation's focused/broad required checks
and human gate before a separate candidate-version/native qualification and
owner-authorized release milestone.

If A is selected instead, R2 must first explicitly settle signature loss and
startup hardening, implement fail-on-collision/MR/legal handling and artifact
checks, and test real operations on each claimed JDK. The current prototype is
not approved release tooling.

## 12. Maintainer decisions still required

1. Accept B as the canonical portable application, or prioritize a single JAR
   enough to accept/document A's signature transformation and choose its hardened
   invocation policy? Technical feasibility alone does not decide that tradeoff.
2. Publish one canonical existing ZIP, or retain ZIP/TAR with separate identity
   and extraction obligations? No new format is proposed.
3. Keep Nix native deployment separately qualified/optional, or include it in
   the initial product support claim and complete its native route checks?
4. Name the first Linux architecture, Java distribution/build, filesystem and
   desktop tuple; establish what limited accessibility/clipboard/environment
   behavior is required versus explicitly outside scope before testing.
5. Approve append-only post-release evidence/support-status updates against
   immutable bytes and select where the current support index will live.
6. If later Windows/macOS fail NIO durability/link capabilities, accept remaining
   UNQUALIFIED on 0.1.0 or commission a separately versioned Java/product change?
   No evidence supports promising eventual success for those original bytes.

## 13. Commands, results, accounting and limitations

All build commands used the available Nix-provided tools without invoking Nix.
No clean, release build, archive rebuild, broad qualification ladder or full
JUnit suite was necessary for this report-only milestone.

| Command family (actual commands / local programs) | Runs and result |
| --- | --- |
| `pwd`; `git status --short`; `git branch --show-current`; `git rev-parse HEAD`; file reads/rg; tool/environment identity | Read-only baseline and audit; clean main recorded. An initial `rg --files ... scripts ...` found no scripts directory; current tree inspected instead. Initial assumed user Gradle-cache paths did not exist; installDist supplied resolved runtime bytes. |
| `python3` download helper attempt | 1 failed: python3 unavailable; no files produced by it. Replaced with available curl/JDK facilities, no installation. |
| `curl -fLsS` reference downloads | Maven core/storage source JARs, pinned guidance, Java source tree/KDF and opvaultfx commit/build/package/flake read-only. Optional opvaultfx gradle.properties returned 404; no such input assumed. A documentation lookup of old OpenJDK JarVerifier path also returned 404; current ProviderVerifier and installed source were used. |
| `java -version`; `gradle --version`; `command -v`; environment/uname/date | Exact identities above; no GUI display. |
| `gradle dependencies --configuration runtimeClasspath --dependency-verification=strict` | 1 PASS; 1 task executed; graph only, no tests. |
| `gradle jar installDist qualificationClasses --dependency-verification=strict` | 1 PASS; 6 actionable tasks all UP-TO-DATE. Reused existing Gradle outputs, not claimed fresh compilation or tests. |
| `gradle verifyDistribution --dependency-verification=strict` | 1 PASS; 1 task executed, 5 UP-TO-DATE; current installed inventory/icons/scripts. |
| `unzip -q` source extraction; `sha256sum`; `cmp` tagged KDF; `jar tf`; `javap -verbose` | Source hashes/pin agreement, resource inventory and class major 69 recorded. |
| `java build/research/m4b-r1/Audit.java` | 1 PASS; audits four JARs and writes fat-1/fat-2 once each; same deterministic hash. |
| `javac -d build/research/m4b-r1 .../Probe.java`; `java -cp ... Probe <disposable-root>` | 1 compilation PASS; 2 workflow executions PASS (fat and separate); each includes provider/flag diagnostics. Test secrets only; cleanup confirmed. |
| `jarsigner -verify` original BC and fat-1 | 2 invocations: original signature verified with trust-chain warning; prototype unsigned as intended. Not a strict trust-chain PASS. |
| `java -Djava.awt.headless=true -jar .../fat-1.jar` | 1 expected HeadlessException on EDT; main loading only, no GUI PASS. |
| `java .../Variants.java`; `java -cp ...fat-invalid-signatures.jar Probe` | 1 invalid variant built; 1 expected classloading failure from manifest signature digest. Product/workspace unaffected. |
| `java .../Timings.java` | 1 benchmark driver, 12 child JVM diagnostic probes (6/layout), all exited successfully; no vault workflows in those children. |
| `java -XX:+DisableAttachMechanism -cp ...fat-1.jar Probe` | 1 PASS diagnostic; flag true, runtime write rejected as expected. |
| `jdeps --multi-release 25 --ignore-missing-deps --summary ...` | 1 command-option failure: correct short option is `-s`; corrected invocation PASS, four standard modules listed. |
| ResourceProbe compilation/execution | First compilation failed: four-argument JarFile constructor requires File, not String; attempted run consequently class-not-found. Corrected ignored source to Path.toFile; second compilation and execution PASS, six PNGs decoded/MR resource selection. No product failure concealed. |
| `git diff --check`; `git diff --cached --quiet`; final path/status inspection | PASS after report creation; only report in source diff, no staged content. |

Baseline/focused/full Gradle **test executions: 0 / 0 / 0**. Gradle read/build/
installed-verifier invocations: 3, all successful; no Gradle failure to diagnose.
Experimental filesystem workflows: **2 PASS**; invalid-signature negative case:
**1 expected failure**. Native GUI/keyboard/clipboard/accessibility/packaged GUI
runs: **0**. Windows/macOS and alternate JDK/hardware/filesystems: **not tested**.
Nix agent runs: **0**; human gate runs requested/reported for this milestone:
**0**. No new qualification status is inferred from historical human reports.
No tag, commit, release, upload, workflow or support claim was created.

Useful ignored logs are dependencies.log, build.log, verify.log, audit.log,
fat-vault.log, separate-vault.log, bc-signature.log, fat-signature.log,
invalid-signatures.log, fat-launch.log, timings.log, hardened.log, jdeps.log and
resources.log beneath build/research/m4b-r1. Disposable sources remain there for
inspection; they are not repository tooling and are not required to read this
report. This report preserves the substantive outputs and failure limitations.

Input invariance: package.nix excludes review/build/.gradle and uses an explicit
source allowlist; flake references package-deps.json and flake.lock explicitly.
No consumed production/test/build/package/qualification input changed, so this
report does not invalidate prior evidence for those inputs or require a new
human Nix gate. It does not elevate that evidence to final-release qualification.
Reference baseline hashes: flake.lock
`f9e1f904f518b5f205444b14e0ff7e615c918e5b4037c6caa96531972e59b1b3`;
package-deps.json
`01c8e8f5c67800876fe44b0890dcb2d6d5de67c58b936956646c4dd8390f6a5c`.

Final branch/HEAD unchanged. Report added with intent-to-add only, leaving its
content unstaged. `git diff --cached --quiet` passes. Final `git status --short`:

```text
 A review/M4B_PORTABLE_DISTRIBUTION_RESEARCH_REPORT.md
```

Experimental outputs remain ignored; only this report is an uncommitted source
change. QUALIFICATION.md, VERSION and all supported-platform claims are unchanged.
