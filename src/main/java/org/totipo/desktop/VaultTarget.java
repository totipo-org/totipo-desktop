package org.totipo.desktop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Location recognition only, never validation/authentication or corruption diagnosis. */
final class VaultTarget {
    private VaultTarget() { }
    static boolean recognizable(Path directory) {
        // The public format's vault bootstrap signature. Read no encrypted payload,
        // require no supported version/length: recognizable invalid targets remain selectable.
        byte[] signature = "TOTIPO-VLT".getBytes(StandardCharsets.US_ASCII);
        try {
            if (!Files.isDirectory(directory) || !Files.isRegularFile(directory.resolve("vault"))) { return false; }
            try (var input = Files.newInputStream(directory.resolve("vault"))) {
                return Arrays.equals(signature, input.readNBytes(signature.length));
            }
        } catch (IOException | SecurityException unavailable) { return false; }
    }
}
