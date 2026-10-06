package org.totipo.desktop.ui;

import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.totipo.desktop.ShellState;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.totipo.desktop.ui.TokenBrowserTest.find;
import static org.junit.jupiter.api.Assertions.*;

class S5ConformanceTest {
    @Test void unavailableMutationExplainsEveryEntryWithoutBlockingRetrieval() throws Exception {
        edt(() -> {
            var panel = new VaultPanel(); var browser = find(panel, TokenBrowserPanel.class);
            var changes = new AtomicInteger(); panel.tokenActions(changes::incrementAndGet, (s, a, m) -> changes.incrementAndGet());
            browser.totpAction(TokenFixtures::generate);
            var state = new State(token(1, active("Service"), active("Other")));
            state.result = c -> new org.totipo.TotpCode("001234", c.now(), c.now().plusSeconds(30));
            try {
                panel.render(state.value); browser.select(id(1)); panel.writeAvailability(false);
                assertFalse(panel.createAction.isEnabled()); assertFalse(browser.editMenu.isEnabled()); assertFalse(panel.changePassword.isEnabled());
                assertFalse(browser.row(id(1)).edit.isEnabled());
                assertNotNull(panel.createAction.getValue(Action.SHORT_DESCRIPTION));
                for (JMenuItem item : List.of(browser.editMenu, panel.changePassword)) {
                    assertTrue(item.getAccessibleContext().getAccessibleDescription().contains("unavailable"));
                }
                assertTrue(browser.row(id(1)).edit.getAccessibleContext().getAccessibleDescription().contains("Other"));
                var resolve = TestSupportComponents.all(browser).stream().filter(JButton.class::isInstance).map(JButton.class::cast).filter(b -> b.getText().equals("Resolve")).findFirst().orElseThrow();
                assertFalse(resolve.isEnabled()); assertTrue(resolve.getAccessibleContext().getAccessibleDescription().contains("unavailable"));
                UsabilityTest.invoke(panel, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke("control N"));
                panel.menuBar().getMenu(2).getItem(0).doClick(0); browser.editMenu.doClick(0); resolve.doClick(0);
                assertEquals(0, changes.get());
                browser.search.setText("Service"); assertEquals(2, browser.rows.size());
                browser.row(id(1)).show.doClick(0); assertEquals(1, state.calls.size());
                assertTrue(panel.refreshAction.isEnabled()); assertFalse(panel.notification.isVisible());
            } finally { panel.closing(); }
        });
    }
    @Test void blockingHasReachableRecoveryAndSafeDetailsWithoutPasswordOrList() throws Exception {
        edt(() -> {
            var panel = new ShellPanel(); var retry = new AtomicInteger(); var change = new AtomicInteger(); var details = new AtomicInteger();
            panel.actions(change::incrementAndGet, () -> fail(), value -> fail()); panel.retryAction(retry::incrementAndGet); panel.detailsAction(details::incrementAndGet);
            for (String message : List.of("Required vault data is unavailable.", "Required vault data is invalid or uses an unsupported format.")) {
                panel.render(ShellState.BLOCKING_VAULT_STATE, Path.of("current-vault"), message, false);
                assertFalse(panel.password.isVisible()); assertEquals(message, panel.explanation.getText());
                assertEquals("Vault state details", panel.explanation.getAccessibleContext().getAccessibleName());
                assertTrue(panel.details.isEnabled()); panel.details.doClick(0); panel.primary.doClick(0); panel.secondary.doClick(0);
                assertTrue(TestSupportComponents.all(panel).stream().noneMatch(c -> c instanceof TokenBrowserPanel || c instanceof JButton b && b.getText().contains("Repair")));
            }
            assertEquals(2, retry.get()); assertEquals(2, change.get()); assertEquals(2, details.get());
        });
    }
    @Test void aboutShowsOnlyBoundedSafeValuesAndSelectableFullStrengthLocation() throws Exception {
        edt(() -> {
            Path path = Path.of("/tmp/current-vault"); var closes = new AtomicInteger();
            var panel = new AboutVaultPanel(path, "Unlocked; changes available", "No observation problems reported", closes::incrementAndGet);
            var values = TestSupportComponents.all(panel).stream().filter(JTextArea.class::isInstance).map(JTextArea.class::cast).toList();
            assertEquals(5, values.size());
            assertEquals(path.toString(), values.stream().filter(v -> v.getAccessibleContext().getAccessibleName().equals("Location")).findFirst().orElseThrow().getText());
            for (JTextArea value : values) {
                assertTrue(value.isEnabled()); assertFalse(value.isEditable()); assertTrue(value.isFocusable());
                value.selectAll(); assertEquals(value.getText(), value.getSelectedText());
                assertEquals(DesktopStyle.text(), value.getForeground());
            }
            assertTrue(values.stream().noneMatch(v -> v.getAccessibleContext().getAccessibleName().matches("Password|Secret|Code|Heads|Root")));
            assertTrue(values.stream().anyMatch(v -> v.getText().contains("not reported")));
            panel.close.doClick(0); assertEquals(1, closes.get());
        });
    }
    @Test void emptyPasswordDecisionAndRetirementClearInputsAndPreventSubmission() throws Exception {
        edt(() -> {
            for (boolean confirm : List.of(false, true)) {
                var submissions = new AtomicInteger(); var decisions = new AtomicInteger();
                var panel = new PasswordChangePanel(s -> { submissions.incrementAndGet(); s.close(); }, () -> {}, () -> { decisions.incrementAndGet(); return confirm; });
                panel.current.setText("fixture-current"); panel.change.doClick(0);
                assertEquals(1, decisions.get()); assertEquals(confirm ? 1 : 0, submissions.get());
                for (JPasswordField field : List.of(panel.current, panel.next, panel.confirmation)) { assertEquals(0, field.getPassword().length); }
                panel.retire(); panel.change.doClick(0); assertEquals(confirm ? 1 : 0, submissions.get());
            }
            PasswordChangePanel[] owner = new PasswordChangePanel[1]; var calls = new AtomicInteger();
            owner[0] = new PasswordChangePanel(s -> { calls.incrementAndGet(); s.close(); }, () -> {}, () -> { owner[0].retire(); return true; });
            owner[0].change.doClick(0); assertEquals(0, calls.get());
        });
    }
    @Test void passwordFormUsesConsequenceWarningAndActivatableMismatchValidation() throws Exception {
        edt(() -> {
            var panel = new PasswordChangePanel(s -> fail("Invalid form submitted"), () -> {});
            assertTrue(panel.change.isEnabled()); assertEquals("Change Password", panel.change.getText());
            assertEquals("Current password", panel.current.getAccessibleContext().getAccessibleName());
            assertEquals("New password", panel.next.getAccessibleContext().getAccessibleName());
            assertEquals("Confirm new password", panel.confirmation.getAccessibleContext().getAccessibleName());
            for (JPasswordField field : List.of(panel.current, panel.next, panel.confirmation)) {
                assertEquals(0, field.getDocument().getLength()); // Opening an unlocked session never prefills credentials.
            }
            for (JPasswordField field : List.of(panel.current, panel.next, panel.confirmation)) {
                field.setText("fixture-input"); assertNotEquals(0, field.getEchoChar());
                var accessible = field.getAccessibleContext().getAccessibleText();
                assertNotEquals("f", accessible.getAtIndex(javax.accessibility.AccessibleText.CHARACTER, 0));
            }
            panel.next.setText("fixture-new"); panel.confirmation.setText("different"); panel.change.doClick(0);
            assertTrue(panel.change.isEnabled()); assertTrue(panel.message.getText().contains("do not match"));
            assertTrue(TestSupportComponents.all(panel).stream().anyMatch(c -> c instanceof JTextArea a && a.getText().contains("Old backups or retained copies")));
            for (JPasswordField field : List.of(panel.current, panel.next, panel.confirmation)) {
                assertFalse(field.getAccessibleContext().getAccessibleName().contains("fixture")); assertEquals(0, field.getPassword().length);
            }
            panel.retire();
        });
    }
    @Test void radioAndExclusiveChoicesHaveShapeWeightAndIndependentFocusAcrossThemes() throws Exception {
        edt(() -> {
            Object background = UIManager.get("List.background"), foreground = UIManager.get("List.foreground");
            try {
                for (Color surface : List.of(new Color(38, 42, 47), new Color(248, 248, 248))) {
                    UIManager.put("List.background", surface); UIManager.put("List.foreground", surface.getRed() < 100 ? Color.WHITE : Color.BLACK);
                    JRadioButton radio = new JRadioButton("Version"); DesktopStyle.radio(radio);
                    assertTrue(radio.getIcon() instanceof DesktopStyle.RadioGlyph); assertFalse(radio.isSelected());
                    assertTrue(radio.getBorder() instanceof DesktopStyle.ControlBorder); assertTrue(radio.isFocusable());
                    assertTrue(DesktopStyle.contrast(DesktopStyle.radioOutline(), surface) >= 4.5);
                    assertTrue(DesktopStyle.contrast(DesktopStyle.danger(), surface) >= 4.5);
                    radio.setSelected(true); assertEquals(javax.accessibility.AccessibleRole.RADIO_BUTTON, radio.getAccessibleContext().getAccessibleRole());
                    assertTrue(radio.getAccessibleContext().getAccessibleStateSet().contains(javax.accessibility.AccessibleState.CHECKED));
                    var choices = new TokenChoice<>("Digits", List.of(6, 7, 8), Object::toString, false); TokenManagementPanel.styleChoices(choices);
                    choices.select(8); assertTrue(choices.options.get(8).getFont().isBold()); assertFalse(choices.options.get(6).getFont().isBold());
                    assertTrue(choices.options.get(8).isSelected());
                }
            } finally { UIManager.put("List.background", background); UIManager.put("List.foreground", foreground); }
        });
    }
    private static final class TestSupportComponents {
        static List<Component> all(Container root) {
            var result = new java.util.ArrayList<Component>();
            for (Component child : root.getComponents()) { result.add(child); if (child instanceof Container nested) { result.addAll(all(nested)); } }
            return result;
        }
    }
}
