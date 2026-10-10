# Repository instructions

## Totipo Java application-operation model

Before changing desktop code involving VaultState handling, state subscription or
rendering, concurrency, asynchronous work, Swing EDT scheduling, operation
admission, global BUSY/enabled state, TOTP generation, refresh/observation,
Add/Edit/Delete/Resolve, publication/save, retry/partial publication,
open/create/close, session Lock/lifetime, or filesystem watcher integration with
Java refresh, read the exact reviewed
[API_DESIGN.md at 3b24b54becde0c93479c1fbd80ea0fbd2026e2a8](https://github.com/totipo-dev/totipo-java/blob/3b24b54becde0c93479c1fbd80ea0fbd2026e2a8/API_DESIGN.md),
especially **Operation classes and state-snapshot semantics** and the detailed
contract for the affected operation.

If totipo-java is not locally available, fetch/read that exact pinned guidance
revision from GitHub read-only, outside this worktree (for example, fetch the
commit into a temporary repository and use `git show <commit>:API_DESIGN.md`).
Do not substitute `main`. Verify the revision when using a local Java checkout.
The artifact/source pin and later application-model guidance pin are distinct;
see [TOTIPO_JAVA_DEPENDENCY.md](TOTIPO_JAVA_DEPENDENCY.md). Advancing the guidance
pin alone does not change the Java runtime dependency. When investigating a
contract discrepancy, compare guidance with released 0.2.0 source/tests at
`d6310c177ae930df188fd4f5798622c935698b2e`; do not assume the document is correct.
See [the desktop audit](review/DESKTOP_JAVA_OPERATION_MODEL_AUDIT.md) for existing
policy decisions and evidence gaps.

**A newer VaultState is not, by itself, a generic cancellation signal.**
Consult the operation-specific Java contract before cancelling asynchronous work,
disabling the entire desktop UI, rebasing builders, discarding historical
references, or serializing otherwise independent work. Distinguish Java semantic
validity from presentation relevance and session/lifecycle ownership.

Desktop policy may intentionally be stricter than Java capability, but such
policy must be documented as a product/application choice rather than
misrepresented as a Java correctness requirement.

Do not decide EDT/background placement by analogy with another Java operation:

1. Classify the operation using the pinned Java operation model.
2. Determine whether it performs provider/store I/O or may block.
3. Apply desktop controller/EDT ownership rules, preserving builder confinement.

Local projection is not automatically equivalent to save/open/create.
Non-blocking `requestRefresh()` is not equivalent to provider publication.
Immutable VaultState reads do not need a global background-operation model merely
because some saves do. Preserve operation-specific freshness and frozen retry
semantics; session replacement guards are separate from state validity.

## Qualification ladder

Follow the committed convention in [QUALIFICATION.md](QUALIFICATION.md): agents
run normal non-Nix validation; the human milestone gate is exactly
`nix flake check path:.`. Do not run Nix or request an ordinary `nix build` gate.

**Focused iteration; complete boundary qualification.** Do not rerun full desktop
build/distribution/Nix after every small correction.
Java-operation classification determines WHAT needs testing.
Qualification ladder determines WHEN broad tests run. Neither rule weakens the
operation-model requirements above.

### Classification and baseline

Start from clean `main`. Each future agent must classify its milestone and record
the classification in its report; combine classes when scopes overlap:

| Class | Scope |
| --- | --- |
| `DOCS_ONLY` | Documentation/provenance; production, tests and build/package inputs unchanged |
| `UI_PRESENTATION` | GUI presentation, layout, interaction or accessibility |
| `CONTROLLER_LIFECYCLE` | Admission, concurrency, session ownership, lock or retirement |
| `FILESYSTEM_WATCHER` | Watcher integration or filesystem observation |
| `PRODUCT_MUTATION` | Add/Edit/Delete/Resolve, publication, save or retry |
| `BUILD_DEPENDENCY` | Build inputs, dependency pins, locks or verification |
| `PACKAGING_RELEASE` | Distribution, packaging or release work |

For ordinary production milestones, baseline ONCE using a reasonably fast normal
gate. The committed starting point already has prior distribution qualification:

```sh
./gradlew test verifyMavenBoundary verifyJava17Bytecode
```

Do not use clean/rerun/offline at baseline. Do not build ZIP/TAR at baseline unless
the milestone itself concerns packaging/distribution.

### Development inner loop and failure diagnosis

Use the narrowest affected tests, for example:

```sh
./gradlew test --tests '*TokenBrowserTest'
./gradlew test --tests '*VaultWindowControllerTest'
./gradlew test --tests '*WatcherSessionIntegrationTest'
```

Watcher changes may use the focused disposable filesystem harness. UI changes may
use focused Swing tests. After each edit, do not clean, run distribution tasks,
force offline rebuilds, request Nix, or rerun filesystem/native smoke.

If a full gate fails, follow:

```text
full failure -> focused reproduction -> focused fixes -> focused PASS
             -> rerun the affected full gate once
```

Do not run the entire matrix repeatedly while diagnosing.

### Final milestone qualification

When source/tests are stable, run the final normal gate ONCE:

```sh
./gradlew \
  test \
  build \
  verifyMavenBoundary \
  verifyJava17Bytecode
```

No clean is needed unless investigating a concrete stale-output issue. Existing
task dependencies apply: `build` reaches `check`, which includes installed-
distribution verification and qualification harness compilation.

For production/build/package changes, run the final strict/distribution gate ONCE:

```sh
./gradlew \
  --offline \
  --no-daemon \
  --no-build-cache \
  --rerun-tasks \
  clean \
  test \
  build \
  installDist \
  distTar \
  distZip \
  verifyDistribution \
  verifyDistributionArchives \
  verifyMavenBoundary \
  verifyJava17Bytecode
```

This is the fresh final distribution proof. Do not separately rerun `clean test`,
`installDist`, `distTar` or `distZip` afterward unless actual relevant inputs change.

For `DOCS_ONLY`, with `src/main`, `src/test` and Gradle/build/package inputs
unchanged, use documentation/static consistency checks, dependency/pin inspection
when relevant, and `git diff --check`. Do not automatically run baseline/final
Gradle or expensive strict/distribution gates. Explain why the full distribution
gate was not invalidated. Request human Nix only if changed paths are qualification
inputs or current policy requires it; inspect source filtering and explicit flake
references rather than assuming every Markdown edit requires Nix.

Run specialized final qualification once after stability, only for affected scope:

| Affected scope | Specialized final check |
| --- | --- |
| Watcher/filesystem | Disposable watcher/filesystem qualification |
| GUI/layout | Focused native GUI smoke |
| Clipboard | Clipboard qualification |
| Accessibility | Relevant accessibility/native qualification |

Do not carry every historical harness into every milestone. Record actual tested
environments and limitations; headless Swing tests do not establish native PASS.

### Nix last and invalidation

Freeze production source, tests, package/build configuration and qualification
tooling included by the flake source before requesting exactly the human gate:

```sh
nix flake check path:.
```

Agents never run Nix. No ordinary `nix build` gate. `nix build --rebuild` remains
release/reproducibility-specific, not routine milestone qualification.

| Change after final qualification | Required response |
| --- | --- |
| Production/test/build input after final strict gate | Rerun applicable Gradle gate |
| Only GUI smoke checklist/report text | No Gradle rerun unless packaged/checked |
| Watcher harness; production unchanged | Rerun watcher harness; determine whether Nix source includes it |
| Distribution verifier | Rerun distribution verification against final outputs; rebuild only if verifier identifies a real packaging problem |
| Any flake-source input after Nix | Rerun human Nix |

Apply invalidation to consumed qualification inputs, including explicit flake
references. Files merely present in the checkout are not necessarily consumed.

### Execution counts and evidence

Every substantial report records classification, baseline gate count, focused tests
(commands and runs), final normal gates, final strict gates, specialized filesystem/
GUI runs (and other relevant specialized checks), and Nix runs. Count failures and
invalidation reruns too; distinguish agent runs, human-reported runs and unperformed
checks. Do not infer cache/fresh execution from a human PASS without logs.

Clean target for ordinary production milestones: baseline normal **1**, final normal
**1**, final strict/distribution **1**, human Nix **1**. Focused iteration varies;
specialized runs depend on scope. `DOCS_ONLY` may correctly record zero broad gates
with an input-invariance explanation. Preserve historical reports, failures and
input-specific PASS evidence; their old command matrices are not current policy.
