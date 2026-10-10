package org.totipo.desktop;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import static java.nio.file.StandardWatchEventKinds.*;

/** Advisory namespace notifications only: never reads objects or interprets their names.
 * WatchService can coalesce/lose events, differs by provider, and may overflow. It is
 * not evidence of synchronization completion. Manual refresh remains available.
 */
final class VaultFolderWatcher implements AutoCloseable {
    interface Source {
        WatchService open() throws IOException;
        WatchKey register(Path path, WatchService service) throws IOException;
    }
    private static final Source NIO = new Source() {
        public WatchService open() throws IOException { return FileSystems.getDefault().newWatchService(); }
        public WatchKey register(Path path, WatchService service) throws IOException {
            return path.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        }
    };
    private final Path root;
    private volatile Runnable refresh;
    private final Executor delivery;
    private final Source source;
    private final long debounceNanos;
    private final Thread thread;
    private volatile boolean closed;
    private final java.util.concurrent.atomic.AtomicBoolean deliveryPending = new java.util.concurrent.atomic.AtomicBoolean();
    private WatchService service;

    VaultFolderWatcher(Path root, Runnable refresh, Executor delivery) {
        this(root, refresh, delivery, NIO, 200);
    }
    VaultFolderWatcher(Path root, Runnable refresh, Executor delivery, Source source, long debounceMillis) {
        this.root = root; this.refresh = refresh; this.delivery = delivery; this.source = source;
        debounceNanos = TimeUnit.MILLISECONDS.toNanos(debounceMillis);
        thread = new Thread(this::run, "totipo-vault-watch"); thread.setDaemon(true); thread.start();
    }
    private void run() {
        try {
            WatchService opened = source.open();
            synchronized (this) {
                if (closed) { opened.close(); return; }
                service = opened;
            }
            WatchKey rootKey = source.register(root, opened);
            WatchKey objectsKey = registerObjects(opened);
            long due = 0;
            while (!closed) {
                // Block on notifications, with a single deadline only while a burst is pending.
                // This is not a filesystem polling/retry loop and accumulates no scheduled tasks.
                WatchKey key = due == 0 ? opened.take()
                        : opened.poll(Math.max(0, due - System.nanoTime()), TimeUnit.NANOSECONDS);
                if (key != null) {
                    boolean relevant = false, registration = false;
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == OVERFLOW) { relevant = true; registration = true; }
                        else if (key == objectsKey) { relevant = true; }
                        else if (key == rootKey && Path.of("objects-v1").equals(event.context())) {
                            relevant = true; registration = true;
                        }
                    }
                    if (!key.reset()) {
                        if (key == rootKey) { return; }
                        if (key == objectsKey) { objectsKey = null; relevant = true; }
                    }
                    if (registration) {
                        if (objectsKey != null) { objectsKey.cancel(); }
                        objectsKey = registerObjects(opened);
                    }
                    if (relevant && due == 0) { due = System.nanoTime() + debounceNanos; }
                }
                if (due != 0 && System.nanoTime() >= due) {
                    due = 0;
                    if (deliveryPending.compareAndSet(false, true)) {
                        delivery.execute(() -> {
                            try {
                                Runnable action = refresh;
                                if (!closed && action != null) { action.run(); }
                            }
                            finally { deliveryPending.set(false); }
                        });
                    }
                }
            }
        } catch (IOException | RuntimeException unavailable) {
            if (!closed) { System.err.println("Totipo: automatic refresh unavailable; use manual Refresh (details redacted)."); }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally { close(); }
    }
    private WatchKey registerObjects(WatchService opened) {
        Path objects = root.resolve("objects-v1");
        if (!Files.isDirectory(objects, LinkOption.NOFOLLOW_LINKS)) { return null; }
        try { return source.register(objects, opened); }
        catch (IOException | RuntimeException unavailable) { return null; }
    }
    @Override public synchronized void close() {
        if (closed) { return; }
        closed = true;
        refresh = null; // Queued deliveries must not retain the retired controller/session.
        if (service != null) {
            try { service.close(); }
            catch (IOException | RuntimeException unavailable) { /* Best effort; daemon lifetime cannot hold exit. */ }
            service = null;
        }
        thread.interrupt();
    }
    boolean stopped() { return !thread.isAlive(); }
}
