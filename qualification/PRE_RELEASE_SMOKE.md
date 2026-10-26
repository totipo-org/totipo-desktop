# 0.1.0 preview pre-publication functional smoke

This short human procedure is required for RELEASE-READY. It does not grant
PLATFORM QUALIFIED status or replace GUI_SMOKE, CLIPBOARD_SMOKE,
ACCESSIBILITY_SMOKE or PACKAGED_LAUNCH. Use a physical graphical desktop and only
disposable encrypted vaults/test credentials. No agent GUI PASS is claimed.

Acquire intended exact same-build ZIP/Nix outputs and verify them using
[PACKAGED_LAUNCH.md](PACKAGED_LAUNCH.md#obtain-and-identify-the-artifacts-maintainer-nix-environment).
Use the portable ZIP from the derivation's `portable` output, retain its original
manifests and record ZIP SHA-256/size plus all four JAR hashes. Record Nix
out/portable/derivation and managed runtime identities separately. Do not replace
these files with a later local Gradle build. Before launch verify ZIP/extracted
hashes, Nix payload equivalence and deployment manifest as in that procedure.

Use an empty extraction directory outside the source checkout, with spaces in
its path, and a separate private disposable vault folder. Record UTC date,
operator, source/input freeze, artifact paths/hashes, OS/distribution/architecture,
desktop/display, filesystem and exact Java vendor/version/build for each route.
Record paste target and clipboard manager if used. Keep secrets, passwords,
clipboard contents and actual generated codes out of saved evidence.

Recorded run, 2026-10-25: all nine steps PASS (human-reported), NixOS 26.05
(Yarara), Cinnamon/X11, OpenJDK 25.0.4.1+1, Firefox paste target. Test-root
filesystem reported `ext2/ext3` by `stat`; exact ext-family version was not
established. Artifact identities and dated per-stage observations are in the
[M4b-R3 report](../review/M4B_R3_DESKTOP_0_1_0_RELEASE_PREPARATION_REPORT.md).
Both application routes exited; disposable-folder deletion remains unconfirmed.
Clipboard manager and Firefox version were not supplied; full native qualification
remains UNQUALIFIED. This table records this run, not later candidate results.

| Step | Human action and expected result | Actual result/observation |
| --- | --- | --- |
| 1 | Extract original `totipo-desktop-0.1.0.zip`; launch `bin/totipo-desktop` with externally supplied Java 25 from outside checkout, clearing development/JVM overrides as in PACKAGED_LAUNCH. | PASS (human-reported, 2026-10-25) |
| 2 | Confirm startup, ordinary GUI rendering and reachable Create/Open controls. | PASS (human-reported, 2026-10-25) |
| 3 | Create a disposable encrypted vault with a test-only password; confirm it opens without error. | PASS (human-reported, 2026-10-25) |
| 4 | Add a test TOTP token, e.g. manual Base32 `GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ` (public RFC test material), SHA-1, six digits, 30-second period. Never use a real account. | PASS (human-reported, 2026-10-25) |
| 5 | Reveal its code and explicitly Copy; paste into a disposable local text field and confirm all displayed digits match. This does not qualify expiry, ownership or history cleanup. | PASS (human-reported, 2026-10-25) |
| 6 | Lock/close the vault, exit, relaunch the same extracted launcher and reopen the vault. | PASS (human-reported, 2026-10-25) |
| 7 | Confirm the token persists, reveal a current code and check it against an independent offline TOTP reference using the same public test setup and current time/period. Record reference tool/version and match result, not the code. Avoid interval boundaries; repeat within one interval if necessary. Confirm normal rollover. | PASS (human-reported, 2026-10-25) |
| 8 | Launch the exact Nix package using its installed `bin/totipo-desktop` wrapper, with caller Java/development overrides cleared; record actual managed Java 25 identity. | PASS (human-reported, 2026-10-25) |
| 9 | Confirm ordinary GUI rendering and basic create/add/reveal/copy/lock/reopen operation through this deployment, using a disposable vault. Confirm token persists. Exit and confirm process stops. | PASS (human-reported, 2026-10-25) |

For step 7 an already available trusted offline authenticator is sufficient; no
service account, network peer or synchronization installation is needed. If no
independent reference is available, record that step pending rather than infer
validity from visually plausible digits. The deterministic automated TOTP tests
remain separate evidence.

Record per-step PASS/FAIL/PENDING, runtime/artifact for each executed step, cleanup
of disposable vaults and application processes, and any failures/retries. Append
the dated result to the M4b-R3 report or a linked evidence document. A serious
functional, cryptographic or vault-integrity failure blocks RELEASE-READY; preserve
the observation and investigate before publication. Unperformed full native
scenarios remain UNQUALIFIED and can follow publication against original bytes.
