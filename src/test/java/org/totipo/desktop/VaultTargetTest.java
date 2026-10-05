package org.totipo.desktop;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.totipo.CreateVaultResult;
import org.totipo.storage.nio.NioTotipo;
import static org.junit.jupiter.api.Assertions.*;

class VaultTargetTest {
    @TempDir Path directory;
    @Test void realReleasedApiBootstrapIsRecognizedWithoutUnlocking() {
        char[] password = {'p'};
        try (var session = assertInstanceOf(CreateVaultResult.Created.class, NioTotipo.create(directory, password)).session()) {
            assertTrue(VaultTarget.recognizable(directory)); assertNotNull(session);
        } finally { java.util.Arrays.fill(password, '\0'); }
    }
    @Test void recognizableInvalidOrUnsupportedRecordIsAcceptedAsTarget() throws Exception {
        Files.writeString(directory.resolve("vault"), "TOTIPO-VLT\u007f"); assertTrue(VaultTarget.recognizable(directory));
        Files.writeString(directory.resolve("vault"), "TOTIPO-VLT"); assertTrue(VaultTarget.recognizable(directory));
    }
    @Test void ordinaryFileCalledVaultDoesNotMakeLocationRecognizable() throws Exception {
        Files.writeString(directory.resolve("vault"), "ordinary text"); assertFalse(VaultTarget.recognizable(directory));
    }
    @Test void missingLocationDirectoryOrBootstrapIsNotRecognizable() throws Exception {
        assertFalse(VaultTarget.recognizable(directory)); assertFalse(VaultTarget.recognizable(directory.resolve("missing")));
        Path file = Files.writeString(directory.resolve("file"), "TOTIPO-VLT"); assertFalse(VaultTarget.recognizable(file));
        Files.createDirectory(directory.resolve("vault")); assertFalse(VaultTarget.recognizable(directory));
    }
}
