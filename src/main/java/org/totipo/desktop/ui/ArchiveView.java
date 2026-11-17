package org.totipo.desktop.ui;

import java.nio.file.Path;
import org.totipo.backup.BackupSummary;
import org.totipo.backup.ImportResult;

/** EDT-only archive presentation. Default dismissals keep non-GUI shell seams inert. */
public interface ArchiveView {
    record Destination(Path parent, String child) {
        public Path path() { return parent.resolve(child).toAbsolutePath().normalize(); }
    }
    default void archiveActions(Runnable backup, Runnable restore, Runnable recover) { }
    default void archiveAvailability(boolean backup, boolean restore) { }
    default void importAction(Runnable action) { }
    default void importAvailability(boolean available) { }
    default boolean confirmImport(Path zip, BackupSummary summary) { return false; }
    default boolean backupLocationFailure(String message) { archiveMessage("Backup failed", message); return false; }
    default void archiveStatus(String message) { }
    /** True requests explicit replay of the same owned, frozen import handle. */
    default boolean importResult(ImportResult result, boolean retryAllowed) {
        if (result.outcome() == ImportResult.Outcome.NO_OP || result.outcome() == ImportResult.Outcome.COMPLETE) {
            archiveStatus(importMessage(result));
        } else { archiveMessage("Add History from Backup", importMessage(result)); }
        return false;
    }
    static String importMessage(ImportResult result) {
        return switch (result.outcome()) {
            case NO_OP -> "No new history to add.";
            case COMPLETE -> "History added. Your vault may show new versions or conflicts.";
            case PARTIAL -> "Some history was added. The rest could not be added. Published history will be kept. " + importReason(result);
            case UNCERTAIN -> "History may have been added, but saving could not be confirmed. Published history will be kept. " + importReason(result);
            case FAILED -> "History could not be added. " + importReason(result);
        };
    }
    private static String importReason(ImportResult result) {
        return switch (result.reason()) {
            case CANCELLED, SESSION_CLOSING -> "The operation was stopped. Select the backup again to continue.";
            case VAULT_CHANGED -> "The vault changed. Unlock the matching vault and select the backup again.";
            case TARGET_CONFLICT -> "Existing files differ from the backup. Preserve them; try another intact vault copy.";
            default -> "Check storage access and available space, then retry.";
        };
    }
    /** Owns the password until taken by the worker; closing wipes an unclaimed password. */
    record RestoreInput(Path zip, Destination destination, Path recoveryTarget, PasswordPromptResult password, boolean open) implements AutoCloseable {
        @Override public void close() { password.close(); }
    }
    default RestoreInput restoreInput(Path zip, boolean recovery) { return null; }
    default Path chooseBackupOutput() { return null; }
    default Path chooseBackupInput() { return null; }
    default boolean confirmLimitedRestore() { return false; }
    default boolean confirmLockForRestore() { return false; }
    default void archiveProgress(String phase, Runnable cancel) { }
    default void archiveProgressClosed() { }
    default void archiveMessage(String title, String message) { }
}
