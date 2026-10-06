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
import java.util.Comparator;
import java.util.List;

/** Actual Swing/NIO polish review; fixture values are disposable, never user credentials. */
public class PostS5PolishSwingSmoke extends S5SwingSmoke {
    static void selectionStayed(JComponent target) throws Exception {
        edt(() -> {
            var selected = all(frame).stream().filter(JComponent.class::isInstance).map(JComponent.class::cast)
                    .filter(c -> "divider".equals(c.getClientProperty("totipo.rowPresentation"))
                            && c.getAccessibleContext().getAccessibleStateSet().contains(javax.accessibility.AccessibleState.SELECTED)).toList();
            if (!selected.equals(List.of(target))) { throw new AssertionError("Row action moved semantic selection: " + selected); }
        });
    }
    static void mouseReveal(JComponent target, boolean selected, String capture) throws Exception {
        if (selected) { edt(() -> { target.requestFocusInWindow(); }); robot.waitForIdle(); selectionStayed(target); }
        long revealed = edt(() -> all(frame).stream().filter(c -> c instanceof JButton b && b.isVisible()
                && List.of("Copy", "Copied").contains(b.getText())).count());
        JButton show = button(target, "Show Code");
        click(show); waitFor(() -> all(target).stream().anyMatch(c -> c instanceof JButton b && "Copy".equals(b.getText())));
        robot.waitForIdle(); selectionStayed(target); polishCapture(capture, frame);
        long after = edt(() -> all(frame).stream().filter(c -> c instanceof JButton b && b.isVisible()
                && List.of("Copy", "Copied").contains(b.getText())).count());
        if (after > revealed + 1) { throw new AssertionError("Action revealed another semantic row"); }
        click(button(target, "Copy")); selectionStayed(target);
    }
    static void polishCapture(String name, Window window) throws Exception {
        robot.waitForIdle(); Thread.sleep(180);
        edt(() -> {
            if (window instanceof JDialog task && task.getContentPane() instanceof TokenManagementPanel content
                    && "Delete TOTP?".equals(content.title())) {
                if (content.getHeight() != content.taskSize().height) { throw new AssertionError("Delete retained an earlier task height"); }
                if (all(content).stream().filter(JScrollPane.class::isInstance).map(JScrollPane.class::cast)
                        .anyMatch(s -> s.getVerticalScrollBar().isVisible())) { throw new AssertionError("Short Delete scrolls"); }
            }
            for (Component c : all(window)) {
                if (!c.isShowing()) { continue; }
                if (c instanceof JButton b && "Edit".equals(b.getText())) { throw new AssertionError("Inline Edit remains"); }
                if (c instanceof JScrollPane scroll && scroll.getHorizontalScrollBar().isVisible()) { throw new AssertionError("Horizontal scrolling"); }
                if (c instanceof JTextField || c instanceof JButton || c instanceof JToggleButton) {
                    if (c.getHeight() < c.getPreferredSize().height) { throw new AssertionError("Control height clipped: " + c); }
                }
                if (c instanceof JButton b && List.of("Cancel", "Review", "Open", "Change Vault…").contains(b.getText())) {
                    if (c.getParent() instanceof JPanel row && row.getInsets().top == 16 && row.getInsets().bottom == 16) {
                        if (c.getY() != 16 || row.getHeight() - c.getY() - c.getHeight() != 16) { throw new AssertionError("Footer not vertically centered: " + b.getText()); }
                    }
                    if (b.getInsets().left != 16 || b.getInsets().right != 16) { throw new AssertionError("Task button padding: " + b.getText()); }
                    Rectangle r = SwingUtilities.convertRectangle(c.getParent(), c.getBounds(), window);
                    if (!new Rectangle(window.getSize()).contains(r)) { throw new AssertionError("Footer clipped: " + b.getText()); }
                }
            }
        });
        Path destination = Path.of("review/screenshots/polish"); Files.createDirectories(destination);
        Rectangle bounds = edt(() -> new Rectangle(window.getLocationOnScreen(), window.getSize()));
        ImageIO.write(robot.createScreenCapture(bounds), "png", destination.resolve(capturePrefix + "-" + name + ".png").toFile());
        System.out.println("Captured " + capturePrefix + "-" + name + " " + bounds);
    }
    static void chooseMode(String name) throws Exception {
        edt(() -> all(uncheckedDialog()).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
                .filter(b -> name.equals(b.getText())).findFirst().orElseThrow().doClick(0));
    }
    static void acquisitionFits(JDialog task, Rectangle original) throws Exception {
        edt(() -> {
            if (task.getX() != original.x || task.getWidth() != original.width) { throw new AssertionError("Acquisition horizontal edges moved"); }
            JScrollPane scroll = all(task).stream().filter(JScrollPane.class::isInstance).map(JScrollPane.class::cast).findFirst().orElseThrow();
            if (scroll.getVerticalScrollBar().isVisible()) { throw new AssertionError("Short acquisition unnecessarily scrolls"); }
            if (!scroll.getViewport().getViewPosition().equals(new Point())) { throw new AssertionError("Stale acquisition scroll offset"); }
            var choices = all(task).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
                    .filter(b -> List.of("Setup URI", "Manual entry").contains(b.getText())).toList();
            if (choices.get(0).getWidth() != choices.get(1).getWidth() || choices.get(0).getHeight() != choices.get(1).getHeight()) { throw new AssertionError("Unequal acquisition segments"); }
            for (AbstractButton choice : choices) {
                if (choice.getWidth() < choice.getPreferredSize().width) { throw new AssertionError("Acquisition label clipped"); }
                if (!(choice instanceof JToggleButton) || !"ExclusiveChoice".equals(choice.getClientProperty("totipo.choiceRole"))) { throw new AssertionError("Acquisition is not ExclusiveChoice"); }
                if (choice.isSelected() != choice.getFont().isBold()) { throw new AssertionError("Selected choice needs a non-color cue"); }
            }
            if (choices.stream().filter(AbstractButton::isSelected).count() != 1) { throw new AssertionError("Acquisition must select exactly one mode"); }
            for (Component c : all(task)) {
                if (!c.isShowing() || !SwingUtilities.isDescendingFrom(c, scroll)) { continue; }
                if (c instanceof JTextField || c instanceof AbstractButton || c instanceof JTextArea) {
                    Rectangle bounds = SwingUtilities.convertRectangle(c.getParent(), c.getBounds(), scroll.getViewport());
                    if (!new Rectangle(scroll.getViewport().getExtentSize()).contains(bounds)) { throw new AssertionError("Acquisition control/message clipped"); }
                    if (c instanceof JTextArea area && c.getHeight() < area.getPreferredSize().height) { throw new AssertionError("Wrapped message height clipped"); }
                }
            }
            if (!"Review".equals(task.getRootPane().getDefaultButton().getText())) { throw new AssertionError("Acquisition semantics changed"); }
        });
    }
    static void validateUri(JDialog task, Rectangle original) throws Exception {
        int height = edt(() -> task.getHeight());
        text("Setup URI", "**"); click("Review"); robot.waitForIdle();
        waitFor(() -> all(task).stream().anyMatch(c -> c instanceof JTextArea area && area.isVisible()
                && "Setup URI validation".equals(area.getAccessibleContext().getAccessibleName()) && !area.getText().isBlank()));
        if (edt(() -> task.getHeight()) <= height) { throw new AssertionError("URI validation did not grow task height"); }
        acquisitionFits(task, original);
        if (!edt(() -> KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JPasswordField field
                && "Setup URI".equals(field.getAccessibleContext().getAccessibleName()))) { throw new AssertionError("Invalid URI field not focused"); }
        polishCapture("add-uri-invalid", task);
    }
    private static String synchronizedCountdown() throws Exception {
        return edt(() -> {
            var values = all(frame).stream().filter(c -> c instanceof JLabel label && label.isShowing()
                    && label.getText().matches("[0-9]+ sec")).map(c -> ((JLabel) c).getText()).toList();
            if (values.size() != 5 || values.stream().distinct().count() != 1) {
                throw new AssertionError("Same-period ordinary/conflict countdowns disagree: " + values);
            }
            var rings = all(frame).stream().filter(c -> c.isShowing() && c.getClass().getSimpleName().equals("CountdownRing"))
                    .map(c -> c.getAccessibleContext().getAccessibleName()).toList();
            if (rings.size() != 5 || rings.stream().distinct().count() != 1) {
                throw new AssertionError("Countdown ring presentation disagrees: " + rings);
            }
            return values.getFirst();
        });
    }

    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> { error.printStackTrace(); System.exit(1); });
        theme = args.length == 0 ? "dark" : args[0]; capturePrefix = theme + (args.length > 1 ? "-font" + args[1] : "");
        robot = new Robot(); robot.setAutoDelay(35);
        edt(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) { throw new AssertionError(e); }
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
        vault = Files.createTempDirectory("totipo-polish-review-");
        try (VaultSession initial = ((CreateVaultResult.Created) NioTotipo.create(vault, "review".toCharArray())).session()) {
            for (String issuer : List.of("GitHub", "Example Service", "Long issuer identity that uses the available row width")) {
                try (CreateToken builder = initial.state().createToken(); NewSecret secret = NewSecret.copyOf(new byte[]{102})) {
                    SaveResult.Saved saved = (SaveResult.Saved) builder.issuer(issuer).account("review@example.com").secret(secret).save();
                    ready(initial, saved.tokenId(), 1);
                }
            }
        }
        edt(() -> {
            frame = new ShellFrame(); frame.setLocation(0, 0);
            frame.renderShell(ShellState.NO_VAULT, null, "", false); frame.setVisible(true);
        });
        polishCapture("no-vault", frame);
        edt(() -> {
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
        waitFor(() -> app.state() == ShellState.LOCKED && frame.isShowing()); polishCapture("locked", frame);
        edt(() -> {
            if (!all(frame).stream().filter(c -> c instanceof JButton b && "Change Vault…".equals(b.getText())).findFirst().orElseThrow().getSize().equals(all(frame).stream().filter(c -> c instanceof JButton b && "Open".equals(b.getText())).findFirst().orElseThrow().getSize())) { throw new AssertionError("Unequal LOCKED pair"); }
        });
        edt(() -> app.open("review".toCharArray()));
        waitFor(() -> app.state() == ShellState.UNLOCKED && all(frame).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Show Code")));
        polishCapture("main-normal", frame);
        var beforeActions = session.state().tokens().stream().flatMap(t -> t.heads().stream()).map(TokenHead::revision).toList();
        JComponent ordinary = edt(() -> tokenRow("Example Service"));
        mouseReveal(ordinary, true, "mouse-show-selected");
        JComponent unselected = edt(() -> tokenRow("GitHub"));
        mouseReveal(unselected, false, "mouse-show-unselected"); polishCapture("mouse-copy", frame);
        JComponent keyboard = edt(() -> tokenRow("Long issuer"));
        JButton tabSource = edt(() -> all(unselected).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(b -> b.isVisible() && List.of("Copy", "Copied").contains(b.getText())).findFirst().orElseThrow());
        edt(() -> { tabSource.requestFocusInWindow(); }); waitFor(tabSource::isFocusOwner);
        key(KeyEvent.VK_TAB); selectionStayed(keyboard);
        key(KeyEvent.VK_SPACE);
        waitFor(() -> all(keyboard).stream().anyMatch(c -> c instanceof JButton b && "Copy".equals(b.getText())));
        selectionStayed(keyboard); key(KeyEvent.VK_ENTER); selectionStayed(keyboard);
        JTextField search = edt(() -> all(frame).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast)
                .filter(f -> "Search".equals(f.getAccessibleContext().getAccessibleName())).findFirst().orElseThrow());
        edt(() -> search.setText("Long issuer")); robot.waitForIdle(); selectionStayed(edt(() -> tokenRow("Long issuer")));
        edt(() -> search.setText("")); robot.waitForIdle(); selectionStayed(edt(() -> tokenRow("Long issuer")));
        if (!beforeActions.equals(session.state().tokens().stream().flatMap(t -> t.heads().stream()).map(TokenHead::revision).toList())) { throw new AssertionError("Code action published a mutation"); }
        edt(() -> tokenRow("Example Service").requestFocusInWindow()); robot.waitForIdle(); menu("Edit…");
        waitFor(() -> Arrays.stream(frame.getOwnedWindows()).anyMatch(w -> w instanceof JDialog && w.isVisible()));
        polishCapture("edit-ordinary", uncheckedDialog());
        text("Issuer / service", "Unsaved smoke issuer"); text("Account", "Unsaved smoke account");
        click("Delete TOTP…"); polishCapture("delete-from-edit", uncheckedDialog());
        click("Cancel"); waitFor(() -> "Edit TOTP".equals(uncheckedDialog().getTitle()));
        edt(() -> {
            JTextField draft = all(uncheckedDialog()).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast)
                    .filter(f -> "Issuer / service".equals(f.getAccessibleContext().getAccessibleName())).findFirst().orElseThrow();
            if (!"Unsaved smoke issuer".equals(draft.getText())) { throw new AssertionError("Delete cancellation lost Edit draft"); }
        });
        polishCapture("edit-restored", uncheckedDialog());
        click("Delete TOTP…"); key(KeyEvent.VK_ESCAPE); waitFor(() -> "Edit TOTP".equals(uncheckedDialog().getTitle()));
        click("Cancel"); closed(); polishCapture("list-after-edit-cancel", frame);
        if (!beforeActions.equals(session.state().tokens().stream().flatMap(t -> t.heads().stream()).map(TokenHead::revision).toList())) { throw new AssertionError("Edit/Delete cancellation published"); }
        conflict(session, true); session.requestRefresh();
        waitFor(() -> all(frame).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Resolve")));
        polishCapture("main-conflict", frame);
        JComponent row = edt(() -> tokenRow("Same Service"));
        Point point = edt(row::getLocationOnScreen); robot.mouseMove(point.x + 60, point.y + 20);
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK); robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        waitFor(() -> MenuSelectionManager.defaultManager().getSelectedPath().length > 0);
        if (edt(() -> all(frame).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Copy")))) { throw new AssertionError("Right-click revealed"); }
        polishCapture("row-context", frame); key(KeyEvent.VK_ESCAPE);
        mouseReveal(row, false, "conflict-mouse-show");
        JComponent sibling = edt(() -> all(frame).stream().filter(JComponent.class::isInstance).map(JComponent.class::cast)
                .filter(c -> c != row && "divider".equals(c.getClientProperty("totipo.rowPresentation"))
                        && c.getAccessibleContext().getAccessibleName().contains("Same Service")).findFirst().orElseThrow());
        mouseReveal(sibling, false, "conflict-sibling-mouse-show");
        for (String issuer : List.of("GitHub", "Example Service", "Long issuer")) {
            mouseReveal(edt(() -> tokenRow(issuer)), false, "countdown-reveal-" + issuer.split(" ")[0].toLowerCase());
        }
        synchronizedCountdown();
        polishCapture("synchronized-countdowns", frame);
        String initialCountdown = synchronizedCountdown();
        waitFor(() -> all(frame).stream().anyMatch(c -> c instanceof JLabel label && label.isShowing()
                && label.getText().matches("[0-9]+ sec") && !label.getText().equals(initialCountdown)));
        synchronizedCountdown();
        polishCapture("synchronized-countdowns-after-tick", frame);

        edt(() -> { row.requestFocusInWindow(); }); robot.waitForIdle();
        // Management clears the child's revealed value through the existing flow.
        edt(() -> { row.requestFocusInWindow(); }); robot.waitForIdle();
        robot.keyPress(KeyEvent.VK_SHIFT); key(KeyEvent.VK_F10); robot.keyRelease(KeyEvent.VK_SHIFT);
        waitFor(() -> MenuSelectionManager.defaultManager().getSelectedPath().length > 0); key(KeyEvent.VK_ESCAPE);
        menu("Delete…"); waitFor(() -> Arrays.stream(frame.getOwnedWindows()).anyMatch(w -> w instanceof JDialog && w.isVisible()));
        if (!edt(() -> "Delete TOTP?".equals(uncheckedDialog().getTitle()))) { throw new AssertionError("Delete did not use existing confirmation"); }
        polishCapture("delete-direct-menu", uncheckedDialog());
        int directDeleteHeight = edt(() -> uncheckedDialog().getHeight());
        key(KeyEvent.VK_ESCAPE); closed(); polishCapture("list-after-direct-escape", frame);
        // Invoke the row popup's Delete action directly as well.
        edt(() -> row.requestFocusInWindow()); robot.waitForIdle();
        Point deletePoint = edt(row::getLocationOnScreen); robot.mouseMove(deletePoint.x + 60, deletePoint.y + 20);
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK); robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        waitFor(() -> MenuSelectionManager.defaultManager().getSelectedPath().length > 0);
        edt(() -> {
            for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) {
                if (element instanceof JPopupMenu popup) {
                    for (Component component : popup.getComponents()) {
                        if (component instanceof JMenuItem item && "Delete…".equals(item.getText())) { item.doClick(0); return; }
                    }
                }
            }
            throw new AssertionError("Missing context Delete");
        });
        waitFor(() -> Arrays.stream(frame.getOwnedWindows()).anyMatch(w -> w instanceof JDialog && w.isVisible()));
        polishCapture("delete-direct-context", uncheckedDialog()); click("Cancel"); closed();
        polishCapture("list-after-direct-cancel", frame);
        edt(() -> row.requestFocusInWindow()); robot.waitForIdle(); menu("Edit…");
        waitFor(() -> Arrays.stream(frame.getOwnedWindows()).anyMatch(w -> w instanceof JDialog && w.isVisible()));
        polishCapture("edit-conflict-version", uncheckedDialog());
        click("Delete TOTP…"); polishCapture("delete-from-conflict-edit", uncheckedDialog());
        if (edt(() -> uncheckedDialog().getHeight()) != directDeleteHeight) { throw new AssertionError("Delete origin changed natural height for the same target"); }
        click("Cancel"); waitFor(() -> "Edit TOTP".equals(uncheckedDialog().getTitle()));
        click("Change setup…"); chooseMode("Manual entry"); polishCapture("change-setup-manual", uncheckedDialog());
        click("Cancel"); waitFor(() -> "Edit TOTP".equals(uncheckedDialog().getTitle()));
        click("Cancel"); closed();
        shortcut(KeyEvent.VK_N); waitFor(() -> Arrays.stream(frame.getOwnedWindows()).anyMatch(w -> w instanceof JDialog && w.isVisible()));
        JDialog task = dialog();
        // A bare Xvfb display has no window manager to raise the new owned task.
        edt(() -> { task.toFront(); task.requestFocus(); }); waitFor(task::isActive);
        Rectangle original = edt(() -> task.getBounds());
        acquisitionFits(task, original); polishCapture("add-uri", task);
        validateUri(task, original);
        chooseMode("Setup URI"); robot.waitForIdle();
        acquisitionFits(task, original);
        if (edt(() -> task.getHeight()) != original.height) { throw new AssertionError("Cleared URI validation retained old height"); }
        polishCapture("add-uri-cleared", task);
        validateUri(task, original);
        text("Setup URI", "otpauth://totp/Example:account?secret=MY&issuer=Example"); click("Review");
        if (!edt(() -> all(task).stream().anyMatch(c -> c instanceof JButton b && "Add".equals(b.getText())))) { throw new AssertionError("Corrected URI did not enter existing review"); }
        if (edt(() -> task.getX() != original.x || task.getWidth() != original.width)) { throw new AssertionError("Corrected URI review changed stable width"); }
        polishCapture("add-uri-corrected-review", task); click("Back"); robot.waitForIdle();
        acquisitionFits(task, original);
        if (edt(() -> task.getHeight()) != original.height) { throw new AssertionError("Back retained validation height"); }
        validateUri(task, original);
        for (String mode : List.of("Manual entry", "Setup URI", "Manual entry")) {
            chooseMode(mode); robot.waitForIdle();
            acquisitionFits(task, original);
            if (mode.equals("Setup URI") && edt(() -> task.getHeight()) != original.height) { throw new AssertionError("Mode switch retained validation height"); }
            polishCapture(mode.equals("Setup URI") ? "add-uri-return" : "add-manual", task);
        }
        key(KeyEvent.VK_ESCAPE); closed();
        menu("Change Vault Password…"); polishCapture("change-password", uncheckedDialog()); key(KeyEvent.VK_ESCAPE); closed();
        open(true); polishCapture("conflict-simple", uncheckedDialog()); click("Combine details…");
        JPasswordField custom = edt(() -> all(uncheckedDialog()).stream().filter(JPasswordField.class::isInstance).map(JPasswordField.class::cast).findFirst().orElseThrow());
        edt(() -> { custom.scrollRectToVisible(custom.getBounds()); custom.requestFocusInWindow(); });
        polishCapture("conflict-custom-setup", uncheckedDialog()); click("Cancel"); closed();
        edt(app::shutdown); waitFor(() -> !frame.isDisplayable());
        try (var paths = Files.walk(vault)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) { Files.delete(path); } }
        System.out.println("PASS polish: exact mouse/keyboard row selection, segmented acquisition, shared action geometry, context management, validation growth/clearing/correction, stable URI → Manual → URI, shared forms; " + capturePrefix);
        System.exit(0);
    }
}
