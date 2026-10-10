package org.totipo.desktop.ui;

import org.totipo.*;
import org.totipo.desktop.clipboard.TotpClipboard;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class RevealObservationPolicyTest {
    @Test void repeatedEquivalentAndUnrelatedObservationsPreserveCodeCopyCountdownAndGeometry() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(7);
            var a = active("Alpha"); var state = new State(token(1, a), token(2, active("Beta")));
            var panel = browser(clock); List<String> copied = new ArrayList<>();
            panel.copyAction((c, f, u, n) -> { copied.add(c); return TotpClipboard.COPIED; });
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0);
                var initial = panel.row(id(1)); var size = initial.getPreferredSize();
                var showSize = initial.show.getPreferredSize(); var copySize = TotpCopyTest.buttons(initial).get(0).getPreferredSize();
                String code = panel.totp.presentation(id(1)).get(0).code();
                for (VaultState next : List.of(state.value, new State(token(1, a), token(2, active("Beta"))).value,
                        new State(token(1, a), token(2, active("Changed Beta"))).value,
                        new State(new ObservationProgress.Finished(2, true), List.of(new VaultDiagnostic("TEST_DIAGNOSTIC")),
                                token(1, a), token(2, active("Changed Beta"))).value)) {
                    var oldCode = TotpCopyTest.codeLabel(panel.row(id(1)));
                    clock.now = clock.now.plusSeconds(1); panel.render(next);
                    assertEquals("", oldCode.getText()); // Detached widgets never retain code material.
                    var row = panel.row(id(1));
                    assertEquals(code, panel.totp.presentation(id(1)).get(0).code());
                    assertEquals(30 - clock.now.getEpochSecond(), panel.totp.presentation(id(1)).get(0).seconds());
                    assertFalse(row.show.isVisible()); assertEquals(id(1), panel.selectedId());
                    assertEquals(size, row.getPreferredSize()); assertEquals(showSize, row.show.getPreferredSize());
                    var copy = TotpCopyTest.buttons(row).get(0);
                    assertEquals(copySize, copy.getPreferredSize()); assertTrue(copy.isEnabled());
                    assertTrue(row.getAccessibleContext().getAccessibleName().contains("Code"));
                    copy.doClick(0); assertEquals(code, copied.get(copied.size() - 1));
                }
                assertEquals(1, state.calls.size());
                clock.now = Instant.ofEpochSecond(30); panel.refreshPresentation();
                assertTrue(panel.totp.presentation(id(1)).isEmpty()); assertTrue(panel.row(id(1)).show.isVisible());
            } finally { panel.closing(); }
        });
    }

    @Test void pendingRevealSurvivesUnrelatedObservationAndSearch() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(7);
            var panel = browser(clock); var a = active("Alpha");
            var state = new State(token(1, a), token(2, active("Beta")));
            var callbacks = new ArrayList<Consumer<List<Optional<TotpCode>>>>();
            panel.totpAction((b, alts, n, done) -> callbacks.add(done));
            try {
                panel.render(state.value); panel.reveal(id(1)); panel.search.setText("Beta");
                panel.render(new State(token(1, a), token(2, active("Changed Beta"))).value);
                assertTrue(panel.totp.pending(id(1))); assertNull(panel.row(id(1)));
                callbacks.get(0).accept(GraceRevealTest.code("001234", 0, 30));
                panel.search.setText(""); assertFalse(panel.row(id(1)).show.isVisible());
                assertEquals("001234", panel.totp.presentation(id(1)).get(0).code()); assertEquals(1, callbacks.size());
            } finally { panel.closing(); }
        });
    }

    @Test void pendingActiveAndGraceRevokeOnChangedAlternativeConflictTombstoneOrDisappearance() throws Exception {
        edt(() -> {
            for (String mode : List.of("pending", "active", "grace")) {
                for (String change : List.of("alternative", "conflict", "tombstone", "incomplete", "disappearance")) {
                    var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(mode.equals("grace") ? 25 : 7);
                    var a = active("Alpha"); var state = new State(token(1, a)); var panel = browser(clock);
                    var callbacks = new ArrayList<Consumer<List<Optional<TotpCode>>>>();
                    if (mode.equals("pending")) { panel.totpAction((b, alts, n, done) -> callbacks.add(done)); }
                    else if (mode.equals("grace")) { GraceRevealTest.deferStage(panel, callbacks); }
                    try {
                        panel.render(state.value); panel.reveal(id(1));
                        State next = switch (change) {
                            case "alternative" -> new State(token(1, active("Changed Alpha")));
                            case "conflict" -> new State(token(1, a, active("Other")));
                            case "tombstone" -> new State(token(1, alternative(TokenStatus.TOMBSTONED, "Alpha", "account", TotpAlgorithm.SHA1, 6, 30)));
                            case "incomplete" -> new State(token(1));
                            case "disappearance" -> new State();
                            default -> throw new AssertionError();
                        };
                        panel.render(next.value);
                        // Returning to the original Alternative cannot resurrect revoked authorization.
                        panel.render(state.value);
                        if (!callbacks.isEmpty()) { callbacks.get(0).accept(GraceRevealTest.code("001234", mode.equals("grace") ? 30 : 0, mode.equals("grace") ? 60 : 30)); }
                        assertTrue(panel.totp.presentation(id(1)).isEmpty(), mode + "/" + change);
                        assertFalse(panel.totp.pending(id(1))); assertTrue(panel.row(id(1)).show.isVisible());
                    } finally { panel.closing(); }
                }
            }
        });
    }

    @Test void stagedAndPendingGraceSurviveEquivalentAndUnrelatedObservationOnlyForOnePeriod() throws Exception {
        edt(() -> {
            for (boolean staged : new boolean[]{false, true}) {
                var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
                var a = active("Alpha"); var state = new State(token(1, a), token(2, active("Beta")));
                var panel = browser(clock); var callbacks = new ArrayList<Consumer<List<Optional<TotpCode>>>>();
                GraceRevealTest.deferStage(panel, callbacks);
                try {
                    panel.render(state.value); panel.reveal(id(1));
                    if (staged) { callbacks.get(0).accept(PendingGraceTest.next()); }
                    panel.render(state.value); panel.render(new State(token(1, a), token(2, active("Changed Beta"))).value);
                    assertEquals(1, callbacks.size()); assertEquals(1, state.calls.size());
                    clock.now = Instant.ofEpochSecond(30); panel.refreshPresentation();
                    if (!staged) {
                        assertTrue(panel.totp.pending(id(1))); assertFalse(TotpCopyTest.buttons(panel.row(id(1))).get(0).isEnabled());
                        panel.render(new State(token(1, a)).value); callbacks.get(0).accept(PendingGraceTest.next());
                    }
                    assertEquals("005678", panel.totp.presentation(id(1)).get(0).code());
                    assertTrue(TotpCopyTest.buttons(panel.row(id(1))).get(0).isEnabled());
                    clock.now = Instant.ofEpochSecond(60); panel.refreshPresentation();
                    assertTrue(panel.totp.presentation(id(1)).isEmpty()); assertEquals(1, callbacks.size());
                } finally { panel.closing(); }
            }
        });
    }
}
