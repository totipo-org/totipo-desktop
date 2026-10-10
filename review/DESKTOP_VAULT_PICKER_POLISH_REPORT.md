# Desktop vault picker polish

Status: implementation and all agent Gradle validation complete.
Human Nix gate and functional GUI smoke passed on the initial implementation
(human-reported). A subsequent locked-button alignment correction passed all
agent validation, the human-reported Nix gate and final native layout confirmation.
All changes are unstaged/uncommitted; no Nix command has been run by the agent.

## Starting point and baseline

Before editing, the requested commands returned:

- `git branch --show-current`: `main`.
- `git rev-parse HEAD`: `f960aa0dec45d887b04156b66a0b22f30247a404`.
- `git status --short`: empty.

HEAD is the committed “Unify desktop Nix qualification and CI” milestone.
No applicable AGENTS.md was found. The actual ShellPanel, ShellFrame,
VaultDirectoryChooser, DirectoryPicker, DesktopStyle, SwingUsability,
DesktopApplication creation/selection flow and relevant tests were inspected.

The pre-edit baseline command passed (56s):

```sh
./gradlew clean test build installDist distTar distZip verifyDistribution verifyDistributionArchives verifyMavenBoundary verifyJava17Bytecode
```

All 15 tasks executed. This included current desktop tests/build, installed
distribution and both archive verifications, Maven boundary, Java 17 bytecode
and qualification harness compilation. A fontconfig diagnostic appeared during
headless tests; the build succeeded.

## Locked surface

- Open fills the first action row beneath the password field and retains the
  primary/green style and default-button behavior.
- Change Vault… and New Vault… fill the next row as two equal-width neutral
  secondary siblings, with a sixteen-unit gap between rows (increased from
  eight at the operator's request).
  Both use the normal noncompact SecondaryAction helper, matching neutral fill,
  border, font, padding and height, with an eight-unit horizontal gap.
  The two rows fill the same width as the password field, within the normal
  560-unit bounded form. Grid layouts enforce full-width Open and equal-width
  secondary buttons without manual per-button size overrides.
- The action text is exactly `New Vault…`; the welcome action and landing menu
  use the same spelling for consistency.
- Locked actions explicitly name themselves for accessibility and expose
  O/C/N mnemonics for Open/Change Vault/New Vault. The New Vault menu also
  explicitly supplies its name and N mnemonic.

## Shared directory browser

ShellFrame now routes both choices to DirectoryPicker with explicit
EXISTING_VAULT and NEW_VAULT modes. VaultDirectoryChooser and its Swing-specific
tests were removed. No production Java source references JFileChooser.

Existing mode retains directory-only navigation, hidden-folder filtering,
Enter/double-click navigation, Up, selection and cancellation. Recognition
continues in the unchanged DesktopApplication/VaultTarget layer: an ordinary
folder cannot replace the selected vault, while recognizable unsupported or
truncated bootstraps remain selectable. The picker does not validate protocol
contents or diagnose vault validity.

New mode uses the same browser and can select an ordinary existing empty
directory. Its inline Folder name / New Folder controls explicitly create one
child directory, then enter it so Select Folder returns that Path. Selecting a
folder returns only a Path to the unchanged password/create flow.

Folder creation uses `Files.createDirectory` on the picker worker, never
`createDirectories`. Names must identify one child; absolute paths, path
separators, dot/parent names, invalid paths and the reserved `vault`/`objects-v1`
names are rejected. The UI never writes protocol files. Collisions and
IOException/SecurityException failures produce fixed product text with no raw
exception details. Navigation and selection are disabled during creation;
Cancel remains available. Closing retires pending work and rejects late UI
results. A queued write that has not started is skipped after retirement.

Typing, browsing and cancelling create nothing. Explicitly creating a folder
is a filesystem operation: an already completed or in-flight creation can leave
that ordinary empty folder if the user subsequently cancels. It is not removed
automatically, and it does not change remembered-vault preferences.

## Creation and remembered-location safety

DesktopApplication, VaultTarget, NioVaultAccess and preferences implementation
are unchanged. Actual vault creation still goes through the released Java
NioTotipo/create boundary. Empty-password confirmation, AlreadyExists refusal,
OBJECT_DATA_OBSERVED refusal and creation-uncertainty guidance remain intact.

New application regression tests start with locked, remembered A and establish:

- Picker cancellation, destination-error cancellation, password cancellation
  and declining empty-password creation retain selected/persisted A with zero
  preference writes and no Java create call.
- Java Failed, AlreadyExists, Uncertain and OBJECT_DATA_OBSERVED outcomes retain
  locked A, with one create call and no automatic retry or preference write.
- A successful Created session for B switches to unlocked B and persists it
  once, only after Java returns that session.

Destination errors are covered in the picker separately from application
cancellation; the application test models dismissing the picker after an error.
Permission failures use injected AccessDeniedException/SecurityException so
coverage does not depend on the test runner's filesystem privileges. Existing
real-NIO tests still exercise Java create/reopen, refusal to overwrite a vault
and OBJECT_DATA_OBSERVED with intact object bytes and no new bootstrap.

## Automated coverage and validation

Tests cover locked action roles/names/mnemonics and secondary alignment at
normal and large fonts; normal and narrow shell sizing; switching shell states;
both picker modes; empty ordinary destinations; explicit child creation and
selection; name/path rejection; collision and permission messages; no protocol
files; Enter, arrows, Up, Escape/Cancel; retirement; stale initial locations;
and the absence of JFileChooser from production sources. Existing recognition,
GUI component and accessibility tests remain in the suite.

An intermediate test run identified the old assertion requiring Open and Change
Vault to form an equal-width pair. It now checks full-width Open on the first
row and two equal-width secondary buttons on the next, matching heights,
natural secondary styling and row/column gaps, at 14/28-point fonts and
640/760/900-unit window widths. Ordinary/narrow sizing tests also assert the
same geometry. Large-font checks require each button to fit its natural label.

| Command | Result |
| --- | --- |
| `./gradlew clean test build` | PASS, 31s; 446 tests in 67 suites, zero failures/errors/skips |
| `./gradlew installDist distTar distZip verifyDistribution verifyDistributionArchives` | PASS; exact runtime inventory and both archives verified |
| `./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean test` | PASS, 39s; all six tasks executed; 446 tests in 67 suites, zero failures/errors/skips |
| `./gradlew verifyMavenBoundary verifyJava17Bytecode` | PASS |
| `git diff --check` | PASS; repeated before handoff |

The offline run explicitly disables task/build-cache reuse and forces execution.
No claim of native GUI or assistive-technology qualification follows from the
headless tests.

### Alignment correction validation

After the operator clarified the intended two-row geometry, the focused
ShellPanel/PostS5Polish tests passed (26 tests). The complete combined gate
below passed again in 35s (15 tasks, 12 executed and three from cache), including
tests, build, distribution/archive verification and both boundary checks:

```sh
./gradlew clean test build installDist distTar distZip verifyDistribution verifyDistributionArchives verifyMavenBoundary verifyJava17Bytecode
```

A fresh forced offline clean test passed for the revised layout in 41s:
`./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean test`.
All six tasks executed; 446 tests in 67 suites passed with zero failures,
errors or skips. `git diff --check` and the empty-index/input-invariance checks
also passed again after this correction.
The picker and application flow have not changed during the alignment correction.

The operator subsequently requested more space between Open and the secondary
row. That vertical gap is now `DesktopStyle.NORMAL` (16 units); the horizontal
gap remains `DesktopStyle.TIGHT` (8 units). The 26 focused layout tests passed
again, including normal/enlarged fonts and supported window widths. The final
combined forced-offline clean build/test/distribution/boundary run passed in
46s, with all 15 tasks executed, 446 tests in 67 suites and zero failures/errors/
skips:

```sh
./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean test build installDist distTar distZip verifyDistribution verifyDistributionArchives verifyMavenBoundary verifyJava17Bytecode
```

`git diff --check`, empty-index and dependency/package/VERSION invariance checks
passed again. Revised-source human Nix confirmation passed (human-reported);
final layout confirmation also passed (human-reported).

## Scope invariants

No dependency, Gradle, packaging, package-deps.json, flake.lock, package.nix or
VERSION changes. VERSION remains `0.0.0-dev`. Totipo Java, protocol semantics,
creation rules, preferences semantics, automatic refresh, token UI and Android
are unchanged. The only existing token-layout test edit updates its New Vault
label assertion. Historical reports are unchanged.

The verified runtime inventory remains:

```text
bcprov-jdk18on-1.86.jar
totipo-core-0.2.0.jar
totipo-desktop-0.0.0-dev.jar
totipo-storage-nio-0.2.0.jar
```

## Human gates

Nix: PASS (human-reported). The operator replied “nix flake check passed”
after being asked to run only the established package-inclusive gate:

```sh
nix flake check path:.
```

No terminal logs or fresh-build/cache-reuse details were supplied; none are
inferred. The agent did not run Nix. The initial PASS preceded the alignment
correction. After the full-width Open, equal-width secondary buttons and final
16-unit vertical gap were implemented and validated, the operator again replied
“nix flake check passed”. This second human-reported PASS applies to the final
revised source; no additional Nix command was requested or run by the agent.

Focused native GUI smoke requested after that result:

1. Locked screen shows Open as primary and Change Vault… / New Vault… as aligned
   neutral secondary actions.
2. New Vault… opens the Totipo-owned picker, with no JFileChooser.
3. Create a disposable directory through it and successfully create a vault.
4. Cancel a second attempt and confirm the remembered vault stays unchanged.

GUI smoke: PASS (human-reported) for functionality. The operator replied “All
these pass” but supplied alignment-wrong.png showing Open isolated above the
two secondary actions. The operator clarified the intended layout: a big Open
button on the first line and two equal-width vault controls on the next.
The revised layout implements that geometry as described above.
Only revised button placement needs native visual confirmation;
the picker/create/cancellation implementation has not changed since that smoke.

Revised-source human Nix confirmation: PASS (human-reported).
Final native layout confirmation: PASS (human-reported). After the final Nix
PASS, the operator confirmed “the layout also change appropriately”. Together
with the earlier functional smoke, this completes the focused milestone gates.
No broader native GUI/accessibility or release qualification is inferred.
No remote CI, staging, commit, push, tag or release action has been performed.
HEAD remains the starting commit on main, with an empty index. The supplied
alignment-wrong.png is an untracked user-provided reference, not a generated
deliverable; it has not been modified.
