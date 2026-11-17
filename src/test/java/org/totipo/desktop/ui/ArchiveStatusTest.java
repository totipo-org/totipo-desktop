package org.totipo.desktop.ui;

import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.edt;

class ArchiveStatusTest {
    @Test void progressIsInWindowCancellationUsesCurrentActionAndSuccessSurvivesCleanup() throws Exception {
        edt(() -> {
            ArchiveDialogs dialogs = new ArchiveDialogs(null);
            AtomicInteger old = new AtomicInteger(), current = new AtomicInteger();
            dialogs.progress("Checking backup…", old::incrementAndGet);
            dialogs.progress("Restoring…", current::incrementAndGet);
            Object escape = dialogs.status.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(KeyStroke.getKeyStroke("ESCAPE"));
            dialogs.status.getActionMap().get(escape).actionPerformed(null);
            assertEquals(0, old.get()); assertEquals(1, current.get());
            assertTrue(dialogs.status.isVisible()); dialogs.progressClosed(); assertFalse(dialogs.status.isVisible());
            dialogs.status("Backup saved."); dialogs.progressClosed(); assertTrue(dialogs.status.isVisible());
            assertEquals("Backup saved.", ((JLabel) dialogs.status.getComponent(0)).getText());
            dialogs.status.getActionMap().get(escape).actionPerformed(null); assertEquals(1, current.get());
        });
    }
}
