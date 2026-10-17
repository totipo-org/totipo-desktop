# Java editor development support

Classification: BUILD_DEPENDENCY (development tooling and IDE build model).
Started on clean `main` at `f022db57ed01ffd9f73732a8e5ac91debb57a968`.
Application Java operations are unaffected; no desktop Java code changed.

Reviewed upstream HEAD and its README, INTEGRATION.md, flake, library and source
helper at [b5fe56eaebc570a50dbb9803e7d7a8cf179731d3](https://github.com/ingon/java-dev-tools/tree/b5fe56eaebc570a50dbb9803e7d7a8cf179731d3).
Read-only clone used `/tmp/totipo-java-dev-tools.5LoDqG`, outside this checkout.

## Changes

- `flake.nix` consumes `github:ingon/java-dev-tools`, following root nixpkgs,
  and uses the existing `pkgs`, full JDK 25 and patched Gradle. The shell merges
  `javaEditor.shellEnv` and adds `javaEditor.packages`. Package/check outputs
  and jailed-agent configuration are preserved.
- `build.gradle.kts` configures Eclipse only when an IDE applies its plugin.
  Main, test and qualification output goes under `build/eclipse/`; Gradle
  classes remain under `build/classes/java/`.
- `.gitignore` excludes generated Eclipse state. Existing `build/` and
  `.direnv/` patterns already cover the required output and environment state.
- Verification metadata adds exactly 13 source artifacts, preserving every
  existing artifact entry. Dependency versions, Gradle locks, package-deps.json,
  package.nix, production and test sources are unchanged. Normal package
  build/check tasks do not resolve IDE sources, so its fixed dependency cache
  does not need source attachments added. Verification metadata and the Gradle
  build script are included by the existing package source filter.
- README documents source resolution, shell/editor reload and navigation checks.
  No helper scripts, proxy code or project-local Helix configuration were copied.

The lockfile changed during the session without an agent Nix invocation. Static
comparison confirms it adds only the new toolkit node and root input reference,
pins the reviewed revision above, and follows root nixpkgs. Every preexisting
node/pin is unchanged. This observation is not evidence of Nix qualification.
Root nixpkgs remains `319e4cebef10cbb636c6ea392137b52ad105336a`.

## Agent validation

Used the already available Nix-store toolchain:

- JDK: `/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1`,
  OpenJDK 25.0.4.1+1; its `lib/openjdk/lib/src.zip` exists.
- Gradle: `/nix/store/jma178bc8ijyv2xbb6c142ajylyhvcnc-gradle-9.8.0`,
  revision `a927be5e08efe79e0b87ada06c762dde6bb9f8b8`.

No baseline Gradle run; six post-edit Gradle invocations:

| Command/check | Runs and result |
| --- | --- |
| `gradle --no-configuration-cache --init-script /tmp/totipo-java-dev-tools.5LoDqG/gradle/ide-sources.gradle ideSources` | Two runs: initial FAIL for 13 missing source checksums; final strict PASS with all 13 attachments |
| Same helper with `--write-verification-metadata sha256` | One PASS; generated source entries |
| `gradle test build installDist distZip distTar verifyDistribution` | One PASS; 1 task executed (distribution verification), 10 up-to-date, including tests; no fresh test execution claimed |
| Temporary external `verifyDesktopIdeModel` init-script task, with the upstream helper applied | Two runs: first assertion incorrectly counted source folders without resources; corrected check PASS for all four folders, three distinct IDE output directories, and unchanged Gradle main output |

The source helper was invoked directly with the managed Gradle because the new
Nix shell launcher has not been qualified. These commands exercise the exact
upstream helper without copying it into this repository.

Independently downloaded all 13 source JARs from canonical Maven Central and
matched SHA-256 against generated metadata. Eleven also matched published
SHA-256 sidecars. Totipo's two artifacts have no SHA-256 sidecars (HTTP 404);
their canonical downloads matched recorded SHA-256 and published SHA-1 sidecars.
The first checksum script attempt could not find Python on PATH; reran using an
existing Nix-store Python. The sidecar check stopped at the first missing Totipo
SHA-256 sidecar, then both Totipo artifacts passed the explicit fallback check.
No verification exemption was added.

Static checks PASS: existing lock pins and dependency inputs preserved,
`git check-ignore` covers generated IDE state/output, and `git diff --check`.
New report uses intent-to-add source membership only; no staged contents.

## Human qualification

- Agent Nix command executions: **zero**. Human reported **PASS** for
  `nix flake check` after the implementation handoff. No execution logs were
  supplied; fresh execution versus store reuse is unknown. Post-report review
  confirms the flake, lock, Gradle build and verification changes still match
  the reviewed implementation. Only this evidence report was edited afterward;
  it is excluded from package sources. No gate rerun is required for this
  report-only update. Later consumed-input edits invalidate affected evidence
  and require the human gate again.
- Editor integration smoke check: **PASS (human-reported)**. The human confirmed
  go-to-definition works for internal and external symbols and reported that
  every LSP action tried appears to work. This establishes observed editor
  navigation; no exhaustive LSP coverage, separate JDK-source check, health-command
  output or Eclipse/Buildship GUI check is inferred from that report.
- Filesystem harness, native application GUI/clipboard and packaged launch:
  not run for this development-tooling change; no new qualification claimed.
  No release is authorized by this work.
