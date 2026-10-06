package org.totipo.desktop;

import java.util.ArrayDeque;
import org.junit.jupiter.api.Test;
import org.totipo.desktop.ui.*;
import static org.totipo.desktop.TestSupport.edt;
import static org.junit.jupiter.api.Assertions.*;

class S5PasswordRetirementTest {
    @Test void retirementDiscardsQueuedPasswordReplacementBeforeCallingJava() throws Exception {
        var tasks = new ArrayDeque<Runnable>(); var session = new PasswordChangeTest.PasswordSession();
        var view = new PasswordChangeTest.PasswordView();
        edt(() -> {
            var gate = new MutationGate(view::writeAvailability);
            var controller = new PasswordChangeController(session, tasks::add, view, gate, reason -> fail());
            controller.open(); PasswordChangePanel form = view.panel;
            PasswordChangeTest.enterAndSubmit(form); assertEquals(1, tasks.size());
            gate.closing(); controller.closing(); assertNull(view.panel);
            for (var field : PasswordChangeTest.fields(form)) { assertEquals(0, field.getPassword().length); }
        });
        while (!tasks.isEmpty()) { tasks.remove().run(); }
        assertEquals(0, session.calls);
        edt(() -> { assertNull(view.panel); assertFalse(view.available); });
    }
}
