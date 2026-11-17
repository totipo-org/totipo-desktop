package org.totipo.desktop.ui;

import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.edt;

class RestoreFormTest {
    @TempDir Path directory;
    @Test void browseReturnsToSameFormPreservingNamePasswordAndOpenChoiceWithoutAllocation() throws Exception {
        edt(() -> {
            AtomicInteger browses = new AtomicInteger();
            RestoreForm form = new RestoreForm(directory.resolve("<backup>.zip"), false, initial -> {
                browses.incrementAndGet(); assertEquals(directory.resolve("My Vault"), initial); return directory.resolve("parent");
            });
            form.destination.setText(directory.resolve("My Vault").toString()); form.password.setText("test password");
            form.open.setSelected(false); form.browse.doClick(0);
            assertEquals(1, browses.get()); assertEquals(directory.resolve("parent/My Vault").toString(), form.destination.getText());
            try (var input = form.submit()) {
                assertNotNull(input); assertFalse(input.open()); assertNull(input.recoveryTarget());
                assertEquals(directory.resolve("parent/My Vault"), input.destination().path());
                assertArrayEquals("test password".toCharArray(), input.password().takePassword());
            }
            assertFalse(Files.exists(directory.resolve("parent"))); form.clear(); assertEquals(0, form.password.getPassword().length);
            assertEquals("Original vault password", form.password.getAccessibleContext().getAccessibleName());
            assertEquals("New folder path", form.destination.getAccessibleContext().getAccessibleName());
        });
    }
    @Test void browseFromEmptyPathSuggestsNewChildAndKeepsSelectedPathAccessible() throws Exception {
        edt(() -> {
            RestoreForm form = new RestoreForm(directory.resolve("input.zip"), false, initial -> {
                assertNull(initial); return directory;
            });
            form.browse.doClick(0); assertEquals(directory.resolve("Restored Vault").toString(), form.destination.getText());
            assertFalse(Files.exists(directory.resolve("Restored Vault")));
        });
    }
    @Test void browseCancellationKeepsInputAndInvalidPathDoesNotConsumePassword() throws Exception {
        edt(() -> {
            RestoreForm form = new RestoreForm(directory.resolve("input.zip"), false, initial -> null);
            form.password.setText("test password"); form.destination.setText("relative"); form.browse.doClick(0);
            assertEquals("relative", form.destination.getText()); assertNull(form.submit());
            assertArrayEquals("test password".toCharArray(), form.password.getPassword());
            form.destination.setText(directory.resolve("new").toString());
            try (var input = form.submit()) { assertNotNull(input); }
            form.clear(); assertFalse(Files.exists(directory.resolve("new")));
        });
    }
    @Test void changingZipReturnsToSameFormAndCancellationKeepsLastSelectionAndInput() throws Exception {
        edt(() -> {
            AtomicInteger selections = new AtomicInteger(); Path replacement = directory.resolve("replacement.zip");
            RestoreForm form = new RestoreForm(directory.resolve("original.zip"), false, initial -> null,
                    () -> selections.getAndIncrement() == 0 ? replacement : null);
            form.destination.setText(directory.resolve("new").toString()); form.password.setText("test password");
            form.open.setSelected(false); form.chooseZip.doClick(0);
            assertEquals("replacement.zip", form.file.getText()); assertEquals(replacement.toString(), form.file.getToolTipText());
            form.chooseZip.doClick(0);
            try (var input = form.submit()) {
                assertNotNull(input); assertEquals(replacement, input.zip()); assertFalse(input.open());
                assertEquals(directory.resolve("new"), input.destination().path());
                char[] password = input.password().takePassword();
                assertArrayEquals("test password".toCharArray(), password); java.util.Arrays.fill(password, '\0');
            }
            form.clear(); assertFalse(Files.exists(directory.resolve("new")));
        });
    }
    @Test void recoverySelectsExistingFolderAndCancelledSubmissionWipesUnclaimedPassword() throws Exception {
        edt(() -> {
            RestoreForm form = new RestoreForm(directory.resolve("input.zip"), true, initial -> directory);
            form.browse.doClick(0); form.password.setText("test password");
            var input = form.submit(); assertNotNull(input); assertNull(input.destination()); assertEquals(directory, input.recoveryTarget());
            input.close(); assertNull(input.password().takePassword()); form.clear();
            JRootPane root = new JRootPane(); JButton restore = new JButton("Restore"); AtomicInteger cancels = new AtomicInteger();
            SwingUsability.dialog(root, restore, cancels::incrementAndGet);
            assertSame(restore, root.getDefaultButton());
            Object binding = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("ESCAPE"));
            root.getActionMap().get(binding).actionPerformed(null); assertEquals(1, cancels.get());
        });
    }
}
