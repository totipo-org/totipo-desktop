package org.totipo.desktop;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.prefs.Preferences;

/** Best-effort user preference; unavailable preferences must not prevent opening a vault. */
final class JdkVaultPreferences implements VaultPreferences {
    static final String NODE = "/org/totipo/desktop";
    static final String KEY = "lastVault";
    static final String REGISTRY_KEY = "vaultRegistryV1";
    private final Preferences store;

    JdkVaultPreferences() { this(userStore()); }
    private static Preferences userStore() {
        try { return Preferences.userRoot().node(NODE); }
        catch (SecurityException unavailable) { return null; }
    }
    JdkVaultPreferences(Preferences store) { this.store = store; }

    @Override public VaultRegistry.Snapshot loadRegistry() {
        if (store != null) {
            try {
                String encoded = store.get(REGISTRY_KEY, null);
                if (encoded != null) {
                    String[] lines = encoded.split("\n", -1);
                    if (lines.length < 2 || !lines[0].equals("1")) { throw new IllegalArgumentException(); }
                    int selection = Integer.parseInt(lines[1]);
                    var paths = new java.util.ArrayList<Path>();
                    for (int i = 2; i < lines.length; i++) {
                        String value = java.nio.charset.StandardCharsets.UTF_8.newDecoder().decode(
                                java.nio.ByteBuffer.wrap(java.util.Base64.getDecoder().decode(lines[i]))).toString();
                        Path path = Path.of(value);
                        if (value.isEmpty() || !path.isAbsolute()) { throw new IllegalArgumentException(); }
                        paths.add(path);
                    }
                    if (selection < -1 || selection >= paths.size()) { throw new IllegalArgumentException(); }
                    return new VaultRegistry.Snapshot(paths, selection == -1 ? null : paths.get(selection));
                }
            } catch (SecurityException | IllegalArgumentException | java.nio.charset.CharacterCodingException unusable) { /* Recover legacy without touching vault files. */ }
        }
        var migrated = VaultPreferences.super.loadRegistry();
        saveRegistry(migrated); // Never remove legacy, including when this save fails.
        return migrated;
    }
    @Override public void saveRegistry(VaultRegistry.Snapshot snapshot) {
        if (store == null) { return; }
        StringBuilder encoded = new StringBuilder("1\n").append(snapshot.paths().indexOf(snapshot.selected()));
        for (Path path : snapshot.paths()) {
            encoded.append('\n').append(java.util.Base64.getEncoder().encodeToString(
                    path.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }
        try { store.put(REGISTRY_KEY, encoded.toString()); store.flush(); }
        catch (SecurityException | IllegalArgumentException | java.util.prefs.BackingStoreException unavailable) { /* Optional local history. */ }
    }

    @Override public Optional<Path> lastVault() {
        if (store == null) { return Optional.empty(); }
        try {
            String value = store.get(KEY, null);
            return value == null ? Optional.empty() : Optional.of(Path.of(value).toAbsolutePath().normalize());
        } catch (InvalidPathException invalid) {
            clearLastVault();
            return Optional.empty();
        } catch (SecurityException unavailable) {
            return Optional.empty();
        }
    }
    @Override public void setLastVault(Path path) {
        if (store == null) { return; }
        try { store.put(KEY, path.toAbsolutePath().normalize().toString()); store.flush(); }
        catch (SecurityException | IllegalArgumentException | java.util.prefs.BackingStoreException unavailable) { /* Optional desktop convenience. */ }
    }
    @Override public void clearLastVault() {
        if (store == null) { return; }
        try { store.remove(KEY); }
        catch (SecurityException unavailable) { /* Optional desktop convenience. */ }
    }
}
