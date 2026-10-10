package org.totipo.desktop;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.totipo.CreateVaultResult;
import org.totipo.OpenResult;
import org.totipo.desktop.clipboard.TotpClipboard;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.*;

class RememberedNewVaultTest {
    @TempDir Path directory;

    static final class Access implements VaultAccess {
        CreateVaultResult result = new CreateVaultResult.Failed();
        final AtomicInteger creates = new AtomicInteger();
        public OpenResult open(Path path, char[] password) { throw new AssertionError("No implicit open"); }
        public CreateVaultResult create(Path path, char[] password) { creates.incrementAndGet(); return result; }
    }

    @Test void rememberedASurvivesPickerPasswordAndEmptyPasswordCancellation() throws Exception {
        for (String cancellation : List.of("picker", "password", "empty password", "destination error")) {
            var access = new Access(); var shell = new Shell(); Path a = vault("a-" + cancellation);
            var store = new RememberedVaultTest.Store(a);
            var app = onEdt(() -> new DesktopApplication(access, shell, path -> new Window(), new TotpClipboard(), store));
            try {
                edt(app::show); await(shell.ready);
                shell.directory = Files.createDirectory(directory.resolve("b-" + cancellation));
                shell.duringDirectory = () -> {
                    assertEquals(a, app.selectedVault()); assertEquals(a, store.path); assertEquals(a, shell.chooserLocation);
                    assertTrue(shell.choosingCreate);
                    if (cancellation.equals("destination error")) { shell.directory = null; }
                };
                shell.duringPassword = () -> { assertEquals(a, app.selectedVault()); assertEquals(a, store.path); };
                if (cancellation.equals("picker")) { shell.directory = null; }
                if (cancellation.equals("password")) { shell.password = null; }
                if (cancellation.equals("empty password")) { shell.password = new char[0]; shell.allowEmptyPassword = false; }
                edt(() -> { shell.ready = new CountDownLatch(1); shell.create.run(); }); await(shell.ready);
                edt(() -> {
                    assertEquals(a, app.selectedVault()); assertEquals(a, shell.selected); assertEquals(a, store.path);
                    assertEquals(0, store.writes); assertEquals(ShellState.LOCKED, app.state()); assertFalse(shell.busy);
                });
                assertEquals(0, access.creates.get());
            } finally { edt(app::shutdown); await(shell.disposed); }
        }
    }

    @Test void allJavaCreationRefusalsAndUncertaintyPreserveRememberedA() throws Exception {
        List<CreateVaultResult> results = List.of(new CreateVaultResult.Failed(), new CreateVaultResult.AlreadyExists(),
                new CreateVaultResult.Uncertain(), new CreateVaultResult.Failed(CreateVaultResult.FailureReason.OBJECT_DATA_OBSERVED));
        int index = 0;
        for (var result : results) {
            Path a = vault("a-" + index), b = Files.createDirectory(directory.resolve("b-" + index++));
            var access = new Access(); access.result = result; var shell = new Shell(); shell.directory = b;
            var store = new RememberedVaultTest.Store(a);
            var app = onEdt(() -> new DesktopApplication(access, shell, path -> fail("No content on failure"), new TotpClipboard(), store));
            try {
                edt(app::show); await(shell.ready);
                edt(() -> { shell.ready = new CountDownLatch(1); shell.create.run(); }); await(shell.ready);
                edt(() -> {
                    assertEquals(a, app.selectedVault()); assertEquals(a, shell.selected); assertEquals(a, store.path);
                    assertEquals(0, store.writes); assertEquals(ShellState.LOCKED, app.state()); assertFalse(shell.busy);
                    assertEquals(1, shell.titles.size());
                });
                assertEquals(1, access.creates.get());
            } finally { edt(app::shutdown); await(shell.disposed); }
        }
    }

    @Test void successfulCreationSwitchesAndPersistsBOnlyAfterJavaReturnsSession() throws Exception {
        Path a = vault("a"), b = Files.createDirectory(directory.resolve("b"));
        var shell = new Shell(); shell.directory = b; var store = new RememberedVaultTest.Store(a);
        var session = new Session(); var window = new Window();
        VaultAccess access = new VaultAccess() {
            public OpenResult open(Path path, char[] password) { throw new AssertionError(); }
            public CreateVaultResult create(Path path, char[] password) {
                assertEquals(b, path); assertEquals(a, store.path); assertEquals(0, store.writes);
                return new CreateVaultResult.Created(session);
            }
        };
        var app = onEdt(() -> new DesktopApplication(access, shell, path -> window, new TotpClipboard(), store));
        try {
            edt(app::show); await(shell.ready);
            edt(() -> { shell.ready = new CountDownLatch(1); shell.create.run(); }); await(shell.ready);
            edt(() -> {
                assertEquals(b, app.selectedVault()); assertEquals(b, store.path); assertEquals(1, store.writes);
                assertEquals(ShellState.UNLOCKED, app.state());
            });
        } finally { edt(app::shutdown); await(shell.disposed); }
        assertEquals(1, session.closes.get());
    }

    private Path vault(String name) throws Exception {
        Path path = Files.createDirectory(directory.resolve(name));
        Files.writeString(path.resolve("vault"), "TOTIPO-VLT"); return path;
    }
}
