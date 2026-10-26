# Development and packaging

Contributor and operator procedures for working from source. For everyday use,
see the [user guide](USER_GUIDE.md). Downloads are on the
[latest release page](https://github.com/totipo-org/totipo-desktop/releases/latest).
The portable ZIP requires a separately installed desktop-capable Java 25 runtime;
the Linux Nix deployment provides managed Java 25. Full native qualification
remains pending for every platform; Windows and macOS are UNQUALIFIED and support
is not claimed. See [installation instructions](USER_GUIDE.md#download-and-install).
[Qualification](../QUALIFICATION.md)
owns evidence; the [release checklist](../RELEASE_CHECKLIST.md) owns release gates.
Building or running does not declare a release.

## Run from source

Clone normally:

```sh
git clone https://github.com/totipo-org/totipo-desktop.git
cd totipo-desktop
```

Gradle resolves `org.totipo:totipo-storage-nio:0.2.0` and its transitive
`org.totipo:totipo-core:0.2.0` from Maven Central. Internet access is needed
for first resolution unless dependencies are already cached or Nix-provided.

Supported development requires Nix: activate `nix develop` or allow the
repository's direnv environment. Both select the same full **JDK 25** and patched
**Gradle 9.8.0** as the Linux package and agent environment. Toolchain auto-download
is disabled. Desktop production, tests and qualification sources target Java 25.
Non-Nix Gradle acquisition is not supported.

```sh
gradle test --tests '*TokenBrowserTest'
gradle run
```

The run command requires a graphical desktop; tests are headless. First dependency
resolution needs network access or the reviewed Nix-provided cache. Use focused
checks while editing; see [qualification policy](../AGENTS.md#qualification-ladder).

The development shell also includes [java-dev-tools](https://github.com/ingon/java-dev-tools)
for Java source navigation in Helix and dependency source resolution. Start Helix
from the activated shell and keep its Java server command as `jdtls`; no local
`languages.toml` is required. `GRADLE_HOME` is supplied for Eclipse/Buildship too.
Run `java-ide-sources` from the repository root to resolve and verify dependency
sources. After intentional dependency changes, use
`java-ide-sources --write-verification-metadata sha256` and review new checksums
against canonical artifacts before accepting them. Runtime dependency versions
and locks are separate from source attachments; the package dependency cache
continues to cover its existing build/check tasks.

After updating the tooling input, reload direnv or reenter `nix develop`, restart
Helix, and check `hx --health java`, then navigate to project, JDK and dependency
sources. Refresh the Gradle import in Eclipse/JDTLS after output-layout changes:
IDE compilation uses `build/eclipse/`, separate from Gradle's classes. Generated
Eclipse files and output are ignored. Each Git checkout gets its own JDTLS
workspace; set `JDTLS_WORKSPACE` only when an explicit override is needed.

For optional graphical regression review, `org.totipo.desktop.SwingSmoke` is the
current driver. After test compilation and `installDist`, run one scenario group
per JVM on an available graphical display:

```sh
SMOKE_CP='build/classes/java/test:build/classes/java/main:build/install/totipo-desktop/lib/*'
java -cp "$SMOKE_CP" org.totipo.desktop.SwingSmoke --list
java -cp "$SMOKE_CP" org.totipo.desktop.SwingSmoke mutations dark
java -cp "$SMOKE_CP" org.totipo.desktop.SwingSmoke conflicts light
java -cp "$SMOKE_CP" org.totipo.desktop.SwingSmoke lifecycle dark 20
java -cp "$SMOKE_CP" org.totipo.desktop.SwingSmoke polish dark
```

Each group accepts `light`, `dark` or `native`, followed by an optional base font
size. Synthetic palettes use Metal; polish retains Metal even with `native`, as
its previous driver did. Other groups use the environment delegate for `native`.
Optional `-Dsun.java2d.uiScale=2` simulates scaling; it does not qualify a physical
display. Lifecycle's unreadable-bootstrap scenario requires POSIX permissions and
an unprivileged user. Drivers use disposable vaults and clear their fixtures on
success or failure. Screenshots are ignored diagnostics under
`review/screenshots/swing-smoke/<group>/`; they are not pixel assertions.

These groups replace the historical S3/S4/S5/PostS5 entry names. Robot execution,
including execution on Xvfb, remains separate from the native
[GUI](../qualification/GUI_SMOKE.md), [packaged launch](../qualification/PACKAGED_LAUNCH.md),
[clipboard](../qualification/CLIPBOARD_SMOKE.md) and
[accessibility](../qualification/ACCESSIBILITY_SMOKE.md) procedures. Run the affected
native procedure at the milestone boundary; this driver does not replace it.

The Nix shell supplies the full desktop-capable JDK. Dependency changes are
deliberate review work: use standard Gradle `dependencies --write-locks` and
`--write-verification-metadata sha256` when needed, review generated locks/hashes,
and separately update the Nix dependency cache below. These commands do not
authorize accepting new artifacts or changing pins.

See [architecture](../ARCHITECTURE.md) for threading, state and ownership policy,
[dependency provenance](../TOTIPO_JAVA_DEPENDENCY.md) for the released dependency
boundary, and [review/](../review/) for historical milestones. The current
create-only credential policy supersedes historical password-change guidance.
Desktop preserves operation-specific failures and uncertainty; it does not probe
storage permissions or fabricate vault-format metadata.

## Packaging and qualification

Publication requires RELEASE-READY
status: reviewed candidate, automated and human Nix checks, verified artifacts,
disclosures and basic human smoke. Full native qualification can follow publication
against original immutable artifacts. Initial targets are NixOS (Nix package and
ZIP with Java 25) and Linux Mint (the same ZIP with independently installed Java 25).
See [QUALIFICATION.md](../QUALIFICATION.md), [RELEASE_CHECKLIST.md](../RELEASE_CHECKLIST.md)
and the [publication procedure](DESKTOP_RELEASE_AUTOMATION.md). Build success is not native GUI,
clipboard, accessibility, or filesystem support qualification.

Development uses `gradle run` and focused tests. The owner and CI use
`nix flake check` as the authoritative package-inclusive automatic gate.
No duplicate full online/offline suites are required before it. Separate
filesystem, native GUI/clipboard and actual package launch evidence remain
necessary for claimed environments. See [QUALIFICATION.md](../QUALIFICATION.md)
and the release checklist for policy and current evidence.

To materialize the canonical generic distribution for development use (this does
not replace final qualification and need not follow every edit):

```sh
gradle verifyDistribution
build/install/totipo-desktop/bin/totipo-desktop
```

`build/distributions` contains the canonical portable ZIP. Gradle `distTar` remains
available for internal use; TAR publication and separate qualification are not required.
The ZIP contains generated launchers,
the application JAR, core/storage-nio/Bouncy Castle runtime JARs, and licenses.
They **do not include Java**: install a **desktop-capable Java 25 runtime** and set `JAVA_HOME` or
provide `java` on PATH. `verifyDistribution` checks installed and final ZIP
inventory, duplicates, CRC/size, extracted bytes, original JAR/license identities,
application entry point/icons and launcher policy. Its report at
`build/reports/distribution/portable.sha256` records ZIP SHA-256/size and every file.
Actual extracted archive launch is a separate check; standard Gradle owns archive
construction. Generated launchers disable JVM Attach
API availability with `-XX:+DisableAttachMechanism`; this does not prevent
same-user process inspection or memory attacks. Development `run` is unchanged.

`VERSION` is the single application-version source. `gradle validateVersion -PreleaseBuild=true`
must pass for a release; it rejects the `0.0.0-dev` development sentinel.
Missing, empty, whitespace-bearing, or unsafe filename versions fail configuration.
Gradle copies that input into the application classpath as
`org/totipo/desktop/application-version.txt` during `processResources`.
**Help → About Totipo…** reads this embedded resource for `gradle run`, the
portable ZIP and the Nix package; no external runtime version file is needed.
`verifyDistribution` compares the packaged resource with the canonical input.

### Nix package — single normal qualification gate

The flake exposes a Linux-only package using the full pinned `jdk25`, Gradle 9
and the same `installDist`. Existing dev-shell/jailed-agent inputs remain intact.
The source is an ordinary repository checkout. Java arrives as released Maven
artifacts through the reviewed dependency cache. Freeze Nix qualification inputs
before the human gate for milestones requiring Nix qualification:

```sh
nix flake check
```

`checks.desktop` and `packages.default` reference the same derivation. This gate
evaluates the flake and realizes the actual desktop package, including its Gradle
tests through check, installed distribution verification and Nix-specific install
checks. It exports the verified canonical ZIP and manifests in its `portable`
output from the same application build, and checks actual installed JAR hashes
against that ZIP. Nix can reuse an already qualified store result; this is not a
forced rebuild. CI uses the pinned Nix environment and this single gate.

To collect release artifact evidence with the tools and exact outputs selected
by this flake:

```sh
nix run .#collect-release-evidence
```

The helper creates a private `build/release-evidence/<UTC timestamp>/` directory;
optionally choose a new directory with
`nix run .#collect-release-evidence -- /absolute/path/to/new-evidence`.
It refuses to overwrite existing evidence and records failures as well as success.
Keep the entire directory and share `summary.txt`, `result.txt` and the manifests
for review. It records out/portable/derivation/NAR identities, runtime closure,
managed Java diagnostics, source revision metadata and original manifests;
checks ZIP size/hash, extraction/inventory, installed JAR equivalence and the
complete deployment inventory; and checksums its evidence files. `source.json`
records revision/dirty status when available; the derivation and manifests
identify actual artifacts even for a dirty worktree. It retains no vault data.
The ZIP remains in the original portable store output; retain that output/closure
separately. `nix run` may materialize missing outputs but requests no forced
rebuild. Collection itself performs read-only Nix queries and file verification,
without GUI launch, a new gate, native qualification or proof of fresh tests.

`nix build .` materializes the default package and creates a local `result`
link when wanted, for example for `./result/bin/totipo-desktop` native testing.
It provides no independent build/test result after the same derivation passes
flake checks and is not an additional qualification gate.
`nix build --rebuild .` is reserved for deliberate release/reproducibility
spot-checks. Its evidence is limited to the tested derivation/builders, not
universal reproducibility. Flake checks do not qualify native GUI behavior,
every filesystem, release publication, or reproducibility across arbitrary builders.

Agents do not run Nix in this workflow. Human routine Nix gate:
`nix flake check`. Do not request an additional ordinary `nix build`
as qualification evidence or a forced rebuild for a routine milestone.

The wrapper fixes `JAVA_HOME` to the managed full JDK and supplies launcher shell
utilities. Runtime use requires neither Gradle, checkout sources nor shell
Java configuration. The desktop entry is Totipo / Utility / Terminal=false, with
no handlers, autostart or placeholder icon. Vaults remain user-selected paths;
installation directories are never vault storage.

The current dependency pin is Java 0.2.0 / v1/r19; BC 1.86 is unchanged.
Gradle locks and verification hashes track reviewed Maven Central artifacts.
Human cache regeneration and review passed. Prior human Nix results are recorded
in the historical r19 reconciliation report; they do not qualify later changes.
For current release readiness and native qualification, consult
[QUALIFICATION.md](../QUALIFICATION.md).
See [dependency provenance](../TOTIPO_JAVA_DEPENDENCY.md).

Only when dependency inputs change, generate/refresh using the flake app, which runs the
official Gradle dependency-cache update script for the current system:

```sh
nix run .#update-package-deps
```

To build and run that script directly instead:

```fish
set update_script (
    nix build \
        --no-link \
        --print-out-paths \
        '.#packages.x86_64-linux.default.mitmCache.updateScript'
)

$update_script

nix flake check
```

Substitute `aarch64-linux` only when qualifying that system. Local pre-commit
qualification uses the Git-aware working tree: modified tracked files remain
unstaged; for each new intended milestone file, use
`git add --intent-to-add <file>` to establish source membership without staging
contents. Require `git diff --cached --quiet` to pass. Staging actual file contents
requires explicit user authorization. Ignored caches, build output and local
secrets must remain outside Git source membership. Explicit `path:` sources are
reserved for intentional raw/non-Git paths, such as generated or exported source.
Do not update `flake.lock` or Gradle verification hashes
just to make the build pass. Review `package-deps.json`: it should match the locked
released Totipo artifacts, retain BC 1.86, and leave unrelated dependencies unchanged.
The update task runs desktop checks and distribution verification only.
The ordinary qualification gate remains the single flake check above.
MITM transport does not waive Gradle's artifact hash verification.

The derivation sets `LC_ALL=C.UTF-8` for dependency fetching and package tests.
NIO filename encoding follows the native locale: `-Dfile.encoding=UTF-8` alone
does not make Unicode paths representable under the C locale. After changing
`package.nix`, regenerate the update script with the command above; an existing
Nix-store script still references its original derivation/source snapshot.

Root nixpkgs is locked at `319e4cebef10cbb636c6ea392137b52ad105336a`.
Its Gradle **9.8.0** is explicitly overridden to use the shared JDK 25 in
development, agent and package declarations. Nix's patched Gradle and official
fetchDeps/setup hook are retained. The dependency cache, locks and verification
metadata remain reviewed inputs.

### Explicit filesystem qualification

Create/select an empty disposable test root on each filesystem to be claimed:

```sh
gradle filesystemQualification -PqualificationRoot=/explicit/disposable/test/root
```

For release evidence supply `-PqualificationDistribution=/absolute/extracted/totipo-desktop-VERSION`
to execute with only the supplied JARs plus the separate harness, or compile the
harness directly against those JARs without Gradle. See
[packaged launch](../qualification/PACKAGED_LAUNCH.md) for copyable commands,
same-build Nix/ZIP materialization, native Linux/NixOS steps and evidence reuse.
Qualification code is never released. Later Windows/macOS qualification uses the
original immutable ZIP without rebuilding; see [qualification](../QUALIFICATION.md).

No default root is provided. The harness creates a unique `totipo-qualification-`
child, uses only test credentials, and deletes only that child without following
symlinks. Keep the selected root private and do not use production vault storage.
Cleanup failure prints the exact remaining test directory. Record the non-secret
environment output and checklist results in QUALIFICATION.md; omit the root path.
A pass applies only to that tested environment, not other Linux filesystems,
network shares, cloud synchronization or FUSE configurations.

Updates are manual. There is no updater or native installer. Signing, tags and
publication require separate release decisions. Future final artifacts get SHA-256 checksums with
the standard `sha256sum -- FILE [FILE ...]` command. See [THIRD_PARTY.md](../THIRD_PARTY.md) for the production inventory and
[the M4a report](../review/M4A_RELEASE_HARDENING_REPORT.md) for all limitations.
