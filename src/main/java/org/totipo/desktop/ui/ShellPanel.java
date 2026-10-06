package org.totipo.desktop.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.function.Consumer;
import javax.swing.*;
import org.totipo.desktop.ShellState;

/** Non-sensitive shell states share one in-window form. */
public final class ShellPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    final JLabel identity = new JLabel("Totipo");
    final JLabel path = new JLabel();
    final JButton details = new JButton("Details…");
    final JPanel detailsRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING, 0, 0));
    final JLabel status = new JLabel("No vault selected");
    final JPasswordField password = new JPasswordField(24);
    final JLabel passwordLabel = SwingUsability.label("Password", password);
    final JLabel error = new JLabel();
    final JTextArea explanation = new JTextArea();
    final JButton primary = new JButton("Select Vault");
    final JButton secondary = new JButton("Create New Vault…");
    final JPanel task = new JPanel(new GridBagLayout());
    final JPanel actionRow = SwingUsability.taskActions(primary, secondary);
    final EmptyState welcome = new EmptyState(identity, explanation, 480);
    static final int MAX_TASK_WIDTH = 560;
    private transient Runnable select = () -> { };
    private transient Runnable create = () -> { };
    private transient Runnable retry = () -> { };
    private transient Consumer<char[]> open = buffer -> java.util.Arrays.fill(buffer, '\0');
    private ShellState state = ShellState.NO_VAULT;
    public ShellPanel() {
        super(null);
        Edt.require();
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        welcome.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        identity.setFont(DesktopStyle.font(DesktopStyle.Typography.ScreenTitle));
        identity.putClientProperty("html.disable", Boolean.TRUE);
        path.setFocusable(false); path.putClientProperty("html.disable", Boolean.TRUE);
        path.setFont(DesktopStyle.font(DesktopStyle.Typography.Secondary));
        path.setForeground(DesktopStyle.textSecondary());
        path.getAccessibleContext().setAccessibleName("Vault location");
        password.getAccessibleContext().setAccessibleName("Password");
        // Preserve JPasswordField's password delegate and echo/accessibility behavior.
        DesktopStyle.password(password);
        error.setForeground(DesktopStyle.danger());
        explanation.setEditable(false); explanation.setOpaque(false); explanation.setLineWrap(true); explanation.setWrapStyleWord(true);
        explanation.setFont(DesktopStyle.font(DesktopStyle.Typography.Body)); explanation.setForeground(DesktopStyle.danger());
        explanation.getAccessibleContext().setAccessibleName("Vault state details");
        DesktopStyle.action(details, DesktopStyle.ActionRole.QuietAction, false);
        detailsRow.setOpaque(false); detailsRow.add(details);
        DesktopStyle.action(primary, DesktopStyle.ActionRole.PrimaryAction, false);
        DesktopStyle.action(secondary, DesktopStyle.ActionRole.QuietAction, false);
        for (JComponent field : new JComponent[] {identity, path, status, passwordLabel, password, error}) {
            field.setMinimumSize(new Dimension(0, field.getPreferredSize().height));
        }
        add(task);
        primary.addActionListener(event -> activate());
        password.addActionListener(event -> activate());
        secondary.addActionListener(event -> { if (state == ShellState.NO_VAULT) { create.run(); } else { select.run(); } });
    }
    /** Bound width independently of long identity/path text; sparse tasks sit slightly above center. */
    @Override public void doLayout() {
        if (state == ShellState.NO_VAULT) { welcome.setBounds(0, 0, getWidth(), getHeight()); return; }
        Insets margins = getInsets();
        int availableWidth = Math.max(0, getWidth() - margins.left - margins.right);
        int availableHeight = Math.max(0, getHeight() - margins.top - margins.bottom);
        int width = Math.min(MAX_TASK_WIDTH, availableWidth);
        explanation.setSize(width, Integer.MAX_VALUE / 1024);
        explanation.setMinimumSize(new Dimension(0, explanation.getPreferredSize().height));
        task.invalidate();
        int height = Math.min(task.getPreferredSize().height, availableHeight);
        task.setBounds(margins.left + (availableWidth - width) / 2,
                margins.top + (int) ((availableHeight - height) * .42), width, height);
    }
    @Override public Dimension getPreferredSize() { return new Dimension(640, 520); }
    private void layoutTask() {
        task.removeAll();
        removeAll();
        if (state == ShellState.NO_VAULT) {
            add(welcome); welcome.compose(primary, secondary); return;
        }
        add(task); identity.setHorizontalAlignment(SwingConstants.LEADING); error.setHorizontalAlignment(SwingConstants.LEADING);
        actionRow.removeAll(); actionRow.add(secondary); actionRow.add(primary);
        int row = taskLine(identity, 0, 0);
        row = taskLine(path, row, 8);
        if (status.isVisible()) { row = taskLine(status, row, 16); }
        if (password.isVisible()) {
            row = taskLine(passwordLabel, row, 24);
            row = taskLine(password, row, 8);
        }
        if (error.isVisible()) { row = taskLine(explanation, row, 12); }
        row = taskLine(actionRow, row, 24);
        if (details.isVisible()) { taskLine(detailsRow, row, 8); }
        task.revalidate();
    }
    private int taskLine(JComponent component, int row, int gap) {
        if (component != actionRow) {
            component.setMinimumSize(new Dimension(0, Math.max(component.getPreferredSize().height,
                    component.getFontMetrics(component.getFont()).getHeight())));
        }
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0; constraints.gridy = row; constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL; constraints.anchor = GridBagConstraints.LINE_START;
        constraints.insets = new Insets(gap, 0, 0, 0);
        task.add(component, constraints);
        return row + 1;
    }
    public void actions(Runnable select, Runnable create, Consumer<char[]> open) {
        this.select = select; this.create = create; this.open = open;
    }
    public void detailsAction(Runnable action) { details.addActionListener(event -> action.run()); }
    public void retryAction(Runnable action) { retry = action; }
    private void activate() {
        if (!primary.isEnabled()) { return; }
        if (state == ShellState.NO_VAULT) { select.run(); }
        else if (state == ShellState.BLOCKING_VAULT_STATE) {
            retry.run();
        } else {
            char[] submitted = password.getPassword(); password.setText("");
            try { open.accept(submitted); }
            catch (RuntimeException failure) { java.util.Arrays.fill(submitted, '\0'); throw failure; }
        }
    }
    private transient Path currentPath;
    public void render(ShellState next, Path selected, String notice, boolean busy) {
        Edt.require();
        if (next != state || !java.util.Objects.equals(selected, currentPath)) { clear(); }
        state = next; currentPath = selected;
        identity.setText(next == ShellState.NO_VAULT ? "Choose a vault to continue" : selected == null ? "Totipo"
                : UntrustedText.display(selected.getFileName() == null ? selected.toString() : selected.getFileName().toString()));
        path.setText(selected == null ? "" : selected.toString()); path.setVisible(selected != null);
        path.setToolTipText(selected == null ? null : selected.toString());
        path.getAccessibleContext().setAccessibleDescription(selected == null ? null : selected.toString());
        status.setText(next == ShellState.NO_VAULT || next == ShellState.LOCKED ? "" : "Totipo can’t safely open this vault");
        status.setVisible(!status.getText().isEmpty());
        password.setVisible(next == ShellState.LOCKED); passwordLabel.setVisible(next == ShellState.LOCKED);
        details.setVisible(next == ShellState.BLOCKING_VAULT_STATE);
        details.setEnabled(!busy);
        error.setText(notice); error.setVisible(!notice.isEmpty());
        explanation.setText(notice); explanation.setVisible(!notice.isEmpty());
        explanation.setForeground(next == ShellState.NO_VAULT ? DesktopStyle.textSecondary() : DesktopStyle.danger());
        error.setToolTipText(notice.isEmpty() ? null : notice);
        primary.setText(next == ShellState.NO_VAULT ? "Select Vault" : next == ShellState.BLOCKING_VAULT_STATE ? "Try Again" : "Open");
        secondary.setText(next == ShellState.NO_VAULT ? "Create New Vault…" : "Change Vault…");
        primary.setEnabled(!busy); secondary.setEnabled(!busy); password.setEnabled(!busy);
        layoutTask();
        if (getRootPane() != null) { getRootPane().setDefaultButton(primary); }
        revalidate(); repaint();
    }
    public void clear() { password.setText(""); }
}
