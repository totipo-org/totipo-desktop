package org.totipo.desktop.ui;

import org.totipo.*;
import org.totipo.desktop.MergeDraft;
import org.totipo.desktop.MergeInputs;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.*;

/** Direct semantic resolver. Builders and publication remain with the existing controller. */
public final class MergeEditorPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient MergeInputs inputs;
    private final transient Consumer<MergeDraft> submit;
    private final transient Runnable abandon;
    final JButton save = new JButton("Save");
    final JButton cancel = new JButton("Cancel");
    private final JTextArea message = new JTextArea();
    private final JPanel body = new JPanel(new GridBagLayout());
    private final transient TextChoice issuer;
    private final transient TextChoice account;
    private final transient List<TokenAlternative> setups = new ArrayList<>();
    private final transient List<JRadioButton> setupButtons = new ArrayList<>();
    private final TokenChoice<TokenStatus> status = new TokenChoice<>("Status", List.of(TokenStatus.values()),
            value -> value == TokenStatus.ACTIVE ? "Active" : "Deleted", true);
    private final JRadioButton customSetup = new JRadioButton("Use a different authenticator setup");
    // Reuse U3.1 input validation, Long spinner, toggles and sensitive-input retirement.
    private final TokenEditorPanel custom = new TokenEditorPanel(null, "", draft -> draft.close(), () -> {});
    private final JPanel customFields = new JPanel(new GridLayout(0, 1, 0, 6));
    private boolean busy;
    private boolean retired;
    private boolean selecting;
    private int row;

    public MergeEditorPanel(MergeInputs captured, Consumer<MergeDraft> submit, Runnable abandon) {
        Edt.require(); this.inputs = captured.select(captured.captured()); this.submit = submit; this.abandon = abandon;
        setLayout(new BorderLayout(0, 12)); setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setPreferredSize(TokenEditorPanel.PREFERRED_SIZE); setMinimumSize(TokenEditorPanel.MINIMUM_SIZE);
        JLabel intro = TokenRowPanel.literal("Choose the value to keep for each conflicting field."); intro.setLabelFor(body);
        add(intro, BorderLayout.NORTH);
        issuer = new TextChoice("Issuer", inputs.selectedCompetition().issuer());
        account = new TextChoice("Account", inputs.selectedCompetition().account());
        addField("Issuer", issuer.panel); addField("Account", account.panel);
        var statuses = inputs.selectedCompetition().status().values();
        if (statuses.size() == 1) { status.select(statuses.get(0).value()); }
        else { status.clearSelection(); }
        status.options.values().forEach(button -> button.addActionListener(e -> validateForm()));
        addField("Status", status);
        ButtonGroup setupGroup = new ButtonGroup();
        JPanel setupChoices = new JPanel(new GridLayout(0, 1, 0, 8));
        for (TokenAlternative alternative : inputs.selected()) {
            if (setups.stream().noneMatch(previous -> sameSetup(inputs, previous, alternative))) { setups.add(alternative); }
        }
        for (int i = 0; i < setups.size(); i++) {
            TokenAlternative alternative = setups.get(i); var d = alternative.descriptor();
            String identity = TokenPresentation.identity(d);
            boolean ambiguous = setups.stream().filter(a -> TokenPresentation.identity(a.descriptor()).equals(identity)
                    && metadata(a.descriptor()).equals(metadata(d))).count() > 1;
            String title = setups.size() == 1 ? "Keep existing setup" : ambiguous || identity.isBlank()
                    ? "Existing setup " + (i + 1) : "Setup used by " + identity;
            JRadioButton button = new JRadioButton(title); button.putClientProperty("html.disable", Boolean.TRUE);
            button.getAccessibleContext().setAccessibleName(title + ". " + metadata(d));
            setupGroup.add(button); setupButtons.add(button);
            JPanel option = new JPanel(new BorderLayout(0, 2)); option.add(button, BorderLayout.NORTH);
            JLabel detail = TokenRowPanel.literal(metadata(d)); detail.setLabelFor(button);
            option.add(detail, BorderLayout.CENTER); setupChoices.add(option);
            button.addActionListener(e -> { selecting = true; custom.secret.setText(""); selecting = false; validateForm(); });
        }
        setupGroup.add(customSetup); setupChoices.add(customSetup);
        if (setups.size() == 1) { setupButtons.get(0).setSelected(true); }
        addField("Authenticator Setup", setupChoices);
        for (var pair : List.of(new Object[]{"Secret", custom.secret}, new Object[]{"Algorithm", custom.algorithm},
                new Object[]{"Digits", custom.digits}, new Object[]{"Period (seconds)", custom.period})) {
            JComponent control = (JComponent) pair[1]; customFields.add(SwingUsability.label((String) pair[0], control)); customFields.add(control);
            autoSelect(control, () -> { if (!busy && !retired) { customSetup.setSelected(true); validateForm(); } });
        }
        custom.secret.setEnabled(true);
        custom.secret.getDocument().addDocumentListener(listener(() -> { if (!selecting && !busy && !retired) { customSetup.setSelected(true); validateForm(); } }));
        custom.algorithm.options.values().forEach(button -> button.addActionListener(e -> chooseCustom()));
        custom.digits.options.values().forEach(button -> button.addActionListener(e -> chooseCustom()));
        custom.period.addChangeListener(e -> chooseCustom());
        customSetup.addActionListener(e -> validateForm()); addField("", customFields);
        JPanel wrapper = new TokenEditorPanel.FormBody(); wrapper.add(body, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(wrapper, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16); add(scroll, BorderLayout.CENTER);
        message.setEditable(false); message.setLineWrap(true); message.setWrapStyleWord(true);
        message.getAccessibleContext().setAccessibleName("Resolution status");
        JPanel footer = new JPanel(new BorderLayout(0, 8)); JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        buttons.add(cancel); buttons.add(save); footer.add(message); footer.add(buttons, BorderLayout.SOUTH); add(footer, BorderLayout.SOUTH);
        save.addActionListener(e -> save()); cancel.addActionListener(e -> { if (canCancel()) { retire(); abandon.run(); } });
        validateForm();
        if (!inputs.token().unresolvedReferences().isEmpty()) {
            message.setText("Cannot resolve yet: some versions are incomplete. Wait for a complete observation.");
        }
    }
    static boolean sameSetup(MergeInputs inputs, TokenAlternative a, TokenAlternative b) {
        var x = a.descriptor(); var y = b.descriptor();
        return x.algorithm() == y.algorithm() && x.digits() == y.digits() && x.period().equals(y.period())
                && inputs.selectedCompetition().secret().groups().stream().anyMatch(g -> g.alternatives().contains(a) && g.alternatives().contains(b));
    }
    private static String metadata(TokenDescriptor d) { return d.algorithm() + " · " + d.digits() + " digits · " + d.period().getSeconds() + " seconds"; }
    private void chooseCustom() { if (!busy && !retired) { customSetup.setSelected(true); validateForm(); } }
    private void addField(String label, JComponent control) {
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = row++; c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(8, 0, 4, 0); body.add(SwingUsability.label(label, control), c);
        c.gridy = row++; c.insets = new Insets(0, 0, 4, 0); body.add(control, c);
    }
    private final class TextChoice {
        final JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
        final List<String> values = new ArrayList<>();
        final List<JRadioButton> radios = new ArrayList<>();
        final JTextField customField = new JTextField(24);
        final JRadioButton customRadio = new JRadioButton();
        final boolean agreed;
        TextChoice(String name, CompetingField<String> field) {
            field.values().forEach(value -> { if (!values.contains(value.value())) { values.add(value.value()); } });
            agreed = values.size() == 1;
            customField.getDocument().putProperty("filterNewlines", Boolean.FALSE);
            customField.getAccessibleContext().setAccessibleName("Enter a different " + name.toLowerCase() + " value");
            if (agreed) { customField.setText(values.get(0)); panel.add(customField); }
            else {
                ButtonGroup group = new ButtonGroup();
                for (String value : values) {
                    JRadioButton radio = new JRadioButton(); group.add(radio); radios.add(radio);
                    JTextField text = new JTextField(24); text.getDocument().putProperty("filterNewlines", Boolean.FALSE); text.setText(value); text.setEditable(false);
                    String accessible = name + " choice: " + value; radio.getAccessibleContext().setAccessibleName(accessible); text.getAccessibleContext().setAccessibleName(accessible);
                    autoSelect(text, () -> { radio.setSelected(true); validateForm(); });
                    radio.addActionListener(e -> validateForm()); panel.add(line(radio, text));
                }
                group.add(customRadio); customRadio.getAccessibleContext().setAccessibleName(customField.getAccessibleContext().getAccessibleName());
                customRadio.addActionListener(e -> validateForm()); panel.add(line(customRadio, customField));
                autoSelect(customField, () -> { customRadio.setSelected(true); validateForm(); });
            }
            customField.getDocument().addDocumentListener(listener(() -> { if (!agreed) { customRadio.setSelected(true); } validateForm(); }));
        }
        String value() {
            if (agreed || customRadio.isSelected()) { return customField.getText(); }
            for (int i = 0; i < radios.size(); i++) { if (radios.get(i).isSelected()) { return values.get(i); } }
            throw new IllegalArgumentException("Choose a value for each conflicting field.");
        }
    }
    private static JPanel line(JRadioButton radio, JTextField field) { JPanel panel = new JPanel(new BorderLayout(6, 0)); panel.add(radio, BorderLayout.WEST); panel.add(field); return panel; }
    private static void autoSelect(Component component, Runnable select) {
        component.addFocusListener(new FocusAdapter() { @Override public void focusGained(FocusEvent e) { select.run(); } });
        component.addMouseListener(new MouseAdapter() { @Override public void mousePressed(MouseEvent e) { select.run(); } });
        if (component instanceof Container container) { for (Component child : container.getComponents()) { autoSelect(child, select); } }
    }
    private int setupIndex() { for (int i = 0; i < setupButtons.size(); i++) { if (setupButtons.get(i).isSelected()) { return i; } } return -1; }
    private void validateForm() {
        boolean valid = false;
        if (issuer != null && account != null) {
            try { issuer.value(); account.value(); status.selected(); valid = customSetup.isSelected() || setupIndex() >= 0;
                if (customSetup.isSelected()) { char[] chars = custom.secret.getPassword(); try { valid = chars.length > 0; } finally { java.util.Arrays.fill(chars, '\0'); } }
            } catch (IllegalArgumentException ignored) { valid = false; }
        }
        save.setEnabled(valid && !busy && !retired && inputs.token().unresolvedReferences().isEmpty());
    }
    private void save() {
        if (!save.isEnabled() || busy || retired) { return; }
        try {
            TokenDescriptor setup; byte[] replacement = null; TokenAlternative source = null;
            if (customSetup.isSelected()) {
                try { custom.period.commitEdit(); }
                catch (java.text.ParseException invalid) { throw new IllegalArgumentException("Enter valid whole seconds from 1 to 4294967295."); }
                setup = new TokenDescriptor(status.selected(), issuer.value(), account.value(), custom.algorithm.selected(), custom.digits.selected(),
                        java.time.Duration.ofSeconds((Long) custom.period.getValue()));
                selecting = true;
                try { replacement = TokenEditorPanel.decodeAndWipe(custom.secret.getPassword()); }
                finally { custom.secret.setText(""); selecting = false; }
            } else { source = setups.get(setupIndex()); setup = source.descriptor(); }
            TokenDescriptor fields = new TokenDescriptor(status.selected(), issuer.value(), account.value(), setup.algorithm(), setup.digits(), setup.period());
            MergeDraft draft = new MergeDraft(inputs, fields, source, replacement); busy(true, "Saving resolution…"); submit.accept(draft);
        } catch (IllegalArgumentException invalid) { message.setText(invalid.getMessage()); validateForm(); }
    }
    public void busy(boolean value, String text) {
        Edt.require(); busy = value; enable(body, !busy && !retired); cancel.setEnabled(!busy && !retired); message.setText(text); validateForm();
    }
    private static void enable(Container root, boolean enabled) { for (Component child : root.getComponents()) { child.setEnabled(enabled); if (child instanceof Container container) { enable(container, enabled); } } }
    public boolean canCancel() { return !busy && !retired; }
    public void cancel() { if (canCancel()) { cancel.doClick(); } }
    public void retire() { Edt.require(); retired = true; selecting = true; custom.retire(); selecting = false; busy(true, " "); }
    void installDialog(JRootPane root) { SwingUsability.dialog(root, save, this::cancel); }
    private static DocumentListener listener(Runnable action) { return new DocumentListener() {
        public void insertUpdate(DocumentEvent e) { action.run(); } public void removeUpdate(DocumentEvent e) { action.run(); } public void changedUpdate(DocumentEvent e) { action.run(); }
    }; }
}
