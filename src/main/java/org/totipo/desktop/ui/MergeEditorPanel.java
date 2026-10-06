package org.totipo.desktop.ui;

import org.totipo.*;
import org.totipo.desktop.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.*;

/** Session-owned resolver. Alternatives are choices; Java owns all resolution semantics. */
public final class MergeEditorPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient MergeInputs inputs;
    private final transient List<TokenAlternative> presented;
    private final transient Consumer<MergeDraft> submit;
    private final transient Runnable abandon;
    final JButton save = new JButton("Resolve"), cancel = new JButton("Cancel");
    private final JButton combine = new JButton("Combine details…"), back = new JButton("Back");
    private final JPanel body = new JPanel(new GridBagLayout());
    private final JPanel footer = new JPanel(new BorderLayout(16, 0));
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
    private final JScrollPane scroll;
    private final JTextArea notice = message("");
    private final transient List<JRadioButton> versions = new ArrayList<>();
    private final ButtonGroup versionGroup = new ButtonGroup();
    private ButtonGroup setupGroup = new ButtonGroup();
    private final transient List<JRadioButton> setupButtons = new ArrayList<>();
    private final transient List<TokenAlternative> setups = new ArrayList<>();
    private final JRadioButton customSetup = new JRadioButton("Use a different authenticator setup");
    private final TokenChoice<TokenStatus> status = new TokenChoice<>("Status", List.of(TokenStatus.ACTIVE, TokenStatus.TOMBSTONED),
            value -> value == TokenStatus.ACTIVE ? "Active" : "Deleted", true);
    // Borrow only S3-compatible setup controls, never the legacy editor's workflow/validation.
    private final TokenEditorPanel custom = new TokenEditorPanel(null, "", TokenDraft::close, () -> {});
    private transient TextChoice issuer, account;
    private transient Decision statusDecision, setupDecision, versionDecision;
    private boolean detailed, busy, retired, selecting, stale;
    private int row;
    private JRootPane root;
    private transient Runnable resized = () -> {};

    public MergeEditorPanel(MergeInputs captured, Consumer<MergeDraft> submit, Runnable abandon) {
        Edt.require(); inputs = captured.select(captured.captured()); this.submit = submit; this.abandon = abandon;
        presented = TokenPresentation.ordered(inputs.token());
        setLayout(new BorderLayout(0, 24)); setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        setBackground(DesktopStyle.surface()); body.setOpaque(false); footer.setOpaque(false); actions.setOpaque(false);
        TokenEditorPanel.FormBody wrapper = new TokenEditorPanel.FormBody(); wrapper.setOpaque(false); wrapper.add(body, BorderLayout.NORTH);
        scroll = new JScrollPane(wrapper, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.setOpaque(false); scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16); add(scroll, BorderLayout.CENTER);
        footer.add(actions, BorderLayout.EAST); add(footer, BorderLayout.SOUTH);
        DesktopStyle.action(cancel, DesktopStyle.ActionRole.SecondaryAction, false);
        DesktopStyle.action(combine, DesktopStyle.ActionRole.QuietAction, false);
        DesktopStyle.action(back, DesktopStyle.ActionRole.SecondaryAction, false);
        save.addActionListener(e -> activate()); cancel.addActionListener(e -> cancel());
        combine.addActionListener(e -> details()); back.addActionListener(e -> simple());
        customSetup.addItemListener(e -> { if (e.getStateChange() == ItemEvent.DESELECTED) { clearSecret(); } });
        status.clearSelection(); simple();
        if (!inputs.token().unresolvedReferences().isEmpty()) { busy(false, "Cannot resolve yet: some versions are incomplete. Wait for a complete observation."); save.getAccessibleContext().setAccessibleDescription(notice.getText()); }
    }

    private static JTextArea message(String text) {
        JTextArea area = new JTextArea(text); area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true);
        area.setOpaque(false); area.setForeground(DesktopStyle.text()); area.setFont(DesktopStyle.font(DesktopStyle.Typography.Body));
        return area;
    }
    private void reset(String action) {
        row = 0; body.removeAll(); actions.removeAll(); footer.remove(back); notice.setText("");
        save.setText(action); DesktopStyle.action(save, DesktopStyle.ActionRole.PrimaryAction, false);
        actions.add(cancel); if (!detailed) { actions.add(combine); } actions.add(save);
        if (detailed) { footer.add(back, BorderLayout.WEST); }
    }
    private void wide(JComponent control, int gap) {
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = row++; c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(0, 0, gap, 0); body.add(control, c);
    }
    private void simple() {
        if (busy || retired || stale) { return; }
        // Back discards the detailed draft, including its secret, and explicitly starts fresh.
        if (detailed) { clearSecret(); versionGroup.clearSelection(); }
        detailed = false; reset("Resolve");
        wide(message("Choose the version to keep."), 16);
        JPanel choices = new JPanel(new GridLayout(0, 1, 0, 16)); choices.setOpaque(false);
        if (versions.isEmpty()) {
            for (int i = 0; i < inputs.captured().size(); i++) {
                TokenAlternative alternative = presented.get(i);
                TokenDescriptor d = alternative.descriptor();
                String identity = identity(d);
                boolean indistinguishable = presented.stream().filter(a -> identity(a.descriptor()).equals(identity) && summary(a.descriptor()).equals(summary(d))).count() > 1;
                String title = indistinguishable ? "Version " + (i + 1) + " · " + identity : identity;
                JRadioButton radio = radio(title, title + ". " + summary(d));
                versionGroup.add(radio); versions.add(radio);
            }
        }
        for (int i = 0; i < versions.size(); i++) {
            JPanel option = new JPanel(new BorderLayout(0, 4)); option.setOpaque(false);
            option.add(versions.get(i), BorderLayout.NORTH);
            JTextArea detail = message(summary(presented.get(i).descriptor()));
            detail.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0)); option.add(detail); choices.add(option);
        }
        versionDecision = new Decision("Version", choices, versions.get(0), false); wide(versionDecision.panel, 0);
        finishLayout();
    }
    private static String identity(TokenDescriptor d) { return d.issuer().isBlank() ? TokenPresentation.primary(d) : TokenPresentation.identity(d); }
    private static String summary(TokenDescriptor d) {
        return (d.status() == TokenStatus.TOMBSTONED ? "Deleted · " : "") + SetupSummary.format(d);
    }
    private JRadioButton radio(String title, String accessible) {
        JRadioButton button = new JRadioButton(title); button.putClientProperty("html.disable", Boolean.TRUE);
        button.setOpaque(false); button.setForeground(DesktopStyle.text()); button.setFont(DesktopStyle.font(DesktopStyle.Typography.Body));
        button.getAccessibleContext().setAccessibleName(accessible); styleRadio(button); trackFocus(button); return button;
    }
    private static void styleRadio(AbstractButton button) {
        DesktopStyle.radio(button);
    }
    private void details() {
        if (busy || retired || stale) { return; }
        // A new detailed draft never consults the whole-version choice.
        detailed = true; reset("Save Resolution");
        wide(message("Combine details from the versions. Choose each conflicting value.\nBack discards these choices and any new secret."), 16);
        issuer = new TextChoice("Issuer", inputs.competition().issuer()); account = new TextChoice("Account", inputs.competition().account());
        wide(issuer.decision.panel, 16); wide(account.decision.panel, 16);
        status.clearSelection();
        var statuses = inputs.competition().status().values(); if (statuses.size() == 1) { status.select(statuses.get(0).value()); }
        status.setLayout(new GridLayout(0, 1, 0, 8)); status.setOpaque(false);
        status.options.values().forEach(b -> { styleRadio(b); trackFocus(b); });
        statusDecision = new Decision("Status", status, status.options.get(TokenStatus.ACTIVE), true); wide(statusDecision.panel, 16);
        status.options.values().forEach(b -> b.addActionListener(e -> statusDecision.clear()));
        clearSecret(); selecting = true; custom.algorithm.select(TotpAlgorithm.SHA1); custom.digits.select(6); custom.period.setValue(30L); selecting = false;
        setupGroup.clearSelection();
        setups.clear(); setupButtons.clear(); setupGroup = new ButtonGroup();
        JPanel setupChoices = new JPanel(new GridLayout(0, 1, 0, 12)); setupChoices.setOpaque(false);
        for (TokenAlternative a : presented) { if (setups.stream().noneMatch(b -> sameSetup(inputs, a, b))) { setups.add(a); } }
        for (TokenAlternative a : setups) {
            String identity = identity(a.descriptor());
            boolean ambiguous = setups.stream().filter(b -> identity(b.descriptor()).equals(identity) && SetupSummary.format(b.descriptor()).equals(SetupSummary.format(a.descriptor()))).count() > 1;
            String title = setups.size() == 1 ? "Keep existing setup" : ambiguous
                    ? "Existing setup " + (setups.indexOf(a) + 1) + " · " + identity : "Setup used by " + identity;
            JRadioButton button = radio(title, title + ". " + SetupSummary.format(a.descriptor()));
            setupGroup.add(button); setupButtons.add(button);
            button.addActionListener(e -> { clearSecret(); if (setupDecision != null) { setupDecision.clear(); } });
            JPanel choice = new JPanel(new BorderLayout(0, 4)); choice.setOpaque(false); choice.add(button, BorderLayout.NORTH);
            JTextArea description = message(SetupSummary.format(a.descriptor())); description.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0));
            choice.add(description); setupChoices.add(choice);
        }
        styleRadio(customSetup); trackFocus(customSetup);
        setupGroup.add(customSetup); setupChoices.add(customSetup);
        if (setups.size() == 1) { setupButtons.get(0).setSelected(true); }
        JPanel fields = new JPanel(new GridBagLayout()); fields.setOpaque(false);
        List<JComponent> controls = List.of(custom.secret, custom.algorithm, custom.digits, custom.period);
        List<String> names = List.of("New Base32 secret", "Algorithm", "Digits", "Period (seconds)");
        for (int i = 0; i < controls.size(); i++) {
            JComponent control = controls.get(i); String name = names.get(i);
            control.getAccessibleContext().setAccessibleName(name); control.setEnabled(true);
            if (control instanceof JTextField text && !(control instanceof JPasswordField)) { DesktopStyle.input(text); }
            if (control instanceof TokenChoice<?> choice) {
                TokenManagementPanel.styleChoices(choice); choice.options.values().forEach(b -> {
                    b.addActionListener(e -> chooseCustom());
                });
            }
            GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = i; c.anchor = GridBagConstraints.FIRST_LINE_START;
            c.insets = new Insets(12, 0, 0, 16); JLabel label = SwingUsability.label(name, control);
            label.setForeground(DesktopStyle.textSecondary()); label.setFont(DesktopStyle.font(DesktopStyle.Typography.Body)); fields.add(label, c);
            c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(12, 0, 0, 0); fields.add(control, c);
            autoSelect(control, this::chooseCustom); trackFocus(control);
        }
        custom.secret.getDocument().addDocumentListener(listener(this::chooseCustom));
        custom.period.addChangeListener(e -> chooseCustom());
        custom.secret.setEnabled(true);
        DesktopStyle.input(((JSpinner.DefaultEditor) custom.period.getEditor()).getTextField());
        custom.period.setOpaque(false);
        custom.secret.setUI(new javax.swing.plaf.basic.BasicPasswordFieldUI());
        custom.secret.setBackground(DesktopStyle.surfaceInput()); custom.secret.setForeground(DesktopStyle.text());
        custom.secret.setCaretColor(DesktopStyle.text()); custom.secret.setFont(DesktopStyle.font(DesktopStyle.Typography.Body));
        custom.secret.setBorder(new DesktopStyle.ControlBorder(null, new Insets(6, 8, 6, 8)));
        JPanel setup = new JPanel(new BorderLayout(0, 8)); setup.setOpaque(false); setup.add(setupChoices, BorderLayout.NORTH); setup.add(fields);
        setupDecision = new Decision("Authenticator Setup", setup, setupButtons.get(0), true); wide(setupDecision.panel, 0);
        finishLayout();
    }
    static boolean sameSetup(MergeInputs inputs, TokenAlternative a, TokenAlternative b) {
        TokenDescriptor x = a.descriptor(), y = b.descriptor();
        return x.algorithm() == y.algorithm() && x.digits() == y.digits() && x.period().equals(y.period())
                && inputs.competition().secret().groups().stream().anyMatch(g -> g.alternatives().contains(a) && g.alternatives().contains(b));
    }
    private void chooseCustom() { if (!selecting && !busy && !retired && detailed && !stale) { customSetup.setSelected(true); if (setupDecision != null) { setupDecision.clear(); } } }
    private void clearSecret() { selecting = true; custom.secret.setText(""); selecting = false; }

    private final class Decision {
        final JPanel panel = new JPanel(new BorderLayout(0, 4));
        final JTextArea error = message("");
        final JComponent first;
        Decision(String name, JComponent contents, JComponent first, boolean border) {
            this.first = first; panel.setOpaque(false);
            if (border) { panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(DesktopStyle.borderStrong()), name, 0, 0, DesktopStyle.font(DesktopStyle.Typography.SectionTitle), DesktopStyle.text()), BorderFactory.createEmptyBorder(12, 12, 12, 12))); }
            panel.getAccessibleContext().setAccessibleName(name);
            panel.getAccessibleContext().setAccessibleDescription("Choose the " + name.toLowerCase() + " to keep.");
            error.setForeground(DesktopStyle.danger()); error.setVisible(false); error.getAccessibleContext().setAccessibleName(name + " validation");
            panel.add(contents, BorderLayout.CENTER); panel.add(error, BorderLayout.SOUTH);
        }
        void clear() { error.setText(""); error.setVisible(false); first.getAccessibleContext().setAccessibleDescription(null); }
        void invalid(String text) { error.setText(text); error.setVisible(true); first.getAccessibleContext().setAccessibleDescription(text); }
    }
    private final class TextChoice {
        final List<String> values = new ArrayList<>(); final List<JRadioButton> radios = new ArrayList<>();
        final JTextField customField = new JTextField(24); final JRadioButton customRadio = radio("", "");
        final boolean agreed; final Decision decision;
        TextChoice(String name, CompetingField<String> field) {
            field.values().forEach(v -> { if (!values.contains(v.value())) { values.add(v.value()); } }); agreed = values.size() == 1;
            JPanel choices = new JPanel(new GridLayout(0, 1, 0, 8)); choices.setOpaque(false);
            DesktopStyle.input(customField); customField.getDocument().putProperty("filterNewlines", Boolean.FALSE);
            customField.getAccessibleContext().setAccessibleName("Enter a different " + name.toLowerCase() + " value");
            trackFocus(customField);
            if (agreed) { customField.setText(values.get(0)); choices.add(customField); }
            else {
                ButtonGroup group = new ButtonGroup();
                for (String value : values) {
                    JRadioButton radio = radio("", name + " choice: " + value); group.add(radio); radios.add(radio);
                    JTextField text = new JTextField(value, 24); DesktopStyle.input(text); text.setEditable(false);
                    text.getAccessibleContext().setAccessibleName(name + " choice: " + value); trackFocus(text);
                    autoSelect(text, () -> radio.setSelected(true)); choices.add(line(radio, text));
                }
                group.add(customRadio); customRadio.getAccessibleContext().setAccessibleName(customField.getAccessibleContext().getAccessibleName());
                choices.add(line(customRadio, customField)); autoSelect(customField, () -> customRadio.setSelected(true));
                customField.getDocument().addDocumentListener(listener(() -> customRadio.setSelected(true)));
            }
            decision = new Decision(name, choices, agreed ? customField : radios.get(0), true);
            radios.forEach(b -> b.addItemListener(e -> { if (b.isSelected()) { decision.clear(); } }));
            customRadio.addItemListener(e -> { if (customRadio.isSelected()) { decision.clear(); } });
            customField.getDocument().addDocumentListener(listener(decision::clear));
        }
        String value() {
            if (agreed || customRadio.isSelected()) { return customField.getText(); }
            for (int i = 0; i < radios.size(); i++) { if (radios.get(i).isSelected()) { return values.get(i); } }
            return null;
        }
    }
    private static JPanel line(JRadioButton radio, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(8, 0)); p.setOpaque(false); p.add(radio, BorderLayout.WEST); p.add(field); return p;
    }
    private static void autoSelect(Component component, Runnable select) {
        component.addFocusListener(new FocusAdapter() { @Override public void focusGained(FocusEvent e) { select.run(); } });
        component.addMouseListener(new MouseAdapter() { @Override public void mousePressed(MouseEvent e) { select.run(); } });
        if (component instanceof Container c) { for (Component child : c.getComponents()) { autoSelect(child, select); } }
    }
    private void trackFocus(Component control) {
        if (control instanceof JComponent component && component.getClientProperty("totipo.resolverFocus") == null) {
            component.putClientProperty("totipo.resolverFocus", Boolean.TRUE);
            component.addFocusListener(new FocusAdapter() { @Override public void focusGained(FocusEvent event) { reveal(component); } });
        }
        if (control instanceof Container container) { for (Component child : container.getComponents()) { trackFocus(child); } }
    }
    private void reveal(JComponent control) {
        if (SwingUtilities.isDescendingFrom(control, body)) { body.scrollRectToVisible(SwingUtilities.convertRectangle(control.getParent(), control.getBounds(), body)); }
    }
    private void focus(JComponent control) { revalidate(); resized.run(); SwingUtilities.invokeLater(() -> { if (!retired) { reveal(control); control.requestFocusInWindow(); } }); }
    private int selected(List<JRadioButton> buttons) { for (int i = 0; i < buttons.size(); i++) { if (buttons.get(i).isSelected()) { return i; } } return -1; }
    private void activate() {
        if (busy || retired || stale) { return; }
        if (!detailed) {
            versionDecision.clear(); int index = selected(versions);
            if (index < 0) { versionDecision.invalid("Choose a version to keep."); focus(versionDecision.first); return; }
            send(MergeDraft.keep(inputs, presented.get(index))); return;
        }
        for (Decision d : List.of(issuer.decision, account.decision, statusDecision, setupDecision)) { d.clear(); }
        JComponent first = null;
        if (issuer.value() == null) { issuer.decision.invalid("Choose an issuer or enter your own."); first = issuer.decision.first; }
        if (account.value() == null) { account.decision.invalid("Choose an account or enter your own."); if (first == null) { first = account.decision.first; } }
        TokenStatus lifecycle = null;
        try { lifecycle = status.selected(); } catch (IllegalArgumentException invalid) {
            statusDecision.invalid("Choose Active or Deleted."); if (first == null) { first = statusDecision.first; }
        }
        int setup = selected(setupButtons);
        if (!customSetup.isSelected() && setup < 0) { setupDecision.invalid("Choose an authenticator setup."); if (first == null) { first = setupDecision.first; } }
        if (first != null) { focus(first); return; }
        try {
            SetupValidation.identity(issuer.value(), account.value());
            if (customSetup.isSelected()) {
                char[] chars = custom.secret.getPassword();
                try (SetupDraft acquired = SetupValidation.manual(issuer.value(), account.value(), chars, custom.algorithm.selected(),
                        custom.digits.selected(), ((JSpinner.DefaultEditor) custom.period.getEditor()).getTextField().getText())) {
                    TokenDescriptor d = acquired.review();
                    TokenDescriptor fields = new TokenDescriptor(lifecycle, issuer.value(), account.value(), d.algorithm(), d.digits(), d.period());
                    send(new MergeDraft(inputs, acquired.transfer(fields)));
                } finally { java.util.Arrays.fill(chars, '\0'); clearSecret(); }
            } else {
                TokenAlternative source = setups.get(setup); TokenDescriptor d = source.descriptor();
                send(new MergeDraft(inputs, new TokenDescriptor(lifecycle, issuer.value(), account.value(), d.algorithm(), d.digits(), d.period()), source, null));
            }
        } catch (SetupValidation.Invalid invalid) {
            Decision d = invalid.field() == SetupValidation.Field.ISSUER ? issuer.decision
                    : invalid.field() == SetupValidation.Field.ACCOUNT ? account.decision : setupDecision;
            d.invalid(invalid.getMessage());
            JComponent control = switch (invalid.field()) {
                case ISSUER -> issuer.customField; case ACCOUNT -> account.customField;
                case PERIOD -> ((JSpinner.DefaultEditor) custom.period.getEditor()).getTextField();
                case ALGORITHM -> custom.algorithm; case DIGITS -> custom.digits; default -> custom.secret;
            };
            focus(control);
        } catch (IllegalArgumentException invalid) { setupDecision.invalid("Choose a valid authenticator setup."); focus(setupDecision.first); }
    }
    private void send(MergeDraft draft) { busy(true, "Saving resolution…"); submit.accept(draft); }
    private void finishLayout() { save.setEnabled(!busy && !retired && !stale && inputs.token().unresolvedReferences().isEmpty()); revalidate(); repaint(); resized.run(); }
    public void busy(boolean value, String text) {
        Edt.require(); busy = value; enable(body, !busy && !retired && !stale); notice.setText(text);
        if (notice.getParent() == null) { footer.add(notice, BorderLayout.NORTH); }
        notice.setVisible(!text.isBlank()); cancel.setEnabled(!busy && !retired); back.setEnabled(!busy && !retired && !stale);
        combine.setEnabled(!busy && !retired && !stale); finishLayout();
    }
    private static void enable(Container root, boolean enabled) { for (Component c : root.getComponents()) { c.setEnabled(enabled); if (c instanceof Container child) { enable(child, enabled); } } }
    /** Replaces the draft with an explicit review state; abandoned secret is cleared immediately. */
    public void changed(Runnable review) {
        changed("The conflict changed while you were resolving it.\nAnother version may have appeared or the conflict may have been resolved.\nReview the updated conflict before continuing.", review);
    }
    public void changed(String explanation, Runnable review) {
        Edt.require(); if (retired || stale) { return; } clearSecret(); stale = true; busy = false;
        body.removeAll(); row = 0; actions.removeAll(); footer.remove(back);
        wide(message(explanation), 16);
        JButton updated = new JButton("Review Updated Conflict"); DesktopStyle.action(updated, DesktopStyle.ActionRole.PrimaryAction, false);
        updated.addActionListener(e -> review.run()); actions.add(cancel); actions.add(updated); cancel.setEnabled(true);
        notice.setText(""); notice.setVisible(false); revalidate(); repaint(); resized.run();
        if (root != null) { root.setDefaultButton(updated); } focus(updated);
    }
    void focusInitialField() { if (!retired && !versions.isEmpty()) { focus(versions.get(0)); } }
    public boolean canCancel() { return !busy && !retired; }
    public void cancel() { if (canCancel()) { retire(); abandon.run(); } }
    public void retire() { Edt.require(); retired = true; clearSecret(); custom.retire(); versionGroup.clearSelection(); enable(this, false); }
    int preferredTaskWidth() { return Math.max(680, Math.max(body.getPreferredSize().width, footer.getPreferredSize().width) + 48); }
    Dimension taskSize(int width) { return TaskDialogSizing.contentSize(body, scroll, footer, getInsets(), 24, width); }
    void installDialog(JRootPane root, Runnable resized) { this.root = root; this.resized = resized; SwingUsability.dialog(root, save, this::cancel); resized.run(); }
    private static DocumentListener listener(Runnable action) { return new DocumentListener() {
        public void insertUpdate(DocumentEvent e) { action.run(); } public void removeUpdate(DocumentEvent e) { action.run(); } public void changedUpdate(DocumentEvent e) { action.run(); }
    }; }
}
