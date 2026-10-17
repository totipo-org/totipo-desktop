# Totipo Desktop

A Java/Swing desktop application for Totipo. Current status: **M4a release
hardening, unreleased and NOT QUALIFIED**. The feature set includes explicit
TOTP clipboard copying, alongside merge/conflict resolution, token create,
ordinary update, and publication uncertainty. S2 adds a persistent application
shell and desktop locking on the committed S1 visual foundation. The application supports local
vault create/open, observation and diagnostics, logical-token/TOTP
browsing, manual Base32 token creation, and ordinary update of an explicitly
selected semantic alternative. One application window contains no-vault, locked,
unlocked, or blocking vault content, with at most one session. Refresh requests local observation.

While OPEN, ordinary changes in the vault's `objects-v1` namespace automatically
request local refresh. Best-effort JDK filesystem notifications can coalesce or
lose events; they do not establish synchronization completion. Vault → Refresh
and F5 remain available. Lock, Change Vault and Exit stop watching.

Selecting a recognizable vault remembers that location immediately, before any
password succeeds. Startup opens directly to its locked form. A missing or
non-vault remembered location shows a selection warning. A non-vault selection
does not replace a previously remembered vault. The full path appears in the
locked form; the title uses the vault basename.
The locked form and Vault menu offer Create New Vault using the existing creation
flow. Cancelling or failing creation preserves the previous remembered vault;
successful creation opens and remembers the new vault. Uncertain creation keeps
the previous remembered location and gives recovery advice without deleting data.

Vault → Lock and Ctrl+L (also the platform menu shortcut on macOS) retire owned
forms without draft confirmation, clear reveal/clipboard ownership, and close the
session. Change Vault locks first; cancelling its chooser leaves the previous
vault locked. File → Exit and window close exit the application. Desktop inactivity
locks after 15 minutes; only direct keyboard/pointer/scroll interaction resets
the deadline. Focus loss/minimization alone does not lock or conceal.

Pure-JDK user-session and sleep listeners lock where supported. A portable fallback
locks after a 30-second event-processing pause or a wall/monotonic clock discrepancy
over five seconds; long UI stalls and clock corrections can conservatively lock too.
Without platform listeners, short suspend/OS-lock detection is not guaranteed.
Deadline checks run before input and on activation. Native event delivery and
resume privacy still require desktop qualification. No native dependency was added.
See [the S2 report](review/S2_PERSISTENT_SHELL_LOCKING_REPORT.md).

S2.2 uses a centered welcome stack, an informational locked-vault path, and a
restricted existing-vault directory picker with folder navigation and no rename,
delete, filename, filter, or new-folder controls. Create keeps its separate chooser.
The normal collection header is Search / count / Add; Refresh stays in the Vault
menu and on F5. Empty vaults show a centered Add action; zero search results show
Clear Search while retaining the normal header. Filtered counts use “M of N.”
See [the S2.2 report](review/S2_2_SHELL_COLLECTION_REPORT.md).

Token issuer/account text uses the remaining row width and elides by pixel width,
with full accessible metadata and tooltips for clipped text. Concealed codes
reserve no code/timer space; revealing adds a compact code/timer and natural-width
Copy action. Resizing and concealing reclaim identity space without rebuilding rows.

Search filters logical TOTPs by issuer/account only, using case-insensitive
whitespace-separated AND terms across both fields. A matching conflict Alternative
keeps the full group visible, with result counts and distinct empty states.
Ctrl/Cmd+F focuses Search, Escape in Search clears it, F5 refreshes, and Ctrl/Cmd+N
opens Create Token when available. Normal list arrow/Page/Home/End navigation is
preserved. Hiding a selected token clears its selection; clearing search does not
restore it or select another token. Search is temporary and stays within the process.
Forms have associated labels, accessible control names, guarded Escape cancellation
and ordinary default buttons. Long details and merge content scroll; focus is not
requested by state updates. These are concrete usability improvements, not a formal
accessibility certification.

Visible active TOTP codes have an explicit **Copy** button, including a separate
button for each active conflict alternative. Copying preserves the displayed digits
exactly. There is no automatic copy, rollover copy, or global Ctrl/Cmd+C override.
Totipo attempts to clear its exact current clipboard payload at code expiry or
after at most 30 seconds, whichever comes first, and when its vault closes or
the application shuts down. It never clears content it cannot positively identify
as its own exact copy.

Once copied to the operating-system clipboard, a code is outside Totipo's exclusive
control. The OS, desktop environment, clipboard managers/history, remote-desktop
systems, accessibility software, or another application may read or retain it.
Clearing is best effort: Totipo can replace only the current clipboard contents
when its exact per-copy marker is still present. It cannot delete copies retained
elsewhere and does not provide secure clipboard erasure. Clipboard unavailability
requires another deliberate Copy click; Totipo never restores previous clipboard data.

The single vault session allows one write workflow at a time for Add, Edit, Delete,
Resolve and publication retry decisions.

VAULT is immutable and create-once. Desktop provides no in-place password change.
Password, KDF or root changes require creating a new vault and a future migration
workflow; migration and cross-vault copy are not implemented.

**Add** / `Ctrl+N` opens Add TOTP: explicitly paste an `otpauth://totp` setup URI
and Review its identity and non-secret setup before publication, or choose Manual
entry and Add directly after validation (Design v0.9). Supported setups use SHA1/SHA256/SHA512, 6–8 digits, and whole periods
from 1 to 4294967295 seconds. Manual Base32 accepts ordinary spaces. Totipo does
not inspect the clipboard for enrollment material. Matching active issuer/account
values require an explicit Update Existing / Add Another / Cancel decision;
multiple matches require a target choice. Update Existing reviews a proposed
setup replacement and preserves the logical TOTP identity. Duplicate Cancel closes
the Add flow and destroys the owned setup: source secret inputs have already been
securely transferred/cleared, so the manual draft is not reconstructed. Design v0.9
permits returning to that draft only where practical.

**Edit TOTP** changes issuer/account and shows a non-secret setup summary.
**Change setup…** acquires and reviews a new complete authenticator setup;
existing secrets are never redisplayed. **Delete TOTP…** opens a separate danger
confirmation and removes the TOTP from the active list; previous versions remain
in vault history. An edit is based on the observed version when it opened; later
concurrent changes are not silently folded into the draft. Editing a conflict
version changes that version and does not resolve its peers. Management does not
derive codes. Lock immediately retires these flows and discards temporary input.
After an acknowledged change, emitted state supplies the updated list. Codes stay
concealed, and a nonmatching search is retained with an explicit Clear Search option.

**Resolve Conflict** first offers one choice per complete semantic version, with
no preselection. Resolve validates on activation and uses Java's whole-Alternative
`keep(...)` operation. **Combine details…** opens the secondary composition path:
agreed text is editable once; disagreements offer existing read-only values and a
custom row. Status is Active / Deleted, and Authenticator Setup is one atomic choice
of secret + algorithm + digits + period. Existing secrets are never displayed.
Back discards detailed choices and temporary secret; Cancel publishes nothing.
New conflict information requires **Review Updated Conflict**, with a fresh
unselected resolver. Uncertain publication uses the frozen retry capability rather
than creating another resolution. Lock retires either screen immediately.

Deleted (TOMBSTONED) is logical deletion: tombstones and immutable history retain
secrets, and storage/synchronization provider copies are not erased.

Creating or opening with an empty password requires a separate explicit confirmation.
Existing empty-password vaults remain readable. Possession of the vault bootstrap
permits offline password guessing; Argon2id raises its cost, not its possibility.
Creation refuses a location with observed plausible Totipo object data and no
usable vault bootstrap. Check synchronization/recovery or choose another folder.
Observed names do not prove authenticity, vault identity or recoverability; there
is no confirmation bypass.

Issuer/account/client text is rendered literally; control/direction characters
and backslashes use visible escapes in browsing and merge choices. Original model
values remain exact for storage and editing, without trimming or normalization.

Publication uncertainty offers **Retry exact publication** or **Stop retrying**.
Stopping releases the retry capability and leaves a persistent warning for that
open session: publication may already have occurred. Starting Create again makes
a distinct token, not a retry. Acknowledged publication and finished local
observation do not mean synchronization, freshness or complete history.
Protocol target: Totipo Vault Format **v1/r19**, through released Totipo Java 0.2.0.
Local configured-store acknowledgement is not remote synchronization or rollback
protection. Provider qualification remains limited; local NIO integration tests do
not establish guarantees for arbitrary filesystems or remote providers.

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
checks while editing; see [qualification policy](AGENTS.md#qualification-ladder).

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
[GUI](qualification/GUI_SMOKE.md), [packaged launch](qualification/PACKAGED_LAUNCH.md),
[clipboard](qualification/CLIPBOARD_SMOKE.md) and
[accessibility](qualification/ACCESSIBILITY_SMOKE.md) procedures. Run the affected
native procedure at the milestone boundary; this driver does not replace it.

The Nix shell supplies the full desktop-capable JDK. Dependency changes are
deliberate review work: use standard Gradle `dependencies --write-locks` and
`--write-verification-metadata sha256` when needed, review generated locks/hashes,
and separately update the Nix dependency cache below. These commands do not
authorize accepting new artifacts or changing pins.

See [ARCHITECTURE.md](ARCHITECTURE.md) for threading, state, and ownership policy,
[TOTIPO_JAVA_DEPENDENCY.md](TOTIPO_JAVA_DEPENDENCY.md) for the released dependency boundary, and
[the M0 report](review/M0_BOOTSTRAP_REPORT.md) for bootstrap evidence, and
[the M1a report](review/M1A_LIFECYCLE_OBSERVATION_REPORT.md) for lifecycle validation.
See also the [M1b report](review/M1B_READ_ONLY_TOKEN_REPORT.md) and
[M2a review report](review/M2A_CREATE_UPDATE_PUBLICATION_REPORT.md) and
[M2b review report](review/M2B_MERGE_RESOLUTION_REPORT.md).
The [S5 conformance report](review/S5_FINAL_CONFORMANCE_REPORT.md) records earlier
vault-state, accessibility and qualification evidence. The current create-only
credential policy above supersedes its password-change guidance and the older
Design v0.8 requirement. Desktop preserves operation-specific failures and
uncertainty; it does not probe storage permissions or fabricate vault-format metadata.
See [the r19 reconciliation report](review/JAVA_0_2_0_R19_DESKTOP_RECONCILIATION_REPORT.md)
for current dependency and automated validation evidence.

## Packaging and qualification (M4a)

No first release has been declared. **Linux is the first desktop qualification
target.** Current release status is **NOT QUALIFIED**; see the exact evidence in
[QUALIFICATION.md](QUALIFICATION.md) and the blocking
[RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md). Build success is not native GUI,
clipboard, accessibility, or filesystem support qualification.

Development uses `gradle run` and focused tests. The owner and CI use
`nix flake check` as the authoritative package-inclusive automatic gate.
No duplicate full online/offline suites are required before it. Separate
filesystem, native GUI/clipboard and actual package launch evidence remain
necessary for claimed environments. See [QUALIFICATION.md](QUALIFICATION.md)
and the release checklist for policy and current evidence.

To materialize the canonical generic distribution for development use (this does
not replace final qualification and need not follow every edit):

```sh
gradle installDist distTar distZip verifyDistribution
build/install/totipo-desktop/bin/totipo-desktop
```

`build/distributions` contains the ZIP and TAR. Both contain generated launchers,
the application JAR, core/storage-nio/Bouncy Castle runtime JARs, and licenses.
They **do not include Java**: install **Java 25 or newer** and set `JAVA_HOME` or
provide `java` on PATH. `verifyDistribution` checks essential installed files,
resolved runtime dependencies, application entry point/icons and launcher policy.
Actual extracted archive launch is a separate check; standard Gradle owns archive
construction. Generated launchers disable JVM Attach
API availability with `-XX:+DisableAttachMechanism`; this does not prevent
same-user process inspection or memory attacks. Development `run` is unchanged.

`VERSION` is the single application-version source. It currently names an
unreleased development state. `gradle validateVersion -PreleaseBuild=true`
intentionally fails until an actual candidate version replaces the sentinel.
Missing, empty, whitespace-bearing, or unsafe filename versions fail configuration.

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
checks. It does not generate generic ZIP/TAR archives. Nix can reuse an already qualified store result; this is not a
forced rebuild. CI uses the pinned Nix environment and this single gate.

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
in the historical r19 reconciliation report; they do not validate this CI change.
Native qualification remains UNQUALIFIED; application
release status remains NOT QUALIFIED. See [dependency provenance](TOTIPO_JAVA_DEPENDENCY.md).

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

Root nixpkgs is locked at `39ad350a0602fa0a58a544344e3e9187526ea45c`.
Its Gradle **9.8.0** is explicitly overridden to use the shared JDK 25 in
development, agent and package declarations. Nix's patched Gradle and official
fetchDeps/setup hook are retained. The dependency cache, locks and verification
metadata remain reviewed inputs.

### Explicit filesystem qualification

Create/select an empty disposable test root on each filesystem to be claimed:

```sh
gradle filesystemQualification -PqualificationRoot=/explicit/disposable/test/root
```

No default root is provided. The harness creates a unique `totipo-qualification-`
child, uses only test credentials, and deletes only that child without following
symlinks. Keep the selected root private and do not use production vault storage.
Cleanup failure prints the exact remaining test directory. Record the non-secret
environment output and checklist results in QUALIFICATION.md; omit the root path.
A pass applies only to that tested environment, not other Linux filesystems,
network shares, cloud synchronization or FUSE configurations.

Updates are manual. M4a adds no updater, native installers, signing, tags or
publication. Future final artifacts get SHA-256 checksums with
the standard `sha256sum -- FILE [FILE ...]` command. See [THIRD_PARTY.md](THIRD_PARTY.md) for the production inventory and
[the M4a report](review/M4A_RELEASE_HARDENING_REPORT.md) for all limitations.
