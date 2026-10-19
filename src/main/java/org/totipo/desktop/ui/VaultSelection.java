package org.totipo.desktop.ui;

import java.nio.file.Path;

/** Explicit shell choice; a path grants no session authority. */
public record VaultSelection(Action action, Path path) {
    public enum Action { SELECT, BROWSE, CREATE, FORGET, RETURN }
    public static VaultSelection select(Path path) {
        return new VaultSelection(Action.SELECT, java.util.Objects.requireNonNull(path));
    }
}
