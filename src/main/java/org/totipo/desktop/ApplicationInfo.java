package org.totipo.desktop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Application metadata shared by development and installed classpaths. */
public final class ApplicationInfo {
    public static final String NAME = "Totipo Desktop";
    public static final String REPOSITORY = "https://github.com/totipo-org/totipo-desktop";
    private ApplicationInfo() { }

    public static String version() {
        try (var input = ApplicationInfo.class.getResourceAsStream("application-version.txt")) {
            if (input == null) { throw new IllegalStateException("Missing application version resource"); }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).stripTrailing();
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot read application version", failure);
        }
    }
}
