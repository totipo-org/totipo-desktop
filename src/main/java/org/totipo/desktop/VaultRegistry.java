package org.totipo.desktop;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Installation-local selection history, never authority to use a session. EDT owned. */
public final class VaultRegistry {
    public record Snapshot(List<Path> paths, Path selected) {
        public Snapshot {
            paths = paths.stream().map(VaultRegistry::normalize).distinct().toList();
            selected = selected == null ? null : normalize(selected);
            if (selected != null && !paths.contains(selected)) { throw new IllegalArgumentException("Unknown selection"); }
        }
    }
    private final VaultPreferences preferences;
    private final List<Path> paths = new ArrayList<>();
    private Path selected;

    VaultRegistry(VaultPreferences preferences) {
        this.preferences = preferences;
        Snapshot saved = preferences.loadRegistry();
        paths.addAll(saved.paths()); selected = saved.selected();
    }
    static Path normalize(Path path) { return path.toAbsolutePath().normalize(); }
    public List<Path> paths() { return List.copyOf(paths); }
    public Optional<Path> selected() { return Optional.ofNullable(selected); }
    public boolean contains(Path path) { return paths.contains(normalize(path)); }
    void select(Path path) {
        selected = normalize(path);
        if (!paths.contains(selected)) { paths.add(selected); }
        save();
    }
    void forget(Path path) {
        Path normalized = normalize(path);
        paths.remove(normalized);
        if (normalized.equals(selected)) { selected = null; }
        save();
    }
    private void save() { preferences.saveRegistry(new Snapshot(paths, selected)); }
}
