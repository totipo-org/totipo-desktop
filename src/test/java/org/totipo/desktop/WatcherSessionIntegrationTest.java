package org.totipo.desktop;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.totipo.*;
import org.totipo.storage.nio.NioTotipo;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.*;
import static org.totipo.desktop.VaultFolderWatcherTest.eventually;

class WatcherSessionIntegrationTest {
    @TempDir Path directory;
    @Test void watcherAndManualRefreshPreserveUnchangedRevealButExternalAlternativeChangeConceals() throws Exception {
        Path root = Files.createDirectory(directory.resolve("reveal-vault"));
        var session = assertInstanceOf(CreateVaultResult.Created.class, NioTotipo.create(root, new char[]{'p'})).session();
        TokenId a = createToken(session, "Alpha"), b = createToken(session, "Beta");
        eventually(() -> session.state().token(a).isPresent() && session.state().token(b).isPresent());
        var view = onEdt(ControllerRevealObservationTest.BrowserWindow::new);
        var controller = onEdt(() -> new VaultWindowController(session, view, 61, owner -> { }));
        RealSource source = new RealSource(); CountDownLatch revealed = new CountDownLatch(1);
        try {
            edt(() -> {
                assertTrue(controller.start());
                controller.watch(root, (path, callback) -> new VaultFolderWatcher(path, callback,
                        javax.swing.SwingUtilities::invokeLater, source, 100));
            });
            await(source.ready);
            eventually(() -> view.observed != null && view.observed.token(a).isPresent());
            TokenAlternative original = view.observed.token(a).orElseThrow().alternatives().get(0);
            edt(() -> { view.probe.time(7); view.afterDelivery = revealed::countDown; view.probe.reveal(a); });
            await(revealed); edt(() -> view.probe.assertRevealed(a, 23));
            try (var external = assertInstanceOf(OpenResult.Opened.class, NioTotipo.open(root, new char[]{'p'})).session()) {
                eventually(() -> external.state().token(b).isPresent());
                try (var edit = external.state().update(external.state().token(b).orElseThrow().alternatives().get(0))) {
                    assertInstanceOf(SaveResult.Saved.class, edit.issuer("Changed Beta").save());
                }
                eventually(() -> view.observed.token(b).orElseThrow().alternatives().get(0).descriptor().issuer().equals("Changed Beta"));
                TokenAlternative observed = view.observed.token(a).orElseThrow().alternatives().get(0);
                assertNotSame(original, observed); assertEquals(original, observed); assertEquals(original.hashCode(), observed.hashCode());
                edt(() -> { view.probe.time(11); view.probe.assertRevealed(a, 19); view.probe.copyAndAssertVisible(); });
                VaultState beforeRefresh = view.observed;
                edt(view.refresh);
                eventually(() -> view.observed != beforeRefresh); // Wait for a new emission, not a relevance criterion.
                edt(() -> view.probe.assertRevealed(a, 19));
                try (var edit = external.state().update(external.state().token(a).orElseThrow().alternatives().get(0))) {
                    assertInstanceOf(SaveResult.Saved.class, edit.issuer("Changed Alpha").save());
                }
                eventually(() -> view.observed.token(a).orElseThrow().alternatives().get(0).descriptor().issuer().equals("Changed Alpha"));
                edt(() -> view.probe.assertConcealed(a));
            }
            edt(() -> { controller.close(); view.probe.assertRetired(); });
        } finally { edt(controller::close); await(view.disposed); }
        assertThrows(ClosedWatchServiceException.class, source.service::poll);
    }
    private static TokenId createToken(VaultSession session, String issuer) {
        try (var secret = NewSecret.copyOf(new byte[]{1, 2, 3}); var builder = session.state().createToken()) {
            return assertInstanceOf(SaveResult.Saved.class, builder.issuer(issuer).secret(secret).save()).tokenId();
        }
    }
    static final class RealSource implements VaultFolderWatcher.Source {
        final CountDownLatch ready = new CountDownLatch(1);
        volatile WatchService service;
        public WatchService open() throws IOException { service = FileSystems.getDefault().newWatchService(); return service; }
        public WatchKey register(Path path, WatchService watcher) throws IOException {
            WatchKey key = path.register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
            if (path.endsWith("objects-v1")) { ready.countDown(); }
            return key;
        }
    }
    @Test void independentNioPublisherBurstBecomesVisibleThroughControllerWithoutManualRefreshOrWrites() throws Exception {
        Path root = Files.createDirectory(directory.resolve("vault"));
        System.out.println("Watcher filesystem qualification: os=" + System.getProperty("os.name")
                + "; runtime=" + System.getProperty("java.runtime.version")
                + "; filesystem=" + Files.getFileStore(root).type()
                + "; POSIX=" + Files.getFileStore(root).supportsFileAttributeView("posix"));
        var opened = assertInstanceOf(CreateVaultResult.Created.class, NioTotipo.create(root, new char[]{'p'})).session();
        Files.createDirectories(root.resolve("objects-v1"));
        AtomicReference<VaultState> latest = new AtomicReference<>(); AtomicInteger refreshes = new AtomicInteger();
        Window view = new Window() {
            @Override public void render(VaultState state) { super.render(state); latest.set(state); }
        };
        var controller = onEdt(() -> new VaultWindowController(opened, view, 1, owner -> { }));
        RealSource source = new RealSource(); AtomicReference<VaultFolderWatcher> watcher = new AtomicReference<>();
        try {
            edt(() -> {
                assertTrue(controller.start());
                controller.watch(root, (path, callback) -> {
                    var value = new VaultFolderWatcher(path, () -> { refreshes.incrementAndGet(); callback.run(); },
                            javax.swing.SwingUtilities::invokeLater, source, 200);
                    watcher.set(value); return value;
                });
            });
            await(source.ready);
            List<TokenId> ids = new ArrayList<>();
            try (var external = assertInstanceOf(OpenResult.Opened.class, NioTotipo.open(root, new char[]{'p'})).session()) {
                for (int i = 0; i < 5; i++) {
                    try (var secret = NewSecret.copyOf(new byte[]{1, 2, 3}); var builder = external.state().createToken()) {
                        ids.add(assertInstanceOf(SaveResult.Saved.class, builder.issuer("External").account("burst " + i).secret(secret).save()).tokenId());
                    }
                }
            }
            Map<Path, List<Byte>> evidence = snapshot(root);
            eventually(() -> latest.get() != null && ids.stream().allMatch(id -> latest.get().token(id).isPresent()));
            assertTrue(refreshes.get() > 0); assertTrue(refreshes.get() < 10, "Notifications must remain bounded across a small publication burst");
            assertEquals(evidence, snapshot(root), "Observation must not write namespace contents");
            edt(view.refresh); // Manual refresh continues to use the same controller operation.
        } finally { edt(controller::close); await(view.disposed); }
        eventually(() -> watcher.get().stopped());
        assertThrows(ClosedWatchServiceException.class, source.service::poll);
    }
    static Map<Path, List<Byte>> snapshot(Path root) throws IOException {
        Map<Path, List<Byte>> result = new HashMap<>();
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                List<Byte> bytes = new ArrayList<>(); for (byte value : Files.readAllBytes(file)) { bytes.add(value); }
                result.put(root.relativize(file), bytes);
            }
        }
        return result;
    }
    @Test void retiredOwnerRejectsQueuedEventAndReplacementSessionIsIndependent() throws Exception {
        Files.createDirectory(directory.resolve("objects-v1"));
        Session a = new Session(), b = new Session(); Window av = new Window(), bv = new Window();
        var first = onEdt(() -> new VaultWindowController(a, av, 1, owner -> { }));
        var second = onEdt(() -> new VaultWindowController(b, bv, 2, owner -> { }));
        var source = new VaultFolderWatcherTest.Source(); BlockingQueue<Runnable> delayed = new LinkedBlockingQueue<>();
        AtomicReference<VaultFolderWatcher> watcher = new AtomicReference<>();
        try {
            edt(() -> {
                first.watch(directory, (path, callback) -> { fail("Not started"); return null; });
                assertTrue(first.start());
                first.watch(directory, (path, callback) -> {
                    var value = new VaultFolderWatcher(path, callback, delayed::add, source, 100); watcher.set(value); return value;
                });
            });
            source.registered(); var objects = source.registered(); source.event(objects, ENTRY_CREATE, "child");
            Runnable late = Objects.requireNonNull(delayed.poll(10, TimeUnit.SECONDS));
            Runnable staleControllerCallback = av.refresh;
            edt(first::close); await(av.disposed); assertTrue(source.closed);
            edt(() -> { assertTrue(second.start()); late.run(); staleControllerCallback.run(); });
            assertEquals(0, a.refreshes.get()); assertEquals(0, b.refreshes.get());
            edt(bv.refresh); assertEquals(1, b.refreshes.get());
        } finally { edt(first::close); edt(second::close); await(bv.disposed); }
        eventually(() -> watcher.get().stopped());
    }
    @Test void applicationLockChangeVaultReopenAndExitOwnWatcherLifetime() throws Exception {
        Path root = Files.createDirectory(directory.resolve("a"));
        Files.writeString(root.resolve("vault"), "TOTIPO-VLT"); Files.createDirectory(root.resolve("objects-v1"));
        var store = new RememberedVaultTest.Store(root); var sessions = new ArrayList<Session>(); Shell shell = new Shell();
        VaultAccess access = new VaultAccess() {
            public OpenResult open(Path path, char[] password) { var session = new Session(); sessions.add(session); return new OpenResult.Opened(session); }
            public CreateVaultResult create(Path path, char[] password) { throw new AssertionError(); }
        };
        var app = onEdt(() -> new DesktopApplication(access, shell, path -> new Window(), new org.totipo.desktop.clipboard.TotpClipboard(), store));
        try {
            edt(app::show); await(shell.ready); assertEquals(0, watchThreads());
            for (int step = 0; step < 3; step++) {
                edt(() -> { shell.ready = new CountDownLatch(1); shell.submit.accept(new char[]{'p'}); }); await(shell.ready);
                eventually(() -> watchThreads() == 1);
                Session current = sessions.get(step);
                Path child = root.resolve("objects-v1/external");
                eventually(() -> { try { Files.writeString(child, "external"); } catch (IOException e) { throw new AssertionError(e); }
                    return current.refreshes.get() > 0; });
                if (step == 0) { edt(app::lock); }
                else if (step == 1) { shell.directory = null; edt(app::changeVault); }
                else { edt(app::shutdown); }
                await(current.closeEntered); eventually(() -> watchThreads() == 0);
                eventually(() -> current.closes.get() == 1);
                if (step < 2) { eventually(() -> { try { return onEdt(() -> !shell.busy); } catch (Exception e) { throw new AssertionError(e); } }); }
            }
        } finally { edt(app::shutdown); await(shell.disposed); }
    }
    static long watchThreads() {
        return Thread.getAllStackTraces().keySet().stream().filter(t -> t.isAlive() && t.getName().equals("totipo-vault-watch")).peek(t -> assertTrue(t.isDaemon())).count();
    }
}
