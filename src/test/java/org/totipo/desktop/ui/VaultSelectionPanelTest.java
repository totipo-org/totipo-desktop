package org.totipo.desktop.ui;

import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.edt;

class VaultSelectionPanelTest {
    @Test void duplicateBasenamesHaveFullAccessiblePathsAndTooltips() throws Exception {
        edt(() -> {
            Path a = Path.of("/one/same"), b = Path.of("/two/same");
            var panel = new VaultSelectionPanel(choice -> { }); panel.render(List.of(a, b), a, false, false);
            panel.vaults.setSize(400, 240);
            assertEquals(a, panel.vaults.getSelectedValue());
            for (int i = 0; i < 2; i++) {
                Path path = panel.vaults.getModel().getElementAt(i);
                var cell = cell(panel, path, i, false, false, 400);
                assertEquals("same", cell.name.getText());
                assertEquals(path.getParent().toString(), cell.parent.getText());
                assertEquals(path.toString(), cell.getToolTipText());
                assertEquals("same — " + path, cell.getAccessibleContext().getAccessibleName());
                assertEquals(path.toString(), cell.getAccessibleContext().getAccessibleDescription());
                assertEquals(path.toString(), cell.name.getToolTipText());
                assertEquals(path.toString(), cell.parent.getToolTipText());
                var accessible = panel.vaults.getAccessibleContext().getAccessibleChild(i).getAccessibleContext();
                assertEquals(path.toString(), accessible.getAccessibleDescription());
                var bounds = panel.vaults.getCellBounds(i, i);
                assertEquals(path.toString(), panel.vaults.getToolTipText(new java.awt.event.MouseEvent(panel.vaults,
                        java.awt.event.MouseEvent.MOUSE_MOVED, 0, 0, 4, bounds.y + 4, 0, false)));
            }
            assertEquals(ListSelectionModel.SINGLE_SELECTION, panel.vaults.getSelectionMode());
            assertEquals("Remembered vaults", panel.vaults.getAccessibleContext().getAccessibleName());
        });
    }
    @Test void keyboardAndActionsEmitOnlyExplicitChoices() throws Exception {
        edt(() -> {
            Path a = Path.of("/a"); var choices = new ArrayList<VaultSelection>();
            var panel = new VaultSelectionPanel(choices::add); panel.render(List.of(a), null, true, false);
            assertFalse(panel.select.isEnabled()); assertFalse(panel.forget.isEnabled());
            panel.vaults.setSelectedIndex(0);
            Object key = panel.vaults.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("ENTER"));
            panel.vaults.getActionMap().get(key).actionPerformed(null);
            assertEquals(VaultSelection.select(a), choices.getLast());
            panel.browse.doClick(); assertEquals(VaultSelection.Action.BROWSE, choices.getLast().action());
            panel.create.doClick(); assertEquals(VaultSelection.Action.CREATE, choices.getLast().action());
            panel.back.doClick(); assertEquals(VaultSelection.Action.RETURN, choices.getLast().action());
            for (JButton button : List.of(panel.select, panel.browse, panel.create, panel.forget, panel.back)) {
                assertNotEquals(0, button.getMnemonic()); assertTrue(button.isFocusable());
                assertEquals(button.getText(), button.getAccessibleContext().getAccessibleName());
            }
            assertTrue(panel.forget.getAccessibleContext().getAccessibleDescription().contains("No vault files"));
        });
    }
    @Test void doubleClickSelectsOnlyRealRowsAndBusyDisablesActions() throws Exception {
        edt(() -> {
            Path a = Path.of("/a"); var choices = new ArrayList<VaultSelection>();
            var panel = new VaultSelectionPanel(choices::add); panel.render(List.of(a), a, false, false);
            panel.vaults.setSize(400, 240);
            var bounds = panel.vaults.getCellBounds(0, 0);
            var click = new java.awt.event.MouseEvent(panel.vaults, java.awt.event.MouseEvent.MOUSE_CLICKED,
                    0, 0, bounds.x + 4, bounds.y + 4, 2, false, java.awt.event.MouseEvent.BUTTON1);
            for (var listener : panel.vaults.getMouseListeners()) { listener.mouseClicked(click); }
            assertEquals(List.of(VaultSelection.select(a)), choices); assertFalse(panel.back.isVisible());
            var blank = new java.awt.event.MouseEvent(panel.vaults, java.awt.event.MouseEvent.MOUSE_CLICKED,
                    0, 0, 4, 220, 2, false, java.awt.event.MouseEvent.BUTTON1);
            for (var listener : panel.vaults.getMouseListeners()) { listener.mouseClicked(blank); }
            assertEquals(1, choices.size());
            panel.render(List.of(a), a, true, true);
            for (JButton button : List.of(panel.select, panel.browse, panel.create, panel.forget, panel.back)) {
                assertFalse(button.isEnabled()); button.doClick(0);
            }
            assertEquals(1, choices.size());
        });
    }
    @Test void rendersKeepLocalHighlightAndUseShellDefaultButtonWithoutAnyDialog() throws Exception {
        edt(() -> {
            Path a = Path.of("/a"), b = Path.of("/b");
            var panel = new VaultSelectionPanel(choice -> { }); var root = new JRootPane(); root.setContentPane(panel);
            panel.render(List.of(a, b), a, false, false); panel.vaults.setSelectedValue(b, true);
            panel.render(List.of(a, b), a, false, true); panel.render(List.of(a, b), a, false, false);
            assertEquals(b, panel.vaults.getSelectedValue()); panel.defaultButton(root); assertSame(panel.select, root.getDefaultButton());
            panel.navigationEntered(); assertEquals(a, panel.vaults.getSelectedValue());
            panel.render(List.of(a), null, false, false); assertNull(panel.vaults.getSelectedValue());
            assertFalse(panel.select.isEnabled()); assertFalse(panel.forget.isEnabled());
        });
    }
    @Test void shellLayoutKeepsNavigationAndManagementActionsVisibleAtMinimumSize() throws Exception {
        edt(() -> {
            var panel = new VaultSelectionPanel(choice -> { });
            panel.render(List.of(Path.of("/a")), Path.of("/a"), true, false);
            for (float size : new float[] {12f, 20f}) {
                for (JButton button : List.of(panel.select, panel.browse, panel.create, panel.forget, panel.back)) {
                    button.setFont(button.getFont().deriveFont(size));
                }
                panel.setSize(VaultPanel.MINIMUM_SIZE); layout(panel);
                for (JButton button : List.of(panel.select, panel.browse, panel.create, panel.forget, panel.back)) {
                    var bounds = SwingUtilities.convertRectangle(button.getParent(), button.getBounds(), panel);
                    assertTrue(panel.getBounds().contains(bounds), button.getText());
                    assertTrue(button.getWidth() >= button.getPreferredSize().width, button.getText());
                }
                assertTrue(panel.vaults.getParent().getHeight() > 0);
            }
        });
    }
    @Test void middleElisionKeepsUsefulPathEndsAndRestoresFullTextAfterResize() throws Exception {
        edt(() -> {
            Path a = Path.of("/home/niki/" + "some-long-component/".repeat(12) + "Syncthing/totipo-vault");
            var panel = new VaultSelectionPanel(choice -> { }); panel.render(List.of(a), a, false, false);
            var cell = cell(panel, a, 0, false, false, 260);
            assertEquals("totipo-vault", cell.name.getText());
            String truncated = cell.parent.getText();
            assertTrue(truncated.contains("…")); assertTrue(truncated.startsWith("/home/")); assertTrue(truncated.endsWith("/Syncthing"));
            assertFits(cell);
            cell.setSize(80, cell.getHeight()); cell.doLayout(); assertFits(cell);
            assertNotEquals(truncated, cell.parent.getText()); assertTrue(cell.name.getText().contains("…"));
            assertEquals(a.toString(), cell.getAccessibleContext().getAccessibleDescription());
            cell.setSize(4000, cell.getHeight()); cell.doLayout();
            assertEquals(a.getParent().toString(), cell.parent.getText()); assertEquals("totipo-vault", cell.name.getText());
            assertEquals(a, panel.vaults.getModel().getElementAt(0));
        });
    }
    @Test void actualListPaintingLaysOutBothLinesAtTheCurrentCellWidth() throws Exception {
        edt(() -> {
            Path path = Path.of("/home/" + "long-directory/".repeat(12) + "Sources/redbook");
            var panel = new VaultSelectionPanel(choice -> { }); panel.render(List.of(path), path, false, false);
            panel.vaults.setSize(240, 180); paint(panel.vaults);
            var renderer = (VaultSelectionPanel.VaultCellRenderer) panel.vaults.getCellRenderer();
            assertEquals("redbook", renderer.name.getText()); assertTrue(renderer.parent.getText().contains("…"));
            String small = renderer.parent.getText(); assertFits(renderer);
            panel.vaults.setSize(1000, 180); paint(panel.vaults);
            assertTrue(renderer.parent.getText().length() > small.length()); assertFits(renderer);
        });
    }
    @Test void viewportResizeNeverScrollsHorizontallyAndRetainsVerticalNavigation() throws Exception {
        edt(() -> {
            var paths = java.util.stream.IntStream.range(0, 30)
                    .mapToObj(i -> Path.of("/home/" + "long-parent/".repeat(25) + "vault-" + i)).toList();
            var choices = new ArrayList<VaultSelection>();
            var panel = new VaultSelectionPanel(choices::add); panel.render(paths, paths.getFirst(), false, false);
            var scroll = (JScrollPane) panel.vaults.getParent().getParent();
            String small = null;
            for (int width : new int[] {180, 760, 320}) {
                scroll.setSize(width, 180); layout(scroll); layout(scroll);
                assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, scroll.getHorizontalScrollBarPolicy());
                assertFalse(scroll.getHorizontalScrollBar().isVisible()); assertTrue(scroll.getVerticalScrollBar().isVisible());
                assertEquals(scroll.getViewport().getExtentSize().width, panel.vaults.getWidth());
                var cell = cell(panel, paths.getFirst(), 0, false, false, panel.vaults.getCellBounds(0, 0).width);
                assertFits(cell);
                if (width == 180) { small = cell.parent.getText(); }
                if (width == 760) { assertTrue(cell.parent.getText().length() > small.length()); }
            }
            Object down = panel.vaults.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("DOWN"));
            assertNotNull(down); panel.vaults.getActionMap().get(down).actionPerformed(
                    new java.awt.event.ActionEvent(panel.vaults, java.awt.event.ActionEvent.ACTION_PERFORMED, "down"));
            assertEquals(paths.get(1), panel.vaults.getSelectedValue());
            Object enter = panel.vaults.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("ENTER"));
            panel.vaults.getActionMap().get(enter).actionPerformed(null);
            assertEquals(VaultSelection.select(paths.get(1)), choices.getLast());
            panel.render(List.of(), null, false, false); layout(scroll); layout(scroll);
            assertEquals(0, panel.vaults.getModel().getSize()); assertFalse(scroll.getHorizontalScrollBar().isVisible());
            assertFalse(panel.select.isEnabled());
        });
    }
    @Test void rowsUseNativeSelectionFocusAndCompactTwoLineTypography() throws Exception {
        edt(() -> {
            Path path = Path.of("/home/niki/redbook"); var panel = new VaultSelectionPanel(choice -> { });
            panel.render(List.of(path), path, false, false);
            for (var background : List.of(java.awt.Color.WHITE, new java.awt.Color(30, 34, 40))) {
                panel.vaults.setBackground(background);
                panel.vaults.setForeground(background.equals(java.awt.Color.WHITE) ? java.awt.Color.BLACK : java.awt.Color.WHITE);
                panel.vaults.setSelectionBackground(new java.awt.Color(45, 90, 150));
                panel.vaults.setSelectionForeground(java.awt.Color.WHITE);
                for (float size : new float[] {12f, 24f, 32f}) {
                    panel.vaults.setFont(panel.vaults.getFont().deriveFont(size));
                    var cell = cell(panel, path, 0, false, false, 320);
                    assertTrue(cell.name.getFont().isBold()); assertTrue(cell.parent.getFont().getSize2D() < cell.name.getFont().getSize2D());
                    assertEquals(background, cell.getBackground()); assertNotEquals(cell.name.getForeground(), cell.parent.getForeground());
                    assertTrue(cell.getInsets().top >= 8 && cell.getInsets().top <= 10);
                    assertEquals(cell.name.getY() + cell.name.getHeight() + DesktopStyle.MICRO, cell.parent.getY()); assertFits(cell);
                    cell = cell(panel, path, 0, true, true, 320);
                    var nativeCell = (JLabel) new DefaultListCellRenderer().getListCellRendererComponent(panel.vaults, path, 0, true, true);
                    assertEquals(nativeCell.getBackground(), cell.getBackground());
                    assertEquals(nativeCell.getForeground(), cell.name.getForeground()); assertEquals(nativeCell.getForeground(), cell.parent.getForeground());
                    assertSame(nativeCell.getBorder(), ((javax.swing.border.CompoundBorder) cell.getBorder()).getOutsideBorder());
                }
            }
        });
    }
    @Test void rootsRelativePathsUnicodeAndLongBasenamesRetainCompleteIdentity() throws Exception {
        edt(() -> {
            var panel = new VaultSelectionPanel(choice -> { });
            for (Path path : List.of(Path.of("/"), Path.of("vault"), Path.of("/" + "😀long-name".repeat(50)),
                    Path.of("/home/first/" + "😀nested/".repeat(30) + "last/vault"))) {
                panel.render(List.of(path), path, false, false);
                for (int width : new int[] {0, 1, 30, 120, 4000}) {
                    var cell = cell(panel, path, 0, false, false, width); assertFits(cell);
                    for (JLabel label : List.of(cell.name, cell.parent)) {
                        assertTrue(label.getText().codePoints().noneMatch(cp -> cp >= 0xD800 && cp <= 0xDFFF));
                        assertEquals(path.toAbsolutePath().toString(), label.getAccessibleContext().getAccessibleDescription());
                    }
                    assertEquals(path.toAbsolutePath().toString(), cell.getToolTipText());
                    assertEquals(path, panel.vaults.getModel().getElementAt(0));
                }
            }
            Path a = Path.of("/home/" + "nested/".repeat(20) + "Syncthing/same"),
                    b = Path.of("/home/" + "nested/".repeat(20) + "Sources/same");
            String first = cell(panel, a, 0, false, false, 240).parent.getText();
            assertNotEquals(first, cell(panel, b, 1, false, false, 240).parent.getText());
        });
    }
    private static VaultSelectionPanel.VaultCellRenderer cell(VaultSelectionPanel panel, Path path,
            int index, boolean selected, boolean focused, int width) {
        var cell = (VaultSelectionPanel.VaultCellRenderer) panel.vaults.getCellRenderer()
                .getListCellRendererComponent(panel.vaults, path, index, selected, focused);
        cell.setSize(width, cell.getPreferredSize().height); cell.doLayout(); return cell;
    }
    private static void assertFits(VaultSelectionPanel.VaultCellRenderer cell) {
        for (JLabel label : List.of(cell.name, cell.parent)) {
            assertTrue(label.getFontMetrics(label.getFont()).stringWidth(label.getText()) <= label.getWidth(), label.getText());
            // CellRendererPane resets its stamp's bounds to zero after painting.
            assertTrue(label.getY() + label.getHeight() <= cell.getPreferredSize().height);
        }
    }
    private static void paint(JList<?> list) {
        var picture = new java.awt.image.BufferedImage(list.getWidth(), list.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics = picture.createGraphics();
        try { list.paint(graphics); } finally { graphics.dispose(); }
    }
    private static void layout(java.awt.Container container) {
        container.doLayout();
        for (var child : container.getComponents()) {
            if (child instanceof java.awt.Container nested) { layout(nested); }
        }
    }
}
