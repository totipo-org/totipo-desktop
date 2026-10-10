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

/** App-owned folder navigation. Vault recognition and creation belong to the application. */
final class DirectoryPicker extends JPanel implements AutoCloseable {
    private static final long serialVersionUID = 1L;
    enum Mode { EXISTING_VAULT, NEW_VAULT }
    final JLabel location = new JLabel();
    final JLabel status = new JLabel();
    final JList<Path> directories = new JList<>(new DefaultListModel<>());
    final JScrollPane scroll = new JScrollPane(directories);
    final JButton up = new JButton("Up");
    final JButton select = new JButton("Select Folder");
    final JButton cancel = new JButton("Cancel");
    final JTextField folderName = new JTextField(18);
    final JButton newFolder = new JButton("New Folder");
    private final Mode mode;
    @FunctionalInterface interface DirectoryCreator { void create(Path path) throws IOException; }
    private final transient DirectoryCreator creator;
    private final transient Consumer<Path> accepted;
    private final transient Runnable cancelled;
    private final transient Executor executor;
    private final transient ExecutorService ownedExecutor;
    private transient Path current;
    private long generation;
    private volatile boolean retired;
    private boolean creating;

    DirectoryPicker(Path initial, Consumer<Path> accepted, Runnable cancelled) {
        this(initial, Mode.EXISTING_VAULT, accepted, cancelled);
    }
    DirectoryPicker(Path initial, Mode mode, Consumer<Path> accepted, Runnable cancelled) {
        this(initial, mode, accepted, cancelled, Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "totipo-folder-picker"); thread.setDaemon(true); return thread;
        }), true, Files::createDirectory);
    }
    DirectoryPicker(Path initial, Consumer<Path> accepted, Runnable cancelled, Executor executor) {
        this(initial, Mode.EXISTING_VAULT, accepted, cancelled, executor);
    }
    DirectoryPicker(Path initial, Mode mode, Consumer<Path> accepted, Runnable cancelled, Executor executor) {
        this(initial, mode, accepted, cancelled, executor, false, Files::createDirectory);
    }
    DirectoryPicker(Path initial, Mode mode, Consumer<Path> accepted, Runnable cancelled, Executor executor, DirectoryCreator creator) {
        this(initial, mode, accepted, cancelled, executor, false, creator);
    }
    private DirectoryPicker(Path initial, Mode mode, Consumer<Path> accepted, Runnable cancelled, Executor executor,
                            boolean own, DirectoryCreator creator) {
        super(new BorderLayout(12, 0)); Edt.require();
        this.mode = mode; this.creator = creator;
        this.accepted = accepted; this.cancelled = cancelled; this.executor = executor;
        ownedExecutor = own ? (ExecutorService) executor : null;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 0, 16));
        setPreferredSize(new Dimension(720, 520));
        location.putClientProperty("html.disable", Boolean.TRUE);
        location.setFont(DesktopStyle.font(DesktopStyle.Typography.Secondary));
        location.setMinimumSize(new Dimension(0, location.getPreferredSize().height));
        location.getAccessibleContext().setAccessibleName("Current folder");
        JPanel navigation = new JPanel(new BorderLayout(12, 0));
        navigation.add(location, BorderLayout.CENTER); navigation.add(up, BorderLayout.EAST);
        if (mode == Mode.NEW_VAULT) {
            JPanel creation = new JPanel(new BorderLayout(8, 8));
            JLabel label = SwingUsability.label("Folder name", folderName); label.setDisplayedMnemonic(KeyEvent.VK_F);
            DesktopStyle.input(folderName);
            DesktopStyle.action(newFolder, DesktopStyle.ActionRole.SecondaryAction, false);
            newFolder.setMnemonic(KeyEvent.VK_N); newFolder.getAccessibleContext().setAccessibleName("New Folder");
            newFolder.addActionListener(event -> createChild(folderName.getText()));
            folderName.addActionListener(event -> createChild(folderName.getText()));
            creation.add(label, BorderLayout.WEST); creation.add(folderName, BorderLayout.CENTER);
            creation.add(newFolder, BorderLayout.EAST);
            creation.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            navigation.add(creation, BorderLayout.SOUTH);
        }
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
        up.setMnemonic(KeyEvent.VK_U); select.setMnemonic(KeyEvent.VK_S); cancel.setMnemonic(KeyEvent.VK_C);
        for (JButton button : new JButton[] {up, select, cancel}) {
            button.getAccessibleContext().setAccessibleName(button.getText());
        }
        SwingUsability.bind(this, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyStroke.getKeyStroke("ESCAPE"),
                "cancel-picker", SwingUsability.action("Cancel", () -> cancel.doClick(0)));
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
    /** One ordinary child only; no recursive creation or protocol-file writes. */
    void createChild(String name) {
        Edt.require();
        if (retired || creating || mode != Mode.NEW_VAULT || current == null || !newFolder.isEnabled()) { return; }
        Path child;
        try {
            Path relative = current.getFileSystem().getPath(name);
            if (name.isBlank() || relative.isAbsolute() || relative.getNameCount() != 1
                    || name.equals(".") || name.equals("..") || name.contains("/") || name.contains("\\")
                    || name.equalsIgnoreCase("vault") || name.equalsIgnoreCase("objects-v1")) {
                showStatus("Enter a single folder name other than vault or objects-v1."); return;
            }
            child = current.resolve(relative);
        } catch (java.nio.file.InvalidPathException invalid) {
            showStatus("Enter a valid folder name."); return;
        }
        creating = true; long request = ++generation;
        setBrowsingEnabled(false); showStatus("Creating folder…");
        executor.execute(() -> {
            if (retired) { return; }
            String failure = null;
            try { creator.create(child); }
            catch (java.nio.file.FileAlreadyExistsException collision) { failure = "A file or folder with that name already exists. Choose another name."; }
            catch (IOException | SecurityException unavailable) { failure = "This folder could not be created. Check permissions or choose another folder."; }
            String message = failure;
            Runnable publish = () -> {
                if (retired || request != generation) { return; }
                creating = false;
                if (message == null) { folderName.setText(""); navigate(child); directories.requestFocusInWindow(); }
                else { setBrowsingEnabled(true); showStatus(message); }
            };
            if (SwingUtilities.isEventDispatchThread()) { publish.run(); } else { SwingUtilities.invokeLater(publish); }
        });
    }
    private void showStatus(String message) { status.setText(message); status.setVisible(true); }
    private void setBrowsingEnabled(boolean enabled) {
        select.setEnabled(enabled); directories.setEnabled(enabled);
        up.setEnabled(enabled && current != null && current.getParent() != null);
        newFolder.setEnabled(enabled); folderName.setEnabled(enabled);
    }
    void navigate(Path target) {
        Edt.require(); if (retired || creating) { return; }
        long request = ++generation;
        current = target.toAbsolutePath().normalize(); updateLocation();
        setBrowsingEnabled(false);
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
                setBrowsingEnabled(true);
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
    static Path ask(java.awt.Window owner, Path initial, Mode mode) {
        Edt.require();
        String title = mode == Mode.NEW_VAULT ? "Select New Vault Folder" : "Select Vault Folder";
        JDialog dialog = new JDialog(owner, title, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        Path[] result = {null};
        DirectoryPicker picker = new DirectoryPicker(initial, mode, path -> { result[0] = path; dialog.dispose(); }, dialog::dispose);
        picker.getAccessibleContext().setAccessibleName(title);
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
