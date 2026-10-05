package org.totipo.desktop.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JFileChooser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.edt;

class VaultDirectoryChooserTest {
    @TempDir Path directory;

    @Test void createRetainsExistingDirectoryOnlyConfiguration() throws Exception {
        edt(() -> {
            var chooser = new VaultDirectoryChooser(null);
            assertEquals(JFileChooser.DIRECTORIES_ONLY, chooser.getFileSelectionMode());
            assertFalse(chooser.isMultiSelectionEnabled());
            assertFalse(chooser.isAcceptAllFileFilterUsed());
            assertEquals(0, chooser.getChoosableFileFilters().length);
            assertEquals("Select New Vault Folder", chooser.getDialogTitle());
            assertEquals("Select Folder", chooser.getApproveButtonText());
            assertEquals(chooser.getDialogTitle(), chooser.getAccessibleContext().getAccessibleName());
            assertTrue(chooser.getPreferredSize().width >= 750 && chooser.getPreferredSize().width <= 850);
            assertTrue(chooser.getPreferredSize().height >= 500 && chooser.getPreferredSize().height <= 600);
            assertEquals(new JFileChooser().getCurrentDirectory(), chooser.getCurrentDirectory());
            assertNull(chooser.getSelectedFile());
        });
    }

    @Test void createStartsInCurrentVaultParentWithoutPreselection() throws Exception {
        Path vault = Files.createDirectory(directory.resolve("current vault"));
        edt(() -> {
            var create = new VaultDirectoryChooser(vault);
            assertEquals(directory.toFile(), create.getCurrentDirectory());
            assertNull(create.getSelectedFile());
        });
    }

    @Test void staleLocationUsesNearestExistingParentWithoutSelectingMissingFolder() throws Exception {
        edt(() -> {
            var chooser = new VaultDirectoryChooser(directory.resolve("missing/old-vault"));
            assertEquals(directory.toFile(), chooser.getCurrentDirectory());
            assertNull(chooser.getSelectedFile());
        });
    }
}
