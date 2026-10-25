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

Supported development requires the activated Nix/direnv environment. Nix selects
one JDK 25 and patched Gradle derivation for development, agents and packaging.
Desktop compilation and generic runtime support target Java 25.

### Classification and development

Start from clean main. Record applicable milestone classes: `DOCS_ONLY`,
`UI_PRESENTATION`, `CONTROLLER_LIFECYCLE`, `FILESYSTEM_WATCHER`,
`PRODUCT_MUTATION`, `BUILD_DEPENDENCY`, `PACKAGING_RELEASE`.
Java-operation classification determines what needs testing; this ladder determines
when to run broad qualification. Use a baseline when useful and focused Gradle
tests during development, for example:

```sh
gradle test --tests '*VaultWindowControllerTest'
gradle test --tests '*WatcherSessionIntegrationTest'
gradle run
```

Do not clean or rebuild every distribution after each small edit. Diagnose broad
failures with focused reproduction, fixes and focused PASS before rerunning the
affected gate. Do not continue past unexplained build or packaging failures.

### Final authority and separate boundaries

The human-owned authoritative automatic repository gate is exactly:

```sh
nix flake check
```

Agents never execute Nix. CI runs the same package-inclusive check. An uncached
package builds installDist, executes the full application test suite through
`check`, verifies essential distribution contents and checks Nix installation
transformations. Store reuse is valid evidence of a qualified derivation; report
reuse accurately rather than claiming fresh tests.

Agents may run focused or full Gradle checks using the already available Nix
toolchain. No mandatory normal-plus-strict/offline duplicate full suites precede
the Nix gate. Build generic archives when the milestone affects them:

```sh
gradle test build installDist distZip verifyDistribution
```

The portable ZIP is canonical. `distTar` remains available internally, without a
separate publication or qualification requirement.

Headless Java tests, disposable filesystem qualification, native GUI/clipboard,
actual packaged launch and deliberate release reproducibility are distinct.
Run affected specialized checks on the artifact/environment claimed. Record
unavailable checks as pending/UNQUALIFIED; headless Swing does not establish
native PASS. Preserve useful behavior tests and historical evidence.
DOCS_ONLY changes use static consistency checks and `git diff --check`; inspect
source filtering and explicit flake inputs before requesting human Nix.

### Nix last and invalidation

Freeze all consumed production/test/build/package/qualification inputs before
requesting the human gate. Any consumed input changed afterward invalidates
affected evidence and requires the human gate again. Report/checklist-only edits
do not require Gradle unless packaged or checked; harness changes require their
affected checks. Verifier-only edits require verification against final outputs;
rebuild only if a real packaging problem is identified.

Use the Git-aware working tree. Modified tracked files stay unstaged. For each
new intended milestone file, including its report, agents may run:

```sh
git add --intent-to-add <file>
git diff --cached --quiet
```

Intent-to-add establishes source membership only; no staged file content is
permitted without explicit authorization. Leave membership for owner review.
Ignored caches/build output/secrets remain outside source membership. Git-aware
local Nix forms consume this checkout; explicit `path:` sources are reserved for
intentional raw/non-Git sources. No ordinary `nix build` qualification gate.
A human may materialize a package for native launch; deliberate independent
release comparisons are separate from routine qualification and store reuse.

### Evidence

Substantial reports record classification, exact source/toolchain identities,
baseline/focused/full Gradle commands and execution counts including failures,
specialized filesystem/GUI/clipboard runs, Nix agent runs (zero), human-reported
gate results, pending checks, and input invalidation. Do not infer fresh execution
from a human PASS without logs. No release is authorized by qualification alone.
