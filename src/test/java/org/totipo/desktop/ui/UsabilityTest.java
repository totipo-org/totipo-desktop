package org.totipo.desktop.ui;

import org.totipo.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.totipo.desktop.ui.TokenBrowserTest.find;
import static org.junit.jupiter.api.Assertions.*;

class UsabilityTest {
    @Test void matchingIsLiteralAndLimitedToIssuerAccountAcrossAlternatives() {
        var conflict = token(171, active("École ISSUER"), alternative(TokenStatus.ACTIVE,
                "<html>.*", "OtherAccount", TotpAlgorithm.SHA1, 6, 30));
        for (String query : List.of("", "école issuer", "OTHERaccount", ".*", "<html>")) {
            assertTrue(TokenSearch.matches(conflict, query), query);
        }
        for (String query : List.of("AB", "SHA1", "ACTIVE", "Alternative", "absent", "^.*$")) {
            assertFalse(TokenSearch.matches(conflict, query), query);
        }
        assertFalse(TokenSearch.matches(token(171), "AB"));
        assertTrue(TokenSearch.matches(token(171), ""));
        assertFalse(TokenSearch.matches(token(171), "issuer"));
    }

    @Test void normalizationDoesNotDependOnDefaultLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertTrue(TokenSearch.matches(token(1, active("École ISSUER")), "école issuer"));
        } finally { Locale.setDefault(previous); }
    }

    @Test void filterKeepsQueryOrderAndHonestSelectionAcrossEmissions() throws Exception {
        edt(() -> {
            var panel = new TokenBrowserPanel(new MutableClock());
            var state = new State(token(3, active("match")), token(1, active("other")), token(2, active("match"), active("else")));
            try {
                panel.render(state.value);
                assertEquals("3 TOTPs", panel.resultCount.getText());
                panel.search.setText("MATCH");
                assertEquals("2 of 3", panel.resultCount.getText());
                assertEquals(id(2), panel.rows.get(0).token.id());
                assertEquals(id(2), panel.rows.get(1).token.id());
                assertEquals(id(3), panel.rows.get(2).token.id());
                assertTrue(panel.rows.get(1).token.hasConflict());
                assertTrue(state.calls.isEmpty());
                panel.select(id(3));
                panel.render(new State(token(2, active("match")), token(3, active("match newest"))).value);
                assertEquals("MATCH", panel.search.getText());
                assertEquals(id(3), panel.selectedId());
                panel.search.setText("absent");
                assertNull(panel.selectedId());
                assertEquals("No TOTPs match \"absent\"", panel.empty.getText());
                invoke(panel.search, JComponent.WHEN_FOCUSED, KeyStroke.getKeyStroke("ESCAPE"));
                assertEquals("", panel.search.getText());
                assertNull(panel.selectedId());
                panel.render(new State().value);
                assertTrue(panel.empty.getText().contains("No TOTPs yet"));
            } finally { panel.closing(); }
        });
    }

    static Action binding(JComponent target, int condition, KeyStroke stroke) {
        Object key = target.getInputMap(condition).get(stroke);
        assertNotNull(key); return target.getActionMap().get(key);
    }
    static void invoke(JComponent target, int condition, KeyStroke stroke) {
        Action action = binding(target, condition, stroke);
        action.actionPerformed(new ActionEvent(target, ActionEvent.ACTION_PERFORMED, "test"));
    }
    @Test void shortcutsShareActionsAndDisabledCreateCannotRun() throws Exception {
        edt(() -> {
            VaultPanel panel = new VaultPanel();
            AtomicInteger creates = new AtomicInteger(); AtomicInteger refreshes = new AtomicInteger();
            panel.tokenActions(creates::incrementAndGet, (s, a, e) -> fail());
            panel.onRefresh(refreshes::incrementAndGet);
            assertSame(panel.refreshAction, binding(panel, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke("F5")));
            KeyStroke create = KeyStroke.getKeyStroke(KeyEvent.VK_N, SwingUsability.menuMask());
            assertSame(panel.createAction, binding(panel, JComponent.WHEN_IN_FOCUSED_WINDOW, create));
            invoke(panel, JComponent.WHEN_IN_FOCUSED_WINDOW, create); assertEquals(0, creates.get());
            panel.render(new State().value);
            invoke(panel, JComponent.WHEN_IN_FOCUSED_WINDOW, create); assertEquals(1, creates.get());
            panel.writeAvailability(false);
            invoke(panel, JComponent.WHEN_IN_FOCUSED_WINDOW, create); assertEquals(1, creates.get());
            invoke(panel, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke("F5")); assertEquals(1, refreshes.get());
            JTextField search = find(panel, JTextField.class); search.setText("query");
            invoke(panel, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke(KeyEvent.VK_F, SwingUsability.menuMask()));
            assertEquals("query", search.getSelectedText());
            assertEquals(3, panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).keys().length);
            panel.closing();
        });
    }

    @Test void editorDefaultsAndEscapeUseGuardedCleanupWithoutSecretMetadata() throws Exception {
        edt(() -> {
            AtomicInteger cancelled = new AtomicInteger();
            TokenEditorPanel editor = new TokenEditorPanel(null, "Create", d -> fail(), cancelled::incrementAndGet);
            JRootPane root = new JRootPane(); root.setContentPane(editor); editor.installDialog(root);
            assertSame(editor.save, root.getDefaultButton());
            editor.secret.setText("JBSWY3DPEHPK3PXP");
            assertFalse(editor.secret.getAccessibleContext().getAccessibleName().contains("JBSWY"));
            assertNull(editor.secret.getAccessibleContext().getAccessibleDescription());
            assertLabels(editor);
            editor.busy(true, "Saving…"); invoke(root, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke("ESCAPE"));
            assertEquals(0, cancelled.get());
            editor.busy(false, ""); invoke(root, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke("ESCAPE"));
            assertEquals(1, cancelled.get()); assertEquals(0, editor.secret.getPassword().length);
            PasswordChangePanel password = new PasswordChangePanel(s -> fail(), cancelled::incrementAndGet);
            root.setContentPane(password); password.installDialog(root);
            assertSame(password.change, root.getDefaultButton()); assertLabels(password);
            password.current.setText("private-password"); password.next.setText("private-password");
            password.confirmation.setText("private-password");
            for (JPasswordField field : List.of(password.current, password.next, password.confirmation)) {
                assertFalse(field.getAccessibleContext().getAccessibleName().contains("private-password"));
                assertNull(field.getAccessibleContext().getAccessibleDescription());
            }
            password.busy(true, "Changing…"); invoke(root, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke("ESCAPE"));
            assertEquals(1, cancelled.get()); password.busy(false, "");
            invoke(root, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke("ESCAPE"));
            assertEquals(2, cancelled.get());
            for (JPasswordField field : List.of(password.current, password.next, password.confirmation)) { assertEquals(0, field.getPassword().length); }
        });
    }
    private static void assertLabels(Container parent) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JLabel label && !label.getText().isBlank()) { assertNotNull(label.getLabelFor(), label.getText()); }
            if (component instanceof Container child) { assertLabels(child); }
        }
    }

    @Test void mergeStepsHaveNormalDefaultsAndEscapeRetiresSecret() throws Exception {
        edt(() -> {
            var token = token(1, active("one"), active("two"), active("three"));
            var state = new State(token);
            AtomicInteger cancelled = new AtomicInteger();
            var panel = new MergeEditorPanel(org.totipo.desktop.MergeInputs.capture(state.value, token),
                    draft -> fail(), cancelled::incrementAndGet);
            JRootPane root = new JRootPane(); root.setContentPane(panel); panel.installDialog(root, () -> {});
            assertEquals("Resolve", root.getDefaultButton().getText());
            all(panel).stream().filter(JButton.class::isInstance).map(JButton.class::cast).filter(b -> b.getText().equals("Combine details…")).findFirst().orElseThrow().doClick();
            assertLabels(panel); assertNotNull(find(panel, JScrollPane.class));
            JPasswordField secret = find(panel, JPasswordField.class); secret.setText("MY");
            panel.busy(true, "Saving merge…");
            invoke(root, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke("ESCAPE"));
            assertEquals(0, cancelled.get());
            panel.busy(false, "");
            invoke(root, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke("ESCAPE"));
            assertEquals(1, cancelled.get()); assertEquals(0, secret.getPassword().length);
        });
    }

    @Test void manyHeadsReferencesDiagnosticsAndCodesRemainAccessibleAndScrollable() throws Exception {
        edt(() -> {
            List<TokenHead> heads = java.util.stream.IntStream.range(1, 100).mapToObj(i ->
                    head(i, new ClientMetadata(java.util.Optional.of("long client ".repeat(100)), java.util.Optional.empty()))).toList();
            List<UnresolvedReference> refs = java.util.stream.IntStream.range(101, 200)
                    .mapToObj(i -> new UnresolvedReference(revision(i), revision(i + 1))).toList();
            var a = alternative(TokenStatus.ACTIVE, "long issuer ".repeat(1000), "long account ".repeat(1000),
                    TotpAlgorithm.SHA1, 6, 30, heads.toArray(TokenHead[]::new));
            var token = token(1, List.of(a), List.of(new SecretGroup(List.of(a))), heads, refs, false);
            var panel = browser(new MutableClock());
            panel.render(new State(token).value); panel.select(id(1));
            var diagnosticPanel = new TokenDiagnosticsPanel(token);
            String detail = diagnosticPanel.text.getText();
            assertTrue(detail.contains("Unresolved causal references: 99"));
            assertTrue(detail.contains(revision(99).hex()));
            panel.row(id(1)).show.doClick(0);
            CountdownRing remaining = find(panel, CountdownRing.class);
            assertEquals("23 seconds remaining", remaining.getAccessibleContext().getAccessibleName());
            JLabel code = TotpCopyTest.codeLabel(panel);
            assertTrue(code.getAccessibleContext().getAccessibleName().contains("001 234"));
            panel.closing(); assertEquals("", code.getText());
            assertFalse(code.getAccessibleContext().getAccessibleName().contains("001 234"));
            VaultPanel vault = new VaultPanel();
            String[] diagnostics = new String[200]; java.util.Arrays.fill(diagnostics, "DIAGNOSTIC");
            vault.render(org.totipo.desktop.TestSupport.state(new ObservationProgress.Finished(200, true), diagnostics));
            assertTrue(all(vault).stream().noneMatch(component -> component instanceof JTextArea area
                    && "Local observation diagnostics".equals(area.getAccessibleContext().getAccessibleName())));
            assertTrue(vault.notification.isVisible()); vault.closing();
        });
    }
    private static List<Component> all(Container parent) {
        var result = new java.util.ArrayList<Component>();
        for (Component child : parent.getComponents()) {
            result.add(child); if (child instanceof Container container) { result.addAll(all(container)); }
        }
        return result;
    }

    @Test void decisionsHaveSafeDefaultsAndLongContentRemainsScrollable() throws Exception {
        edt(() -> {
            VaultPanel panel = new VaultPanel(); JRootPane root = new JRootPane(); root.setContentPane(panel);
            panel.publicationUncertain(true, false, () -> fail(), () -> fail()); assertNull(root.getDefaultButton());
            panel.clearUncertainty();
            var longValue = "<html>long issuer account ".repeat(1000);
            panel.render(new State(token(1, active(longValue), active(longValue + "second"), active("third"))).value);
            assertNotNull(find(panel, JScrollPane.class));
            var browser = find(panel, TokenBrowserPanel.class);
            assertNotNull(browser.list.getAccessibleContext().getAccessibleName());
            browser.select(id(1));
            assertTrue(browser.row(id(1)).getAccessibleContext().getAccessibleName().contains("conflicting versions"));
            assertTrue(new TokenDiagnosticsPanel(browser.row(id(1)).token).text.getLineWrap());
            panel.closing();
        });
    }
}
