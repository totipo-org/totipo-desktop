package org.totipo.desktop.ui;

import org.totipo.*;
import org.totipo.desktop.MergeDraft;
import org.totipo.desktop.MergeInputs;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class U4ResolverTest {
    static List<Component> components(Container root) {
        List<Component> result = new ArrayList<>();
        for (Component child : root.getComponents()) { result.add(child); if (child instanceof Container container) { result.addAll(components(container)); } }
        return result;
    }
    static AbstractButton option(Container root, String label) { return components(root).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast).filter(b -> b.getText().equals(label)).findFirst().orElseThrow(); }
    static void focus(Component component) { for (var listener : component.getFocusListeners()) { listener.focusGained(new FocusEvent(component, FocusEvent.FOCUS_GAINED)); } }
    static List<JRadioButton> setups(Container root) { return components(root).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).filter(b -> b.getText().startsWith("Setup used by") || b.getText().startsWith("Existing setup") || b.getText().equals("Keep existing setup")).toList(); }

    static String title(Border border) {
        if (border instanceof TitledBorder titled) { return titled.getTitle(); }
        if (border instanceof CompoundBorder compound) { return title(compound.getOutsideBorder()); }
        return null;
    }
    static List<JPanel> sections(Container root) {
        return components(root).stream().filter(JPanel.class::isInstance).map(JPanel.class::cast)
                .filter(panel -> title(panel.getBorder()) != null).toList();
    }
    static JPanel section(Container root, String name) {
        return sections(root).stream().filter(panel -> name.equals(title(panel.getBorder()))).findFirst().orElseThrow();
    }
    static List<JRadioButton> radios(Container root) {
        return components(root).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).toList();
    }
    static List<JTextField> textFields(Container root) {
        return components(root).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast).toList();
    }
    static void layout(Container root) {
        root.doLayout(); for (Component child : root.getComponents()) { if (child instanceof Container container) { layout(container); } }
    }

    @Test void titledDecisionsContainVerticalTextChoicesAgreedFieldAndAtomicSetup() throws Exception {
        edt(() -> {
            var a = active("A"); var b = alternative(TokenStatus.TOMBSTONED, "B", "account", TotpAlgorithm.SHA256, 8, 45);
            var token = token(1, a, b); var state = new State(token);
            var panel = new MergeEditorPanel(MergeInputs.capture(state.value, token), MergeDraft::close, () -> {});
            try {
                assertEquals(List.of("Issuer", "Account", "Status", "Authenticator Setup"), sections(panel).stream().map(p -> title(p.getBorder())).toList());
                var parent = section(panel, "Issuer").getParent();
                for (var group : sections(panel)) {
                    assertSame(parent, group.getParent());
                    assertEquals(title(group.getBorder()), group.getAccessibleContext().getAccessibleName());
                    assertNotNull(group.getAccessibleContext().getAccessibleDescription());
                    assertTrue(components(group).stream().filter(JLabel.class::isInstance).map(JLabel.class::cast)
                            .noneMatch(label -> title(group.getBorder()).equals(label.getText())));
                }
                var issuer = section(panel, "Issuer");
                assertEquals(List.of("A", "B", ""), textFields(issuer).stream().map(JTextField::getText).toList());
                assertEquals(3, radios(issuer).size()); assertTrue(radios(issuer).stream().noneMatch(JRadioButton::isSelected));
                var rows = textFields(issuer).stream().map(Component::getParent).toList();
                assertEquals(3, rows.stream().distinct().count());
                assertSame(rows.get(0).getParent(), rows.get(1).getParent()); assertSame(rows.get(1).getParent(), rows.get(2).getParent());
                assertEquals(1, ((GridLayout) rows.get(0).getParent().getLayout()).getColumns());
                for (int i = 0; i < 3; i++) {
                    assertSame(rows.get(i), radios(issuer).get(i).getParent());
                    assertSame(radios(issuer).get(i), ((BorderLayout) rows.get(i).getLayout()).getLayoutComponent(BorderLayout.WEST));
                    assertEquals(i == 2, textFields(issuer).get(i).isEditable());
                }
                focus(textFields(issuer).get(1)); assertTrue(radios(issuer).get(1).isSelected());
                textFields(issuer).get(2).setText("custom"); assertTrue(radios(issuer).get(2).isSelected());
                var account = section(panel, "Account"); assertTrue(radios(account).isEmpty());
                assertEquals(1, textFields(account).size()); assertEquals("account", textFields(account).get(0).getText()); assertTrue(textFields(account).get(0).isEditable());
                assertEquals(List.of("Active", "Deleted"), radios(section(panel, "Status")).stream().map(JRadioButton::getText).toList());
                var setup = section(panel, "Authenticator Setup");
                assertEquals(2, setups(setup).size()); assertEquals(3, radios(setup).size());
                assertSame(option(panel, "Use a different authenticator setup"), option(setup, "Use a different authenticator setup"));
                for (String label : List.of("SHA1", "SHA256", "SHA512", "6", "7", "8")) { assertSame(option(panel, label), option(setup, label)); }
                assertNotNull(TokenBrowserTest.find(setup, JPasswordField.class)); assertNotNull(TokenBrowserTest.find(setup, JSpinner.class));
                assertEquals(1, components(panel).stream().filter(JScrollPane.class::isInstance).count());
                assertFalse(components(TokenBrowserTest.find(panel, JScrollPane.class)).contains(panel.save));
                assertFalse(components(TokenBrowserTest.find(panel, JScrollPane.class)).contains(panel.cancel));
                assertEquals(TokenEditorPanel.MINIMUM_SIZE, panel.getMinimumSize());
            } finally { panel.retire(); }
        });
    }

    @Test void conflictingAccountAndLongLiteralValuesFitOneScrollingBody() throws Exception {
        edt(() -> {
            String longValue = "<html>literal & value ".repeat(100);
            var a = alternative(TokenStatus.ACTIVE, longValue, longValue, TotpAlgorithm.SHA1, 6, 30);
            var b = alternative(TokenStatus.ACTIVE, "B", "other", TotpAlgorithm.SHA1, 6, 30);
            var token = token(1, a, b); var state = new State(token);
            var panel = new MergeEditorPanel(MergeInputs.capture(state.value, token), MergeDraft::close, () -> {});
            try {
                var account = section(panel, "Account"); assertEquals(3, radios(account).size());
                assertEquals(List.of(longValue, "other", ""), textFields(account).stream().map(JTextField::getText).toList());
                for (Dimension size : List.of(panel.getPreferredSize(), panel.getMinimumSize())) {
                    panel.setSize(size); layout(panel);
                    var scroll = TokenBrowserTest.find(panel, JScrollPane.class);
                    assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, scroll.getHorizontalScrollBarPolicy());
                    assertFalse(scroll.getHorizontalScrollBar().isVisible());
                    assertEquals(scroll.getViewport().getWidth(), scroll.getViewport().getView().getWidth());
                    for (var group : sections(panel)) {
                        assertTrue(group.getX() >= 0); assertTrue(group.getX() + group.getWidth() <= group.getParent().getWidth());
                    }
                    var fields = textFields(account);
                    for (int i = 0; i < 3; i++) {
                        var field = fields.get(i); var radio = radios(account).get(i);
                        assertSame(field.getParent(), radio.getParent()); assertTrue(field.getWidth() > 0);
                        assertTrue(field.getX() >= radio.getX() + radio.getWidth());
                        if (i > 0) { assertTrue(field.getParent().getY() > fields.get(i - 1).getParent().getY()); }
                    }
                }
                assertEquals(longValue, textFields(account).get(0).getText());
                focus(textFields(account).get(0)); assertTrue(radios(account).get(0).isSelected());
                textFields(account).get(2).setText("new"); assertTrue(radios(account).get(2).isSelected());
            } finally { panel.retire(); }
        });
    }

    @Test void setupEqualityRequiresSecretGroupAndAllThreeParametersAndIgnoresHeads() throws Exception {
        edt(() -> {
            var a = alternative(TokenStatus.ACTIVE, "A", "a", TotpAlgorithm.SHA1, 6, 30, head(1, ClientMetadata.empty()), head(2, ClientMetadata.empty()));
            List<TokenAlternative> variants = List.of(active("B"), alternative(TokenStatus.ACTIVE, "B", "a", TotpAlgorithm.SHA256, 6, 30),
                    alternative(TokenStatus.ACTIVE, "B", "a", TotpAlgorithm.SHA1, 8, 30), alternative(TokenStatus.ACTIVE, "B", "a", TotpAlgorithm.SHA1, 6, 45));
            for (int i = 0; i < variants.size(); i++) {
                var b = variants.get(i); var all = List.of(a, b); var token = token(1, all, List.of(new SecretGroup(all)), a.heads(), List.of(), true);
                var state = new State(token); var inputs = MergeInputs.capture(state.value, token);
                assertEquals(i == 0, MergeEditorPanel.sameSetup(inputs, a, b));
                var panel = new MergeEditorPanel(inputs, MergeDraft::close, () -> {}); assertEquals(i == 0 ? 1 : 2, setups(panel).size()); panel.retire();
            }
            var b = active("B"); var all = List.of(a, b);
            for (List<SecretGroup> groups : List.of(List.of(new SecretGroup(List.of(a)), new SecretGroup(List.of(b))), List.<SecretGroup>of())) {
                var token = token(1, all, groups, a.heads(), List.of(), true); var state = new State(token);
                var panel = new MergeEditorPanel(MergeInputs.capture(state.value, token), MergeDraft::close, () -> {}); assertEquals(2, setups(panel).size()); panel.retire();
            }
        });
    }
    @Test void everyCustomSetupControlAutoSelectsAndSwitchClearsSecret() throws Exception {
        edt(() -> {
            var token = token(1, active("A"), active("B")); var state = new State(token);
            var panel = new MergeEditorPanel(MergeInputs.capture(state.value, token), MergeDraft::close, () -> {});
            var existing = setups(panel).get(0); var custom = option(panel, "Use a different authenticator setup");
            var secret = TokenBrowserTest.find(panel, JPasswordField.class); var period = TokenBrowserTest.find(panel, JSpinner.class);
            for (Runnable action : List.<Runnable>of(() -> focus(secret), () -> secret.setText("MY"), () -> option(panel, "SHA256").doClick(),
                    () -> option(panel, "8").doClick(), () -> focus(((JSpinner.DefaultEditor) period.getEditor()).getTextField()), () -> period.setValue(45L))) {
                existing.doClick(); assertFalse(custom.isSelected()); action.run(); assertTrue(custom.isSelected());
            }
            secret.setText("MY"); existing.doClick(); assertEquals(0, secret.getPassword().length); assertFalse(custom.isSelected());
            secret.setText("MY"); panel.cancel(); assertEquals(0, secret.getPassword().length);
        });
    }
    @Test void textValuesDeduplicateAndClickFocusAndTypingSelectLiteralCustom() throws Exception {
        edt(() -> {
            var a = active("A"); var b = active("A"); var c = active("<html>B"); var token = token(1, a, b, c); var state = new State(token);
            var panel = new MergeEditorPanel(MergeInputs.capture(state.value, token), MergeDraft::close, () -> {});
            var existing = components(panel).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast)
                    .filter(f -> f.getAccessibleContext().getAccessibleName() != null && f.getAccessibleContext().getAccessibleName().startsWith("Issuer choice:")).toList();
            assertEquals(2, existing.size()); assertEquals(List.of("A", "<html>B"), existing.stream().map(JTextField::getText).toList());
            var custom = components(panel).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast)
                    .filter(f -> "Enter a different issuer value".equals(f.getAccessibleContext().getAccessibleName())).findFirst().orElseThrow();
            var radios = components(panel).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast)
                    .filter(r -> r.getAccessibleContext().getAccessibleName().startsWith("Issuer choice:") || r.getAccessibleContext().getAccessibleName().equals("Enter a different issuer value")).toList();
            assertTrue(radios.stream().noneMatch(JRadioButton::isSelected));
            focus(existing.get(1)); assertTrue(radios.get(1).isSelected()); assertFalse(existing.get(1).isEditable());
            focus(custom); assertTrue(radios.getLast().isSelected()); custom.setText("retained"); focus(existing.get(0)); assertEquals("retained", custom.getText());
            custom.setText("new"); assertTrue(radios.getLast().isSelected()); panel.retire();
        });
    }
    @Test void resolverLayoutAndStatusUseEditorControlsWithoutProtocolLanguage() throws Exception {
        edt(() -> {
            var a = active("A"); var b = alternative(TokenStatus.TOMBSTONED, "B", "account", TotpAlgorithm.SHA1, 6, 30); var token = token(1, a, b); var state = new State(token);
            var panel = new MergeEditorPanel(MergeInputs.capture(state.value, token), MergeDraft::close, () -> {});
            assertFalse(option(panel, "Active").isSelected()); assertFalse(option(panel, "Deleted").isSelected());
            assertFalse(option(panel, "Save").isEnabled()); assertEquals(TokenEditorPanel.PREFERRED_SIZE, panel.getPreferredSize());
            assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, TokenBrowserTest.find(panel, JScrollPane.class).getHorizontalScrollBarPolicy());
            var spinner = TokenBrowserTest.find(panel, JSpinner.class); var model = (SpinnerNumberModel) spinner.getModel();
            assertEquals(1L, model.getMinimum()); assertEquals(4294967295L, model.getMaximum()); assertInstanceOf(Long.class, model.getValue());
            for (String forbidden : List.of("Heads", "TOMBSTONED", "full-frontier", "Alternative 1", "revisions")) {
                assertTrue(components(panel).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast).noneMatch(button -> button.getText().contains(forbidden)));
            }
            panel.retire();
        });
    }
}
