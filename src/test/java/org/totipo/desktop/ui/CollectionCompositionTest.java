package org.totipo.desktop.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import java.awt.BorderLayout;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;

class CollectionCompositionTest {
    static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) { if (child instanceof Container c) { layout(c); } }
    }
    static List<JButton> buttons(Container root) {
        List<JButton> result = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (child instanceof JButton button) { result.add(button); }
            else if (child instanceof Container c) { result.addAll(buttons(c)); }
        }
        return result;
    }
    @Test void collectionHasSixteenPixelOuterPaddingAndHeaderToListGap() throws Exception {
        edt(() -> {
            var vault = new VaultPanel(); var browser = TokenBrowserTest.find(vault, TokenBrowserPanel.class);
            try {
                for (var state : List.of(new State(token(1, active("one"))),
                        new State(token(1, active("one"), active("two"))))) {
                    vault.render(state.value);
                    for (Dimension size : List.of(new Dimension(400, 520), VaultPanel.MINIMUM_SIZE,
                            VaultPanel.INITIAL_SIZE, new Dimension(1200, 1000))) {
                        vault.setSize(size); layout(vault);
                        var search = SwingUtilities.convertRectangle(browser.search.getParent(), browser.search.getBounds(), vault);
                        var header = SwingUtilities.convertRectangle(browser.searchBar.getParent(), browser.searchBar.getBounds(), vault);
                        var first = SwingUtilities.convertRectangle(browser.list, browser.list.getComponent(0).getBounds(), vault);
                        assertEquals(16, header.y);
                        assertEquals(16, header.x);
                        assertEquals(size.width - 16, header.x + header.width);
                        assertTrue(first.y - header.y - header.height >= DesktopStyle.SECTION);
                        assertTrue(first.y - header.y - header.height <= DesktopStyle.SECTION + 2); // Includes the scroll-pane border.
                        assertEquals(browser.search.getPreferredSize().height, search.height);
                        assertEquals(browser.add.getPreferredSize().height, browser.add.getHeight());
                        assertTrue(search.width >= 100);
                        var count = SwingUtilities.convertRectangle(browser.resultCount.getParent(), browser.resultCount.getBounds(), vault);
                        var add = SwingUtilities.convertRectangle(browser.add.getParent(), browser.add.getBounds(), vault);
                        assertTrue(search.x + search.width < count.x);
                        assertTrue(count.x + count.width < add.x);
                        assertEquals(8, count.x - search.x - search.width);
                        assertEquals(DesktopStyle.NORMAL, add.x - count.x - count.width);
                    }
                }
            } finally { vault.closing(); }
        });
    }
    @Test void refreshIsMenuOnlyAndNormalHeaderOrdersSearchCountAddAtMinimumWidth() throws Exception {
        edt(() -> {
            var vault = new VaultPanel(); var browser = TokenBrowserTest.find(vault, TokenBrowserPanel.class);
            try {
                vault.render(new State(token(1, active("one")), token(2, active("two")), token(3, active("three"))).value);
                assertTrue(buttons(vault).stream().noneMatch(button -> "Refresh".equals(button.getText())));
                assertEquals("Refresh", vault.menuBar().getMenu(1).getItem(2).getText());
                vault.setSize(640, 520); layout(vault);
                int searchWidth = browser.search.getWidth(); assertTrue(searchWidth > 200);
                var searchBounds = SwingUtilities.convertRectangle(browser.search.getParent(), browser.search.getBounds(), browser);
                var countBounds = SwingUtilities.convertRectangle(browser.resultCount.getParent(), browser.resultCount.getBounds(), browser);
                var addBounds = SwingUtilities.convertRectangle(browser.add.getParent(), browser.add.getBounds(), browser);
                assertTrue(searchBounds.x + searchBounds.width < countBounds.x);
                assertTrue(countBounds.x + countBounds.width < addBounds.x);
                assertEquals(browser.getWidth(), addBounds.x + addBounds.width);
                int addWidth = browser.add.getWidth(); assertTrue(addWidth < 150);
                assertTrue(browser.add.getHeight() >= DesktopStyle.CONTROL);
                vault.setSize(1000, 800); layout(vault);
                assertTrue(browser.search.getWidth() > searchWidth + 300); assertEquals(addWidth, browser.add.getWidth());
                assertEquals("3 TOTPs", browser.resultCount.getText()); browser.search.setText("one"); assertEquals("1 of 3", browser.resultCount.getText());
            } finally { vault.closing(); }
        });
    }
    @Test void emptyVaultHasCenteredPrimaryAddWithoutHeaderDuplicateAndTransitionsToCollection() throws Exception {
        edt(() -> {
            var vault = new VaultPanel(); var browser = TokenBrowserTest.find(vault, TokenBrowserPanel.class); var creates = new AtomicInteger();
            vault.tokenActions(creates::incrementAndGet, (base, alternative, explanation) -> { });
            try {
                vault.render(new State().value); vault.setSize(760, 820); layout(vault);
                assertEquals("No TOTPs yet", browser.empty.getText()); assertEquals("Add a TOTP to get started.", browser.emptyHint.getText());
                assertTrue(browser.emptyState.isVisible()); assertFalse(browser.searchBar.isVisible());
                assertSame(browser.emptyState.content, browser.emptyAdd.getParent());
                assertEquals(DesktopStyle.ActionRole.PrimaryAction, browser.emptyAdd.getClientProperty("totipo.actionRole"));
                var bounds = browser.emptyState.content.getBounds();
                assertEquals(browser.emptyState.getWidth() / 2.0, bounds.getCenterX(), .5);
                assertTrue(bounds.getCenterY() < browser.emptyState.getHeight() / 2.0); assertTrue(bounds.y > 24);
                assertEquals(bounds.width / 2.0, browser.emptyAdd.getBounds().getCenterX(), .5);
                browser.emptyAdd.doClick(0); assertEquals(1, creates.get());
                vault.render(new State(token(1, active("one"))).value);
                assertTrue(browser.searchBar.isVisible()); assertFalse(browser.emptyState.isVisible()); assertEquals("1 TOTPs", browser.resultCount.getText());
                assertTrue(browser.add.isEnabled());
            } finally { vault.closing(); }
        });
    }
    @Test void visibleVaultHeaderHasSixteenPixelSeparationFromCollectionHeader() throws Exception {
        edt(() -> {
            var vault = new VaultPanel(); var browser = TokenBrowserTest.find(vault, TokenBrowserPanel.class);
            try {
                vault.render(new State(token(1, active("one"))).value);
                vault.writeWarning("Try refreshing.");
                vault.setSize(VaultPanel.MINIMUM_SIZE); layout(vault);
                var heading = vault.notification.getParent();
                var notice = SwingUtilities.convertRectangle(heading.getParent(), heading.getBounds(), vault);
                var header = SwingUtilities.convertRectangle(browser.searchBar.getParent(), browser.searchBar.getBounds(), vault);
                assertEquals(16, header.y - notice.y - notice.height);
            } finally { vault.closing(); }
        });
    }
    @Test void noResultsKeepsHeaderAndQuietClearSearchRestoresRevealWithoutDerivation() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var browser = browser(clock); var state = new State(token(1, active("one")));
            var action = SwingUsability.action("Add", () -> { }); browser.collectionAction(action);
            try {
                browser.render(state.value); browser.reveal(id(1)); assertEquals(1, state.calls.size());
                browser.search.setText("github work");
                assertEquals("0 of 1", browser.resultCount.getText()); assertEquals("No TOTPs match \"github work\"", browser.empty.getText());
                assertTrue(browser.emptyState.isVisible()); assertTrue(browser.searchBar.isVisible()); assertTrue(browser.add.isEnabled());
                assertFalse(browser.emptyHint.isVisible()); assertSame(browser.emptyState.content, browser.clearSearch.getParent());
                assertEquals(DesktopStyle.ActionRole.QuietAction, browser.clearSearch.getClientProperty("totipo.actionRole"));
                var focus = new ArrayList<JComponent>(); browser.focus = focus::add;
                browser.clearSearch.doClick(0);
                assertEquals("", browser.search.getText()); assertEquals(List.of(browser.search), focus);
                assertEquals("1 TOTPs", browser.resultCount.getText()); assertFalse(browser.emptyState.isVisible());
                assertEquals(1, state.calls.size()); assertFalse(browser.row(id(1)).show.isVisible());
                assertNotNull(TotpCopyTest.codeLabel(browser.row(id(1))));
            } finally { browser.closing(); }
        });
    }
    @Test void conflictHeaderBalancesResolveAndHeadingWithoutChangingSemanticChildren() throws Exception {
        edt(() -> {
            var browser = browser(new MutableClock()); var state = new State(token(1, active("one"), active("two")));
            try {
                browser.render(state.value); browser.list.setSize(600, 500); layout(browser.list);
                var group = (JPanel) browser.list.getComponent(0);
                var wrapper = (JPanel) group.getComponent(0);
                var header = (JPanel) ((BorderLayout) wrapper.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                var heading = TokenBrowserTest.find(header, JLabel.class); var resolve = buttons(header).getFirst();
                assertEquals(8, header.getInsets().top); assertEquals(8, header.getInsets().bottom);
                assertEquals(heading.getBounds().getCenterY(), resolve.getBounds().getCenterY(), .5);
                assertEquals("Resolve", resolve.getText()); assertEquals(2, browser.rows.size()); assertTrue(state.calls.isEmpty());
                assertTrue(browser.rows.stream().allMatch(row -> row.token.hasConflict()));
                var divider = (JPanel) ((BorderLayout) wrapper.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
                var first = SwingUtilities.convertRectangle(browser.rows.getFirst().getParent(),
                        browser.rows.getFirst().getBounds(), group);
                assertEquals(1, divider.getHeight()); assertTrue(divider.getWidth() > 0);
                assertSame(wrapper, divider.getParent()); assertNotSame(header, divider.getParent());
                assertEquals(header.getPreferredSize().height + DesktopStyle.BORDER, wrapper.getPreferredSize().height);
                assertEquals(resolve.getPreferredSize(), resolve.getSize());
                assertEquals(header.getY() + header.getHeight(), divider.getY());
                assertTrue(wrapper.getY() + divider.getY() + divider.getHeight() <= first.y);
                assertEquals(first.x + DesktopStyle.NORMAL, wrapper.getX() + divider.getX() + divider.getInsets().left);
                assertEquals(first.x + first.width, wrapper.getX() + divider.getX() + divider.getWidth());
                var border = (javax.swing.border.CompoundBorder) divider.getBorder();
                var line = (javax.swing.border.MatteBorder) border.getInsideBorder();
                assertEquals(DesktopStyle.border(), line.getMatteColor());
                var pixels = new java.awt.image.BufferedImage(divider.getWidth(), divider.getHeight(),
                        java.awt.image.BufferedImage.TYPE_INT_ARGB);
                var graphics = pixels.createGraphics();
                try { divider.paint(graphics); } finally { graphics.dispose(); }
                assertEquals(DesktopStyle.border().getRGB(), pixels.getRGB(divider.getInsets().left, 0));
                assertEquals(0, pixels.getRGB(0, 0));
            } finally { browser.closing(); }
        });
    }
}
