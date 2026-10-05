package org.totipo.desktop.ui;

import java.awt.event.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.totipo.*;
import org.totipo.desktop.clipboard.TotpClipboard;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.totipo.desktop.ui.UsabilityTest.invoke;
import static org.junit.jupiter.api.Assertions.*;

class TokenKeyboardTest {
    static void searchKey(TokenBrowserPanel panel, String key) { invoke(panel.search, JComponent.WHEN_FOCUSED, KeyStroke.getKeyStroke(key)); }
    static void rowKey(TokenRowPanel row, String key) { invoke(row, JComponent.WHEN_FOCUSED, KeyStroke.getKeyStroke(key)); }
    static void arrow(TokenRowPanel row, String key) { invoke(row, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyStroke.getKeyStroke(key)); }

    @Test void ctrlFFocusesSearchSelectsQueryAndDoesNotMutateOrDerive() throws Exception {
        edt(() -> {
            var vault = new VaultPanel(); var panel = TokenBrowserTest.find(vault, TokenBrowserPanel.class);
            var focus = new AtomicReference<JComponent>(); panel.focus = focus::set; panel.totpAction(TokenFixtures::generate);
            var state = new State(token(1, active("Alpha")));
            try {
                vault.render(state.value); panel.search.setText("Alpha");
                invoke(vault, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
                assertSame(panel.search, focus.get()); assertEquals("Alpha", panel.search.getSelectedText()); assertEquals("Alpha", panel.search.getText());
                panel.search.setText(""); focus.set(null);
                invoke(vault, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
                assertSame(panel.search, focus.get()); assertEquals("", panel.search.getText()); assertTrue(state.calls.isEmpty());
                assertEquals("Search", panel.search.getAccessibleContext().getAccessibleName());
            } finally { vault.closing(); }
        });
    }
    @Test void searchArrowsEnterAndRowNavigationUseVisibleOrderAndGenerateNothing() throws Exception {
        edt(() -> {
            for (String key : List.of("DOWN", "ENTER")) {
                var panel = browser(new MutableClock()); var state = new State(token(3, active("Alpha")), token(1, active("Beta")), token(2, active("Gamma")));
                var focus = new AtomicReference<JComponent>(); panel.focus = focus::set;
                try {
                    panel.render(state.value); panel.select(id(1)); searchKey(panel, key);
                    assertSame(panel.row(id(1)), focus.get()); assertEquals(id(1), panel.selectedId()); assertTrue(panel.editMenu.isEnabled());
                    arrow(panel.row(id(1)), "UP"); assertSame(panel.row(id(3)), focus.get()); assertEquals(id(3), panel.selectedId());
                    panel.search.setCaretPosition(0); arrow(panel.row(id(3)), "UP");
                    assertSame(panel.search, focus.get()); assertEquals(id(3), panel.selectedId()); assertEquals(0, panel.search.getSelectionEnd());
                    searchKey(panel, key); assertSame(panel.row(id(3)), focus.get());
                    arrow(panel.row(id(3)), "DOWN"); assertSame(panel.row(id(1)), focus.get());
                    arrow(panel.row(id(1)), "DOWN"); assertSame(panel.row(id(2)), focus.get());
                    arrow(panel.row(id(2)), "DOWN"); assertSame(panel.row(id(2)), focus.get());
                    panel.search.setText("Beta"); assertNull(panel.selectedId()); searchKey(panel, key);
                    assertSame(panel.rows.get(0), focus.get()); assertEquals(id(1), panel.selectedId());
                    arrow(panel.row(id(1)), "UP"); assertSame(panel.search, focus.get()); assertEquals("Beta", panel.search.getText());
                    panel.search.setText("absent"); focus.set(panel.search); searchKey(panel, key);
                    assertSame(panel.search, focus.get()); assertNull(panel.selectedId()); assertFalse(panel.editMenu.isEnabled()); assertFalse(panel.diagnosticsMenu.isEnabled());
                    panel.search.setText(""); panel.select(id(3));
                    for (var listener : panel.row(id(3)).getFocusListeners()) { listener.focusGained(new FocusEvent(panel.row(id(3)), FocusEvent.FOCUS_GAINED)); }
                    assertTrue(state.calls.isEmpty());
                } finally { panel.closing(); }
            }
        });
    }
    @Test void rowEnterAndSpaceRevealThenCopyThroughExistingCallbackAndToast() throws Exception {
        edt(() -> {
            for (String key : List.of("ENTER", "SPACE")) {
                var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(7); var panel = browser(clock); var state = new State(token(1, active("A")));
                var copied = new ArrayList<String>(); panel.copyAction((c, f, u, n) -> { assertEquals(clock.now, n); assertEquals(Instant.EPOCH, f); assertEquals(Instant.ofEpochSecond(30), u); copied.add(c); return TotpClipboard.COPIED; });
                try {
                    panel.render(state.value); var row = panel.row(id(1)); rowKey(row, key);
                    assertEquals(1, state.calls.size()); assertTrue(copied.isEmpty()); assertEquals(id(1), panel.selectedId());
                    rowKey(row, key); assertEquals(List.of("001234"), copied); assertEquals(1, state.calls.size()); assertEquals("Copied", TotpCopyTest.buttons(row).get(0).getText()); assertFalse(panel.copyNotification.isVisible());
                    assertNull(row.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(KeyStroke.getKeyStroke(key)));
                    assertNotSame(row.getActionMap().get("primary-action"), UsabilityTest.binding(TotpCopyTest.buttons(row).get(0), JComponent.WHEN_FOCUSED, KeyStroke.getKeyStroke("SPACE")));
                    TotpCopyTest.buttons(row).get(0).doClick(0); assertEquals(2, copied.size()); assertEquals(1, state.calls.size());
                } finally { panel.closing(); }
            }
        });
    }
    @Test void expiredOrPendingRowCannotCopyOrRequestDuplicateGeneration() throws Exception {
        edt(() -> {
            for (String key : List.of("ENTER", "SPACE")) {
                var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25); var panel = browser(clock); var state = new State(token(1, active("A")));
                var callbacks = new ArrayList<java.util.function.Consumer<List<Optional<TotpCode>>>>(); GraceRevealTest.deferStage(panel, callbacks);
                panel.copyAction((c, f, u, n) -> { fail("Pending/expired copy"); return ""; });
                try {
                    panel.render(state.value); var row = panel.row(id(1)); rowKey(row, key); assertEquals(1, callbacks.size());
                    clock.now = Instant.ofEpochSecond(30); rowKey(row, key);
                    assertFalse(row.show.isVisible()); assertFalse(TotpCopyTest.buttons(row).get(0).isEnabled());
                    panel.totp.tick(); rowKey(row, key); rowKey(row, key); assertEquals(1, callbacks.size()); assertEquals(1, state.calls.size());
                } finally { panel.closing(); }
            }
        });
    }
    @Test void initialInFlightRowActionNeverDuplicatesReveal() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); var state = new State(token(1, active("A")), token(2, active("B"))); var calls = new ArrayList<Object>();
            try {
                panel.render(state.value); rowKey(panel.row(id(2)), "ENTER");
                panel.totpAction((b, a, n, d) -> calls.add(d));
                rowKey(panel.row(id(1)), "ENTER"); panel.totp.tick();
                assertEquals("Showing…", panel.row(id(1)).show.getText()); assertFalse(panel.row(id(1)).show.isEnabled());
                rowKey(panel.row(id(1)), "SPACE"); rowKey(panel.row(id(1)), "ENTER"); assertEquals(1, calls.size());
            } finally { panel.closing(); }
        });
    }
    @Test void conflictPrimaryActionRevealsSemanticOutcomesAndNeverChoosesMultiCodeWinner() throws Exception {
        edt(() -> {
            for (boolean sameSecret : new boolean[]{false, true}) {
                var a = alternative(TokenStatus.ACTIVE, "A", "account", TotpAlgorithm.SHA1, 6, 30, head(1, ClientMetadata.empty()), head(2, ClientMetadata.empty()));
                var b = active("B"); var token = token(1, List.of(a, b), sameSecret ? List.of(new SecretGroup(List.of(a, b))) : List.of(new SecretGroup(List.of(a)), new SecretGroup(List.of(b))), a.heads(), List.of(), true);
                var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(7); var panel = browser(clock); var state = new State(token); var copies = new ArrayList<String>();
                panel.copyAction((c, f, u, n) -> { copies.add(c); return TotpClipboard.COPIED; });
                try {
                    panel.render(state.value); rowKey(panel.row(id(1)), "ENTER"); assertEquals(1, state.calls.size()); assertTrue(copies.isEmpty());
                    assertSame(a, state.calls.get(0).alternative()); rowKey(panel.row(id(1)), "ENTER"); rowKey(panel.row(id(1)), "SPACE");
                    assertEquals(2, copies.size());
                    for (var button : TotpCopyTest.buttons(panel)) { button.doClick(0); }
                    assertEquals(3, copies.size()); assertEquals(1, state.calls.size());
                } finally { panel.closing(); }
            }
        });
    }
    @Test void stagedFutureCodeIsNeverRowPrimaryActionCopyTargetBeforeBoundary() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25); var panel = browser(clock); var state = new State(token(1, active("A")));
            state.result = c -> new TotpCode(c.now().getEpochSecond() < 30 ? "001234" : "005678", Instant.ofEpochSecond(c.now().getEpochSecond() < 30 ? 0 : 30), Instant.ofEpochSecond(c.now().getEpochSecond() < 30 ? 30 : 60));
            var copied = new ArrayList<String>(); panel.copyAction((c, f, u, n) -> { copied.add(c); return TotpClipboard.COPIED; });
            try {
                panel.render(state.value); rowKey(panel.row(id(1)), "ENTER"); rowKey(panel.row(id(1)), "SPACE"); assertEquals(List.of("001234"), copied); assertEquals(2, state.calls.size());
            } finally { panel.closing(); }
        });
    }
}
