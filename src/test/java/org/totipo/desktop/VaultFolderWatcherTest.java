package org.totipo.desktop;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.*;

class VaultFolderWatcherTest {
    @TempDir Path root;
    static final class Key implements WatchKey {
        final Path path;
        final List<WatchEvent<?>> events = new ArrayList<>();
        volatile boolean valid = true;
        Key(Path path) { this.path = path; }
        public boolean isValid() { return valid; }
        public synchronized List<WatchEvent<?>> pollEvents() { var result = List.copyOf(events); events.clear(); return result; }
        public boolean reset() { return valid; }
        public void cancel() { valid = false; }
        public Watchable watchable() { return path; }
    }
    static final class Source implements VaultFolderWatcher.Source, WatchService {
        final BlockingQueue<WatchKey> queue = new LinkedBlockingQueue<>();
        final BlockingQueue<Key> registrations = new LinkedBlockingQueue<>();
        volatile boolean closed;
        boolean fail;
        public WatchService open() throws IOException { if (fail) { throw new IOException("redact"); } return this; }
        public WatchKey register(Path path, WatchService service) {
            Key key = new Key(path); registrations.add(key); return key;
        }
        Key registered() throws Exception { return Objects.requireNonNull(registrations.poll(10, TimeUnit.SECONDS)); }
        void event(Key key, WatchEvent.Kind<Path> kind, String name) {
            synchronized (key) { key.events.add(new WatchEvent<Path>() {
                public Kind<Path> kind() { return kind; }
                public int count() { return 1; }
                public Path context() { return Path.of(name); }
            }); }
            queue.add(key);
        }
        void overflow(Key key) {
            synchronized (key) { key.events.add(new WatchEvent<Object>() {
                public Kind<Object> kind() { return OVERFLOW; }
                public int count() { return 1; }
                public Object context() { return null; }
            }); } queue.add(key);
        }
        public WatchKey poll() { return queue.poll(); }
        public WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException { return queue.poll(timeout, unit); }
        public WatchKey take() throws InterruptedException { return queue.take(); }
        public void close() { closed = true; }
    }
    static void eventually(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        do {
            if (condition.getAsBoolean()) { return; }
            Thread.sleep(10);
        } while (System.nanoTime() < deadline);
        fail("Timed out waiting for filesystem/lifecycle event");
    }
    @Test void burstHasOnePendingDeliveryLaterBurstRefreshesAndOverflowReregisters() throws Exception {
        Files.createDirectory(root.resolve("objects-v1")); Source source = new Source();
        BlockingQueue<Runnable> delivery = new LinkedBlockingQueue<>(); AtomicInteger refreshes = new AtomicInteger();
        try (var watcher = new VaultFolderWatcher(root, refreshes::incrementAndGet, delivery::add, source, 100)) {
            assertFalse(watcher.stopped());
            Key rootKey = source.registered(), objects = source.registered();
            source.event(objects, ENTRY_CREATE, "anything");
            Runnable first = Objects.requireNonNull(delivery.poll(10, TimeUnit.SECONDS));
            // A stalled EDT cannot accumulate callbacks, even across more event batches.
            source.event(objects, ENTRY_MODIFY, "anything"); source.event(objects, ENTRY_DELETE, "anything");
            assertNull(delivery.poll(250, TimeUnit.MILLISECONDS)); first.run(); assertEquals(1, refreshes.get());
            source.event(objects, ENTRY_CREATE, "later");
            Objects.requireNonNull(delivery.poll(10, TimeUnit.SECONDS)).run(); assertEquals(2, refreshes.get());
            source.overflow(rootKey); Key replacement = source.registered(); assertFalse(objects.valid); assertTrue(replacement.valid);
            Objects.requireNonNull(delivery.poll(10, TimeUnit.SECONDS)).run(); assertEquals(3, refreshes.get());
        }
        assertTrue(source.closed);
    }
    @Test void absentObjectsAppearsReplacementAndInvalidKeyWaitForRoot() throws Exception {
        Source source = new Source(); BlockingQueue<Runnable> delivery = new LinkedBlockingQueue<>();
        try (var watcher = new VaultFolderWatcher(root, () -> { }, delivery::add, source, 100)) {
            assertFalse(watcher.stopped());
            Key rootKey = source.registered(); assertNull(source.registrations.poll(150, TimeUnit.MILLISECONDS));
            Files.createDirectory(root.resolve("objects-v1")); source.event(rootKey, ENTRY_CREATE, "objects-v1");
            Key first = source.registered(); Objects.requireNonNull(delivery.poll(10, TimeUnit.SECONDS)).run();
            first.valid = false; source.event(first, ENTRY_DELETE, "child");
            Objects.requireNonNull(delivery.poll(10, TimeUnit.SECONDS)).run();
            assertNull(source.registrations.poll(150, TimeUnit.MILLISECONDS));
            source.event(rootKey, ENTRY_CREATE, "objects-v1"); Key second = source.registered(); assertNotSame(first, second);
            Objects.requireNonNull(delivery.poll(10, TimeUnit.SECONDS)).run();
            source.event(second, ENTRY_MODIFY, "child"); Objects.requireNonNull(delivery.poll(10, TimeUnit.SECONDS)).run();
        }
    }
    @Test void closeIsIdempotentRejectsDelayedDeliveryAndReleasesDaemon() throws Exception {
        Files.createDirectory(root.resolve("objects-v1")); Source source = new Source();
        BlockingQueue<Runnable> delivery = new LinkedBlockingQueue<>(); AtomicInteger refreshes = new AtomicInteger();
        var watcher = new VaultFolderWatcher(root, refreshes::incrementAndGet, delivery::add, source, 100);
        source.registered(); Key objects = source.registered(); source.event(objects, ENTRY_CREATE, "child");
        Runnable delayed = Objects.requireNonNull(delivery.poll(10, TimeUnit.SECONDS));
        watcher.close(); watcher.close(); delayed.run();
        eventually(watcher::stopped); assertTrue(source.closed); assertEquals(0, refreshes.get());
    }
    @Test void failureDoesNotRetireControllerOrDisableManualRefresh() throws Exception {
        Session session = new Session(); Window view = new Window();
        var controller = onEdt(() -> new VaultWindowController(session, view, 1, owner -> { }));
        var watcher = new java.util.concurrent.atomic.AtomicReference<VaultFolderWatcher>();
        try {
            edt(() -> {
                assertTrue(controller.start());
                controller.watch(root.resolve("missing"), (path, callback) -> {
                    var value = new VaultFolderWatcher(path, callback, javax.swing.SwingUtilities::invokeLater);
                    watcher.set(value); return value;
                });
            });
            eventually(() -> watcher.get().stopped());
            edt(view.refresh); assertEquals(1, session.refreshes.get()); assertFalse(view.closing);
        } finally { edt(controller::close); await(view.disposed); }
    }
    @Test void serviceCreationFailureAndFactoryFailureKeepManualRefreshUsable() throws Exception {
        Session session = new Session(); Window view = new Window(); Source source = new Source(); source.fail = true;
        var watcher = new java.util.concurrent.atomic.AtomicReference<VaultFolderWatcher>();
        var controller = onEdt(() -> new VaultWindowController(session, view, 1, owner -> { }));
        try {
            edt(() -> {
                assertTrue(controller.start());
                controller.watch(root, (path, callback) -> { throw new UnsupportedOperationException("redacted"); });
                view.refresh.run();
                controller.watch(root, (path, callback) -> {
                    var value = new VaultFolderWatcher(path, callback, javax.swing.SwingUtilities::invokeLater, source, 100);
                    watcher.set(value); return value;
                });
            });
            eventually(() -> watcher.get().stopped());
            edt(view.refresh); assertEquals(2, session.refreshes.get()); assertFalse(view.closing);
        } finally { edt(controller::close); await(view.disposed); }
    }
    @Test void rootVaultEventsDoNotAdoptIdentityOrRequestRefresh() throws Exception {
        Source source = new Source(); BlockingQueue<Runnable> delivery = new LinkedBlockingQueue<>();
        try (var watcher = new VaultFolderWatcher(root, () -> fail("VAULT is immutable"), delivery::add, source, 100)) {
            assertFalse(watcher.stopped());
            Key key = source.registered(); source.event(key, ENTRY_MODIFY, "vault");
            assertNull(delivery.poll(250, TimeUnit.MILLISECONDS)); assertTrue(source.registrations.isEmpty());
        }
    }
    @Test void unsuccessfulSessionStartupNeverStartsWatching() throws Exception {
        Session session = new Session(); session.failSubscribe = true; Window view = new Window();
        var controller = onEdt(() -> new VaultWindowController(session, view, 1, owner -> { }));
        edt(() -> {
            assertFalse(controller.start());
            controller.watch(root, (path, callback) -> { fail("Failed session must not watch"); return null; });
        });
        await(view.disposed);
    }
    @Test void realFilesystemCreateModifyDeleteAndDirectoryReplacement() throws Exception {
        Path objects = Files.createDirectory(root.resolve("objects-v1"));
        BlockingQueue<Boolean> refreshes = new LinkedBlockingQueue<>();
        var watcher = new VaultFolderWatcher(root, () -> refreshes.add(true), Runnable::run);
        try {
            // Registration is asynchronous; an initial external event may race startup.
            Path child = objects.resolve("external");
            eventually(() -> { try { Files.writeString(child, "test"); } catch (IOException e) { throw new AssertionError(e); }
                return refreshes.poll() != null; });
            Files.writeString(child, "changed"); assertNotNull(refreshes.poll(10, TimeUnit.SECONDS));
            Files.delete(child); assertNotNull(refreshes.poll(10, TimeUnit.SECONDS));
            Files.delete(objects); assertNotNull(refreshes.poll(10, TimeUnit.SECONDS));
            Files.createDirectory(objects); assertNotNull(refreshes.poll(10, TimeUnit.SECONDS));
            Files.writeString(child, "recreated"); assertNotNull(refreshes.poll(10, TimeUnit.SECONDS));
            assertEquals("recreated", Files.readString(child));
            try (var files = Files.list(objects)) { assertEquals(List.of(child), files.toList()); }
        } finally { watcher.close(); }
        eventually(watcher::stopped);
    }
}
