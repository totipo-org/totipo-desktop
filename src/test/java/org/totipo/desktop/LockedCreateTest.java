package org.totipo.desktop;

import java.nio.file.*;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.totipo.*;
import org.totipo.desktop.clipboard.TotpClipboard;
import static org.totipo.desktop.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class LockedCreateTest {
    @TempDir Path directory;
    Path original() throws Exception {
        Path a = Files.createDirectory(directory.resolve("a"));
        Files.writeString(a.resolve("vault"), "TOTIPO-VLT"); return a;
    }
    DesktopApplication app(Shell shell, VaultAccess access, RememberedVaultTest.Store store) throws Exception {
        var app = onEdt(() -> new DesktopApplication(access, shell, path -> new Window(), new TotpClipboard(), store));
        edt(app::show); await(shell.ready); return app;
    }
    void stopped(DesktopApplication app, Shell shell) throws Exception { edt(app::shutdown); await(shell.disposed); }
    void stillA(DesktopApplication app, RememberedVaultTest.Store store, Path a) throws Exception {
        edt(() -> { assertEquals(ShellState.LOCKED, app.state()); assertEquals(a, app.selectedVault()); assertEquals(a, store.path); });
        assertEquals(0, store.writes);
    }
    void restart(VaultAccess access, RememberedVaultTest.Store store, Path expected) throws Exception {
        Shell shell = new Shell(); var next = app(shell, access, store);
        try { edt(() -> { assertEquals(expected, next.selectedVault()); assertEquals(ShellState.LOCKED, next.state()); }); }
        finally { stopped(next, shell); }
    }
    @Test void locationAndPasswordCancellationPreserveLockedAAndRestart() throws Exception {
        Path a = original(); var store = new RememberedVaultTest.Store(a); var access = new NioVaultAccess();
        for (boolean location : new boolean[]{true, false}) {
            Shell shell = new Shell(); var app = app(shell, access, store);
            try {
                shell.directory = location ? null : Files.createTempDirectory(directory, "b-"); shell.password = null;
                edt(() -> { shell.ready = new CountDownLatch(1); shell.create.run(); }); await(shell.ready); stillA(app, store, a);
            } finally { stopped(app, shell); }
            restart(access, store, a);
        }
    }
    @Test void unsafeLocationAndObservedObjectsNeverReplaceA() throws Exception {
        Path a = original(), file = Files.writeString(directory.resolve("file"), "unchanged");
        Path b = Files.createDirectory(directory.resolve("b"));
        Path evidence = Files.createDirectories(b.resolve("objects-v1")).resolve("a".repeat(64));
        Files.writeString(evidence, "candidate evidence");
        var store = new RememberedVaultTest.Store(a); var access = new NioVaultAccess();
        for (Path target : new Path[]{file, b}) {
            Shell shell = new Shell(); var app = app(shell, access, store);
            try {
                shell.directory = target;
                edt(() -> { shell.ready = new CountDownLatch(1); shell.create.run(); }); await(shell.ready);
                stillA(app, store, a); assertFalse(Files.exists(b.resolve("vault")));
                if (target.equals(b)) { assertEquals("Cannot create vault here", shell.titles.get(0)); }
                assertEquals("candidate evidence", Files.readString(evidence));
            } finally { stopped(app, shell); }
            restart(access, store, a);
        }
    }
    @Test void definiteFailureUncertaintyAndInvalidPasswordPreserveA() throws Exception {
        Path a = original(); var store = new RememberedVaultTest.Store(a);
        for (CreateVaultResult result : new CreateVaultResult[]{new CreateVaultResult.Failed(), new CreateVaultResult.Uncertain()}) {
            var access = new VaultAccess() {
                public OpenResult open(Path path, char[] password) { throw new AssertionError(); }
                public CreateVaultResult create(Path path, char[] password) { return result; }
            };
            Shell shell = new Shell(); var app = app(shell, access, store);
            try {
                edt(() -> { shell.ready = new CountDownLatch(1); shell.create.run(); }); await(shell.ready); stillA(app, store, a);
                if (result instanceof CreateVaultResult.Uncertain) { assertTrue(shell.messages.get(0).contains("Do not blindly retry")); }
                edt(() -> app.begin(directory.resolve("invalid"), new char[]{'\ud800'}, true)); stillA(app, store, a);
            } finally { stopped(app, shell); }
            restart(access, store, a);
        }
    }
    @Test void emptyPasswordRequiresConfirmationAndSuccessSwitchesPersistsAndRestartsOnB() throws Exception {
        Path a = original(), b = Files.createDirectory(directory.resolve("b"));
        var store = new RememberedVaultTest.Store(a); var access = new NioVaultAccess(); Shell shell = new Shell();
        var app = app(shell, access, store);
        try {
            shell.directory = b; shell.password = new char[0];
            edt(() -> { shell.ready = new CountDownLatch(1); shell.create.run(); }); await(shell.ready); stillA(app, store, a);
            assertEquals(1, shell.emptyConfirmations); assertFalse(Files.exists(b.resolve("vault")));
            shell.allowEmptyPassword = true;
            edt(() -> { shell.ready = new CountDownLatch(1); shell.create.run(); }); await(shell.ready);
            edt(() -> { assertEquals(ShellState.UNLOCKED, app.state()); assertEquals(b, app.selectedVault()); assertEquals(b, store.path); });
            assertEquals(2, shell.emptyConfirmations); assertEquals(1, store.writes);
        } finally { stopped(app, shell); }
        restart(access, store, b);
    }
    @Test void lockedCreateSharesForegroundAdmission() throws Exception {
        Path a = original(); var store = new RememberedVaultTest.Store(a);
        var access = new VaultAccess() {
            public OpenResult open(Path path, char[] password) { throw new AssertionError("Unexpected open"); }
            public CreateVaultResult create(Path path, char[] password) { throw new AssertionError("Unexpected create"); }
        };
        Shell shell = new Shell(); var app = app(shell, access, store);
        try {
            shell.duringDirectory = () -> { shell.create.run(); shell.open.run(); app.begin(directory, new char[]{'p'}, true); };
            shell.directory = null;
            edt(shell.create); assertEquals(1, shell.directories); stillA(app, store, a);
        } finally { stopped(app, shell); }
    }
}
