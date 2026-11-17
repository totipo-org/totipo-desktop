package org.totipo.desktop;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.totipo.*;
import org.totipo.backup.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.BackupRestoreTest.*;
import static org.totipo.desktop.VaultFolderWatcherTest.eventually;

/** Public released APIs and actual shared-safe NIO publication, with disposable fixtures. */
@Timeout(45)
class HistoryImportTest {
    @TempDir Path directory;
    static BackupImport prepare(VaultSession session, byte[] zip) throws IOException {
        return session.prepareImport(new ByteArrayInputStream(zip), BackupLimits.DEFAULT);
    }
    static Map<Path, byte[]> snapshot(Path root) throws IOException {
        Map<Path, byte[]> result = new LinkedHashMap<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).toList()) {
                result.put(path, Files.readAllBytes(path));
            }
        }
        return result;
    }
    static void preserved(Map<Path, byte[]> before) throws IOException {
        for (var entry : before.entrySet()) { assertArrayEquals(entry.getValue(), Files.readAllBytes(entry.getKey()), entry.getKey().toString()); }
    }
    @ParameterizedTest @ValueSource(strings = {"empty", "history"})
    void emptyAndExactExistingAreNoOpAndRepeatedImportPreservesAllBytes(String name) throws Exception {
        restore(directory, "target", name); Path root = directory.resolve("target");
        Files.createDirectories(root.resolve("objects-v1"));
        Files.write(root.resolve("objects-v1").resolve("f".repeat(64)), new byte[] {9, 8, 7});
        Files.write(root.resolve("objects-v1/unrelated"), new byte[] {6, 5, 4});
        var before = snapshot(root);
        try (var session = open(root); var plan = prepare(session, fixture(name))) {
            for (int i = 0; i < 2; i++) {
                var result = plan.apply(); assertEquals(ImportResult.Outcome.NO_OP, result.outcome());
                assertEquals(name.equals("empty") ? 0 : 3, result.retained()); assertEquals(0, result.installed());
                preserved(before);
            }
        }
    }
    @Test void missingAncestorsRetainNewerDescendantsAndIntroduceTombstoneConflict() throws Exception {
        restore(directory, "target", "missing-parent"); Path root = directory.resolve("target");
        try (var session = open(root)) {
            var first = finished(session); var original = first.tokens().getFirst().alternatives().getFirst();
            assertFalse(first.tokens().getFirst().unresolvedReferences().isEmpty());
            var metadata = new ClientMetadata(Optional.of("desktop test"), Optional.of(-1L));
            try (var edit = first.update(original)) {
                assertInstanceOf(SaveResult.Saved.class, edit.issuer("Newer descendant").metadata(metadata).save());
            }
            var before = snapshot(root);
            try (var plan = prepare(session, fixture("history"))) {
                var result = plan.apply(); assertEquals(ImportResult.Outcome.COMPLETE, result.outcome());
                assertEquals(2, result.installed()); assertEquals(1, result.retained());
                assertEquals(ImportResult.Outcome.COMPLETE, plan.apply().outcome());
            }
            session.requestRefresh();
            eventually(() -> session.state().tokens().getFirst().hasConflict());
            var token = session.state().tokens().getFirst();
            assertTrue(token.unresolvedReferences().isEmpty());
            assertTrue(token.alternatives().stream().anyMatch(a -> a.descriptor().status() == TokenStatus.TOMBSTONED));
            var newer = token.alternatives().stream().filter(a -> a.descriptor().issuer().equals("Newer descendant")).findFirst().orElseThrow();
            assertEquals(metadata, newer.heads().getFirst().metadata());
            assertFalse(newer.heads().getFirst().parents().getFirst().parents().isEmpty());
            preserved(before);
            try (var again = prepare(session, fixture("history"))) { assertEquals(ImportResult.Outcome.NO_OP, again.apply().outcome()); }
            preserved(before);
        }
    }
    @Test void equalValuedDistinctHeadsAndEncryptedMetadataSurviveImportAlongsideConcurrentBranch() throws Exception {
        restore(directory, "source", "missing-parent"); restore(directory, "target", "missing-parent");
        Path root = directory.resolve("target");
        try (var source = open(directory.resolve("source")); var target = open(root)) {
            var base = finished(source); var alternative = base.tokens().getFirst().alternatives().getFirst();
            for (long time : List.of(1L, 2L)) {
                try (var edit = base.update(alternative)) {
                    assertInstanceOf(SaveResult.Saved.class, edit.issuer("Same value").metadata(new ClientMetadata(Optional.of("source"), Optional.of(time))).save());
                }
            }
            var targetBase = finished(target);
            try (var edit = targetBase.update(targetBase.tokens().getFirst().alternatives().getFirst())) {
                assertInstanceOf(SaveResult.Saved.class, edit.issuer("Independent newer branch").save());
            }
            var before = snapshot(root);
            try (var capture = source.captureBackup(BackupLimits.DEFAULT); var plan = prepare(target, bytes(capture))) {
                assertEquals(ImportResult.Outcome.COMPLETE, plan.apply().outcome());
            }
            target.requestRefresh();
            eventually(() -> target.state().tokens().getFirst().heads().size() == 3);
            var token = target.state().tokens().getFirst(); assertTrue(token.hasConflict()); assertEquals(2, token.alternatives().size());
            var same = token.alternatives().stream().filter(a -> a.descriptor().issuer().equals("Same value")).findFirst().orElseThrow();
            assertEquals(2, same.heads().size());
            assertEquals(Set.of(1L, 2L), same.heads().stream().map(h -> h.metadata().clientTimeBits().orElseThrow()).collect(java.util.stream.Collectors.toSet()));
            preserved(before);
        }
    }
    @Test void identityAndFreshExactBootstrapMismatchCannotInstall() throws Exception {
        restore(directory, "target", "empty"); Path root = directory.resolve("target"); var before = snapshot(root);
        try (var session = open(root)) {
            Path other = Files.createDirectory(directory.resolve("other"));
            try (var different = assertInstanceOf(CreateVaultResult.Created.class,
                    org.totipo.storage.nio.NioTotipo.create(other, new char[0])).session();
                 var capture = different.captureBackup(BackupLimits.DEFAULT)) {
                assertEquals(BackupException.Reason.VAULT_CHANGED, assertThrows(BackupException.class, () -> prepare(session, bytes(capture))).reason());
            }
            byte[] original = Files.readAllBytes(root.resolve("vault")); byte[] changed = original.clone(); changed[86] ^= 1;
            // Simulate external substitution, never an import repair or application write.
            Files.write(root.resolve("vault"), changed);
            assertEquals(BackupException.Reason.VAULT_CHANGED, assertThrows(BackupException.class, () -> prepare(session, fixture("history"))).reason());
            assertArrayEquals(changed, Files.readAllBytes(root.resolve("vault")));
            Files.write(root.resolve("vault"), original); preserved(before);
            assertFalse(Files.exists(root.resolve("objects-v1")));
        }
    }
    static byte[] invalidEncryptedArchive() throws IOException {
        var entries = entries(fixture("history"));
        entries.get(entries.keySet().stream().filter(k -> k.startsWith("objects-v1/")).reduce((a, b) -> b).orElseThrow())[100] ^= 1;
        var output = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(output)) {
            List<String> names = new ArrayList<>(); names.add("totipo-backup.txt"); names.add("vault");
            entries.keySet().stream().filter(k -> k.startsWith("objects-v1/")).forEach(names::add);
            for (String name : names) {
                byte[] payload = entries.get(name); var crc = new CRC32(); crc.update(payload);
                var entry = new ZipEntry(name); entry.setMethod(ZipEntry.STORED); entry.setSize(payload.length); entry.setCompressedSize(payload.length);
                entry.setCrc(crc.getValue()); entry.setTimeLocal(java.time.LocalDateTime.of(1980, 1, 2, 0, 0));
                zip.putNextEntry(entry); zip.write(payload); zip.closeEntry();
            }
        }
        return output.toByteArray();
    }
    @Test void fullValidationRejectsMalformedUnsupportedAndLateInvalidObjectBeforeAnyPublication() throws Exception {
        restore(directory, "target", "empty"); Path root = directory.resolve("target"); var before = snapshot(root);
        byte[] malformed = fixture("history"); malformed[0] = 0;
        byte[] unsupported = fixture("history"); unsupported[8] = 8;
        try (var session = open(root)) {
            for (byte[] zip : List.of(malformed, unsupported, invalidEncryptedArchive())) {
                assertThrows(BackupException.class, () -> prepare(session, zip)); preserved(before);
                assertFalse(Files.exists(root.resolve("objects-v1")));
            }
        }
    }
    @ParameterizedTest @ValueSource(strings = {"different", "directory", "symlink"})
    void unsafeTargetsArePreservedAndApplyRechecksWholePlan(String kind) throws Exception {
        restore(directory, "target", "empty"); Path root = directory.resolve("target");
        var archive = entries(fixture("history")); String name = archive.keySet().stream().filter(k -> k.startsWith("objects-v1/")).reduce((a, b) -> b).orElseThrow();
        try (var session = open(root); var plan = prepare(session, fixture("history"))) {
            Files.createDirectory(root.resolve("objects-v1")); Path occupied = root.resolve(name);
            switch (kind) {
                case "different" -> Files.write(occupied, new byte[] {9, 8, 7});
                case "directory" -> Files.createDirectory(occupied);
                case "symlink" -> Files.createSymbolicLink(occupied, root.resolve("vault"));
                default -> throw new AssertionError();
            }
            var before = snapshot(root); var result = plan.apply();
            assertEquals(ImportResult.Outcome.FAILED, result.outcome()); assertEquals(0, result.installed());
            assertEquals(kind.equals("different") ? ImportResult.Reason.TARGET_CONFLICT : ImportResult.Reason.TARGET_UNAVAILABLE, result.reason());
            assertThrows(BackupException.class, () -> prepare(session, fixture("history"))); preserved(before);
            if (kind.equals("symlink")) { assertTrue(Files.isSymbolicLink(occupied)); }
            if (kind.equals("directory")) { assertTrue(Files.isDirectory(occupied)); }
            try (var files = Files.list(root.resolve("objects-v1"))) { assertEquals(1, files.count()); }
        }
    }
    @Test void independentConcurrentNioImportsUseCreateOnlyPublication() throws Exception {
        restore(directory, "target", "empty"); Path root = directory.resolve("target"); var before = snapshot(root);
        try (var a = open(root); var b = open(root); var pa = prepare(a, fixture("history")); var pb = prepare(b, fixture("history"));
             var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var start = new java.util.concurrent.CountDownLatch(1);
            var ra = executor.submit(() -> { start.await(); return pa.apply(); });
            var rb = executor.submit(() -> { start.await(); return pb.apply(); }); start.countDown();
            for (var result : List.of(ra.get(), rb.get())) {
                assertTrue(Set.of(ImportResult.Outcome.COMPLETE, ImportResult.Outcome.NO_OP).contains(result.outcome()));
                assertEquals(3, result.installed() + result.retained());
            }
            preserved(before);
            for (var entry : entries(fixture("history")).entrySet()) {
                if (!entry.getKey().equals("totipo-backup.txt")) { assertArrayEquals(entry.getValue(), Files.readAllBytes(root.resolve(entry.getKey()))); }
            }
        }
    }
    @Test void cancellationBeforePreparationOrApplyAndSessionCloseRetainCanonicalBytes() throws Exception {
        restore(directory, "target", "empty"); Path root = directory.resolve("target"); var before = snapshot(root);
        var session = open(root);
        try {
            Thread.currentThread().interrupt();
            try { assertEquals(BackupException.Reason.CANCELLED, assertThrows(BackupException.class, () -> prepare(session, fixture("history"))).reason()); }
            finally { Thread.interrupted(); }
            try (var plan = prepare(session, fixture("history"))) {
                Thread.currentThread().interrupt();
                try {
                    var result = plan.apply(); assertEquals(ImportResult.Outcome.FAILED, result.outcome());
                    assertEquals(ImportResult.Reason.CANCELLED, result.reason()); assertEquals(3, result.remaining());
                } finally { Thread.interrupted(); }
                session.close();
                assertThrows(SessionClosedException.class, plan::apply);
                assertThrows(IllegalStateException.class, plan::summary);
            }
            assertThrows(SessionClosedException.class, () -> prepare(session, fixture("history")));
            preserved(before); assertFalse(Files.exists(root.resolve("objects-v1")));
        } finally { session.close(); }
    }
}
