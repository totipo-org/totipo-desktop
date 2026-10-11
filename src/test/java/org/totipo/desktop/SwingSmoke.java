package org.totipo.desktop;

import org.totipo.*;
import org.totipo.storage.nio.NioTotipo;
import org.totipo.desktop.ui.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Disposable graphical regression driver. Run one group per JVM; see README.
 * Shared support and four explicit scenario methods; never used by headless JUnit.
 */
public final class SwingSmoke {
    private final List<Path> fixtures = new ArrayList<>();
    private final AtomicReference<Throwable> asynchronousFailure = new AtomicReference<>();
    private Robot robot;
    private ShellFrame frame;
    private DesktopApplication app;
    private VaultSession session;
    private Path vault;
    private TokenId distinct, identical;
    private String theme, capturePrefix, group;
    private boolean cleaning;

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--list")) {
            System.out.println("mutations conflicts lifecycle polish");
            return;
        }
        if (args.length < 1 || args.length > 3
                || !List.of("mutations", "conflicts", "lifecycle", "polish").contains(args[0])
                || args.length > 1 && !List.of("light", "dark", "native").contains(args[1])) {
            System.err.println("Usage: SwingSmoke <mutations|conflicts|lifecycle|polish> [light|dark|native] [font-size]");
            System.exit(2);
        }
        SwingSmoke smoke = new SwingSmoke();
        Throwable failure = null;
        try {
            smoke.setup(args[0], args.length > 1 ? args[1] : "dark", args.length > 2 ? args[2] : null);
            switch (smoke.group) {
                case "mutations" -> smoke.mutations();
                case "conflicts" -> smoke.conflicts();
                case "lifecycle" -> smoke.lifecycle();
                case "polish" -> smoke.polish();
                default -> throw new AssertionError(smoke.group);
            }
            smoke.checkAsynchronousFailure();
        } catch (Throwable error) {
            failure = error;
        } finally {
            try { smoke.cleanup(); }
            catch (Throwable error) {
                if (failure == null) { failure = error; } else { failure.addSuppressed(error); }
            }
        }
        if (failure == null) { failure = smoke.asynchronousFailure.get(); }
        if (failure != null) { failure.printStackTrace(); }
        System.exit(failure == null ? 0 : 1);
    }

    private void setup(String selectedGroup, String selectedTheme, String fontSize) throws Exception {
        group = selectedGroup;
        theme = selectedTheme;
        capturePrefix = theme + (fontSize == null ? "" : "-font" + fontSize)
                + (System.getProperty("sun.java2d.uiScale") == null ? "" : "-scale" + System.getProperty("sun.java2d.uiScale"));
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> asynchronousFailure.compareAndSet(null, error));
        if (GraphicsEnvironment.isHeadless()) { throw new IllegalStateException("SwingSmoke requires a graphical display"); }
        robot = new Robot();
        robot.setAutoDelay(switch (group) { case "mutations" -> 60; case "conflicts" -> 30; default -> 35; });
        edt(() -> {
            if (!theme.equals("native") || group.equals("polish")) {
                try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
                catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) { throw new AssertionError(e); }
            }
            if(theme.equals("dark")) {
                for(String key:List.of("Panel.background","List.background","TextField.background","PasswordField.background","TextArea.background","ToggleButton.background","RadioButton.background","Button.background","Viewport.background","ScrollPane.background", "MenuBar.background", "Menu.background", "MenuItem.background", "PopupMenu.background")) UIManager.put(key,new Color(38,42,47));
                for(String key:List.of("Label.foreground","List.foreground","TextField.foreground","PasswordField.foreground","TextArea.foreground","ToggleButton.foreground","RadioButton.foreground","Button.foreground", "Menu.foreground", "MenuItem.foreground"))UIManager.put(key,new Color(230,232,235));
                UIManager.put("List.selectionBackground",new Color(80,120,190));UIManager.put("List.selectionForeground",Color.WHITE);
            }
            ApplicationFonts.install();
            if (fontSize != null) {
                float size = Float.parseFloat(fontSize);
                var defaults = UIManager.getDefaults();
                for (Object key : new ArrayList<>(defaults.keySet())) {
                    if (defaults.get(key) instanceof Font font) { defaults.put(key, new javax.swing.plaf.FontUIResource(font.deriveFont(size))); }
                }
            }
        });
    }

    /** Called on EDT; application executor owns open/create/close, driver owns fixture saves. */
    private void showApplication(Path remembered, boolean unlock) {
        VaultAccess access = new VaultAccess() {
            public OpenResult open(Path path, char[] password) {
                var result = NioTotipo.open(path, password);
                if (result instanceof OpenResult.Opened opened) { session = opened.session(); }
                return result;
            }
            public CreateVaultResult create(Path path, char[] password) { return NioTotipo.create(path, password); }
        };
        app = new DesktopApplication(access, frame, path -> new VaultContent(frame),
                new org.totipo.desktop.clipboard.TotpClipboard(), new VaultPreferences() {
            public Optional<Path> lastVault() { return Optional.ofNullable(remembered); }
            public void setLastVault(Path path) { }
            public void clearLastVault() { }
        });
        app.show();
        if (unlock) { app.begin(vault, "review".toCharArray(), false); }
    }

    private Path fixture(String label) throws Exception {
        Path path = Files.createTempDirectory("totipo-smoke-" + label + "-");
        fixtures.add(path);
        return path;
    }

    private void checkAsynchronousFailure() {
        Throwable error = asynchronousFailure.get();
        if (error != null) { throw new AssertionError("Asynchronous graphical failure", error); }
    }

    private void cleanup() throws Exception {
        cleaning = true;
        // Shutdown retires dialogs/timers/watchers and asynchronously closes the session.
        // Even on failure, close before deleting only this driver's disposable fixtures.
        try {
            if (app != null) {
                edt(app::shutdown);
                waitFor(() -> !frame.isDisplayable());
            }
        } finally {
            try {
                if (session != null) { session.close(); }
            } finally {
                edt(() -> { for (Window window : Window.getWindows()) { window.dispose(); } });
                for (Path path : fixtures) {
                    try (var paths = Files.walk(path)) {
                        for (Path child : paths.sorted(Comparator.reverseOrder()).toList()) { Files.delete(child); }
                    }
                }
            }
        }
    }

    private void captureImage(String name, Window window) throws Exception {
        Path destination = Path.of("review/screenshots/swing-smoke", group);
        Files.createDirectories(destination);
        Rectangle bounds = edt(() -> new Rectangle(window.getLocationOnScreen(), window.getSize()));
        ImageIO.write(robot.createScreenCapture(bounds), "png", destination.resolve(capturePrefix + "-" + name + ".png").toFile());
        System.out.println("Captured " + group + "/" + capturePrefix + "-" + name + " " + bounds);
    }

    // Shared support. Assertion-bearing captures keep their scenario-specific checks.

    private <T> T edt(Supplier<T> action) throws Exception {
        AtomicReference<T> value = new AtomicReference<>(); AtomicReference<Throwable> error = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> { try { value.set(action.get()); } catch(Throwable e) { error.set(e); } });
        if(error.get()!=null) throw new AssertionError(error.get()); return value.get();
    }
    private void edt(Runnable action) throws Exception { edt(() -> { action.run(); return null; }); }
    private List<Component> all(Container root) {
        List<Component> result = new ArrayList<>(); for(Component c:root.getComponents()) { result.add(c); if(c instanceof Container r) result.addAll(all(r)); } return result;
    }
    private JDialog dialog() throws Exception {
        return edt(() -> Arrays.stream(frame.getOwnedWindows()).filter(w -> w instanceof JDialog && w.isVisible()).map(JDialog.class::cast).findFirst().orElse(null));
    }
    private JButton button(Container root,String name) throws Exception {
        return edt(() -> all(root).stream().filter(JButton.class::isInstance).map(JButton.class::cast).filter(b -> b.getText().equals(name)).findFirst().orElseThrow());
    }
    private void click(Component c) throws Exception {
        Point p=edt(c::getLocationOnScreen); Dimension s=edt(() -> c.getSize());
        robot.mouseMove(p.x+s.width/2,p.y+s.height/2); robot.mousePress(InputEvent.BUTTON1_DOWN_MASK); robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK); robot.waitForIdle(); Thread.sleep(180);
    }
    private void click(String name) throws Exception { click(button(dialog(),name)); }
    private void key(int code) { robot.keyPress(code);robot.keyRelease(code);robot.waitForIdle(); }
    private void shortcut(int code) { robot.keyPress(KeyEvent.VK_CONTROL);key(code);robot.keyRelease(KeyEvent.VK_CONTROL);robot.waitForIdle(); }
    private void text(String name,String value) throws Exception {
        edt(() -> all(uncheckedDialog()).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast)
                .filter(c -> name.equals(c.getAccessibleContext().getAccessibleName())).findFirst().orElseThrow().setText(value));
    }
    private JDialog uncheckedDialog() { return Arrays.stream(frame.getOwnedWindows()).filter(w -> w instanceof JDialog && w.isVisible()).map(JDialog.class::cast).findFirst().orElseThrow(); }
    private void captureMutation(String name) throws Exception {
        robot.waitForIdle(); Thread.sleep(250); Window window=dialog();if(window==null)window=frame;
        if (List.of("edit", "conflict-edit", "add-acquisition", "manual-add", "delete-confirmation").contains(name)) {
            Window task = window;
            edt(() -> {
                JScrollPane scroll = all(task).stream().filter(JScrollPane.class::isInstance).map(JScrollPane.class::cast).findFirst().orElseThrow();
                if (scroll.getVerticalScrollBar().isVisible()) { throw new AssertionError("Default task unexpectedly scrolls: " + name
                        + "; window=" + task.getSize() + "; content=" + uncheckedDialog().getContentPane().getSize()
                        + "; viewport=" + scroll.getViewport().getExtentSize() + "; view preferred=" + scroll.getViewport().getView().getPreferredSize()); }
                String control = switch (name) {
                    case "add-acquisition" -> "Setup URI";
                    case "manual-add" -> "Period (seconds)";
                    case "edit", "conflict-edit" -> "Change setup…";
                    default -> "Delete TOTP";
                };
                Component field = all(task).stream().filter(c -> c instanceof JTextField input
                        && control.equals(input.getAccessibleContext().getAccessibleName())
                        || c instanceof JButton button && control.equals(button.getText())).findFirst().orElseThrow();
                Rectangle visible = SwingUtilities.convertRectangle(field.getParent(), field.getBounds(), (Component) task);
                if (!new Rectangle(task.getSize()).contains(visible)) { throw new AssertionError("Control outside task: " + control); }
                if (SwingUtilities.isDescendingFrom(field, scroll)) {
                    Rectangle inView = SwingUtilities.convertRectangle(field.getParent(), field.getBounds(), scroll.getViewport());
                    if (!new Rectangle(scroll.getViewport().getSize()).contains(inView)) { throw new AssertionError("Control clipped by viewport: " + control); }
                }
            });
        }
        // A small local capture set; functional smoke still exercises every existing step.
        if (!List.of("edit", "conflict-edit", "add-acquisition", "manual-add", "delete-confirmation", "change-review", "manual-duplicate", "validation").contains(name)) { return; }
        captureImage(name, window);
    }
    private void waitFor(Supplier<Boolean> condition) throws Exception {
        long until=System.nanoTime()+10_000_000_000L;while(!edt(condition)) {if (!cleaning) { checkAsynchronousFailure(); } if(System.nanoTime()>until)throw new AssertionError("Timed out");Thread.sleep(50);}robot.waitForIdle();
    }
    private void closed() throws Exception { waitFor(() -> Arrays.stream(frame.getOwnedWindows()).noneMatch(w -> w instanceof JDialog && w.isVisible())); edt(() -> { frame.toFront(); frame.requestFocus(); }); Thread.sleep(150); }
    private JComponent tokenRow(String identity) {
        return all(frame).stream().filter(JComponent.class::isInstance).map(JComponent.class::cast)
                .filter(c -> "divider".equals(c.getClientProperty("totipo.rowPresentation"))
                        && c.getAccessibleContext().getAccessibleName().contains(identity)).findFirst().orElseThrow();
    }
    private void editIdentity(String identity) throws Exception {
        edt(() -> tokenRow(identity).dispatchEvent(new MouseEvent(tokenRow(identity), MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1)));
        menu("Edit…");
        waitFor(() -> Arrays.stream(frame.getOwnedWindows()).anyMatch(w -> w instanceof JDialog && w.isVisible()));
    }
    private void editGithub() throws Exception { editIdentity("GitHub"); }
    private void manualValues(String issuer, String account) throws Exception {
        click(edt(() -> all(uncheckedDialog()).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
                .filter(b -> b.getText().equals("Manual entry")).findFirst().orElseThrow()));
        if (!"Add".equals(edt(() -> uncheckedDialog().getRootPane().getDefaultButton().getText()))) throw new AssertionError("Manual default is not Add");
        text("Issuer / service", issuer); text("Account", account); text("New Base32 secret", "MY");
    }


    private VaultState awaitAlternatives(VaultSession vaultSession, TokenId id, int alternatives) throws Exception {
        long limit = System.nanoTime() + 10_000_000_000L; vaultSession.requestRefresh();
        while (true) {
            VaultState state = vaultSession.state();
            if (state.token(id).filter(t -> t.alternatives().size() == alternatives).isPresent()) { return state; }
            if (System.nanoTime() > limit) { throw new AssertionError("Fixture not established"); } Thread.sleep(20);
        }
    }
    private TokenId conflict(VaultSession initial, boolean same) throws Exception {
        SaveResult.Saved created;
        try (CreateToken builder = initial.state().createToken(); NewSecret secret = NewSecret.copyOf(new byte[]{102})) {
            created = (SaveResult.Saved) builder.issuer(same ? "Same Service" : "Base").account(same ? "same account" : "base").secret(secret).save();
        }
        VaultState basis = awaitAlternatives(initial, created.tokenId(), 1); TokenAlternative original = basis.token(created.tokenId()).orElseThrow().alternatives().getFirst();
        try (UpdateToken builder = basis.update(original)) { builder.issuer(same ? "Same Service" : "Slie").account(same ? "same account" : "a").metadata(new ClientMetadata(java.util.Optional.of("fixture one"), java.util.Optional.empty())).save(); }
        try (UpdateToken builder = basis.update(original); NewSecret secret = NewSecret.copyOf(new byte[]{103})) {
            builder.issuer(same ? "Same Service" : "Sile").account(same ? "same account" : "x").secret(secret)
                    .status(same ? TokenStatus.ACTIVE : TokenStatus.TOMBSTONED).algorithm(same ? TotpAlgorithm.SHA1 : TotpAlgorithm.SHA256).digits(same ? 6 : 8).save();
        }
        awaitAlternatives(initial, created.tokenId(), 2); return created.tokenId();
    }
    private Container conflictGroup(Component component) {
        Container parent = component.getParent();
        while (parent != null) {
            if (parent instanceof JPanel panel && "This token has conflicting versions".equals(panel.getAccessibleContext().getAccessibleName())) { return parent; }
            parent = parent.getParent();
        }
        throw new AssertionError("Resolve has no semantic conflict group");
    }
    private void openConflict(boolean same) throws Exception {
        JButton resolve = edt(() -> all(frame).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(b -> b.getText().equals("Resolve") && all(conflictGroup(b)).stream().anyMatch(c -> c instanceof JComponent row
                        && "divider".equals(row.getClientProperty("totipo.rowPresentation")) && row.getAccessibleContext().getAccessibleName().contains(same ? "Same Service" : "Slie")))
                .findFirst().orElseThrow());
        click(resolve); waitFor(() -> Arrays.stream(frame.getOwnedWindows()).anyMatch(w -> w instanceof JDialog && w.isVisible()));
    }
    private AbstractButton option(String name) throws Exception {
        return edt(() -> all(uncheckedDialog()).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast).filter(b -> b.getText().equals(name)).findFirst().orElseThrow());
    }
    private void captureConflict(String name) throws Exception {
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
        captureImage(name, task);
    }
    private void chooseText(String accessible) throws Exception {
        Component field = edt(() -> all(uncheckedDialog()).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast)
                .filter(f -> accessible.equals(f.getAccessibleContext().getAccessibleName())).findFirst().orElseThrow());
        edt(() -> ((JComponent) field).scrollRectToVisible(field.getBounds())); click(field);
    }


    private void captureLifecycle(String name) throws Exception {
        robot.waitForIdle(); Thread.sleep(150);
        Window task = edt(() -> Arrays.stream(Window.getWindows()).filter(w -> w.isVisible() && w instanceof JDialog)
                .reduce((a, b) -> b).orElse(frame));
        edt(() -> {
            for (Component c : all(task)) {
                if (c.isShowing() && c instanceof JButton b && List.of("Cancel", "Close", "Try Again", "Open").contains(b.getText())) {
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
        captureImage(name, task);
    }
    private void menu(String name) throws Exception {
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


    private void selectionStayed(JComponent target) throws Exception {
        edt(() -> {
            var selected = all(frame).stream().filter(JComponent.class::isInstance).map(JComponent.class::cast)
                    .filter(c -> "divider".equals(c.getClientProperty("totipo.rowPresentation"))
                            && c.getAccessibleContext().getAccessibleStateSet().contains(javax.accessibility.AccessibleState.SELECTED)).toList();
            if (!selected.equals(List.of(target))) { throw new AssertionError("Row action moved semantic selection: " + selected); }
        });
    }
    private void mouseReveal(JComponent target, boolean selected, String capture) throws Exception {
        if (selected) { edt(() -> { target.requestFocusInWindow(); }); robot.waitForIdle(); selectionStayed(target); }
        long revealed = edt(() -> all(frame).stream().filter(c -> c instanceof JButton b && b.isVisible()
                && List.of("Copy", "Copied").contains(b.getText())).count());
        JButton show = button(target, "Show Code");
        click(show); waitFor(() -> all(target).stream().anyMatch(c -> c instanceof JButton b && "Copy".equals(b.getText())));
        robot.waitForIdle(); selectionStayed(target); capturePolish(capture, frame);
        long after = edt(() -> all(frame).stream().filter(c -> c instanceof JButton b && b.isVisible()
                && List.of("Copy", "Copied").contains(b.getText())).count());
        if (after > revealed + 1) { throw new AssertionError("Action revealed another semantic row"); }
        click(button(target, "Copy")); selectionStayed(target);
    }
    private void capturePolish(String name, Window window) throws Exception {
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
                    Rectangle r = SwingUtilities.convertRectangle(c.getParent(), c.getBounds(), window);
                    if (!new Rectangle(window.getSize()).contains(r)) { throw new AssertionError("Footer clipped: " + b.getText()); }
                }
            }
        });
        captureImage(name, window);
    }
    private void chooseMode(String name) throws Exception {
        edt(() -> all(uncheckedDialog()).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
                .filter(b -> name.equals(b.getText())).findFirst().orElseThrow().doClick(0));
    }
    private void acquisitionFits(JDialog task, Rectangle original) throws Exception {
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
            if (!(all(task).stream().anyMatch(c -> c instanceof AbstractButton b && b.getText().equals("Manual entry") && b.isSelected()) && task.getTitle().equals("Add TOTP") ? "Add" : "Review").equals(task.getRootPane().getDefaultButton().getText())) { throw new AssertionError("Acquisition semantics changed"); }
        });
    }
    private void validateUri(JDialog task, Rectangle original) throws Exception {
        int height = edt(() -> task.getHeight());
        text("Setup URI", "**"); click("Review"); robot.waitForIdle();
        waitFor(() -> all(task).stream().anyMatch(c -> c instanceof JTextArea area && area.isVisible()
                && "Setup URI validation".equals(area.getAccessibleContext().getAccessibleName()) && !area.getText().isBlank()));
        if (edt(() -> task.getHeight()) <= height) { throw new AssertionError("URI validation did not grow task height"); }
        acquisitionFits(task, original);
        if (!edt(() -> KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JPasswordField field
                && "Setup URI".equals(field.getAccessibleContext().getAccessibleName()))) { throw new AssertionError("Invalid URI field not focused"); }
        capturePolish("add-uri-invalid", task);
    }
    private String synchronizedCountdown() throws Exception {
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


    // Mutation forms, publication, cancellation and retirement.
    private void mutations() throws Exception {
        vault = fixture("mutations");
        try(var initial=((CreateVaultResult.Created)NioTotipo.create(vault,"review".toCharArray())).session()) {
            try(var builder=initial.state().createToken();var secret=NewSecret.copyOf(new byte[]{102})) {
                builder.issuer("GitHub").account("niki@example.com").secret(secret).save();
            }
        }
        edt(() -> {
            frame=new ShellFrame(); frame.setLocation(0,0);
            showApplication(null, true);
        });
        waitFor(() -> app.state()==ShellState.UNLOCKED && all(frame).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Show Code")));
        TokenId github=session.state().tokens().get(0).id();
        shortcut(KeyEvent.VK_N);captureMutation("add-acquisition");
        Component method=edt(() -> all(uncheckedDialog()).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast).filter(b -> b.getText().equals("Manual entry")).findFirst().orElseThrow());click(method);
        captureMutation("manual-add");
        click("Add");captureMutation("validation");
        Component focused=edt(() -> KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
        if(!(focused instanceof JPasswordField))throw new AssertionError("Invalid secret did not receive focus");
        text("Issuer / service","Example Service");text("Account","review@example.com");text("New Base32 secret","MY");
        // Enter validates and directly adds Manual Entry.
        edt(() -> all(uncheckedDialog()).stream().filter(JPasswordField.class::isInstance).findFirst().orElseThrow().requestFocusInWindow());key(KeyEvent.VK_ENTER);
        closed();
        waitFor(() -> session.state().tokens().size()==2);
        // Validated Manual duplicate handling owns the setup; Cancel discards it.
        shortcut(KeyEvent.VK_N); Thread.sleep(250); manualValues("GitHub", "niki@example.com"); click("Add");
        if (!dialog().getTitle().equals("TOTP already exists")) throw new AssertionError("Manual entered ordinary Review");
        captureMutation("manual-duplicate"); click("Cancel"); closed();
        if (session.state().tokens().size()!=2) throw new AssertionError("Duplicate Cancel published");
        shortcut(KeyEvent.VK_N); Thread.sleep(250); manualValues("Example Service", "review@example.com"); click("Add");
        click("Add Another"); closed(); waitFor(() -> session.state().tokens().size()==3);
        shortcut(KeyEvent.VK_N); Thread.sleep(250); manualValues("GitHub", "niki@example.com"); click("Add");
        click("Update Existing…");
        if (!button(dialog(), "Save setup").isShowing()) throw new AssertionError("Missing replacement review");
        click("Save setup"); closed();
        shortcut(KeyEvent.VK_N); Thread.sleep(250); text("Setup URI","otpauth://totp/GitHub:niki%40example.com?secret=MY&issuer=GitHub&algorithm=SHA512&digits=8&period=60");click("Review");captureMutation("uri-review");click("Add");captureMutation("duplicate-choice");
        if(edt(() -> uncheckedDialog().getRootPane().getDefaultButton().getText()).equals("Update Existing…"))throw new AssertionError("Unsafe duplicate default");
        click("Update Existing…");captureMutation("update-existing-review");click("Save setup");closed();
        waitFor(() -> session.state().token(github).orElseThrow().alternatives().get(0).descriptor().algorithm()==TotpAlgorithm.SHA512);
        editGithub();captureMutation("edit");
        // Normal keyboard traversal visits real labeled fields/actions, no reveal.
        key(KeyEvent.VK_TAB); key(KeyEvent.VK_TAB); key(KeyEvent.VK_TAB);
        click("Change setup…");captureMutation("change-acquisition");text("Setup URI","otpauth://totp/Other:ignored?secret=MY&algorithm=SHA256&digits=7&period=45");click("Review");captureMutation("change-review");click("Save setup");closed();
        waitFor(() -> session.state().token(github).orElseThrow().alternatives().get(0).descriptor().algorithm()==TotpAlgorithm.SHA256);
        editGithub();text("Account","renamed@example.com");click("Save");closed();
        waitFor(() -> session.state().token(github).orElseThrow().alternatives().get(0).descriptor().account().equals("renamed@example.com"));
        if(!session.state().token(github).orElseThrow().alternatives().get(0).descriptor().issuer().equals("GitHub"))throw new AssertionError("URI replaced identity");
        editGithub();click("Delete TOTP…");captureMutation("delete-confirmation");key(KeyEvent.VK_ESCAPE);
        if(!dialog().getTitle().equals("Edit TOTP"))throw new AssertionError("Escape did not cancel deletion");
        click("Delete TOTP…");click("Cancel");
        if(!dialog().getTitle().equals("Edit TOTP"))throw new AssertionError("Cancel did not return to Edit");
        click("Delete TOTP…");click("Delete TOTP");closed();
        waitFor(() -> session.state().token(github).orElseThrow().alternatives().get(0).descriptor().status()==TokenStatus.TOMBSTONED);
        captureMutation("after-delete");
        shortcut(KeyEvent.VK_N); Thread.sleep(250); text("Setup URI","otpauth://totp/Abandoned:review?secret=MY");click("Review");shortcut(KeyEvent.VK_L);
        waitFor(() -> app.state()==ShellState.LOCKED);closed();captureMutation("locked-after-review");
        edt(() -> app.begin(vault,"review".toCharArray(),false));waitFor(() -> app.state()==ShellState.UNLOCKED);closed();
        if(session.state().tokens().size()!=3)throw new AssertionError("Abandoned draft published");
        // Two deliberate fixture branches exercise the existing conflict-version Edit notice.
        // No conflict resolution is performed by this probe.
        VaultState base = session.state();
        TokenState example = base.tokens().stream().filter(t -> !t.id().equals(github)).findFirst().orElseThrow();
        TokenAlternative alternative = example.alternatives().get(0);
        for (String account : List.of("review@example.com", "other@example.com")) {
            try (var builder = base.update(alternative)) {
                if (!(builder.account(account).save() instanceof SaveResult.Saved)) { throw new AssertionError("Conflict fixture publication failed"); }
            }
        }
        waitFor(() -> session.state().token(example.id()).orElseThrow().hasConflict());
        waitFor(() -> all(frame).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Resolve")));
        editIdentity("Example Service");
        captureMutation("conflict-edit"); key(KeyEvent.VK_ESCAPE); closed();
        edt(app::shutdown);waitFor(() -> !frame.isDisplayable());
        System.out.println("PASS: real Swing/NIO Add, Edit, setup replacement, duplicate update, Delete, Enter/Escape/Tab/Ctrl+L, reopen; "+theme);
    }

    // Whole-version and composed resolution, updated conflict and lock.
    private void conflicts() throws Exception {
        vault = fixture("conflicts");
        try (VaultSession initial = ((CreateVaultResult.Created) NioTotipo.create(vault, "review".toCharArray())).session()) {
            distinct = conflict(initial, false); identical = conflict(initial, true);
        }
        edt(() -> {
            frame = new ShellFrame(); frame.setLocation(0, 0);
            showApplication(null, true);
        });
        waitFor(() -> app.state() == ShellState.UNLOCKED && all(frame).stream().filter(JButton.class::isInstance).map(JButton.class::cast).filter(b -> b.getText().equals("Resolve")).count() == 2);
        openConflict(false); captureConflict("simple");
        if (edt(() -> all(uncheckedDialog()).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).anyMatch(AbstractButton::isSelected))) { throw new AssertionError("Preselected version"); }
        click("Resolve"); captureConflict("simple-validation");
        if (!(edt(() -> KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()) instanceof JRadioButton)) { throw new AssertionError("Validation did not focus version"); }
        click(option("Sile · x")); click("Combine details…"); captureConflict("details");
        if (edt(() -> all(uncheckedDialog()).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).anyMatch(AbstractButton::isSelected))) { throw new AssertionError("Preference leaked into composition"); }
        click("Save Resolution"); captureConflict("details-validation");
        chooseText("Issuer choice: Slie"); chooseText("Account choice: a"); click(option("Active"));
        click(option("Setup used by Slie · a"));
        JPasswordField secret = edt(() -> all(uncheckedDialog()).stream().filter(JPasswordField.class::isInstance).map(JPasswordField.class::cast).findFirst().orElseThrow());
        edt(() -> { secret.scrollRectToVisible(secret.getBounds()); secret.requestFocusInWindow(); }); text("New Base32 secret", "MY");
        captureConflict("custom-setup");
        click(option("Setup used by Slie · a")); if (edt(() -> secret.getPassword().length) != 0) { throw new AssertionError("Abandoned secret retained"); }
        click("Back"); if (edt(() -> all(uncheckedDialog()).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).anyMatch(AbstractButton::isSelected))) { throw new AssertionError("Back chose whole version"); }
        key(KeyEvent.VK_ESCAPE); closed();
        openConflict(true); captureConflict("indistinguishable"); click("Cancel"); closed();
        openConflict(false); click("Combine details…"); text("New Base32 secret", "MY");
        JPasswordField staleSecret = edt(() -> all(uncheckedDialog()).stream().filter(JPasswordField.class::isInstance).map(JPasswordField.class::cast).findFirst().orElseThrow());
        try (VaultSession remote = ((OpenResult.Opened) NioTotipo.open(vault, "review".toCharArray())).session()) {
            VaultState state = awaitAlternatives(remote, distinct, 2); TokenAlternative original = state.token(distinct).orElseThrow().alternatives().getFirst();
            try (UpdateToken builder = state.update(original)) { builder.issuer("Changed elsewhere").save(); }
            session.requestRefresh(); waitFor(() -> all(uncheckedDialog()).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Review Updated Conflict")));
            if (edt(() -> staleSecret.getPassword().length) != 0) { throw new AssertionError("Stale draft secret retained"); }
            captureConflict("updated-conflict"); click("Review Updated Conflict");
        }
        click("Cancel"); closed();
        // Publish the deliberately composed custom setup through the actual controller/library.
        JButton distinctResolve = edt(() -> all(frame).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(b -> b.getText().equals("Resolve") && all(conflictGroup(b)).stream().anyMatch(c -> c instanceof JComponent row
                        && "divider".equals(row.getClientProperty("totipo.rowPresentation")) && !row.getAccessibleContext().getAccessibleName().contains("Same Service"))).findFirst().orElseThrow());
        click(distinctResolve); click("Combine details…");
        for (String field : List.of("Issuer choice:", "Account choice:")) {
            JTextField chosen = edt(() -> all(uncheckedDialog()).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast)
                    .filter(f -> f.getAccessibleContext().getAccessibleName() != null && f.getAccessibleContext().getAccessibleName().startsWith(field)).findFirst().orElseThrow());
            click(chosen);
        }
        click(option("Active")); text("New Base32 secret", "MY");
        click("Save Resolution"); closed(); awaitAlternatives(session, distinct, 1);
        if (edt(() -> all(frame).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Copy")))) { throw new AssertionError("Resolution automatically revealed a code"); }
        openConflict(true); click("Combine details…"); text("New Base32 secret", "MY");
        JPasswordField retiring = edt(() -> all(uncheckedDialog()).stream().filter(JPasswordField.class::isInstance).map(JPasswordField.class::cast).findFirst().orElseThrow());
        shortcut(KeyEvent.VK_L); waitFor(() -> app.state() == ShellState.LOCKED); closed();
        if (edt(() -> retiring.getPassword().length) != 0) { throw new AssertionError("Lock retained secret"); }
        edt(() -> app.begin(vault, "review".toCharArray(), false)); waitFor(() -> app.state() == ShellState.UNLOCKED); closed();
        openConflict(true); click(option("Version 1 · Same Service · same account")); click("Resolve"); closed();
        awaitAlternatives(session, identical, 1);
        edt(app::shutdown); waitFor(() -> !frame.isDisplayable());
        System.out.println("PASS " + capturePrefix + " conflicts Swing/Robot whole/detailed/validation/stale/lock/keep smoke");
    }

    // Password, blocking-state and keyboard surfaces.
    private void lifecycle() throws Exception {
        vault = fixture("lifecycle");
        try (VaultSession initial = ((CreateVaultResult.Created) NioTotipo.create(vault, "review".toCharArray())).session()) { distinct = conflict(initial, false); }
        edt(() -> {
            frame = new ShellFrame(); frame.setLocation(0, 0);
            System.out.println("Environment " + UIManager.getLookAndFeel().getClass().getName()
                    + "; font=" + UIManager.getFont("Label.font").getSize() + "; transform=" + frame.getGraphicsConfiguration().getDefaultTransform());
            showApplication(vault, false);
        });
        waitFor(() -> app.state() == ShellState.LOCKED && frame.isShowing()); captureLifecycle("locked");
        waitFor(() -> KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JPasswordField);
        edt(() -> all(frame).stream().filter(JPasswordField.class::isInstance).map(JPasswordField.class::cast).findFirst().orElseThrow().setText("wrong"));
        key(KeyEvent.VK_ENTER); waitFor(() -> all(frame).stream().anyMatch(c -> c instanceof JTextArea a && a.getText().contains("that password")));
        if (edt(app::state) != ShellState.LOCKED) { throw new AssertionError("Wrong password blocked vault"); }
        edt(() -> app.open("review".toCharArray())); waitFor(() -> app.state() == ShellState.UNLOCKED && all(frame).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Resolve")));
        shortcut(KeyEvent.VK_F); key(KeyEvent.VK_DOWN); captureLifecycle("main-focus-selection");
        if (edt(() -> all(frame).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Copy")))) { throw new AssertionError("Focus revealed code"); }
        click(edt(() -> frame.getJMenuBar().getMenu(1))); captureLifecycle("vault-menu"); key(KeyEvent.VK_ESCAPE);
        shortcut(KeyEvent.VK_F); key(KeyEvent.VK_DOWN); key(KeyEvent.VK_DOWN);
        key(KeyEvent.VK_SPACE);
        waitFor(() -> all(frame).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Copy")));
        key(KeyEvent.VK_ENTER); shortcut(KeyEvent.VK_R);
        menu("About This Vault…"); captureLifecycle("about"); key(KeyEvent.VK_ESCAPE); closed();
        openConflict(false); captureLifecycle("simple-resolver");
        if (edt(() -> all(uncheckedDialog()).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).anyMatch(AbstractButton::isSelected))) { throw new AssertionError("Focus preselected version"); }
        key(KeyEvent.VK_DOWN);
        if (edt(() -> all(uncheckedDialog()).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).filter(AbstractButton::isSelected).count()) != 1) { throw new AssertionError("Radio arrow navigation unavailable"); }
        click(option("Slie · a")); captureLifecycle("simple-resolver-selected"); click("Combine details…"); captureLifecycle("detailed-resolver"); key(KeyEvent.VK_ESCAPE); closed();
        edt(app::shutdown); waitFor(() -> !frame.isDisplayable());
        Path blocked = fixture("lifecycle"); Files.writeString(blocked.resolve("vault"), "TOTIPO-VLT\u007funsupported");
        edt(() -> {
            frame = new ShellFrame(); frame.setLocation(0, 0);
            showApplication(blocked, false);
        });
        waitFor(() -> app.state() == ShellState.LOCKED && frame.isShowing());
        Files.setPosixFilePermissions(blocked.resolve("vault"), java.util.Set.of());
        edt(() -> app.open("fixture".toCharArray())); waitFor(() -> app.state() == ShellState.BLOCKING_VAULT_STATE); captureLifecycle("blocking-unavailable");
        Files.setPosixFilePermissions(blocked.resolve("vault"), java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        click(button(frame, "Try Again")); waitFor(() -> app.state() == ShellState.LOCKED);
        edt(() -> app.open("fixture".toCharArray())); waitFor(() -> app.state() == ShellState.BLOCKING_VAULT_STATE); captureLifecycle("blocking-invalid-or-unsupported");
        click(button(frame, "Details…")); captureLifecycle("blocking-details"); key(KeyEvent.VK_ESCAPE); closed();
        click(button(frame, "Try Again")); waitFor(() -> app.state() == ShellState.LOCKED);
        edt(app::shutdown); waitFor(() -> !frame.isDisplayable());
        edt(() -> { frame = new ShellFrame(); showApplication(null, false); });
        captureLifecycle("no-vault"); edt(app::shutdown); waitFor(() -> !frame.isDisplayable());
        System.out.println("PASS " + capturePrefix + " lifecycle Swing/Robot");
    }

    // Exact click targeting, selection, validation sizing and countdown repaint.
    private void polish() throws Exception {
        vault = fixture("polish");
        try (VaultSession initial = ((CreateVaultResult.Created) NioTotipo.create(vault, "review".toCharArray())).session()) {
            for (String issuer : List.of("GitHub", "Example Service", "Long issuer identity that uses the available row width")) {
                try (CreateToken builder = initial.state().createToken(); NewSecret secret = NewSecret.copyOf(new byte[]{102})) {
                    SaveResult.Saved saved = (SaveResult.Saved) builder.issuer(issuer).account("review@example.com").secret(secret).save();
                    awaitAlternatives(initial, saved.tokenId(), 1);
                }
            }
        }
        edt(() -> {
            frame = new ShellFrame(); frame.setLocation(0, 0);
            frame.renderShell(ShellState.NO_VAULT, null, "", false); frame.setVisible(true);
        });
        capturePolish("no-vault", frame);
        edt(() -> {
            showApplication(vault, false);
        });
        waitFor(() -> app.state() == ShellState.LOCKED && frame.isShowing()); capturePolish("locked", frame);
        edt(() -> {
            if (!all(frame).stream().filter(c -> c instanceof JButton b && "Change Vault…".equals(b.getText())).findFirst().orElseThrow().getSize().equals(all(frame).stream().filter(c -> c instanceof JButton b && "Open".equals(b.getText())).findFirst().orElseThrow().getSize())) { throw new AssertionError("Unequal LOCKED pair"); }
        });
        edt(() -> app.open("review".toCharArray()));
        waitFor(() -> app.state() == ShellState.UNLOCKED && all(frame).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Show Code")));
        capturePolish("main-normal", frame);
        var beforeActions = session.state().tokens().stream().flatMap(t -> t.heads().stream()).map(TokenHead::revision).toList();
        JComponent ordinary = edt(() -> tokenRow("Example Service"));
        mouseReveal(ordinary, true, "mouse-show-selected");
        JComponent unselected = edt(() -> tokenRow("GitHub"));
        mouseReveal(unselected, false, "mouse-show-unselected"); capturePolish("mouse-copy", frame);
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
        capturePolish("edit-ordinary", uncheckedDialog());
        text("Issuer / service", "Unsaved smoke issuer"); text("Account", "Unsaved smoke account");
        click("Delete TOTP…"); capturePolish("delete-from-edit", uncheckedDialog());
        click("Cancel"); waitFor(() -> "Edit TOTP".equals(uncheckedDialog().getTitle()));
        edt(() -> {
            JTextField draft = all(uncheckedDialog()).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast)
                    .filter(f -> "Issuer / service".equals(f.getAccessibleContext().getAccessibleName())).findFirst().orElseThrow();
            if (!"Unsaved smoke issuer".equals(draft.getText())) { throw new AssertionError("Delete cancellation lost Edit draft"); }
        });
        capturePolish("edit-restored", uncheckedDialog());
        click("Delete TOTP…"); key(KeyEvent.VK_ESCAPE); waitFor(() -> "Edit TOTP".equals(uncheckedDialog().getTitle()));
        click("Cancel"); closed(); capturePolish("list-after-edit-cancel", frame);
        if (!beforeActions.equals(session.state().tokens().stream().flatMap(t -> t.heads().stream()).map(TokenHead::revision).toList())) { throw new AssertionError("Edit/Delete cancellation published"); }
        conflict(session, true); session.requestRefresh();
        waitFor(() -> all(frame).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Resolve")));
        capturePolish("main-conflict", frame);
        JComponent row = edt(() -> tokenRow("Same Service"));
        Point point = edt(row::getLocationOnScreen); robot.mouseMove(point.x + 60, point.y + 20);
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK); robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        waitFor(() -> MenuSelectionManager.defaultManager().getSelectedPath().length > 0);
        if (edt(() -> all(frame).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Copy")))) { throw new AssertionError("Right-click revealed"); }
        capturePolish("row-context", frame); key(KeyEvent.VK_ESCAPE);
        mouseReveal(row, false, "conflict-mouse-show");
        JComponent sibling = edt(() -> all(frame).stream().filter(JComponent.class::isInstance).map(JComponent.class::cast)
                .filter(c -> c != row && "divider".equals(c.getClientProperty("totipo.rowPresentation"))
                        && c.getAccessibleContext().getAccessibleName().contains("Same Service")).findFirst().orElseThrow());
        mouseReveal(sibling, false, "conflict-sibling-mouse-show");
        for (String issuer : List.of("GitHub", "Example Service", "Long issuer")) {
            mouseReveal(edt(() -> tokenRow(issuer)), false, "countdown-reveal-" + issuer.split(" ")[0].toLowerCase());
        }
        synchronizedCountdown();
        capturePolish("synchronized-countdowns", frame);
        String initialCountdown = synchronizedCountdown();
        waitFor(() -> all(frame).stream().anyMatch(c -> c instanceof JLabel label && label.isShowing()
                && label.getText().matches("[0-9]+ sec") && !label.getText().equals(initialCountdown)));
        synchronizedCountdown();
        capturePolish("synchronized-countdowns-after-tick", frame);

        edt(() -> { row.requestFocusInWindow(); }); robot.waitForIdle();
        // Management clears the child's revealed value through the existing flow.
        edt(() -> { row.requestFocusInWindow(); }); robot.waitForIdle();
        robot.keyPress(KeyEvent.VK_SHIFT); key(KeyEvent.VK_F10); robot.keyRelease(KeyEvent.VK_SHIFT);
        waitFor(() -> MenuSelectionManager.defaultManager().getSelectedPath().length > 0); key(KeyEvent.VK_ESCAPE);
        menu("Delete…"); waitFor(() -> Arrays.stream(frame.getOwnedWindows()).anyMatch(w -> w instanceof JDialog && w.isVisible()));
        if (!edt(() -> "Delete TOTP?".equals(uncheckedDialog().getTitle()))) { throw new AssertionError("Delete did not use existing confirmation"); }
        capturePolish("delete-direct-menu", uncheckedDialog());
        int directDeleteHeight = edt(() -> uncheckedDialog().getHeight());
        key(KeyEvent.VK_ESCAPE); closed(); capturePolish("list-after-direct-escape", frame);
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
        capturePolish("delete-direct-context", uncheckedDialog()); click("Cancel"); closed();
        capturePolish("list-after-direct-cancel", frame);
        edt(() -> row.requestFocusInWindow()); robot.waitForIdle(); menu("Edit…");
        waitFor(() -> Arrays.stream(frame.getOwnedWindows()).anyMatch(w -> w instanceof JDialog && w.isVisible()));
        capturePolish("edit-conflict-version", uncheckedDialog());
        click("Delete TOTP…"); capturePolish("delete-from-conflict-edit", uncheckedDialog());
        if (edt(() -> uncheckedDialog().getHeight()) != directDeleteHeight) { throw new AssertionError("Delete origin changed natural height for the same target"); }
        click("Cancel"); waitFor(() -> "Edit TOTP".equals(uncheckedDialog().getTitle()));
        click("Change setup…"); chooseMode("Manual entry"); capturePolish("change-setup-manual", uncheckedDialog());
        click("Cancel"); waitFor(() -> "Edit TOTP".equals(uncheckedDialog().getTitle()));
        click("Cancel"); closed();
        shortcut(KeyEvent.VK_N); waitFor(() -> Arrays.stream(frame.getOwnedWindows()).anyMatch(w -> w instanceof JDialog && w.isVisible()));
        JDialog task = dialog();
        // A bare Xvfb display has no window manager to raise the new owned task.
        edt(() -> { task.toFront(); task.requestFocus(); }); waitFor(task::isActive);
        Rectangle original = edt(() -> task.getBounds());
        acquisitionFits(task, original); capturePolish("add-uri", task);
        validateUri(task, original);
        chooseMode("Setup URI"); robot.waitForIdle();
        acquisitionFits(task, original);
        if (edt(() -> task.getHeight()) != original.height) { throw new AssertionError("Cleared URI validation retained old height"); }
        capturePolish("add-uri-cleared", task);
        validateUri(task, original);
        text("Setup URI", "otpauth://totp/Example:account?secret=MY&issuer=Example"); click("Review");
        if (!edt(() -> all(task).stream().anyMatch(c -> c instanceof JButton b && "Add".equals(b.getText())))) { throw new AssertionError("Corrected URI did not enter existing review"); }
        if (edt(() -> task.getX() != original.x || task.getWidth() != original.width)) { throw new AssertionError("Corrected URI review changed stable width"); }
        capturePolish("add-uri-corrected-review", task); click("Back"); robot.waitForIdle();
        acquisitionFits(task, original);
        if (edt(() -> task.getHeight()) != original.height) { throw new AssertionError("Back retained validation height"); }
        validateUri(task, original);
        for (String mode : List.of("Manual entry", "Setup URI", "Manual entry")) {
            chooseMode(mode); robot.waitForIdle();
            acquisitionFits(task, original);
            if (mode.equals("Setup URI") && edt(() -> task.getHeight()) != original.height) { throw new AssertionError("Mode switch retained validation height"); }
            capturePolish(mode.equals("Setup URI") ? "add-uri-return" : "add-manual", task);
        }
        key(KeyEvent.VK_ESCAPE); closed();
        openConflict(true); capturePolish("conflict-simple", uncheckedDialog()); click("Combine details…");
        JPasswordField custom = edt(() -> all(uncheckedDialog()).stream().filter(JPasswordField.class::isInstance).map(JPasswordField.class::cast).findFirst().orElseThrow());
        edt(() -> { custom.scrollRectToVisible(custom.getBounds()); custom.requestFocusInWindow(); });
        capturePolish("conflict-custom-setup", uncheckedDialog()); click("Cancel"); closed();
        edt(app::shutdown); waitFor(() -> !frame.isDisplayable());
        System.out.println("PASS polish: exact mouse/keyboard row selection, segmented acquisition, shared action geometry, context management, validation growth/clearing/correction, stable URI → Manual → URI, shared forms; " + capturePrefix);
    }

}
