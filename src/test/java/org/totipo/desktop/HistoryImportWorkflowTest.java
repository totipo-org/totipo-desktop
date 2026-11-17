package org.totipo.desktop;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.totipo.desktop.BackupRestoreTest.*;
import static org.totipo.desktop.VaultFolderWatcherTest.eventually;

@Timeout(40)
class HistoryImportWorkflowTest {
    @TempDir Path directory;
    static class View extends Shell {
        Path input;
        Runnable add, cancel;
        boolean available, approve = true, retry;
        Runnable confirmation = () -> { };
        final List<String> statuses = new ArrayList<>();
        @Override public void archiveStatus(String text) { Edt.require(); statuses.add(text); }
        final List<ImportResult> receipts = new ArrayList<>();
        @Override public void importAction(Runnable action) { add = action; }
        @Override public void importAvailability(boolean value) { available = value; }
        @Override public Path chooseBackupInput() { Edt.require(); return input; }
        @Override public boolean confirmImport(Path zip, BackupSummary summary) {
            Edt.require(); assertTrue(summary.sourceAuthenticated()); confirmation.run(); return approve;
        }
        @Override public boolean importResult(ImportResult receipt, boolean retryAllowed) {
            Edt.require(); receipts.add(receipt);
            super.importResult(receipt, retryAllowed);
            return retryAllowed && retry && receipts.size() == 1;
        }
        @Override public void archiveProgress(String phase, Runnable action) { Edt.require(); cancel = action; }
        @Override public void archiveMessage(String title, String message) { message(title, message); }
    }
    static final class Handle implements BackupImport {
        final BackupSummary summary;
        final Queue<ImportResult> results = new ConcurrentLinkedQueue<>();
        final CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(0);
        int applies, closes;
        boolean fail;
        Handle() throws IOException { summary = BackupArchives.inspect(new ByteArrayInputStream(fixture("history")), BackupLimits.DEFAULT); }
        @Override public BackupSummary summary() {
            assertFalse(SwingUtilities.isEventDispatchThread());
            return new BackupSummary(summary.vaultId(), summary.objects(), summary.unresolvedReferences(), true, summary.archiveSha256());
        }
        @Override public ImportResult apply() {
            assertFalse(SwingUtilities.isEventDispatchThread()); applies++; entered.countDown();
            boolean interrupted = false;
            while (release.getCount() != 0) {
                try { release.await(); } catch (InterruptedException requested) { interrupted = true; }
            }
            if (interrupted) { Thread.currentThread().interrupt(); }
            if (fail) { throw new IllegalStateException("sensitive exception text"); }
            return Objects.requireNonNull(results.poll());
        }
        @Override public void close() { assertFalse(SwingUtilities.isEventDispatchThread()); closes++; }
    }
    static VaultSession proxy(Session session, Handle handle, AtomicInteger preparations) {
        return (VaultSession) Proxy.newProxyInstance(VaultSession.class.getClassLoader(), new Class<?>[] {VaultSession.class}, (p, method, args) -> {
            if (method.getName().equals("prepareImport")) {
                assertFalse(SwingUtilities.isEventDispatchThread()); preparations.incrementAndGet(); return handle;
            }
            try { return method.invoke(session, args); } catch (InvocationTargetException failure) { throw failure.getCause(); }
        });
    }
    View view() throws IOException {
        View view = new View(); view.input = Files.write(directory.resolve("input.zip"), fixture("history")); return view;
    }
    static ImportResult receipt(ImportResult.Outcome outcome) {
        return switch (outcome) {
            case COMPLETE -> new ImportResult(outcome, 3, 0, 0, 0, ImportResult.Reason.NONE);
            case NO_OP -> new ImportResult(outcome, 0, 3, 0, 0, ImportResult.Reason.NONE);
            case PARTIAL -> new ImportResult(outcome, 1, 0, 0, 2, ImportResult.Reason.STORAGE);
            case UNCERTAIN -> new ImportResult(outcome, 1, 0, 1, 1, ImportResult.Reason.STORAGE);
            case FAILED -> new ImportResult(outcome, 0, 0, 0, 3, ImportResult.Reason.TARGET_CONFLICT);
        };
    }
    @ParameterizedTest @EnumSource(ImportResult.Outcome.class)
    void receiptsUseObjectKnowledgeAndRefreshDespiteNoWatcherOrObservation(ImportResult.Outcome outcome) throws Exception {
        View view = view(); Handle handle = new Handle(); handle.results.add(receipt(outcome));
        Session backing = new Session(); AtomicInteger preparations = new AtomicInteger(); CountDownLatch retired = new CountDownLatch(1);
        var workflow = onEdt(() -> new ArchiveWorkflow(view, () -> { }));
        edt(() -> workflow.addHistory(proxy(backing, handle, preparations), retired::countDown, backing::requestRefresh)); await(retired);
        edt(() -> {
            assertEquals(List.of(receipt(outcome)), view.receipts); assertEquals(1, backing.refreshes.get());
            if (outcome == ImportResult.Outcome.NO_OP || outcome == ImportResult.Outcome.COMPLETE) {
                assertTrue(view.messages.isEmpty());
                assertEquals(List.of(outcome == ImportResult.Outcome.NO_OP ? "No new history to add."
                        : "History added. Your vault may show new versions or conflicts."), view.statuses);
            } else {
                assertTrue(view.statuses.isEmpty()); assertEquals(1, view.messages.size());
                assertTrue(view.messages.getFirst().contains(outcome == ImportResult.Outcome.PARTIAL ? "Some history was added"
                        : outcome == ImportResult.Outcome.UNCERTAIN ? "saving could not be confirmed" : "could not be added"));
            }
            assertFalse(workflow.active());
        });
        assertEquals(1, handle.closes); assertEquals(1, preparations.get());
    }
    @ParameterizedTest @EnumSource(value = ImportResult.Outcome.class, names = {"PARTIAL", "UNCERTAIN", "FAILED"})
    void explicitRetryReusesSameHandleAndCumulativeReceipts(ImportResult.Outcome outcome) throws Exception {
        View view = view(); view.retry = true; Handle handle = new Handle();
        handle.results.add(receipt(outcome)); handle.results.add(receipt(ImportResult.Outcome.COMPLETE));
        Session backing = new Session(); AtomicInteger preparations = new AtomicInteger(); CountDownLatch retired = new CountDownLatch(1);
        var workflow = onEdt(() -> new ArchiveWorkflow(view, () -> { }));
        edt(() -> workflow.addHistory(proxy(backing, handle, preparations), retired::countDown, backing::requestRefresh)); await(retired);
        assertEquals(2, handle.applies); assertEquals(1, preparations.get()); assertEquals(1, handle.closes); assertEquals(2, backing.refreshes.get());
    }
    @Test void confirmationBindsFrozenBytesEvenIfUserSelectedZipIsReplaced() throws Exception {
        restore(directory, "target", "empty"); Path root = directory.resolve("target"); View view = view();
        view.confirmation = () -> {
            try { Files.write(view.input, new byte[] {1, 2, 3}); } catch (IOException failure) { throw new UncheckedIOException(failure); }
        };
        var before = HistoryImportTest.snapshot(root); CountDownLatch retired = new CountDownLatch(1);
        try (var session = open(root)) {
            var workflow = onEdt(() -> new ArchiveWorkflow(view, () -> { }));
            edt(() -> workflow.addHistory(session, retired::countDown, session::requestRefresh)); await(retired);
            assertEquals(ImportResult.Outcome.COMPLETE, view.receipts.getFirst().outcome()); HistoryImportTest.preserved(before);
            try (var capture = session.captureBackup(BackupLimits.DEFAULT)) { assertArrayEquals(fixture("history"), bytes(capture)); }
        }
    }
    @Test void cancellationDuringEnteredApplyRetainsOwnershipThenReportsActualEffectsAndRefreshes() throws Exception {
        View view = view(); Handle handle = new Handle(); handle.release = new CountDownLatch(1);
        handle.results.add(new ImportResult(ImportResult.Outcome.PARTIAL, 1, 0, 0, 2, ImportResult.Reason.CANCELLED));
        Session backing = new Session(); CountDownLatch retired = new CountDownLatch(1);
        var workflow = onEdt(() -> new ArchiveWorkflow(view, () -> { }));
        try {
            edt(() -> workflow.addHistory(proxy(backing, handle, new AtomicInteger()), retired::countDown, backing::requestRefresh)); await(handle.entered);
            edt(() -> { view.cancel.run(); assertTrue(workflow.active()); assertEquals(1, retired.getCount()); assertEquals(0, handle.closes); });
            handle.release.countDown(); await(retired);
            assertEquals(1, view.receipts.getFirst().installed()); assertEquals(1, backing.refreshes.get()); assertEquals(1, handle.closes);
        } finally { handle.release.countDown(); }
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void lockAndShutdownWaitForImportAndHandleCleanupWhileSuppressingLateResults(boolean shutdown) throws Exception {
        View shell = view(); Session backing = new Session(); Handle handle = new Handle(); handle.release = new CountDownLatch(1);
        handle.results.add(receipt(ImportResult.Outcome.COMPLETE)); var session = proxy(backing, handle, new AtomicInteger());
        AtomicReference<VaultView.TotpAction> generation = new AtomicReference<>();
        Window window = new Window() {
            @Override public void totpAction(VaultView.TotpAction action) { generation.set(action); }
        };
        VaultAccess access = new VaultAccess() {
            public OpenResult open(Path path, char[] password) { return new OpenResult.Opened(session); }
            public CreateVaultResult create(Path path, char[] password) { throw new AssertionError(); }
        };
        var app = onEdt(() -> new DesktopApplication(access, shell, path -> window));
        try {
            edt(() -> app.begin(directory, new char[] {'p'}, false)); await(shell.ready);
            edt(() -> { assertTrue(shell.available); shell.add.run(); }); await(handle.entered);
            CountDownLatch projected = new CountDownLatch(1);
            edt(() -> generation.get().generate(TestSupport.state(new ObservationProgress.Finished(0, false)), List.of(),
                    java.time.Instant.EPOCH, codes -> projected.countDown()));
            await(projected); // Entered provider work does not occupy the local projection executor.
            edt(() -> {
                assertFalse(shell.available); shell.add.run();
                if (shutdown) { app.shutdown(); } else { app.lock(); }
                assertEquals(0, backing.closes.get()); assertEquals(0, handle.closes);
            });
            handle.release.countDown(); await(window.disposed);
            assertEquals(1, handle.applies); assertEquals(1, handle.closes); assertEquals(1, backing.closes.get());
            edt(() -> { assertTrue(shell.receipts.isEmpty()); assertTrue(shell.messages.isEmpty()); });
        } finally { handle.release.countDown(); edt(app::shutdown); await(shell.disposed); }
    }
    @Test void activeEditorExcludesImportAndImportExcludesEditorWithoutDisablingTotpLane() throws Exception {
        try (var harness = new TokenWriteControllerTest.Harness()) {
            harness.open();
            edt(() -> { assertFalse(harness.controller.canImport()); assertNull(harness.controller.acquireImport(() -> fail())); });
        }
        Session backing = new Session(); TokenWriteControllerTest.View window = new TokenWriteControllerTest.View();
        CountDownLatch closed = new CountDownLatch(1); var controller = onEdt(() -> new VaultWindowController(backing, window, 82, owner -> closed.countDown()));
        var recording = new TokenWritesTest.Recording();
        try {
            edt(() -> { controller.start(); backing.subscriber.onNext(recording.state); }); edt(() -> { });
            edt(() -> {
                assertNotNull(controller.acquireImport(() -> { })); assertFalse(window.available);
                window.create.run(); assertNull(window.editor); controller.refresh(); assertEquals(1, backing.refreshes.get());
                controller.archiveRetired(); assertTrue(controller.canImport());
            });
        } finally { edt(controller::close); await(closed); }
    }
    @Test void declineAndLockDuringConfirmationNeverApply() throws Exception {
        for (boolean lock : List.of(false, true)) {
            View view = view(); Handle handle = new Handle(); Session backing = new Session(); CountDownLatch retired = new CountDownLatch(1);
            var workflow = onEdt(() -> new ArchiveWorkflow(view, () -> { }));
            if (lock) { view.confirmation = workflow::cancel; } else { view.approve = false; }
            edt(() -> workflow.addHistory(proxy(backing, handle, new AtomicInteger()), retired::countDown, backing::requestRefresh)); await(retired);
            assertEquals(0, handle.applies); assertEquals(1, handle.closes); assertEquals(0, backing.refreshes.get());
        }
    }
    @Test void unexpectedApplyFailureRequestsObservationAndNeverAssertsNoEffectsOrLeaksExceptionText() throws Exception {
        View view = view(); Handle handle = new Handle(); handle.fail = true; Session backing = new Session(); CountDownLatch retired = new CountDownLatch(1);
        var workflow = onEdt(() -> new ArchiveWorkflow(view, () -> { }));
        edt(() -> workflow.addHistory(proxy(backing, handle, new AtomicInteger()), retired::countDown, backing::requestRefresh)); await(retired);
        assertEquals(1, backing.refreshes.get()); assertTrue(view.messages.getFirst().contains("may have published"));
        assertFalse(view.messages.getFirst().contains("sensitive")); assertEquals(1, handle.closes);
    }
    @Test void resultPresentationFailureStillObservesAndRetiresHandle() throws Exception {
        View view = new View() {
            @Override public boolean importResult(ImportResult receipt, boolean retryAllowed) {
                throw new IllegalStateException("sensitive presentation failure");
            }
        };
        view.input = Files.write(directory.resolve("input.zip"), fixture("history"));
        Handle handle = new Handle(); handle.results.add(receipt(ImportResult.Outcome.COMPLETE));
        Session backing = new Session(); CountDownLatch retired = new CountDownLatch(1);
        var workflow = onEdt(() -> new ArchiveWorkflow(view, () -> { }));
        edt(() -> workflow.addHistory(proxy(backing, handle, new AtomicInteger()), retired::countDown, backing::requestRefresh)); await(retired);
        assertEquals(1, backing.refreshes.get()); assertEquals(1, handle.closes);
        edt(() -> assertFalse(workflow.active()));
    }
    @Test void realImportObservationShowsConflictsWithoutAutomaticRevealOrClipboard() throws Exception {
        restore(directory, "target", "missing-parent"); Path root = directory.resolve("target"); View view = view();
        var session = open(root); var window = onEdt(ControllerRevealObservationTest.BrowserWindow::new);
        CountDownLatch closed = new CountDownLatch(1), retired = new CountDownLatch(1); AtomicInteger generated = new AtomicInteger(), copied = new AtomicInteger();
        var controller = onEdt(() -> new VaultWindowController(session, window, 83, owner -> closed.countDown()));
        var workflow = onEdt(() -> new ArchiveWorkflow(view, () -> { }));
        try {
            edt(() -> {
                controller.start(); // Deliberately no watcher: import must explicitly request observation.
                window.probe.browser.totpAction((base, alternatives, now, done) -> generated.incrementAndGet());
                window.probe.browser.copyAction((code, from, until, now) -> { copied.incrementAndGet(); return "Copied"; });
            });
            eventually(() -> window.observed != null && !window.observed.tokens().isEmpty());
            edt(() -> {
                var owned = controller.acquireImport(workflow::cancel); assertNotNull(owned);
                workflow.addHistory(owned, () -> { controller.archiveRetired(); retired.countDown(); }, controller::refresh);
            }); await(retired);
            eventually(() -> window.observed.tokens().getFirst().hasConflict());
            edt(() -> { window.probe.assertConcealed(window.observed.tokens().getFirst().id()); assertEquals(0, generated.get()); assertEquals(0, copied.get()); });
        } finally { edt(controller::close); await(closed); }
    }
}
