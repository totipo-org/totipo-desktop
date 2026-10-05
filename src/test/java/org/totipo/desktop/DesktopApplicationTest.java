package org.totipo.desktop;

import org.totipo.CreateVaultResult;
import org.totipo.OpenResult;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class DesktopApplicationTest {
    private static final Path DIRECTORY = Path.of("selected-directory");
    private static final class Access implements VaultAccess {
        final AtomicInteger opens = new AtomicInteger(); final AtomicInteger creates = new AtomicInteger();
        Function<char[], OpenResult> open = password -> new OpenResult.Absent();
        Function<char[], CreateVaultResult> create = password -> new CreateVaultResult.Failed();
        public OpenResult open(Path path, char[] password) {
            assertFalse(SwingUtilities.isEventDispatchThread()); assertEquals("totipo-application", Thread.currentThread().getName());
            opens.incrementAndGet(); return open.apply(password);
        }
        public CreateVaultResult create(Path path, char[] password) {
            assertFalse(SwingUtilities.isEventDispatchThread()); creates.incrementAndGet(); return create.apply(password);
        }
    }
    @Test void emptyPasswordCreationRequiresExplicitDecisionButOpenDoesNot() throws Exception {
        for (boolean create : new boolean[] {false, true}) { for (boolean confirm : new boolean[] {false, true}) {
            Access access = new Access(); Shell shell = new Shell(); shell.allowEmptyPassword = confirm;
            var app = onEdt(() -> new DesktopApplication(access, shell, path -> new Window()));
            try {
                edt(() -> app.begin(DIRECTORY, new char[0], create)); await(shell.ready);
                assertEquals(create ? 1 : 0, shell.emptyConfirmations);
                assertEquals(create && confirm ? 1 : 0, access.creates.get()); assertEquals(create ? 0 : 1, access.opens.get());
            } finally { edt(app::shutdown); await(shell.disposed); }
        } }
    }
    @Test void shutdownAndReentrantActionsDuringEmptyConfirmationCannotCreate() throws Exception {
        Access access = new Access(); Shell shell = new Shell(); shell.allowEmptyPassword = true;
        var app = onEdt(() -> new DesktopApplication(access, shell, path -> new Window()));
        shell.duringEmptyConfirmation = () -> { app.begin(DIRECTORY, new char[0], true); app.shutdown(); };
        edt(() -> app.begin(DIRECTORY, new char[0], true)); await(shell.disposed);
        assertEquals(1, shell.emptyConfirmations); assertEquals(0, access.creates.get()); assertTrue(app.executorShutdown());
    }
    @Test void nonSessionOpenOutcomesAreDistinctInlineAndWipePassword() throws Exception {
        List<OpenResult> results = List.of(new OpenResult.Absent(), new OpenResult.Unavailable(), new OpenResult.InvalidVault(), new OpenResult.AuthenticationFailed());
        List<String> messages = List.of("No vault", "unavailable", "invalid or unsupported", "password");
        for (int i = 0; i < results.size(); i++) {
            OpenResult result = results.get(i); Access access = new Access(); char[] password = {'s'};
            access.open = received -> { assertSame(password, received); assertEquals('s', received[0]); return result; };
            Shell shell = new Shell(); var app = onEdt(() -> new DesktopApplication(access, shell, path -> { throw new AssertionError(); }));
            try {
                edt(() -> app.begin(DIRECTORY, password, false)); await(shell.ready); assertArrayEquals(new char[1], password);
                String message = messages.get(i);
                edt(() -> { assertTrue(shell.notice.contains(message)); assertFalse(shell.busy); assertTrue(shell.titles.isEmpty());
                    assertEquals(result instanceof OpenResult.AuthenticationFailed ? ShellState.LOCKED : ShellState.BLOCKING_VAULT_STATE, app.state()); });
                assertEquals(1, access.opens.get()); assertEquals(0, access.creates.get());
            } finally { edt(app::shutdown); await(shell.disposed); }
        }
    }
    @Test void nonSessionCreateOutcomesRemainDistinctAndNeverRetry() throws Exception {
        List<CreateVaultResult> results = List.of(new CreateVaultResult.AlreadyExists(), new CreateVaultResult.Failed(), new CreateVaultResult.Uncertain());
        List<String> titles = List.of("Vault already exists", "Creation failed", "Creation uncertain");
        for (int i = 0; i < results.size(); i++) {
            CreateVaultResult result = results.get(i); Access access = new Access(); access.create = password -> result;
            Shell shell = new Shell(); var app = onEdt(() -> new DesktopApplication(access, shell, path -> { throw new AssertionError(); }));
            char[] password = {'p'};
            try {
                edt(() -> app.begin(DIRECTORY, password, true)); await(shell.ready);
                assertArrayEquals(new char[1], password); assertEquals(List.of(titles.get(i)), shell.titles);
                if (result instanceof CreateVaultResult.Uncertain) { assertTrue(shell.messages.getFirst().contains("Do not blindly retry")); }
                assertEquals(0, access.opens.get()); assertEquals(1, access.creates.get());
            } finally { edt(app::shutdown); await(shell.disposed); }
        }
    }
    @Test void runtimeFailureWipesPasswordAndShowsBlockingState() throws Exception {
        Access access = new Access(); access.open = password -> { throw new IllegalStateException(); };
        Shell shell = new Shell(); var app = onEdt(() -> new DesktopApplication(access, shell, path -> new Window())); char[] password = {'p'};
        try {
            edt(() -> app.begin(DIRECTORY, password, false)); await(shell.ready); assertArrayEquals(new char[1], password);
            edt(() -> { assertEquals(ShellState.BLOCKING_VAULT_STATE, app.state()); assertTrue(shell.notice.contains("unexpectedly")); });
        } finally { edt(app::shutdown); await(shell.disposed); }
    }
    @Test void successfulOpenAndCreateTransferOwnershipAndWipePassword() throws Exception {
        for (boolean create : new boolean[] {false, true}) {
            Access access = new Access(); Session session = new Session();
            access.open = password -> new OpenResult.Opened(session); access.create = password -> new CreateVaultResult.Created(session);
            Shell shell = new Shell(); Window view = new Window();
            var app = onEdt(() -> new DesktopApplication(access, shell, path -> view)); char[] password = {'p'};
            try {
                edt(() -> app.begin(DIRECTORY, password, create)); await(shell.ready); assertArrayEquals(new char[1], password);
                edt(() -> { assertEquals(ShellState.UNLOCKED, app.state()); view.refresh.run(); view.close.run(); view.close.run(); }); await(shell.disposed);
                assertEquals(1, session.closes.get()); assertEquals(1, session.refreshes.get()); assertEquals("totipo-session-1", session.closeThread);
                assertTrue(app.executorShutdown());
            } finally { edt(app::shutdown); await(shell.disposed); }
        }
    }
    @Test void shutdownDuringOpenOrCreateClosesReturnedSessionWithoutContent() throws Exception {
        for (boolean create : new boolean[] {false, true}) {
            Access access = new Access(); CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), close = new CountDownLatch(1);
            Session session = new Session(close);
            access.open = password -> { entered.countDown(); await(release); return new OpenResult.Opened(session); };
            access.create = password -> { entered.countDown(); await(release); return new CreateVaultResult.Created(session); };
            Shell shell = new Shell(); var app = onEdt(() -> new DesktopApplication(access, shell, path -> { throw new AssertionError("Late content"); }));
            char[] password = {'p'};
            try {
                edt(() -> app.begin(DIRECTORY, password, create)); await(entered); edt(app::shutdown); assertFalse(app.executorShutdown());
                release.countDown(); await(session.closeEntered); assertArrayEquals(new char[1], password);
                assertEquals(1, shell.disposed.getCount()); assertFalse(app.executorShutdown());
            } finally { release.countDown(); close.countDown(); edt(app::shutdown); await(shell.disposed); }
            assertEquals(1, session.closes.get()); assertEquals("totipo-application", session.closeThread); assertTrue(app.executorShutdown());
        }
    }
    @Test void failedContentConstructionClosesUnclaimedSession() throws Exception {
        Access access = new Access(); Session session = new Session(); access.open = password -> new OpenResult.Opened(session);
        Shell shell = new Shell(); var app = onEdt(() -> new DesktopApplication(access, shell, path -> { throw new IllegalStateException(); }));
        try {
            edt(() -> app.begin(DIRECTORY, new char[] {'p'}, false)); await(shell.ready); assertEquals(1, session.closes.get());
            edt(() -> assertEquals(ShellState.BLOCKING_VAULT_STATE, app.state()));
        } finally { edt(app::shutdown); await(shell.disposed); }
    }
    @Test void onePasswordOperationIsAcceptedAndRejectedBufferIsWiped() throws Exception {
        Access access = new Access(); CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        access.open = password -> { entered.countDown(); await(release); return new OpenResult.Absent(); };
        Shell shell = new Shell(); var app = onEdt(() -> new DesktopApplication(access, shell, path -> new Window()));
        try {
            edt(() -> app.begin(DIRECTORY, new char[] {'p'}, false)); await(entered); char[] rejected = {'x'};
            edt(() -> { app.begin(DIRECTORY, rejected, true); shell.open.run(); shell.create.run(); }); assertArrayEquals(new char[1], rejected);
            assertEquals(1, access.opens.get()); assertEquals(0, access.creates.get()); release.countDown(); await(shell.ready);
        } finally { release.countDown(); edt(app::shutdown); await(shell.disposed); }
    }
    @Test void invalidHumanInputNeverReachesCoreAndKeepsOpenAvailable() throws Exception {
        Access access = new Access(); Shell shell = new Shell(); var app = onEdt(() -> new DesktopApplication(access, shell, path -> new Window()));
        char[] invalid = {'\uD800'};
        try {
            edt(() -> app.begin(DIRECTORY, invalid, false)); assertArrayEquals(new char[1], invalid);
            assertEquals(0, access.opens.get()); assertEquals(0, access.creates.get());
            edt(() -> { assertTrue(shell.notice.contains("Unicode")); assertFalse(shell.busy); });
        } finally { edt(app::shutdown); await(shell.disposed); }
    }
    @Test void shutdownInCreatePromptWipesUnsubmittedPassword() throws Exception {
        Access access = new Access(); Shell shell = new Shell(); var app = onEdt(() -> new DesktopApplication(access, shell, path -> new Window()));
        edt(() -> { shell.duringPassword = app::shutdown; shell.create.run(); }); await(shell.disposed);
        assertArrayEquals(new char[1], shell.password); assertEquals(0, access.creates.get()); assertTrue(app.executorShutdown());
    }
    @Test void createChooserAndPasswordCancelLeaveShellAvailable() throws Exception {
        for (boolean directory : new boolean[] {false, true}) {
            Access access = new Access(); Shell shell = new Shell(); if (directory) { shell.directory = null; } else { shell.password = null; }
            var app = onEdt(() -> new DesktopApplication(access, shell, path -> new Window()));
            try { edt(shell.create); await(shell.ready); assertFalse(shell.busy); assertEquals(0, access.creates.get()); }
            finally { edt(app::shutdown); await(shell.disposed); }
        }
    }
    @Test void shutdownInCreateResultDialogWaitsForNestedLoopToReturn() throws Exception {
        Access access = new Access(); Shell shell = new Shell(); var app = onEdt(() -> new DesktopApplication(access, shell, path -> new Window()));
        shell.duringMessage = () -> { app.shutdown(); assertTrue(shell.busy); assertFalse(app.executorShutdown()); };
        edt(() -> app.begin(DIRECTORY, new char[] {'p'}, true)); await(shell.disposed); assertTrue(app.executorShutdown());
    }
    @Test void createDialogReentrancyCannotStartAnotherOperation() throws Exception {
        Access access = new Access(); Shell shell = new Shell(); var app = onEdt(() -> new DesktopApplication(access, shell, path -> new Window()));
        shell.duringMessage = () -> { shell.create.run(); shell.open.run(); };
        try { edt(() -> app.begin(DIRECTORY, new char[] {'p'}, true)); await(shell.ready); assertEquals(1, access.creates.get()); assertEquals(0, access.opens.get()); }
        finally { edt(app::shutdown); await(shell.disposed); }
    }
    @Test void shutdownInChooserPreventsCreationOrSelectionPersistence() throws Exception {
        Access access = new Access(); Shell shell = new Shell(); var store = new RememberedVaultTest.Store(null);
        var app = onEdt(() -> new DesktopApplication(access, shell, path -> new Window(), new org.totipo.desktop.clipboard.TotpClipboard(), store));
        shell.duringDirectory = app::shutdown; edt(shell.create); await(shell.disposed);
        assertEquals(0, access.creates.get()); assertEquals(0, store.writes); assertArrayEquals(new char[] {'p'}, shell.password);
    }
}
