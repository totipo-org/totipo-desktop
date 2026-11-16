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
 * Cancellation invalidates presentation immediately, but retains the lease until cleanup.
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
        Edt.require(); if (job != null) { job.cancel(); view.archiveProgressClosed(); retireIfIdle(job); }
    }
    private void cancelFromProgress() {
        Edt.require();
        if (job != null) {
            job.cancel();
            view.archiveProgress("Cancellation requested; waiting for entered I/O…", () -> { });
            finish(job);
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
            synchronized (owner) { owner.running = Thread.currentThread(); }
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
    void backup(VaultSession session, Path source, Runnable retired) {
        Job owner = begin(retired); if (owner == null) { retired.run(); return; }
        try {
            Path output = view.chooseBackupOutput();
            if (!live(owner) || output == null || !view.confirmBackup(source, output) || !live(owner)) { finish(owner); return; }
            view.archiveProgress("Capturing locally available encrypted history…", this::cancelFromProgress);
            work(owner, () -> {
                String title = "Backup failed", message;
                try {
                    BackupOutput.check(() -> owner.cancelled);
                    try (var capture = session.captureBackup(BackupLimits.DEFAULT)) {
                        var receipt = BackupOutput.write(capture, source, output, () -> owner.cancelled, text -> phase(owner, text));
                        title = receipt.complete() ? "Backup completed" : "Backup output uncertain";
                        message = (receipt.complete() ? "Completed and verified the locally available encrypted history.\n"
                                : "A completed ZIP may exist, but publication persistence or final verification was not established. Preserve it and check it before relying on it.\n")
                                + output + "\nThis does not establish remote completeness or universal power-loss durability."
                                + (receipt.stagingRetained() ? "\nAn operation staging directory may remain beside the output." : "");
                    }
                } catch (IOException | RuntimeException failure) { message = failureMessage(failure); }
                String finalTitle = title, finalMessage = message;
                SwingUtilities.invokeLater(() -> {
                    try { if (live(owner)) { view.archiveMessage(finalTitle, finalMessage); } }
                    finally { finish(owner); }
                });
            });
        } catch (RuntimeException presentationFailure) { finish(owner); }
    }
    void restore(boolean recovery, Runnable retired, Consumer<Path> openRestored) {
        Job owner = begin(retired); if (owner == null) { retired.run(); return; }
        try {
            Path input = view.chooseBackupInput();
            if (!live(owner) || input == null) { finish(owner); return; }
            view.archiveProgress("Inspecting encrypted ZIP…", this::cancelFromProgress);
            work(owner, () -> {
                try {
                    BackupOutput.check(() -> owner.cancelled);
                    try (var stream = Files.newInputStream(input)) { BackupArchives.inspect(stream, BackupLimits.DEFAULT); }
                    SwingUtilities.invokeLater(() -> authenticate(owner, input, recovery, openRestored));
                } catch (IOException | RuntimeException failure) { failed(owner, failure); }
            });
        } catch (RuntimeException presentationFailure) { finish(owner); }
    }
    private void authenticate(Job owner, Path input, boolean recovery, Consumer<Path> openRestored) {
        if (!live(owner)) { finish(owner); return; }
        view.archiveProgressClosed();
        try (var result = view.archivePassword(input)) {
            char[] password = result.takePassword();
            if (password == null) { finish(owner); return; }
            boolean submitted = false;
            try {
                if (!live(owner) || !PasswordInput.valid(password)) {
                    if (live(owner)) { view.archiveMessage("Invalid password input", "Enter valid Unicode using at most 1024 UTF-8 bytes."); }
                    finish(owner); return;
                }
                view.archiveProgress("Authenticating vault and encrypted history…", this::cancelFromProgress);
                submitted = work(owner, () -> {
                try {
                    BackupOutput.check(() -> owner.cancelled);
                    try (var stream = Files.newInputStream(input)) { owner.plan = BackupArchives.prepareRestore(stream, BackupLimits.DEFAULT, password); }
                    SwingUtilities.invokeLater(() -> destination(owner, recovery, openRestored));
                } catch (IOException | RuntimeException failure) { failed(owner, failure); }
                finally { Arrays.fill(password, '\0'); }
                });
            } finally { if (!submitted) { Arrays.fill(password, '\0'); } }
        } catch (RuntimeException presentationFailure) { finish(owner); }
    }
    private void destination(Job owner, boolean recovery, Consumer<Path> openRestored) {
        if (!live(owner)) { finish(owner); return; }
        view.archiveProgressClosed();
        try {
            ArchiveView.Destination fresh = recovery ? null : view.chooseRestoreDestination();
            Path target = recovery ? view.chooseInterruptedRestore() : fresh == null ? null : fresh.path();
            if (!live(owner) || target == null || !view.confirmRestore(target, owner.plan.summary(), recovery) || !live(owner)) { finish(owner); return; }
            view.archiveProgress(recovery ? "Resuming exact restore…" : "Restoring to new folder…", this::cancelFromProgress);
            work(owner, () -> {
                RestoreResult receipt = null;
                String error = null;
                try {
                    BackupOutput.check(() -> owner.cancelled);
                    receipt = installer.install(owner.plan, fresh, target, recovery);
                } catch (IOException | RuntimeException failure) { error = failureMessage(failure); }
                RestoreResult outcome = receipt; String failure = error;
                SwingUtilities.invokeLater(() -> {
                    Path open = null;
                    try {
                        if (live(owner)) {
                            view.archiveProgressClosed();
                            if (outcome != null && outcome.outcome() == RestoreResult.Outcome.COMPLETE) {
                                if (view.restored(target) && live(owner)) { open = target; }
                            } else {
                                String knowledge = outcome == null ? "Allocation or restoration was not completed. " + failure
                                        : "Restore result: " + outcome.outcome() + ". Bootstrap: " + outcome.bootstrap()
                                        + ". Objects acknowledged: " + (outcome.objects().installed() + outcome.objects().retained())
                                        + ". Objects uncertain: " + outcome.objects().uncertain() + ".";
                                view.archiveMessage("Restore incomplete", knowledge + "\nPreserve this location, including any allocation/staging residue:\n" + target
                                        + "\nUse Resume Interrupted Restore with the same exact ZIP. If the marker is missing or damaged, preserve the folder for investigation and choose a different new folder. Nothing is automatically removed or repaired.");
                            }
                        }
                    } finally {
                        Path requested = open;
                        // Opening waits for plan/provider cleanup and archive admission retirement.
                        finishWithOpen(owner, requested, openRestored);
                    }
                });
            });
        } catch (RuntimeException presentationFailure) { finish(owner); }
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
                case CAPACITY -> "History exceeds the supported limit (4096 objects, 8192 observed entries, 6 MiB ZIP). No history was truncated.";
                case CANCELLED -> "Cancellation requested. Entered filesystem writes are not undone.";
                case AUTHENTICATION_FAILED -> "Authentication failed. The password or authenticated backup data may be unusable.";
                case MALFORMED, UNSUPPORTED -> "The ZIP is malformed or uses an unsupported archive profile.";
                case INVALID_OBJECT -> "Encrypted history validation failed. Preserve the original data.";
                default -> "The archive or source could not be safely used. Preserve the original data.";
            };
        }
        return "The filesystem or archive could not support safe completion. Check access, available space, path aliases, hard-link and file/directory force support. Preserve existing data.";
    }
}
