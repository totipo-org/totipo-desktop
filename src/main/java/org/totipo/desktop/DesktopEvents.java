package org.totipo.desktop;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.desktop.SystemSleepEvent;
import java.awt.desktop.SystemSleepListener;
import java.awt.desktop.UserSessionEvent;
import java.awt.desktop.UserSessionListener;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import org.totipo.desktop.ui.ShellView;
import javax.swing.SwingUtilities;

/** Pure JDK hooks, registered only for the production window and removed on Exit. */
final class DesktopEvents implements AutoCloseable {
    private final Window window;
    private final Runnable activity;
    private final Runnable lock;
    private final Runnable check;
    private final ResumeGuard resume;
    private final AWTEventListener listener = this::event;
    private final java.awt.KeyEventDispatcher keys = this::key;
    private Desktop desktop;
    private boolean sessionHook;
    private boolean sleepHook;
    private boolean closed;
    private final UserSessionListener sessions = new UserSessionListener() {
        public void userSessionDeactivated(UserSessionEvent event) { scheduleLock(); }
        public void userSessionActivated(UserSessionEvent event) { scheduleLock(); }
    };
    private final SystemSleepListener sleep = new SystemSleepListener() {
        public void systemAboutToSleep(SystemSleepEvent event) { scheduleLock(); }
        public void systemAwoke(SystemSleepEvent event) { scheduleLock(); }
    };
    DesktopEvents(ShellView shell, Runnable activity, Runnable lock, Runnable check) {
        window = shell instanceof Window frame ? frame : null;
        this.activity = activity; this.lock = lock; this.check = check;
        resume = new ResumeGuard(java.time.Clock.systemUTC(), System::nanoTime, lock);
        if (window == null) { return; }
        Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.MOUSE_EVENT_MASK
                | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK | AWTEvent.WINDOW_EVENT_MASK);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keys);
        if (Desktop.isDesktopSupported()) {
            try {
                desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.APP_EVENT_USER_SESSION)) {
                    desktop.addAppEventListener(sessions); sessionHook = true;
                }
                if (desktop.isSupported(Desktop.Action.APP_EVENT_SYSTEM_SLEEP)) {
                    desktop.addAppEventListener(sleep); sleepHook = true;
                }
            } catch (UnsupportedOperationException | SecurityException unavailable) {
                // Optional platform support must not prevent portable locking.
            }
        }
    }
    private void scheduleLock() {
        if (SwingUtilities.isEventDispatchThread()) { if (!closed) { lock.run(); } }
        else { SwingUtilities.invokeLater(() -> { if (!closed) { lock.run(); } }); }
    }
    private boolean owned(Object source) {
        if (!(source instanceof Component component)) { return false; }
        Window current = component instanceof Window w ? w : SwingUtilities.getWindowAncestor(component);
        while (current != null) { if (current == window) { return true; } current = current.getOwner(); }
        return false;
    }
    private boolean key(KeyEvent event) {
        if (closed || !owned(event.getSource())) { return false; }
        return dispatchKey(event);
    }
    boolean dispatchKey(KeyEvent event) {
        if (closed) { return false; }
        poll();
        if (event.getID() == KeyEvent.KEY_PRESSED) {
            if (isLockShortcut(event)) { lock.run(); event.consume(); return true; }
            activity.run();
        }
        return false;
    }
    static boolean isLockShortcut(KeyEvent event) {
        int platform = java.awt.GraphicsEnvironment.isHeadless() ? InputEvent.CTRL_DOWN_MASK
                : Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        int modifiers = event.getModifiersEx();
        return event.getKeyCode() == KeyEvent.VK_L
                && (modifiers == InputEvent.CTRL_DOWN_MASK || modifiers == platform);
    }
    private void event(AWTEvent event) {
        if (closed || !owned(event.getSource())) { return; }
        dispatchEvent(event);
    }
    void dispatchEvent(AWTEvent event) {
        if (closed) { return; }
        if (event instanceof MouseEvent mouse) {
            if (mouse.getID() == MouseEvent.MOUSE_PRESSED || mouse.getID() == MouseEvent.MOUSE_WHEEL
                    || mouse.getID() == MouseEvent.MOUSE_DRAGGED) { poll(); activity.run(); }
        } else if (event instanceof WindowEvent windowEvent) { windowEvent(windowEvent.getID()); }
    }
    void windowEvent(int id) {
        if (!closed && (id == WindowEvent.WINDOW_ACTIVATED || id == WindowEvent.WINDOW_DEICONIFIED)) { poll(); }
    }
    void poll() {
        if (closed) { return; }
        if (window != null) { resume.check(); }
        check.run();
    }
    void unlocked() { resume.reset(); }
    @Override public void close() {
        closed = true;
        if (window == null) { return; }
        Toolkit.getDefaultToolkit().removeAWTEventListener(listener);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keys);
        if (sessionHook) { desktop.removeAppEventListener(sessions); }
        if (sleepHook) { desktop.removeAppEventListener(sleep); }
    }
}
