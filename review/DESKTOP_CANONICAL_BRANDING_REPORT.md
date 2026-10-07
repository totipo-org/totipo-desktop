# Desktop canonical branding integration

## Starting state and provenance

- Implementation repository: `/home/niki/Sources/totipo-desktop`.
- Starting branch: `main`.
- Starting HEAD: `547a905a94243bb8aea300de7374f7c0643537de`.
- Starting status: only `?? review/DESKTOP_CANONICAL_BRANDING_REPORT.md` (the previous blocker report).
- Authoritative repository: https://github.com/totipo-dev/totipo-spec
- Exact fetched revision: `fbae625f2cad6c89a1b5555de2f2d75d890bb682`.
- Temporary checkout: `/tmp/totipo-branding.nhk1OR/totipo-spec`, outside Desktop.

Acquisition commands (successful):

```sh
tmp_branding=$(mktemp -d /tmp/totipo-branding.XXXXXX)
git clone --depth 1 https://github.com/totipo-dev/totipo-spec.git "$tmp_branding/totipo-spec"
git -C "$tmp_branding/totipo-spec" rev-parse HEAD
```

Inspected `design/icons/README.md`, `manifest.json`, `SHA256SUMS`,
`totipo-app-icon.svg`, `totipo-mark.svg`, `totipo-mark-white.svg`, the
`reference/` file inventory and preview image, and the `platforms/` inventory
including Linux's README and hicolor hierarchy. The reference PNGs and Linux
exports have matching checksums. All 84 entries in SHA256SUMS verified.
The canonical mark has one circular timer ring, exactly three timer ticks,
exactly three token dots, and no lock/shackle. No artwork was redrawn,
regenerated, recolored, or substituted.

## Copied assets and window integration

Copied these six Linux exports byte-for-byte into application resources:

| Source under `design/icons/` | Desktop resource |
| --- | --- |
| `platforms/linux/hicolor/16x16/apps/totipo.png` | `src/main/resources/org/totipo/desktop/icons/totipo-16.png` |
| `platforms/linux/hicolor/32x32/apps/totipo.png` | `src/main/resources/org/totipo/desktop/icons/totipo-32.png` |
| `platforms/linux/hicolor/48x48/apps/totipo.png` | `src/main/resources/org/totipo/desktop/icons/totipo-48.png` |
| `platforms/linux/hicolor/64x64/apps/totipo.png` | `src/main/resources/org/totipo/desktop/icons/totipo-64.png` |
| `platforms/linux/hicolor/128x128/apps/totipo.png` | `src/main/resources/org/totipo/desktop/icons/totipo-128.png` |
| `platforms/linux/hicolor/256x256/apps/totipo.png` | `src/main/resources/org/totipo/desktop/icons/totipo-256.png` |

Also copied `platforms/linux/hicolor/` to `packaging/icons/hicolor/`:
PNG sizes 16, 24, 32, 48, 64, 128, 256, 512 plus
`scalable/apps/totipo.svg`. Total: 15 copied files. Other platform assets,
reference previews, and symbol-only SVGs are not bundled.

`ApplicationIcons` loads PNGs through `Class#getResourceAsStream`, checks
square dimensions, and caches the six-image list. `ShellFrame`, the sole
production JFrame, calls `setIconImages(...)` before packing. No resource is
loaded from a filesystem or sibling checkout at runtime.

Inspected ownership in `ShellFrame.aboutVault`, `DirectoryPicker`,
`PasswordPrompt`, `TokenEditDialog`, `PasswordChangeDialog`, and
`TokenBrowserPanel`'s task dialogs. Production dialogs and JOptionPane/file
chooser calls use application owners, so normal icon inheritance is retained.
No additional independent production top-level windows need assignment.

## Packaging

The existing Nix package now includes `packaging/icons` in its filtered
source, installs the canonical hicolor tree under `$out/share/icons/hicolor`,
and gives its existing Totipo desktop entry `Icon=totipo`, as prescribed by
the canonical Linux README. Existing install checks now verify the desktop
entry icon name and compare every installed icon with its source bytes.
Package identity, metadata, launchers, dependencies, and framework stay intact.

Gradle distributions contain the six window PNGs in the application JAR.
The existing `verifyDistribution` task additionally reads every packaged PNG
and checks its dimensions. Existing archive verification checks that ZIP/TAR
contents match `installDist`. No sibling dependency or new packaging framework
was added.

## Colors and scope

`BrandPalette` centralizes the four canonical artwork stops:
`#46FB70`, `#08D267`, `#028B55`, `#026344`.
The artwork retains its original gradient. Existing Swing brand accents use
exact `#026344` on light surfaces and `#08D267` on dark surfaces. Existing
contrast helpers continue to adapt focus/foreground colors where needed.

Brand green is used in existing primary-action fills (landing open/create,
Add/review/save flows, conflict review/save, and password change), selected
row borders/subtle selected surfaces, existing focus accents, and the
nonurgent countdown ring. No action roles or layouts changed. This uses the
existing styling vocabulary rather than adding button delegates or a Look &
Feel. Not all buttons are green.

Cancel, Back, Copy, Show/Hide, secondary/quiet actions, menus, labels, inputs,
and normal controls retain their previous native or neutral treatment.
No global UIManager color override was introduced. Informational color keeps
the prior platform-derived blue treatment separately from brand accents.
The existing semantic success color is also separate.

Warning amber, error/danger red, validation text, dangerous-action styling,
and red final delete confirmation are unchanged. The countdown still switches
to warning amber when urgent. Delete/remove actions do not acquire a green
fill. No token-list, vault, add/edit, dialog, protocol, API, or preference
redesign was performed. Neither totipo-spec nor totipo-java was modified.

## Tests and validation

Added `CanonicalBrandingTest` for all six classpath resources and loaded
sizes, the four canonical palette constants, and separation from warning,
danger, informational, and semantic success colors. Existing style tests
cover light/dark contrast, primary versus secondary roles, destructive
styling, selection, and countdown urgency. No screenshot/pixel tests added.
Extended Gradle distribution and Nix install checks as described above.

Validation commands:

```sh
# From /tmp/totipo-branding.nhk1OR/totipo-spec:
sha256sum -c design/icons/SHA256SUMS
# From totipo-desktop:
./gradlew build verifyDistributionArchives
git diff --check
git status --short
git -C /tmp/totipo-branding.nhk1OR/totipo-spec status --short
command -v nix
command -v Xvfb
env | rg '^(DISPLAY|WAYLAND_DISPLAY)='
```

Checksum verification passed. The first Gradle run passed; a second full run
was started after adding the packaged-image checks and updating the style
comment. Final Gradle result is recorded below. Gradle emitted a fontconfig
configuration warning; tests remained functional.

Additional byte verification used the following Python script with
`/nix/store/60m4rxhg2fldqaak400c0lry96ijrzqn-python3-3.13.13/bin/python3`:

```python
from pathlib import Path
import hashlib
s = Path('/tmp/totipo-branding.nhk1OR/totipo-spec/design/icons')
for p in sorted(Path('packaging/icons/hicolor').rglob('*')):
    if p.is_file():
        assert p.read_bytes() == (s/'platforms/linux/hicolor'/p.relative_to('packaging/icons/hicolor')).read_bytes()
for p in sorted(Path('src/main/resources/org/totipo/desktop/icons').glob('*.png')):
    assert p.read_bytes() == (s/'reference/png'/p.name).read_bytes()
print('PASS: all 15 copied assets are byte-identical to canonical exports')
```

Result: passed. Source checkout status is clean.

Nix build/install checks could not execute: `command -v nix` returned exit 1,
with no executable found in the available environment or installed store.
Graphical/manual launch observations are unavailable: no DISPLAY or
WAYLAND_DISPLAY is set and no Xvfb executable is present. The canonical export
preview was visually inspected and matches the specified mark; this is not a
claim of live window-manager/UI verification. Automated Swing tests run
headlessly. Live icon inheritance/title-bar/dock rendering remains a manual
review item on a graphical desktop.

## Final results and working tree

Final `./gradlew build verifyDistributionArchives`: passed (exit 0), including
`check`, Maven dependency boundary, Java 17 bytecode, `installDist`, packaged
PNG validation, and ZIP/TAR archive verification. Final run: 44 seconds,
14 tasks (11 executed, 3 up-to-date). JUnit XML: 467 tests in 66 classes,
0 failures, 0 errors, 0 skipped.

Final `git diff --check`: passed (exit 0).

Final `git status --short`:

```text
 M build.gradle.kts
 M package.nix
 M src/main/java/org/totipo/desktop/ui/DesktopStyle.java
 M src/main/java/org/totipo/desktop/ui/ShellFrame.java
?? packaging/icons/
?? review/DESKTOP_CANONICAL_BRANDING_REPORT.md
?? src/main/java/org/totipo/desktop/ui/ApplicationIcons.java
?? src/main/java/org/totipo/desktop/ui/BrandPalette.java
?? src/main/resources/
?? src/test/java/org/totipo/desktop/ui/CanonicalBrandingTest.java
```

All implementation changes and this replacement report remain uncommitted.
No commit, tag, push, publication, or edits to the source/specification or
totipo-java repositories were performed.

