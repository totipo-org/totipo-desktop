package org.totipo.desktop.ui;

import org.totipo.*;
import org.totipo.desktop.*;
import java.awt.*;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.*;
import static org.totipo.desktop.SetupValidation.Field;

/** One session-owned task surface. Acquisition never publishes; review owns a disposable setup. */
public final class TokenManagementPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    @FunctionalInterface public interface Submit {
        void save(VaultState base, TokenAlternative target, TokenDraft draft);
    }
    enum Stage { EDIT, ACQUIRE, REVIEW, DUPLICATE, REPLACE_REVIEW, DELETE }
    final JTextField issuer = new JTextField(24), account = new JTextField(24);
    final JPasswordField secret = new JPasswordField(24), uri = new JPasswordField(24);
    final TokenChoice<TotpAlgorithm> algorithm = new TokenChoice<>("Algorithm", List.of(TotpAlgorithm.values()), Enum::name, false);
    final TokenChoice<Integer> digits = new TokenChoice<>("Digits", List.of(6, 7, 8), Object::toString, false);
    final JTextField period = new JTextField("30");
    final JButton primary = new JButton(), cancel = new JButton("Cancel");
    final JPanel body = new JPanel(new GridBagLayout());
    final JTextArea notice = text("");
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
    private final JPanel actionRow = new JPanel(new BorderLayout(16, 0));
    private final JPanel footer = new JPanel(new BorderLayout(0, 16));
    private final JScrollPane scroll;
    private final EnumMap<Field, JComponent> inputs = new EnumMap<>(Field.class);
    private final EnumMap<Field, JTextArea> errors = new EnumMap<>(Field.class);
    private final transient Supplier<VaultState> current;
    private final transient Submit submit;
    private final transient Runnable abandon;
    private final transient VaultState editBase;
    private final transient TokenAlternative original;
    private final String explanation;
    private transient TokenAlternative target;
    private transient VaultState targetBase;
    private transient SetupDraft setup;
    private transient List<IdentityMatches.Match> matches = List.of();
    private transient IdentityMatches.Match chosen;
    private TokenChoice<Boolean> method;
    private JRootPane root;
    private transient java.util.function.Consumer<String> title = value -> { };
    private Stage stage;
    private boolean changing, busy, retired;
    private int row;
    private JComponent firstInvalid;

    public TokenManagementPanel(VaultState base, TokenAlternative original, String explanation,
                                Supplier<VaultState> current, Submit submit, Runnable abandon) {
        Edt.require();
        this.editBase = base; this.original = original; this.explanation = explanation;
        this.current = current; this.submit = submit; this.abandon = abandon;
        setLayout(new BorderLayout(0, 24)); setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        setBackground(DesktopStyle.surface()); body.setBackground(DesktopStyle.surface());
        issuer.getDocument().putProperty("filterNewlines", Boolean.FALSE);
        account.getDocument().putProperty("filterNewlines", Boolean.FALSE);
        for (JTextField input : List.of(issuer, account, period)) { DesktopStyle.input(input); }
        for (JPasswordField input : List.of(secret, uri)) {
            // Standard password delegate retains echo/accessibility while respecting the semantic palette.
            input.setUI(new javax.swing.plaf.basic.BasicPasswordFieldUI());
            input.setBackground(DesktopStyle.surfaceInput()); input.setForeground(DesktopStyle.text()); input.setCaretColor(DesktopStyle.text());
            input.setBorder(new DesktopStyle.ControlBorder(null, new Insets(6, 8, 6, 8)));
            Dimension natural = input.getPreferredSize();
            input.setPreferredSize(new Dimension(natural.width, Math.max(DesktopStyle.CONTROL, natural.height)));
            input.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent event) { input.repaint(); }
                @Override public void focusLost(java.awt.event.FocusEvent event) { input.repaint(); }
            });
        }
        for (TokenChoice<?> choices : List.of(algorithm, digits)) { styleChoices(choices); }
        TokenEditorPanel.FormBody wrapper = new TokenEditorPanel.FormBody(); wrapper.setBackground(DesktopStyle.surface()); wrapper.setOpaque(false);
        wrapper.add(body, BorderLayout.NORTH);
        scroll = new JScrollPane(wrapper, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.setOpaque(false); scroll.getViewport().setOpaque(false); scroll.setBackground(DesktopStyle.surface()); scroll.getViewport().setBackground(DesktopStyle.surface()); scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        footer.setOpaque(false); actions.setOpaque(false); actionRow.setOpaque(false);
        actionRow.add(actions, BorderLayout.EAST);
        notice.setVisible(false); footer.add(notice, BorderLayout.NORTH); footer.add(actionRow, BorderLayout.SOUTH); add(footer, BorderLayout.SOUTH);
        DesktopStyle.action(cancel, DesktopStyle.ActionRole.SecondaryAction, false);
        primary.addActionListener(event -> activate());
        cancel.addActionListener(event -> cancel());
        if (original == null) { acquisition(false); } else {
            issuer.setText(original.descriptor().issuer()); account.setText(original.descriptor().account()); edit();
        }
    }
    public String title() {
        return switch (stage) {
            case EDIT -> "Edit TOTP"; case DELETE -> "Delete TOTP?";
            case DUPLICATE -> "TOTP already exists";
            case REPLACE_REVIEW -> "Change authenticator setup";
            default -> changing ? "Change authenticator setup" : "Add TOTP";
        };
    }
    public Dimension taskSize() {
        return taskSize(preferredTaskWidth());
    }
    int preferredTaskWidth() {
        Insets padding = getInsets();
        Insets edge = scroll.getInsets();
        Insets viewport = scroll.getViewportBorder() == null ? new Insets(0, 0, 0, 0)
                : scroll.getViewportBorder().getBorderInsets(scroll);
        int bodyWidth = body.getPreferredSize().width + edge.left + edge.right + viewport.left + viewport.right;
        return Math.max(680, Math.max(bodyWidth, footer.getPreferredSize().width)
                + padding.left + padding.right);
    }
    Dimension taskSize(int width) {
        return TaskDialogSizing.contentSize(body, scroll, footer, getInsets(),
                ((BorderLayout) getLayout()).getVgap(), width);
    }
    public void installDialog(JRootPane root, java.util.function.Consumer<String> title) {
        this.root = root; this.title = title;
        SwingUsability.dialog(root, primary, this::cancel); defaults();
    }
    private void defaults() {
        title.accept(title());
        if (root != null) { root.setDefaultButton(stage == Stage.DUPLICATE || stage == Stage.DELETE ? cancel : primary); }
    }
    public void focusInitialField() {
        if (retired) { return; }
        (stage == Stage.EDIT ? issuer : stage == Stage.ACQUIRE ? method.options.get(false) : cancel).requestFocusInWindow();
    }
    private void reset(Stage next, String action) {
        stage = next; row = 0; body.removeAll(); actions.removeAll(); inputs.clear(); errors.clear(); firstInvalid = null;
        Component leading = ((BorderLayout) actionRow.getLayout()).getLayoutComponent(BorderLayout.WEST);
        if (leading != null) { actionRow.remove(leading); }
        notice.setText(""); notice.setVisible(false);
        primary.setText(action); DesktopStyle.action(primary, DesktopStyle.ActionRole.PrimaryAction, false);
        actions.add(cancel); actions.add(primary);
    }
    private void finishLayout() { revalidate(); repaint(); }
    private void finishTask() { finishLayout(); defaults(); }
    private void heading(String text) {
        JTextArea heading = text(text); heading.setFont(DesktopStyle.font(DesktopStyle.Typography.SectionTitle)); wide(heading, 24);
    }
    private void wide(JComponent control, int top) {
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(top, 0, 0, 0);
        if (control instanceof JButton) {
            JPanel line = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0)); line.setOpaque(false); line.add(control); body.add(line, c);
        } else { body.add(control, c); }
    }
    private void field(Field key, String name, JComponent control) {
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = row; c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.insets = new Insets(12, 0, 0, 16); JLabel label = SwingUsability.label(name, control); label.setFont(DesktopStyle.font(DesktopStyle.Typography.Body));
        label.setForeground(DesktopStyle.textSecondary()); body.add(label, c);
        JPanel group = new JPanel(new BorderLayout(0, 4)); group.setOpaque(false); group.add(control, BorderLayout.NORTH);
        JTextArea error = text(""); error.setForeground(DesktopStyle.danger()); error.setVisible(false);
        error.getAccessibleContext().setAccessibleName(name + " validation"); group.add(error, BorderLayout.CENTER);
        errors.put(key, error); inputs.put(key, control); trackFocus(control);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(12, 0, 0, 0);
        body.add(group, c); row++;
    }
    private void trackFocus(JComponent component) {
        if (component.getClientProperty("totipo.scrollFocus") == null) {
            component.putClientProperty("totipo.scrollFocus", Boolean.TRUE);
            component.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent event) {
                    if (!retired && SwingUtilities.isDescendingFrom(component, body)) {
                        body.scrollRectToVisible(SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), body));
                    }
                }
            });
        }
        for (Component child : component.getComponents()) {
            if (child instanceof JComponent control) { trackFocus(control); }
        }
    }
    static JTextArea text(String value) {
        JTextArea area = new JTextArea(value); area.setEditable(false); area.setOpaque(false);
        area.setLineWrap(true); area.setWrapStyleWord(true); area.setFont(DesktopStyle.font(DesktopStyle.Typography.Body));
        area.setForeground(DesktopStyle.text()); area.setSize(540, Short.MAX_VALUE);
        return area;
    }
    private JButton secondary(String name, Runnable action, boolean danger) {
        JButton button = new JButton(name);
        DesktopStyle.action(button, danger ? DesktopStyle.ActionRole.DestructiveAction : DesktopStyle.ActionRole.SecondaryAction, false);
        button.addActionListener(event -> { if (!busy && !retired) { action.run(); } }); return button;
    }
    private void identity(TokenDescriptor descriptor) {
        JTextArea name = text(TokenPresentation.primary(descriptor)); name.setFont(DesktopStyle.font(DesktopStyle.Typography.ScreenTitle)); wide(name, 0);
        String accountText = TokenPresentation.secondary(descriptor);
        if (!accountText.isBlank()) { wide(text(accountText), 8); }
    }
    private void edit() {
        reset(Stage.EDIT, "Save"); changing = false;
        if (explanation != null && explanation.startsWith("You are editing this version only.")) {
            wide(text("You are editing this version only. Saving does not resolve the other conflicting versions."), 0);
        }
        field(Field.ISSUER, "Issuer / service", issuer); field(Field.ACCOUNT, "Account", account);
        heading("Authenticator setup"); wide(text(SetupSummary.format(original.descriptor())), 8);
        wide(secondary("Change setup…", () -> acquisition(true), false), 8);
        JButton delete = secondary("Delete TOTP…", this::delete, true);
        delete.getAccessibleContext().setAccessibleDescription("Delete " + TokenPresentation.identity(original.descriptor()));
        actionRow.add(delete, BorderLayout.WEST, 0); finishTask();
    }
    private void acquisition(boolean change) {
        discardSetup(); changing = change; target = change ? original : null; targetBase = change ? editBase : null;
        method = new TokenChoice<>("Acquisition method", List.of(false, true), manual -> manual ? "Manual entry" : "Setup URI", false);
        styleChoices(method);
        method.options.values().forEach(button -> button.addActionListener(event -> {
            if (!busy && !retired) { clearInputs(); acquisitionBody(); }
        }));
        acquisitionBody();
    }
    private static void styleChoices(TokenChoice<?> choices) {
        choices.setOpaque(false);
        for (AbstractButton button : choices.options.values()) {
            button.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI() {
                @Override protected void paintButtonPressed(Graphics graphics, AbstractButton toggle) {
                    graphics.setColor(DesktopStyle.surfaceSelected()); graphics.fillRect(0, 0, toggle.getWidth(), toggle.getHeight());
                }
            });
            button.setBackground(DesktopStyle.surfaceRaised()); button.setForeground(DesktopStyle.text());
            button.setFont(DesktopStyle.font(DesktopStyle.Typography.Body).deriveFont(button.isSelected() ? Font.BOLD : Font.PLAIN));
            button.addItemListener(event -> button.setFont(DesktopStyle.font(DesktopStyle.Typography.Body)
                    .deriveFont(button.isSelected() ? Font.BOLD : Font.PLAIN)));
            button.setBorder(new DesktopStyle.ControlBorder(null, new Insets(6, 8, 6, 8)));
            Dimension natural = button.getPreferredSize();
            button.setPreferredSize(new Dimension(natural.width, Math.max(DesktopStyle.CONTROL, natural.height)));
            button.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent event) { button.repaint(); }
                @Override public void focusLost(java.awt.event.FocusEvent event) { button.repaint(); }
            });
        }
    }
    private void acquisitionBody() {
        reset(Stage.ACQUIRE, "Review"); wide(method, 0);
        if (!method.selected()) {
            wide(text("Paste an otpauth:// TOTP setup URI into the field. Review it before saving."), 16);
            field(Field.URI, "Setup URI", uri);
        } else {
            if (!changing) { field(Field.ISSUER, "Issuer / service", issuer); field(Field.ACCOUNT, "Account", account); }
            field(Field.SECRET, "New Base32 secret", secret);
            heading("Authenticator configuration");
            field(Field.ALGORITHM, "Algorithm", algorithm); field(Field.DIGITS, "Digits", digits);
            field(Field.PERIOD, "Period (seconds)", period);
        }
        if (changing) { wide(text("Only the authenticator setup changes. Issuer and account stay as saved."), 24); }
        finishTask();
    }
    private void review(boolean replacement) {
        reset(replacement ? Stage.REPLACE_REVIEW : Stage.REVIEW, replacement ? "Save setup" : "Add");
        identity(replacement ? target.descriptor() : setup.review());
        if (replacement) {
            heading("Current authenticator setup"); wide(text(SetupSummary.format(target.descriptor())), 8);
            heading("Proposed authenticator setup");
        } else { heading("Authenticator setup"); }
        wide(text(SetupSummary.format(setup.review())), 8);
        if (replacement) { wide(text("This replaces the authenticator setup of this TOTP. Its identity stays the same."), 24); }
        JButton back = secondary("Back", () -> acquisition(changing), false); actions.add(back, 0);
        finishTask(); primary.requestFocusInWindow();
    }
    private void duplicates(List<IdentityMatches.Match> found) {
        matches = found; chosen = null; reset(Stage.DUPLICATE, "Update Existing…");
        DesktopStyle.action(primary, DesktopStyle.ActionRole.SecondaryAction, false);
        identity(setup.review()); wide(text("An active TOTP already has this issuer and account. Choose an existing TOTP to update, or add another."), 24);
        if (found.size() == 1) {
            wide(text(SetupSummary.format(found.get(0).alternative().descriptor())), 16);
            if (found.get(0).conflict()) { wide(text("This is one conflicting version. Updating it does not resolve the conflict."), 8); }
        } else {
            ButtonGroup group = new ButtonGroup();
            for (int i = 0; i < found.size(); i++) {
                IdentityMatches.Match match = found.get(i);
                JRadioButton option = new JRadioButton("TOTP " + (i + 1) + " · " + SetupSummary.format(match.alternative().descriptor())
                        + (match.conflict() ? " · Conflicting version" : ""));
                option.setOpaque(false); option.setForeground(DesktopStyle.text()); group.add(option); option.addActionListener(event -> chosen = match); wide(option, 12);
                if (i == 0) { inputs.put(Field.ACCOUNT, option); }
            }
            wide(text("If these setups look identical, Totipo cannot tell which one you intend. Choose deliberately or cancel."), 16);
        }
        JButton another = secondary("Add Another", () -> publish(null, targetBase), false);
        actions.add(another, 1); finishTask(); cancel.requestFocusInWindow();
    }
    private void delete() {
        clearInputs(); reset(Stage.DELETE, "Delete TOTP"); identity(original.descriptor());
        wide(text("This removes the TOTP from the active vault.\nPrevious versions remain in vault history."), 24);
        DesktopStyle.confirmDanger(primary);
        finishTask(); cancel.requestFocusInWindow();
    }
    private void activate() {
        Edt.require(); if (busy || retired) { return; } clearErrors();
        try {
            switch (stage) {
                case EDIT -> {
                    SetupValidation.identity(issuer.getText(), account.getText());
                    TokenDescriptor d = original.descriptor();
                    send(editBase, original, new TokenDraft(new TokenDescriptor(d.status(), issuer.getText(), account.getText(),
                            d.algorithm(), d.digits(), d.period()), null));
                }
                case ACQUIRE -> {
                    if (method.selected()) {
                        char[] input = secret.getPassword();
                        try { setup = SetupValidation.manual(changing ? original.descriptor().issuer() : issuer.getText(),
                                changing ? original.descriptor().account() : account.getText(), input,
                                algorithm.selected(), digits.selected(), period.getText()); }
                        finally { Arrays.fill(input, '\0'); }
                    } else { setup = SetupUri.parse(uri.getPassword()); }
                    clearInputs(); review(changing);
                }
                case REVIEW -> {
                    targetBase = current.get();
                    List<IdentityMatches.Match> found = IdentityMatches.find(targetBase, setup.review());
                    if (found.isEmpty()) { publish(null, targetBase); } else { duplicates(found); }
                }
                case DUPLICATE -> {
                    IdentityMatches.Match selected = matches.size() == 1 ? matches.get(0) : chosen;
                    if (selected == null) { throw new SetupValidation.Invalid(Field.ACCOUNT, "Choose the existing TOTP to update, or Add Another."); }
                    target = selected.alternative(); review(true);
                }
                case REPLACE_REVIEW -> publish(target, targetBase);
                case DELETE -> {
                    TokenDescriptor d = original.descriptor();
                    send(editBase, original, new TokenDraft(new TokenDescriptor(TokenStatus.TOMBSTONED, d.issuer(), d.account(),
                            d.algorithm(), d.digits(), d.period()), null));
                }
            }
        } catch (SetupValidation.Invalid invalid) { invalid(invalid.field(), invalid.getMessage()); }
        catch (IllegalArgumentException invalid) { invalid(Field.ALGORITHM, "Choose a supported authenticator configuration."); }
    }
    private void publish(TokenAlternative alternative, VaultState base) {
        // A changed active collection must be reviewed again rather than using a stale duplicate decision.
        if (original == null && current.get() != targetBase) {
            targetBase = current.get(); target = null;
            List<IdentityMatches.Match> found = IdentityMatches.find(targetBase, setup.review());
            if (found.isEmpty()) { review(false); } else { duplicates(found); }
            notice("The vault view changed. Review your choice again."); return;
        }
        TokenDraft draft = setup.transfer(alternative == null ? null : alternative.descriptor()); discardSetup();
        send(base, alternative, draft);
    }
    private void send(VaultState base, TokenAlternative alternative, TokenDraft draft) {
        clearInputs(); busy(true, "Saving…"); submit.save(base, alternative, draft);
    }
    private void clearErrors() {
        firstInvalid = null;
        errors.values().forEach(error -> { error.setText(""); error.setVisible(false); });
        inputs.values().forEach(input -> input.getAccessibleContext().setAccessibleDescription(null));
    }
    private void invalid(Field field, String message) {
        JTextArea error = errors.get(field);
        if (error != null) { error.setText(message); error.setVisible(true); } else { notice(message); }
        firstInvalid = inputs.get(field);
        if (firstInvalid != null) {
            firstInvalid.getAccessibleContext().setAccessibleDescription(message);
            firstInvalid.requestFocusInWindow();
            revalidate();
            SwingUtilities.invokeLater(() -> {
                if (!retired && firstInvalid != null) { body.scrollRectToVisible(SwingUtilities.convertRectangle(firstInvalid.getParent(), firstInvalid.getBounds(), body)); }
            });
        }
        finishLayout();
    }
    JComponent firstInvalid() { return firstInvalid; }
    private void notice(String message) { notice.setText(message); notice.setVisible(!message.isBlank()); finishLayout(); }
    public void busy(boolean value, String message) {
        Edt.require(); busy = value;
        enable(this, !value && !retired); notice(message);
        if (!value && setup == null && (stage == Stage.REVIEW || stage == Stage.REPLACE_REVIEW || stage == Stage.DUPLICATE)) {
            acquisition(changing); notice(message + " Enter the new setup again before retrying.");
        }
    }
    private static void enable(Container parent, boolean enabled) {
        for (Component child : parent.getComponents()) {
            if (!(child instanceof JTextArea) && !(child instanceof JLabel)) { child.setEnabled(enabled); }
            if (child instanceof Container container) { enable(container, enabled); }
        }
    }
    private void clearInputs() { secret.setText(""); uri.setText(""); }
    private void discardSetup() { if (setup != null) { setup.close(); setup = null; } }
    public void cancel() {
        Edt.require(); if (busy || retired) { return; }
        // Secondary tasks cancel back to Edit; shell lock always calls retire directly.
        if (original != null && stage != Stage.EDIT) { clearInputs(); discardSetup(); edit(); return; }
        retire(); abandon.run();
    }
    public void retire() {
        Edt.require(); retired = true; discardSetup(); clearInputs(); matches = List.of(); chosen = null; target = null; targetBase = null;
        firstInvalid = null; inputs.clear(); errors.clear(); title = value -> { }; root = null; enable(this, false);
    }
}
