package org.totipo.desktop;

import java.nio.file.Path;
import java.util.Optional;

/** Desktop-only persistence. Never stores credentials or vault data. */
public interface VaultPreferences {
    default VaultRegistry.Snapshot loadRegistry() {
        Path legacy = lastVault().orElse(null);
        return new VaultRegistry.Snapshot(legacy == null ? java.util.List.of() : java.util.List.of(legacy), legacy);
    }
    default void saveRegistry(VaultRegistry.Snapshot snapshot) {
        if (snapshot.selected() == null) { clearLastVault(); }
        else { setLastVault(snapshot.selected()); }
    }
    /** Legacy location, retained as a migration fallback. */
    Optional<Path> lastVault();
    void setLastVault(Path path);
    void clearLastVault();
}
