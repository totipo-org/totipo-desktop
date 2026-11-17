package org.totipo.desktop.ui;

import java.awt.Dialog;
import java.awt.FileDialog;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.edt;

class NativeArchiveChooserTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 12, 34, 56);
    private static final class Peer implements NativeArchiveChooser.SelectionDialog {
        Dialog.ModalityType modality;
        boolean multiple = true;
        String suggestion;
        FilenameFilter filter;
        String file = "chosen.zip", directory = System.getProperty("java.io.tmpdir");
        Runnable duringShow = () -> { };
        boolean fail;
        int shown, disposed;
        public void modality(Dialog.ModalityType value) { Edt.require(); modality = value; }
        public void multiple(boolean value) { multiple = value; }
        public void suggestedFile(String value) { suggestion = value; }
        public void filter(FilenameFilter value) { filter = value; }
        public void show() { Edt.require(); shown++; duringShow.run(); if (fail) { throw new IllegalStateException("private error"); } }
        public String file() { return file; }
        public String directory() { return directory; }
        public void dispose() { Edt.require(); disposed++; }
    }
    @SafeVarargs private static <W> NativeArchiveChooser.Owner<W> owner(W identity, boolean showing, boolean modal, NativeArchiveChooser.Owner<W>... children) {
        java.util.ArrayList<NativeArchiveChooser.Owner<W>> owned = new java.util.ArrayList<>();
        for (var child : children) { owned.add(child); }
        return new NativeArchiveChooser.Owner<>(identity, showing, modal, List.copyOf(owned));
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void exactParentModeModalityAndSaveSuggestionReachNativeAdapter(boolean save) throws Exception {
        edt(() -> {
            Object shell = new Object(); Peer peer = new Peer(); NativeArchiveChooser chooser = new NativeArchiveChooser();
            Path path = chooser.choose(owner(shell, true, false), shell, save, NOW, (parent, title, mode) -> {
                assertSame(shell, parent); assertEquals(save ? FileDialog.SAVE : FileDialog.LOAD, mode);
                assertEquals(save ? "Back Up Vault" : "Choose Totipo Backup ZIP", title); return peer;
            });
            assertEquals(Path.of(peer.directory, "chosen.zip"), path); assertEquals(1, peer.shown); assertEquals(1, peer.disposed);
            assertEquals(Dialog.ModalityType.APPLICATION_MODAL, peer.modality); assertFalse(peer.multiple);
            assertEquals(save ? "totipo-backup-20261116-123456.zip" : null, peer.suggestion);
            assertTrue(peer.filter.accept(null, "BACKUP.ZIP")); assertFalse(peer.filter.accept(null, "backup.txt"));
        });
    }
    @Test void activeModalDescendantIsParentAndHiddenOrUnrelatedWindowsCannotHijackIt() {
        Object shell = new Object(), editor = new Object(), form = new Object(), nested = new Object(), unrelated = new Object();
        var tree = owner(shell, true, false, owner(editor, true, false), owner(form, true, true, owner(nested, true, true)));
        assertSame(nested, NativeArchiveChooser.liveParent(tree, form)); // child may be opening before activation catches up
        assertSame(nested, NativeArchiveChooser.liveParent(tree, nested));
        assertSame(nested, NativeArchiveChooser.liveParent(tree, unrelated));
        assertSame(shell, NativeArchiveChooser.liveParent(owner(shell, true, false, owner(form, false, true)), form));
        assertNull(NativeArchiveChooser.liveParent(owner(form, false, true, owner(nested, true, true)), nested));
        assertSame(editor, NativeArchiveChooser.liveParent(owner(editor, true, true), editor));
        var branches = owner(shell, true, false, owner(form, true, true), owner(nested, true, true));
        assertSame(form, NativeArchiveChooser.liveParent(branches, form));
    }
    @Test void nestedFormSelectionUsesFormIdentityAndReturnsEvenWhenFilterIsIgnored() throws Exception {
        edt(() -> {
            Object shell = new Object(), form = new Object(); Peer peer = new Peer(); peer.file = "unusual-name";
            var tree = owner(shell, true, false, owner(form, true, true));
            Path selected = new NativeArchiveChooser().choose(tree, form, false, NOW, (parent, title, mode) -> {
                assertSame(form, parent); return peer;
            });
            assertEquals(Path.of(peer.directory, "unusual-name"), selected); // Java validation, not filename filtering, decides validity
        });
    }
    @Test void cancellationDisposesAndASecondSelectionCanStart() throws Exception {
        edt(() -> {
            Object shell = new Object(); NativeArchiveChooser chooser = new NativeArchiveChooser(); Peer cancel = new Peer(); cancel.file = null;
            assertNull(chooser.choose(owner(shell, true, false), shell, true, NOW, (p, t, m) -> cancel));
            assertEquals(1, cancel.disposed); Peer next = new Peer();
            assertNotNull(chooser.choose(owner(shell, true, false), shell, false, NOW, (p, t, m) -> next));
            assertEquals(1, next.disposed);
        });
    }
    @Test void retirementDuringNestedEventLoopDisposesAndRejectsStaleSelectionAndDuplicateAdmission() throws Exception {
        edt(() -> {
            Object shell = new Object(); var root = owner(shell, true, false); NativeArchiveChooser chooser = new NativeArchiveChooser();
            Peer peer = new Peer(); AtomicInteger duplicates = new AtomicInteger();
            peer.duringShow = () -> {
                assertNull(chooser.choose(root, shell, false, NOW, (p, t, m) -> { duplicates.incrementAndGet(); return new Peer(); }));
                chooser.retire();
            };
            assertNull(chooser.choose(root, shell, true, NOW, (p, t, m) -> peer));
            assertEquals(0, duplicates.get()); assertEquals(2, peer.disposed); // retire, then idempotent finally
            assertNotNull(chooser.choose(root, shell, false, NOW, (p, t, m) -> new Peer()));
        });
    }
    @Test void presentationFailureStillDisposesAndHiddenParentNeverConstructsDialog() throws Exception {
        edt(() -> {
            Object shell = new Object(); NativeArchiveChooser chooser = new NativeArchiveChooser(); Peer peer = new Peer(); peer.fail = true;
            assertThrows(IllegalStateException.class, () -> chooser.choose(owner(shell, true, false), shell, false, NOW, (p, t, m) -> peer));
            assertEquals(1, peer.disposed);
            assertNull(chooser.choose(owner(shell, false, false), shell, true, NOW, (p, t, m) -> { fail("retired owner used"); return peer; }));
        });
    }
}
