package org.totipo.desktop;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.totipo.*;
import org.totipo.backup.*;
import org.totipo.desktop.ui.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.SwingUtilities;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.*;

@Timeout(40)
class ArchiveWorkflowTest {
    @TempDir Path directory;
    static class View extends Shell {
        Runnable backup, restore, recover, addHistory;
        Runnable duringInput = () -> { };
        int inputSelections, outputSelections;
        @Override public void importAction(Runnable action) { addHistory = action; }
        Path zip, output, recoveryTarget;
        ArchiveView.Destination destination;
        Runnable duringConfirmation = () -> { };
        Runnable duringProgress = () -> { };
        boolean confirm = true, openRestored = true;
        int completed;
        Runnable cancel;
        final List<String> phases = new ArrayList<>();
        @Override public void archiveActions(Runnable b, Runnable r, Runnable c) { backup = b; restore = r; recover = c; }
        @Override public Path chooseBackupOutput() { Edt.require(); outputSelections++; duringConfirmation.run(); return output; }
        @Override public Path chooseBackupInput() { Edt.require(); inputSelections++; duringInput.run(); return zip; }
        final List<String> statuses = new ArrayList<>();
        @Override public boolean confirmLockForRestore() { Edt.require(); duringConfirmation.run(); return confirm; }
        @Override public ArchiveView.RestoreInput restoreInput(Path path, boolean recovery) {
            Edt.require(); assertEquals(zip, path);
            if (!recovery) { assertFalse(Files.exists(destination.path())); }
            duringConfirmation.run();
            return new ArchiveView.RestoreInput(path, recovery ? null : destination, recoveryTarget,
                    PasswordPromptResult.submitted(password), openRestored);
        }
        @Override public boolean confirmLimitedRestore() { return confirm; }
        @Override public void archiveStatus(String text) { Edt.require(); statuses.add(text); if (text.equals("Backup restored.")) { completed++; } }
        @Override public void archiveProgress(String text, Runnable cancel) { Edt.require(); phases.add(text); this.cancel = cancel; duringProgress.run(); }
        @Override public void archiveProgressClosed() { Edt.require(); }
        @Override public void archiveMessage(String title, String text) { message(title, text); }
    }
    private static void awaitUi(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!onEdt(condition::getAsBoolean)) {
            assertTrue(System.nanoTime() < deadline, "Timed out waiting for UI"); Thread.sleep(10);
        }
    }
    private View view() throws IOException {
        View view = new View(); view.zip = Files.write(directory.resolve("input.zip"), BackupRestoreTest.fixture("history"));
        view.destination = new ArchiveView.Destination(directory, "restored"); view.password = "fixture password".toCharArray();
        view.output = directory.resolve("output.zip"); return view;
    }
    @Test void completeRestorationWipesPasswordAndOpensOnlyAfterWorkerRetirement() throws Exception {
        View view = view(); CountDownLatch retired = new CountDownLatch(1), opened = new CountDownLatch(1);
        AtomicReference<ArchiveWorkflow> workflow = new AtomicReference<>();
        edt(() -> {
            workflow.set(new ArchiveWorkflow(view, () -> { }));
            workflow.get().restore(false, retired::countDown, path -> {
                assertFalse(workflow.get().active()); assertEquals(0, retired.getCount());
                assertEquals(directory.resolve("restored"), path); opened.countDown();
            });
            assertTrue(workflow.get().active());
            workflow.get().restore(false, () -> { }, path -> fail("second workflow admitted"));
        });
        await(retired); await(opened);
        assertArrayEquals(new char[view.password.length], view.password); assertEquals(1, view.completed);
        try (var session = BackupRestoreTest.open(directory.resolve("restored")); var capture = session.captureBackup(BackupLimits.DEFAULT)) {
            assertArrayEquals(BackupRestoreTest.fixture("history"), BackupRestoreTest.bytes(capture));
        }
    }
    @ParameterizedTest @EnumSource(value = RestoreResult.Outcome.class, names = {"PARTIAL", "UNCERTAIN", "REJECTED"})
    void incompleteReceiptsNeverOfferOpenAndExplainRetainedResidue(RestoreResult.Outcome outcome) throws Exception {
        View view = view(); CountDownLatch retired = new CountDownLatch(1);
        var workflow = onEdt(() -> new ArchiveWorkflow(view, () -> { }, (plan, fresh, target, recovery) -> {
            assertFalse(SwingUtilities.isEventDispatchThread());
            Files.createDirectory(target); Files.writeString(target.resolve("retained"), "preserve");
            return new RestoreResult(outcome, RestoreResult.Bootstrap.ACKNOWLEDGED, outcome != RestoreResult.Outcome.UNCERTAIN,
                    new ImportResult(ImportResult.Outcome.PARTIAL, 1, 0, 0, 2, ImportResult.Reason.CANCELLED), List.of());
        }));
        edt(() -> workflow.restore(false, retired::countDown, path -> fail("incomplete opened"))); await(retired);
        edt(() -> {
            assertEquals(0, view.completed); assertFalse(workflow.active());
            assertTrue(view.messages.getLast().contains(switch (outcome) {
                case UNCERTAIN -> "could not be confirmed"; case PARTIAL -> "part of the backup"; default -> "could not begin"; }));
            assertTrue(view.messages.getLast().contains("Resume Interrupted Restore"));
        });
        assertEquals("preserve", Files.readString(directory.resolve("restored/retained")));
    }
    @Test void cancelDuringDestinationConfirmationDoesNotAllocateAndClosesPlan() throws Exception {
        View view = view(); CountDownLatch retired = new CountDownLatch(1);
        var workflow = onEdt(() -> new ArchiveWorkflow(view, () -> { }));
        view.duringConfirmation = workflow::cancel;
        edt(() -> workflow.restore(false, retired::countDown, path -> fail())); await(retired);
        assertFalse(Files.exists(directory.resolve("restored"))); assertEquals(0, view.completed);
    }
    @Test void recoveryReauthenticatesSameZipAndDoneDoesNotSelectOrOpen() throws Exception {
        View view = view(); view.recoveryTarget = directory.resolve("retained"); view.openRestored = false;
        try (var plan = BackupRestoreTest.plan("history"); var reservation = org.totipo.storage.nio.NioRestore.reserve(directory, "retained", plan.summary().archiveSha256())) {
            assertEquals(plan.summary().archiveSha256(), reservation.archiveSha256());
        }
        CountDownLatch retired = new CountDownLatch(1);
        var workflow = onEdt(() -> new ArchiveWorkflow(view, () -> { }));
        edt(() -> workflow.restore(true, retired::countDown, path -> fail("Done opened a vault"))); await(retired);
        assertEquals(1, view.completed);
        try (var session = BackupRestoreTest.open(view.recoveryTarget); var capture = session.captureBackup(BackupLimits.DEFAULT)) {
            assertArrayEquals(BackupRestoreTest.fixture("history"), BackupRestoreTest.bytes(capture));
        }
    }
    @Test void cancelledBackupChooserNeverCapturesOrCreatesOutput() throws Exception {
        View view = view(); view.output = null; CountDownLatch retired = new CountDownLatch(1);
        var workflow = onEdt(() -> new ArchiveWorkflow(view, () -> { }));
        edt(() -> workflow.backup(new Session(), directory, retired::countDown)); await(retired);
        assertFalse(Files.exists(directory.resolve("output.zip"))); assertTrue(view.messages.isEmpty()); assertTrue(view.statuses.isEmpty());
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(strings = {"history", "missing-parent"})
    void backupExecutesDirectlyAndReportsVerifiedSuccessWithoutModal(String fixture) throws Exception {
        BackupRestoreTest.restore(directory, "source", fixture); Path source = directory.resolve("source");
        View view = view(); CountDownLatch retired = new CountDownLatch(1);
        var workflow = onEdt(() -> new ArchiveWorkflow(view, () -> { }));
        try (var session = BackupRestoreTest.open(source)) {
            edt(() -> workflow.backup(session, source, retired::countDown)); await(retired);
        }
        assertArrayEquals(BackupRestoreTest.fixture(fixture), Files.readAllBytes(view.output));
        assertTrue(view.messages.isEmpty()); assertEquals(List.of(fixture.equals("history") ? "Backup saved."
                : "Backup saved. Some earlier history is missing."), view.statuses);
    }
    @Test void occupiedBackupOffersAnotherLocationAndPreservesWinner() throws Exception {
        BackupRestoreTest.restore(directory, "source", "history"); Path source = directory.resolve("source");
        View view = new View() {
            @Override public boolean backupLocationFailure(String text) {
                assertTrue(text.contains("already exists")); output = ArchiveWorkflowTest.this.directory.resolve("second.zip"); return true;
            }
        };
        view.output = Files.writeString(directory.resolve("occupied.zip"), "winner");
        CountDownLatch retired = new CountDownLatch(1); var workflow = onEdt(() -> new ArchiveWorkflow(view, () -> { }));
        try (var session = BackupRestoreTest.open(source)) {
            edt(() -> workflow.backup(session, source, retired::countDown)); await(retired);
        }
        assertEquals("winner", Files.readString(directory.resolve("occupied.zip")));
        assertArrayEquals(BackupRestoreTest.fixture("history"), Files.readAllBytes(directory.resolve("second.zip")));
        assertTrue(view.messages.isEmpty()); assertEquals(List.of("Backup saved."), view.statuses);
    }
    @Test void restoredVaultIsSelectedLockedOnlyAfterRetirementWithoutPasswordReuse() throws Exception {
        View shell = view(); Session session = new Session(); Window window = new Window(); AtomicInteger opens = new AtomicInteger();
        VaultAccess access = new VaultAccess() {
            public OpenResult open(Path p, char[] password) { opens.incrementAndGet(); return new OpenResult.Opened(session); }
            public CreateVaultResult create(Path p, char[] password) { throw new AssertionError(); }
        };
        var app = onEdt(() -> new DesktopApplication(access, shell, path -> window));
        try {
            edt(() -> app.begin(directory, new char[] {'p'}, false)); await(shell.ready);
            shell.duringConfirmation = () -> { assertFalse(window.closing); assertEquals(0, session.closes.get()); };
            edt(shell.restore);
            awaitUi(() -> shell.selected != null && shell.selected.equals(directory.resolve("restored")));
            edt(() -> {
                assertEquals(ShellState.LOCKED, app.state()); assertEquals(1, opens.get()); assertEquals(1, session.closes.get());
                assertTrue(shell.messages.isEmpty()); assertEquals(1, shell.completed);
                assertArrayEquals(new char[shell.password.length], shell.password);
            });
        } finally { edt(app::shutdown); await(shell.disposed); }
    }
    @Test void cancelFormKeepsOpenSessionAndAllocatesNothing() throws Exception {
        View shell = new View() {
            @Override public ArchiveView.RestoreInput restoreInput(Path zip, boolean recovery) { return null; }
        };
        shell.zip = Files.write(directory.resolve("input.zip"), BackupRestoreTest.fixture("history"));
        Session session = new Session(); Window window = new Window();
        VaultAccess access = new VaultAccess() {
            public OpenResult open(Path p, char[] password) { return new OpenResult.Opened(session); }
            public CreateVaultResult create(Path p, char[] password) { throw new AssertionError(); }
        };
        var app = onEdt(() -> new DesktopApplication(access, shell, path -> window));
        try {
            edt(() -> app.begin(directory, new char[] {'p'}, false)); await(shell.ready); edt(shell.restore);
            awaitUi(() -> !shell.busy);
            edt(() -> { assertFalse(window.closing); assertEquals(0, session.closes.get()); assertEquals(ShellState.UNLOCKED, app.state()); });
            assertFalse(Files.exists(directory.resolve("restored")));
        } finally { edt(app::shutdown); await(shell.disposed); }
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void lockOrShutdownWhileRestoreWaitsForClosePreventsAllocation(boolean shutdown) throws Exception {
        View shell = view(); Session session = new Session(new CountDownLatch(1)); Window window = new Window();
        VaultAccess access = new VaultAccess() {
            public OpenResult open(Path p, char[] password) { return new OpenResult.Opened(session); }
            public CreateVaultResult create(Path p, char[] password) { throw new AssertionError(); }
        };
        var app = onEdt(() -> new DesktopApplication(access, shell, path -> window));
        try {
            edt(() -> app.begin(directory, new char[] {'p'}, false)); await(shell.ready); edt(shell.restore); await(session.closeEntered);
            edt(() -> { if (shutdown) { app.shutdown(); } else { app.lock(); } });
            session.allowClose.countDown(); await(window.disposed);
            if (shutdown) { await(shell.disposed); } else { awaitUi(() -> !shell.busy); }
            assertFalse(Files.exists(directory.resolve("restored")));
            edt(() -> { assertTrue(shell.messages.isEmpty()); assertEquals(directory, app.selectedVault()); });
        } finally { session.allowClose.countDown(); edt(app::shutdown); await(shell.disposed); }
    }
    @Test void userCancellationDuringEnteredRestoreShowsRecoveryAndKeepsSavedBytes() throws Exception {
        View view = view(); CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), retired = new CountDownLatch(1);
        var workflow = onEdt(() -> new ArchiveWorkflow(view, () -> { }, (plan, fresh, target, recovery) -> {
            Files.createDirectory(target); Files.writeString(target.resolve("entered"), "retain"); entered.countDown();
            while (release.getCount() != 0) { try { release.await(); } catch (InterruptedException requested) { /* return actual receipt */ } }
            return new RestoreResult(RestoreResult.Outcome.PARTIAL, RestoreResult.Bootstrap.ACKNOWLEDGED, true,
                    new ImportResult(ImportResult.Outcome.PARTIAL, 1, 0, 0, 2, ImportResult.Reason.CANCELLED), List.of());
        }));
        try {
            edt(() -> workflow.restore(false, retired::countDown, path -> fail("cancelled restore opened"))); await(entered);
            edt(() -> { view.cancel.run(); assertTrue(workflow.active()); }); release.countDown(); await(retired);
            assertEquals("retain", Files.readString(directory.resolve("restored/entered")));
            edt(() -> { assertEquals(1, view.messages.size()); assertTrue(view.messages.getFirst().contains("Resume Interrupted Restore")); });
        } finally { release.countDown(); }
    }
    private record Blocked(VaultSession session, CountDownLatch entered, CountDownLatch release, AtomicInteger captureCloses) { }
    private static final class TotpWindow extends Window {
        VaultView.TotpAction generate;
        @Override public void totpAction(VaultView.TotpAction action) { generate = action; }
    }
    private Blocked blocked(Session backing) {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1); AtomicInteger closes = new AtomicInteger();
        VaultSession session = (VaultSession) Proxy.newProxyInstance(VaultSession.class.getClassLoader(), new Class<?>[] {VaultSession.class}, (proxy, method, args) -> {
            if (method.getName().equals("captureBackup")) {
                assertFalse(SwingUtilities.isEventDispatchThread()); entered.countDown();
                boolean interrupted = false;
                while (release.getCount() != 0) {
                    try { release.await(); } catch (InterruptedException cancelled) { interrupted = true; }
                }
                if (interrupted) { Thread.currentThread().interrupt(); }
                return new BackupCapture() {
                    public BackupSummary summary() { throw new AssertionError(); }
                    public void writeTo(OutputStream output) throws IOException { throw new BackupException(BackupException.Reason.CANCELLED); }
                    public void close() { assertFalse(SwingUtilities.isEventDispatchThread()); closes.incrementAndGet(); }
                };
            }
            try { return method.invoke(backing, args); } catch (InvocationTargetException failure) { throw failure.getCause(); }
        });
        return new Blocked(session, entered, release, closes);
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void lockAndShutdownWaitForArchiveRetirementWithoutBlockingEdtOrShowingLateResults(boolean shutdown) throws Exception {
        View shell = view(); Session backing = new Session(); Blocked blocked = blocked(backing); TotpWindow window = new TotpWindow();
        VaultAccess access = new VaultAccess() {
            public OpenResult open(Path p, char[] password) { return new OpenResult.Opened(blocked.session()); }
            public CreateVaultResult create(Path p, char[] password) { throw new AssertionError(); }
        };
        DesktopApplication app = onEdt(() -> new DesktopApplication(access, shell, path -> window));
        try {
            edt(() -> app.begin(directory, new char[] {'p'}, false)); await(shell.ready);
            edt(shell.backup); await(blocked.entered());
            CountDownLatch codeReady = new CountDownLatch(1);
            var alternative = MergeFixtures.alternative(0);
            VaultState projection = ControllerRevealObservationTest.state(List.of(), () -> { });
            edt(() -> window.generate.generate(projection, List.of(alternative), java.time.Instant.EPOCH, codes -> {
                assertTrue(codes.getFirst().isPresent()); codeReady.countDown();
            }));
            await(codeReady); // TOTP lane remains available while archive I/O is held.
            edt(() -> {
                // State emissions remain admitted and don't cancel a frozen archive job.
                backing.subscriber.onNext(state(new ObservationProgress.Finished(0, false)));
                assertFalse(shell.busy);
                if (shutdown) { app.shutdown(); } else { app.lock(); }
                assertEquals(0, backing.closes.get()); assertEquals(0, window.disposals);
            });
            edt(() -> { assertEquals(0, backing.closes.get()); }); // responsiveness barrier
            blocked.release().countDown(); await(window.disposed);
            assertEquals(1, blocked.captureCloses().get()); assertEquals(1, backing.closes.get());
            edt(() -> { assertTrue(shell.messages.isEmpty()); assertEquals(directory.toAbsolutePath(), app.selectedVault()); });
        } finally { blocked.release().countDown(); edt(app::shutdown); await(shell.disposed); }
    }
    @Test void unfinishedEditorIsKeptOnDiscardDeclineAndCommitWaitsForNormalClose() throws Exception {
        View shell = view(); Session session = new Session(new CountDownLatch(1));
        var window = new TokenWriteControllerTest.View(); var recording = new TokenWritesTest.Recording();
        VaultAccess access = new VaultAccess() {
            public OpenResult open(Path p, char[] password) { return new OpenResult.Opened(session); }
            public CreateVaultResult create(Path p, char[] password) { throw new AssertionError(); }
        };
        var app = onEdt(() -> new DesktopApplication(access, shell, path -> window));
        try {
            edt(() -> app.begin(directory, new char[] {'p'}, false)); await(shell.ready);
            edt(() -> session.subscriber.onNext(recording.state)); edt(() -> { });
            edt(window.create);
            edt(() -> { assertNotNull(window.editor); shell.confirm = false; shell.restore.run(); });
            awaitUi(() -> !shell.busy);
            edt(() -> { assertNotNull(window.editor); assertFalse(window.closing); assertEquals(0, session.closes.get()); });
            // Passwords belong to one attempt and are never reused by the workflow.
            shell.password = "fixture password".toCharArray();
            edt(() -> { shell.confirm = true; shell.restore.run(); }); await(session.closeEntered);
            edt(() -> { assertNull(window.editor); assertFalse(Files.exists(directory.resolve("restored"))); assertTrue(shell.busy); });
            session.allowClose.countDown(); await(window.disposed);
            edt(app::shutdown); await(shell.disposed);
            assertEquals(1, session.closes.get());
        } finally { session.allowClose.countDown(); edt(app::shutdown); await(shell.disposed); }
    }
    @Test void nestedLockWhileChoosingBackupReleasesLeaseWithoutWorkerUse() throws Exception {
        View shell = view(); Session session = new Session(); Window window = new Window();
        VaultAccess access = new VaultAccess() {
            public OpenResult open(Path p, char[] password) { return new OpenResult.Opened(session); }
            public CreateVaultResult create(Path p, char[] password) { throw new AssertionError(); }
        };
        var app = onEdt(() -> new DesktopApplication(access, shell, p -> window));
        try {
            edt(() -> app.begin(directory, new char[] {'p'}, false)); await(shell.ready);
            shell.duringConfirmation = app::lock;
            edt(shell.backup); await(window.disposed); assertEquals(1, session.closes.get());
        } finally { edt(app::shutdown); await(shell.disposed); }
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.CsvSource({"backup,false", "backup,true", "restore,false", "restore,true", "import,false", "import,true"})
    void nestedChooserLockOrShutdownRejectsDuplicateAdmissionAndStaleSelectedPath(String operation, boolean shutdown) throws Exception {
        View shell = view(); Session session = new Session(); Window window = new Window();
        VaultAccess access = new VaultAccess() {
            public OpenResult open(Path p, char[] password) { return new OpenResult.Opened(session); }
            public CreateVaultResult create(Path p, char[] password) { throw new AssertionError(); }
        };
        var app = onEdt(() -> new DesktopApplication(access, shell, path -> window));
        try {
            edt(() -> app.begin(directory, new char[] {'p'}, false)); await(shell.ready);
            Runnable select = switch (operation) { case "backup" -> shell.backup; case "restore" -> shell.restore; default -> shell.addHistory; };
            Runnable nested = () -> {
                shell.backup.run(); shell.restore.run(); shell.addHistory.run();
                assertEquals(1, shell.inputSelections + shell.outputSelections);
                if (shutdown) { app.shutdown(); } else { app.lock(); }
                // The fake deliberately returns a selected path after retirement.
            };
            if (operation.equals("backup")) { shell.duringConfirmation = nested; } else { shell.duringInput = nested; }
            edt(select); await(window.disposed);
            if (shutdown) { await(shell.disposed); } else { awaitUi(() -> !shell.busy); }
            edt(() -> { assertTrue(shell.messages.isEmpty()); assertTrue(shell.statuses.isEmpty()); assertEquals(directory, app.selectedVault()); });
            assertEquals(1, session.closes.get()); assertFalse(Files.exists(directory.resolve("output.zip")));
            assertFalse(Files.exists(directory.resolve("restored"))); assertArrayEquals("fixture password".toCharArray(), shell.password);
        } finally { edt(app::shutdown); await(shell.disposed); }
    }
    @Test void restoreValidatesTheReplacementZipChosenInsideForm() throws Exception {
        Path invalid = Files.write(directory.resolve("replacement.zip"), new byte[] {1, 2, 3});
        View view = new View() {
            @Override public ArchiveView.RestoreInput restoreInput(Path path, boolean recovery) {
                return new ArchiveView.RestoreInput(invalid, destination, null, PasswordPromptResult.submitted(password), true);
            }
        };
        view.zip = Files.write(directory.resolve("original.zip"), BackupRestoreTest.fixture("history"));
        view.destination = new ArchiveView.Destination(directory, "restored"); view.password = "fixture password".toCharArray();
        CountDownLatch retired = new CountDownLatch(1); var workflow = onEdt(() -> new ArchiveWorkflow(view, () -> { }));
        edt(() -> workflow.restore(false, retired::countDown, path -> fail("invalid replacement opened"))); await(retired);
        assertFalse(Files.exists(directory.resolve("restored"))); assertArrayEquals(new char[view.password.length], view.password);
        edt(() -> { assertEquals(1, view.messages.size()); assertTrue(view.messages.getFirst().contains("ZIP")); });
    }
    @Test void cancellationDuringEnteredRestoreKeepsOwnershipAndSuppressesLateComplete() throws Exception {
        View view = view(); CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), retired = new CountDownLatch(1);
        var workflow = onEdt(() -> new ArchiveWorkflow(view, () -> { }, (plan, fresh, target, recovery) -> {
            Files.createDirectory(target); Files.writeString(target.resolve("entered"), "retain"); entered.countDown();
            boolean interrupted = false;
            while (release.getCount() != 0) {
                try { release.await(); } catch (InterruptedException requested) { interrupted = true; }
            }
            if (interrupted) { Thread.currentThread().interrupt(); }
            return new RestoreResult(RestoreResult.Outcome.COMPLETE, RestoreResult.Bootstrap.ACKNOWLEDGED, true,
                    new ImportResult(ImportResult.Outcome.COMPLETE, 3, 0, 0, 0, ImportResult.Reason.NONE), List.of());
        }));
        try {
            edt(() -> workflow.restore(false, retired::countDown, path -> fail("late result opened"))); await(entered);
            edt(() -> { workflow.cancel(); assertTrue(workflow.active()); assertEquals(1, retired.getCount()); });
            release.countDown(); await(retired);
            edt(() -> { assertFalse(workflow.active()); assertEquals(0, view.completed); assertTrue(view.messages.isEmpty()); });
            assertEquals("retain", Files.readString(directory.resolve("restored/entered")));
        } finally { release.countDown(); }
    }
}
