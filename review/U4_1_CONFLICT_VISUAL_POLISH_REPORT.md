# U4.1 — Conflict visual hierarchy and resolver grouping polish

## Baseline

- Baseline commit: `92bee71468588f269fc1b3e8a2b32dd343922786` (committed U4).
- `git status --short` was empty before any edits.
- `./gradlew clean test build` passed before any edits: **321 tests**, zero failures, errors, or skips.
- Parent conflict group: hard-coded cream full fill, amber left/bottom border, warning text, and Resolve button.
- Alternative children: normal list color mixed with hard-coded amber (visually brown on dark surfaces), additional amber left/bottom border, and individual warning glyphs.
- Selection: conflict children retained their conflict fill and added the ordinary two-pixel selection outline. Ordinary selected rows used a subtle List-derived tint; conflicting rows did not share that tint.
- Resolver: flat label/control pairs for Issuer, Account, Status, and Authenticator Setup. Custom setup controls followed as a separate unlabeled block. Conflicting text choices were already vertical and custom selection behavior was already implemented.
- Resolver default/minimum: **760×720 / 640×580**, fitted to available screen bounds by the existing frame. One outer vertical scroll pane, horizontal scrolling disabled, fixed footer containing Cancel/Save.

## Presentation changes

### Conflict list

The parent and unselected Alternative children now use `UIManager`'s `List.background`. The cream parent fill and amber/brown child fills are removed. Each child uses the same surface, ordinary separator, padding, and selection treatment as an ordinary row. The existing 16-pixel indentation and four-pixel inter-child gap remain; there are no nested child cards.

One **three-pixel amber left edge on the parent** is the primary conflict accent. There is no amber bottom edge or child edge. The transparent, compact header has bold `⚠ Conflict` in `List.foreground` and a compact Resolve button. It does not receive token selection treatment or token actions. Warning text and accessible warning names remain, so color is not the sole conflict signal.

The accent derives from the standard Look & Feel warning palette (`OptionPane.warningDialog.titlePane.background`, then `nimbusOrange`) blended equally with JDK's semantic `Color.ORANGE`. If neither warning key exists, it uses `Color.ORANGE`. This fallback is an edge color only, never a surface fill. No platform-specific beige/brown RGB values remain in the conflict styling. Header text uses the normal List foreground for compatibility with light and dark List surfaces.

Selected children use the unchanged ordinary selection treatment: a 1/12 blend of `List.selectionBackground` into `List.background`, plus the existing two-pixel selection outline. Selection changes only the row. The parent's amber edge and conflict warning remain intact. Accessible SELECTED state notifications remain unchanged. Red near-expiry countdown presentation remains unchanged.

### Resolver

Four lightweight standard Swing titled sections now contain the decisions:

- **Issuer:** vertical distinct read-only choice rows plus the editable custom row when conflicting; one editable field when agreed.
- **Account:** the same structure, with no radios invented for an agreed field.
- **Status:** existing Active/Deleted radios stacked vertically in one section. Selection and ACTIVE/TOMBSTONED mapping are unchanged.
- **Authenticator Setup:** all existing setup choices, their metadata, the custom setup radio, and Secret/Algorithm/Digits/Period controls in one section. There are no independent algorithm/digits/period resolver sections.

Each section uses a Look & Feel-derived `TitledBorder` and eight-pixel inner padding. Section titles replace the former duplicate field labels. Sections have a 12-pixel gap; text/status/setup choices have eight-pixel gaps. The existing 20-pixel outer padding and short intro remain. Custom setup controls retain their existing full-width equal toggles and Long spinner.

Existing/custom setup selection, enabled state behavior, secret clearing, and custom auto-selection are unchanged. Controls remain visible with stable geometry; selecting an existing setup does not collapse the custom controls.

The default/minimum sizes remain **760×720 / 640×580**. One outer body scroll pane remains, with vertical scrolling as needed and no horizontal scrollbar. Cancel/Save remain outside that scroll pane. Sections permit their width to shrink to the viewport rather than allowing a long setup identity to impose its minimum width on the body. Existing text choices remain selectable literal JTextFields; their full values are retained without HTML interpretation or dialog widening. Tests exercise long values at both sizes. Large conflicts can scroll vertically.

### Accessibility

Every titled section has an explicit accessible name and description. Existing radio names and field accessible names are retained. Custom text fields also expose their existing instruction as an accessible description (for example, `Enter a different issuer value`). Parent/child conflict warnings, Alternative identities, Resolve, Show Code/Copy/Edit, and selected-state reporting are retained. The header is explicitly non-focusable; Resolve remains independently reachable.

## Scope and files

Production files changed:

- `src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java`: normal parent surface, one derived amber edge, compact header and Resolve.
- `src/main/java/org/totipo/desktop/ui/TokenRowPanel.java`: ordinary child background/separator/selection styling.
- `src/main/java/org/totipo/desktop/ui/MergeEditorPanel.java`: titled grouping, spacing, viewport sizing, explicit accessible section descriptions.

Tests changed:

- `src/test/java/org/totipo/desktop/ui/U4ConflictGroupTest.java`: three new tests for parent/child hierarchy and actions, surface/selection relationships, warning palette derivation/fallback, and light/dark List palette relationships.
- `src/test/java/org/totipo/desktop/ui/U4ResolverTest.java`: two new tests for titled sections, vertical associated text rows, agreed Account, atomic setup containment, accessible grouping, footer/scroll hierarchy, and long literal Issuer/Account values at default/minimum sizes.
- `src/test/java/org/totipo/desktop/ui/TokenRowSelectionTest.java`: replace assertions requiring the removed conflict tint/child edge with ordinary surface/selection relationships; retain warning, accessible selection, foreground, and stable geometry assertions. Parent accent preservation is tested in U4ConflictGroupTest.

All existing U4 semantic tests remain. No semantic assertion was relaxed to accommodate grouping. The unchanged merge/editor/controller/write tests cover no implicit winner, deduplication, status mapping, setup equality/atomicity, custom behavior, secret hygiene, AdditionalConflict, PartialResolution, PublicationUncertain, retry, and stale/freshness handling. Existing browser/reveal/search/count/action tests remain in the full suite.

**No protocol, storage, conflict-resolution, Alternatives/Heads, merge, TOTP, edit/create, lifecycle, clipboard, dependency, Maven boundary, or Nix architecture changes.** One logical group per token and one child per semantic Alternative remain. No publishing, tagging, releasing, committing, or Nix dependency-cache regeneration was performed.

## Validation

Baseline: **321 tests**. Final: **326 tests**, zero failures, errors, or skips.

- `./gradlew clean test build` — PASS.
- `./gradlew --no-daemon --no-build-cache --rerun-tasks clean test` — PASS (326 tests).
- `./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean test` — PASS (326 tests).
- `./gradlew installDist verifyDistribution verifyDistributionArchives` — PASS.
- `./gradlew verifyMavenBoundary` — PASS.
- `git diff --check` — PASS.

An intermediate focused run exposed a test-fixture mistake: removing a UIManager override exposes its underlying Look & Feel default. The fallback test now explicitly removes and restores that default while exercising absent-key behavior. The subsequent full build passed.

## Manual operator checklist

Not executed here: automated tests establish structure, palette relationships, and behavior; they do not establish subjective color quality or operator readability. Review these on light and dark Look & Feels:

1. [ ] Conflict group no longer has cream background + brown child rows.
2. [ ] Conflict state remains obvious through one restrained amber accent.
3. [ ] Alternative rows visually match normal token rows.
4. [ ] Selecting a child does not erase the conflict indication.
5. [ ] Selection no longer creates a clash of amber/brown/bright-blue surfaces.
6. [ ] Conflict parent header is compact and clear.
7. [ ] Resolve dialog visually separates Issuer, Account, Status, and Authenticator Setup.
8. [ ] Each titled section reads as one independent decision.
9. [ ] Conflicting issuer choices are stacked vertically inside Issuer.
10. [ ] Agreed fields appear once in their own section.
11. [ ] Custom text row remains on one line with its radio.
12. [ ] Authenticator Setup remains one grouped semantic decision.
13. [ ] Custom setup controls remain together inside that group.
14. [ ] Resolver has one outer vertical scrolling body, not nested scroll panes.
15. [ ] Cancel/Save remain easy to reach.
16. [ ] Long values do not force horizontal scrolling.
17. [ ] No merge/resolution behavior changed.

## Final working-tree evidence

All U4.1 changes remain uncommitted. Working-tree snapshots recorded after validation follow. `git diff --stat` excludes the new untracked report.

`git status --short`:

```text
 M src/main/java/org/totipo/desktop/ui/MergeEditorPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenRowPanel.java
 M src/test/java/org/totipo/desktop/ui/TokenRowSelectionTest.java
 M src/test/java/org/totipo/desktop/ui/U4ConflictGroupTest.java
 M src/test/java/org/totipo/desktop/ui/U4ResolverTest.java
?? review/U4_1_CONFLICT_VISUAL_POLISH_REPORT.md
```

`git diff --stat`:

```text
 .../org/totipo/desktop/ui/MergeEditorPanel.java    |  28 ++++--
 .../org/totipo/desktop/ui/TokenBrowserPanel.java   |  19 +++-
 .../java/org/totipo/desktop/ui/TokenRowPanel.java  |  11 +--
 .../totipo/desktop/ui/TokenRowSelectionTest.java   |  15 +--
 .../org/totipo/desktop/ui/U4ConflictGroupTest.java | 105 +++++++++++++++++++++
 .../java/org/totipo/desktop/ui/U4ResolverTest.java | 105 ++++++++++++++++++++-
 6 files changed, 255 insertions(+), 28 deletions(-)
```

Final HEAD remains `92bee71468588f269fc1b3e8a2b32dd343922786`.
