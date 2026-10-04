package org.totipo.desktop.ui;

import java.awt.Color;
import javax.swing.*;
import javax.swing.border.*;
import javax.accessibility.AccessibleState;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class TokenRowSelectionTest {
    static boolean hasSelectionOutline(Border border) {
        if (border instanceof LineBorder line) { return line.getThickness() == 2 && line.getLineColor().equals(UIManager.getColor("List.selectionBackground")); }
        return border instanceof CompoundBorder compound && (hasSelectionOutline(compound.getInsideBorder()) || hasSelectionOutline(compound.getOutsideBorder()));
    }
    @Test void ordinarySelectionUsesRestrainedDerivedFillAndAccessibleOutlineWithoutGeometryChange() throws Exception {
        edt(() -> {
            var row = new TokenRowPanel(token(1, active("A")), () -> { }, () -> { }, () -> { }, i -> { });
            var height = row.getPreferredSize().height; var insets = row.getInsets();
            assertFalse(hasSelectionOutline(row.getBorder())); row.selected(true);
            assertNotEquals(UIManager.getColor("List.selectionBackground"), row.getBackground()); assertTrue(hasSelectionOutline(row.getBorder()));
            assertEquals(UIManager.getColor("List.foreground"), row.primary.getForeground()); assertNotEquals(row.getBackground(), row.primary.getForeground());
            assertEquals(height, row.getPreferredSize().height); assertEquals(insets, row.getInsets());
            assertTrue(row.isFocusable()); assertTrue(row.getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.SELECTED));
            row.selected(false); assertFalse(row.getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.SELECTED)); row.retire();
        });
    }
    @Test void conflictChildUsesOrdinarySelectionSurfaceAndRetainsAccessibleWarning() throws Exception {
        edt(() -> {
            var row = new TokenRowPanel(token(1, active("A"), active("B")), () -> { }, () -> { }, () -> { }, i -> { });
            Color conflict = row.getBackground(); var height = row.getPreferredSize().height;
            var ordinary = new TokenRowPanel(token(2, active("C")), () -> { }, () -> { }, () -> { }, i -> { });
            var separator = (MatteBorder) ((CompoundBorder) row.getBorder()).getOutsideBorder();
            assertEquals(0, separator.getBorderInsets(row).left); assertEquals(UIManager.getColor("List.background"), conflict);
            row.selected(true); ordinary.selected(true);
            assertEquals(ordinary.getBackground(), row.getBackground()); assertNotEquals(conflict, row.getBackground());
            assertTrue(hasSelectionOutline(row.getBorder()));
            assertEquals(UIManager.getColor("Separator.foreground"), ((MatteBorder)((CompoundBorder)row.getBorder()).getOutsideBorder()).getMatteColor());
            assertSame(row.identityBottom, row.warning.getParent()); assertTrue(row.getAccessibleContext().getAccessibleName().contains("conflicting versions"));
            assertTrue(row.getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.SELECTED)); assertEquals(height, row.getPreferredSize().height);
            assertEquals(UIManager.getColor("List.foreground"), row.primary.getForeground()); assertNotEquals(conflict, row.primary.getForeground()); row.retire(); ordinary.retire();
        });
    }
}
