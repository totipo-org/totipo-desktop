package org.totipo.desktop;

import org.totipo.*;
import org.totipo.desktop.ui.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.MergeFixtures.*;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.TokenWriteControllerTest.*;
import static org.junit.jupiter.api.Assertions.*;

class MergeEditorTest {
    static AbstractButton option(java.awt.Container panel, String text) {
        return components(panel).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast).filter(b -> text.equals(b.getText())).findFirst().orElseThrow();
    }
    static void chooseFirst(MergeEditorPanel panel) {
        for (String name : List.of("Issuer", "Account")) {
            var fields = components(panel).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast)
                    .filter(f -> f.getAccessibleContext().getAccessibleName() != null && f.getAccessibleContext().getAccessibleName().startsWith(name + " choice:")).toList();
            if (!fields.isEmpty()) { focus(fields.get(0)); }
        }
        option(panel, "Active").doClick();
        components(panel).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast)
                .filter(b -> b.getText().startsWith("Setup used by") || b.getText().startsWith("Existing setup") || b.getText().equals("Keep existing setup"))
                .findFirst().orElseThrow().doClick();
    }
    static void focus(JComponent field) { for (var listener : field.getFocusListeners()) { listener.focusGained(new java.awt.event.FocusEvent(field, java.awt.event.FocusEvent.FOCUS_GAINED)); } }
    static JTextField customText(MergeEditorPanel panel, String name) {
        return components(panel).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast)
                .filter(f -> ("Enter a different " + name + " value").equals(f.getAccessibleContext().getAccessibleName())).findFirst().orElseThrow();
    }
    @Test void directResolverNoImplicitWinnerAndAtomicExistingSetups() throws Exception {
        for (int selected = 0; selected < 3; selected++) {
            final int index = selected;
            var fake = new Recording(); fake.results.add(TokenWritesTest.saved()); var submitted = new AtomicReference<MergeDraft>();
            edt(() -> {
                var panel = new MergeEditorPanel(fake.inputs(), submitted::set, () -> {}); button(panel, "Combine details…").doClick();
                assertTrue(button(panel, "Save Resolution").isEnabled()); button(panel, "Save Resolution").doClick(); assertNull(submitted.get());
                assertTrue(components(panel).stream().noneMatch(JComboBox.class::isInstance));
                assertTrue(components(panel).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).noneMatch(JRadioButton::isSelected));
                assertTrue(button(panel, "Back").isEnabled());
                chooseFirst(panel);
                if (index == 1) { option(panel, "Deleted").doClick(); }
                var setups = components(panel).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).filter(b -> b.getText().startsWith("Setup used by")).toList();
                setups.get(index).doClick(); button(panel, "Save Resolution").doClick(); assertNotNull(submitted.get()); panel.retire();
            });
            assertInstanceOf(SaveResult.Saved.class, MergeWrites.save(submitted.get()));
            var expected = fake.token.alternatives().get(index);
            assertTrue(fake.used.get(0).alternatives().contains(expected));
            assertEquals(expected.descriptor().algorithm(), fake.values.get("algorithm"));
            assertEquals(expected.descriptor().digits(), fake.values.get("digits"));
            assertEquals(expected.descriptor().period(), fake.values.get("period"));
            assertEquals(index == 1 ? TokenStatus.TOMBSTONED : TokenStatus.ACTIVE, fake.values.get("status"));
            assertEquals(List.of(ID), fake.factories);
        }
    }
    @Test void literalReadonlyChoicesCustomAutoSelectionAndEmptyValue() throws Exception {
        var fake = new Recording(); fake.results.add(TokenWritesTest.saved()); var submitted = new AtomicReference<MergeDraft>();
        edt(() -> {
            var panel = new MergeEditorPanel(fake.inputs(), submitted::set, () -> {}); button(panel, "Combine details…").doClick(); chooseFirst(panel);
            for (var component : components(panel)) {
                if (component instanceof JTextField field && field.getAccessibleContext().getAccessibleName() != null && field.getAccessibleContext().getAccessibleName().contains(" choice:")) {
                    assertFalse(field.isEditable()); assertTrue(field.isEnabled());
                }
            }
            var issuer = customText(panel, "issuer"); focus(issuer); issuer.setText("<html>literal\n issuer");
            var account = customText(panel, "account"); focus(account); assertTrue(button(panel, "Save Resolution").isEnabled());
            button(panel, "Save Resolution").doClick(); panel.retire();
        });
        MergeWrites.save(submitted.get()); assertEquals("<html>literal\n issuer", fake.values.get("issuer")); assertEquals("", fake.values.get("account"));
    }
    @Test void agreedFieldsEditableAndSetupDeduplicatesPublicEquality() throws Exception {
        edt(() -> {
            var fake = new Recording(List.of(alternative(0), alternative(0)), true);
            var panel = new MergeEditorPanel(fake.inputs(), MergeDraft::close, () -> {}); button(panel, "Combine details…").doClick();
            assertTrue(button(panel, "Save Resolution").isEnabled());
            assertEquals(1, components(panel).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).filter(b -> b.getText().equals("Keep existing setup")).count());
            assertEquals("issuer 0", customText(panel, "issuer").getText()); customText(panel, "issuer").setText("changed");
            assertTrue(button(panel, "Save Resolution").isEnabled()); panel.retire();
        });
    }
    @Test void customSetupValidatesAndClearsSecretOnSwitchAndCancel() throws Exception {
        var fake = new Recording(); fake.ownedSecret = new byte[1]; fake.results.add(TokenWritesTest.saved()); var submitted = new AtomicReference<MergeDraft>();
        edt(() -> {
            var panel = new MergeEditorPanel(fake.inputs(), submitted::set, () -> {}); button(panel, "Combine details…").doClick(); chooseFirst(panel);
            focus(password(panel)); assertTrue(option(panel, "Use a different authenticator setup").isSelected());
            password(panel).setText("MY"); chooseFirst(panel); assertEquals(0, password(panel).getPassword().length);
            password(panel).setText("!"); button(panel, "Save Resolution").doClick(); assertNull(submitted.get()); assertEquals(0, password(panel).getPassword().length);
            password(panel).setText("MY"); option(panel, "SHA512").doClick(); option(panel, "8").doClick();
            var spinner = components(panel).stream().filter(JSpinner.class::isInstance).map(JSpinner.class::cast).findFirst().orElseThrow(); spinner.setValue(4294967295L);
            button(panel, "Save Resolution").doClick(); assertNotNull(submitted.get()); assertEquals(0, password(panel).getPassword().length); panel.retire();
            var cancelled = new MergeEditorPanel(fake.inputs(), d -> fail(), () -> {}); button(cancelled, "Combine details…").doClick(); password(cancelled).setText("MY"); cancelled.cancel(); assertEquals(0, password(cancelled).getPassword().length);
        });
        MergeWrites.save(submitted.get()); assertEquals(TotpAlgorithm.SHA512, fake.values.get("algorithm")); assertEquals(8, fake.values.get("digits")); assertEquals(java.time.Duration.ofSeconds(4294967295L), fake.values.get("period")); assertTrue(fake.used.isEmpty());
    }
    static String text(java.awt.Container panel) { return components(panel).stream().filter(JTextArea.class::isInstance).map(JTextArea.class::cast).map(JTextArea::getText).collect(java.util.stream.Collectors.joining("\n")); }
}
