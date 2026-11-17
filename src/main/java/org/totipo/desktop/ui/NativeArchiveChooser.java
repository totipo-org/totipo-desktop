package org.totipo.desktop.ui;

import java.awt.*;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** One synchronous native chooser, including nested EDT retirement. No filesystem authority. */
final class NativeArchiveChooser {
    /** Immutable ownership snapshot; identity stays the real Window in production. */
    record Owner<W>(W window, boolean showing, boolean modal, List<Owner<W>> children) { }
    interface SelectionDialog {
        void modality(Dialog.ModalityType type);
        void multiple(boolean enabled);
        void suggestedFile(String file);
        void filter(FilenameFilter filter);
        void show();
        String file();
        String directory();
        void dispose();
    }
    @FunctionalInterface interface Factory<W> {
        SelectionDialog create(W owner, String title, int mode);
    }
    private SelectionDialog active;
    private long generation;

    Path choose(Window parent, boolean save) {
        Edt.require();
        return choose(snapshot(parent), KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow(),
                save, LocalDateTime.now(), NativeArchiveChooser::create);
    }
    private static Owner<Window> snapshot(Window window) {
        if (window == null) { return null; }
        return new Owner<>(window, window.isShowing() && window.isDisplayable(),
                window instanceof Dialog dialog && dialog.isModal(),
                java.util.Arrays.stream(window.getOwnedWindows()).map(NativeArchiveChooser::snapshot).toList());
    }
    /** Prefer the active modal descendant; otherwise the deepest visible modal blocker.
     * Never parent a new chooser to an invisible/retired requested window. */
    static <W> W liveParent(Owner<W> root, W activeWindow) {
        if (root == null || !root.showing()) { return null; }
        for (Owner<W> child : root.children()) {
            if (containsModal(child) && contains(child, activeWindow)) { return liveParent(child, activeWindow); }
        }
        for (Owner<W> child : root.children().reversed()) {
            if (containsModal(child)) { return liveParent(child, activeWindow); }
        }
        return root.window();
    }
    private static <W> boolean containsModal(Owner<W> owner) {
        return owner.showing() && (owner.modal() || owner.children().stream().anyMatch(NativeArchiveChooser::containsModal));
    }
    private static <W> boolean contains(Owner<W> owner, W window) {
        return owner.showing() && (owner.window() == window || owner.children().stream().anyMatch(child -> contains(child, window)));
    }
    <W> Path choose(Owner<W> requested, W activeWindow, boolean save, LocalDateTime now, Factory<W> factory) {
        Edt.require();
        W parent = liveParent(requested, activeWindow);
        if (parent == null || active != null) { return null; }
        long attempt = generation;
        SelectionDialog dialog = factory.create(parent, save ? "Back Up Vault" : "Choose Totipo Backup ZIP",
                save ? FileDialog.SAVE : FileDialog.LOAD);
        active = dialog;
        try {
            dialog.modality(Dialog.ModalityType.APPLICATION_MODAL);
            dialog.multiple(false);
            dialog.filter((directory, name) -> name.toLowerCase(Locale.ROOT).endsWith(".zip"));
            if (save) { dialog.suggestedFile("totipo-backup-" + now.format(DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss")) + ".zip"); }
            if (attempt != generation) { return null; }
            dialog.show();
            if (attempt != generation || dialog.file() == null || dialog.directory() == null) { return null; }
            return Path.of(dialog.directory(), dialog.file());
        } finally {
            if (active == dialog) { active = null; }
            dialog.dispose();
        }
    }
    void retire() {
        Edt.require(); generation++;
        if (active != null) { active.dispose(); }
    }
    private static SelectionDialog create(Window parent, String title, int mode) {
        FileDialog dialog;
        if (parent instanceof Dialog modal) { dialog = new FileDialog(modal, title, mode); }
        else if (parent instanceof Frame frame) { dialog = new FileDialog(frame, title, mode); }
        else { throw new IllegalArgumentException("A live frame or dialog is required"); }
        return new SelectionDialog() {
            public void modality(Dialog.ModalityType type) { dialog.setModalityType(type); }
            public void multiple(boolean enabled) { dialog.setMultipleMode(enabled); }
            public void suggestedFile(String file) { dialog.setFile(file); }
            public void filter(FilenameFilter filter) { dialog.setFilenameFilter(filter); }
            public void show() { if (parent.isShowing()) { dialog.setLocationRelativeTo(parent); dialog.setVisible(true); } }
            public String file() { return dialog.getFile(); }
            public String directory() { return dialog.getDirectory(); }
            public void dispose() { dialog.dispose(); }
        };
    }
}
