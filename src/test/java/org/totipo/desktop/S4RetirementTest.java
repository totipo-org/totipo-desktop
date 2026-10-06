package org.totipo.desktop;

import java.util.ArrayDeque;
import java.util.Queue;
import javax.swing.JPasswordField;
import org.junit.jupiter.api.Test;
import org.totipo.desktop.ui.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.*;
import static org.totipo.desktop.TokenWriteControllerTest.*;

class S4RetirementTest {
    @Test void queuedWholeAndCustomResolutionCannotPublishAfterRetirement() throws Exception {
        for (boolean custom : java.util.List.of(false, true)) {
            Queue<Runnable> tasks = new ArrayDeque<>(); var fake = new MergeFixtures.Recording();
            class View extends Window {
                MergeEditorPanel panel;
                @Override public void editMerge(MergeEditorPanel form) { panel = form; }
                @Override public void retireEditor() { panel = null; }
            }
            View view = new View();
            edt(() -> {
                var controller = new TokenWriteController(tasks::add, view, () -> {}, new MutationGate(view::writeAvailability));
                controller.openMerge(fake.state, fake.token); MergeEditorPanel abandoned = view.panel;
                if (custom) {
                    button(abandoned, "Combine details…").doClick(); MergeEditorTest.chooseFirst(abandoned);
                    password(abandoned).setText("MY"); button(abandoned, "Save Resolution").doClick();
                } else {
                    MergeEditorTest.option(abandoned, "issuer 0 · account 0").doClick(); button(abandoned, "Resolve").doClick();
                }
                assertEquals(1, tasks.size()); controller.closing(); assertNull(view.panel);
                assertFalse(button(abandoned, custom ? "Save Resolution" : "Resolve").isEnabled());
                if (custom) { assertEquals(0, password(abandoned).getPassword().length); }
            });
            while (!tasks.isEmpty()) { tasks.remove().run(); }
            assertTrue(fake.calls.isEmpty()); edt(() -> assertNull(view.panel));
        }
    }
    @Test void emittedChangeDiscardsQueuedResolutionBeforeBuilderCreation() throws Exception {
        Queue<Runnable> tasks = new ArrayDeque<>(); var fake = new MergeFixtures.Recording();
        class View extends Window {
            MergeEditorPanel panel;
            @Override public void editMerge(MergeEditorPanel form) { panel = form; }
            @Override public void retireEditor() { panel = null; }
        }
        View view = new View();
        edt(() -> {
            var controller = new TokenWriteController(tasks::add, view, () -> {}, new MutationGate(view::writeAvailability));
            controller.openMerge(fake.state, fake.token);
            MergeEditorTest.option(view.panel, "issuer 0 · account 0").doClick(); button(view.panel, "Resolve").doClick();
            controller.current(new MergeFixtures.Recording().state);
            assertNotNull(button(view.panel, "Review Updated Conflict"));
            controller.closing();
        });
        while (!tasks.isEmpty()) { tasks.remove().run(); }
        assertTrue(fake.factories.isEmpty());
    }

}
