package org.totipo.desktop;

import org.totipo.VaultSession;
import org.totipo.backup.*;
import org.totipo.storage.nio.NioRestore;
import org.totipo.desktop.ui.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

/** One bounded archive workflow. Ownership transitions on EDT; resources on its worker.
 * Lifecycle cancellation invalidates presentation, but retains the lease until cleanup.
 * User cancellation of an entered import still presents its cumulative receipt.
 * Workers never wait for the EDT. A newer VaultState has no role in this lifetime.
 */
final class ArchiveWorkflow {
    private final ArchiveView view;
    private final Runnable changed;
    @FunctionalInterface interface Installer {
        RestoreResult install(BackupRestore plan, ArchiveView.Destination fresh, Path target, boolean recovery) throws IOException;
    }
    private final Installer installer;
    private Job job;
    private static final class Job {
        final ExecutorService executor = Executors.newSingleThreadExecutor(task -> new Thread(task, "totipo-archive"));
        volatile boolean cancelled;
        volatile Thread running;
        volatile BackupRestore plan;
        volatile BackupImport history;
        boolean applying;
        boolean suppressed;
        boolean finishing;
        final Runnable retired;
        Job(Runnable retired) { this.retired = retired; }
        void cancel() {
            cancelled = true;
            BackupRestore current = plan;
            if (current != null) { current.cancel(); }
            synchronized (this) { if (running != null) { running.interrupt(); } }
        }
    }
    ArchiveWorkflow(ArchiveView view, Runnable changed) { this(view, changed, ArchiveWorkflow::install); }
    ArchiveWorkflow(ArchiveView view, Runnable changed, Installer installer) {
        this.view = view; this.changed = changed; this.installer = installer;
    }
    private static RestoreResult install(BackupRestore plan, ArchiveView.Destination fresh, Path target, boolean recovery) throws IOException {
        var reservation = recovery ? NioRestore.recover(target, plan.summary().archiveSha256())
                : NioRestore.reserve(fresh.parent(), fresh.child(), plan.summary().archiveSha256());
        try { return plan.install(reservation); }
        catch (RuntimeException failure) { reservation.close(); throw failure; }
    }
    boolean active() { Edt.require(); return job != null; }
    private Job begin(Runnable retired) {
        Edt.require(); if (job != null) { return null; }
        Job created = new Job(retired); job = created; changed.run(); return created;
    }
    void cancel() {
        Edt.require(); if (job != null) { job.suppressed = true; job.cancel(); view.archiveProgressClosed(); retireIfIdle(job); }
    }
    private void cancelFromProgress() {
        Edt.require();
        if (job != null) {
            job.cancel();
            view.archiveProgress("Stopping… Waiting for current work to finish.", () -> { });
            // An entered import returns cumulative knowledge even after user cancellation.
            if (!job.applying) { finish(job); }
        }
    }
    private void retireIfIdle(Job owner) {
        // Queue cleanup even if a prompt/phase has not submitted work. Duplicate cleanup
        // is harmless; identity guards deliver retirement exactly once.
        finish(owner);
    }
    private boolean live(Job owner) { return job == owner && !owner.cancelled; }
    private boolean work(Job owner, Runnable action) {
        Edt.require();
        if (!live(owner) || owner.finishing) { return false; }
        owner.executor.execute(() -> {
            synchronized (owner) {
                owner.running = Thread.currentThread();
                // Cancellation may have arrived while this task was still queued.
                if (owner.cancelled) { owner.running.interrupt(); }
            }
            try { action.run(); }
            finally { synchronized (owner) { owner.running = null; Thread.interrupted(); } }
        });
        return true;
    }
    private void phase(Job owner, String text) {
        SwingUtilities.invokeLater(() -> { if (live(owner)) { view.archiveProgress(text, this::cancelFromProgress); } });
    }
    private void finish(Job owner) {
        finishWithOpen(owner, null, path -> { });
    }
    void addHistory(VaultSession session, Runnable retired, Runnable observe) {
        Job owner = begin(retired); if (owner == null) { retired.run(); return; }
        try {
            Path input = view.chooseBackupInput();
            if (!live(owner) || input == null) { finish(owner); return; }
            view.archiveProgress("Checking backup…", this::cancelFromProgress);
            work(owner, () -> {
                try {
                    BackupOutput.check(() -> owner.cancelled);
                    try (var stream = Files.newInputStream(input)) { owner.history = session.prepareImport(stream, BackupLimits.DEFAULT); }
                    // summary() takes the provider gate too: materialize it off EDT.
                    BackupSummary summary = owner.history.summary();
                    SwingUtilities.invokeLater(() -> {
                        if (!live(owner)) { finish(owner); return; }
                        try {
                            view.archiveProgressClosed();
                            if (!view.confirmImport(input, summary) || !live(owner)) { finish(owner); return; }
                            applyHistory(owner, observe);
                        } catch (RuntimeException presentationFailure) { finish(owner); }
                    });
                } catch (IOException | RuntimeException failure) { failed(owner, failure); }
            });
        } catch (RuntimeException presentationFailure) { finish(owner); }
    }
    private void applyHistory(Job owner, Runnable observe) {
        Edt.require();
        if (!live(owner) || owner.finishing) { finish(owner); return; }
        try {
            owner.applying = true;
            view.archiveProgress("Adding history…", this::cancelFromProgress);
            boolean submitted = work(owner, () -> {
                ImportResult result = null;
                try { result = owner.history.apply(); }
                catch (RuntimeException unexpected) { /* Possible entered publication: observe, never assert no effects. */ }
                ImportResult receipt = result;
                SwingUtilities.invokeLater(() -> {
                    owner.applying = false;
                    boolean retry = false;
                    try {
                        // Independent of watcher delivery, success presentation and cancellation.
                        observe.run();
                        if (job == owner && !owner.suppressed && !owner.finishing) {
                            view.archiveProgressClosed();
                            if (receipt == null) {
                                view.archiveMessage("Add History outcome unavailable", "The operation may have published history, but saving could not be confirmed. Keep the existing files. Check storage access, then select the backup again to retry.");
                            } else {
                                boolean allowed = !owner.cancelled && receipt.outcome() != ImportResult.Outcome.COMPLETE
                                        && receipt.outcome() != ImportResult.Outcome.NO_OP;
                                retry = view.importResult(receipt, allowed) && allowed && live(owner);
                            }
                        }
                    } catch (RuntimeException presentationFailure) {
                        // Retire without exposing arbitrary exception text from presentation.
                        retry = false;
                    } finally {
                        if (retry) { applyHistory(owner, observe); } else { finish(owner); }
                    }
                });
            });
            if (!submitted) { owner.applying = false; finish(owner); }
        } catch (RuntimeException presentationFailure) { owner.applying = false; finish(owner); }
    }
    void backup(VaultSession session, Path source, Runnable retired) {
        Job owner = begin(retired); if (owner == null) { retired.run(); return; }
        backupOutput(owner, session, source);
    }
    private void backupOutput(Job owner, VaultSession session, Path source) {
        try {
            Path output = view.chooseBackupOutput();
            if (!live(owner) || output == null) { finish(owner); return; }
            view.archiveProgress("Backing up…", this::cancelFromProgress);
            owner.applying = true;
            work(owner, () -> {
                String title = "Backup failed", message;
                boolean success = false, chooseAgain = false, outputMayExist = false;
                try {
                    BackupOutput.check(() -> owner.cancelled);
                    try (var capture = session.captureBackup(BackupLimits.DEFAULT)) {
                        var receipt = BackupOutput.write(capture, source, output, () -> owner.cancelled, text -> phase(owner, text));
                        success = receipt.complete(); outputMayExist = receipt.publicationMayExist();
                        title = "Backup output uncertain";
                        message = success ? (capture.summary().unresolvedReferences() == 0 ? "Backup saved." : "Backup saved. Some earlier history is missing.")
                                : "A backup may exist here, but saving could not be confirmed:\n" + output
                                + "\nInspect this ZIP before relying on it. Keep the original vault. Do not overwrite this location.";
                        if (receipt.stagingRetained()) {
                            success = false;
                            title = receipt.complete() ? "Backup saved; cleanup incomplete" : title;
                            message += "\nA temporary backup folder remains beside this location. Preserve it until you have checked the ZIP.";
                        }
                    }
                } catch (IOException | RuntimeException failure) {
                    message = outputMayExist ? "A backup may exist here, but cleanup could not finish:\n" + output
                            + "\nKeep and inspect this ZIP before relying on it. Do not overwrite this location." : failureMessage(failure);
                    chooseAgain = !outputMayExist && failure instanceof IOException && !(failure instanceof BackupException);
                    success = false;
                }
                boolean completed = success, offerLocation = chooseAgain;
                String finalTitle = title, finalMessage = message;
                SwingUtilities.invokeLater(() -> {
                    boolean retry = false;
                    try {
                        owner.applying = false;
                        if (job == owner && !owner.suppressed) {
                            view.archiveProgressClosed();
                            if (completed) { view.archiveStatus(finalMessage); }
                            else if (offerLocation && live(owner)) { retry = view.backupLocationFailure(finalMessage) && live(owner); }
                            else { view.archiveMessage(finalTitle, finalMessage); }
                        }
                    }
                    finally { if (retry) { backupOutput(owner, session, source); } else { finish(owner); } }
                });
            });
        } catch (RuntimeException presentationFailure) { finish(owner); }
    }
    void restore(boolean recovery, Runnable retired, Consumer<Path> openRestored) {
        restore(recovery, retired, openRestored, Runnable::run);
    }
    /** Admission runs after form commitment and detached authentication, before allocation.
     * It may defer continuation until the old controller has fully retired. */
    void restore(boolean recovery, Runnable retired, Consumer<Path> openRestored, Consumer<Runnable> admit) {
        Job owner = begin(retired); if (owner == null) { retired.run(); return; }
        try {
            Path input = view.chooseBackupInput();
            if (!live(owner) || input == null) { finish(owner); return; }
            try (var form = view.restoreInput(input, recovery)) {
                if (form == null || !live(owner)) { finish(owner); return; }
                char[] password = form.password().takePassword();
                boolean submitted = false;
                try {
                    if (password == null || !PasswordInput.valid(password)) {
                        view.archiveMessage("Invalid password input", "Enter valid Unicode using at most 1024 UTF-8 bytes.");
                        finish(owner); return;
                    }
                    view.archiveProgress("Checking backup…", this::cancelFromProgress);
                    submitted = work(owner, () -> {
                        try {
                            BackupOutput.check(() -> owner.cancelled);
                            try (var stream = Files.newInputStream(form.zip())) { owner.plan = BackupArchives.prepareRestore(stream, BackupLimits.DEFAULT, password); }
                            BackupSummary summary = owner.plan.summary();
                            Arrays.fill(password, '\0');
                            SwingUtilities.invokeLater(() -> {
                                if (!live(owner)) { finish(owner); return; }
                                try {
                                    view.archiveProgressClosed();
                                    if (summary.unresolvedReferences() != 0 && !view.confirmLimitedRestore()) { finish(owner); return; }
                                    if (!live(owner)) { finish(owner); return; }
                                    admit.accept(() -> installRestore(owner, form, recovery, openRestored));
                                } catch (RuntimeException presentationFailure) { finish(owner); }
                            });
                        } catch (IOException | RuntimeException failure) { failed(owner, failure); }
                        finally { Arrays.fill(password, '\0'); }
                    });
                } finally { if (!submitted && password != null) { Arrays.fill(password, '\0'); } }
            }
        } catch (RuntimeException presentationFailure) { finish(owner); }
    }
    private void installRestore(Job owner, ArchiveView.RestoreInput form, boolean recovery, Consumer<Path> openRestored) {
        Edt.require();
        if (!live(owner) || owner.finishing) { finish(owner); return; }
        ArchiveView.Destination fresh = form.destination();
        Path target = recovery ? form.recoveryTarget() : fresh == null ? null : fresh.path();
        if (target == null) { finish(owner); return; }
        try {
            owner.applying = true;
            view.archiveProgress(recovery ? "Resuming restore…" : "Restoring…", this::cancelFromProgress);
            boolean submitted = work(owner, () -> {
                RestoreResult receipt = null;
                String error = null;
                try {
                    BackupOutput.check(() -> owner.cancelled);
                    receipt = installer.install(owner.plan, fresh, target, recovery);
                } catch (IOException | RuntimeException failure) { error = failureMessage(failure); }
                RestoreResult outcome = receipt; String failure = error;
                SwingUtilities.invokeLater(() -> {
                    owner.applying = false;
                    Path open = null;
                    try {
                        if (job == owner && !owner.suppressed) {
                            view.archiveProgressClosed();
                            if (outcome != null && outcome.outcome() == RestoreResult.Outcome.COMPLETE) {
                                view.archiveStatus("Backup restored.");
                                if (form.open() && live(owner)) { open = target; }
                            } else {
                                String knowledge = outcome == null ? "Restoration could not finish. " + failure
                                        : switch (outcome.outcome()) {
                                            case UNCERTAIN -> "Some files may have been saved, but restoration could not be confirmed.";
                                            case PARTIAL -> "Only part of the backup was restored. Saved files will be kept.";
                                            default -> "Restoration could not begin. A newly allocated folder may remain.";
                                        };
                                view.archiveMessage("Restore incomplete", knowledge + "\nKeep the original ZIP and this folder, including hidden files:\n" + target
                                        + "\nUse Resume Interrupted Restore with the same exact ZIP. If recovery is refused, preserve this folder and restore into a different new folder.");
                            }
                        }
                    } finally { finishWithOpen(owner, open, openRestored); }
                });
            });
            if (!submitted) { owner.applying = false; finish(owner); }
        } catch (RuntimeException presentationFailure) { owner.applying = false; finish(owner); }
    }
    private void finishWithOpen(Job owner, Path requested, Consumer<Path> openRestored) {
        Edt.require();
        if (owner.finishing) { return; }
        owner.finishing = true;
        owner.executor.execute(() -> {
            boolean cleaned = true;
            try { if (owner.plan != null) { owner.plan.close(); } }
            catch (RuntimeException failure) { cleaned = false; }
            finally { owner.plan = null; }
            try { if (owner.history != null) { owner.history.close(); } }
            catch (RuntimeException failure) { cleaned = false; }
            finally { owner.history = null; }
            boolean cleanupSucceeded = cleaned;
            SwingUtilities.invokeLater(() -> {
                if (job != owner) { return; }
                boolean valid = live(owner); job = null; owner.executor.shutdown();
                try {
                    view.archiveProgressClosed();
                    if (valid && !cleanupSucceeded) { view.archiveMessage("Archive cleanup failed", "Resource cleanup failed. No automatic retry or opening was attempted."); }
                } finally { try { owner.retired.run(); } finally { changed.run(); } }
                if (valid && cleanupSucceeded && requested != null) { openRestored.accept(requested); }
            });
        });
    }
    private void failed(Job owner, Throwable failure) {
        String message = failureMessage(failure);
        SwingUtilities.invokeLater(() -> {
            try { if (live(owner)) { view.archiveMessage("Archive operation failed", message); } }
            finally { finish(owner); }
        });
    }
    static String failureMessage(Throwable failure) {
        if (failure instanceof FileAlreadyExistsException) { return "The destination already exists. Nothing was replaced. Choose a new name."; }
        if (failure instanceof BackupException backup) {
            return switch (backup.reason()) {
                case CAPACITY -> "This history is too large for a supported backup. Nothing was truncated. Keep the original vault and try a smaller supported backup.";
                case CANCELLED -> "Cancellation requested. Entered filesystem writes are not undone.";
                case AUTHENTICATION_FAILED -> "The password is incorrect or the backup is damaged. Check the password or try another backup copy.";
                case MALFORMED, UNSUPPORTED -> "This ZIP is damaged or uses an unsupported backup format. Choose another Totipo backup.";
                case INVALID_OBJECT -> "Encrypted history validation failed. Preserve the original data.";
                case VAULT_CHANGED -> "This backup does not match the open vault. Unlock its original vault and try again. Existing files were kept.";
                case TARGET_CONFLICT -> "Existing files differ from the backup. Keep them and try another intact copy.";
                default -> "The archive or source could not be safely used. Preserve the original data.";
            };
        }
        return "Could not safely use this location. Check access and free space, or choose another private local folder. Keep existing files.";
    }
}
