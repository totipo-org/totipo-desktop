# Desktop qualification ladder milestone

## 1. Starting HEAD

2026-10-10: clean `main` at
`187841b2bd92a8dbcb8530a2d32695891fdbcd47` (Preserve desktop TOTP reveal across
unrelated observations). Initial `git status --short --branch` showed only the
branch line, with no changes. Classification: **DOCS_ONLY**.

Inspected AGENTS.md, QUALIFICATION.md, README.md, package.nix, flake.nix,
.github/workflows/ci.yml, Gradle task wiring, qualification checklists and scripts.
No desktop behavior changes are part of this milestone.

## 2. Existing qualification pattern

The [picker report](DESKTOP_VAULT_PICKER_POLISH_REPORT.md) records a full clean
build/distribution baseline, separate clean build, distribution and forced-offline
test gates, and repeated broad gates around small alignment/gap corrections.
The [reveal report](DESKTOP_REVEAL_OBSERVATION_POLICY_REPORT.md) records normal,
distribution, offline and boundary baselines/finals, with build/distribution checks
repeated after a test refinement. The
[Nix/CI report](DESKTOP_NIX_CI_UNIFICATION_REPORT.md) already established one
package-inclusive human Nix gate, but still used multiple broad Gradle commands.
These reports remain historical evidence and are unchanged.

Current CI runs `nix flake check --print-build-logs path:.` with pinned actions.
`checks.desktop` and `packages.default` reference the same derivation; wrapper
integrity is separate. No workflow or build configuration was edited.

## 3. New classifications

Future agents must record applicable classes in milestone reports:
`DOCS_ONLY`, `UI_PRESENTATION`, `CONTROLLER_LIFECYCLE`, `FILESYSTEM_WATCHER`,
`PRODUCT_MUTATION`, `BUILD_DEPENDENCY`, `PACKAGING_RELEASE`. Overlapping scopes
can carry multiple classes. This milestone is exclusively `DOCS_ONLY`.
Java-operation classification determines WHAT needs testing; the qualification
ladder determines WHEN broad tests run. The exact reviewed guidance pin and all
existing operation-model requirements remain intact.

## 4. Baseline policy

Ordinary production milestones baseline once with
`./gradlew test verifyMavenBoundary verifyJava17Bytecode`. No clean/rerun/offline;
no baseline ZIP/TAR except packaging/distribution work. Prior committed distribution
qualification is preserved. Docs-only work can justify zero Gradle gates.

## 5. Inner-loop policy

Use narrow affected tests, including the documented TokenBrowserTest,
VaultWindowControllerTest and WatcherSessionIntegrationTest examples. Focused Swing
tests and disposable watcher harnesses are available for their respective scopes.
Do not clean, distribute, force offline rebuild, request Nix or repeat native/
filesystem smoke after each edit. Full failure leads to focused reproduction,
focused fixes, focused PASS, then one rerun of the affected full gate.

## 6. Final normal policy

After source/tests stabilize, run once:
`./gradlew test build verifyMavenBoundary verifyJava17Bytecode`.
Clean requires a concrete stale-output investigation. Existing task dependencies
remain: `build` reaches `check`, which includes installed-distribution verification
and qualification harness compilation. The ladder changes timing, not task wiring.

## 7. Final strict/distribution policy

Production/build/package changes receive one combined final fresh gate, reproduced
exactly in AGENTS.md: offline, no daemon, no build cache, rerun tasks, clean, test,
build, installDist, distTar, distZip, distribution/archive verification, Maven
boundary and Java 17 bytecode verification. Do not separately rerun its components
unless relevant inputs change. This docs-only milestone did not invalidate it.

## 8. Specialized qualification policy

After stability, run only affected specialized qualification once: disposable
watcher/filesystem checks, focused native GUI smoke, clipboard qualification or
relevant accessibility/native qualification. Historical harnesses are not a
universal milestone matrix. Native/platform limitations and UNQUALIFIED statuses
remain unchanged.

## 9. Invalidation matrix

| Change after final qualification | Response |
| --- | --- |
| Production/test/build after strict gate | Rerun applicable Gradle gate |
| GUI smoke checklist/report text only | No Gradle rerun unless packaged/checked |
| Watcher harness only | Rerun harness; inspect Nix source inclusion |
| Distribution verifier | Verify final outputs again; rebuild only for a real packaging problem |
| Any Nix qualification source input after Nix | Rerun human Nix |

## 10. Nix-last rule

Freeze production, tests, build/package configuration and qualification tooling
included by the flake source, then request exactly `nix flake check path:.` when
required. Agents never run Nix; ordinary `nix build` is not an extra gate.
`nix build --rebuild` remains deliberate release/reproducibility work.

## 11. Execution-count reporting

Every substantial report records attempts, failures and invalidation reruns, with
human-reported and agent evidence distinguished. Ordinary production clean target:
baseline normal 1, final normal 1, final strict/distribution 1, human Nix 1.

Actual counts for this documentation-only milestone:

| Category | Runs / disposition |
| --- | --- |
| Baseline normal gate | 0; docs-only profile |
| Focused tests | 0; no affected executable inputs |
| Final normal gates | 0; docs-only profile |
| Final strict/distribution gates | 0; existing distribution proof not invalidated |
| Specialized filesystem runs | 0; unaffected |
| Specialized native GUI runs | 0; unaffected |
| Clipboard/accessibility runs | 0; unaffected |
| Agent Nix runs | 0; prohibited |
| Human Nix runs | 0 requested/reported for this milestone; not required |

Static inspection and whitespace checks are documented below. No passing build
or native result is claimed for work not executed.

## 12. Evidence-preservation rationale

Focused iteration limits repeated broad work while complete boundary qualification
still proves final production inputs. Input-specific invalidation avoids both
unnecessary rebuilds and claiming stale PASS evidence. Historical reports retain
their commands, failures, environments and limitations; they are not rewritten
to match the new process. Release status remains NOT QUALIFIED.

## 13. Documentation changed

- AGENTS.md: durable ladder, classification, gates, invalidation and counts;
  existing Java-operation instructions retained verbatim.
- QUALIFICATION.md: current process separated from historical evidence and release
  reproducibility checks.
- README.md: matching development/final/release guidance; generic materialization
  command no longer prescribes clean as a routine step.
- This report: scope, evidence, counts and Nix disposition.

## 14. Validation

PASS: `git diff --check`; empty-index check; changed-path allowlist; unchanged
source/test/build/package/dependency/CI inputs; exact preservation of the AGENTS.md
Java-operation section; local Markdown file-target checks for changed documents;
balanced fenced code blocks and required class/command consistency inspection.
No dedicated documentation lint/check task was found in Gradle, scripts or CI.
Static checks used the existing shell/Git tools; no tools/dependencies were added.

The full distribution gate was not invalidated: neither Gradle inputs nor packaged
content changed. Gradle distribution notices are LICENSE, THIRD_PARTY.md, VERSION
and packaging/licenses; the changed documentation is not consumed by build tasks.

## 15. Human Nix disposition

**Not required for these paths; no request made.** package.nix selects only `src`,
`gradle`, `packaging/licenses`, `packaging/icons`, build/settings/properties/locks,
VERSION, LICENSE and THIRD_PARTY.md, and explicitly excludes `review`.
AGENTS.md, QUALIFICATION.md and README.md are outside that selected source.
flake.nix additionally references package.nix, package-deps.json and wrapper
integrity inputs; none changed. Although `path:.` sees the checkout, these edits
do not change consumed qualification inputs. The new docs-only policy does not
require Nix for excluded documentation. No Nix evaluation/build was performed.

## 16. Final Git state

HEAD remains `187841b2bd92a8dbcb8530a2d32695891fdbcd47` on `main`. Index empty;
changes left unstaged/uncommitted. No staging, commit, push or remote CI action.
Only the three intended documentation files and this new report changed:

```text
 M AGENTS.md
 M QUALIFICATION.md
 M README.md
?? review/DESKTOP_QUALIFICATION_LADDER_REPORT.md
```
