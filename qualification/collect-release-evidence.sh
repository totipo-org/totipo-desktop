#!/usr/bin/env bash
# Invoked by the flake app with exact same-derivation output/runtime paths.
set -euo pipefail
export LC_ALL=C
umask 077

if (( $# < 4 || $# > 5 )); then
    printf 'Usage: collect-release-evidence PACKAGE PORTABLE JDK SOURCE_JSON [NEW_DIRECTORY]\n' >&2
    exit 2
fi
package=$(realpath -e -- "$1")
portable=$(realpath -e -- "$2")
managed_jdk=$(realpath -e -- "$3")
source_json=$(realpath -e -- "$4")
destination=${5:-"build/release-evidence/$(date -u +%Y%m%dT%H%M%SZ)"}
mkdir -p -- "$(dirname -- "$destination")"
# Refuse overwrites, including a prior failed attempt. Each run is independent.
mkdir -- "$destination"
evidence=$(realpath -e -- "$destination")
scratch=$(mktemp -d)
exec 3>&1 4>&2
finish() {
    local result=$1
    trap - EXIT
    if (( result == 0 )); then
        printf 'Artifact evidence verification: PASS\nHuman Nix gate execution/reuse: not established by this collector\nHuman GUI smoke: PENDING\nPlatform qualification: UNQUALIFIED\n' > "$evidence/result.txt"
    else
        printf 'Artifact evidence verification: FAIL (exit %s)\nHuman GUI smoke: PENDING\nPlatform qualification: UNQUALIFIED\n' "$result" > "$evidence/result.txt"
    fi
    (cd "$evidence" && find . -type f -print0 | sort -z | xargs -0 sha256sum) > "$scratch/SHA256SUMS"
    mv -- "$scratch/SHA256SUMS" "$evidence/SHA256SUMS"
    rm -rf -- "$scratch"
    printf 'Evidence: %s\n' "$evidence" >&3
    if (( result != 0 )); then
        tail -n 20 "$evidence/verification.log" >&4
    fi
    cat "$evidence/result.txt" >&3
    exit "$result"
}
trap 'finish "$?"' EXIT
exec > "$evidence/verification.log" 2>&1

version=$(cat "$package/share/doc/totipo-desktop/VERSION")
printf '%s\n' "$version" | grep -Eq '^[A-Za-z0-9][A-Za-z0-9._+-]*$'
zip_name="totipo-desktop-$version.zip"
zip="$portable/$zip_name"
test -f "$zip"
cp -- "$portable/portable.sha256" "$portable/nix-deployment.sha256" "$evidence/"
cp -- "$source_json" "$evidence/source.json"
cp -- "$package/bin/totipo-desktop" "$evidence/nix-launcher.txt"
cp -- "$package/share/applications/totipo-desktop.desktop" "$evidence/desktop-entry.txt"
{
    printf 'Recorded UTC: %s\nVersion: %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$version"
    printf 'Nix deployment: %s\nPortable output: %s\nManaged JDK: %s\n' "$package" "$portable" "$managed_jdk"
    printf 'ZIP: %s\nZIP bytes: %s\n' "$zip" "$(stat -c %s -- "$zip")"
    printf 'Collector SHA-256: '
    sha256sum -- "${BASH_SOURCE[0]}"
    printf 'Kernel/architecture: '
    uname -srmo
    printf 'Evidence filesystem: '
    stat -f -c %T -- "$evidence"
    printf 'This collector does not launch the GUI, run the Nix gate or establish fresh tests/reproducibility.\n'
} > "$evidence/summary.txt"
if [[ -f /etc/os-release ]]; then
    cp -- /etc/os-release "$evidence/os-release.txt"
fi
nix --version > "$evidence/nix-version.txt"
nix path-info --derivation "$package" > "$evidence/derivation.txt"
nix path-info --derivation "$portable" > "$scratch/portable-derivation.txt"
cmp -- "$evidence/derivation.txt" "$scratch/portable-derivation.txt"
nix-store -qR "$package" | sort > "$evidence/runtime-closure.txt"
nix-store --query --hash "$package" > "$evidence/nix-deployment-nar-hash.txt"
nix-store --query --hash "$portable" > "$evidence/portable-nar-hash.txt"
# Confirm diagnostics use the runtime selected by the actual installed wrapper.
grep -Fx -- "export JAVA_HOME='$managed_jdk'" "$package/bin/totipo-desktop"
grep -F -- '-XX:+DisableAttachMechanism' "$package/lib/totipo-desktop/bin/totipo-desktop-unwrapped"
env -u JAVA_TOOL_OPTIONS -u JDK_JAVA_OPTIONS -u _JAVA_OPTIONS \
    "$managed_jdk/bin/java" -XshowSettings:properties -version > "$evidence/java-runtime.txt" 2>&1

# Original same-build manifest and transport bytes, including the recorded size.
head -n 2 "$evidence/portable.sha256" > "$evidence/zip.sha256"
grep -Eq "^[0-9a-f]{64}  totipo-desktop-[A-Za-z0-9._+-]+[.]zip$" <(tail -n 1 "$evidence/zip.sha256")
(cd "$portable" && sha256sum --strict --check "$evidence/zip.sha256")
expected_size=$(sed -n '1s/^# ZIP size=\([0-9][0-9]*\) bytes$/\1/p' "$evidence/portable.sha256")
test "$(stat -c %s -- "$zip")" = "$expected_size"
unzip -t "$zip"
mkdir -- "$scratch/extracted"
unzip -q "$zip" -d "$scratch/extracted"
tail -n +3 "$evidence/portable.sha256" > "$scratch/extracted.sha256"
test "$(wc -l < "$scratch/extracted.sha256")" -eq 11
(cd "$scratch/extracted" && sha256sum --strict --check "$scratch/extracted.sha256")
sed 's/^[0-9a-f]\{64\}  //' "$scratch/extracted.sha256" | sort > "$scratch/expected-inventory.txt"
(cd "$scratch/extracted" && find . -type f -printf '%P\n' | sort) > "$scratch/actual-inventory.txt"
cmp -- "$scratch/expected-inventory.txt" "$scratch/actual-inventory.txt"
cp -- "$scratch/actual-inventory.txt" "$evidence/extracted-inventory.txt"
cmp -- "$scratch/extracted/totipo-desktop-$version/VERSION" "$package/share/doc/totipo-desktop/VERSION"

# Compare actual Nix-installed original JARs with those extracted from this ZIP.
sed -n "s@  totipo-desktop-$version/lib/@  lib/@p" "$evidence/portable.sha256" > "$evidence/payload.sha256"
test "$(wc -l < "$evidence/payload.sha256")" -eq 4
test "$(find "$package/lib/totipo-desktop/lib" -type f | wc -l)" -eq 4
(cd "$package/lib/totipo-desktop" && sha256sum --strict --check "$evidence/payload.sha256")
(cd "$package" && sha256sum --strict --check "$evidence/nix-deployment.sha256")
sed 's/^[0-9a-f]\{64\}  //' "$evidence/nix-deployment.sha256" | sort > "$scratch/expected-deployment.txt"
(cd "$package" && find . -type f -print | sort) > "$scratch/actual-deployment.txt"
cmp -- "$scratch/expected-deployment.txt" "$scratch/actual-deployment.txt"
printf 'PASS: original ZIP size/hash, extraction/inventory, installed JAR equality, deployment manifest and managed runtime.\n'
