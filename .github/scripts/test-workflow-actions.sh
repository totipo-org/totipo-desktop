#!/bin/sh
# Disposable negative fixtures for the offline action review boundary.
set -eu
guard=$(pwd)/.github/scripts/verify-workflow-actions.sh
fixtures=$(mktemp -d)
trap 'rm -rf "$fixtures"' EXIT HUP INT TERM
mkdir "$fixtures/workflows"
cp .github/workflows/ci.yml "$fixtures/workflows/ci.yml"
sh "$guard" "$fixtures/workflows"

reject() {
    printf '%s\n' "$1" > "$fixtures/workflows/extra.yaml"
    if sh "$guard" "$fixtures/workflows" > "$fixtures/output" 2>&1; then
        echo "FAIL: accepted fixture: $1" >&2
        exit 1
    fi
}
reject '      - uses: actions/checkout@v7'
reject '      - uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b0 # v7.0.1'
reject '      - uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.0'
reject '      - uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683 # v4.2.2'
reject '      - uses: other/action@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1'
reject '      - uses: docker://alpine:latest'
reject '      - uses: ./local-action'
reject '      - {uses: actions/checkout@v7}'
reject '      - "uses": actions/checkout@v7'
reject '      - &action uses: actions/checkout@v7'
reject '      - uses: >'
reject '      - uses: "actions/checkout@v7"'
reject '      - "us\u0065s": actions/checkout@v7'
reject '      - <<: *action'
reject '      - uses:'
echo 'PASS: 15 unreviewed/unsupported fixtures rejected, including a second workflow'
