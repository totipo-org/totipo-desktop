package org.totipo.desktop.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.edt;

class DirectoryPickerTest {
    @TempDir Path directory;
    static List<Component> components(Container root) {
        List<Component> values = new ArrayList<>();
        for (Component child : root.getComponents()) { values.add(child); if (child instanceof Container c) { values.addAll(components(c)); } }
        return values;
    }
    @Test void onlyDirectoriesAppearAndNoFileManagerControlsAreConstructed() throws Exception {
        Path b = Files.createDirectory(directory.resolve("b")), a = Files.createDirectory(directory.resolve("a"));
        Files.writeString(directory.resolve("file"), "text");
        Files.createDirectory(directory.resolve(".hidden"));
        Files.createDirectory(directory.resolve(".git"));
        edt(() -> {
            try (var picker = new DirectoryPicker(directory.resolve("vault"), path -> { }, () -> { }, Runnable::run)) {
                assertEquals(List.of(a, b), java.util.stream.IntStream.range(0, picker.directories.getModel().getSize())
                        .mapToObj(picker.directories.getModel()::getElementAt).toList());
                assertEquals(List.of("Up", "Cancel", "Select Folder"), components(picker).stream()
                        .filter(JButton.class::isInstance).map(JButton.class::cast).filter(button -> !button.getText().isEmpty())
                        .map(JButton::getText).toList());
                assertTrue(components(picker).stream().noneMatch(c -> c instanceof JTextField || c instanceof JComboBox<?> || c instanceof JFileChooser));
                assertTrue(picker.select.isEnabled()); assertEquals(directory, picker.currentDirectory());
            }
        });
    }
    @Test void platformHiddenDirectoryIsOmittedWhenSupported() throws Exception {
        Path hidden = Files.createDirectory(directory.resolve("platform-hidden"));
        Path ordinary = Files.createDirectory(directory.resolve("ordinary"));
        if (Files.getFileStore(hidden).supportsFileAttributeView("dos")) {
            Files.setAttribute(hidden, "dos:hidden", true);
        }
        boolean platformHidden = Files.isHidden(hidden);
        edt(() -> {
            try (var picker = new DirectoryPicker(directory.resolve("vault"), path -> { }, () -> { }, Runnable::run)) {
                var model = (DefaultListModel<Path>) picker.directories.getModel();
                assertTrue(model.contains(ordinary));
                assertEquals(!platformHidden, model.contains(hidden));
            }
        });
    }
    @Test void failedHiddenCheckConservativelyOmitsEntry() throws Exception {
        var unavailable = Files.createDirectory(directory.resolve("unavailable"));
        var ordinary = Files.createDirectory(directory.resolve("ordinary"));
        DirectoryPicker.HiddenCheck check = path -> {
            if (path.equals(unavailable)) { throw new java.io.IOException("Hidden attribute unavailable"); }
            return false;
        };
        assertEquals(List.of(ordinary), List.of(unavailable, ordinary).stream()
                .filter(path -> DirectoryPicker.visibleDirectory(path, check)).toList());
        assertFalse(DirectoryPicker.visibleDirectory(unavailable, path -> { throw new SecurityException(); }));
    }
    @Test void folderListUsesExpandingCenterAndSelectionChoosesHighlightedChildOrCurrentDirectory() throws Exception {
        Path child = Files.createDirectory(directory.resolve("child")); AtomicReference<Path> chosen = new AtomicReference<>();
        edt(() -> {
            try (var picker = new DirectoryPicker(directory.resolve("vault"), chosen::set, () -> { }, Runnable::run)) {
                picker.setSize(720, 520); picker.doLayout();
                assertSame(picker.scroll, ((BorderLayout) picker.getLayout()).getLayoutComponent(BorderLayout.CENTER));
                assertTrue(picker.scroll.getHeight() > picker.getHeight() * .65);
                picker.select.doClick(0); assertEquals(directory, chosen.get());
                picker.directories.setSelectedValue(child, true); picker.select.doClick(0);
                assertEquals(child, chosen.get()); assertEquals(directory, picker.currentDirectory());
                picker.enterSelected(); assertEquals(child, picker.currentDirectory());
                assertNull(picker.directories.getSelectedValue());
                picker.select.doClick(0); assertEquals(child, chosen.get());
                assertEquals(child.toString(), picker.location.getText());
            }
        });
    }
    @Test void enterAndUpNavigateWhileCancelDoesNotSelect() throws Exception {
        Path child = Files.createDirectory(directory.resolve("child")); AtomicReference<Path> chosen = new AtomicReference<>(); AtomicInteger cancels = new AtomicInteger();
        edt(() -> {
            try (var picker = new DirectoryPicker(directory.resolve("vault"), chosen::set, cancels::incrementAndGet, Runnable::run)) {
                picker.directories.setSelectedValue(child, true);
                Object key = picker.directories.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("ENTER"));
                picker.directories.getActionMap().get(key).actionPerformed(null); assertEquals(child, picker.currentDirectory());
                assertNull(chosen.get()); assertNull(picker.directories.getSelectedValue());
                picker.up.doClick(0); assertEquals(directory, picker.currentDirectory());
                assertNull(picker.directories.getSelectedValue());
                picker.cancel.doClick(0); assertEquals(1, cancels.get()); assertNull(chosen.get());
                assertNotNull(picker.directories.getInputMap().get(KeyStroke.getKeyStroke("DOWN")));
            }
        });
    }
    @Test void doubleClickEntersDirectory() throws Exception {
        Path child = Files.createDirectory(directory.resolve("child"));
        AtomicReference<Path> chosen = new AtomicReference<>();
        edt(() -> {
            try (var picker = new DirectoryPicker(directory.resolve("vault"), chosen::set, () -> { }, Runnable::run)) {
                picker.directories.setSize(600, 350); picker.directories.setSelectedIndex(0);
                var bounds = picker.directories.getCellBounds(0, 0);
                var click = new MouseEvent(picker.directories, MouseEvent.MOUSE_CLICKED, 0, 0, bounds.x + 2, bounds.y + 2, 2, false, MouseEvent.BUTTON1);
                for (var listener : picker.directories.getMouseListeners()) { listener.mouseClicked(click); }
                assertEquals(child, picker.currentDirectory());
                assertNull(chosen.get()); assertNull(picker.directories.getSelectedValue());
            }
        });
    }
    @Test void primaryStaysActivatableForEmptyNonVaultDirectory() throws Exception {
        AtomicReference<Path> chosen = new AtomicReference<>();
        edt(() -> {
            try (var picker = new DirectoryPicker(directory.resolve("vault"), chosen::set, () -> { }, Runnable::run)) {
                assertEquals(0, picker.directories.getModel().getSize()); assertTrue(picker.select.isEnabled());
                picker.select.doClick(0); assertEquals(directory, chosen.get());
            }
        });
    }
    @Test void navigationClearsChildSelectionImmediatelyAndAfterListing() throws Exception {
        Path child = Files.createDirectory(directory.resolve("child"));
        Files.createDirectory(child.resolve("grandchild"));
        List<Runnable> queued = new ArrayList<>();
        edt(() -> {
            try (var picker = new DirectoryPicker(directory.resolve("vault"), path -> { }, () -> { }, queued::add)) {
                queued.removeFirst().run(); picker.directories.setSelectedValue(child, true);
                picker.enterSelected(); assertNull(picker.directories.getSelectedValue());
                queued.removeFirst().run(); assertNull(picker.directories.getSelectedValue());
                assertEquals(child, picker.currentDirectory());
            }
        });
    }
    @Test void retiredPickerRejectsLateListingsAndActions() throws Exception {
        List<Runnable> queued = new ArrayList<>(); AtomicReference<Path> chosen = new AtomicReference<>();
        edt(() -> {
            var picker = new DirectoryPicker(directory.resolve("vault"), chosen::set, () -> { }, queued::add);
            picker.close(); queued.getFirst().run(); picker.select.doClick(0);
            assertNull(chosen.get()); assertNull(picker.currentDirectory()); assertEquals(0, picker.directories.getModel().getSize());
        });
    }
    @Test void olderNavigationResultCannotReplaceNewerDirectory() throws Exception {
        Path child = Files.createDirectory(directory.resolve("child")); List<Runnable> queued = new ArrayList<>();
        edt(() -> {
            try (var picker = new DirectoryPicker(directory.resolve("vault"), path -> { }, () -> { }, queued::add)) {
                picker.navigate(child); queued.getLast().run(); queued.getFirst().run(); assertEquals(child, picker.currentDirectory());
            }
        });
    }
}
