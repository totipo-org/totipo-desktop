package org.totipo.desktop.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.swing.*;

/** Restricted, read-only folder navigation. Selection validation belongs to the application. */
final class DirectoryPicker extends JPanel implements AutoCloseable {
    private static final long serialVersionUID = 1L;
    final JLabel location = new JLabel();
    final JLabel status = new JLabel();
    final JList<Path> directories = new JList<>(new DefaultListModel<>());
    final JScrollPane scroll = new JScrollPane(directories);
    final JButton up = new JButton("Up");
    final JButton select = new JButton("Select Folder");
    final JButton cancel = new JButton("Cancel");
    private final transient Consumer<Path> accepted;
    private final transient Runnable cancelled;
    private final transient Executor executor;
    private final transient ExecutorService ownedExecutor;
    private transient Path current;
    private long generation;
    private boolean retired;

    DirectoryPicker(Path initial, Consumer<Path> accepted, Runnable cancelled) {
        this(initial, accepted, cancelled, Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "totipo-folder-picker"); thread.setDaemon(true); return thread;
        }), true);
    }
    DirectoryPicker(Path initial, Consumer<Path> accepted, Runnable cancelled, Executor executor) {
        this(initial, accepted, cancelled, executor, false);
    }
    private DirectoryPicker(Path initial, Consumer<Path> accepted, Runnable cancelled, Executor executor, boolean own) {
        super(new BorderLayout(12, 12)); Edt.require();
        this.accepted = accepted; this.cancelled = cancelled; this.executor = executor;
        ownedExecutor = own ? (ExecutorService) executor : null;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setPreferredSize(new Dimension(720, 520));
        location.putClientProperty("html.disable", Boolean.TRUE);
        location.setFont(DesktopStyle.font(DesktopStyle.Typography.Secondary));
        location.setMinimumSize(new Dimension(0, location.getPreferredSize().height));
        location.getAccessibleContext().setAccessibleName("Current folder");
        JPanel navigation = new JPanel(new BorderLayout(12, 0));
        navigation.add(location, BorderLayout.CENTER); navigation.add(up, BorderLayout.EAST);
        add(navigation, BorderLayout.NORTH); add(scroll, BorderLayout.CENTER);
        directories.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        directories.getAccessibleContext().setAccessibleName("Folders");
        directories.getAccessibleContext().setAccessibleDescription("Select Folder chooses the highlighted folder, or the current folder if none is highlighted. Enter or double-click to enter a folder.");
        directories.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                Path folder = (Path) value;
                label.putClientProperty("html.disable", Boolean.TRUE);
                label.setText(UntrustedText.display(folder.getFileName() == null ? folder.toString() : folder.getFileName().toString()) + "/");
                label.setToolTipText(folder.toString());
                label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8)); return label;
            }
        });
        directories.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                int index = directories.locationToIndex(event.getPoint());
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event) && index >= 0
                        && directories.getCellBounds(index, index).contains(event.getPoint())) { enterSelected(); }
            }
        });
        SwingUsability.bind(directories, WHEN_FOCUSED, KeyStroke.getKeyStroke("ENTER"), "enter-folder",
                SwingUsability.action("Enter folder", this::enterSelected));
        up.addActionListener(event -> { if (!retired && current != null && current.getParent() != null) { navigate(current.getParent()); } });
        select.addActionListener(event -> {
            if (!retired && current != null) {
                Path child = directories.getSelectedValue();
                accepted.accept(child == null ? current : child);
            }
        });
        cancel.addActionListener(event -> { if (!retired) { cancelled.run(); } });
        DesktopStyle.action(up, DesktopStyle.ActionRole.SecondaryAction, false);
        DesktopStyle.action(cancel, DesktopStyle.ActionRole.SecondaryAction, false);
        DesktopStyle.action(select, DesktopStyle.ActionRole.PrimaryAction, false);
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        status.putClientProperty("html.disable", Boolean.TRUE); status.setVisible(false);
        footer.add(status, BorderLayout.NORTH); footer.add(SwingUsability.taskActions(select, cancel), BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        Path start = initial == null ? Path.of(System.getProperty("user.home")) : initial.toAbsolutePath().normalize();
        if (initial != null && start.getParent() != null) { start = start.getParent(); }
        navigate(start);
    }
    private record Listing(Path directory, List<Path> folders, boolean failed) { }
    void navigate(Path target) {
        Edt.require(); if (retired) { return; }
        long request = ++generation;
        current = target.toAbsolutePath().normalize(); updateLocation();
        directories.clearSelection();
        ((DefaultListModel<Path>) directories.getModel()).clear();
        status.setText("Reading folders…"); status.setVisible(true);
        Path requested = current;
        executor.execute(() -> {
            Path found = requested;
            List<Path> folders = List.of(); boolean failed = false;
            try {
                while (!Files.isDirectory(found) && found.getParent() != null) { found = found.getParent(); }
                try (var children = Files.list(found)) {
                    folders = children.filter(Files::isDirectory).filter(DirectoryPicker::visibleDirectory)
                            .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER)).toList();
                }
            } catch (IOException | SecurityException unavailable) { failed = true; }
            Listing result = new Listing(found, folders, failed);
            Runnable publish = () -> {
                if (retired || request != generation) { return; }
                current = result.directory(); updateLocation();
                var model = (DefaultListModel<Path>) directories.getModel(); model.clear(); model.addAll(result.folders());
                status.setText(result.failed() ? "This folder could not be listed. Try its parent or select another folder." : "");
                status.setVisible(result.failed()); revalidate(); repaint();
            };
            if (SwingUtilities.isEventDispatchThread()) { publish.run(); } else { SwingUtilities.invokeLater(publish); }
        });
    }
    static boolean visibleDirectory(Path path) {
        return visibleDirectory(path, Files::isHidden);
    }
    @FunctionalInterface interface HiddenCheck { boolean hidden(Path path) throws IOException; }
    static boolean visibleDirectory(Path path, HiddenCheck check) {
        if (path.getFileName().toString().startsWith(".")) { return false; }
        try { return !check.hidden(path); }
        catch (IOException | SecurityException unavailable) { return false; }
    }
    private void updateLocation() {
        location.setText(UntrustedText.display(current.toString())); location.setToolTipText(current.toString());
        location.getAccessibleContext().setAccessibleDescription(current.toString()); up.setEnabled(current.getParent() != null);
    }
    void enterSelected() { if (!retired && directories.getSelectedValue() != null) { navigate(directories.getSelectedValue()); } }
    Path currentDirectory() { return current; }
    @Override public void close() {
        Edt.require(); retired = true; generation++; current = null;
        ((DefaultListModel<Path>) directories.getModel()).clear();
        if (ownedExecutor != null) { ownedExecutor.shutdown(); }
    }
    static Path ask(java.awt.Window owner, Path initial) {
        Edt.require();
        JDialog dialog = new JDialog(owner, "Select Vault Folder", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        Path[] result = {null};
        DirectoryPicker picker = new DirectoryPicker(initial, path -> { result[0] = path; dialog.dispose(); }, dialog::dispose);
        dialog.setContentPane(picker); dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { dialog.dispose(); }
            @Override public void windowClosed(WindowEvent event) { picker.close(); }
            @Override public void windowOpened(WindowEvent event) { picker.directories.requestFocusInWindow(); }
        });
        SwingUsability.dialog(dialog.getRootPane(), picker.select, dialog::dispose);
        dialog.pack(); SwingUsability.fit(dialog, dialog.getWidth(), dialog.getHeight()); dialog.setLocationRelativeTo(owner);
        try { dialog.setVisible(true); return result[0]; }
        finally { picker.close(); dialog.dispose(); }
    }
}
