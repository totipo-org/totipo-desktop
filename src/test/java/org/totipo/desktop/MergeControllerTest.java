package org.totipo.desktop;

import org.totipo.*;
import org.totipo.desktop.ui.*;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.MergeFixtures.*;
import static org.totipo.desktop.TestSupport.*;
import static org.totipo.desktop.TokenWriteControllerTest.*;
import static org.totipo.desktop.TokenWritesTest.saved;
import static org.junit.jupiter.api.Assertions.*;

/** S4 controller semantics replace the legacy optional partial-publication workflow. */
class MergeControllerTest {
    static final class View extends Window {
        Runnable passwordAction; int passwordForms;
        @Override public void passwordAction(Runnable action) { passwordAction = action; }
        @Override public void editPassword(PasswordChangePanel panel) { passwordForms++; }
        void passwordBlocked() { passwordAction.run(); assertEquals(0, passwordForms); }
        MergeAction merge; MergeEditorPanel editor;
        Runnable retry, stop; String message; boolean available;
        SaveResult.Saved acknowledged;
        final BlockingQueue<String> events = new LinkedBlockingQueue<>();
        @Override public void mergeAction(MergeAction action) { merge = action; }
        @Override public void editMerge(MergeEditorPanel panel) { editor = panel; events.add("editor"); }
        @Override public void retireEditor() { editor = null; }
        @Override public void writeAvailability(boolean value) { available = value; if (value) { events.add("finished"); } }
        @Override public void mergePublicationUncertain(boolean busy, Runnable retry, Runnable stop) {
            this.retry = retry; this.stop = stop; if (!busy) { events.add("uncertain"); }
        }
        @Override public void clearUncertainty() { retry = null; stop = null; }
        @Override public void mutationAcknowledged(SaveResult.Saved saved) { acknowledged = saved; }
        @Override public void writeMessage(String value) { message = value; }
    }
    static final class Harness implements AutoCloseable {
        final Session session = new Session(); final View view = new View(); final Recording fake = new Recording();
        final CountDownLatch retired = new CountDownLatch(1); final VaultWindowController controller;
        Harness() throws Exception {
            controller = onEdt(() -> new VaultWindowController(session, view, 92, c -> retired.countDown()));
            edt(() -> { controller.start(); session.subscriber.onNext(fake.state); }); edt(() -> {});
        }
        void event(String expected) throws Exception { assertEquals(expected, view.events.poll(10, TimeUnit.SECONDS)); }
        void open() throws Exception { edt(() -> view.merge.open(fake.state, fake.token)); event("editor"); }
        void save() throws Exception { edt(() -> {
            MergeEditorTest.option(view.editor, "issuer 0 · account 0").doClick(); button(view.editor, "Resolve").doClick();
        }); }
        void flush() throws Exception { edt(() -> {}); }
        @Override public void close() { try { fake.release.countDown(); edt(controller::close); await(retired); } catch (Exception failure) { throw new AssertionError(failure); } }
    }
    static void waitChanged(Harness h) throws Exception {
        long limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!onEdt(() -> components(h.view.editor).stream().anyMatch(c -> c instanceof javax.swing.JButton b && b.getText().equals("Review Updated Conflict")))) {
            if (System.nanoTime() > limit) { fail("No updated-conflict state"); } Thread.sleep(10);
        }
    }
    @Test void wholeVersionUsesKeepOnlyAndAffirmationClosesAndAcknowledges() throws Exception {
        try (Harness h = new Harness()) {
            h.fake.results.add(saved()); h.open(); h.save(); h.event("finished");
            assertEquals(List.of("merge", "keep", "save", "close"), h.fake.calls);
            edt(() -> { assertNull(h.view.editor); assertNotNull(h.view.acknowledged); });
        }
    }
    @Test void noChoiceAndCancelNeverMutate() throws Exception {
        try (Harness h = new Harness()) {
            h.open(); edt(() -> { button(h.view.editor, "Resolve").doClick(); assertTrue(MergeEditorTest.text(h.view.editor).contains("Choose a version")); h.view.editor.cancel(); });
            h.event("finished"); assertTrue(h.fake.factories.isEmpty());
        }
    }
    @Test void additionalConflictDiscardsPartialAndRequiresFreshUnselectedReview() throws Exception {
        try (Harness h = new Harness()) {
            Recording latest = new Recording(); Partial partial = new Partial();
            h.fake.results.add(new SaveResult.AdditionalConflict(latest.state, partial)); h.open(); h.save(); waitChanged(h);
            assertEquals(0, partial.saves); assertEquals(1, partial.closes); assertEquals(1, h.fake.factories.size());
            edt(() -> assertTrue(MergeEditorTest.text(h.view.editor).contains("New conflict information appeared")));
            edt(() -> button(h.view.editor, "Review Updated Conflict").doClick()); h.event("finished"); h.event("editor");
            edt(() -> { assertTrue(button(h.view.editor, "Resolve").isEnabled()); button(h.view.editor, "Resolve").doClick(); });
            assertTrue(latest.factories.isEmpty());
        }
    }
    @Test void emittedMaterialChangeAndDisappearancePreventPublicationAndClearSecret() throws Exception {
        for (boolean disappears : List.of(false, true)) {
            try (Harness h = new Harness()) {
                h.open(); MergeEditorPanel old = onEdt(() -> h.view.editor);
                edt(() -> { button(old, "Combine details…").doClick(); password(old).setText("MY"); });
                javax.swing.JPasswordField abandonedSecret = onEdt(() -> password(old));
                Recording latest = new Recording(disappears ? List.of(alternative(0)) : List.of(alternative(0), alternative(1), alternative(2), alternative(3)), false);
                edt(() -> h.session.subscriber.onNext(latest.state)); h.flush(); waitChanged(h);
                edt(() -> { assertEquals(0, abandonedSecret.getPassword().length); button(old, "Review Updated Conflict").doClick(); });
                h.event("finished"); if (!disappears) { h.event("editor"); }
                assertTrue(h.fake.factories.isEmpty()); assertTrue(latest.factories.isEmpty());
            }
        }
    }
    @Test void definiteFailureKeepsResolverForExplicitRetry() throws Exception {
        try (Harness h = new Harness()) {
            h.fake.results.add(new SaveResult.Failed(SaveResult.Reason.PREPARATION_FAILED)); h.open(); h.save();
            long limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (!onEdt(() -> MergeEditorTest.text(h.view.editor).contains("Nothing was published"))) {
                if (System.nanoTime() > limit) { fail("No definite failure"); } Thread.sleep(10);
            }
            assertEquals(1, h.fake.factories.size()); assertNull(h.view.retry);
            h.fake.results.add(saved()); edt(() -> button(h.view.editor, "Resolve").doClick()); h.event("finished");
        }
    }

    @Test void observationUnresolvedAndClosingFailuresRetainTheirSpecificMeaning() throws Exception {
        for (SaveResult.Reason reason : List.of(SaveResult.Reason.OBSERVATION_UNAVAILABLE, SaveResult.Reason.UNRESOLVED_FIELDS, SaveResult.Reason.SESSION_CLOSING)) {
            try (Harness h = new Harness()) {
                h.fake.results.add(new SaveResult.Failed(reason)); h.open(); h.save();
                if (reason == SaveResult.Reason.SESSION_CLOSING) { await(h.retired); }
                else {
                    long limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                    while (!onEdt(() -> button(h.view.editor, "Resolve").isEnabled())) {
                        if (System.nanoTime() > limit) { fail("Failure left task busy"); } Thread.sleep(10);
                    }
                    edt(() -> { assertTrue(MergeEditorTest.text(h.view.editor).contains("not published")); assertNull(h.view.retry); });
                }
                assertEquals(1, h.fake.factories.size());
            }
        }
    }
    @Test void uncertainPublicationUsesFrozenRetryWithoutNewMerge() throws Exception {
        try (Harness h = new Harness()) {
            Handle retry = new Handle(); retry.outcomes.add(saved());
            h.fake.results.add(new SaveResult.PublicationUncertain(retry)); h.open(); h.save(); h.event("uncertain");
            assertEquals(0, retry.calls); edt(() -> assertNull(h.view.editor));
            edt(h.view.retry); h.event("finished"); assertEquals(1, retry.calls); assertEquals(1, retry.closes);
            assertEquals(1, h.fake.factories.size());
        }
    }
    @Test void closeDuringSubmittedSaveSuppressesLateUiAndCleansResultCapability() throws Exception {
        for (boolean conflict : List.of(false, true)) {
            try (Harness h = new Harness()) {
                Partial partial = new Partial(); Handle retry = new Handle();
                h.fake.results.add(conflict ? new SaveResult.AdditionalConflict(h.fake.state, partial) : new SaveResult.PublicationUncertain(retry));
                h.fake.release = new CountDownLatch(1); h.open(); h.save(); await(h.fake.entered);
                edt(h.controller::close); h.fake.release.countDown(); await(h.retired); h.flush();
                assertEquals(conflict ? 1 : 0, partial.closes); assertEquals(conflict ? 0 : 1, retry.closes);
                assertEquals(0, partial.saves); edt(() -> { assertNull(h.view.editor); assertNull(h.view.retry); assertTrue(h.view.events.isEmpty()); });
            }
        }
    }
}
