# Desktop GitHub Actions maintenance review

Review date: 2026-10-10. Classification: **BUILD_DEPENDENCY**.
No packaging/release workflow exists or was changed, so PACKAGING_RELEASE does
not apply. No product, UI, controller, watcher or mutation scope is involved.

## Starting state and scope

Before editing, `git branch --show-current`, `git rev-parse HEAD`, and
`git status --short` established clean `main` at
`f85aa2cf77d84210ac72a4a02d0f2c1c305a43dd` (empty status output).
Read AGENTS.md, QUALIFICATION.md, README.md, all workflow YAML, flake.nix and
package.nix. Inspection found exactly one workflow, `.github/workflows/ci.yml`,
one job (`build`), and the two external action uses below. No reusable workflows,
local actions, Docker actions, release workflows or artifact actions exist.

**Both existing pins already resolve to upstream's latest stable releases.**
Retain them rather than inventing a version upgrade. This milestone independently
verifies those pins and adds an offline shell review guard and disposable tests.
No Android checkout or Android pin was used as evidence.

## Complete action inventory and old/new pins

Both rows belong to `.github/workflows/ci.yml`, job `build`.

| Action / step | Old release and exact SHA | Reviewed final release and exact SHA | Type / old → final runtime | Maintenance |
| --- | --- | --- | --- | --- |
| `actions/checkout` / Checkout | v7.0.1 / `3d3c42e5aac5ba805825da76410c181273ba90b1` | v7.0.1 / `3d3c42e5aac5ba805825da76410c181273ba90b1` | JavaScript / Node 24 → Node 24, main and post | Canonical GitHub project; latest stable published 2026-07-20; not archived or disabled. Security updates and major breaking fixes maintained; general contributions paused. |
| `cachix/install-nix-action` / Install Nix | v31.11.1 / `13d8dd58da0234aa297dedd986986ccb8e7f3e24` | v31.11.1 / `13d8dd58da0234aa297dedd986986ccb8e7f3e24` | Composite / Bash → Bash, no JS runtime | Canonical Cachix project; latest stable published 2026-08-13; not archived or disabled. |

For each project independently, read GitHub's `/repos/OWNER/REPO/releases/latest`
response (neither draft nor prerelease), release notes, pinned README and action
metadata. `git ls-remote` for both the exact release tag and its peeled `^{}` ref
returned the listed 40-hex commit (lightweight tags, no separate peeled result).
Release `target_commitish` was not used as the pin. No floating workflow refs.

Evidence:

- [Checkout latest release API](https://api.github.com/repos/actions/checkout/releases/latest),
  [v7.0.1 notes](https://github.com/actions/checkout/releases/tag/v7.0.1),
  [exact metadata](https://github.com/actions/checkout/blob/3d3c42e5aac5ba805825da76410c181273ba90b1/action.yml),
  [exact README](https://github.com/actions/checkout/blob/3d3c42e5aac5ba805825da76410c181273ba90b1/README.md).
- [Installer latest release API](https://api.github.com/repos/cachix/install-nix-action/releases/latest),
  [v31.11.1 notes](https://github.com/cachix/install-nix-action/releases/tag/v31.11.1),
  [exact metadata](https://github.com/cachix/install-nix-action/blob/13d8dd58da0234aa297dedd986986ccb8e7f3e24/action.yml),
  [exact script](https://github.com/cachix/install-nix-action/blob/13d8dd58da0234aa297dedd986986ccb8e7f3e24/install-nix.sh),
  [exact README](https://github.com/cachix/install-nix-action/blob/13d8dd58da0234aa297dedd986986ccb8e7f3e24/README.md).

## Release notes, runtime and checkout behavior

Checkout v7.0.1 fixes the default-input unsafe-PR check, ASCII branch-whitespace
handling and escaping values passed to Git `--unset`, plus dependencies. Also read
[v7.0.0 notes](https://github.com/actions/checkout/releases/tag/v7.0.0) and the
pinned README's v5/v6/v7 migration notes: Node 24 requires runner >=2.327.1;
v6 moved persisted credentials to RUNNER_TEMP (authenticated Docker-container
commands require runner >=2.329.0); v7 migrated to ESM and blocks unsafe fork PR
checkout under `pull_request_target`/`workflow_run` unless explicitly allowed.

Desktop retains `ubuntu-latest`, `push`/`pull_request` and
`persist-credentials: false`; it does not enable unsafe checkout. No custom ref,
history depth, tags, submodules or LFS existed. Default event ref/SHA and depth 1
remain appropriate: package version comes from VERSION and checks need the source
tree, not Git history. No authenticated container commands are used. Hosted-runner
compatibility is reviewed statically; no remote execution is claimed.

Installer v31.11.1 advances Nix 2.35.1 to 2.35.2 to fix a build-aborting assertion
crash. The exact script confirms `nix_version=2.35.2`. No version/input changes
are needed in this workflow.

Checkout is the only JavaScript action and its main/post both declare `node24`.
The [Node release schedule](https://github.com/nodejs/Release/blob/main/schedule.json)
places Node 24 in active LTS on the review date, maintenance beginning 2026-10-20,
end of life 2028-04-30. There are **zero Node-20 JavaScript actions** and no warning
suppression or runtime-forcing environment variables.

## Third-party trust and permissions

The installer's exact `action.yml` contains one Bash `run` of its colocated
`install-nix.sh`, no nested `uses`, JS bundle or Docker image. Read the entire
script. Existing behavior includes sudo/system configuration, optional KVM setup,
trusted runner user, GitHub token in Nix access-token configuration, flakes and
nix-command, and always-allow-substitutes. It downloads and executes the official
versioned releases.nixos.org installer over HTTPS, with retries; the action does
not independently pin that downloaded script by digest. These are retained trust
boundaries, not a new dependency or strengthened reproducibility claim. Its
existing daemon-user-count calculation invokes system Python; no Python code or
Python validation mechanism was introduced into this repository.

No Cachix cache-upload action, new vendor, secret, elevated GitHub permission or
replacement project is introduced. Workflow permission remains `contents: read`,
all other token permissions implicitly disabled. Installer token use and sudo
behavior are unchanged. The new guard runs locally after checkout, before the
installer, and has no network or token requirements. No stop condition arose.

## CI architecture and artifact/package boundaries

The authoritative package gate remains exactly:

```sh
nix flake check --print-build-logs path:.
```

The new shell step is a static action-policy check, not a second package build
authority. No direct Gradle CI authority or routine `nix build` was added.
Explicit `accept-flake-config = true` remains. flake.nix is unchanged:
`checks.desktop` and `packages.default` both reference `desktop`; its separate
wrapper check still verifies the wrapper JAR and distribution properties.

There are no artifact upload/download actions or release transport steps.
Archive roots/layout, hidden files, overwrite, retention, download directories
and artifact digest verification are therefore not applicable to action changes.
No `result`, build cache or unrelated tree is transported. Generic distribution
contents and package installation/archive verifiers are unchanged. Local `result`
was already present and was not followed, packaged or modified.

## Static guard and focused validation

Prior workflow verification was documented static inspection in the Nix CI
unification report; no executable workflow guard was found. Added
`.github/scripts/verify-workflow-actions.sh` using the repository's existing
shell tooling style. It scans every `.yml`/`.yaml` under the workflow directory,
accepts only the exact reviewed SHA/release strings and associates each with its
reviewed runtime in the allowlist. Unknown external, Docker and local actions
require review. It rejects alternate YAML key/flow/anchor spellings rather than
attempting to resolve them. This intentionally conservative line guard is not a
general YAML parser, a live release updater or a substitute for upstream review.
Future syntax changes may require extending it. Runtime metadata is established
by immutable-SHA review, not by trusting workflow comments or fetching at CI time.

`.github/scripts/test-workflow-actions.sh` uses a disposable directory. Positive
current workflow plus 15 negative fixtures cover floating tags, altered SHA,
wrong release comment, old Node-20 checkout, unknown action, Docker/local uses,
flow mappings, quoted/escaped keys, anchors/merges and missing/multiline values.
A second `.yaml` workflow is scanned alongside the current `.yml` fixture.

Actual focused commands and counts:

| Command/check | Runs | Result |
| --- | ---: | --- |
| `sh -n .github/scripts/verify-workflow-actions.sh .github/scripts/test-workflow-actions.sh` | 3 | All PASS |
| `sh .github/scripts/verify-workflow-actions.sh` | 3 | First failed; final two PASS |
| `sh .github/scripts/test-workflow-actions.sh` | 3 | First two failed; final PASS, including all 15 negatives |
| `/tmp/totipo-actionlint-1.7.12/actionlint .github/workflows/ci.yml` | 1 | PASS |
| `git diff --check` | 3 | All PASS, including final report |

First failure: find interpreted literal `{}` inside the awk regex as another
placeholder with `-exec ... +`; corrected to octal brace escapes. Second failure:
a blank extra workflow fixture was wrongly expected to fail despite adding no
action; replaced with a missing-value `uses:` fixture. No broad gate was run to
diagnose these focused failures.

Validation environment: Linux x86_64, system sh/awk/find. actionlint v1.7.12 was
downloaded from its canonical rhysd/actionlint release into /tmp, verified against
that release's SHA-256 checksum list, and run once. It is not a workflow action,
repository dependency or package input. No native smoke testing applies.

## Qualification counts and Nix disposition

Baseline Gradle gates: **0**. Final normal Gradle gates: **0**. Final strict/
distribution Gradle gates: **0**. Filesystem/GUI/clipboard/accessibility runs:
**0**, unaffected. Agent Nix: **0**. Human Nix for this milestone: **0**, not
requested. Focused checks and failures are counted above; no remote CI run.

This is BUILD_DEPENDENCY maintenance of excluded CI tooling, with no effective
desktop build/package/check input change. package.nix selects only src, gradle,
packaging/licenses, packaging/icons and its explicit build/license/version file
list. `.github/**` is not selected and `review` is explicitly excluded. The new
shell guard is also outside that source, unlike a guard added under src/test or
build.gradle.kts. flake.nix does not explicitly consume any changed path: its
package, wrapper references, dependency cache and lock inputs remain unchanged.
Therefore package derivation inputs and wrapper check inputs are invariant.
No full distribution proof is invalidated, and no human Nix gate is required
merely for this workflow/report change. This does not weaken the final ladder for
actual production/build/package inputs. Existing historical evidence retains its
original limitations; no new package or native qualification is claimed.

## Remote disposition and final Git state

No staging, commit, push, tag, release, workflow dispatch or Nix execution.
Remote workflow behavior remains untested until a later authorized commit/push.
Release remains NOT QUALIFIED; no release is declared.

Final branch/HEAD remain `main` /
`f85aa2cf77d84210ac72a4a02d0f2c1c305a43dd`. All work is unstaged/uncommitted:

```text
 M .github/workflows/ci.yml
?? .github/scripts/
?? review/DESKTOP_GITHUB_ACTIONS_REFRESH_REPORT.md
```

The scripts directory contains only the two new guard/test shell files.
`src/**`, Java pins, Gradle/build inputs, distributions, package.nix, flake.nix,
flake.lock, package-deps.json, README.md and QUALIFICATION.md are unchanged.
