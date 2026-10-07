package org.totipo.desktop.ui;

import java.awt.Color;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.edt;

class CanonicalBrandingTest {
    @Test void allWindowSizesLoadFromClasspath() {
        int[] sizes = {16, 32, 48, 64, 128, 256};
        var images = ApplicationIcons.windowImages();
        assertEquals(sizes.length, images.size());
        for (int i = 0; i < sizes.length; i++) {
            assertNotNull(ApplicationIcons.class.getResource("/org/totipo/desktop/icons/totipo-" + sizes[i] + ".png"));
            assertEquals(sizes[i], images.get(i).getWidth(null));
            assertEquals(sizes[i], images.get(i).getHeight(null));
        }
    }

    @Test void canonicalStopsAndSemanticSeparation() throws Exception {
        assertEquals(new Color(0x46FB70), BrandPalette.GREEN_LIGHT);
        assertEquals(new Color(0x08D267), BrandPalette.GREEN);
        assertEquals(new Color(0x028B55), BrandPalette.GREEN_DEEP);
        assertEquals(new Color(0x026344), BrandPalette.GREEN_DARK);
        edt(() -> {
            var accent = DesktopStyle.accent();
            assertTrue(accent.equals(BrandPalette.GREEN) || accent.equals(BrandPalette.GREEN_DARK));
            assertNotEquals(accent, DesktopStyle.warning());
            assertNotEquals(accent, DesktopStyle.danger());
            assertNotEquals(accent, DesktopStyle.info());
            assertNotEquals(accent, DesktopStyle.success());
        });
    }
}
