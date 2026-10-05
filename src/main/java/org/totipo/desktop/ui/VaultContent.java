package org.totipo.desktop.ui;

import org.totipo.desktop.clipboard.TotpClipboard;

import org.totipo.VaultState;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JOptionPane;

/** Session-owned content and child flows inside the persistent application frame. */
public final class VaultContent implements VaultView {
    private final ShellFrame owner;
    private final VaultPanel panel = new VaultPanel();
    private javax.swing.JDialog editor;
    private PasswordChangeDialog passwordDialog;

    public VaultContent(ShellFrame owner) {
        Edt.require();
        this.owner = owner;
    }

    @Override public void actions(Runnable refresh, Runnable close) {
        Edt.require();
        panel.onRefresh(refresh);
    }
    @Override public void quitAction(Runnable action) {
        Edt.require(); panel.exitAction(action);
    }
    @Override public void changeVaultAction(Runnable action) { panel.changeVaultAction(action); }
    @Override public void copyAction(TotpClipboard.Copy action) { panel.copyAction(action); }
    @Override public void passwordAction(Runnable action) { panel.passwordAction(action); }
    @Override public void editPassword(PasswordChangePanel content) {
        passwordDialog = new PasswordChangeDialog(owner, content); passwordDialog.setVisible(true);
    }
    @Override public void retirePassword() {
        if (passwordDialog != null) {
            PasswordChangeDialog owned = passwordDialog; passwordDialog = null; owned.dispose();
        }
    }
    @Override public void mergeAction(MergeAction action) { panel.mergeAction(action); }
    @Override public void editMerge(MergeEditorPanel content) {
        editor = new javax.swing.JDialog(owner, "Resolve Conflict", false);
        editor.setDefaultCloseOperation(javax.swing.JDialog.DO_NOTHING_ON_CLOSE);
        editor.setContentPane(content);
        content.installDialog(editor.getRootPane());
        editor.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { content.cancel(); }
        });
        SwingUsability.fit(editor, TokenEditorPanel.PREFERRED_SIZE.width, TokenEditorPanel.PREFERRED_SIZE.height);
        editor.setMinimumSize(new java.awt.Dimension(Math.min(TokenEditorPanel.MINIMUM_SIZE.width, editor.getWidth()),
                Math.min(TokenEditorPanel.MINIMUM_SIZE.height, editor.getHeight())));
        editor.setLocationRelativeTo(owner); editor.setVisible(true);
    }
    @Override public void additionalConflict(Runnable review, Runnable publish, Runnable cancel) {
        panel.additionalConflict(review, publish, cancel);
    }
    @Override public void confirmOriginalResolution(Runnable confirmed) {
        String text = "This publishes the exact resolution you already chose, based on the original selected inputs.\n"
                + "It does not include the newly observed information and does not run the merge new-information check again.\n"
                + "The resulting token may still have competing alternatives.";
        Object[] choices = {"Cancel", "Publish original resolution anyway"};
        if (JOptionPane.showOptionDialog(owner, text, "Publish original resolution anyway", JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, choices, choices[0]) == 1) { confirmed.run(); }
    }
    @Override public void mergePublicationUncertain(boolean original, boolean busy, Runnable retry, Runnable stop) {
        panel.mergePublicationUncertain(original, busy, retry, stop);
    }
    @Override public void tokenActions(Runnable create, EditAction edit) { panel.tokenActions(create, edit); }
    @Override public void writeAvailability(boolean available) { panel.writeAvailability(available); }
    @Override public void manageToken(TokenManagementPanel content) {
        editor = new javax.swing.JDialog(owner, content.title(), false);
        editor.setDefaultCloseOperation(javax.swing.JDialog.DO_NOTHING_ON_CLOSE);
        editor.setContentPane(content);
        javax.swing.JDialog owned = editor;
        content.installDialog(editor.getRootPane(), title -> {
            owned.setTitle(title);
            TaskDialogSizing.fit(owned, content);
        });
        editor.addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent event) { content.focusInitialField(); }
            @Override public void windowClosing(WindowEvent event) { content.cancel(); }
        });
        editor.setMinimumSize(new java.awt.Dimension(Math.min(560, editor.getWidth()), Math.min(260, editor.getHeight())));
        editor.setLocationRelativeTo(owner); editor.setVisible(true);
    }
    @Override public void mutationAcknowledged(org.totipo.SaveResult.Saved saved) { panel.mutationAcknowledged(saved); }
    @Override public void editToken(TokenEditorPanel content, boolean create) {
        editor = new TokenEditDialog(owner, content, create); editor.setVisible(true);
    }
    @Override public void retireEditor() { if (editor != null) { editor.dispose(); editor = null; } }
    @Override public void publicationUncertain(boolean create, boolean busy, Runnable retry, Runnable stop) {
        panel.publicationUncertain(create, busy, retry, stop);
    }
    @Override public void clearUncertainty() { panel.clearUncertainty(); }
    @Override public void abandonedPublication(boolean abandoned) { panel.abandonedPublication(abandoned); }
    @Override public void writeMessage(String message) { panel.writeMessage(message); }
    @Override public void writeWarning(String detail, String message) { panel.writeWarning(message); }
    @Override public void totpAction(TotpAction action) { panel.totpAction(action); }
    @Override public void render(VaultState state) { panel.render(state); }
    @Override public void closing() { panel.closing(); owner.retireDialogs(); }
    @Override public void failure() {
        Edt.require();
        owner.contentFailure();
    }
    @Override public void showWindow() { Edt.require(); owner.mount(panel); }
    @Override public void hideWindow() { Edt.require(); owner.unmount(panel); }
    @Override public void dispose() { Edt.require(); panel.closing(); owner.unmount(panel); }
}
