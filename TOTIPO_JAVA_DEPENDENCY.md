# Totipo Java released dependency

Desktop directly consumes `org.totipo:totipo-storage-nio:0.1.3` from Maven
Central. Its public dependency exposes `org.totipo:totipo-core:0.1.3`
transitively; desktop deliberately does not declare core directly. Core adds
`org.bouncycastle:bcprov-jdk18on:1.86` at runtime.

Upstream: https://github.com/totipo-org/totipo-java, source tag `v0.1.3`.
Release commit: `e2326aca5f5aac661d74925a30fcc57cd91014e8`.
That release targets Totipo Vault Format v1/r18 at spec commit
`4623a7e1718e23504903096c92332597057bd8f0` (a committed revision, not an r18 release tag).
Maven Central is the
consumer boundary; Java source/spec snapshots and the conformance corpus are
no longer part of the desktop source or package graph. Their provenance and
conformance validation belong to the Java release. Desktop does not independently
carry/pin those protocol artifacts and does not inherit application conformance
from Java's operation-scoped core/store claims.

The exact Java version is configured in `build.gradle.kts`. Gradle strict
dependency locks and SHA-256 verification metadata enforce the resolved
dependencies. `verifyMavenBoundary`, wired into `check`, requires external
modules and the transitive core relationship. Nix `package-deps.json`
separately pins package-build downloads. The S4 repin moves directly from 0.1.1 to
0.1.3; reviewed JAR/module/POM hashes match Maven Central bytes, with BC 1.86 and
unrelated dependencies unchanged. See the S4 report for validation of this pin.
Historical M4b Nix evidence applies to its earlier dependency pin.

Desktop uses the high-level `org.totipo` application APIs, including
`VaultSession` and `VaultState`, and the filesystem entry point
`org.totipo.storage.nio.NioTotipo`. It does not consume storage SPI or
implementation internals. No local source or Maven-local fallback is configured.
