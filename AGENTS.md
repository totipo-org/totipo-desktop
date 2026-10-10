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

## Qualification

Follow the committed convention in [QUALIFICATION.md](QUALIFICATION.md): agents
run normal non-Nix validation; the human milestone gate is exactly
`nix flake check path:.`. Do not run Nix or request an ordinary `nix build` gate.
