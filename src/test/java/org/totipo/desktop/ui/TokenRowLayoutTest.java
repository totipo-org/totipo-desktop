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
        if (column > 0) { assertEquals(20, c.insets.left); assertEquals(0, c.weightx); }
        else { assertEquals(1, c.weightx); assertEquals(0, slot.getMinimumSize().width); }
    }
    @Test void ordinaryHiddenRevealedAndPendingShareTwoLinesAndStableActionSlots() throws Exception {
        edt(() -> {
            var row = new TokenRowPanel(token(1, active("GitHub")), () -> { }, () -> { }, () -> { }, i -> { });
            cell(row, row.identityTop, 0, 0); cell(row, row.identityBottom, 0, 1);
            cell(row, row.statusTop, 1, 0); cell(row, row.statusBottom, 1, 1);
            cell(row, row.actionTop, 2, 0); cell(row, row.actionBottom, 2, 1);
            assertSame(row.identityTop, row.primary.getParent()); assertSame(row.identityBottom, row.account.getParent());
            assertSame(row.actionTop, row.show.getParent()); assertSame(row.actionBottom, row.edit.getParent());
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
            assertEquals(FlowLayout.TRAILING, ((FlowLayout) ring.getParent().getLayout()).getAlignment());
            JButton copy = TotpCopyTest.buttons(row).get(0); assertSame(row.actionTop, copy.getParent());
            assertSame(row.actionBottom, row.edit.getParent()); assertFalse(row.show.isVisible());
            assertEquals(preferred.height, row.getPreferredSize().height); assertEquals(minimum.height, row.getMinimumSize().height);
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
            assertTrue(row.statusTop.getX() >= row.identityTop.getX() + row.identityTop.getWidth());
            assertTrue(row.actionTop.getX() >= row.statusTop.getX() + row.statusTop.getWidth());
            assertEquals(row.statusTop.getPreferredSize().width, row.statusTop.getWidth());
            assertEquals(row.actionTop.getPreferredSize().width, row.actionTop.getWidth());
            assertEquals(row.primary.getText(), row.primary.getToolTipText()); row.retire();
        });
    }
}
