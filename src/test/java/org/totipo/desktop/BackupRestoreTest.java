package org.totipo.desktop;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.totipo.*;
import org.totipo.backup.*;
import org.totipo.storage.nio.NioRestore;
import org.totipo.storage.nio.NioTotipo;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.backup.RestoreResult.Outcome.*;

@Timeout(45)
class BackupRestoreTest {
    @TempDir Path directory;
    static byte[] fixture(String name) throws IOException {
        try (var input = BackupRestoreTest.class.getResourceAsStream("/backup/profile-1/" + name + ".zip")) {
            return Objects.requireNonNull(input).readAllBytes();
        }
    }
    static BackupRestore plan(String name) throws IOException {
        return BackupArchives.prepareRestore(new ByteArrayInputStream(fixture(name)), BackupLimits.DEFAULT, "fixture password".toCharArray());
    }
    static void restore(Path parent, String child, String name) throws IOException {
        try (var plan = plan(name)) { assertEquals(COMPLETE, plan.install(NioRestore.reserve(parent, child, plan.summary().archiveSha256())).outcome()); }
    }
    static VaultSession open(Path root) {
        return assertInstanceOf(OpenResult.Opened.class, NioTotipo.open(root, "fixture password".toCharArray())).session();
    }
    static byte[] bytes(BackupCapture capture) throws IOException {
        var output = new ByteArrayOutputStream(); capture.writeTo(output); return output.toByteArray();
    }
    static Map<String, byte[]> entries(byte[] zip) throws IOException {
        Map<String, byte[]> entries = new TreeMap<>();
        try (var input = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry; while ((entry = input.getNextEntry()) != null) { entries.put(entry.getName(), input.readAllBytes()); }
        }
        return entries;
    }
    @ParameterizedTest @ValueSource(strings = {"empty", "history", "missing-parent"})
    void sharedGoldenRestoresAndBacksUpExactlyWithOriginalPassword(String name) throws Exception {
        String expected = switch (name) {
            case "empty" -> "603eb0dd84f67678bab82c78ee684dc22592b5e4de37f24d16f5694c382fa425";
            case "history" -> "369b77c872a32f2474d867e0653e39b66befdfc6cbd1244da486505ba8bca95b";
            default -> "6991ee0b764409728a10ee6612d6eced56207dbefa58a35611ea3d6112c25e58";
        };
        byte[] golden = fixture(name);
        assertEquals(expected, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(golden)));
        restore(directory, "vault-copy", name); Path root = directory.resolve("vault-copy");
        for (var entry : entries(golden).entrySet()) {
            if (!entry.getKey().equals("totipo-backup.txt")) { assertArrayEquals(entry.getValue(), Files.readAllBytes(root.resolve(entry.getKey()))); }
        }
        assertInstanceOf(OpenResult.AuthenticationFailed.class, NioTotipo.open(root, "wrong password".toCharArray()));
        try (var session = open(root); var capture = session.captureBackup(BackupLimits.DEFAULT)) {
            assertArrayEquals(golden, bytes(capture));
            assertEquals(name.equals("missing-parent") ? 1 : 0, capture.summary().unresolvedReferences());
            Path output = directory.resolve(name + ".zip");
            var receipt = BackupOutput.write(capture, root, output, () -> false, phase -> { });
            assertTrue(receipt.complete()); assertFalse(receipt.stagingRetained());
            assertArrayEquals(golden, Files.readAllBytes(output));
            if (name.equals("history")) {
                var observed = finished(session);
                assertTrue(observed.tokens().getFirst().hasConflict());
                assertTrue(observed.tokens().getFirst().alternatives().stream().anyMatch(a -> a.descriptor().status() == TokenStatus.TOMBSTONED));
            }
        }
    }
    static VaultState finished(VaultSession session) throws Exception {
        CompletableFuture<VaultState> finished = new CompletableFuture<>();
        session.states().subscribe(new Flow.Subscriber<>() {
            Flow.Subscription subscription;
            public void onSubscribe(Flow.Subscription value) { subscription = value; value.request(Long.MAX_VALUE); }
            public void onNext(VaultState state) { if (state.observation() instanceof ObservationProgress.Finished) { finished.complete(state); subscription.cancel(); } }
            public void onError(Throwable error) { finished.completeExceptionally(error); }
            public void onComplete() { }
        });
        return finished.get(10, TimeUnit.SECONDS);
    }
    @Test void frozenCaptureSurvivesMutationsRefreshAndSessionClose() throws Exception {
        restore(directory, "source", "history");
        BackupCapture frozen;
        try (var session = open(directory.resolve("source"))) {
            frozen = session.captureBackup(BackupLimits.DEFAULT);
            try (var setup = SetupUri.parse("otpauth://totp/Test:user?secret=MY".toCharArray())) {
                assertInstanceOf(SaveResult.Saved.class, TokenWrites.save(session.state(), null, setup.transfer(null)));
            }
            session.requestRefresh();
            try (var next = session.captureBackup(BackupLimits.DEFAULT)) { assertEquals(4, next.summary().objects()); }
            assertArrayEquals(fixture("history"), bytes(frozen));
        }
        try (frozen) { assertArrayEquals(fixture("history"), bytes(frozen)); }
    }
    @Test void outputRefusesExistingFilesLinksVaultPathsAndParentAliases() throws Exception {
        restore(directory, "source", "empty"); Path source = directory.resolve("source");
        try (var session = open(source); var capture = session.captureBackup(BackupLimits.DEFAULT)) {
            Path existing = Files.write(directory.resolve("exists.zip"), new byte[] {7});
            Path link = Files.createSymbolicLink(directory.resolve("alias.zip"), source.resolve("vault"));
            for (Path path : List.of(existing, link)) { assertThrows(FileAlreadyExistsException.class, () -> BackupOutput.write(capture, source, path, () -> false, s -> { })); }
            assertThrows(IOException.class, () -> BackupOutput.write(capture, source, source.resolve("bad.zip"), () -> false, s -> { }));
            Path alias = Files.createSymbolicLink(directory.resolve("alias"), source);
            assertThrows(IOException.class, () -> BackupOutput.write(capture, source, alias.resolve("bad.zip"), () -> false, s -> { }));
            Path outerAlias = Files.createSymbolicLink(directory.resolve("outer-alias"), directory);
            assertThrows(IOException.class, () -> BackupOutput.write(capture, source, outerAlias.resolve("bad.zip"), () -> false, s -> { }));
            Path writable = Files.createDirectory(directory.resolve("writable"));
            Files.setPosixFilePermissions(writable, java.nio.file.attribute.PosixFilePermissions.fromString("rwxrwx---"));
            assertThrows(IOException.class, () -> BackupOutput.write(capture, source, writable.resolve("bad.zip"), () -> false, s -> { }));
            assertArrayEquals(new byte[] {7}, Files.readAllBytes(existing)); assertTrue(Files.isSymbolicLink(link));
            assertArrayEquals(entries(fixture("empty")).get("vault"), Files.readAllBytes(source.resolve("vault")));
        }
    }
    @Test void writeCloseAndCapabilityFailuresNeverPublishPartialOutput() throws Exception {
        restore(directory, "source", "empty"); Path source = directory.resolve("source");
        try (var session = open(source); var capture = session.captureBackup(BackupLimits.DEFAULT)) {
            for (String fault : List.of("write", "close", "force", "directory")) {
                Path output = directory.resolve(fault + ".zip");
                var barriers = new BackupOutput.Barriers() {
                    @Override public OutputStream output(FileChannel channel) {
                        return new FilterOutputStream(BackupOutput.Barriers.super.output(channel)) {
                            @Override public void write(byte[] bytes, int offset, int length) throws IOException {
                                if (fault.equals("write")) { out.write(bytes, offset, Math.min(length, 3)); throw new IOException("secret-bearing arbitrary text"); }
                                out.write(bytes, offset, length);
                            }
                            @Override public void close() throws IOException { super.close(); if (fault.equals("close")) { throw new IOException("close failed"); } }
                        };
                    }
                    @Override public void file(FileChannel channel) throws IOException {
                        if (fault.equals("force")) { throw new IOException(); } BackupOutput.Barriers.super.file(channel);
                    }
                    @Override public void directory(Path path) throws IOException {
                        if (fault.equals("directory")) { throw new IOException(); } BackupOutput.Barriers.super.directory(path);
                    }
                };
                assertThrows(IOException.class, () -> BackupOutput.write(capture, source, output, () -> false, s -> { }, barriers));
                assertFalse(Files.exists(output));
            }
        }
    }
    @Test void concurrentFinalWinnerNeverOverwrittenAndLostAckNeverComplete() throws Exception {
        restore(directory, "source", "empty"); Path source = directory.resolve("source");
        try (var session = open(source); var capture = session.captureBackup(BackupLimits.DEFAULT)) {
            Path output = directory.resolve("winner.zip");
            var race = new BackupOutput.Barriers() {
                @Override public void publish(Path target, Path stage) throws IOException {
                    Files.write(target, new byte[] {9}, StandardOpenOption.CREATE_NEW); BackupOutput.Barriers.super.publish(target, stage);
                }
            };
            assertThrows(FileAlreadyExistsException.class, () -> BackupOutput.write(capture, source, output, () -> false, s -> { }, race));
            assertArrayEquals(new byte[] {9}, Files.readAllBytes(output));
            var lost = new BackupOutput.Barriers() {
                @Override public void publish(Path target, Path stage) throws IOException { BackupOutput.Barriers.super.publish(target, stage); throw new IOException(); }
            };
            Path uncertain = directory.resolve("uncertain.zip");
            var receipt = BackupOutput.write(capture, source, uncertain, () -> false, s -> { }, lost);
            assertFalse(receipt.complete()); assertTrue(receipt.publicationMayExist()); assertArrayEquals(fixture("empty"), Files.readAllBytes(uncertain));
        }
    }
    @Test void cancelledOutputNeverPublishesButKeepsSource() throws Exception {
        restore(directory, "source", "empty"); Path source = directory.resolve("source"), output = directory.resolve("cancel.zip");
        try (var session = open(source); var capture = session.captureBackup(BackupLimits.DEFAULT)) {
            boolean[] cancel = {false};
            assertThrows(BackupException.class, () -> BackupOutput.write(capture, source, output, () -> cancel[0], phase -> cancel[0] = true));
            assertFalse(Files.exists(output));
            assertArrayEquals(fixture("empty"), bytes(capture));
        }
    }
    @Test void malformedWrongPasswordAndAuthenticatedInvalidHistoryFailBeforeAllocation() throws Exception {
        assertThrows(BackupException.class, () -> BackupArchives.prepareRestore(new ByteArrayInputStream(new byte[] {1, 2}), BackupLimits.DEFAULT, new char[] {'p'}));
        var wrong = assertThrows(BackupException.class, () -> BackupArchives.prepareRestore(new ByteArrayInputStream(fixture("history")), BackupLimits.DEFAULT, "wrong".toCharArray()));
        assertEquals(BackupException.Reason.AUTHENTICATION_FAILED, wrong.reason());
        Map<String, byte[]> records = entries(fixture("history"));
        records.entrySet().stream().filter(e -> e.getKey().startsWith("objects-v1/")).findFirst().orElseThrow().getValue()[40] ^= 1;
        // Test-only profile framing, never a production encoder. Recompute CRC so structural
        // inspection succeeds and detached object authentication must reject the payload.
        var encoded = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(encoded)) {
            List<String> order = new ArrayList<>(); order.add("totipo-backup.txt"); order.add("vault");
            records.keySet().stream().filter(s -> s.startsWith("objects-v1/")).forEach(order::add);
            for (String name : order) {
                byte[] data = records.get(name); var crc = new CRC32(); crc.update(data);
                var entry = new ZipEntry(name); entry.setMethod(ZipEntry.STORED); entry.setSize(data.length); entry.setCompressedSize(data.length);
                entry.setCrc(crc.getValue()); entry.setTimeLocal(java.time.LocalDateTime.of(1980, 1, 2, 0, 0)); zip.putNextEntry(entry); zip.write(data); zip.closeEntry();
            }
        }
        BackupArchives.inspect(new ByteArrayInputStream(encoded.toByteArray()), BackupLimits.DEFAULT);
        var invalid = assertThrows(BackupException.class, () -> BackupArchives.prepareRestore(new ByteArrayInputStream(encoded.toByteArray()), BackupLimits.DEFAULT, "fixture password".toCharArray()));
        assertEquals(BackupException.Reason.INVALID_OBJECT, invalid.reason());
    }
    @Test void everyExistingDestinationAndConcurrentReservationIsRefused() throws Exception {
        Files.createDirectory(directory.resolve("empty")); Files.write(directory.resolve("file"), new byte[] {3});
        Files.createSymbolicLink(directory.resolve("link"), directory.resolve("empty"));
        try (var plan = plan("empty")) {
            for (String child : List.of("empty", "file", "link")) { assertThrows(FileAlreadyExistsException.class, () -> NioRestore.reserve(directory, child, plan.summary().archiveSha256())); }
            try (var executor = Executors.newFixedThreadPool(2)) {
                CountDownLatch start = new CountDownLatch(1);
                Callable<Boolean> reserve = () -> { start.await(); try (var r = NioRestore.reserve(directory, "race", plan.summary().archiveSha256())) { return r.archiveSha256().equals(plan.summary().archiveSha256()); }
                    catch (FileAlreadyExistsException expected) { return false; } };
                var first = executor.submit(reserve); var second = executor.submit(reserve); start.countDown();
                assertNotEquals(first.get(), second.get());
            }
        }
    }
    @ParameterizedTest @ValueSource(strings = {"allocation", "bootstrap", "object"})
    void interruptedRealNioInstallRetainsAllocationAndExplicitRecoveryCompletes(String boundary) throws Exception {
        Path target = directory.resolve("retained"); String digest;
        try (var plan = plan("history")) {
            digest = plan.summary().archiveSha256();
            var reservation = NioRestore.reserve(directory, "retained", digest, path -> {
                try (var channel = FileChannel.open(path, StandardOpenOption.READ)) { channel.force(true); }
                if (boundary.equals("bootstrap") && Files.exists(target.resolve("vault"))) { Thread.currentThread().interrupt(); }
                if (boundary.equals("object") && path.getFileName().toString().equals("objects-v1")) { Thread.currentThread().interrupt(); }
            });
            if (boundary.equals("allocation")) { Thread.currentThread().interrupt(); }
            try { assertNotEquals(COMPLETE, plan.install(reservation).outcome()); }
            finally { Thread.interrupted(); }
        }
        assertTrue(Files.exists(target.resolve(".totipo-restore-v1")));
        try (var recovered = plan("history")) { assertEquals(COMPLETE, recovered.install(NioRestore.recover(target, digest)).outcome()); }
        try (var session = open(target); var capture = session.captureBackup(BackupLimits.DEFAULT)) { assertArrayEquals(fixture("history"), bytes(capture)); }
    }
    @Test void lostLocationAcknowledgementIsUncertainAndMarkerMismatchIsQuarantined() throws Exception {
        Path target = directory.resolve("retained"); String digest;
        try (var plan = plan("empty")) {
            digest = plan.summary().archiveSha256();
            var result = plan.install(NioRestore.reserve(directory, "retained", digest, path -> { throw new IOException("lost acknowledgement"); }));
            assertEquals(UNCERTAIN, result.outcome()); assertFalse(result.locationAcknowledged());
        }
        assertThrows(IOException.class, () -> NioRestore.recover(target, "0".repeat(64)));
        Files.writeString(target.resolve(".totipo-restore-v1"), "torn");
        assertThrows(IOException.class, () -> NioRestore.recover(target, digest));
        Files.delete(target.resolve(".totipo-restore-v1"));
        assertThrows(IOException.class, () -> NioRestore.recover(target, digest));
        assertTrue(Files.isDirectory(target)); assertFalse(Files.exists(target.resolve("vault")));
    }
    @Test void failuresNeverExposeArbitraryExceptionText() {
        for (Throwable failure : List.of(new IOException("password=secret"), new IllegalStateException("otpauth://secret"), new FileAlreadyExistsException("private-account"))) {
            String message = ArchiveWorkflow.failureMessage(failure);
            assertFalse(message.contains("secret")); assertFalse(message.contains("private-account")); assertFalse(message.contains("password="));
        }
    }
    @Test void capacityFailureNeverReturnsTruncatedHistory() throws Exception {
        restore(directory, "source", "history");
        try (var session = open(directory.resolve("source"))) {
            var failure = assertThrows(BackupException.class, () -> session.captureBackup(new BackupLimits(2, 8192, 6 * 1024 * 1024)));
            assertEquals(BackupException.Reason.CAPACITY, failure.reason());
            try (var capture = session.captureBackup(BackupLimits.DEFAULT)) { assertArrayEquals(fixture("history"), bytes(capture)); }
        }
    }
    @Test void realPartialRestorePreservesOccupiedBytesAndAnOpenedBootstrapDoesNotCompleteIt() throws Exception {
        Path target = directory.resolve("partial"); String digest;
        String object = entries(fixture("history")).keySet().stream().filter(name -> name.startsWith("objects-v1/")).findFirst().orElseThrow();
        try (var plan = plan("history")) {
            digest = plan.summary().archiveSha256();
            var reservation = NioRestore.reserve(directory, "partial", digest);
            Files.createDirectory(target.resolve("objects-v1")); Files.write(target.resolve(object), new byte[] {9});
            var result = plan.install(reservation);
            assertEquals(PARTIAL, result.outcome()); assertEquals(RestoreResult.Bootstrap.ACKNOWLEDGED, result.bootstrap());
            assertArrayEquals(new byte[] {9}, Files.readAllBytes(target.resolve(object)));
            try (var session = open(target)) { assertEquals(plan.summary().vaultId(), session.vaultId()); }
            assertEquals(PARTIAL, result.outcome()); // Opening isn't a restoration receipt.
        }
        try (var recovered = plan("history")) {
            assertEquals(PARTIAL, recovered.install(NioRestore.recover(target, digest)).outcome());
            assertArrayEquals(new byte[] {9}, Files.readAllBytes(target.resolve(object)));
        }
    }
}
