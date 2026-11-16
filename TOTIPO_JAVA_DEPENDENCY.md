# Totipo Java released dependency

Desktop directly consumes `org.totipo:totipo-storage-nio:0.3.2` from canonical
[Maven Central](https://repo.maven.apache.org/maven2/org/totipo/totipo-storage-nio/0.3.2/).
It exposes exactly `org.totipo:totipo-core:0.3.2` transitively; desktop does not
declare core directly. Core's sole runtime dependency remains
`org.bouncycastle:bcprov-jdk18on:1.86`. Strict constraints, active locks and SHA-256
verification enforce these versions. Both Java modules retain Java 17 bytecode;
Desktop compilation and generic runtime support target Java 25.

[Java v0.3.2](https://github.com/totipo-org/totipo-java/releases/tag/v0.3.2)
identifies source `af11553659dd8bd4157064c7e3de27bed8980ac5`.
The annotated tag object is `8d40c864635c666ecd7642f0f06dace29315ca12`;
its peeled target matches that source. The exact v1/r19 spec pin remains
`cdb4e91be1c6d3704874b2b92457ffe7be5e9084`. Archive profile 1 is an application
format, separate from the vault protocol.

Canonical Central bytes downloaded independently of the Gradle cache:

| Artifact | SHA-256 |
| --- | --- |
| totipo-core-0.3.2.jar | `db35f01b51ba1777b3411306824559126179d868199c40281534b8b02da6bd98` |
| totipo-core-0.3.2.module | `a46eb72f8c00060244f67667dcd462bc4f3a557a9fa02eb687b2eeee1a681f74` |
| totipo-core-0.3.2.pom | `3f873b517e72f70bbcd5d47ee7df8c102ac0f1dc05ab23aaf23d42c481802cd7` |
| totipo-core-0.3.2-sources.jar | `044ea95e2d6eafe2b34f2accb2c959a12a5bdd008d350c9841fb75c9bac6c0d0` |
| totipo-storage-nio-0.3.2.jar | `c680ac4b09d68b2635b7bd4bfc3fb77b2519634df57c91b57a7b63045ea458bb` |
| totipo-storage-nio-0.3.2.module | `a9962bde6e871ad8aba84f193cddf5a44954dd415c388bd776ad503ce089f86b` |
| totipo-storage-nio-0.3.2.pom | `7bf48a433e23ba97a5db72586a0fa10bb1b297ae2a2254ade80054c198fd4ce9` |
| totipo-storage-nio-0.3.2-sources.jar | `14a691e74bcde1f5cddd5a5b70783b53bc9f85aa36890dd877e5126d607a6968` |

All published Java source files match the exact Git blobs at the release source.
All eight artifact hashes match its
[release preparation inventory](https://github.com/totipo-org/totipo-java/blob/af11553659dd8bd4157064c7e3de27bed8980ac5/review/JAVA_0_3_2_RELEASE_PREPARATION_REPORT.md).
POM and module metadata agree on the transitive versions; the packaged Totipo
license matches the release LICENSE byte for byte. BC bytes/license are unchanged.
No Maven Local, vendored implementation, composite build or unpublished dependency
participates. Java conformance evidence does not qualify Desktop platforms.

## Separate implementation and reviewed guidance pins

| Pin | Exact identity | Meaning |
| --- | --- | --- |
| Runtime artifact | `org.totipo:totipo-storage-nio:0.3.2` | Exposes core 0.3.2; BC remains 1.86. |
| Released implementation | `af11553659dd8bd4157064c7e3de27bed8980ac5` | Executable 0.3.2 source and archive APIs. |
| Existing reviewed operation guidance | `API_DESIGN.md` at `3b24b54becde0c93479c1fbd80ea0fbd2026e2a8` | Retained reviewed 0.2.0 operation/state clarification; not silently advanced. |
| Additional released archive guidance | `API_DESIGN.md` and `docs/BACKUP_ARCHIVE.md` at `af11553659dd8bd4157064c7e3de27bed8980ac5` | Actual capture, detached restore, cancellation, ownership and NIO recovery contracts absent from the older document. |

Read the existing exact
[reviewed API guidance](https://github.com/totipo-dev/totipo-java/blob/3b24b54becde0c93479c1fbd80ea0fbd2026e2a8/API_DESIGN.md)
and the released
[API design](https://github.com/totipo-org/totipo-java/blob/af11553659dd8bd4157064c7e3de27bed8980ac5/API_DESIGN.md)
and [archive contract](https://github.com/totipo-org/totipo-java/blob/af11553659dd8bd4157064c7e3de27bed8980ac5/docs/BACKUP_ARCHIVE.md).
Exact document SHA-256 values respectively:
`bd017168bf0dcb103c5880cff9d6d5b2f358c493fbc305e1eea0b0623ca81493`,
`bf168036f9ac18a9d1f58a4ce6f4a5af0c2755941af1de6f5f8afff61ae5b4b4`,
`6886c99a5e87f8ebfc3716c77aadb378e6c2cb24a51f110063ba2cceecbd9918`.
A guidance-only change never changes runtime dependencies. The earlier
[operation audit](review/DESKTOP_JAVA_OPERATION_MODEL_AUDIT.md) and
[0.2.0 reconciliation](review/JAVA_0_2_0_R19_DESKTOP_RECONCILIATION_REPORT.md)
remain historical evidence with their original scope. R0's missing API blocker
is resolved by the released archive APIs; see the
[D-BR1 report](review/DESKTOP_BACKUP_RESTORE_D_BR1_REPORT.md).

## Nix dependency cache readiness

`package-deps.json` is a separate, human-owned cache inventory. The owner reports
`nix run .#update-package-deps` succeeded for 0.3.2. Review confirmed that only
the two Totipo coordinates changed from 0.2.0 to 0.3.2; all six JAR/module/POM
hashes match the independently verified Central artifacts above. BC 1.86 and
every unrelated cache entry are unchanged. Cache SHA-256:
`4600e19a0c4d188b0b10b5f5324a455cf6ef9250b1c34aacf0deda0f5a7fec5d`.
Agents do not execute Nix or manually manufacture this cache. The flake exposes
an explicit cache-readiness check alongside the package-inclusive check. After
cache review and a final input freeze, the owner runs `nix flake check`.
The owner subsequently reports `nix flake check` PASS against the matching frozen
inputs. Successful-run logs and execution/store-reuse details were not supplied;
no fresh test execution or native platform qualification is inferred.
See [development](docs/DEVELOPMENT.md) and the D-BR1 report for evidence.
