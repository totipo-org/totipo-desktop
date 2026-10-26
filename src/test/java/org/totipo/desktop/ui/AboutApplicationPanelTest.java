package org.totipo.desktop.ui;

import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.totipo.desktop.ApplicationInfo;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.edt;

class AboutApplicationPanelTest {
    @Test void buildVersionAndAccessibleKeyboardDismissal() throws Exception {
        assertEquals(System.getProperty("totipo.expectedVersion"), ApplicationInfo.version());
        edt(() -> {
            boolean[] closed = {false};
            var panel = new AboutApplicationPanel(() -> closed[0] = true);
            assertTrue(panel.details.getText().contains("Totipo Desktop\nVersion " + ApplicationInfo.version()));
            assertTrue(panel.details.getText().contains(ApplicationInfo.REPOSITORY));
            assertFalse(panel.details.isEditable());
            assertTrue(panel.details.isFocusable());
            assertNotNull(panel.details.getAccessibleContext().getAccessibleName());
            var root = new JRootPane(); panel.installDialog(root);
            assertSame(panel.close, root.getDefaultButton());
            Object escape = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("ESCAPE"));
            root.getActionMap().get(escape).actionPerformed(null);
            assertTrue(closed[0]);
            var menu = AboutApplicationPanel.menu(null);
            assertEquals("Help", menu.getText());
            assertEquals('H', menu.getMnemonic());
            assertEquals("About Totipo…", menu.getItem(0).getText());
            assertTrue(menu.getItem(0).isEnabled());
        });
    }
}
