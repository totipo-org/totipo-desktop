package org.totipo.desktop.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.edt;

class NewVaultPickerTest {
    @TempDir Path directory;

    @Test void newModeNavigatesAndSelectsOrdinaryEmptyDirectory() throws Exception {
        Path child = Files.createDirectory(directory.resolve("empty"));
        AtomicReference<Path> selected = new AtomicReference<>();
        edt(() -> {
            try (var picker = new DirectoryPicker(directory.resolve("a"), DirectoryPicker.Mode.NEW_VAULT,
                    selected::set, () -> { }, Runnable::run)) {
                assertTrue(DirectoryPickerTest.components(picker).stream().noneMatch(JFileChooser.class::isInstance));
                assertTrue(DirectoryPickerTest.components(picker).contains(picker.folderName));
                picker.directories.setSelectedValue(child, true);
                var key = picker.directories.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("ENTER"));
                picker.directories.getActionMap().get(key).actionPerformed(null);
                assertEquals(child, picker.currentDirectory()); assertNull(selected.get());
                picker.select.doClick(0); assertEquals(child, selected.get());
                picker.up.doClick(0); assertEquals(directory, picker.currentDirectory());
                assertNotNull(picker.directories.getInputMap().get(KeyStroke.getKeyStroke("DOWN")));
                assertEquals("Folder name", picker.folderName.getAccessibleContext().getAccessibleName());
                assertEquals(java.awt.event.KeyEvent.VK_N, picker.newFolder.getMnemonic());
            }
        });
        assertFalse(Files.exists(child.resolve("vault"))); assertFalse(Files.exists(child.resolve("objects-v1")));
    }

    @Test void explicitNewFolderCreatesOneEmptyChildAndSelectionReturnsOnlyItsPath() throws Exception {
        AtomicReference<Path> selected = new AtomicReference<>(); Path child = directory.resolve("new child");
        edt(() -> {
            try (var picker = new DirectoryPicker(directory.resolve("a"), DirectoryPicker.Mode.NEW_VAULT,
                    selected::set, () -> { }, Runnable::run)) {
                picker.folderName.setText("new child"); picker.newFolder.doClick(0);
                assertEquals(child, picker.currentDirectory()); assertNull(selected.get());
                assertEquals("", picker.folderName.getText());
                picker.select.doClick(0); assertEquals(child, selected.get());
            }
        });
        try (var children = Files.list(child)) { assertEquals(0, children.count()); }
    }

    @Test void cancelAndEscapeNeverCreateTypedFolderOrSelectAnything() throws Exception {
        AtomicReference<Path> selected = new AtomicReference<>(); AtomicInteger cancelled = new AtomicInteger();
        edt(() -> {
            try (var picker = new DirectoryPicker(directory.resolve("a"), DirectoryPicker.Mode.NEW_VAULT,
                    selected::set, cancelled::incrementAndGet, Runnable::run)) {
                picker.folderName.setText("unsubmitted"); picker.cancel.doClick(0);
                var key = picker.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(KeyStroke.getKeyStroke("ESCAPE"));
                picker.getActionMap().get(key).actionPerformed(null);
                assertEquals(2, cancelled.get()); assertNull(selected.get());
            }
        });
        try (var children = Files.list(directory)) { assertEquals(0, children.count()); }
    }

    @Test void collisionsAndPermissionFailuresUseFixedTextAndKeepDestination() throws Exception {
        Path collision = Files.writeString(directory.resolve("taken"), "keep");
        edt(() -> {
            try (var picker = new DirectoryPicker(directory.resolve("a"), DirectoryPicker.Mode.NEW_VAULT,
                    path -> fail(), () -> { }, Runnable::run)) {
                picker.createChild("taken");
                assertEquals("A file or folder with that name already exists. Choose another name.", picker.status.getText());
                assertEquals(directory, picker.currentDirectory()); assertTrue(picker.select.isEnabled());
            }
            for (boolean security : new boolean[] {false, true}) {
                try (var picker = new DirectoryPicker(directory.resolve("a"), DirectoryPicker.Mode.NEW_VAULT,
                        path -> fail(), () -> { }, Runnable::run, path -> {
                            if (security) { throw new SecurityException("secret raw detail"); }
                            throw new java.nio.file.AccessDeniedException("secret raw detail");
                        })) {
                    picker.createChild("denied");
                    assertEquals("This folder could not be created. Check permissions or choose another folder.", picker.status.getText());
                    assertEquals(directory, picker.currentDirectory()); assertTrue(picker.newFolder.isEnabled());
                }
            }
        });
        assertEquals("keep", Files.readString(collision)); assertFalse(Files.exists(directory.resolve("denied")));
    }

    @Test void invalidNestedAndProtocolNamesCannotCreateAnything() throws Exception {
        AtomicInteger writes = new AtomicInteger();
        edt(() -> {
            try (var picker = new DirectoryPicker(directory.resolve("a"), DirectoryPicker.Mode.NEW_VAULT,
                    path -> fail(), () -> { }, Runnable::run, path -> writes.incrementAndGet())) {
                for (String name : List.of("", " ", ".", "..", "a/b", "a\\b", directory.toString(), "vault", "objects-v1", "bad\0name")) {
                    picker.createChild(name); assertTrue(picker.status.isVisible());
                }
                assertEquals(0, writes.get());
            }
            try (var existing = new DirectoryPicker(directory.resolve("a"), path -> fail(), () -> { }, Runnable::run)) {
                existing.createChild("not-allowed");
                assertFalse(DirectoryPickerTest.components(existing).contains(existing.newFolder));
            }
        });
        try (var children = Files.list(directory)) { assertEquals(0, children.count()); }
    }

    @Test void creationDisablesNavigationAndSelectionAndRetirementRejectsLateResult() throws Exception {
        List<Runnable> tasks = new ArrayList<>(); AtomicReference<Path> selected = new AtomicReference<>();
        edt(() -> {
            var picker = new DirectoryPicker(directory.resolve("a"), DirectoryPicker.Mode.NEW_VAULT,
                    selected::set, () -> { }, tasks::add);
            tasks.removeFirst().run(); picker.createChild("child");
            assertFalse(picker.select.isEnabled()); assertFalse(picker.up.isEnabled()); assertFalse(picker.newFolder.isEnabled());
            assertTrue(picker.cancel.isEnabled()); picker.select.doClick(0); assertNull(selected.get());
            picker.close(); tasks.removeFirst().run();
            assertNull(picker.currentDirectory()); assertNull(selected.get());
        });
        assertFalse(Files.exists(directory.resolve("child")));
    }

    @Test void staleInitialLocationUsesNearestExistingParentWithoutPreselection() throws Exception {
        edt(() -> {
            try (var picker = new DirectoryPicker(directory.resolve("missing/a"), DirectoryPicker.Mode.NEW_VAULT,
                    path -> { }, () -> { }, Runnable::run)) {
                assertEquals(directory, picker.currentDirectory()); assertNull(picker.directories.getSelectedValue());
            }
        });
    }

    @Test void newVaultEntryPointUsesOnlyAppOwnedPicker() throws Exception {
        try (var sources = Files.walk(Path.of("src/main/java"))) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(source).contains("JFileChooser"), source.toString());
            }
        }
    }
}
