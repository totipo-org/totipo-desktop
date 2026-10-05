package org.totipo.desktop;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import org.junit.jupiter.api.Test;
import org.totipo.*;
import org.totipo.desktop.ui.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.*;
import static org.totipo.desktop.TokenWriteControllerTest.*;

class OwnedFlowLockTest {
    private static final class View extends Window {
        Runnable add;
        Runnable password;
        EditAction edit;
        MergeAction resolve;
        TokenManagementPanel token;
        MergeEditorPanel merge;
        PasswordChangePanel passwords;
        @Override public void tokenActions(Runnable add, EditAction edit) { this.add = add; this.edit = edit; }
        @Override public void mergeAction(MergeAction action) { resolve = action; }
        @Override public void passwordAction(Runnable action) { password = action; }
        @Override public void manageToken(TokenManagementPanel panel) { token = panel; }
        @Override public void editMerge(MergeEditorPanel panel) { merge = panel; }
        @Override public void editPassword(PasswordChangePanel panel) { passwords = panel; }
        @Override public void retireEditor() { token = null; merge = null; }
        @Override public void retirePassword() { passwords = null; }
    }
    private void retire(String kind, boolean shortcut) throws Exception {
        Shell shell = new Shell(); View view = new View(); Session session = new Session();
        var fixture = new MergeFixtures.Recording();
        VaultAccess access = new VaultAccess() {
            public OpenResult open(Path path, char[] password) { return new OpenResult.Opened(session); }
            public CreateVaultResult create(Path path, char[] password) { throw new AssertionError(); }
        };
        var app = onEdt(() -> new DesktopApplication(access, shell, path -> view));
        try {
            edt(() -> app.begin(Path.of("vault"), new char[] {'p'}, false)); await(shell.ready);
            edt(() -> session.subscriber.onNext(fixture.state)); edt(() -> { });
            edt(() -> {
                switch (kind) {
                    case "add" -> view.add.run();
                    case "edit", "setup", "delete" -> view.edit.open(fixture.state, fixture.token.alternatives().getFirst(), "Edit");
                    case "resolve" -> view.resolve.open(fixture.state, fixture.token);
                    case "password" -> view.password.run();
                    default -> throw new AssertionError();
                }
                if (kind.equals("setup")) {
                    button(view.token, "Change setup…").doClick(0);
                    password(view.token).setText("otpauth://totp/Service:account?secret=MY");
                    button(view.token, "Review").doClick(0);
                }
                if (kind.equals("delete")) { button(view.token, "Delete TOTP…").doClick(0); }
                JPanel form = view.token != null ? view.token : view.merge != null ? view.merge : view.passwords;
                assertNotNull(form);
                var secrets = components(form).stream().filter(JPasswordField.class::isInstance).map(JPasswordField.class::cast).toList();
                secrets.forEach(field -> field.setText("temporary secret"));
                if (shortcut) {
                    app.userEvent(new KeyEvent(form, KeyEvent.KEY_PRESSED, 0, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_L, 'l'));
                } else { shell.lock.run(); }
                assertEquals(ShellState.LOCKED, app.state());
                assertNull(view.token); assertNull(view.merge); assertNull(view.passwords);
                secrets.forEach(field -> assertEquals(0, field.getDocument().getLength()));
                assertTrue(view.closing);
            });
            await(view.disposed); assertEquals(1, session.closes.get());
        } finally { edt(app::shutdown); await(shell.disposed); }
    }
    @Test void lockRetiresAddWithoutDraftConfirmation() throws Exception { retire("add", false); }
    @Test void ctrlLRetiresAdd() throws Exception { retire("add", true); }
    @Test void lockRetiresEditWithoutDraftConfirmation() throws Exception { retire("edit", false); }
    @Test void lockRetiresSetupReviewWithoutPublication() throws Exception { retire("setup", false); }
    @Test void ctrlLRetiresSetupReviewWithoutPublication() throws Exception { retire("setup", true); }
    @Test void lockRetiresDeleteConfirmationWithoutPublication() throws Exception { retire("delete", false); }
    @Test void ctrlLRetiresDeleteConfirmationWithoutPublication() throws Exception { retire("delete", true); }
    @Test void ctrlLRetiresEdit() throws Exception { retire("edit", true); }
    @Test void lockRetiresResolverWithoutDraftConfirmation() throws Exception { retire("resolve", false); }
    @Test void ctrlLRetiresResolver() throws Exception { retire("resolve", true); }
    @Test void lockRetiresPasswordChangeAndClearsAllFields() throws Exception { retire("password", false); }
    @Test void ctrlLRetiresPasswordChange() throws Exception { retire("password", true); }
    @Test void latePublicationAfterLockCannotRestoreSessionOrEditor() throws Exception {
        Shell shell = new Shell(); View view = new View(); Session session = new Session();
        var recording = new TokenWritesTest.Recording(); recording.release = new CountDownLatch(1);
        recording.results.add(TokenWritesTest.saved());
        VaultAccess access = new VaultAccess() {
            public OpenResult open(Path path, char[] password) { return new OpenResult.Opened(session); }
            public CreateVaultResult create(Path path, char[] password) { throw new AssertionError(); }
        };
        var app = onEdt(() -> new DesktopApplication(access, shell, path -> view));
        try {
            edt(() -> app.begin(Path.of("vault"), new char[] {'p'}, false)); await(shell.ready);
            edt(() -> session.subscriber.onNext(recording.state)); edt(() -> { });
            edt(() -> { view.add.run(); acquireAndAdd(view.token); });
            await(recording.entered);
            edt(() -> { shell.lock.run(); assertEquals(ShellState.LOCKED, app.state()); assertNull(view.token); });
            recording.release.countDown(); await(view.disposed);
            edt(() -> { assertEquals(ShellState.LOCKED, app.state()); assertNull(view.token); assertEquals(1, session.closes.get()); });
        } finally { recording.release.countDown(); edt(app::shutdown); await(shell.disposed); }
    }
}
