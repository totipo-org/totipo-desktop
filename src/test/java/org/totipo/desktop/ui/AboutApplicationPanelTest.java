package org.totipo.desktop.ui;

import java.awt.event.ActionEvent;
import java.awt.Container;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
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
            assertFalse(panel.details.isEditable());
            assertTrue(panel.details.isFocusable());
            assertNotNull(panel.details.getAccessibleContext().getAccessibleName());
            assertEquals(ApplicationInfo.REPOSITORY, assertDoesNotThrow(() ->
                    panel.project.getDocument().getText(0, panel.project.getDocument().getLength())).strip());
            assertEquals(ApplicationInfo.REPOSITORY, panel.project.getToolTipText());
            assertEquals(ApplicationInfo.REPOSITORY, panel.project.getAccessibleContext().getAccessibleName());
            assertTrue(panel.project.getAccessibleContext().getAccessibleDescription().contains(ApplicationInfo.REPOSITORY));
            assertTrue(panel.project.isFocusable());
            assertFalse(panel.project.isEditable());
            panel.project.selectAll();
            assertEquals(ApplicationInfo.REPOSITORY, panel.project.getSelectedText().strip());
            assertNotNull(panel.project.getActionMap().get("copy-to-clipboard"));
            var hypertext = (javax.accessibility.AccessibleHypertext) panel.project.getAccessibleContext().getAccessibleText();
            assertEquals(1, hypertext.getLinkCount());
            assertEquals(ApplicationInfo.REPOSITORY, hypertext.getLink(0).getAccessibleActionObject(0).toString());
            panel.close.doClick();
            assertTrue(closed[0]); closed[0] = false;
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

    @Test void mouseActivationOpensCanonicalUrlOffEdt() throws Exception {
        activate(null);
    }

    @Test void spaceActivationOpensCanonicalUrlOffEdt() throws Exception {
        activate("SPACE");
    }

    @Test void enterAndMnemonicActivationOpenCanonicalUrlOffEdt() throws Exception {
        activate("ENTER");
        activate("alt G");
    }

    @Test void naturalSizeFitsFullUrlAndGrowsWithFonts() throws Exception {
        edt(() -> {
            var normal = new AboutApplicationPanel(() -> { });
            var normalSize = normal.getPreferredSize();
            assertFalse(normal.isPreferredSizeSet());
            assertFalse(normal.project.isPreferredSizeSet());
            assertFits(normal);
            Object original = UIManager.get("Label.font");
            try {
                UIManager.put("Label.font", normal.details.getFont().deriveFont(normal.details.getFont().getSize2D() * 2));
                var scaled = new AboutApplicationPanel(() -> { });
                assertTrue(scaled.getPreferredSize().width > normalSize.width);
                assertTrue(scaled.getPreferredSize().height > normalSize.height);
                assertFits(scaled);
            } finally { UIManager.put("Label.font", original); }
        });
    }

    private static void assertFits(AboutApplicationPanel panel) {
        panel.setSize(panel.getPreferredSize());
        layout(panel);
        assertTrue(panel.project.getWidth() >= panel.project.getPreferredSize().width);
        assertTrue(panel.project.getHeight() >= panel.project.getPreferredSize().height);
        assertTrue(panel.close.getWidth() >= panel.close.getPreferredSize().width);
        assertTrue(panel.close.getHeight() >= panel.close.getPreferredSize().height);
    }

    private static void layout(Container container) {
        container.doLayout();
        for (var child : container.getComponents()) {
            if (child instanceof Container nested) { layout(nested); }
        }
    }

    private static void clickLink(AboutApplicationPanel panel) {
        var event = new HyperlinkEvent(panel.project, HyperlinkEvent.EventType.ACTIVATED,
                assertDoesNotThrow(() -> URI.create(ApplicationInfo.REPOSITORY).toURL()));
        for (var listener : panel.project.getHyperlinkListeners()) { listener.hyperlinkUpdate(event); }
    }

    private void activate(String key) throws Exception {
        var opened = new CompletableFuture<URI>();
        var completed = new CompletableFuture<Void>();
        edt(() -> {
            var panel = new AboutApplicationPanel(() -> { }, uri -> {
                if (SwingUtilities.isEventDispatchThread()) {
                    opened.completeExceptionally(new AssertionError("Browser invoked on EDT"));
                } else { opened.complete(uri); }
            }, message -> completed.completeExceptionally(new AssertionError(message)));
            panel.project.addPropertyChangeListener("enabled", event -> {
                if (Boolean.TRUE.equals(event.getNewValue())) { completed.complete(null); }
            });
            if (key != null) {
                int condition = key.equals("alt G") ? JComponent.WHEN_IN_FOCUSED_WINDOW : JComponent.WHEN_FOCUSED;
                Object binding = panel.project.getInputMap(condition).get(KeyStroke.getKeyStroke(key));
                assertNotNull(binding);
                panel.project.getActionMap().get(binding).actionPerformed(
                        new ActionEvent(panel.project, ActionEvent.ACTION_PERFORMED, "keyboard"));
            } else { clickLink(panel); }
            assertFalse(panel.project.isEnabled());
            assertTrue(panel.close.isEnabled());
        });
        assertEquals(URI.create(ApplicationInfo.REPOSITORY), opened.get(5, TimeUnit.SECONDS));
        completed.get(5, TimeUnit.SECONDS);
    }

    @Test void unsupportedFailedAndDeniedBrowsingProvideManualUrlAndAllowRetry() throws Exception {
        for (Exception failure : new Exception[]{new UnsupportedOperationException(), new IOException(), new SecurityException()}) {
            var reported = new CompletableFuture<String>();
            var panels = new AboutApplicationPanel[1];
            edt(() -> {
                var panel = new AboutApplicationPanel(() -> { }, uri -> {
                    if (failure instanceof IOException io) { throw io; }
                    throw (RuntimeException) failure;
                }, message -> {
                    assertTrue(SwingUtilities.isEventDispatchThread());
                    reported.complete(message);
                });
                panels[0] = panel;
                clickLink(panel);
            });
            String message = reported.get(5, TimeUnit.SECONDS);
            assertTrue(message.contains("Open this address manually"));
            assertTrue(message.contains(ApplicationInfo.REPOSITORY));
            edt(() -> {
                assertTrue(panels[0].project.isEnabled());
                assertTrue(panels[0].close.isEnabled());
            });
        }
    }
}
