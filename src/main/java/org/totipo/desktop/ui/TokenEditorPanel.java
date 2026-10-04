package org.totipo.desktop.ui;

import org.totipo.*;
import org.totipo.desktop.Base32;
import org.totipo.desktop.TokenDraft;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.ParseException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;

/** Literal Swing fields own input, never a core builder or existing secret. */
public final class TokenEditorPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    static final Dimension PREFERRED_SIZE = new Dimension(760, 720);
    static final Dimension MINIMUM_SIZE = new Dimension(640, 580);
    private static final long MAX_PERIOD = 4294967295L;
    final JTextField issuer = new JTextField(24);
    final JTextField account = new JTextField(24);
    final TokenChoice<TotpAlgorithm> algorithm = new TokenChoice<>("Algorithm", List.of(TotpAlgorithm.values()), Enum::name, false);
    final TokenChoice<TokenStatus> status = new TokenChoice<>("Status", List.of(TokenStatus.values()),
            value -> switch (value) { case ACTIVE -> "Active"; case TOMBSTONED -> "Deleted"; }, true);
    final JLabel statusHelp = new JLabel("Deleted tokens remain in vault history.");
    final TokenChoice<Integer> digits = new TokenChoice<>("Digits", List.of(6, 7, 8), Object::toString, false);
    final JSpinner period = new JSpinner(new SpinnerNumberModel(
            Long.valueOf(30), Long.valueOf(1), Long.valueOf(MAX_PERIOD), Long.valueOf(1)));
    final JCheckBox replace = new JCheckBox("Replace secret");
    final JPasswordField secret = new JPasswordField();
    final JLabel secretLabel = SwingUsability.label("New Base32 secret", secret);
    final JButton save = new JButton("Save");
    final JButton cancel = new JButton("Cancel");
    final JTextArea message = wrappedText("");
    final JPanel fields = new JPanel(new GridBagLayout());
    final JScrollPane scroll;
    final JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
    private final boolean create;
    private boolean busy;
    private boolean retired;
    private int row;

    public TokenEditorPanel(TokenDescriptor descriptor, String explanation,
                            Consumer<TokenDraft> submit, Runnable abandon) {
        Edt.require();
        create = descriptor == null;
        // JTextField otherwise replaces stored newlines with spaces during prefill.
        issuer.getDocument().putProperty("filterNewlines", Boolean.FALSE);
        account.getDocument().putProperty("filterNewlines", Boolean.FALSE);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setPreferredSize(PREFERRED_SIZE);
        setMinimumSize(MINIMUM_SIZE);
        configurePeriod();
        if (!create) {
            status.select(descriptor.status());
            issuer.setText(descriptor.issuer()); account.setText(descriptor.account());
            algorithm.select(descriptor.algorithm()); digits.select(descriptor.digits());
            period.setValue(descriptor.period().getSeconds());
        }
        section("Token");
        if (!create) {
            JPanel statusControl = new JPanel(new BorderLayout(0, 4));
            statusControl.add(status, BorderLayout.NORTH);
            statusHelp.setLabelFor(status);
            statusHelp.setFont(statusHelp.getFont().deriveFont(Math.max(10f, statusHelp.getFont().getSize2D() - 1f)));
            statusControl.add(statusHelp, BorderLayout.CENTER);
            String deletionHelp = "Deleted is logical deletion. Tokens remain in vault history; "
                    + "historical and provider copies are not erased.";
            AbstractButton deleted = status.options.get(TokenStatus.TOMBSTONED);
            deleted.setToolTipText(deletionHelp);
            deleted.getAccessibleContext().setAccessibleDescription(deletionHelp);
            field("Status", statusControl, true);
        }
        field("Issuer", issuer, true);
        field("Account", account, true);
        field("Algorithm", algorithm, true);
        field("Digits", digits, true);
        field("Period (seconds)", period, true);
        section("Secret");
        if (!create) { wideRow(replace, 0); }
        field(secretLabel, secret, true);
        secret.setEnabled(create); secretLabel.setEnabled(create);
        replace.getAccessibleContext().setAccessibleName("Replace secret");
        replace.addActionListener(event -> {
            boolean replacing = replace.isSelected();
            if (!replacing) { secret.setText(""); }
            secret.setEnabled(replacing && !busy && !retired);
            secretLabel.setEnabled(secret.isEnabled());
            if (secret.isEnabled()) {
                secret.requestFocusInWindow();
                scrollSecretIntoView();
            }
        });
        secret.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent event) { scrollSecretIntoView(); }
        });
        FormBody body = new FormBody(); body.add(fields, BorderLayout.NORTH);
        scroll = new JScrollPane(body, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        message.setVisible(false);
        JPanel footer = new JPanel(new BorderLayout(0, 10));
        actions.add(cancel); actions.add(save);
        footer.add(message, BorderLayout.CENTER); footer.add(actions, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        save.setText(create ? "Create" : "Save");
        save.addActionListener(event -> {
            if (busy || retired) { return; }
            TokenDraft draft;
            try { draft = draft(); }
            catch (IllegalArgumentException invalid) { showMessage(invalid.getMessage()); return; }
            busy(true, "Saving…");
            submit.accept(draft);
        });
        save.setMnemonic(create ? 'R' : 'S'); cancel.setMnemonic('C');
        secret.getAccessibleContext().setAccessibleName(create ? "New TOTP secret" : "Replacement TOTP secret");
        cancel.addActionListener(event -> { if (!busy && !retired) { retire(); abandon.run(); } });
    }

    private void configurePeriod() {
        JSpinner.DefaultEditor editor = new JSpinner.DefaultEditor(period);
        JFormattedTextField input = editor.getTextField();
        input.setEditable(true);
        input.setColumns(10);
        // The default NumberFormatter can accept fractional/trailing input or group separators.
        // Keep the previous Long.parseLong grammar and never save a stale model value after a bad edit.
        DefaultFormatter formatter = new DefaultFormatter() {
            private static final long serialVersionUID = 1L;
            @Override public Object stringToValue(String text) throws ParseException {
                try {
                    long value = Long.parseLong(text);
                    if (value < 1 || value > MAX_PERIOD) { throw new NumberFormatException(); }
                    return value;
                } catch (NumberFormatException invalid) { throw new ParseException("Invalid whole seconds", 0); }
            }
        };
        formatter.setCommitsOnValidEdit(false); formatter.setOverwriteMode(false);
        input.setFormatterFactory(new DefaultFormatterFactory(formatter));
        input.setValue(period.getValue());
        input.setFocusLostBehavior(JFormattedTextField.PERSIST);
        input.getAccessibleContext().setAccessibleName("Period (seconds)");
        input.getAccessibleContext().setAccessibleDescription("Whole seconds from 1 to 4294967295.");
        period.setToolTipText("Whole seconds from 1 to 4294967295.");
        period.setEditor(editor);
    }

    private static JTextArea wrappedText(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false); area.setFocusable(false);
        area.setLineWrap(true); area.setWrapStyleWord(true); area.setOpaque(false);
        // Start with a useful wrapping width; subsequent layout uses the available viewport width.
        area.setSize(PREFERRED_SIZE.width - 80, Short.MAX_VALUE);
        area.setFont(UIManager.getFont("Label.font"));
        area.setForeground(UIManager.getColor("Label.foreground"));
        return area;
    }

    private void scrollSecretIntoView() {
        // JTextField's scrollRectToVisible scrolls its text horizontally; scroll the form's row instead.
        fields.scrollRectToVisible(secret.getBounds());
    }

    private void section(String title) {
        JPanel heading = new JPanel(new BorderLayout());
        heading.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")), title));
        wideRow(heading, 16);
    }

    private void wideRow(JComponent control, int top) {
        GridBagConstraints constraint = new GridBagConstraints();
        constraint.gridx = 0; constraint.gridy = row++; constraint.gridwidth = 2;
        constraint.weightx = 1; constraint.fill = GridBagConstraints.HORIZONTAL;
        constraint.insets = new Insets(top, 0, 0, 0);
        fields.add(control, constraint);
    }

    private void field(String title, JComponent control, boolean expand) {
        field(SwingUsability.label(title, control), control, expand);
    }

    private void field(JLabel label, JComponent control, boolean expand) {
        GridBagConstraints constraint = new GridBagConstraints();
        constraint.gridx = 0; constraint.gridy = row; constraint.anchor = GridBagConstraints.LINE_START;
        constraint.insets = new Insets(10, 0, 0, 12);
        fields.add(label, constraint);
        constraint.gridx = 1; constraint.weightx = 1; constraint.insets = new Insets(10, 0, 0, 0);
        constraint.fill = expand ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        fields.add(control, constraint); row++;
    }

    private static final class FormBody extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;
        FormBody() { super(new BorderLayout()); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return Math.max(16, visible.height - 16); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    void installDialog(JRootPane root) { SwingUsability.dialog(root, save, () -> { if (canCancel()) { cancel.doClick(); } }); }
    void focusInitialField() { issuer.requestFocusInWindow(); }

    TokenDraft draft() {
        Edt.require();
        TokenDescriptor fields;
        try {
            period.commitEdit();
            fields = new TokenDescriptor(create ? TokenStatus.ACTIVE : status.selected(),
                    issuer.getText(), account.getText(), algorithm.selected(),
                    digits.selected(), Duration.ofSeconds((Long) period.getValue()));
        } catch (RuntimeException | ParseException invalid) {
            throw new IllegalArgumentException("Choose valid status/algorithm, digits 6–8 and whole seconds 1–4294967295.");
        }
        byte[] decoded = null;
        if (create || replace.isSelected()) {
            char[] input = secret.getPassword();
            try { decoded = decodeAndWipe(input); }
            finally { secret.setText(""); }
        }
        return new TokenDraft(fields, decoded);
    }

    static byte[] decodeAndWipe(char[] input) {
        try { return Base32.decode(input); }
        finally { Arrays.fill(input, '\0'); }
    }

    private void showMessage(String text) {
        message.setText(text); message.setVisible(!text.isBlank());
        revalidate(); repaint();
    }

    public void busy(boolean value, String text) {
        Edt.require();
        busy = value;
        for (var control : new JComponent[]{issuer, account, algorithm, status, digits, period, replace, save, cancel}) {
            control.setEnabled(!value && !retired);
        }
        secret.setEnabled(!value && !retired && (create || replace.isSelected()));
        secretLabel.setEnabled(secret.isEnabled());
        showMessage(text);
    }
    public void retire() {
        Edt.require(); retired = true; secret.setText(""); busy(true, " ");
    }
    public boolean canCancel() { Edt.require(); return !busy && !retired; }
}
