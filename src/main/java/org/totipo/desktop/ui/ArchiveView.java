package org.totipo.desktop.ui;

import java.nio.file.Path;
import org.totipo.backup.BackupSummary;

/** EDT-only archive presentation. Default dismissals keep non-GUI shell seams inert. */
public interface ArchiveView {
    record Destination(Path parent, String child) {
        public Path path() { return parent.resolve(child).toAbsolutePath().normalize(); }
    }
    default void archiveActions(Runnable backup, Runnable restore, Runnable recover) { }
    default void archiveAvailability(boolean backup, boolean restore) { }
    default Path chooseBackupOutput() { return null; }
    default Path chooseBackupInput() { return null; }
    default boolean confirmBackup(Path source, Path output) { return false; }
    default boolean confirmLockForRestore() { return false; }
    default PasswordPromptResult archivePassword(Path zip) { return PasswordPromptResult.dismissed(PasswordPromptResult.Action.CANCEL); }
    default Destination chooseRestoreDestination() { return null; }
    default Path chooseInterruptedRestore() { return null; }
    default boolean confirmRestore(Path destination, BackupSummary summary, boolean recovery) { return false; }
    default boolean restored(Path destination) { return false; }
    default void archiveProgress(String phase, Runnable cancel) { }
    default void archiveProgressClosed() { }
    default void archiveMessage(String title, String message) { }
}
