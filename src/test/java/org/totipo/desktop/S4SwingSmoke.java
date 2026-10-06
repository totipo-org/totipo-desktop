package org.totipo.desktop;

import org.totipo.*;
import org.totipo.storage.nio.NioTotipo;
import org.totipo.desktop.ui.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Real application/session Swing + Robot review with disposable fixture credentials. */
public class S4SwingSmoke extends S3SwingSmoke {
    static TokenId distinct, identical;
    static VaultState ready(VaultSession vaultSession, TokenId id, int alternatives) throws Exception {
        long limit = System.nanoTime() + 10_000_000_000L; vaultSession.requestRefresh();
        while (true) {
            VaultState state = vaultSession.state();
            if (state.token(id).filter(t -> t.alternatives().size() == alternatives).isPresent()) { return state; }
            if (System.nanoTime() > limit) { throw new AssertionError("Fixture not established"); } Thread.sleep(20);
        }
    }
    static TokenId conflict(VaultSession initial, boolean same) throws Exception {
        SaveResult.Saved created;
        try (CreateToken builder = initial.state().createToken(); NewSecret secret = NewSecret.copyOf(new byte[]{102})) {
            created = (SaveResult.Saved) builder.issuer(same ? "Same Service" : "Base").account(same ? "same account" : "base").secret(secret).save();
        }
        VaultState basis = ready(initial, created.tokenId(), 1); TokenAlternative original = basis.token(created.tokenId()).orElseThrow().alternatives().getFirst();
        try (UpdateToken builder = basis.update(original)) { builder.issuer(same ? "Same Service" : "Slie").account(same ? "same account" : "a").metadata(new ClientMetadata(java.util.Optional.of("fixture one"), java.util.Optional.empty())).save(); }
        try (UpdateToken builder = basis.update(original); NewSecret secret = NewSecret.copyOf(new byte[]{103})) {
            builder.issuer(same ? "Same Service" : "Sile").account(same ? "same account" : "x").secret(secret)
                    .status(same ? TokenStatus.ACTIVE : TokenStatus.TOMBSTONED).algorithm(same ? TotpAlgorithm.SHA1 : TotpAlgorithm.SHA256).digits(same ? 6 : 8).save();
        }
        ready(initial, created.tokenId(), 2); return created.tokenId();
    }
    static void open(boolean same) throws Exception {
        JButton resolve = edt(() -> all(frame).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(b -> b.getText().equals("Resolve") && all(b.getParent().getParent()).stream().anyMatch(c -> c instanceof JButton edit
                        && edit.getText().equals("Edit") && edit.getAccessibleContext().getAccessibleDescription().contains(same ? "Same Service" : "Slie")))
                .findFirst().orElseThrow());
        click(resolve); waitFor(() -> Arrays.stream(frame.getOwnedWindows()).anyMatch(w -> w instanceof JDialog && w.isVisible()));
    }
    static AbstractButton option(String name) throws Exception {
        return edt(() -> all(uncheckedDialog()).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast).filter(b -> b.getText().equals(name)).findFirst().orElseThrow());
    }
    static void capture(String name) throws Exception {
        robot.waitForIdle(); Thread.sleep(180); JDialog task = dialog();
        edt(() -> {
            JScrollPane scroll = all(task).stream().filter(JScrollPane.class::isInstance).map(JScrollPane.class::cast).findFirst().orElseThrow();
            if (all(task).stream().filter(JScrollPane.class::isInstance).count() != 1) { throw new AssertionError("Nested scrollers"); }
            for (Component c : all(task)) {
                if (c instanceof JButton button && List.of("Resolve", "Save Resolution", "Review Updated Conflict", "Cancel", "Back").contains(button.getText())) {
                    Rectangle bounds = SwingUtilities.convertRectangle(c.getParent(), c.getBounds(), task);
                    if (!new Rectangle(task.getSize()).contains(bounds)) { throw new AssertionError("Footer clipped: " + button.getText()); }
                }
                if (c instanceof JRadioButton button && button.getText() != null && !button.getText().isBlank()
                        && button.getWidth() < button.getPreferredSize().width) { throw new AssertionError("Radio label clipped: " + button.getText()); }
            }
            if (name.startsWith("simple") && scroll.getVerticalScrollBar().isVisible()) { throw new AssertionError("Ordinary simple task starts scrolling"); }
        });
        Files.createDirectories(Path.of("review/screenshots/s4")); Rectangle r = edt(() -> new Rectangle(task.getLocationOnScreen(), task.getSize()));
        ImageIO.write(robot.createScreenCapture(r), "png", Path.of("review/screenshots/s4/" + capturePrefix + "-" + name + ".png").toFile());
        System.out.println("Captured " + capturePrefix + "-" + name);
    }
    static void chooseText(String accessible) throws Exception {
        Component field = edt(() -> all(uncheckedDialog()).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast)
                .filter(f -> accessible.equals(f.getAccessibleContext().getAccessibleName())).findFirst().orElseThrow());
        edt(() -> ((JComponent) field).scrollRectToVisible(field.getBounds())); click(field);
    }
    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> { error.printStackTrace(); System.exit(1); });
        theme = args.length == 0 ? "dark" : args[0]; capturePrefix = theme + (args.length > 1 ? "-font" + args[1] : "");
        vault = Files.createTempDirectory("totipo-s4-review-");
        try (VaultSession initial = ((CreateVaultResult.Created) NioTotipo.create(vault, "review".toCharArray())).session()) {
            distinct = conflict(initial, false); identical = conflict(initial, true);
        }
        robot = new Robot(); robot.setAutoDelay(30);
        edt(() -> {
            if (!theme.equals("native")) {
                try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
                catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) { throw new AssertionError(e); }
            }
            if (theme.equals("dark")) {
                for (String key : List.of("Panel.background", "List.background", "TextField.background", "PasswordField.background", "TextArea.background", "ToggleButton.background", "RadioButton.background", "Button.background", "Viewport.background", "ScrollPane.background", "MenuBar.background", "Menu.background", "MenuItem.background", "PopupMenu.background")) { UIManager.put(key, new Color(38, 42, 47)); }
                for (String key : List.of("Label.foreground", "List.foreground", "TextField.foreground", "PasswordField.foreground", "TextArea.foreground", "ToggleButton.foreground", "RadioButton.foreground", "Button.foreground", "Menu.foreground", "MenuItem.foreground")) { UIManager.put(key, new Color(230, 232, 235)); }
                UIManager.put("List.selectionBackground", new Color(80, 120, 190)); UIManager.put("List.selectionForeground", Color.WHITE);
            }
            ApplicationFonts.install();
            if (args.length > 1) {
                float size = Float.parseFloat(args[1]); var defaults = UIManager.getDefaults();
                for (Object key : new ArrayList<>(defaults.keySet())) { if (defaults.get(key) instanceof Font font) { defaults.put(key, new javax.swing.plaf.FontUIResource(font.deriveFont(size))); } }
            }
            frame = new ShellFrame(); frame.setLocation(0, 0);
            VaultAccess access = new VaultAccess() {
                public OpenResult open(Path path, char[] password) { var result = NioTotipo.open(path, password); if (result instanceof OpenResult.Opened opened) { session = opened.session(); } return result; }
                public CreateVaultResult create(Path path, char[] password) { return NioTotipo.create(path, password); }
            };
            app = new DesktopApplication(access, frame, path -> new VaultContent(frame)); app.show(); app.begin(vault, "review".toCharArray(), false);
        });
        waitFor(() -> app.state() == ShellState.UNLOCKED && all(frame).stream().filter(JButton.class::isInstance).map(JButton.class::cast).filter(b -> b.getText().equals("Resolve")).count() == 2);
        open(false); capture("simple");
        if (edt(() -> all(uncheckedDialog()).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).anyMatch(AbstractButton::isSelected))) { throw new AssertionError("Preselected version"); }
        click("Resolve"); capture("simple-validation");
        if (!(edt(() -> KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()) instanceof JRadioButton)) { throw new AssertionError("Validation did not focus version"); }
        click(option("Sile · x")); click("Combine details…"); capture("details");
        if (edt(() -> all(uncheckedDialog()).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).anyMatch(AbstractButton::isSelected))) { throw new AssertionError("Preference leaked into composition"); }
        click("Save Resolution"); capture("details-validation");
        chooseText("Issuer choice: Slie"); chooseText("Account choice: a"); click(option("Active"));
        click(option("Setup used by Slie · a"));
        JPasswordField secret = edt(() -> all(uncheckedDialog()).stream().filter(JPasswordField.class::isInstance).map(JPasswordField.class::cast).findFirst().orElseThrow());
        edt(() -> { secret.scrollRectToVisible(secret.getBounds()); secret.requestFocusInWindow(); }); text("New Base32 secret", "MY");
        capture("custom-setup");
        click(option("Setup used by Slie · a")); if (edt(() -> secret.getPassword().length) != 0) { throw new AssertionError("Abandoned secret retained"); }
        click("Back"); if (edt(() -> all(uncheckedDialog()).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).anyMatch(AbstractButton::isSelected))) { throw new AssertionError("Back chose whole version"); }
        key(KeyEvent.VK_ESCAPE); closed();
        open(true); capture("indistinguishable"); click("Cancel"); closed();
        open(false); click("Combine details…"); text("New Base32 secret", "MY");
        JPasswordField staleSecret = edt(() -> all(uncheckedDialog()).stream().filter(JPasswordField.class::isInstance).map(JPasswordField.class::cast).findFirst().orElseThrow());
        try (VaultSession remote = ((OpenResult.Opened) NioTotipo.open(vault, "review".toCharArray())).session()) {
            VaultState state = ready(remote, distinct, 2); TokenAlternative original = state.token(distinct).orElseThrow().alternatives().getFirst();
            try (UpdateToken builder = state.update(original)) { builder.issuer("Changed elsewhere").save(); }
            session.requestRefresh(); waitFor(() -> all(uncheckedDialog()).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Review Updated Conflict")));
            if (edt(() -> staleSecret.getPassword().length) != 0) { throw new AssertionError("Stale draft secret retained"); }
            capture("updated-conflict"); click("Review Updated Conflict");
        }
        click("Cancel"); closed();
        // Publish the deliberately composed custom setup through the actual controller/library.
        JButton distinctResolve = edt(() -> all(frame).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(b -> b.getText().equals("Resolve") && all(b.getParent().getParent()).stream().anyMatch(c -> c instanceof JButton edit
                        && edit.getText().equals("Edit") && !edit.getAccessibleContext().getAccessibleDescription().contains("Same Service"))).findFirst().orElseThrow());
        click(distinctResolve); click("Combine details…");
        for (String field : List.of("Issuer choice:", "Account choice:")) {
            JTextField chosen = edt(() -> all(uncheckedDialog()).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast)
                    .filter(f -> f.getAccessibleContext().getAccessibleName() != null && f.getAccessibleContext().getAccessibleName().startsWith(field)).findFirst().orElseThrow());
            click(chosen);
        }
        click(option("Active")); text("New Base32 secret", "MY");
        click("Save Resolution"); closed(); ready(session, distinct, 1);
        if (edt(() -> all(frame).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Copy")))) { throw new AssertionError("Resolution automatically revealed a code"); }
        open(true); click("Combine details…"); text("New Base32 secret", "MY");
        JPasswordField retiring = edt(() -> all(uncheckedDialog()).stream().filter(JPasswordField.class::isInstance).map(JPasswordField.class::cast).findFirst().orElseThrow());
        shortcut(KeyEvent.VK_L); waitFor(() -> app.state() == ShellState.LOCKED); closed();
        if (edt(() -> retiring.getPassword().length) != 0) { throw new AssertionError("Lock retained secret"); }
        edt(() -> app.begin(vault, "review".toCharArray(), false)); waitFor(() -> app.state() == ShellState.UNLOCKED); closed();
        open(true); click(option("Version 1 · Same Service · same account")); click("Resolve"); closed();
        ready(session, identical, 1);
        edt(app::shutdown); waitFor(() -> !frame.isDisplayable());
        System.out.println("PASS " + capturePrefix + " S4 Swing/Robot whole/detailed/validation/stale/lock/keep smoke");
    }
}
