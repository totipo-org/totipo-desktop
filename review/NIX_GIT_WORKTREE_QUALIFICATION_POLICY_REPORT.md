# Git-aware Nix worktree qualification policy

## Repository and starting state

2026-10-11, isolated checkout `/home/niki/Sources/totipo-desktop`.
Remote: `git@github.com:totipo-org/totipo-desktop.git`; README, Java/Swing
source layout and package metadata identify Totipo Desktop. No sibling repository
was inspected or changed.

Starting branch `main`, HEAD `2c962498dad02bfbfc47634bec217dea61c28874`.
The required first commands were `git branch --show-current`, `git rev-parse HEAD`,
`git status --short`, and `git diff --cached --quiet`: clean status, no staged
contents, no untracked files. HEAD remains unchanged; all work remains uncommitted.

Classification: **BUILD_TOOLING / QUALIFICATION_POLICY** as requested;
the existing ladder's nearest class is **BUILD_DEPENDENCY** (qualification
invocation/tooling), with documentation scope. No effective Gradle/package input
changes. This is not a production or dependency migration milestone.

## Policy and command changes

Authoritative AGENTS.md and QUALIFICATION.md now require Git-aware local
pre-commit qualification. README, RELEASE_CHECKLIST.md and
qualification/PACKAGED_LAUNCH.md agree.

- `nix flake check path:.` becomes `nix flake check`.
- CI's `nix flake check --print-build-logs path:.` becomes
  `nix flake check --print-build-logs`.
- Materialization `nix build path:.` becomes `nix build .`.
- Release-only `nix build --rebuild path:.` becomes `nix build --rebuild .`;
  its release-specific purpose and evidence limits are unchanged.
- README's `nix run path:.#update-package-deps` and direct
  `path:.#packages.x86_64-linux.default.mitmCache.updateScript` build example
  become their `.#` forms, preserving app/attribute names, arguments and command count.
- Removed README's advice to use raw paths to include untracked review files.

Existing modified tracked files remain unstaged. For each new intended milestone
file, agents may use `git add --intent-to-add <file>`. This establishes Git source
membership only. Actual content staging requires explicit user authorization;
`git diff --cached --quiet` must pass. Intent-to-add entries remain for human review.
Ignored/local cache, build and secret state must remain outside Git membership.
Explicit raw/non-Git paths remain allowed only when intentional.

.envrc already uses `use flake`; bootstrap-m0.sh already uses `nix develop`.
Both remain unchanged, as do dev-shell contents and versions. There is no Makefile,
release workflow, release-controller app or command-string test requiring changes.
The existing update-package-deps app implementation is unchanged.

## Complete starting path occurrence inventory

Repository-wide hidden-file search covered `path:`, `nix flake check`,
`nix build`, `nix run`, `nix develop`, `nix eval` and `nix flake metadata`,
excluding Git internals/local build caches. A tracked `git grep -n 'path:' HEAD -- .`
inventory below records every matching starting line; multiple references on a
line share its classification. Line numbers refer to the starting HEAD.
Historical evidence is classified C (example/evidence), not active instructions.

| File (starting HEAD) | Lines containing `path:` | Classification / disposition |
| --- | --- | --- |
| `.github/workflows/ci.yml` | 29 | A — CURRENT_GIT_WORKTREE. Converted current-checkout command/policy to Git-aware form. |
| `AGENTS.md` | 53, 174 | A — CURRENT_GIT_WORKTREE. Converted current-checkout command/policy to Git-aware form. |
| `QUALIFICATION.md` | 53, 59, 60 | A — CURRENT_GIT_WORKTREE. Converted current-checkout command/policy to Git-aware form. |
| `QUALIFICATION.md` | 67, 89, 90 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `README.md` | 116 | Not a flake reference. Prose/diagnostic/classpath text; unchanged. |
| `README.md` | 280, 290, 294, 300, 320, 330, 335, 339 | A — CURRENT_GIT_WORKTREE. Converted current-checkout command/policy to Git-aware form. |
| `RELEASE_CHECKLIST.md` | 28, 31, 32 | A — CURRENT_GIT_WORKTREE. Converted current-checkout command/policy to Git-aware form. |
| `build.gradle.kts` | 212 | Not a flake reference. Prose/diagnostic/classpath text; unchanged. |
| `package.nix` | 60 | Not a flake reference. Nix lambda argument syntax; unchanged. |
| `qualification/PACKAGED_LAUNCH.md` | 19, 20, 41 | A — CURRENT_GIT_WORKTREE. Converted current-checkout command/policy to Git-aware form. |
| `review/D1_MAVEN_DEPENDENCY_MIGRATION_REPORT.md` | 251, 252, 253, 280, 285, 286, 287, 290 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/DESIGN_V0_9_MANUAL_ADD_DESKTOP_REPORT.md` | 122 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/DESKTOP_GITHUB_ACTIONS_REFRESH_REPORT.md` | 100 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/DESKTOP_GITHUB_ACTIONS_REFRESH_REPORT.md` | 169 | Not a flake reference. Prose/diagnostic/classpath text; unchanged. |
| `review/DESKTOP_JAVA_OPERATION_MODEL_AUDIT.md` | 597 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/DESKTOP_NIX_CI_UNIFICATION_REPORT.md` | 72, 132, 137, 141, 223, 278 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/DESKTOP_QUALIFICATION_LADDER_REPORT.md` | 26, 93, 158 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/DESKTOP_REFRESH_CREATE_LAYOUT_POLISH_REPORT.md` | 165 | Not a flake reference. Prose/diagnostic/classpath text; unchanged. |
| `review/DESKTOP_REFRESH_CREATE_LAYOUT_POLISH_REPORT.md` | 297, 298, 299 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/DESKTOP_REVEAL_OBSERVATION_POLICY_REPORT.md` | 232 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/DESKTOP_SUPPORT_TOOLING_AUDIT.md` | 74, 319 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/DESKTOP_SUPPORT_TOOLING_WAVE2_REPORT.md` | 111 | Not a flake reference. Prose/diagnostic/classpath text; unchanged. |
| `review/DESKTOP_SUPPORT_TOOLING_WAVE2_REPORT.md` | 156, 159 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/DESKTOP_VAULT_PICKER_POLISH_REPORT.md` | 193 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/JAVA_0_2_0_R19_DESKTOP_RECONCILIATION_REPORT.md` | 145, 339, 340, 341, 346, 347, 348 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/M3A_PASSWORD_CHANGE_REPORT.md` | 110 | Not a flake reference. Prose/diagnostic/classpath text; unchanged. |
| `review/M3C_TOTP_CLIPBOARD_REPORT.md` | 80 | Not a flake reference. Prose/diagnostic/classpath text; unchanged. |
| `review/M4A_RELEASE_HARDENING_REPORT.md` | 210, 409 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/M4B_TOTIPO_JAVA_0_1_1_R18_REPIN_REPORT.md` | 141, 146 | Not a flake reference. Prose/diagnostic/classpath text; unchanged. |
| `review/M4B_TOTIPO_JAVA_0_1_1_R18_REPIN_REPORT.md` | 222, 259, 260, 261, 264 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/POST_S5_VISUAL_POLISH_REPORT.md` | 83, 177, 267, 471, 752, 953 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/POST_S5_VISUAL_POLISH_REPORT.md` | 147 | Not a flake reference. Prose/diagnostic/classpath text; unchanged. |
| `review/S1_DESKTOP_VISUAL_FOUNDATION_REPORT.md` | 132, 253 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/S2_2_SHELL_COLLECTION_REPORT.md` | 180, 181 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/S2_PERSISTENT_SHELL_LOCKING_REPORT.md` | 176, 177 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/S4_CONFLICT_RESOLUTION_REPORT.md` | 222, 223, 225, 229 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/S5_FINAL_CONFORMANCE_REPORT.md` | 458 | C — historical example/evidence. Retained verbatim to preserve prior commands, results and limitations; superseded by current policy. |
| `review/S5_FINAL_CONFORMANCE_REPORT.md` | 477 | Not a flake reference. Prose/diagnostic/classpath text; unchanged. |

No B (INTENTIONAL_PATH_SOURCE) or D (UNKNOWN) source occurrences were found.
No intentional raw source was changed. Newly added literal `path:` mentions in
AGENTS/QUALIFICATION/README explain the allowed raw-source exception; this report
quotes old forms as inventory/evidence (C). Existing review reports and
QUALIFICATION.md's historical results remain verbatim, including obsolete
untracked-source advice within those historical reports.

## Workflow review and non-change boundary

The only workflow is .github/workflows/ci.yml. Before its Nix gate, checkout
provides tracked source, the offline pin guard only reads workflows, and the
existing installer configures Nix outside repository source. No preceding step
creates an untracked flake source input. Only the gate's source argument changed.
Action pins, permissions, events, installer configuration, network behavior and
step/command counts are preserved. No workflow dispatch, push or remote CI run
was performed; remote behavior awaits a later reviewed commit/push.

Java/product source and tests, Swing behavior, branding/assets, Gradle/build inputs,
distribution contents/semantics, dependency versions/locks/verification/cache,
flake.nix and package.nix are unchanged. No application-operation code changed;
no Java guidance fetch or other repository inspection was necessary.

## Non-Nix validation and execution counts

- Shell syntax: `sh -n .github/scripts/verify-workflow-actions.sh .github/scripts/test-workflow-actions.sh`:
  **1 run, PASS**.
- `sh .github/scripts/verify-workflow-actions.sh`: **1 direct run, PASS**.
- `sh .github/scripts/test-workflow-actions.sh`: **1 run, PASS**, including
  its positive guard invocation and all 15 rejection fixtures.
- Static workflow inspection: only the final command argument changed; existing
  offline action-pin/security guard passed. actionlint is unavailable locally;
  no new tool/dependency or network download was introduced.
- `git diff --check`: **2 runs before the human gate, PASS**.
- Static command/policy consistency, complete source-reference inventory,
  changed-path allowlist and immutable-input comparison: **PASS**.
- `git diff --exit-code HEAD --` for src, Gradle inputs, packaging, flake/lock,
  package/cache, .envrc, bootstrap, scripts and workflow guards: **PASS**.
- An initial edit helper attempt failed because python3 is absent; it made no
  edits. Changes used the available patch tool; no dependency was installed.

Baseline Gradle **0**, focused Gradle **0**, final normal Gradle **0**, final
strict/distribution Gradle **0**, specialized filesystem/GUI/clipboard/accessibility
**0**, agent Nix **0**. No failures of executed validation gates.
The full distribution proof is not invalidated: changed docs/workflow are not
Gradle build inputs or packaged contents. No GUI/manual physical rerun applies.

## Source membership, freeze and human Nix

The only new intended milestone file is
`review/NIX_GIT_WORKTREE_QUALIFICATION_POLICY_REPORT.md`.
It is added with intent-to-add before requesting Nix; no other new files are
intended. No ignored cache/output/secret path is intent-added. Git membership
checks for .gradle, .direnv, .qualification, build and result return no tracked paths.

package.nix's existing selected trees are src, gradle, packaging/licenses and
packaging/icons, plus its explicit build/license/version files; review is explicitly
excluded. flake.nix additionally reads package.nix, package-deps.json and wrapper
integrity inputs. It does not reference this report, workflow or changed guidance.
No source-copy/filter mechanism was added or altered. The report is Git-known
for the initial flake source but excluded from effective package/wrapper checks.

All effective inputs are frozen before the human gate. The excluded report may
be finalized afterward solely to record human evidence and final Git state;
no included effective input may change after PASS.

Human gate from this repository root: `nix flake check`.
**PASS — human-reported**, 2026-10-11: the operator replied “flake check passed”
to the requested gate. Human Nix runs: **1 reported**. Logs and detailed operator
environment were not supplied; no store reuse/fresh execution claim is inferred.
Agent Nix runs remain **0**. No additional build or reproducibility gate was requested.

## Final Git/index state and commit suggestion

Final state: branch `main`, starting HEAD unchanged; six modified tracked files
and this one new report with intent-to-add. `git diff --cached --quiet` passes;
`git diff --cached --name-only` is empty. The worktree diff names are:

```text
.github/workflows/ci.yml
AGENTS.md
QUALIFICATION.md
README.md
RELEASE_CHECKLIST.md
qualification/PACKAGED_LAUNCH.md
review/NIX_GIT_WORKTREE_QUALIFICATION_POLICY_REPORT.md
```

Post-human immutable-input comparison and index verification passed. Only this
excluded report was edited after the reported PASS; no effective input changed.
Final `git diff --check` passed (third total run, including this post-human check).
Intent-to-add remains in place. No actual content staging, commit, tag, push or
release was performed. Remote CI remains unperformed. Milestone complete.

Suggested commit subject: **Adopt Git-aware Nix worktree qualification**.
