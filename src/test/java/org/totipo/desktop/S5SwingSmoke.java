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

/** Actual Swing/Robot surfaces, real NIO password replacement and Design v0.8 qualification. */
public class S5SwingSmoke extends S4SwingSmoke {
    static void captureSurface(String name) throws Exception {
        robot.waitForIdle(); Thread.sleep(150);
        Window task = edt(() -> Arrays.stream(Window.getWindows()).filter(w -> w.isVisible() && w instanceof JDialog)
                .reduce((a, b) -> b).orElse(frame));
        edt(() -> {
            for (Component c : all(task)) {
                if (c.isShowing() && c instanceof JButton b && List.of("Change Password", "Cancel", "Close", "Try Again", "Open").contains(b.getText())) {
                    Rectangle r = SwingUtilities.convertRectangle(c.getParent(), c.getBounds(), task);
                    if (!new Rectangle(task.getSize()).contains(r)) { throw new AssertionError("Clipped action: " + b.getText()); }
                }
                if (c.isShowing() && c instanceof JPasswordField field) {
                    if (field.getHeight() < field.getPreferredSize().height) { throw new AssertionError("Clipped password field"); }
                    JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, field);
                    if (viewport != null) {
                        Rectangle bounds = SwingUtilities.convertRectangle(field.getParent(), field.getBounds(), viewport);
                        if (bounds.x < 0 || bounds.x + bounds.width > viewport.getWidth()) { throw new AssertionError("Password field exceeds viewport"); }
                    }
                }
                if (c.isShowing() && c instanceof JScrollPane scroll && scroll.getHorizontalScrollBar().isVisible()) {
                    throw new AssertionError("Horizontal task scrolling");
                }
            }
        });
        Files.createDirectories(Path.of("review/screenshots/s5"));
        Rectangle bounds = edt(() -> new Rectangle(task.getLocationOnScreen(), task.getSize()));
        ImageIO.write(robot.createScreenCapture(bounds), "png", Path.of("review/screenshots/s5/" + capturePrefix + "-" + name + ".png").toFile());
        System.out.println("Captured " + capturePrefix + "-" + name);
    }
    static void menu(String name) throws Exception {
        JMenuItem item = edt(() -> all(frame.getJMenuBar()).stream().filter(JMenuItem.class::isInstance).map(JMenuItem.class::cast)
                .filter(i -> i.getText().equals(name)).findFirst().orElse(null));
        // JMenu popup children are outside getComponents until open.
        if (item == null) {
            item = edt(() -> {
                for (int i = 0; i < frame.getJMenuBar().getMenuCount(); i++) {
                    for (Component c : frame.getJMenuBar().getMenu(i).getMenuComponents()) {
                        if (c instanceof JMenuItem m && m.getText().equals(name)) { return m; }
                    }
                }
                throw new AssertionError("Missing menu " + name);
            });
        }
        JMenuItem selected = item; edt(() -> selected.doClick(0)); Thread.sleep(150);
    }
    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> { error.printStackTrace(); System.exit(1); });
        theme = args.length == 0 ? "dark" : args[0]; capturePrefix = theme + (args.length > 1 ? "-font" + args[1] : "")
                + (System.getProperty("sun.java2d.uiScale") == null ? "" : "-scale" + System.getProperty("sun.java2d.uiScale"));
        robot = new Robot(); robot.setAutoDelay(35);
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
        });
        vault = Files.createTempDirectory("totipo-s5-review-");
        try (VaultSession initial = ((CreateVaultResult.Created) NioTotipo.create(vault, "review".toCharArray())).session()) { distinct = conflict(initial, false); }
        edt(() -> {
            frame = new ShellFrame(); frame.setLocation(0, 0);
            System.out.println("Environment " + UIManager.getLookAndFeel().getClass().getName()
                    + "; font=" + UIManager.getFont("Label.font").getSize() + "; transform=" + frame.getGraphicsConfiguration().getDefaultTransform());
            VaultAccess access = new VaultAccess() {
                public OpenResult open(Path path, char[] password) { var result = NioTotipo.open(path, password); if (result instanceof OpenResult.Opened opened) { session = opened.session(); } return result; }
                public CreateVaultResult create(Path path, char[] password) { return NioTotipo.create(path, password); }
            };
            app = new DesktopApplication(access, frame, path -> new VaultContent(frame), new org.totipo.desktop.clipboard.TotpClipboard(), new VaultPreferences() {
                public java.util.Optional<Path> lastVault() { return java.util.Optional.of(vault); }
                public void setLastVault(Path path) { }
                public void clearLastVault() { }
            }); app.show();
        });
        waitFor(() -> app.state() == ShellState.LOCKED && frame.isShowing()); captureSurface("locked");
        waitFor(() -> KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JPasswordField);
        edt(() -> all(frame).stream().filter(JPasswordField.class::isInstance).map(JPasswordField.class::cast).findFirst().orElseThrow().setText("wrong"));
        key(KeyEvent.VK_ENTER); waitFor(() -> all(frame).stream().anyMatch(c -> c instanceof JTextArea a && a.getText().contains("that password")));
        if (edt(app::state) != ShellState.LOCKED) { throw new AssertionError("Wrong password blocked vault"); }
        edt(() -> app.open("review".toCharArray())); waitFor(() -> app.state() == ShellState.UNLOCKED && all(frame).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Resolve")));
        shortcut(KeyEvent.VK_F); key(KeyEvent.VK_DOWN); captureSurface("main-focus-selection");
        if (edt(() -> all(frame).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Copy")))) { throw new AssertionError("Focus revealed code"); }
        click(edt(() -> frame.getJMenuBar().getMenu(1))); captureSurface("vault-menu"); key(KeyEvent.VK_ESCAPE);
        shortcut(KeyEvent.VK_F); key(KeyEvent.VK_DOWN); key(KeyEvent.VK_DOWN);
        key(KeyEvent.VK_SPACE);
        waitFor(() -> all(frame).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Copy")));
        key(KeyEvent.VK_ENTER); shortcut(KeyEvent.VK_R);
        menu("About This Vault…"); captureSurface("about"); key(KeyEvent.VK_ESCAPE); closed();
        menu("Change Vault Password…"); captureSurface("password");
        text("Current password", "review"); text("New password", "one"); text("Confirm new password", "two"); click("Change Password");
        waitFor(() -> KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JPasswordField p && "Confirm new password".equals(p.getAccessibleContext().getAccessibleName())); captureSurface("password-mismatch");
        text("Current password", "review");
        JButton submit = button(dialog(), "Change Password");
        edt(() -> SwingUtilities.invokeLater(() -> submit.doClick(0)));
        waitFor(() -> Arrays.stream(Window.getWindows()).anyMatch(w -> w instanceof JDialog d && d.isVisible() && d.getTitle().equals("Change to Empty Password?")));
        captureSurface("empty-password-confirmation");
        JDialog confirmation = edt(() -> Arrays.stream(Window.getWindows()).filter(w -> w instanceof JDialog d && d.isVisible() && d.getTitle().equals("Change to Empty Password?")).map(JDialog.class::cast).findFirst().orElseThrow());
        click(button(confirmation, "Cancel"));
        text("Current password", "review"); text("New password", "replacement"); text("Confirm new password", "replacement"); click("Change Password"); closed();
        try (VaultSession check = ((OpenResult.Opened) NioTotipo.open(vault, "replacement".toCharArray())).session()) {
            if (!check.fingerprint().equals(session.fingerprint())) { throw new AssertionError("Password change changed vault identity"); }
        }
        menu("Change Vault Password…"); text("New password", "abandoned");
        JPasswordField abandoned = edt(() -> all(uncheckedDialog()).stream().filter(JPasswordField.class::isInstance).map(JPasswordField.class::cast)
                .filter(p -> "New password".equals(p.getAccessibleContext().getAccessibleName())).findFirst().orElseThrow());
        shortcut(KeyEvent.VK_L); waitFor(() -> app.state() == ShellState.LOCKED); closed();
        if (edt(() -> abandoned.getPassword().length) != 0) { throw new AssertionError("Lock retained password"); }
        edt(() -> app.open("replacement".toCharArray())); waitFor(() -> app.state() == ShellState.UNLOCKED && all(frame).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Resolve")));
        open(false); captureSurface("simple-resolver");
        if (edt(() -> all(uncheckedDialog()).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).anyMatch(AbstractButton::isSelected))) { throw new AssertionError("Focus preselected version"); }
        key(KeyEvent.VK_DOWN);
        if (edt(() -> all(uncheckedDialog()).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).filter(AbstractButton::isSelected).count()) != 1) { throw new AssertionError("Radio arrow navigation unavailable"); }
        click(option("Slie · a")); captureSurface("simple-resolver-selected"); click("Combine details…"); captureSurface("detailed-resolver"); key(KeyEvent.VK_ESCAPE); closed();
        edt(app::shutdown); waitFor(() -> !frame.isDisplayable());
        Path blocked = Files.createTempDirectory("totipo-s5-invalid-"); Files.writeString(blocked.resolve("vault"), "TOTIPO-VLT\u007funsupported");
        edt(() -> {
            frame = new ShellFrame(); frame.setLocation(0, 0);
            app = new DesktopApplication(new NioVaultAccess(), frame, path -> new VaultContent(frame), new org.totipo.desktop.clipboard.TotpClipboard(), new VaultPreferences() {
                public java.util.Optional<Path> lastVault() { return java.util.Optional.of(blocked); }
                public void setLastVault(Path path) { }
                public void clearLastVault() { }
            }); app.show();
        });
        waitFor(() -> app.state() == ShellState.LOCKED && frame.isShowing());
        Files.setPosixFilePermissions(blocked.resolve("vault"), java.util.Set.of());
        edt(() -> app.open("fixture".toCharArray())); waitFor(() -> app.state() == ShellState.BLOCKING_VAULT_STATE); captureSurface("blocking-unavailable");
        Files.setPosixFilePermissions(blocked.resolve("vault"), java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        click(button(frame, "Try Again")); waitFor(() -> app.state() == ShellState.LOCKED);
        edt(() -> app.open("fixture".toCharArray())); waitFor(() -> app.state() == ShellState.BLOCKING_VAULT_STATE); captureSurface("blocking-invalid-or-unsupported");
        click(button(frame, "Details…")); captureSurface("blocking-details"); key(KeyEvent.VK_ESCAPE); closed();
        click(button(frame, "Try Again")); waitFor(() -> app.state() == ShellState.LOCKED);
        edt(app::shutdown); waitFor(() -> !frame.isDisplayable());
        edt(() -> { frame = new ShellFrame(); app = new DesktopApplication(new NioVaultAccess(), frame, path -> new VaultContent(frame)); app.show(); });
        captureSurface("no-vault"); edt(app::shutdown); waitFor(() -> !frame.isDisplayable());
        for (Path fixture : List.of(vault, blocked)) {
            try (var paths = Files.walk(fixture)) { for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) { Files.delete(path); } }
        }
        System.out.println("PASS " + capturePrefix + " S5 Design v0.8 Swing/Robot");
        System.exit(0);
    }
}
