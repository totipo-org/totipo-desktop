package org.totipo.desktop.ui;

import java.awt.*;
import java.nio.file.Path;
import java.util.function.Function;
import javax.swing.*;

/** Presentation only: editing and browsing never allocate a restore destination. */
final class RestoreForm extends JPanel {
    private static final long serialVersionUID = 1L;
    final JTextField destination = new JTextField(32);
    final JPasswordField password = new JPasswordField(24);
    final JButton browse = new JButton("Browse…");
    final JCheckBox open = new JCheckBox("Open restored vault", true);
    final JLabel error = new JLabel(" ");
    private final boolean recovery;
    private transient Path zip;
    final JLabel file = new JLabel();
    final JButton chooseZip = new JButton("Choose ZIP…");

    RestoreForm(Path zip, boolean recovery, Function<Path, Path> chooseParent) {
        this(zip, recovery, chooseParent, () -> null);
    }
    RestoreForm(Path zip, boolean recovery, Function<Path, Path> chooseParent, java.util.function.Supplier<Path> chooseBackup) {
        super(new GridBagLayout()); Edt.require(); this.recovery = recovery;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        selectedZip(zip);
        file.setPreferredSize(new Dimension(0, file.getPreferredSize().height));
        file.setMinimumSize(new Dimension(0, file.getPreferredSize().height));
        file.putClientProperty("html.disable", Boolean.TRUE);
        DesktopStyle.input(destination); DesktopStyle.password(password);
        JPanel backup = new JPanel(new BorderLayout(8, 0)); backup.add(file, BorderLayout.CENTER); backup.add(chooseZip, BorderLayout.EAST);
        chooseZip.setMnemonic('Z');
        chooseZip.addActionListener(event -> {
            Path selected = chooseBackup.get();
            if (selected != null) { selectedZip(selected); }
            if (isShowing()) { chooseZip.requestFocusInWindow(); }
        });
        addRow(0, new JLabel("Backup"), backup);
        JPanel location = new JPanel(new BorderLayout(8, 0)); location.add(destination, BorderLayout.CENTER); location.add(browse, BorderLayout.EAST);
        addRow(1, SwingUsability.label(recovery ? "Interrupted folder" : "New folder path", destination), location);
        browse.setMnemonic('B');
        browse.addActionListener(event -> {
            Path initial = null;
            try { if (!destination.getText().isBlank()) { initial = Path.of(destination.getText()); } } catch (IllegalArgumentException invalid) { /* no selection */ }
            Path parent = chooseParent.apply(initial);
            if (parent != null) {
                String name = initial == null || initial.getFileName() == null ? "Restored Vault" : initial.getFileName().toString();
                destination.setText(recovery ? parent.toString() : parent.resolve(name).toString());
            }
            destination.requestFocusInWindow();
        });
        addRow(2, SwingUsability.label("Original vault password", password), password);
        addRow(3, new JLabel(), ArchiveDialogs.text(recovery ? "Use the same backup and retained folder." : "Creates a new folder. Existing folders will not be replaced."));
        addRow(4, new JLabel(), open); addRow(5, new JLabel(), error);
        error.putClientProperty("html.disable", Boolean.TRUE);
    }
    private void selectedZip(Path selected) {
        zip = selected;
        file.setText(UntrustedText.display(zip.getFileName().toString()));
        file.setToolTipText(UntrustedText.display(zip.toString()));
        file.getAccessibleContext().setAccessibleDescription(UntrustedText.display(zip.toString()));
    }
    private void addRow(int row, JComponent label, JComponent input) {
        GridBagConstraints c = new GridBagConstraints(); c.gridy = row; c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 0, 4, 12); add(label, c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(4, 0, 4, 0); add(input, c);
    }
    ArchiveView.RestoreInput submit() {
        try {
            if (destination.getText().isBlank()) { throw new IllegalArgumentException(); }
            Path target = Path.of(destination.getText());
            if (!target.isAbsolute()) { throw new IllegalArgumentException(); }
            target = target.normalize();
            if (target.getParent() == null || target.getFileName() == null) { throw new IllegalArgumentException(); }
            return new ArchiveView.RestoreInput(zip, recovery ? null : new ArchiveView.Destination(target.getParent(), target.getFileName().toString()),
                    recovery ? target : null, PasswordPromptResult.submitted(password.getPassword()), open.isSelected());
        } catch (IllegalArgumentException invalid) {
            error.setText(recovery ? "Enter the full interrupted folder path." : "Enter a full new folder path."); destination.requestFocusInWindow(); return null;
        }
    }
    void clear() { password.setText(""); }
}
