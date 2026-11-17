package org.totipo.desktop;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.totipo.*;
import org.totipo.backup.*;
import org.totipo.spi.*;
import org.totipo.storage.nio.*;
import java.io.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.BackupRestoreTest.*;
import static org.totipo.desktop.HistoryImportTest.*;

/** Test-only fault boundary around real NIO. Every provider call shares the same root monitor;
 * no session method is called while holding it. Production uses ordinary NioTotipo only. */
@Timeout(45)
class HistoryImportFaultTest {
    @TempDir Path directory;
    static final class Store implements TotipoStore {
        final TotipoStore delegate;
        final Path root;
        int calls;
        boolean fault = true;
        String mode;
        final java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        Store(Path root, String mode) throws IOException {
            this.root = root; this.mode = mode;
            delegate = NioStoreComposition.coordinatedDelegate(root, new NioDurability());
        }
        public synchronized BoundedRead readVault(int n) { return delegate.readVault(n); }
        public synchronized BoundedRead readObject(ObjectName name, int n) { return delegate.readObject(name, n); }
        public synchronized ObjectScan scanObjects() { return delegate.scanObjects(); }
        public synchronized ObjectWrite publishObject(ObjectName name, byte[] bytes) {
            calls++;
            if (mode.equals("blocked") && calls == 1) { entered.countDown(); TestSupport.await(release); }
            if (fault && calls == 2 && mode.equals("partial")) { return new ObjectWrite.Failed(StoreFailure.UNAVAILABLE); }
            if (fault && calls == 2 && mode.equals("winner")) {
                try { Files.write(root.resolve("objects-v1").resolve(name.value()), new byte[] {4, 3, 2}, StandardOpenOption.CREATE_NEW); }
                catch (IOException failure) { throw new UncheckedIOException(failure); }
            }
            var result = delegate.publishObject(name, bytes);
            if (fault && calls == 2 && mode.equals("uncertain")) { return new ObjectWrite.Uncertain(StoreFailure.UNAVAILABLE); }
            if (fault && calls == 1 && mode.equals("cancel")) { Thread.currentThread().interrupt(); }
            return result;
        }
        public synchronized VaultCreate createVault(byte[] bytes) { return delegate.createVault(bytes); }
        public synchronized void close() { delegate.close(); }
    }
    @ParameterizedTest @ValueSource(strings = {"partial", "uncertain", "cancel", "winner"})
    void actualNioPrefixesSurviveIncompleteOutcomesAndExactRetry(String mode) throws Exception {
        restore(directory, "target", "empty"); Path root = directory.resolve("target"); var before = snapshot(root);
        var store = new Store(root, mode);
        try (var session = assertInstanceOf(OpenResult.Opened.class, Totipo.open(store, "fixture password".toCharArray())).session();
             var plan = prepare(session, fixture("history"))) {
            ImportResult result;
            try { result = plan.apply(); } finally { Thread.interrupted(); }
            assertEquals(mode.equals("uncertain") ? ImportResult.Outcome.UNCERTAIN : ImportResult.Outcome.PARTIAL, result.outcome());
            assertEquals(1, result.installed()); assertEquals(mode.equals("uncertain") ? 1 : 0, result.uncertain());
            assertEquals(mode.equals("cancel") ? ImportResult.Reason.CANCELLED : mode.equals("winner") ? ImportResult.Reason.TARGET_CONFLICT : ImportResult.Reason.STORAGE, result.reason());
            var prefix = snapshot(root); preserved(before);
            synchronized (store) { store.fault = false; }
            if (mode.equals("winner")) {
                assertEquals(ImportResult.Outcome.PARTIAL, plan.apply().outcome()); // No repair, even on retry.
            } else {
                var retry = plan.apply(); assertEquals(ImportResult.Outcome.COMPLETE, retry.outcome()); assertEquals(0, retry.uncertain());
                assertEquals(mode.equals("uncertain") ? 1 : 0, retry.retained());
            }
            preserved(prefix); preserved(before);
        }
    }
    @Test void uncertainAcknowledgementCannotBeRecoveredByPreflightRead() throws Exception {
        restore(directory, "target", "empty"); Path root = directory.resolve("target"); var store = new Store(root, "uncertain");
        try (var session = assertInstanceOf(OpenResult.Opened.class, Totipo.open(store, "fixture password".toCharArray())).session();
             var plan = prepare(session, fixture("history"))) {
            assertEquals(ImportResult.Outcome.UNCERTAIN, plan.apply().outcome()); var before = snapshot(root);
            Thread.currentThread().interrupt();
            try { assertEquals(ImportResult.Outcome.UNCERTAIN, plan.apply().outcome()); }
            finally { Thread.interrupted(); }
            preserved(before);
        }
    }
    @Test void actualSessionCloseWaitsForEnteredNioPublicationAndStopsRemainingObjects() throws Exception {
        restore(directory, "target", "empty"); Path root = directory.resolve("target"); var before = snapshot(root);
        var store = new Store(root, "blocked");
        var session = assertInstanceOf(OpenResult.Opened.class, Totipo.open(store, "fixture password".toCharArray())).session();
        try (var plan = prepare(session, fixture("history")); var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var applying = executor.submit(plan::apply); TestSupport.await(store.entered);
            try {
                var closing = executor.submit(session::close);
                VaultFolderWatcherTest.eventually(() -> {
                    try (var builder = session.state().createToken()) { builder.issuer("Not saved"); return false; }
                    catch (SessionClosedException expected) { return true; }
                });
                assertFalse(closing.isDone()); store.release.countDown();
                var result = applying.get(10, java.util.concurrent.TimeUnit.SECONDS);
                assertEquals(ImportResult.Outcome.PARTIAL, result.outcome()); assertEquals(1, result.installed());
                assertEquals(ImportResult.Reason.SESSION_CLOSING, result.reason());
                closing.get(10, java.util.concurrent.TimeUnit.SECONDS); preserved(before);
                assertThrows(SessionClosedException.class, plan::apply);
            } finally { store.release.countDown(); }
        } finally { store.release.countDown(); session.close(); }
    }
}
