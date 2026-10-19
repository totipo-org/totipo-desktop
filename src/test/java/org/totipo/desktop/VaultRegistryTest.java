package org.totipo.desktop;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class VaultRegistryTest {
    @TempDir Path directory;

    @Test void migrationIsIdempotentAndNewRegistryWinsOverLegacy() throws Exception {
        Preferences node = node();
        try {
            Path a = directory.resolve("a"), b = directory.resolve("b");
            node.put(JdkVaultPreferences.KEY, a.resolve("../a").toString());
            var registry = new VaultRegistry(new JdkVaultPreferences(node));
            assertEquals(List.of(a), registry.paths()); assertEquals(a, registry.selected().orElseThrow());
            assertNotNull(node.get(JdkVaultPreferences.REGISTRY_KEY, null));
            registry.select(b);
            var restarted = new VaultRegistry(new JdkVaultPreferences(node));
            assertEquals(List.of(a, b), restarted.paths()); assertEquals(b, restarted.selected().orElseThrow());
            assertEquals(List.of(a, b), new VaultRegistry(new JdkVaultPreferences(node)).paths());
            assertNotNull(node.get(JdkVaultPreferences.KEY, null));
        } finally { node.removeNode(); }
    }
    @Test void emptyRegistryDuplicatesOrderAndMissingPathsSurviveRestart() throws Exception {
        Preferences node = node();
        try {
            var registry = new VaultRegistry(new JdkVaultPreferences(node));
            assertTrue(registry.paths().isEmpty()); assertTrue(registry.selected().isEmpty());
            Path a = directory.resolve("a"), b = directory.resolve("b");
            registry.select(a); registry.select(b); registry.select(a.resolve("../a"));
            assertEquals(List.of(a, b), registry.paths());
            var restarted = new VaultRegistry(new JdkVaultPreferences(node));
            assertEquals(List.of(a, b), restarted.paths()); assertEquals(a, restarted.selected().orElseThrow());
            assertFalse(Files.exists(a)); assertFalse(Files.exists(b));
        } finally { node.removeNode(); }
    }
    @Test void forgetOnlyChangesRegistryAndDoesNotSelectAnotherVaultOrRemigrate() throws Exception {
        Preferences node = node();
        try {
            Path a = Files.createDirectory(directory.resolve("a"));
            Path marker = Files.writeString(a.resolve("vault"), "unchanged");
            node.put(JdkVaultPreferences.KEY, a.toString());
            var registry = new VaultRegistry(new JdkVaultPreferences(node));
            Path b = directory.resolve("b"); registry.select(b); registry.forget(a);
            assertEquals(b, registry.selected().orElseThrow());
            registry.forget(b);
            var restarted = new VaultRegistry(new JdkVaultPreferences(node));
            assertTrue(restarted.paths().isEmpty()); assertTrue(restarted.selected().isEmpty());
            assertEquals("unchanged", Files.readString(marker));
        } finally { node.removeNode(); }
    }
    @Test void forgettingSelectedRetainsOtherEntriesWithoutChoosingOne() throws Exception {
        Preferences node = node();
        try {
            var registry = new VaultRegistry(new JdkVaultPreferences(node));
            Path a = directory.resolve("a"), b = directory.resolve("b");
            registry.select(a); registry.select(b); registry.forget(b);
            var restarted = new VaultRegistry(new JdkVaultPreferences(node));
            assertEquals(List.of(a), restarted.paths()); assertTrue(restarted.selected().isEmpty());
        } finally { node.removeNode(); }
    }
    @Test void malformedMetadataRecoversLegacyWithoutFilesystemChanges() throws Exception {
        Preferences node = node();
        try {
            Path legacy = Files.writeString(directory.resolve("keep"), "preserve");
            node.put(JdkVaultPreferences.KEY, legacy.toString());
            for (String value : List.of("garbage", "2\n-1", "1\nno", "1\n8", "1\n0\n%%%", "1\n0\ncmVsYXRpdmU=", "1\n0\nAA==")) {
                node.put(JdkVaultPreferences.REGISTRY_KEY, value);
                var registry = new VaultRegistry(new JdkVaultPreferences(node));
                assertEquals(List.of(legacy), registry.paths()); assertEquals(legacy, registry.selected().orElseThrow());
                assertEquals("preserve", Files.readString(legacy));
            }
            node.remove(JdkVaultPreferences.KEY); node.put(JdkVaultPreferences.REGISTRY_KEY, "bad");
            assertTrue(new VaultRegistry(new JdkVaultPreferences(node)).paths().isEmpty());
        } finally { node.removeNode(); }
    }
    @Test void unavailablePreferenceStoreAndFailedMigrationSaveRemainUsable() {
        assertTrue(new VaultRegistry(new JdkVaultPreferences(null)).paths().isEmpty());
        Path legacy = directory.resolve("legacy");
        var failing = new java.util.prefs.AbstractPreferences(null, "") {
            protected void putSpi(String key, String value) { throw new SecurityException(); }
            protected String getSpi(String key) { return key.equals(JdkVaultPreferences.KEY) ? legacy.toString() : null; }
            protected void removeSpi(String key) { fail("Migration must retain legacy"); }
            protected void removeNodeSpi() { }
            protected String[] keysSpi() { return new String[] {JdkVaultPreferences.KEY}; }
            protected String[] childrenNamesSpi() { return new String[0]; }
            protected java.util.prefs.AbstractPreferences childSpi(String name) { throw new UnsupportedOperationException(); }
            protected void syncSpi() { }
            protected void flushSpi() { }
        };
        for (int i = 0; i < 2; i++) {
            var registry = new VaultRegistry(new JdkVaultPreferences(failing));
            assertEquals(List.of(legacy), registry.paths()); assertEquals(legacy, registry.selected().orElseThrow());
        }
    }
    @Test void normalizationDoesNotResolveSymlinks() throws Exception {
        Preferences node = node();
        try {
            Path real = Files.createDirectory(directory.resolve("real"));
            Path alias = Files.createSymbolicLink(directory.resolve("alias"), real);
            var registry = new VaultRegistry(new JdkVaultPreferences(node)); registry.select(real); registry.select(alias);
            assertEquals(List.of(real, alias), registry.paths());
        } finally { node.removeNode(); }
    }
    private Preferences node() { return Preferences.userRoot().node("/org/totipo/mv1-tests/" + UUID.randomUUID()); }
}
