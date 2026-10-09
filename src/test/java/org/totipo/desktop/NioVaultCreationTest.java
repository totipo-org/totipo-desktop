package org.totipo.desktop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.totipo.CreateVaultResult;
import org.totipo.OpenResult;
import org.totipo.storage.nio.NioTotipo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class NioVaultCreationTest {
    @TempDir Path directory;

    @Test void cleanLocationCreatesAndReopensWithOrdinaryAndEmptyPasswords() throws Exception {
        for (char[] password : new char[][] { {'t', 'e', 's', 't'}, {} }) {
            Path target = Files.createTempDirectory(directory, "clean-");
            try {
                try (var session = assertInstanceOf(CreateVaultResult.Created.class, NioTotipo.create(target, password)).session()) {
                    assertNotNull(session.state());
                }
                byte[] bootstrap = Files.readAllBytes(target.resolve("vault"));
                assertInstanceOf(CreateVaultResult.AlreadyExists.class, NioTotipo.create(target, password));
                assertArrayEquals(bootstrap, Files.readAllBytes(target.resolve("vault")));
                try (var session = assertInstanceOf(OpenResult.Opened.class, NioTotipo.open(target, password)).session()) {
                    assertNotNull(session.state());
                }
            } finally { Arrays.fill(password, '\0'); }
        }
    }

    @Test void observedObjectCandidateRefusesCreationAndLeavesFolderIntact() throws Exception {
        Path objects = Files.createDirectory(directory.resolve("objects-v1"));
        Path candidate = objects.resolve("a".repeat(64));
        byte[] contextualBytes = {1, 2, 3};
        Files.write(candidate, contextualBytes);
        char[] password = {'t'};
        try {
            var failed = assertInstanceOf(CreateVaultResult.Failed.class, NioTotipo.create(directory, password));
            assertEquals(CreateVaultResult.FailureReason.OBJECT_DATA_OBSERVED, failed.reason());
            assertFalse(Files.exists(directory.resolve("vault")));
            assertArrayEquals(contextualBytes, Files.readAllBytes(candidate));
        } finally { Arrays.fill(password, '\0'); }
    }
}
