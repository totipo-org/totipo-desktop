package org.totipo.desktop;

import org.junit.jupiter.api.Test;
import org.totipo.desktop.ui.*;
import java.util.ArrayDeque;
import java.util.Queue;
import javax.swing.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.*;
import static org.totipo.desktop.TokenWriteControllerTest.*;

class S3RetirementTest {
    @Test void queuedValidDraftIsWipedAndCannotPublishAfterSessionRetirement() throws Exception {
        Queue<Runnable> tasks = new ArrayDeque<>(); var fake = new TokenWritesTest.Recording();
        class View extends Window {
            TokenManagementPanel panel;
            @Override public void manageToken(TokenManagementPanel form) { panel = form; }
            @Override public void retireEditor() { panel = null; }
        }
        View view = new View(); TokenWriteController[] controller = new TokenWriteController[1];
        edt(() -> {
            controller[0] = new TokenWriteController(tasks::add, view, () -> {}, new MutationGate(view::writeAvailability));
            controller[0].open(fake.state, null, ""); TokenManagementPanel panel = view.panel;
            acquireAndAdd(panel); assertEquals(1, tasks.size()); controller[0].closing(); assertNull(view.panel);
            button(panel, "Add").doClick(0); assertFalse(button(panel, "Add").isEnabled());
        });
        while (!tasks.isEmpty()) { tasks.remove().run(); }
        edt(() -> assertNull(view.panel)); assertTrue(fake.calls.isEmpty());
    }
    @Test void acquiredReviewCancellationCannotResurrectOrPublishWhenReopened() throws Exception {
        Queue<Runnable> tasks = new ArrayDeque<>(); var fake = new TokenWritesTest.Recording();
        class View extends Window {
            TokenManagementPanel panel;
            @Override public void manageToken(TokenManagementPanel form) { panel = form; }
            @Override public void retireEditor() { panel = null; }
        }
        View view = new View();
        edt(() -> {
            var controller = new TokenWriteController(tasks::add, view, () -> {}, new MutationGate(view::writeAvailability));
            controller.open(fake.state, null, ""); TokenManagementPanel abandoned = view.panel;
            password(abandoned).setText("otpauth://totp/Service:account?secret=MY"); button(abandoned, "Review").doClick(0);
            button(abandoned, "Cancel").doClick(0); assertNull(view.panel); button(abandoned, "Add").doClick(0);
            controller.open(fake.state, null, ""); assertNotSame(abandoned, view.panel);
            assertNotNull(button(view.panel, "Review")); assertEquals(0, password(view.panel).getPassword().length);
            controller.closing();
        });
        while (!tasks.isEmpty()) { tasks.remove().run(); }
        assertTrue(fake.calls.isEmpty());
    }
}
