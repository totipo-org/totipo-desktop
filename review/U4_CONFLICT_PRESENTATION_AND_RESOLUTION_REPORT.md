# U4 — conflict presentation and field resolution

## Baseline and review state

Baseline commit: `652efea94fb175330f7a9d0aa160b073bbbf6864` (accepted U3/U3.1).
`git status --short` was empty before work. No production or test edits occurred until
`./gradlew clean test build` passed: 316 tests, zero failures/errors.

U4 remains uncommitted. Nothing was published, tagged, or released. `totipo-java`
was not modified. Manual visual/operator verification remains pending; automated
checks do not establish subjective visual quality.

## Previous behavior and public API

The list rendered one selectable logical-token row, joining competing issuer/account
values, with an amber conflict warning. Parent-level Show Code requested the distinct
eligible code-producing outcomes as a batch (deduplicated using public secret groups
and algorithm/digits/period). Edit opened `Edit conflicting token` with a version
chooser before opening the ordinary editor. Resolve was reachable from that chooser.

The old merge editor first offered checkboxes for captured complete Alternatives,
then Continue opened seven independent combo-box field/secret choices. It permitted
subset selection, custom text/numeric fields, changing agreed fields, and replacement
secret ingress. Separate setup fields could inadvertently combine an existing secret
with another version's parameters.

The UI consumes `VaultState.tokens()/token()`, `TokenState.alternatives()`,
`hasConflict()`, `competingValues()`, `heads()` and `unresolvedReferences()`.
`TokenAlternative` supplies a complete `TokenDescriptor` and its provenance Heads.
`CompetingField.values()` supplies distinct field values and their member Alternatives;
`CompetingSecret.groups()` supplies public secret-equivalence membership. No raw
existing secret is available or needed. `TokenHead` provides causal/provenance
identifiers and metadata for diagnostics, not a user-facing token value.

Complete tombstones previously appeared with technical status terminology in merge
choices/diagnostics and were excluded from TOTP eligibility. Complete tombstones
remain editable under the existing update semantics. Tokens lacking complete
Alternatives previously offered diagnostics and no usable code/edit value.

The existing merge/result path remains `MergeDraft` -> `MergeWrites` -> the public
merge builder. Full-frontier input uses `base.merge(token.id())`; subsets use
`base.merge(selectedAlternatives)`. Publication acknowledgement does not itself
replace the observed browser state. AdditionalConflict retains the unpublished
PartialResolution for explicit review, cancel, or confirmed original publication;
PublicationUncertain retains the existing retry capability and lifecycle.

## Main list and interaction

A conflicted logical token now renders one expanded amber group with warning text,
accessible description `This token has conflicting versions`, and a compact Resolve
button. The header contains no Show Code, Copy, or arbitrary-version Edit.

Each public complete semantic Alternative has one indented `TokenRowPanel`. One
Alternative with several Heads remains one ordinary row. No child creation,
code request, setup choice, or conflict warning is driven by Head count.
Issuer/account remain the primary identity. Duplicate visible identities gain
non-secret setup metadata; neutral Version numbering is reserved for identities
still indistinguishable by metadata/status. These labels imply no priority.

Children reuse the normal two-line row, Show Code, countdown/ring, Copy, inline Edit,
and Enter/Space behavior. Focus/selection/listing/search cause no derivation. Each
Show Code requests exactly that Alternative. The existing `<10 seconds` authorization,
one staged adjacent period, callback invalidation, expiry, and clipboard policy are
unchanged. More than one child may be explicitly revealed concurrently.

Each conflict Alternative receives its own `TotpDisplay` owner, held by public
Alternative object identity within the captured observation. Authorization is not
keyed solely by logical Token ID across children and is never based on Heads.
Search-only reconstruction retains owners and valid authorization. VaultState
replacement and closing clear all child owners conservatively; stable ownership
across observations is not assumed. Editing clears the edited child's owner, leaving
another child's reveal intact. Ordinary rows retain the existing shared owner.

Child Edit directly passes the chosen Alternative through the existing edit action.
The child editor visibly shows a short warning that saving this version does not resolve other conflicting versions. The old chooser had carried that warning; the normal editor previously accepted but did not display its explanation argument.
Inline and Token-menu Edit target the selected child; diagnostics retain the logical
token identity. Up/Down visits rows/children; Enter/Space shows or copies that child's
single code. Resolve is a real button in normal keyboard focus traversal. Search
shortcuts and main-window list sizing remain unchanged.

Search still matches issuer/account only through `TokenSearch`. Any child match
retains the complete group, including nonmatching children. Result counts increment
once per matching logical token and total counts use `VaultState.tokens().size()`.
No IDs, Heads, setup metadata, diagnostics, or codes enter search matching.

Deleted children display `Deleted`, remain editable if complete, and hide/disable
Show Code. No secret or complete descriptor is fabricated for unavailable state.
An empty Alternative projection gets an `Incomplete version` row with no code/edit;
unresolved references alongside complete versions get one compact unavailable notice.
The API does not enumerate descriptor-bearing incomplete Alternatives, so this is
an aggregate unavailable notice, not one invented row per unresolved reference or
Head. Diagnostics retain exact technical provenance and unresolved-reference detail.
Resolve explains incomplete observation and leaves children usable. The resolver
also prevents Save when captured unresolved references are present.

After successful resolution is actually observed, the standard render path clears
old authorization and replaces the group with the ordinary row if conflict is gone.
Cancel, failed publication, and uncertain publication do not erase the group.

## Direct resolver

Resolve opens `Resolve Conflict` directly with all captured complete semantic
Alternatives. The intro is: `Choose the value to keep for each conflicting field.`
There is no preliminary checkbox screen, Continue, Back, or normal subset UI.

Subset selection was an advanced public API capability, not necessary for correctness
of the normal full-frontier path. No advanced UI was added. `MergeInputs.select`,
`MergeDraft`, and the subset builder path remain intact for underlying supported
semantics/tests. Selecting all captured inputs ensures the normal resolver continues
to use the existing full-frontier `merge(TokenId)` path. Missing values are never
synthesized to complete that frontier.

Agreed issuer/account fields appear once as ordinary editable fields. Disagreements
show deduplicated vertical radio rows containing enabled, read-only, selectable
literal text fields, followed by a blank editable field on the same line as its
unlabeled radio. Existing-field mouse/focus selects its choice. Custom mouse/focus
selects custom before ordinary user typing; document changes also select custom.
Custom values are retained when another text choice is selected; only the selected
choice contributes to Save. Exact stored/custom strings, including empty strings
and literal markup/newlines, follow existing domain validation and reach the builder
without display escaping. No new required-field rule was introduced.

Status uses Active/Deleted, exactly mapping to ACTIVE/TOMBSTONED. A conflicting status
starts without a selection. An agreed status initializes normally and remains editable.
There is no custom third status.

Authenticator Setup is a single choice containing secret source + algorithm + digits
+ period. Deduplication requires public secret-group membership AND equality of all
three non-secret parameters. Missing equality information is conservative; coincident
TOTP digits and raw secret comparison are never used. Multiple Heads add no choices.
Existing options show literal source identity and metadata; ambiguous visible sources
and metadata use neutral Existing setup numbering. Existing secrets are never shown.

Selecting an existing option constructs the merge descriptor's algorithm/digits/period
from that same representative Alternative and passes that representative as the secret
source. The existing-setup path reads none of the custom parameter controls. Tests
submit each existing setup through `MergeWrites` and verify all parameters plus the
builder-issued secret choice belong to the selected source. This prevents mixed tuples.

The explicit custom setup uses U3.1's sensitive password field, algorithm/digit
toggles, Long period spinner (1–4294967295), Base32 decode/wipe validation, and
retirement behavior. Focus/click on controls, secret edits, toggle activation, and
period changes select custom. Custom setup is the deliberate synthesis path. Selecting
an existing setup promptly clears custom secret input; Cancel/close/retire clear it,
and submitting wipes the character buffer and transfers owned decoded bytes through
the unchanged MergeDraft lifecycle. Harmless custom parameter state can remain.

Real disagreements start with no winner. Save is disabled until required choices
exist; custom secret/numeric validation runs before submitting. Agreed setup gets
Keep existing setup selected and still permits deliberate replacement.

The resolver uses the editor's 760×720 default and 640×580 minimum (bounded to the
available screen), a width-tracking vertically scrollable form with no horizontal
scrolling, stacked choices, and fixed bottom-right Cancel/Save actions. Long untrusted
text stays in literal fields instead of widening combo-box labels.

## Runtime safety and scope

`TokenWriteController`, `MergeInputs`, `MergeDraft`, `MergeWrites`, core protocol,
storage, vault schema, secret equality, TOTP calculation, and clipboard policy are
unchanged. Captured base state and fresh-information checks remain with the existing
builder/controller. Newly observed state is not silently folded into an open editor.
Existing AdditionalConflict review, PartialResolution publication/cleanup,
PublicationUncertain retry, stale/current-base behavior, mutation reservation,
closing, and acknowledgement-versus-observation behavior remain covered.

Removed obsolete production UI: `TokenEditChoicePanel`, the merge checkbox step,
Back/Continue, and the independent combo-box field resolver. Low-level display timing
coverage remains shared; there is no reachable parent batch-reveal action in the browser.

Public API limitation: no editable descriptor or semantic identity for an incomplete
value is exposed. U4 therefore presents unavailable state compactly and does not
invent data or derive rows from Head provenance. No API limitation required a
protocol/dependency change.

Production files changed:

- `TokenBrowserPanel.java`: groups, child ownership/selection/edit/Resolve and logical counting.
- `TokenRowPanel.java`: exact Alternative identity/eligibility and Deleted presentation.
- `TotpDisplay.java`: explicit single-Alternative reveal entry point, shared timing unchanged.
- `MergeEditorPanel.java`: direct semantic radio/custom resolver and atomic setup mapping.
- `TokenChoice.java`: clearing a real disagreement's initial selection.
- `TokenEditorPanel.java`: package-local reuse of the width-tracking form body and the short visible warning when editing a conflict child.
- `VaultFrame.java`: resolver sizing/minimum.
- `TokenEditChoicePanel.java`: removed.

Tests added: `U4ConflictGroupTest`, `U4ResolverTest`. These cover grouping/Heads,
independent codes/copy/edit, cancellation, incomplete observation, search/count,
observed collapse, setup-equality cases, conservative missing equality, all custom
controls, literal text/deduplication, status, and sizing/control structure.

Tests updated: `MergeEditorTest` (direct flow, no default winner, builder-level atomic
existing/custom tuples, secret clearing, agreed fields), `MergeControllerTest`
(full-frontier UI mapping while preserving all concurrency/result tests),
`TextRoundTripTest`, `TokenEditingBrowserTest`, `TokenSearchTest`, `TokenKeyboardTest`,
`TotpCopyTest`, `GraceRevealTest`, `PendingGraceTest`, `PasswordBrowserTest`, and
`UsabilityTest` for child scoping and removal of obsolete presentation steps.

## Validation

Final suite: 321 tests, zero failures/errors/skips. Baseline: 316.

- `./gradlew clean test build` — PASS.
- `./gradlew --no-daemon --no-build-cache --rerun-tasks clean test` — PASS.
- `./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean test` — PASS (321 tests).
- `./gradlew installDist verifyDistribution verifyDistributionArchives` — PASS. Verified runtime jars: BC 1.86, Totipo core/storage 0.1.1, desktop 0.0.0-dev.
- `./gradlew verifyMavenBoundary` — PASS.
- `git diff --check` — PASS.

No dependency versions, Gradle wrapper, Maven repositories, verification metadata,
lockfiles, package-deps.json, or Nix/cache files were changed. No Nix cache regeneration.
U1–U3 lifecycle/security invariants and Alternatives/Heads/merge/storage semantics remain
unchanged. Nothing in `totipo-java` was modified.

## Manual operator checklist

Pending operator verification; these entries are not a claim of a completed visual review.

1. A conflicted token appears as one amber Conflict group.
2. Every semantic Alternative appears as one indented child, not one row per Head.
3. Child Alternatives are usable without resolving the conflict.
4. Each child has its own Show Code / Copy / Edit.
5. Revealing one child does not reveal the others.
6. Multiple child codes may remain explicitly revealed independently.
7. Resolve remains optional.
8. Search matching one child shows the whole conflict group.
9. Logical token count does not increase because Alternatives are shown.
10. Edit on a child directly edits that Alternative; no intermediate version chooser.
11. Resolve opens directly into the field-by-field resolver.
12. No preliminary Alternative-selection screen appears in the normal flow.
13. Resolver intro is short and user-facing.
14. Conflicting issuer/account values appear as vertical radio + read-only field rows.
15. Blank custom text row is on one line with its radio.
16. Clicking/typing the custom field selects it automatically.
17. Existing read-only value rows are selectable and not editable.
18. Agreed fields appear once.
19. Status says Active / Deleted.
20. Authenticator setup treats secret + algorithm + digits + period as one unit.
21. Existing setup choices never expose the secret.
22. Choosing an existing setup cannot mix its secret with another setup's parameters.
23. Custom setup lets the user deliberately supply a new secret/algorithm/digits/period.
24. Interacting with any custom-setup control selects the custom setup.
25. Switching away from custom setup clears abandoned custom secret.
26. Heads/revisions/causal IDs are absent from the normal resolver.
27. Cancel leaves the conflict usable and unchanged.
28. Successful resolution collapses naturally to an ordinary token after observation.
29. Conflict/partial/publication-uncertain handling remains correct.
30. Main window remains compact/list-oriented.

## Final working-tree evidence

Recorded after validation; untracked new files are listed by status and are not included
by plain `git diff --stat`. All U4 changes are intentionally unstaged/uncommitted.

`git status --short`:

```text
 M src/main/java/org/totipo/desktop/ui/MergeEditorPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenChoice.java
 D src/main/java/org/totipo/desktop/ui/TokenEditChoicePanel.java
 M src/main/java/org/totipo/desktop/ui/TokenEditorPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenRowPanel.java
 M src/main/java/org/totipo/desktop/ui/TotpDisplay.java
 M src/main/java/org/totipo/desktop/ui/VaultFrame.java
 M src/test/java/org/totipo/desktop/MergeControllerTest.java
 M src/test/java/org/totipo/desktop/MergeEditorTest.java
 M src/test/java/org/totipo/desktop/TextRoundTripTest.java
 M src/test/java/org/totipo/desktop/ui/GraceRevealTest.java
 M src/test/java/org/totipo/desktop/ui/PasswordBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/PendingGraceTest.java
 M src/test/java/org/totipo/desktop/ui/TokenEditingBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenKeyboardTest.java
 M src/test/java/org/totipo/desktop/ui/TokenSearchTest.java
 M src/test/java/org/totipo/desktop/ui/TotpCopyTest.java
 M src/test/java/org/totipo/desktop/ui/UsabilityTest.java
?? review/U4_CONFLICT_PRESENTATION_AND_RESOLUTION_REPORT.md
?? src/test/java/org/totipo/desktop/ui/U4ConflictGroupTest.java
?? src/test/java/org/totipo/desktop/ui/U4ResolverTest.java
```

`git diff --stat`:

```text
 .../org/totipo/desktop/ui/MergeEditorPanel.java    | 331 ++++++++++-----------
 .../org/totipo/desktop/ui/TokenBrowserPanel.java   | 160 +++++++---
 .../java/org/totipo/desktop/ui/TokenChoice.java    |   1 +
 .../totipo/desktop/ui/TokenEditChoicePanel.java    |  35 ---
 .../org/totipo/desktop/ui/TokenEditorPanel.java    |   7 +-
 .../java/org/totipo/desktop/ui/TokenRowPanel.java  |  21 +-
 .../java/org/totipo/desktop/ui/TotpDisplay.java    |  14 +-
 .../java/org/totipo/desktop/ui/VaultFrame.java     |   5 +-
 .../org/totipo/desktop/MergeControllerTest.java    |  25 +-
 .../java/org/totipo/desktop/MergeEditorTest.java   | 159 +++++-----
 .../java/org/totipo/desktop/TextRoundTripTest.java |  10 +-
 .../org/totipo/desktop/ui/GraceRevealTest.java     |  10 +-
 .../org/totipo/desktop/ui/PasswordBrowserTest.java |   8 +-
 .../org/totipo/desktop/ui/PendingGraceTest.java    |   8 +-
 .../totipo/desktop/ui/TokenEditingBrowserTest.java |  62 ++--
 .../org/totipo/desktop/ui/TokenKeyboardTest.java   |   6 +-
 .../org/totipo/desktop/ui/TokenSearchTest.java     |   2 +-
 .../java/org/totipo/desktop/ui/TotpCopyTest.java   |   4 +-
 .../java/org/totipo/desktop/ui/UsabilityTest.java  |   3 +-
 19 files changed, 437 insertions(+), 434 deletions(-)
```

HEAD remains `652efea94fb175330f7a9d0aa160b073bbbf6864`; no staged changes, commits, tags,
releases, or publication actions were made.
