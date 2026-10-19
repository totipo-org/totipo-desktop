package org.totipo.desktop;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.totipo.*;
import org.totipo.desktop.clipboard.TotpClipboard;
import org.totipo.desktop.ui.VaultSelection;
import static org.totipo.desktop.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class MultiVaultLifecycleTest {
    @TempDir Path directory;
    final class Fixture implements AutoCloseable {
        final Path a = vault("a"), b = vault("b");
        final Preferences node = Preferences.userRoot().node("/org/totipo/mv1-tests/" + java.util.UUID.randomUUID());
        final JdkVaultPreferences preferences = new JdkVaultPreferences(node);
        final Shell shell = new Shell();
        final List<Session> sessions = new ArrayList<>();
        final List<TokenRevealExecutionTest.RevealWindow> windows = new ArrayList<>();
        final CountDownLatch releaseClose = new CountDownLatch(1);
        final DesktopApplication app;
        boolean failOpen;
        CreateVaultResult creation = new CreateVaultResult.Failed();
        boolean throwCreate;
        final CountDownLatch createEntered = new CountDownLatch(1);
        CountDownLatch allowCreate = new CountDownLatch(0);
        Fixture() throws Exception { this(false); }
        Fixture(boolean noSelection) throws Exception {
            var registry = new VaultRegistry(preferences); registry.select(b); registry.select(a);
            if (noSelection) { preferences.saveRegistry(new VaultRegistry.Snapshot(List.of(b, a), null)); }
            app = onEdt(() -> new DesktopApplication(new VaultAccess() {
                public OpenResult open(Path path, char[] password) {
                    assertFalse(javax.swing.SwingUtilities.isEventDispatchThread());
                    for (Session old : sessions) { assertEquals(1, old.closes.get()); assertEquals(0, old.allowClose.getCount()); }
                    if (failOpen) { return new OpenResult.AuthenticationFailed(); }
                    Session session = new Session(sessions.isEmpty() ? releaseClose : new CountDownLatch(0));
                    sessions.add(session); return new OpenResult.Opened(session);
                }
                public CreateVaultResult create(Path path, char[] password) {
                    createEntered.countDown(); await(allowCreate);
                    if (throwCreate) { throw new IllegalStateException(); }
                    return creation;
                }
            }, shell, path -> { var view = new TokenRevealExecutionTest.RevealWindow(); windows.add(view); return view; }, new TotpClipboard(), preferences));
            edt(app::show); await(shell.ready);
        }
        void action(Runnable action) throws Exception {
            edt(() -> { shell.ready = new CountDownLatch(1); action.run(); }); await(shell.ready);
        }
        void choice(VaultSelection.Action action, Path path) throws Exception {
            action(() -> shell.selectionAction.accept(new VaultSelection(action, path)));
        }
        void select(Path path) throws Exception { choice(VaultSelection.Action.SELECT, path); }
        void open() throws Exception {
            if (onEdt(app::state) == ShellState.VAULT_SELECTION) { select(onEdt(app::selectedVault)); }
            action(() -> shell.submit.accept(new char[] {'p'}));
        }
        void switchTo(Path target) throws Exception {
            action(app::changeVault);
            if (target == null) { choice(VaultSelection.Action.RETURN, null); }
            else { select(target); }
        }
        public void close() {
            releaseClose.countDown(); allowCreate.countDown();
            try { edt(app::shutdown); await(shell.disposed); node.removeNode(); }
            catch (Exception failure) { throw new AssertionError(failure); }
        }
    }
    Path vault(String name) throws Exception {
        Path path = Files.createDirectory(directory.resolve(name)); Files.writeString(path.resolve("vault"), "TOTIPO-VLT"); return path;
    }
    @Test void multipleVaultsStartInSelectionHighlightLastWithoutProbingOrOpening() throws Exception {
        try (var f = new Fixture()) {
            edt(() -> { assertEquals(ShellState.VAULT_SELECTION, f.app.state()); assertEquals(f.a, f.shell.highlighted);
                assertEquals(List.of(f.b, f.a), f.shell.remembered); assertFalse(f.shell.canReturn); assertFalse(f.shell.busy); });
            assertTrue(f.sessions.isEmpty()); assertTrue(f.windows.isEmpty());
            edt(() -> f.shell.selectionAction.accept(new VaultSelection(VaultSelection.Action.RETURN, null)));
            edt(() -> assertEquals(ShellState.VAULT_SELECTION, f.app.state()));
        }
    }
    @Test void startupWithoutLastSelectionDoesNotPersistAVisualDefault() throws Exception {
        try (var f = new Fixture(true)) {
            edt(() -> { assertEquals(ShellState.VAULT_SELECTION, f.app.state()); assertNull(f.shell.highlighted); assertFalse(f.shell.canReturn); });
            assertTrue(new VaultRegistry(f.preferences).selected().isEmpty());
            f.select(f.b);
            edt(() -> assertEquals(ShellState.LOCKED, f.app.state()));
            assertEquals(f.b, new VaultRegistry(f.preferences).selected().orElseThrow()); assertTrue(f.sessions.isEmpty());
        }
    }
    @Test void soleRegisteredVaultWithoutLastSelectionStillStartsLockedAndChangeEntersSelection() throws Exception {
        Path a = vault("sole"); Shell shell = new Shell();
        VaultPreferences preferences = new VaultPreferences() {
            public java.util.Optional<Path> lastVault() { return java.util.Optional.empty(); }
            public void setLastVault(Path path) { fail("Startup must not change persisted selection"); }
            public void clearLastVault() { fail("Startup must not change persisted selection"); }
            public VaultRegistry.Snapshot loadRegistry() { return new VaultRegistry.Snapshot(List.of(a), null); }
        };
        VaultAccess access = new VaultAccess() {
            public OpenResult open(Path path, char[] password) { throw new AssertionError("No implicit authentication"); }
            public CreateVaultResult create(Path path, char[] password) { throw new AssertionError(); }
        };
        var app = onEdt(() -> new DesktopApplication(access, shell, path -> new Window(), new TotpClipboard(), preferences));
        try {
            edt(app::show); await(shell.ready);
            edt(() -> { assertEquals(ShellState.LOCKED, app.state()); assertEquals(a, app.selectedVault());
                app.changeVault(); assertEquals(ShellState.VAULT_SELECTION, app.state()); assertTrue(shell.canReturn); });
        } finally { edt(app::shutdown); await(shell.disposed); }
    }
    @Test void switchingWaitsForFullCloseThenRequiresIndependentPasswordAndRejectsLateA() throws Exception {
        try (var f = new Fixture()) {
            f.open(); Session a = f.sessions.getFirst(); Window old = f.windows.getFirst();
            edt(() -> { f.shell.ready = new CountDownLatch(1); old.changeVault.run();
                assertTrue(old.closing); assertEquals(ShellState.LOCKED, f.app.state()); });
            await(a.closeEntered);
            edt(() -> { assertEquals(1, f.shell.selectorVisits); char[] rejected = {'x'};
                f.app.open(rejected); assertArrayEquals(new char[1], rejected);
                f.shell.selectionAction.accept(VaultSelection.select(f.b)); assertEquals(f.a, f.app.selectedVault()); });
            f.releaseClose.countDown(); await(f.shell.ready);
            edt(() -> { assertEquals(ShellState.VAULT_SELECTION, f.app.state()); assertTrue(f.shell.canReturn); });
            a.subscriber.onNext(state(new ObservationProgress.Enumerating(98)));
            a.subscriber.onError(new IllegalStateException());
            edt(() -> { old.changeVault.run(); assertEquals(ShellState.VAULT_SELECTION, f.app.state()); });
            f.select(f.b); assertEquals(1, f.sessions.size()); f.open();
            a.subscriber.onNext(state(new ObservationProgress.Enumerating(99)));
            edt(() -> { old.changeVault.run(); assertEquals(f.b, f.app.selectedVault());
                assertEquals(ShellState.UNLOCKED, f.app.state()); assertTrue(f.windows.getLast().rendered.isEmpty()); });
            for (int i = 0; i < 4; i++) { f.switchTo(i % 2 == 0 ? f.a : f.b); f.open(); }
            assertEquals(6, f.sessions.size());
        }
    }
    @Test void returnAndFailedBAuthenticationLeaveAClosed() throws Exception {
        try (var f = new Fixture()) {
            f.open(); f.releaseClose.countDown(); f.switchTo(null);
            edt(() -> { assertEquals(f.a, f.app.selectedVault()); assertEquals(ShellState.LOCKED, f.app.state()); });
            assertEquals(1, f.sessions.getFirst().closes.get());
            f.switchTo(f.b); f.failOpen = true; f.open();
            edt(() -> { assertEquals(f.b, f.app.selectedVault()); assertEquals(ShellState.LOCKED, f.app.state()); });
            assertEquals(1, f.sessions.size());
        }
    }
    @Test void forgettingEntriesStaysInSelectionUntilFinalEntryAndNeverChangesVaultFiles() throws Exception {
        try (var f = new Fixture()) {
            f.open(); f.releaseClose.countDown(); f.action(f.app::changeVault);
            f.choice(VaultSelection.Action.FORGET, f.a);
            edt(() -> { assertNull(f.app.selectedVault()); assertEquals(ShellState.VAULT_SELECTION, f.app.state()); assertFalse(f.shell.canReturn); });
            assertEquals(1, f.sessions.getFirst().closes.get());
            var registry = new VaultRegistry(f.preferences);
            assertEquals(List.of(f.b), registry.paths()); assertTrue(registry.selected().isEmpty());
            f.choice(VaultSelection.Action.FORGET, f.b);
            edt(() -> assertEquals(ShellState.NO_VAULT, f.app.state()));
            assertEquals("TOTIPO-VLT", Files.readString(f.a.resolve("vault")));
            assertEquals("TOTIPO-VLT", Files.readString(f.b.resolve("vault")));
        }
    }
    @Test void rememberedMissingPathRemainsRecoverableAndInvalidNewPathDoesNotReplaceIt() throws Exception {
        try (var f = new Fixture()) {
            Files.delete(f.b.resolve("vault")); Files.delete(f.b); f.select(f.b);
            edt(() -> { assertEquals(f.b, f.app.selectedVault()); assertTrue(f.shell.notice.contains("unavailable")); });
            f.action(f.app::changeVault); f.shell.directory = directory.resolve("not-remembered");
            f.choice(VaultSelection.Action.BROWSE, null);
            edt(() -> { assertEquals(f.b, f.app.selectedVault()); assertEquals(ShellState.VAULT_SELECTION, f.app.state()); });
            assertEquals(List.of(f.b, f.a), new VaultRegistry(f.preferences).paths());
            Path c = vault("c"); f.shell.directory = c; f.choice(VaultSelection.Action.BROWSE, null);
            assertEquals(List.of(f.b, f.a, c), new VaultRegistry(f.preferences).paths());
            assertEquals(c, new VaultRegistry(f.preferences).selected().orElseThrow());
            edt(() -> assertEquals(ShellState.LOCKED, f.app.state()));
        }
    }
    @Test void browseAndCreateCancellationStayOnSelection() throws Exception {
        try (var f = new Fixture()) {
            f.shell.directory = null; f.choice(VaultSelection.Action.BROWSE, null);
            f.choice(VaultSelection.Action.CREATE, null);
            f.shell.directory = directory; f.shell.password = null; f.choice(VaultSelection.Action.CREATE, null);
            edt(() -> { assertEquals(ShellState.VAULT_SELECTION, f.app.state()); assertEquals(f.a, f.app.selectedVault()); assertFalse(f.shell.canReturn); });
            assertEquals(List.of(f.b, f.a), new VaultRegistry(f.preferences).paths()); assertTrue(f.sessions.isEmpty());
        }
    }
    @Test void selectionCreateUsesExistingFlowAndOnlyConfirmedSuccessRegistersNewVault() throws Exception {
        try (var f = new Fixture()) {
            Path c = Files.createDirectory(directory.resolve("c")); f.shell.directory = c;
            for (CreateVaultResult result : List.of(new CreateVaultResult.Failed(), new CreateVaultResult.Uncertain(), new CreateVaultResult.AlreadyExists())) {
                f.creation = result; f.choice(VaultSelection.Action.CREATE, null);
                assertEquals(f.a, new VaultRegistry(f.preferences).selected().orElseThrow());
                assertEquals(List.of(f.b, f.a), new VaultRegistry(f.preferences).paths());
                edt(() -> assertEquals(ShellState.VAULT_SELECTION, f.app.state()));
            }
            f.shell.password = null; f.choice(VaultSelection.Action.CREATE, null);
            assertEquals(f.a, new VaultRegistry(f.preferences).selected().orElseThrow());
            f.shell.password = new char[] {'p'}; f.creation = new CreateVaultResult.Created(new Session());
            f.choice(VaultSelection.Action.CREATE, null);
            assertEquals(List.of(f.b, f.a, c), new VaultRegistry(f.preferences).paths());
            assertEquals(c, new VaultRegistry(f.preferences).selected().orElseThrow());
            edt(() -> assertEquals(ShellState.UNLOCKED, f.app.state()));
        }
    }
    @Test void lockDuringBrowseAndForgetInvalidatesDialogResultsWithoutLeavingSelection() throws Exception {
        try (var f = new Fixture()) {
            f.shell.directory = f.b; f.shell.duringDirectory = f.app::lock;
            f.choice(VaultSelection.Action.BROWSE, null);
            f.shell.duringForget = f.app::lock; f.choice(VaultSelection.Action.FORGET, f.a);
            edt(() -> { assertEquals(ShellState.VAULT_SELECTION, f.app.state()); assertEquals(f.a, f.app.selectedVault()); });
            assertEquals(List.of(f.b, f.a), new VaultRegistry(f.preferences).paths());
            assertEquals(f.a, new VaultRegistry(f.preferences).selected().orElseThrow());
        }
    }
    @Test void failedForgetConfirmationAndForgettingOtherEntryPreserveReturnTarget() throws Exception {
        try (var f = new Fixture()) {
            f.select(f.a); f.action(f.app::changeVault);
            f.shell.allowForget = false; f.choice(VaultSelection.Action.FORGET, f.a);
            assertEquals(List.of(f.b, f.a), new VaultRegistry(f.preferences).paths());
            f.shell.allowForget = true; f.choice(VaultSelection.Action.FORGET, f.b);
            edt(() -> { assertEquals(ShellState.VAULT_SELECTION, f.app.state()); assertTrue(f.shell.canReturn); });
            f.choice(VaultSelection.Action.RETURN, null);
            edt(() -> { assertEquals(f.a, f.app.selectedVault()); assertEquals(ShellState.LOCKED, f.app.state()); });
            assertTrue(f.sessions.isEmpty());
        }
    }
    @Test void exceptionalCreationStaysInSelectionAndRetainsRegistry() throws Exception {
        try (var f = new Fixture()) {
            f.throwCreate = true; f.shell.directory = directory; f.choice(VaultSelection.Action.CREATE, null);
            edt(() -> { assertEquals(ShellState.VAULT_SELECTION, f.app.state()); assertEquals(f.a, f.app.selectedVault()); });
            assertEquals(List.of(f.b, f.a), new VaultRegistry(f.preferences).paths());
        }
    }
    @Test void lateCreateAfterLockCannotReplaceSelectionAndClosesUnclaimedSession() throws Exception {
        try (var f = new Fixture()) {
            Session unclaimed = new Session(); f.creation = new CreateVaultResult.Created(unclaimed);
            f.allowCreate = new CountDownLatch(1); f.shell.directory = directory;
            edt(() -> { f.shell.ready = new CountDownLatch(1);
                f.shell.selectionAction.accept(new VaultSelection(VaultSelection.Action.CREATE, null)); });
            await(f.createEntered); edt(f.app::lock); f.allowCreate.countDown(); await(f.shell.ready);
            edt(() -> { assertEquals(ShellState.VAULT_SELECTION, f.app.state()); assertEquals(f.a, f.app.selectedVault()); });
            assertEquals(1, unclaimed.closes.get()); assertTrue(f.windows.isEmpty());
            assertEquals(List.of(f.b, f.a), new VaultRegistry(f.preferences).paths());
        }
    }
    @Test void switchDuringPendingProjectionRetiresImmediatelyAndWaitsBeforeSelection() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        try (var f = new Fixture()) {
            try {
                f.open(); f.releaseClose.countDown();
                VaultState base = (VaultState) java.lang.reflect.Proxy.newProxyInstance(VaultState.class.getClassLoader(),
                        new Class<?>[] {VaultState.class}, (proxy, method, args) -> {
                            assertEquals("generateTotp", method.getName()); entered.countDown(); await(release);
                            return new TotpCode("001234", java.time.Instant.EPOCH, java.time.Instant.EPOCH.plusSeconds(30));
                        });
                var deliveries = new java.util.concurrent.atomic.AtomicInteger();
                edt(() -> f.windows.getFirst().reveal.generate(base, List.of(MergeFixtures.alternative(0)),
                        java.time.Instant.EPOCH, codes -> deliveries.incrementAndGet())); await(entered);
                edt(() -> { f.shell.ready = new CountDownLatch(1); f.app.changeVault();
                    assertTrue(f.windows.getFirst().closing); assertEquals(1, f.shell.selectorVisits);
                    assertEquals(0, f.sessions.getFirst().closes.get()); });
                release.countDown(); await(f.shell.ready); f.select(f.b); f.open();
                edt(() -> { assertEquals(f.b, f.app.selectedVault()); assertEquals(ShellState.UNLOCKED, f.app.state()); });
                assertEquals(0, deliveries.get());
            } finally { release.countDown(); }
        }
    }
    @Test void exitWhileAClosesSuppressesSelectionAndWaitsForRetirement() throws Exception {
        try (var f = new Fixture()) {
            f.open(); edt(f.app::changeVault); await(f.sessions.getFirst().closeEntered);
            edt(() -> { f.app.shutdown(); assertEquals(1, f.shell.disposed.getCount()); });
            f.releaseClose.countDown(); await(f.shell.disposed);
            assertEquals(1, f.shell.selectorVisits); assertEquals(1, f.sessions.size()); assertTrue(f.app.executorShutdown());
        }
    }
}
