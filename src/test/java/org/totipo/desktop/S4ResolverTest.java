package org.totipo.desktop;

import org.totipo.*;
import org.totipo.desktop.ui.*;
import javax.swing.*;
import java.awt.event.FocusEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.MergeFixtures.*;
import static org.totipo.desktop.MergeEditorTest.*;
import static org.totipo.desktop.TokenWriteControllerTest.*;

class S4ResolverTest {
    static List<JRadioButton> radios(java.awt.Container panel) {
        return components(panel).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).toList();
    }
    @Test void noPreselectionFocusDoesNotChooseAndActivationValidatesWithoutDraft() throws Exception {
        edt(() -> {
            Recording fake = new Recording(); AtomicInteger submits = new AtomicInteger(); AtomicInteger cancelled = new AtomicInteger();
            var panel = new MergeEditorPanel(fake.inputs(), draft -> { submits.incrementAndGet(); draft.close(); }, cancelled::incrementAndGet);
            assertEquals(3, radios(panel).size()); assertTrue(radios(panel).stream().noneMatch(AbstractButton::isSelected));
            focus(radios(panel).getFirst()); assertTrue(radios(panel).stream().noneMatch(AbstractButton::isSelected));
            assertTrue(button(panel, "Resolve").isEnabled()); button(panel, "Resolve").doClick();
            assertEquals(0, submits.get()); assertTrue(text(panel).contains("Choose a version to keep."));
            panel.cancel(); assertEquals(1, cancelled.get()); assertTrue(fake.calls.isEmpty());
        });
    }
    @Test void selectedVersionSubmitsKeepWithoutComposition() throws Exception {
        Recording fake = new Recording(); fake.results.add(TokenWritesTest.saved()); AtomicReference<MergeDraft> submitted = new AtomicReference<>();
        edt(() -> {
            var panel = new MergeEditorPanel(fake.inputs(), submitted::set, () -> {});
            radios(panel).get(1).doClick(); button(panel, "Resolve").doClick(); panel.retire();
        });
        assertInstanceOf(SaveResult.Saved.class, MergeWrites.save(submitted.get()));
        assertEquals(List.of("merge", "keep", "save", "close"), fake.calls);
        assertSame(fake.token.alternatives().get(1), fake.values.get("keep"));
    }
    @Test void indistinguishableVersionsRemainDistinctWithoutHeadOrCodeInspection() throws Exception {
        TokenDescriptor d = alternative(0).descriptor();
        TokenAlternative a = S3MutationTest.alternative(d), b = S3MutationTest.alternative(d);
        Recording fake = new Recording(List.of(a, b), false);
        edt(() -> {
            var panel = new MergeEditorPanel(fake.inputs(), MergeDraft::close, () -> {});
            assertEquals(2, radios(panel).size());
            assertEquals(List.of("Version 1 · issuer 0 · account 0", "Version 2 · issuer 0 · account 0"), radios(panel).stream().map(AbstractButton::getText).toList());
            assertTrue(radios(panel).stream().noneMatch(AbstractButton::isSelected));
            for (JRadioButton button : radios(panel)) {
                String name = button.getAccessibleContext().getAccessibleName();
                assertTrue(name.contains("SHA1")); assertFalse(name.contains("Newest")); assertFalse(name.contains("Recommended"));
            }
            assertTrue(components(panel).stream().noneMatch(JPasswordField.class::isInstance));
            button(panel, "Combine details…").doClick(); assertEquals(2, radios(panel).stream().filter(r -> r.getText().startsWith("Existing setup")).count());
            panel.retire();
        });
        assertTrue(fake.calls.isEmpty());
    }
    @Test void detailedDoesNotInheritWholeChoiceAndBackDiscardsChoicesAndSecret() throws Exception {
        edt(() -> {
            Recording fake = new Recording(); AtomicReference<MergeDraft> submitted = new AtomicReference<>();
            var panel = new MergeEditorPanel(fake.inputs(), submitted::set, () -> {});
            radios(panel).get(1).doClick(); button(panel, "Combine details…").doClick();
            assertTrue(radios(panel).stream().noneMatch(AbstractButton::isSelected));
            assertTrue(button(panel, "Save Resolution").isEnabled()); button(panel, "Save Resolution").doClick();
            assertNull(submitted.get()); assertTrue(text(panel).contains("Choose an issuer")); assertTrue(text(panel).contains("Choose an account"));
            assertTrue(text(panel).contains("Choose Active or Deleted")); assertTrue(text(panel).contains("Choose an authenticator setup"));
            JPasswordField secret = password(panel); secret.setText("MY"); button(panel, "Back").doClick();
            assertEquals(0, secret.getPassword().length); assertTrue(radios(panel).stream().noneMatch(AbstractButton::isSelected));
            button(panel, "Resolve").doClick(); assertNull(submitted.get()); button(panel, "Combine details…").doClick();
            assertEquals(0, password(panel).getPassword().length); assertTrue(radios(panel).stream().noneMatch(AbstractButton::isSelected));
            panel.cancel(); assertNull(submitted.get());
        });
    }
    @Test void equalTextValuesDeduplicateAndSecretEqualityDoesNotFollowVisibleConfiguration() throws Exception {
        TokenAlternative a = alternative(0), b = alternative(0), c = alternative(1);
        edt(() -> {
            var panel = new MergeEditorPanel(new Recording(List.of(a, b, c), false).inputs(), MergeDraft::close, () -> {});
            button(panel, "Combine details…").doClick();
            assertEquals(2, components(panel).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast)
                    .filter(f -> f.getAccessibleContext().getAccessibleName() != null && f.getAccessibleContext().getAccessibleName().startsWith("Issuer choice:")).count());
            assertEquals(3, radios(panel).stream().filter(r -> r.getText().startsWith("Existing setup") || r.getText().startsWith("Setup used by")).count());
            assertEquals(List.of("Active", "Deleted"), radios(panel).stream().map(AbstractButton::getText).filter(t -> t.equals("Active") || t.equals("Deleted")).toList());
            panel.retire();
        });
    }
    @Test void setupEqualityRequiresBothPublicSecretGroupAndAllConfigurationFields() throws Exception {
        TokenAlternative a = alternative(0), b = alternative(0), c = alternative(1);
        for (boolean equalSecret : List.of(false, true)) {
            edt(() -> {
                var panel = new MergeEditorPanel(new Recording(List.of(a, b, c), equalSecret).inputs(), MergeDraft::close, () -> {});
                button(panel, "Combine details…").doClick();
                assertEquals(equalSecret ? 2 : 3, radios(panel).stream().filter(r -> r.getText().startsWith("Setup used by") || r.getText().startsWith("Existing setup")).count());
                panel.retire();
            });
        }
    }
    @Test void customFocusChoosesAndEveryAbandonmentClearsSecret() throws Exception {
        edt(() -> {
            Recording fake = new Recording();
            for (String abandon : List.of("existing", "back", "cancel", "retire", "changed")) {
                var panel = new MergeEditorPanel(fake.inputs(), draft -> fail(), () -> {}); button(panel, "Combine details…").doClick();
                JPasswordField secret = password(panel); focus(secret); assertTrue(option(panel, "Use a different authenticator setup").isSelected());
                secret.setText("MY");
                assertNotEquals(0, secret.getEchoChar());
                switch (abandon) {
                    case "existing" -> option(panel, "Setup used by issuer 0 · account 0").doClick();
                    case "back" -> button(panel, "Back").doClick();
                    case "cancel" -> panel.cancel(); case "changed" -> panel.changed(() -> {}); default -> panel.retire();
                }
                assertEquals(0, secret.getPassword().length); panel.retire();
            }
        });
    }
    @Test void textRowsSelectOnFocusOrTypingAndDoNotLookDisabled() throws Exception {
        edt(() -> {
            var panel = new MergeEditorPanel(new Recording().inputs(), MergeDraft::close, () -> {}); button(panel, "Combine details…").doClick();
            var existing = components(panel).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast)
                    .filter(f -> "Issuer choice: issuer 1".equals(f.getAccessibleContext().getAccessibleName())).findFirst().orElseThrow();
            assertTrue(existing.isEnabled()); assertFalse(existing.isEditable()); focus(existing);
            assertTrue(radios(panel).stream().anyMatch(r -> r.isSelected() && r.getAccessibleContext().getAccessibleName().equals("Issuer choice: issuer 1")));
            JTextField custom = customText(panel, "issuer"); custom.setText("Custom");
            assertTrue(radios(panel).stream().anyMatch(r -> r.isSelected() && r.getAccessibleContext().getAccessibleName().equals("Enter a different issuer value")));
            panel.retire();
        });
    }
    @Test void customValidationUsesS3RulesAndPublishesNothing() throws Exception {
        edt(() -> {
            AtomicReference<MergeDraft> submitted = new AtomicReference<>();
            var panel = new MergeEditorPanel(new Recording().inputs(), submitted::set, () -> {});
            button(panel, "Combine details…").doClick(); chooseFirst(panel);
            password(panel).setText("MY");
            JSpinner spinner = components(panel).stream().filter(JSpinner.class::isInstance).map(JSpinner.class::cast).findFirst().orElseThrow();
            ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setText("0");
            button(panel, "Save Resolution").doClick(); assertNull(submitted.get()); assertTrue(text(panel).contains("whole seconds"));
            assertEquals(0, password(panel).getPassword().length);
            panel.retire();
        });
    }

}
