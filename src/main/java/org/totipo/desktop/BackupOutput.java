package org.totipo.desktop;

import org.totipo.backup.*;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import static java.nio.file.StandardOpenOption.*;

/** Complete-stage, exclusive hard-link publication. No move/exists-check fallback.
 * Requires a trusted local POSIX parent, hard links and directory force. Same-user
 * hostile namespace replacement is outside the local-provider threat model.
 */
final class BackupOutput {
    record Receipt(boolean complete, boolean publicationMayExist, boolean stagingRetained) { }
    interface Barriers {
        default java.io.OutputStream output(FileChannel file) { return Channels.newOutputStream(file); }
        default void file(FileChannel file) throws IOException { file.force(true); }
        default void directory(Path path) throws IOException {
            try (var channel = FileChannel.open(path, READ)) { channel.force(true); }
        }
        default void publish(Path target, Path stage) throws IOException { Files.createLink(target, stage); }
    }
    private static final Barriers NIO = new Barriers() { };
    private BackupOutput() { }
    static Receipt write(BackupCapture capture, Path source, Path destination,
                         BooleanSupplier cancelled, Consumer<String> progress) throws IOException {
        return write(capture, source, destination, cancelled, progress, NIO);
    }
    static Receipt write(BackupCapture capture, Path source, Path destination,
                         BooleanSupplier cancelled, Consumer<String> progress, Barriers barriers) throws IOException {
        Path target = destination.toAbsolutePath().normalize();
        Path parent = target.getParent();
        if (parent == null || target.getFileName() == null || !target.getFileSystem().equals(FileSystems.getDefault())) {
            throw new IOException("UNSAFE_OUTPUT");
        }
        Path realSource = source.toRealPath();
        Path realParent = parent.toRealPath();
        if (!parent.equals(realParent) || realParent.startsWith(realSource)) { throw new IOException("UNSAFE_OUTPUT"); }
        // Also catch an ancestor alias of the source root (for example an ordinary bind alias).
        for (Path ancestor = realParent; ancestor != null; ancestor = ancestor.getParent()) {
            if (Files.isSameFile(ancestor, realSource)) { throw new IOException("UNSAFE_OUTPUT"); }
        }
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(parent, LinkOption.NOFOLLOW_LINKS);
        if (permissions.contains(PosixFilePermission.GROUP_WRITE) || permissions.contains(PosixFilePermission.OTHERS_WRITE)) {
            throw new IOException("UNSAFE_PARENT");
        }
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) { throw new FileAlreadyExistsException("OUTPUT_PRESENT"); }
        check(cancelled);
        barriers.directory(parent); // Capability preflight; no output allocated yet.
        Path staging = Files.createTempDirectory(parent, ".totipo-backup-", PosixFilePermissions.asFileAttribute(
                PosixFilePermissions.fromString("rwx------")));
        Path stage = staging.resolve("archive.part");
        boolean entered = false, complete = false, retained = false;
        try {
            progress.accept("Writing encrypted ZIP…");
            try (var channel = FileChannel.open(stage, CREATE_NEW, WRITE, LinkOption.NOFOLLOW_LINKS)) {
                // Closing the borrowed output is owned here, independently of ZIP finish.
                try (var output = barriers.output(channel)) {
                    capture.writeTo(output); output.flush(); barriers.file(channel);
                }
            }
            check(cancelled);
            progress.accept("Verifying completed ZIP…");
            verify(stage, capture.summary());
            barriers.directory(staging);
            check(cancelled);
            progress.accept("Publishing completed ZIP…");
            entered = true;
            try { barriers.publish(target, stage); }
            catch (FileAlreadyExistsException collision) { entered = false; throw collision; }
            barriers.directory(parent);
            verify(target, capture.summary());
            complete = true;
        } catch (IOException | RuntimeException failure) {
            if (!entered) { throw failure; }
            // A failed link/barrier/readback may already have published the complete stage.
            // Keep final output; never remove a potentially visible user backup.
        } finally {
            try { Files.deleteIfExists(stage); Files.delete(staging); barriers.directory(parent); }
            catch (IOException | RuntimeException cleanupFailure) { retained = true; }
        }
        return new Receipt(complete, entered, retained);
    }
    private static void verify(Path path, BackupSummary expected) throws IOException {
        try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            if (!BackupArchives.inspect(input, BackupLimits.DEFAULT).archiveSha256().equals(expected.archiveSha256())) {
                throw new IOException("OUTPUT_MISMATCH");
            }
        }
    }
    static void check(BooleanSupplier cancelled) throws BackupException {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
            throw new BackupException(BackupException.Reason.CANCELLED);
        }
    }
}
