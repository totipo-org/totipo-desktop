package org.totipo.desktop.ui;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import org.junit.jupiter.api.Test;
import org.totipo.*;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class DesktopStyleTest {
    @Test void styledActionsAndSearchShareRoundedSurfacesWithoutChangingInsetsOrDimensions() throws Exception {
        edt(() -> {
            var primary = new JButton("Add"); var secondary = new JButton("Copy"); var quiet = new JButton("Edit");
            DesktopStyle.action(primary, DesktopStyle.ActionRole.PrimaryAction, false);
            DesktopStyle.action(secondary, DesktopStyle.ActionRole.SecondaryAction, true);
            DesktopStyle.action(quiet, DesktopStyle.ActionRole.QuietAction, true);
            var search = new JTextField(24); DesktopStyle.input(search);
            assertTrue(primary.isContentAreaFilled()); assertTrue(secondary.isContentAreaFilled());
            assertFalse(quiet.isContentAreaFilled());
            for (JComponent control : List.of(primary, secondary, search)) {
                assertInstanceOf(DesktopStyle.ControlBorder.class, control.getBorder());
                assertFalse(control.isOpaque(), "Rounded corners must expose their parent surface");
                var size = control.getPreferredSize(); var insets = control.getInsets();
                control.setSize(size);
                for (int scale : new int[]{1, 2}) {
                    var canvas = new java.awt.image.BufferedImage(size.width * scale, size.height * scale,
                            java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    var graphics = canvas.createGraphics(); graphics.scale(scale, scale);
                    try { control.paint(graphics); } finally { graphics.dispose(); }
                    assertEquals(0, canvas.getRGB(0, 0) >>> 24, "No rectangular fill outside the rounded corner");
                    assertTrue((canvas.getRGB(2 * scale, 2 * scale) >>> 24) > 0,
                            "Rounding is modest: the surface remains present near the corner");
                    assertTrue((canvas.getRGB(canvas.getWidth() / 2, canvas.getHeight() / 2) >>> 24) > 0,
                            "Styled controls retain a filled surface at either platform scale");
                }
                assertEquals(size, control.getPreferredSize()); assertEquals(insets, control.getInsets());
            }
            assertEquals(new Insets(6, 12, 6, 12), primary.getInsets());
            assertEquals(primary.getInsets(), secondary.getInsets());
            assertEquals(new Insets(6, 8, 6, 8), search.getInsets());
            assertTrue(primary.getPreferredSize().height >= 36); assertTrue(secondary.getPreferredSize().height >= 32);
        });
    }

    private static final class FocusButton extends JButton {
        private static final long serialVersionUID = 1L;
        boolean focused;
        FocusButton(String label) { super(label); }
        @Override public boolean hasFocus() { return focused; }
    }
    private static final class FocusField extends JTextField {
        private static final long serialVersionUID = 1L;
        boolean focused;
        int fullRepaints;
        FocusField() { super("Search text", 24); }
        @Override public boolean hasFocus() { return focused; }
        @Override public void repaint() { fullRepaints++; super.repaint(); }
    }
    @Test void searchFocusPaintsTwoPixelsOnEveryEdgeWithoutChangingSize() throws Exception {
        edt(() -> {
            var field = new FocusField(); DesktopStyle.input(field);
            var size = field.getPreferredSize(); var insets = field.getInsets();
            var idle = paint(field); field.focused = true; field.fullRepaints = 0;
            for (var listener : field.getFocusListeners()) {
                listener.focusGained(new java.awt.event.FocusEvent(field, java.awt.event.FocusEvent.FOCUS_GAINED));
            }
            assertTrue(field.fullRepaints > 0, "Focus must repaint the entire field, including its border");
            Color expected = DesktopStyle.readable(DesktopStyle.focus(), field.getBackground(), 3);
            for (int scale : new int[] {1, 2}) {
                field.setSize(size);
                var image = new java.awt.image.BufferedImage(size.width * scale, size.height * scale,
                        java.awt.image.BufferedImage.TYPE_INT_ARGB);
                var graphics = image.createGraphics(); graphics.scale(scale, scale);
                try { field.paint(graphics); } finally { graphics.dispose(); }
                for (int offset = 0; offset < DesktopStyle.FOCUS * scale; offset++) {
                    assertEquals(expected.getRGB(), image.getRGB(image.getWidth() / 2, offset), "Top edge");
                    assertEquals(expected.getRGB(), image.getRGB(image.getWidth() / 2, image.getHeight() - 1 - offset), "Bottom edge");
                    assertEquals(expected.getRGB(), image.getRGB(offset, image.getHeight() / 2), "Left edge");
                    assertEquals(expected.getRGB(), image.getRGB(image.getWidth() - 1 - offset, image.getHeight() / 2), "Right edge");
                }
            }
            assertNotEquals(idle.getRGB(size.width / 2, 1), expected.getRGB());
            field.focused = false; field.fullRepaints = 0;
            for (var listener : field.getFocusListeners()) {
                listener.focusLost(new java.awt.event.FocusEvent(field, java.awt.event.FocusEvent.FOCUS_LOST));
            }
            assertTrue(field.fullRepaints > 0, "Blur must also repaint the entire border");
            assertEquals(size, field.getPreferredSize()); assertEquals(size, field.getSize()); assertEquals(insets, field.getInsets());
        });
    }
    @Test void sharedMenuSpacingRetainsNativeDelegatesMnemonicsAcceleratorsAndAlignment() throws Exception {
        edt(() -> {
            var vault = new VaultPanel();
            try {
                var bar = vault.menuBar();
                for (int i = 0; i < bar.getMenuCount(); i++) {
                    var menu = bar.getMenu(i); var delegate = menu.getUI(); var mnemonic = menu.getMnemonic();
                    assertEquals(new Insets(4, 10, 4, 10), menu.getInsets());
                    assertNotEquals(0, mnemonic);
                    var item = menu.getItem(0); var itemDelegate = item.getUI();
                    var accelerator = item.getAccelerator();
                    DesktopStyle.menus(bar);
                    assertSame(delegate, menu.getUI()); assertEquals(mnemonic, menu.getMnemonic());
                    assertSame(itemDelegate, item.getUI()); assertEquals(accelerator, item.getAccelerator());
                    for (int j = 0; j < menu.getItemCount(); j++) {
                        assertEquals(new Insets(6, 16, 6, 16), menu.getItem(j).getInsets());
                        assertNotNull(menu.getItem(j).getAccessibleContext());
                    }
                }
                assertEquals(KeyStroke.getKeyStroke("control R"), bar.getMenu(1).getItem(2).getAccelerator());
                var menu = bar.getMenu(0); menu.addSeparator(); DesktopStyle.menus(bar);
                assertEquals(10, menu.getMenuComponent(1).getPreferredSize().height);
            } finally { vault.closing(); }
        });
    }
    private static java.awt.image.BufferedImage paint(JComponent control) {
        var size = control.getPreferredSize(); control.setSize(size);
        var image = new java.awt.image.BufferedImage(size.width, size.height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try { control.paint(graphics); } finally { graphics.dispose(); }
        return image;
    }
    @Test void quietButtonsHavePaddedHitAreasTransientNeutralSurfacesAndExplicitFocus() throws Exception {
        edt(() -> {
            for (String label : List.of("Edit", "Refresh")) {
                var button = new FocusButton(label); DesktopStyle.action(button, DesktopStyle.ActionRole.QuietAction, label.equals("Edit"));
                var size = button.getPreferredSize(); var insets = button.getInsets();
                assertTrue(button.isRolloverEnabled()); assertFalse(button.isContentAreaFilled()); assertFalse(button.isOpaque());
                assertTrue(size.width >= button.getFontMetrics(button.getFont()).stringWidth(label) + 24);
                assertTrue(size.height >= (label.equals("Edit") ? 32 : 36));
                var idle = paint(button); int x = idle.getWidth() / 2;
                assertEquals(0, idle.getRGB(x, 2) >>> 24, "No persistent fill or top border");
                button.getModel().setRollover(true); var hover = paint(button);
                assertTrue((hover.getRGB(x, 2) >>> 24) > 0);
                button.getModel().setArmed(true); button.getModel().setPressed(true); var pressed = paint(button);
                assertNotEquals(hover.getRGB(x, 2), pressed.getRGB(x, 2), "Press has a distinct subtle surface");
                assertTrue(DesktopStyle.contrast(button.getForeground(), DesktopStyle.quietSurface(button)) >= 4.5);
                button.getModel().setPressed(false); button.getModel().setArmed(false); button.getModel().setRollover(false);
                button.focused = true; var focus = paint(button);
                assertTrue((focus.getRGB(x, 0) >>> 24) > 0, "Keyboard focus has a visible boundary");
                button.focused = false; assertEquals(0, paint(button).getRGB(x, 2) >>> 24);
                button.setEnabled(false); button.getModel().setRollover(true);
                assertEquals(0, paint(button).getRGB(x, 2) >>> 24, "Unavailable actions do not show active hover feedback");
                assertEquals(size, button.getPreferredSize()); assertEquals(insets, button.getInsets());
            }
        });
    }

    @Test void brightPlatformCyanIsMutedInDarkModeWhileSemanticColorsAndContrastRemainIndependent() throws Exception {
        edt(() -> {
            String[] keys = {"List.background", "List.foreground", "List.selectionBackground", "List.selectionForeground", "Panel.background"};
            Object[] previous = java.util.Arrays.stream(keys).map(UIManager::get).toArray();
            try {
                for (Color surface : List.of(new Color(248, 248, 248), new Color(28, 28, 28))) {
                    UIManager.put("List.background", surface); UIManager.put("Panel.background", surface.getRed() > 128 ? new Color(238, 238, 238) : surface);
                    UIManager.put("List.foreground", surface.getRed() > 128 ? Color.BLACK : Color.WHITE);
                    Color cyan = new Color(0, 240, 255); UIManager.put("List.selectionBackground", cyan);
                    Color warning = DesktopStyle.warning(), danger = DesktopStyle.danger();
                    Color accent = DesktopStyle.accent();
                    float[] nativeHsb = Color.RGBtoHSB(cyan.getRed(), cyan.getGreen(), cyan.getBlue(), null);
                    float[] accentHsb = Color.RGBtoHSB(accent.getRed(), accent.getGreen(), accent.getBlue(), null);
                    assertTrue(accentHsb[1] < nativeHsb[1], "Interaction color is less saturated than native cyan");
                    assertTrue(accentHsb[2] < nativeHsb[2], "Interaction color avoids the bright native cyan treatment");
                    assertTrue(DesktopStyle.contrast(accent, surface) >= 3);
                    assertTrue(DesktopStyle.contrast(DesktopStyle.focus(), DesktopStyle.surfaceSelected()) >= 3);
                    assertTrue(DesktopStyle.contrast(DesktopStyle.focus(), DesktopStyle.surfaceRaised()) >= 3);
                    assertTrue(DesktopStyle.contrast(DesktopStyle.onAccent(), accent) >= 4.5);
                    UIManager.put("List.selectionBackground", new Color(80, 120, 190));
                    assertEquals(warning, DesktopStyle.warning()); assertEquals(danger, DesktopStyle.danger());
                }
            } finally { for (int i = 0; i < keys.length; i++) { UIManager.put(keys[i], previous[i]); } }
        });
    }

    @Test void addIsTheOnlyPrimaryActionAndRowCommandsAreNeutralOrQuiet() throws Exception {
        edt(() -> {
            var panel = new VaultPanel();
            try {
                panel.render(new State(token(1, active("GitHub")), token(2, active("Alpha"), active("Beta"))).value);
                var buttons = allButtons(panel);
                var primary = buttons.stream().filter(b -> b.getClientProperty("totipo.actionRole") == DesktopStyle.ActionRole.PrimaryAction).toList();
                assertEquals(1, primary.size()); assertEquals("Add", primary.get(0).getText());
                var browser = TokenBrowserTest.find(panel, TokenBrowserPanel.class);
                browser.totpAction(TokenFixtures::generate); browser.rows.forEach(r -> r.show.doClick(0));
                for (var row : browser.rows) {
                    assertEquals(DesktopStyle.ActionRole.SecondaryAction, row.show.getClientProperty("totipo.actionRole"));
                    assertEquals(DesktopStyle.ActionRole.SecondaryAction, TotpCopyTest.buttons(row).get(0).getClientProperty("totipo.actionRole"));
                    assertEquals(DesktopStyle.ActionRole.QuietAction, row.edit.getClientProperty("totipo.actionRole"));
                    assertFalse(row.edit.isContentAreaFilled());
                }
                for (var button : allButtons(browser.list)) {
                    assertNotEquals(DesktopStyle.ActionRole.PrimaryAction, button.getClientProperty("totipo.actionRole"));
                }
            } finally { panel.closing(); }
        });
    }
    static java.util.List<JButton> allButtons(Container root) {
        var result = new java.util.ArrayList<JButton>();
        for (var component : root.getComponents()) {
            if (component instanceof JButton button) { result.add(button); }
            if (component instanceof Container container) { result.addAll(allButtons(container)); }
        }
        return result;
    }
    static boolean outline(Border border, int width, Color color) {
        if (border instanceof LineBorder line) { return line.getThickness() == width && line.getLineColor().equals(color); }
        return border instanceof CompoundBorder c && (outline(c.getInsideBorder(), width, color) || outline(c.getOutsideBorder(), width, color));
    }
    @Test void dividerRowsKeepGeometryAcrossSelectionFocusRevealAndCopyAndRetainConflictMeaning() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var panel = browser(clock);
            panel.copyAction((c, f, u, n) -> org.totipo.desktop.clipboard.TotpClipboard.COPIED);
            try {
                panel.render(new State(token(1, active("A"), active("B"))).value);
                var row = panel.rows.get(0); var group = row.getParent().getParent();
                var groupBorder = ((JPanel) group).getBorder(); var size = row.getPreferredSize(); var insets = row.getInsets();
                assertEquals("divider", row.getClientProperty("totipo.rowPresentation"));
                var divider = (MatteBorder) ((CompoundBorder) row.getBorder()).getOutsideBorder();
                assertEquals(0, divider.getBorderInsets(row).left); assertEquals(1, divider.getBorderInsets(row).bottom);
                assertTrue(size.height >= 64 && size.height <= 80, "font-adapted two-line height: " + size.height);
                row.selected(true); assertTrue(outline(row.getBorder(), 1, DesktopStyle.accent()));
                row.focused(true); assertTrue(outline(row.getBorder(), 2, DesktopStyle.focus()));
                assertTrue(row.show.isVisible()); row.show.doClick(0);
                var revealedSize = row.getPreferredSize(); assertEquals(size, revealedSize);
                TotpCopyTest.buttons(row).get(0).doClick(0); row.selected(false); row.focused(false);
                assertEquals("Copied", TotpCopyTest.buttons(row).get(0).getText());
                assertSame(groupBorder, ((JPanel) group).getBorder());
                assertEquals(insets, row.getInsets()); assertEquals(size, row.getPreferredSize());
                assertTrue(row.getAccessibleContext().getAccessibleName().contains("conflicting versions"));
                assertTrue(TotpCopyTest.codeLabel(row).getFont().getSize2D() > row.primary.getFont().getSize2D());
                assertEquals(Font.PLAIN, row.account.getFont().getStyle());
            } finally { panel.closing(); }
        });
    }
    @Test void lightAndDarkPalettesMeetReadableTextFilledActionAndFocusContrast() throws Exception {
        edt(() -> {
            String[] keys = {"List.background", "List.foreground", "List.selectionBackground", "List.selectionForeground", "TextField.background", "Panel.background"};
            Object[] previous = java.util.Arrays.stream(keys).map(UIManager::get).toArray();
            try {
                for (Color surface : List.of(new Color(248, 248, 248), new Color(28, 28, 28))) {
                    UIManager.put("List.background", surface); UIManager.put("Panel.background", surface.getRed() > 128 ? new Color(238, 238, 238) : surface);
                    UIManager.put("TextField.background", surface);
                    UIManager.put("List.foreground", surface.getRed() > 128 ? new Color(75, 75, 75) : new Color(195, 195, 195));
                    UIManager.put("List.selectionBackground", new Color(80, 120, 190));
                    UIManager.put("List.selectionForeground", new Color(185, 185, 185));
                    assertTrue(DesktopStyle.contrast(DesktopStyle.text(), surface) >= 4.5);
                    assertTrue(DesktopStyle.contrast(DesktopStyle.textSecondary(), surface) >= 4.5);
                    assertTrue(DesktopStyle.contrast(DesktopStyle.textDisabled(), surface) >= 3);
                    assertTrue(DesktopStyle.contrast(DesktopStyle.textDisabled(), DesktopStyle.surfaceRaised()) >= 3);
                    assertTrue(DesktopStyle.contrast(DesktopStyle.warning(), surface) >= 4.5);
                    assertTrue(DesktopStyle.contrast(DesktopStyle.focus(), DesktopStyle.surfaceSelected()) >= 3);
                    assertTrue(DesktopStyle.contrast(DesktopStyle.focus(), DesktopStyle.surfaceRaised()) >= 3);
                    assertTrue(DesktopStyle.contrast(DesktopStyle.onAccent(), DesktopStyle.accent()) >= 4.5);
                    assertNotEquals(DesktopStyle.warning(), DesktopStyle.danger());
                    var row = new TokenRowPanel(token(1, active("A")), () -> {}, () -> {}, () -> {}, i -> {});
                    row.selected(true);
                    assertTrue(DesktopStyle.contrast(row.account.getForeground(), row.getBackground()) >= 4.5);
                    assertTrue(DesktopStyle.contrast(row.primary.getForeground(), row.getBackground()) >= 4.5);
                    row.retire();
                }
            } finally { for (int i = 0; i < keys.length; i++) { UIManager.put(keys[i], previous[i]); } }
        });
    }
    @Test void actualRemainingDurationSwitchesCountdownFromAccentToWarningStrictlyBelowTenSeconds() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(20);
            var panel = browser(clock);
            try {
                panel.render(new State(token(1, active("A"))).value); var row = panel.row(id(1)); row.show.doClick(0);
                var ring = TokenBrowserTest.find(row, CountdownRing.class);
                assertFalse(ring.urgent()); assertEquals(DesktopStyle.accent(), ring.indicatorColor());
                assertEquals("10 seconds remaining", ring.getAccessibleContext().getAccessibleName());
                clock.now = clock.now.plusNanos(1); panel.totp.tick();
                assertTrue(ring.urgent()); assertEquals(DesktopStyle.warning(), ring.indicatorColor());
                assertNotEquals(DesktopStyle.danger(), ring.indicatorColor());
                assertEquals("10 sec", TokenBrowserTest.find(row.statusBottom, JLabel.class).getText());
            } finally { panel.closing(); }
        });
    }
    @Test void identityOrderingAppliesInsideConflictAndOutsideWithoutHeadVotesOrDerivation() throws Exception {
        edt(() -> {
            var a = alternative(TokenStatus.ACTIVE, "alpha", "z", TotpAlgorithm.SHA1, 6, 30, head(1, ClientMetadata.empty()), head(2, ClientMetadata.empty()));
            var b = alternative(TokenStatus.ACTIVE, "Alpha", "a", TotpAlgorithm.SHA1, 6, 30, head(3, ClientMetadata.empty()));
            var state = new State(token(9, active("Zulu")), token(7, a, b), token(1, active("Beta")));
            var panel = browser(new MutableClock());
            try {
                panel.render(state.value);
                assertEquals(List.of(b, a, state.value.tokens().get(2).alternatives().get(0), state.value.tokens().get(0).alternatives().get(0)),
                        panel.rows.stream().map(r -> r.alternative).toList());
                assertEquals(3, panel.list.getComponentCount()); assertEquals(4, panel.rows.size()); assertNull(panel.selectedId());
                assertTrue(state.calls.isEmpty()); panel.search.setText("alpha a");
                assertEquals(2, panel.rows.size()); assertEquals("1 of 3", panel.resultCount.getText()); assertTrue(state.calls.isEmpty());
            } finally { panel.closing(); }
        });
    }
}
