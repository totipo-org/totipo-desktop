# Desktop Nix / CI unification

Status: implementation and agent non-Nix validation complete; human Nix
qualification PASS (human-reported). Remote CI NOT RUN. All changes remain unstaged/uncommitted.
No Nix command was run by the agent. No staging, commit, tag, release, push or
CI dispatch occurred. This infrastructure change does not qualify a release.

## 1. Starting HEAD/state

Required starting commands were run before editing:

- Branch: `main`.
- HEAD: `d5640d24a94703f6b6fbe5ea6e3d517f708f073b`.
- `git status --short`: empty.
- `cat VERSION`: `0.0.0-dev`.

HEAD is “Polish desktop refresh vault creation and token layout”; its parent
`5fb43c2` reconciles Java 0.2.0 / v1/r19. Build inputs, Gradle locks and README
confirm released Java 0.2.0 / v1/r19. The existing package/cache architecture
was inspected before edits and preserved. No applicable AGENTS.md was found.

## 2. Baseline Gradle validation

All baseline gates passed before infrastructure edits:

| Command | Result |
| --- | --- |
| `./gradlew clean test build` | PASS, 56s; 437 tests in 66 suites, zero failures/errors/skips |
| `./gradlew installDist distTar distZip verifyDistribution verifyDistributionArchives` | PASS; installed layout and both archives verified |
| `./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean test` | PASS, 36s; fresh task execution |
| `./gradlew verifyMavenBoundary verifyJava17Bytecode` | PASS |
| `./gradlew --offline dependencies --configuration runtimeClasspath` | PASS; graph below |
| `git diff --check` | PASS; worktree still clean |

The wrapper downloaded its pinned Gradle 9.8.0 distribution for the first run.
A fontconfig diagnostic appeared during headless tests; the build/test result
was successful, with no skipped tests or ignored gate failures.

Runtime graph (strict locked constraints select the same versions):

```text
org.totipo:totipo-storage-nio:0.2.0
  -> org.totipo:totipo-core:0.2.0
    -> org.bouncycastle:bcprov-jdk18on:1.86
```

Distribution JAR inventory, verified before and after editing:

```text
bcprov-jdk18on-1.86.jar
totipo-core-0.2.0.jar
totipo-desktop-0.0.0-dev.jar
totipo-storage-nio-0.2.0.jar
```

VERSION: `0.0.0-dev`. Baseline package-deps SHA-256:
`01c8e8f5c67800876fe44b0890dcb2d6d5de67c58b936956646c4dd8390f6a5c`.

## 3. Existing flake/package architecture

`flake.nix` constructs `desktop` once with `pkgs.callPackage ./package.nix`,
passing the full `pkgs.jdk25` and `pkgs.gradle_9.override { java = pkgs.jdk25; }`.
The package is exposed only on Linux. The flake also retains the development
shell, jailed-agent inputs, formatter and official dependency-cache updater app.
`package-deps.json` is a fixed Gradle dependency cache, separate from Gradle
locks and strict artifact verification. `flake.lock` pins all existing inputs;
the root nixpkgs revision is `39ad350a0602fa0a58a544344e3e9187526ea45c`.

## 4. Why prior flake check was incomplete

The starting flake exposed `packages.${system}.default = desktop`, but had no
`checks` output. `nix build path:.` therefore realized the desktop package and
its build/check/install phases; the prior flake check evaluated package output
shape without realizing that package as a check. This explains why the earlier
check/build sequence did not represent two independent package tests.
The [Nix 2.35 manual](https://nix.dev/manual/nix/2.35/command-ref/new-cli/nix3-flake-check)
distinguishes output evaluation from building derivations in `checks`.

## 5. Same-derivation checks/packages design

Static review of the final diff confirms:

- One `desktop` binding and one `callPackage ./package.nix` remain.
- `packages.default = desktop` and `checks.desktop = desktop` reuse that value.
- Both attribute sets use `pkgs.stdenv.hostPlatform.isLinux`.
- No second package construction, repeated Gradle invocation or recursive Nix
  call was introduced in a derivation.
- The nearby comment documents the single normal gate and derivation identity.
- No dependency input changed. macOS/Windows package qualification is not claimed.

This is a static audit, not an agent-executed Nix evaluation.

## 6. Package.nix build/test coverage

`package.nix` remains the build/test/package authority, unchanged:

- Full pinned Nix JDK/tooling, `gradle.fetchDeps` from package-deps.json,
  strict Gradle dependency verification and fail-closed cache readiness.
- `gradleBuildTask = "installDist verifyDistributionArchives"`.
- `doCheck = true`, `gradleCheckTask = "check"`.
- Gradle `check` includes tests, Maven boundary, Java 17 bytecode, distribution
  verification and separate filesystem-harness compilation, not its execution.
- Archive verification depends on installDist verification, distZip and distTar.
- Install checks enforce four exact runtime JARs, byte comparisons, pinned JDK
  launcher, attach flag, desktop entry, icons, licenses and absence of source,
  tests and mutable development caches.

The pinned nixpkgs Gradle setup hook was inspected read-only at the locked
revision: its build/check phases invoke these tasks and its normal configure
path adds `--offline` outside dependency-cache update transport. No Maven Local,
project/source substitution or network fallback was introduced.

## 7. Wrapper-integrity preservation

The old CI alone checked the wrapper before running Gradle; package.nix uses
Nix Gradle and did not verify that JAR. `checks.wrapper` is now a small Linux
`runCommand` using normal nixpkgs shell/coreutils/grep tools, with no network
fetch and no wrapper execution. It checks the exact previously reviewed JAR
SHA-256 from old CI and bootstrap-m0.sh:

`238e777fcddd7e34f9708186085def2abd6e08e658505b38718d79d74c21abd5`.

It also checks the pinned Gradle 9.8.0 bin URL and existing distribution SHA-256
`bafd5ce9cfaea0fbccfdc8439a1ac42fbd4cd9c89dc9a988228d8a2639a58e6c`.
Successful verification produces a trivial output file. The local JAR hash
and exact property lines were verified without executing the wrapper for that
verification. This is property-pin verification, not a new distribution download.
The redundant CI-only hash rule is removed; flake checks now cover it.

## 8–10. Command semantics and limits

Human routine Nix gate: **`nix flake check path:.` only**.
It evaluates the flake and realizes desktop and wrapper checks, so the actual
package build/tests/distribution/install checks are included. Already qualified
store outputs may be reused; every invocation need not execute a fresh build.

`nix build path:.` materializes the same default package and creates a local
result link for use/inspection/native launch. It provides no independent
qualification evidence after flake checks and is not a second normal gate.

`nix build --rebuild path:.` remains a deliberate release/reproducibility
spot-check only, scoped to the tested derivation/builders and supplied comparison
evidence. It does not prove reproducibility across arbitrary builders.

These commands do not qualify full native GUI/clipboard/accessibility behavior,
every supported filesystem, release publication or universal reproducibility.
Agents do not run Nix in this workflow or request an additional ordinary build
or release rebuild for routine milestone qualification.

## 11–12. Current documentation / checklist updates and search classification

The complete current tree, including hidden workflow files and historical
reports, was searched for `nix flake check`, `nix build` (including `--rebuild`),
`nix develop`, `setup-java`, `./gradlew`, `package-deps` and
`update-package-deps`. Git internals and generated .gradle/build output were
excluded. Historical reports were inspected as evidence, not rewritten.
All relevant occurrence groups are classified below (files can contain more
than one category):

| Occurrences | Classification / disposition |
| --- | --- |
| `.github/workflows/ci.yml`, former setup-java/hash/direct Gradle steps | CURRENT CI; replaced by checkout, Nix installation, one flake check |
| `README.md` Gradle/run/bootstrap/dev-shell and generic distribution instructions | CURRENT DEVELOPER GUIDANCE; preserved |
| `README.md` Nix package qualification and former check/build/rebuild blocks | CURRENT QUALIFICATION GUIDANCE; replaced by single routine check and explicit materialization/release roles |
| `README.md` updater app and alternative `nix build --no-link ...mitmCache.updateScript` | CURRENT DEVELOPER GUIDANCE; retained separately for dependency-input changes, not normal package qualification |
| `README.md` release version validation / forced-rebuild explanation | CURRENT RELEASE GUIDANCE; explicit limited release scope |
| `README.md` filesystem harness | CURRENT QUALIFICATION GUIDANCE; separate explicit filesystem qualification preserved |
| `QUALIFICATION.md` new routine gate and workflow rules | CURRENT QUALIFICATION GUIDANCE; one human command, no agent Nix |
| `QUALIFICATION.md` prior reported multi-command passes | HISTORICAL REPORT evidence embedded in a live document; preserved and explicitly distinguished from current instructions |
| `RELEASE_CHECKLIST.md` build/version/cache/Nix requirements | CURRENT RELEASE GUIDANCE; single source/package check, local result link only if needed, separate release forced rebuild; unrelated gates retained |
| `qualification/PACKAGED_LAUNCH.md` cache/package/result/native launch | CURRENT QUALIFICATION GUIDANCE; single check, build only to obtain native-launch result link |
| `qualification/PACKAGED_LAUNCH.md` forced rebuild | CURRENT RELEASE GUIDANCE; explicit deliberate reproducibility scope |
| `bootstrap-m0.sh` shell/bootstrap/wrapper/metadata-refresh/Gradle commands | CURRENT DEVELOPER GUIDANCE executable bootstrap; unchanged, not CI or ordinary Nix qualification |
| `gradle.lockfile` regeneration comment | CURRENT DEVELOPER GUIDANCE; unchanged |
| `TOTIPO_JAVA_DEPENDENCY.md` dependency-cache explanation | CURRENT DEVELOPER GUIDANCE; unchanged |
| `package.nix` cache input/staleness message and `flake.nix` updater app | CURRENT DEVELOPER GUIDANCE/build implementation; preserved |
| `flake.nix` new invariant comment | CURRENT QUALIFICATION GUIDANCE; same package derivation under both outputs |
| All pre-existing `review/*.md` matching commands/cache references | HISTORICAL REPORT; no changes |
| This new report | Current milestone evidence; human Nix PASS, never a request for multiple gates |

No existing agent/contributor instructions requested both commands outside the
live documents above. Agent-facing rules are now explicit in README and
QUALIFICATION. Release native/security/legal/artifact/version gates remain.
Cache regeneration is required only when dependency inputs change; current
reviewed cache inspection remains required.

## 13–15. Android reference, bootstrap choice and action provenance

Sibling repositories are not present locally. Committed GitHub content was
inspected read-only via exact revisions resolved with `git ls-remote`:

- [Android flake](https://github.com/totipo-org/totipo-android/blob/3493126a06aa6eeccb905b8f1bc87fc95b2f6717/flake.nix)
  and [CI](https://github.com/totipo-org/totipo-android/blob/3493126a06aa6eeccb905b8f1bc87fc95b2f6717/.github/workflows/ci.yml).
- Android uses cachix/install-nix-action v31.11.1, already SHA-pinned, with
  checkout v7.0.1 also SHA-pinned. Its installer defaults mark the runner user
  trusted for user cache configuration and enable nix-command/flakes. Its
  workflow does not explicitly set accept-flake-config; no remote run behavior
  was inferred from that omission.
- Android exposes the same androidPackage under packages.default and
  checks.android, restricted to x86_64-linux. Its qualification job redundantly
  runs flake check and ordinary build. Desktop does not copy that redundancy.

Desktop reuses these two action revisions, independently resolved read-only
using upstream `git ls-remote` exact tag refs (no peeled alternative was returned):

| Action | Intended upstream tag | Resolved commit |
| --- | --- | --- |
| [actions/checkout](https://github.com/actions/checkout/tree/v7.0.1) | v7.0.1 | `3d3c42e5aac5ba805825da76410c181273ba90b1` |
| [cachix/install-nix-action](https://github.com/cachix/install-nix-action/tree/v31.11.1) | v31.11.1 | `13d8dd58da0234aa297dedd986986ccb8e7f3e24` |

The installer's action.yml and install-nix.sh at that exact SHA were read.
It is a composite action installing its upstream versioned Nix 2.35.2 release;
no custom installer, floating action branch or new cache was introduced.
Human-readable release comments accompany both immutable workflow refs.

## 16–19. CI audit, permissions and cache/config handling

Static workflow review confirms one Linux build/check job on ubuntu-latest:

- Checkout has `persist-credentials: false`; `contents: read` is the sole grant.
- Reviewed SHA-pinned Nix installer; no setup-java or separately installed Gradle.
- Exactly one application qualification command:
  `nix flake check --print-build-logs path:.`.
- No direct Gradle build, ordinary nix build, forced rebuild or artifact upload.
- No user credentials or configured secrets required. The installer may use
  GitHub's automatic read-only job token for GitHub input rate limits by default.
- `extra_nix_config` sets `accept-flake-config = true` for noninteractive use of
  the existing repository policy; the installer retains its normal trusted-user
  setup. Android's pinned installer is reused with this explicit acceptance.
- Existing cache.numtide.com substituter and its exact trusted public key are
  unchanged. No new cache, signature bypass or trust-policy weakening added.
- Flake inputs come through Nix; Gradle inputs use the fixed dependency cache
  and existing strict verification, with no Maven Local or source substitution.

This is static review, not executed workflow evidence.

## 20–22. Input/package invariance

`git diff --exit-code` confirms flake.lock, package-deps.json, package.nix,
VERSION, all src files, Gradle build/settings/properties/locks and wrapper/
verification inputs are unchanged from starting HEAD. No updater or input
refresh was run. SHA-256 values:

| File | Before/after invariant SHA-256 |
| --- | --- |
| package-deps.json | `01c8e8f5c67800876fe44b0890dcb2d6d5de67c58b936956646c4dd8390f6a5c` |
| flake.lock | `44728fcfd8529462cb415b186ee4ce3400343dd911d325b6a965bd386b9f7749` |
| package.nix | `ac67a516967ae81c8770bde390b3f36820a35b5aa2511132c814898589147155` |
| VERSION | `fe4d33c8c2c76a67725ea7d54dafb79c485bd2819a317d7235a6910157ef76f4` |

Production Java/UI, tests, dependency boundary and package logic are unchanged.

## 23. Post-edit non-Nix validation

All required post-edit gates passed. No production behavior or test change was
needed.

| Command | Result |
| --- | --- |
| `./gradlew clean test build` | PASS; 14 actionable tasks, 10 executed and 4 from cache (including tests); restored results contain 437 tests |
| `./gradlew installDist distTar distZip verifyDistribution verifyDistributionArchives` | PASS; inventory and both archive verifications passed |
| `./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean test` | PASS, 35s; all 6 tasks executed; 437 tests in 66 suites, zero failures/errors/skips |
| `./gradlew verifyMavenBoundary verifyJava17Bytecode` | PASS |
| `./gradlew --offline dependencies --configuration runtimeClasspath` | PASS; identical locked runtime graph |
| `git diff --check` | PASS |

Final invariance review confirms package-deps/flake.lock hashes above, VERSION
`0.0.0-dev`, unchanged production sources and no staged changes. The ordinary
post-edit build reused Gradle cache entries; the subsequent offline run explicitly
disabled build-cache reuse and reran tests.

## 24. Human single-command Nix result

**PASS (human-reported).** After all agent non-Nix gates passed, the human was
asked to run only:

```sh
nix flake check path:.
```

The human replied “flake check passed”. No terminal logs, derivation paths or
fresh-build/cache-reuse details were supplied, and none are inferred. The
structural identity `checks.desktop = packages.default = desktop` establishes
that the actual desktop package is included in this gate. No additional ordinary
build or forced rebuild was requested or needed.

After the human's PASS report, Git status and the complete tracked diff were inspected again:
HEAD remains `d5640d24a94703f6b6fbe5ea6e3d517f708f073b` on main, the index is
empty, and only the intended infrastructure/documentation files have changes.
Package-deps, flake.lock, package.nix and VERSION hashes exactly match the values
above. Production sources, all src files and Gradle/dependency inputs remain
unchanged. `git diff --check` passed. The final edits after the human result
record evidence only; no flake, workflow or package behavior changed.

## 25. Remote CI

**NOT RUN.** Nothing was pushed or dispatched. The new workflow can be
qualified authoritatively only after a later human review/commit/push and
inspection of the exact committed run.

## 26. Concrete later Android / Java / spec follow-ups

No other Totipo repository was edited.

- Android at `3493126a06aa6eeccb905b8f1bc87fc95b2f6717`: remove the redundant
  ordinary build after its package-inclusive flake check; review explicit
  noninteractive flake-config acceptance. Action SHA pinning is already present,
  so no tag-to-SHA migration finding applies to this Android snapshot.
- [Java flake](https://github.com/totipo-org/totipo-java/blob/d6310c177ae930df188fd4f5798622c935698b2e/flake.nix)
  at `d6310c177ae930df188fd4f5798622c935698b2e` has a dev shell but no checks.
  Its CI still provisions JDK 25 and runs Gradle/publication/consumer checks and
  Python release-guardrail tests directly; checkout/setup-java are tag-pinned.
  A future Nix unification must preserve those meaningful checks and wrapper
  integrity rather than merely add a superficial flake check.
- [Spec flake](https://github.com/totipo-org/totipo-spec/blob/cdb4e91be1c6d3704874b2b92457ffe7be5e9084/flake.nix)
  at `cdb4e91be1c6d3704874b2b92457ffe7be5e9084` has a dev shell but no checks.
  Its actual workflow is `.github/workflows/conformance.yml` (ci.yml is absent).
  Read-only inspection of that workflow and Makefile found meaningful Python
  structure/vector tests, Go vet/tests, manifest conformance/integrity, Linux
  race and bounded fuzz checks outside flake checks. A later review can expose
  meaningful conformance qualification while preserving the existing platform
  matrix and deliberate test scope.

## 27. Final Git state

HEAD remains the starting HEAD on main. Index is empty. Only flake.nix, CI,
README, QUALIFICATION, RELEASE_CHECKLIST and qualification/PACKAGED_LAUNCH are
modified, plus this new untracked report. Historical reports are unchanged.
Human Nix qualification passed (human-reported); remote CI remains unrun.

```text
 M .github/workflows/ci.yml
 M QUALIFICATION.md
 M README.md
 M RELEASE_CHECKLIST.md
 M flake.nix
 M qualification/PACKAGED_LAUNCH.md
?? review/DESKTOP_NIX_CI_UNIFICATION_REPORT.md
```
