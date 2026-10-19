package org.totipo.desktop.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

/** Non-secret shell content. Registry actions never access vault files. */
public final class VaultSelectionPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    final JList<Path> vaults = new JList<>(new DefaultListModel<>()) {
        private static final long serialVersionUID = 1L;
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
    };
    final JButton select = new JButton("Select Vault");
    final JButton browse = new JButton("Browse…");
    final JButton create = new JButton("New Vault…");
    final JButton forget = new JButton("Forget Entry…");
    final JButton back = new JButton("Return to Locked Vault");
    final JTextArea notice = new JTextArea();
    private transient List<Path> paths = List.of();
    private transient Path highlighted;
    private boolean busy;

    VaultSelectionPanel(Consumer<VaultSelection> action) {
        super(new BorderLayout(DesktopStyle.NORMAL, DesktopStyle.SECTION)); Edt.require();
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JLabel heading = new JLabel("Choose a vault");
        heading.setFont(DesktopStyle.font(DesktopStyle.Typography.ScreenTitle));
        JPanel header = new JPanel(new BorderLayout(0, DesktopStyle.TIGHT));
        header.add(heading, BorderLayout.NORTH);
        header.add(SwingUsability.label("Remembered vaults", vaults), BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);
        vaults.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vaults.setFont(DesktopStyle.font(DesktopStyle.Typography.Body));
        vaults.getAccessibleContext().setAccessibleName("Remembered vaults");
        vaults.getAccessibleContext().setAccessibleDescription("Vault name and full directory path. Select a vault to open its locked form.");
        vaults.setCellRenderer(new VaultCellRenderer());
        ToolTipManager.sharedInstance().registerComponent(vaults);
        vaults.addListSelectionListener(event -> updateActions());
        JScrollPane scroll = new JScrollPane(vaults); scroll.setPreferredSize(new Dimension(480, 220));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(0, DesktopStyle.NORMAL));
        notice.setEditable(false); notice.setOpaque(false); notice.setLineWrap(true); notice.setWrapStyleWord(true);
        notice.setFont(DesktopStyle.font(DesktopStyle.Typography.Body));
        notice.getAccessibleContext().setAccessibleName("Vault selection status");
        footer.add(notice, BorderLayout.NORTH);
        JPanel actions = new JPanel(new java.awt.GridLayout(0, 1, 0, DesktopStyle.NORMAL));
        JPanel management = new JPanel(new java.awt.GridLayout(1, 3, DesktopStyle.TIGHT, 0));
        management.add(browse); management.add(create); management.add(forget);
        JPanel navigation = new JPanel(new java.awt.GridLayout(1, 0, DesktopStyle.TIGHT, 0));
        navigation.add(back); navigation.add(select);
        actions.add(management); actions.add(navigation); footer.add(actions, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        JButton[] buttons = {browse, create, forget, back, select};
        int[] mnemonics = {KeyEvent.VK_B, KeyEvent.VK_N, KeyEvent.VK_F, KeyEvent.VK_R, KeyEvent.VK_S};
        for (int i = 0; i < buttons.length; i++) {
            JButton button = buttons[i]; button.setMnemonic(mnemonics[i]);
            DesktopStyle.action(button, button == select ? DesktopStyle.ActionRole.PrimaryAction
                    : button == forget ? DesktopStyle.ActionRole.DestructiveAction : DesktopStyle.ActionRole.SecondaryAction, false);
            button.getAccessibleContext().setAccessibleName(button.getText());
        }
        forget.setToolTipText("Remove only this installation’s remembered entry. No vault files are deleted or changed.");
        forget.getAccessibleContext().setAccessibleDescription(forget.getToolTipText());
        select.addActionListener(event -> {
            if (!busy && vaults.getSelectedValue() != null) { action.accept(VaultSelection.select(vaults.getSelectedValue())); }
        });
        browse.addActionListener(event -> action.accept(new VaultSelection(VaultSelection.Action.BROWSE, null)));
        create.addActionListener(event -> action.accept(new VaultSelection(VaultSelection.Action.CREATE, null)));
        back.addActionListener(event -> action.accept(new VaultSelection(VaultSelection.Action.RETURN, null)));
        forget.addActionListener(event -> action.accept(new VaultSelection(VaultSelection.Action.FORGET, vaults.getSelectedValue())));
        SwingUsability.bind(vaults, JComponent.WHEN_FOCUSED, KeyStroke.getKeyStroke("ENTER"), "select-vault",
                SwingUsability.action("Select Vault", () -> select.doClick(0)));
        vaults.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                int index = vaults.locationToIndex(event.getPoint());
                if (!busy && SwingUtilities.isLeftMouseButton(event) && event.getClickCount() == 2 && index >= 0
                        && vaults.getCellBounds(index, index).contains(event.getPoint())) {
                    vaults.setSelectedIndex(index); select.doClick(0);
                }
            }
        });
    }
    void render(List<Path> remembered, Path selected, boolean canReturn, boolean working) {
        Edt.require(); busy = working;
        if (!paths.equals(remembered) || !java.util.Objects.equals(highlighted, selected)) {
            paths = List.copyOf(remembered); highlighted = selected;
            vaults.setListData(paths.toArray(Path[]::new));
            if (selected != null && paths.contains(selected)) { vaults.setSelectedValue(selected, true); }
            else { vaults.clearSelection(); }
        }
        back.setVisible(canReturn); back.setEnabled(canReturn && !busy);
        browse.setEnabled(!busy); create.setEnabled(!busy); vaults.setEnabled(!busy); updateActions();
    }
    void status(String message) { notice.setText(message); notice.setVisible(!message.isEmpty()); }
    void focusList() { vaults.requestFocusInWindow(); }
    void navigationEntered() {
        if (highlighted != null && paths.contains(highlighted)) { vaults.setSelectedValue(highlighted, true); }
        else { vaults.clearSelection(); }
    }
    void defaultButton(JRootPane root) { root.setDefaultButton(select); }
    private void updateActions() {
        select.setEnabled(!busy && vaults.getSelectedValue() != null);
        forget.setEnabled(!busy && vaults.getSelectedValue() != null);
    }

    /** Renderer text is presentation only; the list model always contains the complete Path. */
    static final class VaultCellRenderer extends JPanel implements ListCellRenderer<Path> {
        private static final long serialVersionUID = 1L;
        final JLabel name = new JLabel();
        final JLabel parent = new JLabel();
        private final DefaultListCellRenderer nativeStyle = new DefaultListCellRenderer();
        private String fullName = "";
        private String fullParent = "";

        VaultCellRenderer() {
            super(null);
            for (JLabel label : new JLabel[] {name, parent}) {
                label.putClientProperty("html.disable", Boolean.TRUE); add(label);
            }
        }
        @Override public java.awt.Component getListCellRendererComponent(JList<? extends Path> list, Path value,
                int index, boolean selected, boolean focused) {
            JLabel style = (JLabel) nativeStyle.getListCellRendererComponent(list, value, index, selected, focused);
            setBackground(style.getBackground());
            setBorder(BorderFactory.createCompoundBorder(style.getBorder(), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
            setComponentOrientation(list.getComponentOrientation());
            name.setFont(list.getFont().deriveFont(java.awt.Font.BOLD));
            parent.setFont(list.getFont().deriveFont(java.awt.Font.PLAIN, list.getFont().getSize2D() * .95f));
            name.setForeground(style.getForeground());
            // Keep the native selection foreground; mute ordinary secondary text with shared contrast rules.
            parent.setForeground(selected ? style.getForeground() : DesktopStyle.readable(
                    DesktopStyle.mix(style.getForeground(), style.getBackground(), .25), style.getBackground(), 4.5));
            name.setEnabled(list.isEnabled()); parent.setEnabled(list.isEnabled());
            Path absolute = value.toAbsolutePath();
            String fullPath = absolute.toString();
            String basename = absolute.getFileName() == null ? fullPath : absolute.getFileName().toString();
            String directory = absolute.getParent() == null ? fullPath : absolute.getParent().toString();
            fullName = UntrustedText.display(basename); fullParent = UntrustedText.display(directory);
            setToolTipText(fullPath); name.setToolTipText(fullPath); parent.setToolTipText(fullPath);
            getAccessibleContext().setAccessibleName(basename + " — " + fullPath);
            getAccessibleContext().setAccessibleDescription(fullPath);
            name.getAccessibleContext().setAccessibleName(basename);
            parent.getAccessibleContext().setAccessibleName(directory);
            name.getAccessibleContext().setAccessibleDescription(fullPath);
            parent.getAccessibleContext().setAccessibleDescription(fullPath);
            return this;
        }
        @Override public Dimension getPreferredSize() {
            var insets = getInsets();
            return new Dimension(0, insets.top + insets.bottom + DesktopStyle.MICRO
                    + name.getFontMetrics(name.getFont()).getHeight() + parent.getFontMetrics(parent.getFont()).getHeight());
        }
        @Override public void doLayout() {
            var insets = getInsets();
            int width = Math.max(0, getWidth() - insets.left - insets.right);
            int nameHeight = name.getFontMetrics(name.getFont()).getHeight();
            name.setBounds(insets.left, insets.top, width, nameHeight);
            parent.setBounds(insets.left, insets.top + nameHeight + DesktopStyle.MICRO, width,
                    parent.getFontMetrics(parent.getFont()).getHeight());
            name.setText(middleElide(fullName, name.getFontMetrics(name.getFont()), width));
            parent.setText(middleElide(fullParent, parent.getFontMetrics(parent.getFont()), width));
        }
        @Override protected void paintComponent(java.awt.Graphics graphics) {
            // CellRendererPane stamps need layout at their actual paint width even outside a validated hierarchy.
            doLayout(); super.paintComponent(graphics);
        }
        private static String middleElide(String full, java.awt.FontMetrics metrics, int width) {
            if (metrics.stringWidth(full) <= width) { return full; }
            if (metrics.stringWidth("…") > width) { return ""; }
            int count = full.codePointCount(0, full.length());
            int[] boundaries = new int[count + 1];
            for (int i = 1; i <= count; i++) {
                boundaries[i] = boundaries[i - 1] + Character.charCount(full.codePointAt(boundaries[i - 1]));
            }
            int low = 0, high = count;
            while (low < high) {
                int kept = (low + high + 1) >>> 1;
                String candidate = full.substring(0, boundaries[(kept + 1) / 2]) + "…"
                        + full.substring(boundaries[count - kept / 2]);
                if (metrics.stringWidth(candidate) <= width) { low = kept; } else { high = kept - 1; }
            }
            int left = boundaries[(low + 1) / 2], right = boundaries[count - low / 2];
            // Prefer whole beginning/ending components when those boundaries fit inside the retained text.
            char separator = java.io.File.separatorChar;
            int leftSeparator = full.lastIndexOf(separator, left - 1), rightSeparator = full.indexOf(separator, right);
            if (leftSeparator > 0) { left = leftSeparator + 1; }
            if (rightSeparator >= 0) { right = rightSeparator; }
            return full.substring(0, left) + "…" + full.substring(right);
        }
    }
}
