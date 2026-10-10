package org.totipo.desktop.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.totipo.*;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class ResponsiveTokenRowTest {
    static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) { if (child instanceof Container container) { layout(container); } }
    }
    static void width(TokenRowPanel row, int width) { row.setSize(width, row.getPreferredSize().height); layout(row); }
    @Test void concealRevealResizeAndExpiryReclaimWidthWithoutRebuildingRowOrChangingHeight() throws Exception {
        edt(() -> {
            for (int digits : new int[]{6, 7, 8}) {
                var token = token(1, alternative(TokenStatus.ACTIVE, "Long issuer 😀 ".repeat(20),
                        "long-account-界".repeat(20), TotpAlgorithm.SHA1, digits, 30));
                var row = new TokenRowPanel(token, () -> { }, () -> { }, () -> { }, i -> { });
                try {
                    width(row, 700); int concealed = row.identityTop.getWidth(), height = row.getHeight();
                    assertFalse(row.statusTop.isVisible()); assertFalse(row.statusBottom.isVisible());
                    assertEquals("Show Code", row.show.getText());
                    var primary = (ElidingLabel) row.primary; var account = (ElidingLabel) row.account;
                    assertTrue(primary.displayText().endsWith("…")); assertTrue(account.displayText().endsWith("…"));
                    assertEquals(primary.getText(), primary.getAccessibleContext().getAccessibleName());
                    assertEquals(account.getText(), account.getToolTipText());
                    row.display(List.of(new TotpDisplay.Display("identity", "1".repeat(digits), 500, 15, false)));
                    width(row, 700);
                    assertTrue(concealed > row.identityTop.getWidth()); assertEquals(height, row.getHeight());
                    JLabel code = TokenBrowserTest.find(row.statusTop, JLabel.class); JButton copy = TotpCopyTest.buttons(row).get(0);
                    assertTrue(code.getWidth() >= code.getPreferredSize().width);
                    assertTrue(code.getHeight() >= code.getFontMetrics(code.getFont()).getHeight());
                    assertTrue(row.statusBottom.getWidth() >= row.statusBottom.getPreferredSize().width);
                    assertEquals(copy.getPreferredSize().width, copy.getWidth());
                    assertTrue(copy.getWidth() < row.show.getPreferredSize().width);
                    int revealed = row.identityTop.getWidth();
                    width(row, 1000); assertTrue(row.identityTop.getWidth() > revealed);
                    width(row, 400); assertTrue(row.identityTop.getWidth() < revealed);
                    Rectangle identity = SwingUtilities.convertRectangle(primary.getParent(), primary.getBounds(), row);
                    Rectangle controls = SwingUtilities.convertRectangle(row.statusTop.getParent(), row.statusTop.getBounds(), row);
                    assertTrue(identity.x + identity.width <= controls.x);
                    row.display(List.of()); width(row, 700);
                    assertEquals(concealed, row.identityTop.getWidth()); assertEquals(height, row.getHeight());
                    assertFalse(row.statusTop.isVisible()); assertFalse(row.statusBottom.isVisible());
                    assertTrue(row.show.isVisible());
                } finally { row.retire(); }
            }
        });
    }
    @Test void pixelElisionPreservesFullLiteralUnicodeAndRespondsToFontAndWidth() throws Exception {
        edt(() -> {
            String full = "<html>😀界é👨‍👩‍👧‍👦".repeat(10);
            var label = new ElidingLabel(full); label.setSize(100, 40);
            String narrow = label.displayText(); assertTrue(narrow.endsWith("…"));
            assertTrue(label.getFontMetrics(label.getFont()).stringWidth(narrow) <= label.getWidth());
            for (int i = 0; i < narrow.length(); i++) {
                if (Character.isHighSurrogate(narrow.charAt(i))) { assertTrue(Character.isLowSurrogate(narrow.charAt(++i))); }
                else { assertFalse(Character.isLowSurrogate(narrow.charAt(i))); }
            }
            var graphics = new BufferedImage(200, 80, BufferedImage.TYPE_INT_ARGB).createGraphics();
            try { label.paint(graphics); } finally { graphics.dispose(); }
            assertEquals(full, label.getText()); assertEquals(full, label.getAccessibleContext().getAccessibleName());
            assertEquals(full, label.getToolTipText()); assertEquals(Boolean.TRUE, label.createToolTip().getClientProperty("html.disable"));
            label.setSize(200, 40); assertTrue(label.displayText().length() > narrow.length());
            String beforeFont = label.displayText(); label.setFont(label.getFont().deriveFont(30f));
            assertTrue(label.displayText().length() < beforeFont.length());
            label.setSize(10000, 40); assertEquals(full, label.displayText()); assertNull(label.getToolTipText());
            label.setText(""); label.setSize(0, 40); assertEquals("", label.displayText());
        });
    }
    @Test void emptyMetadataAndConflictAlternativeRowsUseSameResponsiveIdentity() throws Exception {
        edt(() -> {
            var token = token(1, alternative(TokenStatus.ACTIVE, "", "", TotpAlgorithm.SHA1, 6, 30), active("Other"));
            for (var alternative : token.alternatives()) {
                var row = new TokenRowPanel(token, alternative, () -> { }, () -> { }, () -> { }, i -> { });
                try {
                    width(row, 700); int hidden = row.identityTop.getWidth();
                    row.display(List.of(new TotpDisplay.Display("Alternative", "123456", 400, 10, false)));
                    width(row, 700); assertTrue(hidden > row.identityTop.getWidth()); assertTrue(row.edit.isEnabled());
                } finally { row.retire(); }
            }
        });
    }
    @Test void lockedCreateIsExplicitEnabledAndSharesCallback() throws Exception {
        edt(() -> {
            var shell = new ShellPanel(); var creates = new java.util.concurrent.atomic.AtomicInteger();
            shell.actions(() -> { }, creates::incrementAndGet, password -> fail());
            shell.render(org.totipo.desktop.ShellState.LOCKED, java.nio.file.Path.of("a"), "", false);
            assertTrue(shell.createNew.isVisible()); assertTrue(shell.createNew.isEnabled());
            assertEquals("New Vault…", shell.createNew.getText()); shell.createNew.doClick(0); assertEquals(1, creates.get());
            shell.render(org.totipo.desktop.ShellState.LOCKED, java.nio.file.Path.of("a"), "", true);
            shell.createNew.doClick(0); assertEquals(1, creates.get());
        });
    }
}
