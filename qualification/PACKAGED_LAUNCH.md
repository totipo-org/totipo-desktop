# Packaged launch qualification — UNQUALIFIED until executed

Use the reviewed candidate commit and immutable ZIP. Record the evidence fields
in QUALIFICATION.md. Run these procedures on a physical native desktop; headless
entry-point loading and Xvfb do not establish native PASS. Use disposable vaults.

## Obtain and identify the artifacts (maintainer, Nix environment)

After source/input freeze, run the single automatic gate:

```sh
nix flake check
```

To obtain its outputs for inspection/native launch (materialization, not another
gate or independent build), from that same checkout:

```sh
nix build . .#packages.x86_64-linux.default.portable --no-link --print-out-paths
```

Use the actual qualified system in the attribute, e.g. `aarch64-linux` only when
that architecture is being tested. Of the printed store paths, the one with
`bin/totipo-desktop` is the Nix deployment; the `portable` output contains the canonical `totipo-desktop-VERSION.zip`,
`portable.sha256` and `nix-deployment.sha256`. Record both store paths/derivation
identity. These outputs come from one Gradle application build. Retain the ZIP
and manifests as candidate evidence; do not substitute a later local rebuild.

A local Gradle development preview is `gradle verifyDistribution`; this verifies
its final ZIP and writes `build/reports/distribution/portable.sha256`. To use a
separately produced ZIP as the canonical candidate, compare every JAR hash with
the actual Nix output as below. Any mismatch blocks evidence sharing/release;
investigate before proceeding. No independent reproducibility is inferred.

## Portable ZIP — Linux native route

Select the actual version, paths, and externally supplied Java 25 runtime. Keep
the extraction outside the checkout and include spaces. Commands below are Bash;
`jar` is provided by the existing JDK, and extraction restores the Unix executable
bit explicitly (JDK jar does not preserve ZIP Unix modes).

```sh
ZIP='/absolute/path/to/totipo-desktop-VERSION.zip'
IDENTITIES='/absolute/path/to/portable.sha256'
EXTRACT='/absolute/disposable/path/portable candidate'
JAVA_HOME='/absolute/path/to/tested/java-25'
export JAVA_HOME
mkdir -p "$EXTRACT"
(cd "$(dirname "$ZIP")" && head -n 2 "$IDENTITIES" | sha256sum --check)
wc -c < "$ZIP" # Compare with the manifest's ZIP size comment.
(cd "$EXTRACT" && "$JAVA_HOME/bin/jar" xf "$ZIP")
(cd "$EXTRACT" && tail -n +3 "$IDENTITIES" | sha256sum --check)
DIST="$EXTRACT/totipo-desktop-VERSION"
chmod +x "$DIST/bin/totipo-desktop"
"$JAVA_HOME/bin/java" -XshowSettings:properties -version
(cd /tmp && env -u JAVA_OPTS -u TOTIPO_DESKTOP_OPTS -u CLASSPATH -u JAVA_TOOL_OPTIONS -u JDK_JAVA_OPTIONS \
  JAVA_HOME="$JAVA_HOME" PATH=/usr/bin:/bin "$DIST/bin/totipo-desktop")
```

- [ ] Verify ZIP size and SHA-256 against retained identities before extraction;
      extract into an empty directory. Inspect full inventory against manifest,
      with no extra files. Check both launcher files, notices and embedded icons.
- [ ] Verify Unix launch after a normal OS archive extraction too (including its
      executable permission). Record extractor/version and any chmod requirement.
- [ ] Execute outside checkout without Gradle or development classpath; check
      Java vendor/build, Java 25 compatibility, attach-disabled option and no
      host-path leakage. Environment overrides above are cleared intentionally.
- [ ] Run the common scenarios below and every applicable native checklist.

Windows/macOS initially remain UNQUALIFIED. Later runs use the original ZIP hash,
never a rebuild; run the BAT or Unix launcher as appropriate, record native
extraction/runtime/filesystem/UI results and append dated evidence. Failed later
qualification does not authorize replacing the original artifact or tag.

## Nix package — native NixOS route

```sh
NIX_PACKAGE='/nix/store/actual-qualified-totipo-desktop-output'
IDENTITIES='/absolute/path/to/portable.sha256'
NIX_IDENTITIES='/absolute/path/to/nix-deployment.sha256'
# Explicit comparison with canonical ZIP JAR hashes, including application JAR:
sed -n 's@  totipo-desktop-[^/]*/lib/@  lib/@p' "$IDENTITIES" > /tmp/totipo-payload.sha256
(cd "$NIX_PACKAGE/lib/totipo-desktop" && sha256sum --check /tmp/totipo-payload.sha256)
(cd "$NIX_PACKAGE" && sha256sum --check "$NIX_IDENTITIES")
cat "$NIX_PACKAGE/bin/totipo-desktop"
cat "$NIX_PACKAGE/share/applications/totipo-desktop.desktop"
# Use the JAVA_HOME printed in the wrapper to record managed runtime identity:
# /nix/store/actual-jdk/bin/java -XshowSettings:properties -version
(cd /tmp && env -u JAVA_HOME -u JAVA_OPTS -u TOTIPO_DESKTOP_OPTS -u CLASSPATH -u JAVA_TOOL_OPTIONS -u JDK_JAVA_OPTIONS \
  PATH=/nonexistent "$NIX_PACKAGE/bin/totipo-desktop")
```

- [ ] Record actual managed JDK store path/version and runtime closure identity;
      verify wrapper selects it despite unset caller JAVA_HOME and minimal PATH.
- [ ] Confirm attach-disabled option in installed unwrapped launcher and actual
      JVM arguments (e.g. `/proc/PID/cmdline` on Linux; do not retain secrets).
- [ ] Inspect original JAR inventory, license bytes and absence of development
      material; installCheck automates these checks but is not a native PASS.
- [ ] Inspect desktop entry and hicolor icons, then install the reviewed output
      through the maintainer's usual NixOS/profile integration. For a temporary
      profile installation: `nix profile add "$NIX_PACKAGE"`; record profile and
      activation/relogin steps, and remove that specific entry afterward if desired.
- [ ] Launch Totipo from the native NixOS graphical application menu, confirm icon,
      no terminal, correct process/JDK and installed executable. A direct path
      launch alone does not qualify desktop integration.
- [ ] Run ordinary create/open/token/TOTP/lock/reopen/exit functionality through
      this installed launcher, plus the route-specific native checks below.

## Common scenarios and exact-JAR filesystem harness

Use [GUI_SMOKE.md](GUI_SMOKE.md), [CLIPBOARD_SMOKE.md](CLIPBOARD_SMOKE.md) and
[ACCESSIBILITY_SMOKE.md](ACCESSIBILITY_SMOKE.md), retaining per-item outcomes.
Common application scenarios cover vault creation/opening, encrypted publication
and reopen, token add/edit/delete/TOTP, conflicts/resolution, refresh/provider
observation, lock/session retirement, keyboard interaction and clipboard policy.
Existing headless tests supplement these scenarios; they do not replace native
checks. Record unsupported controlled scenarios explicitly.

Compile only the separate harness against the extracted JARs, then execute it
outside the checkout. No application rebuild or development classes participate:

```sh
DIST='/absolute/path/to/extracted/totipo-desktop-VERSION'
HARNESS='/absolute/disposable/path/qualification-classes'
TEST_ROOT='/absolute/private/disposable/filesystem-root'
mkdir -p "$HARNESS" "$TEST_ROOT"
javac --release 25 -cp "$DIST/lib/*" -d "$HARNESS" \
  src/qualification/java/org/totipo/qualification/FilesystemQualification.java
(cd /tmp && java -XX:+DisableAttachMechanism -cp "$HARNESS:$DIST/lib/*" \
  org.totipo.qualification.FilesystemQualification "$TEST_ROOT")
```

Use the recorded tested JDK's `javac` and `java`. On Windows use `;` rather than
`:` in the classpath. Alternatively, in the activated development environment:
`gradle filesystemQualification -PqualificationDistribution="$DIST" -PqualificationRoot="$TEST_ROOT"`.
That task compiles the external harness but its execution classpath contains only
harness output and the supplied distribution JARs. Keep harness classes/source
outside the release. Record harness source revision/hash, runtime, commands,
filesystem/provider and cleanup result. Repeat on each claimed filesystem.
For the managed Nix runtime use its exact `java` and the equivalent Nix `lib/`
path; do not transfer runtime/provider PASS solely on JAR equality.

Where all application/dependency JAR hashes match, cite the common application
results by evidence ID rather than repeating the full suite. Each route still
needs extraction/installation, actual launcher, runtime, filesystem, GUI,
keyboard/accessibility, clipboard and shutdown evidence on its named environment.
No OS/runtime/filesystem/desktop PASS transfers automatically. Record any reuse
and its justification, and exercise ordinary functionality through both launchers.

No application process may linger after exit, and installation directories must
never become vault storage. Reused Nix output is not an independent build. A
separate deliberate comparison is required for any reproducibility claim.
