package org.totipo.desktop.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import org.totipo.desktop.ApplicationInfo;

/** Application details independent of vault state. */
final class AboutApplicationPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    final JButton close = new JButton("Close");
    final JEditorPane project = new JEditorPane();
    final JTextArea details = new JTextArea(ApplicationInfo.NAME + "\nVersion "
            + ApplicationInfo.version());

    @FunctionalInterface
    interface Browser { void browse(URI uri) throws IOException; }

    AboutApplicationPanel(Runnable closed) {
        this(closed, AboutApplicationPanel::browse, null);
    }

    AboutApplicationPanel(Runnable closed, Browser browser, Consumer<String> feedback) {
        Edt.require();
        setLayout(new BorderLayout(0, DesktopStyle.TIGHT));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 8, 24));
        details.setEditable(false); details.setOpaque(false);
        details.setFont(DesktopStyle.font(DesktopStyle.Typography.Body));
        details.setForeground(DesktopStyle.text());
        details.getAccessibleContext().setAccessibleName("Application name and version");
        details.setCaretPosition(0);
        project.setContentType("text/html");
        project.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        project.setFont(DesktopStyle.font(DesktopStyle.Typography.Body));
        project.setEditable(false); project.setOpaque(false);
        project.setBorder(null);
        String linkColor = String.format("#%06x", DesktopStyle.accent().getRGB() & 0xffffff);
        project.setText("<html><body style='margin:0'><a style='color:" + linkColor
                + "' href='" + ApplicationInfo.REPOSITORY + "'>" + ApplicationInfo.REPOSITORY + "</a></body></html>");
        project.setCaretPosition(0);
        project.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        project.setToolTipText(ApplicationInfo.REPOSITORY);
        project.getAccessibleContext().setAccessibleName(ApplicationInfo.REPOSITORY);
        project.getAccessibleContext().setAccessibleDescription("Open " + ApplicationInfo.REPOSITORY + " in your default browser");
        Runnable openProject = () -> {
            if (!project.isEnabled()) { return; }
            project.setEnabled(false);
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws IOException {
                    browser.browse(URI.create(ApplicationInfo.REPOSITORY));
                    return null;
                }
                @Override protected void done() {
                    project.setEnabled(true);
                    try { get(); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                    catch (ExecutionException failure) {
                        String message = "Could not open your default browser. Open this address manually:\n"
                                + ApplicationInfo.REPOSITORY;
                        if (feedback != null) { feedback.accept(message); }
                        else if (isShowing()) {
                            JOptionPane.showMessageDialog(AboutApplicationPanel.this, message,
                                    "Project on GitHub", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            }.execute();
        };
        project.addHyperlinkListener(event -> {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) { openProject.run(); }
        });
        Action openLink = SwingUsability.action("Open project on GitHub", openProject);
        for (String key : new String[]{"ENTER", "SPACE"}) {
            SwingUsability.bind(project, JComponent.WHEN_FOCUSED, KeyStroke.getKeyStroke(key), "open-project", openLink);
        }
        SwingUsability.bind(project, JComponent.WHEN_IN_FOCUSED_WINDOW,
                KeyStroke.getKeyStroke(KeyEvent.VK_G, java.awt.event.InputEvent.ALT_DOWN_MASK), "open-project", openLink);
        close.setMnemonic('C');
        DesktopStyle.action(close, DesktopStyle.ActionRole.SecondaryAction, false);
        close.addActionListener(event -> closed.run());
        JPanel information = new JPanel(new BorderLayout(0, DesktopStyle.NORMAL));
        information.setOpaque(false);
        information.add(details, BorderLayout.NORTH);
        information.add(project, BorderLayout.SOUTH);
        add(information, BorderLayout.CENTER);
        add(SwingUsability.taskActions(close), BorderLayout.SOUTH);
    }

    private static void browse(URI uri) throws IOException {
        if (!Desktop.isDesktopSupported()) { throw new UnsupportedOperationException("Desktop browsing unavailable"); }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            throw new UnsupportedOperationException("Desktop browsing unavailable");
        }
        desktop.browse(uri);
    }

    void installDialog(JRootPane root) { SwingUsability.dialog(root, close, () -> close.doClick()); }

    static JMenu menu(Window owner) {
        JMenu help = new JMenu("Help"); help.setMnemonic('H');
        JMenuItem about = new JMenuItem("About Totipo…"); about.setMnemonic('A');
        about.addActionListener(event -> {
            JDialog dialog = new JDialog(owner, "About Totipo", java.awt.Dialog.ModalityType.MODELESS);
            AboutApplicationPanel content = new AboutApplicationPanel(dialog::dispose);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setContentPane(content); content.installDialog(dialog.getRootPane());
            dialog.pack(); SwingUsability.fit(dialog, dialog.getWidth(), dialog.getHeight());
            dialog.setLocationRelativeTo(owner); dialog.setVisible(true);
        });
        help.add(about); return help;
    }
}
