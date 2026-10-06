package org.totipo.desktop.ui;

import java.awt.*;
import javax.swing.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.totipo.desktop.ui.TokenBrowserTest.find;
import static org.junit.jupiter.api.Assertions.*;

class TokenRowLayoutTest {
    static void cell(TokenRowPanel row, JPanel slot, int column, int line) {
        assertSame(row.grid, slot.getParent());
        var c = ((GridBagLayout) row.grid.getLayout()).getConstraints(slot);
        assertEquals(column, c.gridx); assertEquals(line, c.gridy);
        assertEquals(GridBagConstraints.BOTH, c.fill);
        if (column > 0) { assertEquals(column == 1 ? 16 : 12, c.insets.left); assertEquals(0, c.weightx); }
        else { assertEquals(1, c.weightx); assertEquals(0, slot.getMinimumSize().width); }
    }
    @Test void ordinaryHiddenRevealedAndPendingShareTwoLinesAndStableActionSlots() throws Exception {
        edt(() -> {
            var row = new TokenRowPanel(token(1, active("GitHub")), () -> { }, () -> { }, () -> { }, i -> { });
            cell(row, row.identityTop, 0, 0); cell(row, row.identityBottom, 0, 1);
            cell(row, row.statusTop, 1, 0); cell(row, row.statusBottom, 1, 1);
            assertEquals(2, ((GridBagLayout) row.grid.getLayout()).getConstraints(row.actionTop.getParent()).gridheight);
            assertSame(row.identityTop, row.primary.getParent()); assertSame(row.identityBottom, row.account.getParent());
            assertSame(row.actionTop, row.show.getParent()); assertSame(row.contextMenu, row.edit.getParent());
            assertEquals(0, row.statusTop.getComponentCount()); assertEquals(0, row.statusBottom.getComponentCount());
            assertNull(find(row, CountdownRing.class));
            Dimension preferred = row.getPreferredSize(), minimum = row.getMinimumSize();
            row.display(List.of(new TotpDisplay.Display("GitHub", "001234", 800, 24, false)));
            JLabel code = TotpCopyTest.codeLabel(row); assertSame(row.statusTop, code.getParent());
            assertEquals(SwingConstants.TRAILING, code.getHorizontalAlignment());
            CountdownRing ring = find(row.statusBottom, CountdownRing.class);
            assertNotNull(ring); assertEquals("24 seconds remaining", ring.getAccessibleContext().getAccessibleName());
            JLabel seconds = find(row.statusBottom, JLabel.class); assertEquals("24 sec", seconds.getText());
            assertSame(ring.getParent(), seconds.getParent());
            assertSame(seconds, ring.getParent().getComponent(0)); assertSame(ring, ring.getParent().getComponent(1));
            assertSame(ring.getParent(), ((BorderLayout) row.statusBottom.getLayout()).getLayoutComponent(BorderLayout.EAST));
            assertEquals(FlowLayout.TRAILING, ((FlowLayout) ring.getParent().getLayout()).getAlignment());
            JButton copy = TotpCopyTest.buttons(row).get(0); assertSame(row.actionTop, copy.getParent());
            assertSame(row.contextMenu, row.edit.getParent()); assertFalse(row.show.isVisible());
            assertEquals(preferred.height, row.getPreferredSize().height); assertEquals(minimum.height, row.getMinimumSize().height);
            for (long left : new long[]{29, 11, 9, 1}) {
                row.display(List.of(new TotpDisplay.Display("GitHub", "001234", 100, left, left < 10)));
                assertSame(ring, find(row.statusBottom, CountdownRing.class));
                assertSame(ring, ring.getParent().getComponent(1)); assertSame(seconds, ring.getParent().getComponent(0));
                assertEquals(left + " sec", seconds.getText()); assertEquals(preferred.height, row.getPreferredSize().height);
                assertSame(ring.getParent(), ((BorderLayout) row.statusBottom.getLayout()).getLayoutComponent(BorderLayout.EAST));
            }
            row.gracePending(List.of("GitHub"));
            assertEquals("", code.getText()); assertFalse(copy.isEnabled()); assertFalse(row.show.isVisible());
            assertEquals("Updating…", find(row.statusTop, JLabel.class).getText());
            assertEquals(0, find(row.statusBottom, CountdownRing.class).remaining());
            assertFalse(TotpCopyTest.buttons(row).get(0).isEnabled()); assertTrue(row.edit.isEnabled());
            assertFalse(row.getAccessibleContext().getAccessibleName().contains("001"));
            assertTrue(row.getAccessibleContext().getAccessibleName().contains("Updating"));
            assertEquals(preferred.height, row.getPreferredSize().height); assertEquals(minimum.height, row.getMinimumSize().height);
            row.display(List.of(new TotpDisplay.Display("GitHub", "005678", 1000, 30, false)));
            assertFalse(row.show.isVisible()); assertTrue(TotpCopyTest.buttons(row).get(0).isEnabled());
            row.retire();
        });
    }
    @Test void narrowRowsClipIdentityWhileKeepingCodeAndActionsSeparate() throws Exception {
        edt(() -> {
            var row = new TokenRowPanel(token(1, active("Long identity ".repeat(100))), () -> { }, () -> { }, () -> { }, i -> { });
            row.display(List.of(new TotpDisplay.Display("identity", "00123456", 500, 15, false)));
            row.setSize(360, row.getPreferredSize().height); row.doLayout(); row.grid.doLayout();
            row.actionTop.getParent().doLayout();
            assertTrue(row.statusTop.getX() >= row.identityTop.getX() + row.identityTop.getWidth());
            assertTrue(SwingUtilities.convertPoint(row.actionTop, 0, 0, row.grid).x >= row.statusTop.getX() + row.statusTop.getWidth());
            assertEquals(row.statusTop.getPreferredSize().width, row.statusTop.getWidth());
            assertEquals(row.actionTop.getPreferredSize().width, row.actionTop.getWidth());
            assertEquals(row.primary.getText(), row.primary.getToolTipText()); row.retire();
        });
    }
}
