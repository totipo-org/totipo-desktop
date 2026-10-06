package org.totipo.desktop;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;
import org.totipo.OpenResult;
import org.totipo.CreateVaultResult;
import org.totipo.desktop.clipboard.TotpClipboard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.totipo.desktop.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class RememberedVaultTest {
    @TempDir Path directory;
    static final class Store implements VaultPreferences {
        Path path; int writes;
        Store(Path path) { this.path = path; }
        public Optional<Path> lastVault() { return Optional.ofNullable(path); }
        public void setLastVault(Path value) { path = value.toAbsolutePath().normalize(); writes++; }
        public void clearLastVault() { path = null; }
    }
    static final class Access implements VaultAccess {
        OpenResult result = new OpenResult.Absent();
        final AtomicInteger opens = new AtomicInteger();
        public OpenResult open(Path path, char[] password) { opens.incrementAndGet(); return result; }
        public CreateVaultResult create(Path path, char[] password) { return new CreateVaultResult.Failed(); }
    }
    Path vault(String name) throws Exception {
        Path path = Files.createDirectory(directory.resolve(name));
        Files.writeString(path.resolve("vault"), "TOTIPO-VLT"); return path;
    }
    DesktopApplication app(Access access, Shell shell, Store store) throws Exception {
        return onEdt(() -> new DesktopApplication(access, shell, path -> new Window(), new TotpClipboard(), store));
    }
    void stop(DesktopApplication app, Shell shell) throws Exception { edt(app::shutdown); await(shell.disposed); }
    void choose(Shell shell, Path target) throws Exception {
        edt(() -> { shell.ready = new CountDownLatch(1); shell.directory = target; shell.open.run(); }); await(shell.ready);
    }
    @Test void noRememberedVaultStartsInNoVault() throws Exception {
        Shell shell = new Shell(); Access access = new Access(); var app = app(access, shell, new Store(null));
        try {
            edt(app::show);
            edt(() -> { assertEquals(ShellState.NO_VAULT, app.state()); assertEquals(1, shell.shown); });
            assertEquals(0, access.opens.get()); assertNull(shell.passwordDirectory);
        } finally { stop(app, shell); }
    }
    @Test void recognizableRememberedVaultStartsDirectlyLockedWithoutPasswordDialog() throws Exception {
        Path target = vault("a"); Shell shell = new Shell(); Access access = new Access();
        var app = app(access, shell, new Store(target));
        try {
            edt(app::show); await(shell.ready);
            edt(() -> { assertEquals(ShellState.LOCKED, app.state()); assertEquals(target, shell.selected);
                assertEquals(1, shell.shown); assertEquals(0, shell.directories); assertNull(shell.passwordDirectory); });
            assertEquals(0, access.opens.get());
        } finally { stop(app, shell); }
    }
    @Test void missingNonDirectoryAndNonVaultRememberedTargetsAreUnavailableNotCorrupt() throws Exception {
        Path file = Files.writeString(directory.resolve("file"), "plain text");
        Path empty = Files.createDirectory(directory.resolve("empty"));
        for (Path target : new Path[] {directory.resolve("missing"), file, empty}) {
            Shell shell = new Shell(); Access access = new Access(); Store store = new Store(target);
            var app = app(access, shell, store);
            try {
                edt(app::show); await(shell.ready);
                edt(() -> { assertEquals(ShellState.NO_VAULT, app.state()); assertTrue(shell.notice.contains("unavailable"));
                    assertFalse(shell.notice.contains("corrupt")); assertEquals(1, shell.shown); });
                assertEquals(0, access.opens.get()); assertEquals(0, store.writes);
            } finally { stop(app, shell); }
        }
    }
    @Test void acceptingRecognizableBRemembersItBeforeAnyUnlock() throws Exception {
        Path a = vault("a"), b = vault("b"); Store store = new Store(a); Shell shell = new Shell(); Access access = new Access();
        var app = app(access, shell, store);
        try {
            edt(app::show); await(shell.ready); choose(shell, b);
            edt(() -> { assertEquals(b, app.selectedVault()); assertEquals(b, store.path); assertEquals(ShellState.LOCKED, app.state()); });
            assertEquals(1, store.writes); assertEquals(0, access.opens.get());
        } finally { stop(app, shell); }
    }
    @Test void successfulFormOpenTransitionsSameRememberedShellToUnlocked() throws Exception {
        Path target = vault("a"); Shell shell = new Shell(); Access access = new Access(); Session session = new Session();
        access.result = new OpenResult.Opened(session); var app = app(access, shell, new Store(target));
        try {
            edt(app::show); await(shell.ready);
            edt(() -> { shell.ready = new CountDownLatch(1); shell.submit.accept(new char[] {'p'}); }); await(shell.ready);
            edt(() -> { assertEquals(ShellState.UNLOCKED, app.state()); assertEquals(target, app.selectedVault()); assertEquals(1, shell.shown); });
        } finally { stop(app, shell); }
        assertEquals(1, session.closes.get());
    }
    @Test void wrongPasswordOnBKeepsBAndRestartStartsLockedOnB() throws Exception {
        Path a = vault("a"), b = vault("b"); Store store = new Store(a); Shell shell = new Shell(); Access access = new Access();
        access.result = new OpenResult.AuthenticationFailed(); var app = app(access, shell, store);
        try {
            edt(app::show); await(shell.ready); choose(shell, b);
            char[] password = {'x'};
            edt(() -> { shell.ready = new CountDownLatch(1); shell.submit.accept(password); }); await(shell.ready);
            edt(() -> { assertEquals(ShellState.LOCKED, app.state()); assertEquals(b, app.selectedVault()); assertEquals(b, store.path);
                assertTrue(shell.notice.contains("password")); assertFalse(shell.busy); });
            assertArrayEquals(new char[1], password);
        } finally { stop(app, shell); }
        Shell restarted = new Shell(); var next = app(access, restarted, store);
        try { edt(next::show); await(restarted.ready); edt(() -> { assertEquals(b, restarted.selected); assertEquals(ShellState.LOCKED, next.state()); }); }
        finally { stop(next, restarted); }
    }
    @Test void nonVaultSelectionNeverReplacesRememberedRecognizableAAndCanChooseAgain() throws Exception {
        Path a = vault("a"), b = vault("b"); Path invalid = Files.createDirectory(directory.resolve("invalid"));
        Files.writeString(invalid.resolve("vault"), "not a Totipo bootstrap");
        Store store = new Store(a); Shell shell = new Shell(); Access access = new Access(); var app = app(access, shell, store);
        try {
            edt(app::show); await(shell.ready); choose(shell, invalid);
            edt(() -> { assertEquals(a, store.path); assertEquals(a, app.selectedVault()); assertEquals(ShellState.LOCKED, app.state());
                assertTrue(shell.notice.contains("not a recognizable")); });
            assertEquals(0, store.writes); choose(shell, b); assertEquals(b, store.path);
        } finally { stop(app, shell); }
    }
    @Test void unsupportedOrTruncatedRecognizableBootstrapCanBeSelected() throws Exception {
        Path b = vault("b"); Files.writeString(b.resolve("vault"), "TOTIPO-VLT\u007funsupported");
        Shell shell = new Shell(); Access access = new Access(); Store store = new Store(null); var app = app(access, shell, store);
        try {
            edt(app::show); choose(shell, b); assertEquals(b, store.path);
            access.result = new OpenResult.InvalidVault();
            edt(() -> { shell.ready = new CountDownLatch(1); shell.submit.accept(new char[] {'p'}); }); await(shell.ready);
            edt(() -> { assertEquals(ShellState.BLOCKING_VAULT_STATE, app.state()); assertEquals(b, app.selectedVault());
                shell.retry.run(); assertEquals(ShellState.LOCKED, app.state()); });
        } finally { stop(app, shell); }
    }
    @Test void lockedChangeVaultCancelPreservesLockedTarget() throws Exception {
        Path a = vault("a"); Shell shell = new Shell(); Store store = new Store(a); var app = app(new Access(), shell, store);
        try { edt(app::show); await(shell.ready); choose(shell, null);
            edt(() -> { assertEquals(ShellState.LOCKED, app.state()); assertEquals(a, app.selectedVault()); assertEquals(a, store.path); }); }
        finally { stop(app, shell); }
    }
    @Test void cancellingChangeWhenPreviousLocationDisappearedFallsBackToNoVault() throws Exception {
        Path a = vault("disappeared"); Shell shell = new Shell(); Store store = new Store(a); var app = app(new Access(), shell, store);
        try {
            edt(app::show); await(shell.ready); Files.delete(a.resolve("vault")); Files.delete(a); choose(shell, null);
            edt(() -> { assertEquals(ShellState.NO_VAULT, app.state()); assertNull(app.selectedVault());
                assertTrue(shell.notice.contains("previous vault location is unavailable")); });
        } finally { stop(app, shell); }
    }
    @Test void noVaultChooserCancelStaysNoVault() throws Exception {
        Shell shell = new Shell(); Store store = new Store(null); var app = app(new Access(), shell, store);
        try { edt(app::show); choose(shell, null); edt(() -> assertEquals(ShellState.NO_VAULT, app.state())); assertNull(store.path); }
        finally { stop(app, shell); }
    }
    @Test void isolatedJdkStorePersistsOnlyNormalizedPathAcrossStoreInstances() throws Exception {
        Preferences node = Preferences.userRoot().node("/org/totipo/desktop-tests/" + java.util.UUID.randomUUID());
        try {
            var store = new JdkVaultPreferences(node); assertTrue(store.lastVault().isEmpty());
            Path path = directory.resolve("child/../vault"); store.setLastVault(path);
            assertEquals(path.toAbsolutePath().normalize(), new JdkVaultPreferences(node).lastVault().orElseThrow());
            assertArrayEquals(new String[] {JdkVaultPreferences.KEY}, node.keys());
            store.clearLastVault(); assertTrue(store.lastVault().isEmpty());
        } finally { node.removeNode(); Preferences.userRoot().flush(); }
    }
    @Test void shutdownDuringStartupDoesNotShowContentOrOpen() throws Exception {
        Shell shell = new Shell(); Access access = new Access(); var app = app(access, shell, new Store(vault("a")));
        edt(() -> { app.show(); app.shutdown(); }); await(shell.disposed);
        assertEquals(0, shell.shown); assertEquals(0, access.opens.get()); assertTrue(app.executorShutdown());
    }
    @Test void repeatedShowDoesNotRecheckPreferenceOrShowAnotherShell() throws Exception {
        Shell shell = new Shell(); var app = app(new Access(), shell, new Store(vault("a")));
        try { edt(app::show); await(shell.ready); edt(() -> { app.show(); app.show(); assertEquals(1, shell.shown); }); }
        finally { stop(app, shell); }
    }
}
