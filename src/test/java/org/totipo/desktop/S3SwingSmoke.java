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

/** Disposable, real Swing/NIO review harness; no production credentials. */
public class S3SwingSmoke {
    static Robot robot;
    static ShellFrame frame;
    static DesktopApplication app;
    static VaultSession session;
    static Path vault;
    static String theme;
    static String capturePrefix;
    static <T> T edt(Supplier<T> action) throws Exception {
        AtomicReference<T> value = new AtomicReference<>(); AtomicReference<Throwable> error = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> { try { value.set(action.get()); } catch(Throwable e) { error.set(e); } });
        if(error.get()!=null) throw new AssertionError(error.get()); return value.get();
    }
    static void edt(Runnable action) throws Exception { edt(() -> { action.run(); return null; }); }
    static List<Component> all(Container root) {
        List<Component> result = new ArrayList<>(); for(Component c:root.getComponents()) { result.add(c); if(c instanceof Container r) result.addAll(all(r)); } return result;
    }
    static JDialog dialog() throws Exception {
        return edt(() -> Arrays.stream(frame.getOwnedWindows()).filter(w -> w instanceof JDialog && w.isVisible()).map(JDialog.class::cast).findFirst().orElse(null));
    }
    static JButton button(Container root,String name) throws Exception {
        return edt(() -> all(root).stream().filter(JButton.class::isInstance).map(JButton.class::cast).filter(b -> b.getText().equals(name)).findFirst().orElseThrow());
    }
    static void click(Component c) throws Exception {
        Point p=edt(c::getLocationOnScreen); Dimension s=edt(() -> c.getSize());
        robot.mouseMove(p.x+s.width/2,p.y+s.height/2); robot.mousePress(InputEvent.BUTTON1_DOWN_MASK); robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK); robot.waitForIdle(); Thread.sleep(180);
    }
    static void click(String name) throws Exception { click(button(dialog(),name)); }
    static void key(int code) { robot.keyPress(code);robot.keyRelease(code);robot.waitForIdle(); }
    static void shortcut(int code) { robot.keyPress(KeyEvent.VK_CONTROL);key(code);robot.keyRelease(KeyEvent.VK_CONTROL);robot.waitForIdle(); }
    static void text(String name,String value) throws Exception {
        edt(() -> all(uncheckedDialog()).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast)
                .filter(c -> name.equals(c.getAccessibleContext().getAccessibleName())).findFirst().orElseThrow().setText(value));
    }
    static JDialog uncheckedDialog() { return Arrays.stream(frame.getOwnedWindows()).filter(w -> w instanceof JDialog && w.isVisible()).map(JDialog.class::cast).findFirst().orElseThrow(); }
    static void snapshot(String name) throws Exception {
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
        if (!List.of("edit", "conflict-edit", "add-acquisition", "manual-add", "delete-confirmation", "change-review").contains(name)) { return; }
        Files.createDirectories(Path.of("review/screenshots/s3"));
        Window selected=window; Rectangle r=edt(() -> new Rectangle(selected.getLocationOnScreen(),selected.getSize()));
        ImageIO.write(robot.createScreenCapture(r),"png",Path.of("review/screenshots/s3/"+capturePrefix+"-"+name+".png").toFile());
        System.out.println("Captured "+capturePrefix+"-"+name);
    }
    static void waitFor(Supplier<Boolean> condition) throws Exception {
        long until=System.nanoTime()+10_000_000_000L;while(!edt(condition)) {if(System.nanoTime()>until)throw new AssertionError("Timed out");Thread.sleep(50);}robot.waitForIdle();
    }
    static void closed() throws Exception { waitFor(() -> Arrays.stream(frame.getOwnedWindows()).noneMatch(w -> w instanceof JDialog && w.isVisible())); edt(() -> { frame.toFront(); frame.requestFocus(); }); Thread.sleep(150); }
    static JComponent tokenRow(String identity) {
        return all(frame).stream().filter(JComponent.class::isInstance).map(JComponent.class::cast)
                .filter(c -> "divider".equals(c.getClientProperty("totipo.rowPresentation"))
                        && c.getAccessibleContext().getAccessibleName().contains(identity)).findFirst().orElseThrow();
    }
    static void editIdentity(String identity) throws Exception {
        edt(() -> tokenRow(identity).dispatchEvent(new MouseEvent(tokenRow(identity), MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1)));
        S5SwingSmoke.menu("Edit…");
        waitFor(() -> Arrays.stream(frame.getOwnedWindows()).anyMatch(w -> w instanceof JDialog && w.isVisible()));
    }
    static void editGithub() throws Exception { editIdentity("GitHub"); }
    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> { error.printStackTrace(); System.exit(1); });
        theme=args.length==0?"light":args[0];vault=Files.createTempDirectory("totipo-s3-review-");
        capturePrefix=theme+(args.length>1?"-font"+args[1]:"");
        try(var initial=((CreateVaultResult.Created)NioTotipo.create(vault,"review".toCharArray())).session()) {
            try(var builder=initial.state().createToken();var secret=NewSecret.copyOf(new byte[]{102})) {
                builder.issuer("GitHub").account("niki@example.com").secret(secret).save();
            }
        }
        robot=new Robot(); robot.setAutoDelay(60);
        edt(() -> {
            if (!theme.equals("native")) {
                try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
                catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) { throw new AssertionError(e); }
            }
            if(theme.equals("dark")) {
                for(String key:List.of("Panel.background","List.background","TextField.background","PasswordField.background","TextArea.background","ToggleButton.background","RadioButton.background","Button.background","Viewport.background","ScrollPane.background", "MenuBar.background", "Menu.background", "MenuItem.background", "PopupMenu.background")) UIManager.put(key,new Color(38,42,47));
                for(String key:List.of("Label.foreground","List.foreground","TextField.foreground","PasswordField.foreground","TextArea.foreground","ToggleButton.foreground","RadioButton.foreground","Button.foreground", "Menu.foreground", "MenuItem.foreground"))UIManager.put(key,new Color(230,232,235));
                UIManager.put("List.selectionBackground",new Color(80,120,190));UIManager.put("List.selectionForeground",Color.WHITE);
            }
            ApplicationFonts.install();
            if (args.length > 1) {
                float size = Float.parseFloat(args[1]);
                var defaults = UIManager.getDefaults();
                for (Object key : new ArrayList<>(defaults.keySet())) {
                    if (defaults.get(key) instanceof Font font) { defaults.put(key, new javax.swing.plaf.FontUIResource(font.deriveFont(size))); }
                }
            }
            frame=new ShellFrame(); frame.setLocation(0,0);
            VaultAccess access=new VaultAccess() {
                public OpenResult open(Path path,char[] password) {var result=NioTotipo.open(path,password); if(result instanceof OpenResult.Opened opened)session=opened.session();return result;}
                public CreateVaultResult create(Path path,char[] password) {return NioTotipo.create(path,password);}
            };
            app=new DesktopApplication(access,frame,path -> new VaultContent(frame));app.show();app.begin(vault,"review".toCharArray(),false);
        });
        waitFor(() -> app.state()==ShellState.UNLOCKED && all(frame).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Show Code")));
        TokenId github=session.state().tokens().get(0).id();
        shortcut(KeyEvent.VK_N);snapshot("add-acquisition");
        Component method=edt(() -> all(uncheckedDialog()).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast).filter(b -> b.getText().equals("Manual entry")).findFirst().orElseThrow());click(method);
        snapshot("manual-add");
        click("Review");snapshot("validation");
        Component focused=edt(() -> KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
        if(!(focused instanceof JPasswordField))throw new AssertionError("Invalid secret did not receive focus");
        text("Issuer / service","Example Service");text("Account","review@example.com");text("New Base32 secret","MY");
        // Enter uses the same validation/review action as a click.
        edt(() -> all(uncheckedDialog()).stream().filter(JPasswordField.class::isInstance).findFirst().orElseThrow().requestFocusInWindow());key(KeyEvent.VK_ENTER);
        snapshot("manual-review");click("Add");closed();
        waitFor(() -> session.state().tokens().size()==2);
        shortcut(KeyEvent.VK_N); Thread.sleep(250); text("Setup URI","otpauth://totp/GitHub:niki%40example.com?secret=MY&issuer=GitHub&algorithm=SHA512&digits=8&period=60");click("Review");snapshot("uri-review");click("Add");snapshot("duplicate-choice");
        if(edt(() -> uncheckedDialog().getRootPane().getDefaultButton().getText()).equals("Update Existing…"))throw new AssertionError("Unsafe duplicate default");
        click("Update Existing…");snapshot("update-existing-review");click("Save setup");closed();
        waitFor(() -> session.state().token(github).orElseThrow().alternatives().get(0).descriptor().algorithm()==TotpAlgorithm.SHA512);
        editGithub();snapshot("edit");
        // Normal keyboard traversal visits real labeled fields/actions, no reveal.
        key(KeyEvent.VK_TAB); key(KeyEvent.VK_TAB); key(KeyEvent.VK_TAB);
        click("Change setup…");snapshot("change-acquisition");text("Setup URI","otpauth://totp/Other:ignored?secret=MY&algorithm=SHA256&digits=7&period=45");click("Review");snapshot("change-review");click("Save setup");closed();
        waitFor(() -> session.state().token(github).orElseThrow().alternatives().get(0).descriptor().algorithm()==TotpAlgorithm.SHA256);
        editGithub();text("Account","renamed@example.com");click("Save");closed();
        waitFor(() -> session.state().token(github).orElseThrow().alternatives().get(0).descriptor().account().equals("renamed@example.com"));
        if(!session.state().token(github).orElseThrow().alternatives().get(0).descriptor().issuer().equals("GitHub"))throw new AssertionError("URI replaced identity");
        editGithub();click("Delete TOTP…");snapshot("delete-confirmation");key(KeyEvent.VK_ESCAPE);
        if(!dialog().getTitle().equals("Edit TOTP"))throw new AssertionError("Escape did not cancel deletion");
        click("Delete TOTP…");click("Cancel");
        if(!dialog().getTitle().equals("Edit TOTP"))throw new AssertionError("Cancel did not return to Edit");
        click("Delete TOTP…");click("Delete TOTP");closed();
        waitFor(() -> session.state().token(github).orElseThrow().alternatives().get(0).descriptor().status()==TokenStatus.TOMBSTONED);
        snapshot("after-delete");
        shortcut(KeyEvent.VK_N); Thread.sleep(250); text("Setup URI","otpauth://totp/Abandoned:review?secret=MY");click("Review");shortcut(KeyEvent.VK_L);
        waitFor(() -> app.state()==ShellState.LOCKED);closed();snapshot("locked-after-review");
        edt(() -> app.begin(vault,"review".toCharArray(),false));waitFor(() -> app.state()==ShellState.UNLOCKED);closed();
        if(session.state().tokens().size()!=2)throw new AssertionError("Abandoned draft published");
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
        snapshot("conflict-edit"); key(KeyEvent.VK_ESCAPE); closed();
        edt(app::shutdown);waitFor(() -> !frame.isDisplayable());
        try(var paths=Files.walk(vault)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}
        System.out.println("PASS: real Swing/NIO Add, Edit, setup replacement, duplicate update, Delete, Enter/Escape/Tab/Ctrl+L, reopen; "+theme);
        System.exit(0);
    }
}
