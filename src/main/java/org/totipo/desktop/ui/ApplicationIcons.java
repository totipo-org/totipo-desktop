package org.totipo.desktop.ui;

import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Canonical exported PNGs bundled in the application JAR. */
final class ApplicationIcons {
    private static final List<Image> WINDOW_IMAGES = loadWindowImages();

    private ApplicationIcons() { }

    static List<Image> windowImages() { return WINDOW_IMAGES; }

    private static List<Image> loadWindowImages() {
        var images = new ArrayList<Image>();
        for (int size : new int[] {16, 32, 48, 64, 128, 256}) {
            String resource = "/org/totipo/desktop/icons/totipo-" + size + ".png";
            try (var input = ApplicationIcons.class.getResourceAsStream(resource)) {
                if (input == null) { throw new IllegalStateException("Missing application icon: " + resource); }
                var image = ImageIO.read(input);
                if (image == null || image.getWidth() != size || image.getHeight() != size) {
                    throw new IllegalStateException("Invalid application icon: " + resource);
                }
                images.add(image);
            } catch (IOException failure) {
                throw new IllegalStateException("Cannot load application icon: " + resource, failure);
            }
        }
        return List.copyOf(images);
    }
}
