package org.totipo.desktop.ui;

import org.totipo.*;
import org.totipo.desktop.TokenDraft;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.FocusEvent;
import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.junit.jupiter.api.Assertions.*;

class TokenEditorPanelTest {
    private static TokenDescriptor descriptor(TokenStatus status, TotpAlgorithm algorithm, int digits, long period) {
        return new TokenDescriptor(status, "<html>issuer", "account", algorithm, digits, Duration.ofSeconds(period));
    }
    private static TokenEditorPanel edit() {
        return new TokenEditorPanel(descriptor(TokenStatus.ACTIVE, TotpAlgorithm.SHA1, 6, 30),
                "This edit is based on the token value observed when the editor was opened. "
                + "Later concurrent changes are not automatically included.", TokenDraft::close, () -> {});
    }
    private static JFormattedTextField periodInput(TokenEditorPanel panel) {
        return ((JSpinner.DefaultEditor) panel.period.getEditor()).getTextField();
    }
    private static List<Component> components(Container parent) {
        List<Component> result = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            result.add(child);
            if (child instanceof Container container) { result.addAll(components(container)); }
        }
        return result;
    }
    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) { if (child instanceof Container container) { layout(container); } }
    }

    @Test void existingUntrustedFieldsRoundTripWithoutNewlineFilteringOrNormalization() throws Exception {
        edt(() -> {
            var descriptor = new TokenDescriptor(TokenStatus.ACTIVE, "  <html>issuer\n\0e\u0301\\  ",
                    "Account\r\n\t\u202E", TotpAlgorithm.SHA1, 6, Duration.ofSeconds(30));
            AtomicInteger submits = new AtomicInteger();
            var panel = new TokenEditorPanel(descriptor, "Update", draft -> {
                submits.incrementAndGet(); draft.close();
            }, () -> {});
            assertEquals(descriptor.issuer(), panel.issuer.getText());
            assertEquals(descriptor.account(), panel.account.getText());
            assertNull(panel.issuer.getClientProperty("html")); assertNull(panel.account.getClientProperty("html"));
            panel.save.doClick(); assertEquals(1, submits.get()); panel.retire();
        });
    }
    @Test void defaultsRequiredSecretValidationAndSingleSubmission() throws Exception {
        edt(() -> {
            AtomicInteger submits = new AtomicInteger();
            var panel = new TokenEditorPanel(null, "Create", draft -> { submits.incrementAndGet(); draft.close(); }, () -> {});
            assertEquals("", panel.issuer.getText()); assertEquals("", panel.account.getText());
            assertEquals(TotpAlgorithm.SHA1, panel.algorithm.selected());
            assertEquals(6, panel.digits.selected()); assertEquals(30L, panel.period.getValue());
            assertEquals(TokenStatus.ACTIVE, panel.status.selected());
            assertTrue(panel.secret.isVisible()); assertTrue(panel.secret.isEnabled());
            assertFalse(components(panel).contains(panel.replace)); assertFalse(components(panel).contains(panel.status));
            assertEquals("Create", panel.save.getText());
            assertFalse(panel.message.isVisible());
            panel.save.doClick(); assertEquals(0, submits.get()); assertTrue(panel.save.isEnabled());
            assertTrue(panel.message.isVisible()); assertTrue(panel.message.getText().contains("valid Base32"));
            assertThrows(IllegalArgumentException.class, () -> panel.digits.select(9));
            panel.secret.setText("MY");
            for (String invalid : List.of("1.5", "4294967296", "0")) {
                periodInput(panel).setText(invalid); panel.save.doClick(); assertEquals(0, submits.get());
            }
            periodInput(panel).setText("4294967295"); panel.save.doClick(); panel.save.doClick();
            assertEquals(1, submits.get()); assertEquals(0, panel.secret.getPassword().length);
            assertFalse(panel.canCancel()); panel.retire();
        });
    }
    @Test void updatePrefillReplacementOptInAndCloseClearing() throws Exception {
        edt(() -> {
            var descriptor = descriptor(TokenStatus.TOMBSTONED, TotpAlgorithm.SHA512, 8, 60);
            var panel = new TokenEditorPanel(descriptor, "Update", TokenDraft::close, () -> {});
            assertEquals(descriptor.issuer(), panel.issuer.getText()); assertEquals("account", panel.account.getText());
            assertEquals(TokenStatus.TOMBSTONED, panel.status.selected());
            assertEquals(TotpAlgorithm.SHA512, panel.algorithm.selected());
            assertEquals(8, panel.digits.selected()); assertEquals(60L, panel.period.getValue());
            assertFalse(panel.secret.isEnabled()); assertTrue(panel.secret.isVisible()); assertTrue(panel.secretLabel.isVisible());
            assertFalse(panel.secretLabel.isEnabled());
            assertEquals(0, panel.secret.getPassword().length);
            panel.setSize(panel.getPreferredSize()); Dimension size = panel.getSize();
            panel.replace.doClick(); assertTrue(panel.secret.isEnabled()); assertTrue(panel.secret.isVisible());
            assertTrue(panel.secretLabel.isVisible()); panel.secret.setText("MY");
            assertEquals(size, panel.getSize());
            panel.replace.doClick(); assertEquals(0, panel.secret.getPassword().length);
            assertTrue(panel.secret.isVisible()); assertTrue(panel.secretLabel.isVisible());
            assertFalse(panel.secret.isEnabled()); assertFalse(panel.secretLabel.isEnabled());
            assertEquals(size, panel.getSize());
            panel.secret.setText("invalid"); // Disabled input must not be read or decoded.
            try (TokenDraft draft = panel.draft()) { assertNotNull(draft); }
            panel.retire(); assertEquals(0, panel.secret.getPassword().length);
        });
    }
    @Test void temporaryCopiesWipedOnSuccessAndFailureAndCancelClearsField() throws Exception {
        char[] valid = {'M', 'Y'};
        byte[] decoded = TokenEditorPanel.decodeAndWipe(valid);
        assertArrayEquals(new char[2], valid); java.util.Arrays.fill(decoded, (byte) 0);
        char[] bad = {'M', '0'};
        assertThrows(IllegalArgumentException.class, () -> TokenEditorPanel.decodeAndWipe(bad));
        assertArrayEquals(new char[2], bad);
        edt(() -> {
            AtomicInteger cancels = new AtomicInteger();
            var panel = new TokenEditorPanel(null, "", TokenDraft::close, cancels::incrementAndGet);
            panel.secret.setText("MY"); panel.cancel.doClick();
            assertEquals(1, cancels.get()); assertEquals(0, panel.secret.getPassword().length);
        });
    }

    @Test void sizesAreExplicitAppliedAfterPackAndFitSmallDisplays() throws Exception {
        edt(() -> {
            for (TokenEditorPanel panel : List.of(edit(), new TokenEditorPanel(null, "Create", TokenDraft::close, () -> {}))) {
                assertEquals(new Dimension(760, 720), panel.getPreferredSize());
                assertEquals(new Dimension(640, 580), panel.getMinimumSize());
                assertTrue(panel.isPreferredSizeSet()); assertTrue(panel.isMinimumSizeSet());
                JPanel shell = new JPanel();
                AtomicInteger packs = new AtomicInteger();
                TokenEditDialog.sizeAfterPack(shell, () -> { packs.incrementAndGet(); shell.setSize(100, 100); }, new Dimension(1920, 1080));
                assertEquals(1, packs.get()); assertEquals(panel.getPreferredSize(), shell.getSize());
                assertEquals(panel.getPreferredSize(), shell.getPreferredSize());
                assertEquals(panel.getMinimumSize(), shell.getMinimumSize());
                TokenEditDialog.sizeAfterPack(shell, () -> shell.setSize(100, 100), new Dimension(600, 500));
                assertEquals(new Dimension(600, 500), shell.getSize());
                assertEquals(new Dimension(600, 500), shell.getMinimumSize());
            }
        });
    }

    @Test void formOrderNarrowLabelsExpandingInputsAndFixedTrailingActions() throws Exception {
        edt(() -> {
            var panel = edit();
            List<String> titles = new ArrayList<>();
            int lastRow = -1;
            GridBagLayout grid = (GridBagLayout) panel.fields.getLayout();
            for (Component component : panel.fields.getComponents()) {
                GridBagConstraints constraint = grid.getConstraints(component);
                assertTrue(constraint.gridy >= lastRow); lastRow = constraint.gridy;
                if (component instanceof JLabel label) { titles.add(label.getText()); }
                else if (component == panel.replace) { titles.add("Replace secret"); }
                else if (component instanceof JPanel section && section.getBorder() instanceof javax.swing.border.TitledBorder border) {
                    titles.add(border.getTitle());
                }
            }
            assertEquals(List.of("Token", "Status", "Issuer", "Account", "Algorithm", "Digits",
                    "Period (seconds)", "Secret", "Replace secret", "New Base32 secret"), titles);
            assertEquals(1, grid.getConstraints(panel.issuer).weightx);
            assertEquals(GridBagConstraints.HORIZONTAL, grid.getConstraints(panel.issuer).fill);
            assertEquals(GridBagConstraints.HORIZONTAL, grid.getConstraints(panel.account).fill);
            for (Component component : panel.fields.getComponents()) {
                if (component instanceof JLabel) { assertEquals(0, grid.getConstraints(component).weightx); }
            }
            assertArrayEquals(new Component[]{panel.cancel, panel.save}, panel.actions.getComponents());
            assertEquals(FlowLayout.TRAILING, ((FlowLayout) panel.actions.getLayout()).getAlignment());
            assertFalse(SwingUtilities.isDescendingFrom(panel.actions, panel.scroll));
            assertFalse(panel.message.isVisible()); assertEquals(0, panel.message.getRows());
        });
    }

    @Test void widthTrackingAvoidsHorizontalScrollingWithVerticalFallback() throws Exception {
        edt(() -> {
            var panel = edit();
            assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, panel.scroll.getHorizontalScrollBarPolicy());
            assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, panel.scroll.getVerticalScrollBarPolicy());
            Scrollable body = (Scrollable) panel.scroll.getViewport().getView();
            assertTrue(body.getScrollableTracksViewportWidth()); assertFalse(body.getScrollableTracksViewportHeight());
            panel.setSize(panel.getPreferredSize());
            for (int i = 0; i < 3; i++) { layout(panel); }
            assertFalse(panel.scroll.getHorizontalScrollBar().isVisible());
            assertFalse(panel.scroll.getVerticalScrollBar().isVisible());
            panel.replace.doClick();
            for (int i = 0; i < 3; i++) { layout(panel); }
            assertFalse(panel.scroll.getVerticalScrollBar().isVisible());
            panel.setSize(600, 350);
            for (int i = 0; i < 3; i++) { layout(panel); }
            assertFalse(panel.scroll.getHorizontalScrollBar().isVisible());
            assertTrue(panel.scroll.getVerticalScrollBar().isVisible());
        });
    }

    @Test void statusAlgorithmDigitsAreExclusiveExactAccessibleAndKeyboardReachable() throws Exception {
        edt(() -> {
            var panel = edit();
            assertEquals(List.of(TokenStatus.values()), new ArrayList<>(panel.status.options.keySet()));
            assertEquals(List.of(TotpAlgorithm.values()), new ArrayList<>(panel.algorithm.options.keySet()));
            assertEquals(List.of(6, 7, 8), new ArrayList<>(panel.digits.options.keySet()));
            for (TokenChoice<?> choices : List.of(panel.status, panel.algorithm, panel.digits)) {
                for (var entry : choices.options.entrySet()) {
                    AbstractButton button = entry.getValue();
                    if (choices == panel.status) { assertInstanceOf(JRadioButton.class, button); }
                    else { assertInstanceOf(JToggleButton.class, button); }
                    assertTrue(button.isFocusable());
                    assertEquals(choices.getAccessibleContext().getAccessibleName() + " " + button.getText(),
                            button.getAccessibleContext().getAccessibleName());
                    assertNotNull(button.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("SPACE")));
                    button.doClick(0); assertEquals(entry.getKey(), choices.selected());
                    assertEquals(1, choices.options.values().stream().filter(AbstractButton::isSelected).count());
                    button.doClick(0); assertTrue(button.isSelected());
                }
            }
            assertEquals("Active", panel.status.options.get(TokenStatus.ACTIVE).getText());
            AbstractButton deleted = panel.status.options.get(TokenStatus.TOMBSTONED);
            assertEquals("Deleted", deleted.getText());
            assertEquals("Status Deleted", deleted.getAccessibleContext().getAccessibleName());
            assertTrue(deleted.getAccessibleContext().getAccessibleDescription().contains("logical deletion"));
            assertTrue(deleted.getAccessibleContext().getAccessibleDescription().contains("vault history"));
            assertTrue(deleted.getToolTipText().contains("copies are not erased"));
            assertEquals("Deleted tokens remain in vault history.", panel.statusHelp.getText());
            assertTrue(SwingUtilities.isDescendingFrom(panel.statusHelp, panel.fields));
            panel.busy(true, "Saving…");
            for (TokenChoice<?> choices : List.of(panel.status, panel.algorithm, panel.digits)) {
                assertTrue(choices.options.values().stream().noneMatch(AbstractButton::isEnabled));
            }
        });
    }

    @Test void everySupportedCurrentChoiceIsRepresentedWithoutChangingIt() throws Exception {
        edt(() -> {
            for (TokenStatus status : TokenStatus.values()) {
                for (TotpAlgorithm algorithm : TotpAlgorithm.values()) {
                    for (int digits : new int[]{6, 7, 8}) {
                        var panel = new TokenEditorPanel(descriptor(status, algorithm, digits, 2147483648L),
                                "Edit", TokenDraft::close, () -> {});
                        assertEquals(status, panel.status.selected()); assertEquals(algorithm, panel.algorithm.selected());
                        assertEquals(digits, panel.digits.selected()); assertEquals(2147483648L, panel.period.getValue());
                        assertEquals("2147483648", periodInput(panel).getText()); assertTrue(periodInput(panel).isEditable());
                        for (TokenChoice<?> choices : List.of(panel.status, panel.algorithm, panel.digits)) {
                            assertEquals(1, choices.options.values().stream().filter(AbstractButton::isSelected).count());
                        }
                    }
                }
            }
        });
    }

    @Test void periodRetainsLongBoundsManualIntegerGrammarAndRejectsStaleValue() throws Exception {
        edt(() -> {
            var panel = new TokenEditorPanel(descriptor(TokenStatus.ACTIVE, TotpAlgorithm.SHA1, 6, 4294967295L),
                    "Edit", TokenDraft::close, () -> {});
            SpinnerNumberModel model = (SpinnerNumberModel) panel.period.getModel();
            assertEquals(1L, model.getMinimum()); assertEquals(4294967295L, model.getMaximum());
            assertEquals(1L, model.getStepSize()); assertEquals(4294967295L, panel.period.getValue());
            assertNull(model.getNextValue());
            for (String text : List.of("1", "30", "2147483648", "4294967295", "+42", "00030")) {
                periodInput(panel).setText(text);
                try (TokenDraft draft = panel.draft()) { assertNotNull(draft); }
                assertEquals(Long.parseLong(text), panel.period.getValue());
                assertInstanceOf(Long.class, panel.period.getValue());
            }
            model.setValue(1L); assertNull(model.getPreviousValue());
            for (String text : List.of("", "0", "-1", "4294967296", "9223372036854775808", "1.5", "1e2", "1junk", "1,000", " 30 ")) {
                periodInput(panel).setText(text);
                assertThrows(ParseException.class, panel.period::commitEdit, text);
                assertThrows(IllegalArgumentException.class, panel::draft, text);
                assertEquals(1L, panel.period.getValue());
            }
            // Programmatic values still pass through TokenDescriptor validation as defense in depth.
            for (long invalid : new long[]{0L, 4294967296L}) {
                panel.period.setValue(invalid); assertThrows(IllegalArgumentException.class, panel::draft);
            }
        });
    }

    @Test void normalFormsBeginWithTokenContentWithoutDocumentationButKeepRuntimeMessages() throws Exception {
        edt(() -> {
            String explanation = "<html>Alternative 2 only; observed when the editor opened. "
                    + "Later concurrent changes are not automatically included. TOMBSTONED means logical deletion.";
            for (boolean create : List.of(false, true)) {
                var panel = new TokenEditorPanel(create ? null : descriptor(TokenStatus.ACTIVE, TotpAlgorithm.SHA1, 6, 30),
                        explanation, TokenDraft::close, () -> {});
                JPanel first = assertInstanceOf(JPanel.class, panel.fields.getComponent(0));
                assertEquals("Token", assertInstanceOf(javax.swing.border.TitledBorder.class, first.getBorder()).getTitle());
                assertEquals(List.of(panel.message), components(panel).stream().filter(JTextArea.class::isInstance).toList());
                assertFalse(panel.message.isVisible());
                for (Component component : components(panel)) {
                    if (component instanceof AbstractButton button) {
                        assertFalse(button.getText().contains("Tombstoned")); assertFalse(button.getText().contains("TOMBSTONED"));
                    }
                    if (component instanceof JLabel label) { assertFalse(label.getText().contains("TOMBSTONED")); }
                }
                String warning = "The token operation could not be published. Review the conflict before trying again.";
                panel.busy(false, warning);
                assertTrue(panel.message.isVisible()); assertEquals(warning, panel.message.getText());
                assertTrue(panel.message.getLineWrap()); assertTrue(panel.message.getWrapStyleWord());
                panel.busy(false, ""); assertFalse(panel.message.isVisible());
            }
        });
    }

    @Test void replacementKeepsRowHierarchyPreferredGeometryAndVisibleViewportStable() throws Exception {
        edt(() -> {
            var panel = edit(); panel.setSize(panel.getPreferredSize());
            for (int i = 0; i < 3; i++) { layout(panel); }
            assertTrue(panel.secret.isVisible()); assertTrue(panel.secretLabel.isVisible());
            assertFalse(panel.secret.isEnabled()); assertEquals(0, panel.secret.getPassword().length);
            assertSame(panel.fields, panel.secret.getParent()); assertSame(panel.fields, panel.secretLabel.getParent());
            List<Component> hierarchy = components(panel.fields);
            Dimension formPreferred = panel.fields.getPreferredSize(); Dimension formSize = panel.fields.getSize();
            Dimension editorSize = panel.getSize();
            List<Rectangle> rowBounds = List.of(panel.fields.getComponents()).stream().map(Component::getBounds).toList();
            Point position = panel.scroll.getViewport().getViewPosition();
            panel.replace.doClick(0); panel.secret.setText("MY");
            for (int i = 0; i < 3; i++) { layout(panel); }
            assertTrue(panel.secret.isEnabled()); assertTrue(panel.secretLabel.isEnabled());
            assertEquals(hierarchy, components(panel.fields)); assertEquals(formPreferred, panel.fields.getPreferredSize());
            assertEquals(formSize, panel.fields.getSize()); assertEquals(editorSize, panel.getSize());
            assertEquals(rowBounds, List.of(panel.fields.getComponents()).stream().map(Component::getBounds).toList());
            assertEquals(position, panel.scroll.getViewport().getViewPosition());
            panel.replace.doClick(0);
            for (int i = 0; i < 3; i++) { layout(panel); }
            assertFalse(panel.secret.isEnabled()); assertTrue(panel.secret.isVisible());
            assertEquals(0, panel.secret.getPassword().length);
            assertEquals(hierarchy, components(panel.fields)); assertEquals(formPreferred, panel.fields.getPreferredSize());
            assertEquals(formSize, panel.fields.getSize()); assertEquals(editorSize, panel.getSize());
            assertEquals(rowBounds, List.of(panel.fields.getComponents()).stream().map(Component::getBounds).toList());
            assertEquals(position, panel.scroll.getViewport().getViewPosition());
        });
    }

    @Test void fieldLikeControlsExpandTogetherWithEqualWidthToggleButtons() throws Exception {
        edt(() -> {
            for (TokenEditorPanel panel : List.of(edit(), new TokenEditorPanel(null, "Create", TokenDraft::close, () -> {}))) {
                GridBagLayout grid = (GridBagLayout) panel.fields.getLayout();
                List<JComponent> controls = List.of(panel.issuer, panel.account, panel.algorithm, panel.digits, panel.period, panel.secret);
                GridBagConstraints reference = grid.getConstraints(panel.issuer);
                for (JComponent control : controls) {
                    assertSame(panel.fields, control.getParent());
                    GridBagConstraints constraint = grid.getConstraints(control);
                    assertEquals(reference.gridx, constraint.gridx); assertEquals(reference.weightx, constraint.weightx);
                    assertEquals(1, constraint.weightx); assertEquals(GridBagConstraints.HORIZONTAL, constraint.fill);
                }
                assertInstanceOf(FlowLayout.class, panel.status.getLayout());
                for (TokenChoice<?> selector : List.of(panel.algorithm, panel.digits)) {
                    GridLayout equal = assertInstanceOf(GridLayout.class, selector.getLayout());
                    assertEquals(1, equal.getRows()); assertEquals(0, equal.getColumns());
                    assertEquals(3, selector.getComponentCount());
                }
                // Compare relative alignment in simulated layouts, never absolute screen pixels.
                for (int width : new int[]{640, 760}) {
                    panel.setSize(width, panel.getPreferredSize().height);
                    for (int i = 0; i < 3; i++) { layout(panel); }
                    for (JComponent control : controls) {
                        assertEquals(panel.issuer.getX(), control.getX()); assertEquals(panel.issuer.getWidth(), control.getWidth());
                    }
                    for (TokenChoice<?> selector : List.of(panel.algorithm, panel.digits)) {
                        int buttonWidth = selector.getComponent(0).getWidth();
                        assertTrue(buttonWidth > 0);
                        for (Component button : selector.getComponents()) { assertEquals(buttonWidth, button.getWidth()); }
                    }
                }
            }
        });
    }

    @Test void smallViewportScrollsStableSecretRowIntoViewOnEnableAndFocus() throws Exception {
        edt(() -> {
            var panel = edit(); panel.setSize(640, 250);
            for (int i = 0; i < 3; i++) { layout(panel); }
            JViewport viewport = panel.scroll.getViewport();
            Dimension preferred = panel.fields.getPreferredSize();
            List<Component> hierarchy = components(panel.fields);
            Rectangle input = SwingUtilities.convertRectangle(panel.secret,
                    new Rectangle(0, 0, panel.secret.getWidth(), panel.secret.getHeight()), viewport.getView());
            assertFalse(viewport.getViewRect().contains(input));
            panel.replace.doClick(0);
            assertTrue(panel.secret.isEnabled());
            assertTrue(viewport.getViewRect().contains(input), () -> "viewport=" + viewport.getViewRect() + ", secret=" + input
                    + ", view size=" + viewport.getViewSize());
            assertEquals(preferred, panel.fields.getPreferredSize()); assertEquals(hierarchy, components(panel.fields));
            Point showing = viewport.getViewPosition(); panel.secret.setText("MY"); panel.replace.doClick(0);
            assertEquals(showing, viewport.getViewPosition()); assertEquals(0, panel.secret.getPassword().length);
            panel.replace.doClick(0); viewport.setViewPosition(new Point(0, 0));
            FocusEvent focus = new FocusEvent(panel.secret, FocusEvent.FOCUS_GAINED);
            for (var listener : panel.secret.getFocusListeners()) { listener.focusGained(focus); }
            assertTrue(viewport.getViewRect().contains(input));
            assertEquals(preferred, panel.fields.getPreferredSize()); assertEquals(hierarchy, components(panel.fields));
        });
    }

    @Test void actionDefaultsEscapeAndToggleActivationDoNotAccidentallySubmit() throws Exception {
        edt(() -> {
            for (boolean create : List.of(false, true)) {
                AtomicInteger submits = new AtomicInteger(); AtomicInteger cancels = new AtomicInteger();
                var panel = new TokenEditorPanel(create ? null : descriptor(TokenStatus.ACTIVE, TotpAlgorithm.SHA1, 6, 30),
                        "Context", draft -> { submits.incrementAndGet(); draft.close(); }, cancels::incrementAndGet);
                JRootPane root = new JRootPane(); root.setContentPane(panel); panel.installDialog(root);
                assertSame(panel.save, root.getDefaultButton()); assertEquals(create ? "Create" : "Save", panel.save.getText());
                assertArrayEquals(new Component[]{panel.cancel, panel.save}, panel.actions.getComponents());
                for (AbstractButton button : panel.algorithm.options.values()) {
                    UsabilityTest.invoke(button, JComponent.WHEN_FOCUSED, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
                    UsabilityTest.invoke(button, JComponent.WHEN_FOCUSED, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true));
                }
                assertEquals(0, submits.get());
                panel.secret.setText("MY"); panel.busy(true, "Saving…");
                UsabilityTest.invoke(root, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke("ESCAPE"));
                assertEquals(0, cancels.get());
                panel.busy(false, "");
                UsabilityTest.invoke(root, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke("ESCAPE"));
                assertEquals(1, cancels.get()); assertEquals(0, panel.secret.getPassword().length);
                assertFalse(panel.canCancel()); assertEquals(0, submits.get());
            }
        });
    }

    @Test void invalidReplacementCannotSubmitAndIsClearedForRetry() throws Exception {
        edt(() -> {
            AtomicInteger submits = new AtomicInteger();
            var panel = new TokenEditorPanel(descriptor(TokenStatus.TOMBSTONED, TotpAlgorithm.SHA512, 8, 60),
                    "Edit", draft -> { submits.incrementAndGet(); draft.close(); }, () -> {});
            panel.replace.doClick();
            for (String invalid : List.of("", "M0", "MZ")) {
                panel.secret.setText(invalid); panel.save.doClick(0);
                assertEquals(0, submits.get()); assertEquals(0, panel.secret.getPassword().length);
                assertTrue(panel.message.getText().contains("valid Base32")); assertTrue(panel.save.isEnabled());
            }
            panel.secret.setText("MY"); panel.save.doClick(0);
            assertEquals(1, submits.get()); assertEquals(0, panel.secret.getPassword().length);
        });
    }
}
