package org.totipo.desktop.ui;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.totipo.desktop.ShellState;
import static org.totipo.desktop.TestSupport.edt;
import static org.junit.jupiter.api.Assertions.*;

class ShellPanelTest {
    @Test void edtGuardRejectsBackgroundThread() {
        assertThrows(IllegalStateException.class, ShellPanel::new);
        assertThrows(IllegalStateException.class, VaultPanel::new);
    }
    @Test void shellUsesComfortablePaddingAndDefaultPrimaryButton() throws Exception {
        edt(() -> {
            ShellPanel panel = new ShellPanel(); var root = new javax.swing.JRootPane(); root.setContentPane(panel);
            panel.render(ShellState.NO_VAULT, null, "", false); assertSame(panel.primary, root.getDefaultButton());
            assertTrue(panel.getInsets().top >= 24); assertTrue(panel.getInsets().left >= 24);
            panel.render(ShellState.LOCKED, Path.of("vault"), "", false); assertSame(panel.primary, root.getDefaultButton());
        });
    }
    @Test void selectAndCreateCallbacksAreUnavailableOnlyWhileBusy() throws Exception {
        edt(() -> {
            ShellPanel panel = new ShellPanel(); AtomicInteger selects = new AtomicInteger(), creates = new AtomicInteger();
            panel.actions(selects::incrementAndGet, creates::incrementAndGet, password -> fail());
            panel.render(ShellState.NO_VAULT, null, "", false); panel.primary.doClick(0); panel.secondary.doClick(0);
            assertEquals(1, selects.get()); assertEquals(1, creates.get());
            panel.render(ShellState.NO_VAULT, null, "", true); panel.primary.doClick(0); panel.secondary.doClick(0);
            assertEquals(1, selects.get()); assertEquals(1, creates.get());
            panel.render(ShellState.NO_VAULT, null, "", false); assertTrue(panel.primary.isEnabled()); assertTrue(panel.secondary.isEnabled());
        });
    }
    @Test void noVaultHasOnePrimaryActionAndBorderedSecondaryCreate() throws Exception {
        edt(() -> {
            ShellPanel panel = new ShellPanel(); panel.render(ShellState.NO_VAULT, null, "", false);
            assertEquals("Select Vault", panel.primary.getText()); assertEquals("New Vault…", panel.secondary.getText());
            assertFalse(panel.password.isVisible()); assertFalse(panel.path.isVisible());
            assertEquals("Choose a vault to continue", panel.identity.getText()); assertFalse(panel.status.isVisible());
            assertEquals(DesktopStyle.ActionRole.PrimaryAction, panel.primary.getClientProperty("totipo.actionRole"));
            assertEquals(DesktopStyle.ActionRole.SecondaryAction, panel.secondary.getClientProperty("totipo.actionRole"));
            assertTrue(panel.secondary.isContentAreaFilled());
            assertInstanceOf(DesktopStyle.ControlBorder.class, panel.secondary.getBorder());
            assertEquals(panel.primary.getPreferredSize(), panel.secondary.getPreferredSize());
            assertEquals("Select Vault", panel.primary.getAccessibleContext().getAccessibleName());
            assertEquals("New Vault…", panel.secondary.getAccessibleContext().getAccessibleName());
        });
    }
    @Test void noVaultStackHasVisibleNonOverlappingBoundsEvenWithLargeHeading() throws Exception {
        edt(() -> {
            ShellPanel panel = new ShellPanel();
            for (float fontSize : new float[] {panel.identity.getFont().getSize2D(), 32f}) {
                panel.identity.setFont(panel.identity.getFont().deriveFont(fontSize));
                panel.render(ShellState.NO_VAULT, null, "", false);
                for (java.awt.Dimension size : new java.awt.Dimension[] {
                        VaultPanel.MINIMUM_SIZE, VaultPanel.INITIAL_SIZE,
                        new java.awt.Dimension(400, 360), new java.awt.Dimension(1200, 1000)}) {
                    panel.setSize(size); layout(panel);
                    var visible = panel.welcome.getVisibleRect();
                    java.awt.Rectangle previous = null;
                    for (javax.swing.JComponent component : new javax.swing.JComponent[] {
                            panel.identity, panel.primary, panel.secondary}) {
                        var bounds = javax.swing.SwingUtilities.convertRectangle(component.getParent(),
                                component.getBounds(), panel.welcome);
                        assertTrue(bounds.width > 0 && bounds.height > 0, component + " has empty bounds");
                        assertTrue(visible.contains(bounds), bounds + " outside " + visible);
                        assertEquals(visible.getCenterX(), bounds.getCenterX(), .5);
                        if (previous != null) { assertTrue(previous.y + previous.height < bounds.y); }
                        previous = bounds;
                    }
                    assertEquals(panel.primary.getPreferredSize().width, panel.primary.getWidth());
                    assertEquals(panel.secondary.getPreferredSize().width, panel.secondary.getWidth());
                    assertTrue(panel.welcome.content.getY() + panel.welcome.content.getHeight() / 2.0 < size.height / 2.0);
                }
            }
        });
    }
    @Test void ordinaryWelcomeHeadingFitsWithoutEllipsisAtSupportedSizes() throws Exception {
        edt(() -> {
            var original = javax.swing.UIManager.getFont("Label.font");
            try {
                // Also exercise the application's two-point font enlargement.
                for (float enlargement : new float[] {0, 2}) {
                    javax.swing.UIManager.put("Label.font", original.deriveFont(original.getSize2D() + enlargement));
                    var panel = new ShellPanel(); panel.render(ShellState.NO_VAULT, null, "", false);
                    for (java.awt.Dimension size : new java.awt.Dimension[] {VaultPanel.MINIMUM_SIZE, VaultPanel.INITIAL_SIZE}) {
                        panel.setSize(size); layout(panel);
                        var heading = panel.identity; var insets = heading.getInsets();
                        var view = new java.awt.Rectangle(insets.left, insets.top,
                                heading.getWidth() - insets.left - insets.right, heading.getHeight() - insets.top - insets.bottom);
                        String painted = javax.swing.SwingUtilities.layoutCompoundLabel(heading,
                                heading.getFontMetrics(heading.getFont()), heading.getText(), null,
                                heading.getVerticalAlignment(), heading.getHorizontalAlignment(),
                                heading.getVerticalTextPosition(), heading.getHorizontalTextPosition(),
                                view, new java.awt.Rectangle(), new java.awt.Rectangle(), heading.getIconTextGap());
                        assertEquals("Choose a vault to continue", painted);
                        assertTrue(heading.getWidth() >= heading.getPreferredSize().width);
                        assertTrue(heading.getHeight() >= heading.getPreferredSize().height);
                        var bounds = javax.swing.SwingUtilities.convertRectangle(heading.getParent(), heading.getBounds(), panel.welcome);
                        assertTrue(panel.welcome.getVisibleRect().contains(bounds));
                    }
                }
            } finally { javax.swing.UIManager.put("Label.font", original); }
        });
    }
    @Test void lockedIdentityAndFullPathHaveSeparateHierarchy() throws Exception {
        edt(() -> {
            ShellPanel panel = new ShellPanel(); Path path = Path.of("/parent/totipo-vault");
            panel.render(ShellState.LOCKED, path, "", false);
            assertEquals("totipo-vault", panel.identity.getText()); assertEquals(path.toString(), panel.path.getText());
            assertInstanceOf(javax.swing.JLabel.class, panel.path); assertFalse(panel.path.isFocusable());
            assertNull(panel.path.getBorder()); assertFalse(panel.path.isOpaque());
            assertEquals(path.toString(), panel.path.getToolTipText());
            assertEquals(path.toString(), panel.path.getAccessibleContext().getAccessibleDescription());
            assertInstanceOf(javax.swing.JPasswordField.class, panel.password);
            assertTrue(panel.password.isVisible()); assertEquals("Open", panel.primary.getText()); assertEquals("Change Vault…", panel.secondary.getText());
            assertTrue(panel.identity.getFont().getSize2D() > panel.path.getFont().getSize2D());
        });
    }
    @Test void emptyPasswordAndInvalidInputDoNotDisableOpen() throws Exception {
        edt(() -> {
            ShellPanel panel = new ShellPanel(); panel.render(ShellState.LOCKED, Path.of("vault"), "Try again", false);
            assertTrue(panel.primary.isEnabled()); panel.password.setText("\uD800"); assertTrue(panel.primary.isEnabled());
        });
    }
    @Test void submitTransfersBufferAndClearsFieldBeforeCallback() throws Exception {
        edt(() -> {
            ShellPanel panel = new ShellPanel(); panel.render(ShellState.LOCKED, Path.of("vault"), "", false);
            panel.password.setText("secret"); AtomicInteger submits = new AtomicInteger();
            panel.actions(() -> { }, () -> { }, password -> {
                assertEquals(0, panel.password.getDocument().getLength()); assertArrayEquals("secret".toCharArray(), password);
                java.util.Arrays.fill(password, '\0'); submits.incrementAndGet();
            });
            panel.primary.doClick(0); assertEquals(1, submits.get());
        });
    }
    @Test void blockingTryAgainAndChangeVaultAreAvailableWithoutPasswordOrTokens() throws Exception {
        edt(() -> {
            ShellPanel panel = new ShellPanel(); panel.render(ShellState.BLOCKING_VAULT_STATE, Path.of("vault"), "Invalid or unsupported", false);
            assertFalse(panel.password.isVisible()); assertEquals("Try Again", panel.primary.getText()); assertTrue(panel.error.isVisible());
            AtomicInteger retry = new AtomicInteger(), change = new AtomicInteger(); panel.retryAction(retry::incrementAndGet);
            panel.actions(change::incrementAndGet, () -> { }, password -> fail()); panel.primary.doClick(0); panel.secondary.doClick(0);
            assertEquals(1, retry.get()); assertEquals(1, change.get());
        });
    }
    @Test void busyIsTemporaryAndVaultChangesClearPassword() throws Exception {
        edt(() -> {
            ShellPanel panel = new ShellPanel(); panel.render(ShellState.LOCKED, Path.of("a"), "", false); panel.password.setText("draft");
            panel.render(ShellState.LOCKED, Path.of("b"), "", true);
            assertEquals(0, panel.password.getDocument().getLength()); assertFalse(panel.primary.isEnabled());
            panel.render(ShellState.LOCKED, Path.of("b"), "Wrong password", false); assertTrue(panel.primary.isEnabled());
        });
    }
    private static void layout(java.awt.Container container) {
        container.doLayout();
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof java.awt.Container nested) { layout(nested); }
        }
    }
    @Test void tasksStayCenteredBoundedAndAboveVerticalCenterAcrossResizes() throws Exception {
        edt(() -> {
            ShellPanel panel = new ShellPanel();
            for (ShellState state : new ShellState[] {ShellState.NO_VAULT, ShellState.LOCKED}) {
                panel.render(state, state == ShellState.NO_VAULT ? null : Path.of("/parent/vault"), "", false);
                for (java.awt.Dimension size : new java.awt.Dimension[] {
                        new java.awt.Dimension(400, 520), new java.awt.Dimension(640, 520),
                        new java.awt.Dimension(760, 820), new java.awt.Dimension(1200, 1000)}) {
                    panel.setSize(size); layout(panel);
                    var task = (state == ShellState.NO_VAULT ? panel.welcome.content : panel.task).getBounds();
                    assertEquals(size.width / 2.0, task.getCenterX(), .5);
                    assertTrue(task.width <= 560); assertTrue(task.x >= 24); assertTrue(task.x + task.width <= size.width - 24);
                    assertTrue(task.y > 24); assertTrue(task.getCenterY() < size.height / 2.0);
                    assertTrue(task.getCenterY() > size.height * .35);
                    if (state == ShellState.NO_VAULT) {
                        assertEquals(task.width / 2.0, panel.primary.getBounds().getCenterX(), .5);
                        assertEquals(task.width / 2.0, panel.secondary.getBounds().getCenterX(), .5);
                        assertEquals(javax.swing.SwingConstants.CENTER, panel.identity.getHorizontalAlignment());
                        assertTrue(panel.primary.getWidth() <= 280); assertTrue(panel.secondary.getWidth() <= 280);
                        assertEquals(8, panel.secondary.getY() - panel.primary.getY() - panel.primary.getHeight());
                        assertEquals(24, panel.primary.getY() - panel.identity.getY() - panel.identity.getHeight());
                    } else {
                        assertSame(panel.secondaryActions, panel.secondary.getParent());
                        assertSame(panel.secondaryActions, panel.createNew.getParent());
                        assertEquals(8, panel.createNew.getX() - panel.secondary.getX() - panel.secondary.getWidth());
                        assertEquals(panel.secondary.getY(), panel.createNew.getY());
                        assertEquals(panel.secondary.getHeight(), panel.createNew.getHeight());
                        assertEquals(panel.secondary.getHeight(), panel.primary.getHeight());
                        assertEquals(panel.secondary.getWidth(), panel.createNew.getWidth());
                        assertEquals(panel.password.getWidth(), panel.primary.getWidth());
                        assertEquals(16, panel.secondaryActions.getY() - panel.primary.getY() - panel.primary.getHeight());
                        assertEquals(0, panel.secondary.getX());
                        assertEquals(panel.secondaryActions.getWidth(), panel.createNew.getX() + panel.createNew.getWidth(), 1);
                    }
                }
            }
        });
    }
    @Test void longPathAndHeadingShrinkWithinTaskWithoutHorizontalOverflow() throws Exception {
        edt(() -> {
            ShellPanel panel = new ShellPanel();
            panel.render(ShellState.LOCKED, Path.of("/" + "parent/".repeat(80) + "vault".repeat(60)), "", false);
            for (int width : new int[] {400, 640, 1200}) {
                panel.setSize(width, 820); layout(panel);
                for (java.awt.Component child : panel.task.getComponents()) {
                    assertTrue(child.getX() >= 0); assertTrue(child.getX() + child.getWidth() <= panel.task.getWidth());
                }
                assertEquals(panel.password.getX() + panel.password.getWidth(), panel.path.getX() + panel.path.getWidth());
                assertEquals(8, panel.password.getY() - panel.passwordLabel.getY() - panel.passwordLabel.getHeight());
            }
        });
    }
    @Test void reusableTaskActionRowKeepsSecondaryActionsBeforeTrailingPrimary() throws Exception {
        edt(() -> {
            var primary = new javax.swing.JButton("Open"); var quiet = new javax.swing.JButton("Change Vault…");
            var row = SwingUsability.taskActions(primary, quiet); row.setSize(560, row.getPreferredSize().height); row.doLayout();
            assertSame(quiet, row.getComponent(0)); assertSame(primary, row.getComponent(1));
            assertEquals(java.awt.FlowLayout.TRAILING, ((java.awt.FlowLayout) row.getLayout()).getAlignment());
            assertEquals(8, primary.getX() - quiet.getX() - quiet.getWidth());
            assertEquals(row.getWidth() - 8, primary.getX() + primary.getWidth());
        });
    }
    @Test void switchingBetweenWelcomeAndLockedKeepsActionsVisibleAndOperable() throws Exception {
        edt(() -> {
            ShellPanel panel = new ShellPanel(); var selects = new AtomicInteger(); var opens = new AtomicInteger();
            panel.actions(selects::incrementAndGet, () -> { }, password -> { opens.incrementAndGet(); java.util.Arrays.fill(password, '\0'); });
            for (int i = 0; i < 3; i++) {
                panel.render(ShellState.NO_VAULT, null, "", false); panel.setSize(640, 520); layout(panel);
                assertSame(panel.welcome.content, panel.primary.getParent()); panel.primary.doClick(0);
                panel.render(ShellState.LOCKED, Path.of("vault"), "", false); layout(panel);
                assertSame(panel.lockedActions, panel.primary.getParent()); assertSame(panel.secondaryActions, panel.secondary.getParent());
                panel.password.setText("p"); panel.primary.doClick(0);
            }
            assertEquals(3, selects.get()); assertEquals(3, opens.get());
        });
    }
    @Test void lockedActionsHaveExplicitNamesMnemonicsAndMatchingSecondaryStyles() throws Exception {
        edt(() -> {
            var panel = new ShellPanel(); panel.render(ShellState.LOCKED, Path.of("a"), "", false);
            assertEquals("New Vault…", panel.createNew.getText());
            assertEquals("New Vault…", panel.createNew.getAccessibleContext().getAccessibleName());
            assertEquals(java.awt.event.KeyEvent.VK_N, panel.createNew.getMnemonic());
            assertEquals(java.awt.event.KeyEvent.VK_C, panel.secondary.getMnemonic());
            assertEquals(java.awt.event.KeyEvent.VK_O, panel.primary.getMnemonic());
            assertEquals(DesktopStyle.ActionRole.PrimaryAction, panel.primary.getClientProperty("totipo.actionRole"));
            for (var button : new javax.swing.JButton[] {panel.secondary, panel.createNew}) {
                assertEquals(DesktopStyle.ActionRole.SecondaryAction, button.getClientProperty("totipo.actionRole"));
                assertTrue(button.isContentAreaFilled());
                assertEquals(DesktopStyle.surfaceRaised(), button.getBackground());
                assertInstanceOf(DesktopStyle.ControlBorder.class, button.getBorder());
                assertEquals(panel.secondary.getMargin(), button.getMargin());
                assertEquals(panel.secondary.getInsets(), button.getInsets());
                assertEquals(panel.secondary.getPreferredSize().height, button.getPreferredSize().height);
            }
        });
    }
}
