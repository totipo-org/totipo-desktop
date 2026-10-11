#!/bin/sh
# Offline review guard, deliberately limited to plain block-style workflow YAML.
# Run from the repository root; an alternate workflow directory supports fixtures.
# Review upstream release notes, tag resolution and exact-SHA action metadata before
# changing this allowlist. Runtime labels below describe that reviewed metadata;
# this check does not fetch upstream or discover newer releases.
set -eu
workflow_dir=${1:-.github/workflows}
test -d "$workflow_dir"
find "$workflow_dir" -type f \( -name '*.yml' -o -name '*.yaml' \) -exec awk '
function reject(message) {
    print FILENAME ":" FNR ": " message > "/dev/stderr"
    failed = 1
}
BEGIN {
    # actions/checkout v7.0.1: JavaScript, runs.using=node24 (main and post).
    reviewed["actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1"] = 1
    # cachix/install-nix-action v31.11.1: composite, Bash; no nested uses.
    reviewed["cachix/install-nix-action@13d8dd58da0234aa297dedd986986ccb8e7f3e24 # v31.11.1"] = 1
}
/^[[:space:]]*#/ { next }
{
    # Fail closed on alternate YAML spellings that a line guard cannot resolve.
    # Keep workflows simple or extend verification with a reviewed parser.
    if ($0 ~ /[\173\175\[\]]/ || $0 ~ /(^|[[:space:]])[&*!]/ ||
        $0 ~ /^[[:space:]]*(-[[:space:]]*)?["\047?]/ ||
        $0 ~ /^[[:space:]]*<</)
        reject("unsupported workflow syntax; review the static guard")
    if ($0 ~ /uses["\047]?[[:space:]]*:/) {
        value = $0
        if (value !~ /^[[:space:]]*(-[[:space:]]+)?uses:[[:space:]]+/) {
            reject("uses must be a plain block-style key")
            next
        }
        sub(/^[[:space:]]*(-[[:space:]]+)?uses:[[:space:]]+/, "", value)
        sub(/[[:space:]]+$/, "", value)
        if (!(value in reviewed)) reject("unreviewed action SHA/release: " value)
        count++
    }
}
END {
    if (!count) reject("no reviewed action uses found")
    exit failed
}' {} +
echo 'PASS: workflow actions match reviewed SHA/release/runtime inventory'
