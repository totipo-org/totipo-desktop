package org.totipo.desktop;

/** Persistent navigation states; operation failures do not introduce a global read-only mode. */
public enum ShellState { NO_VAULT, VAULT_SELECTION, LOCKED, UNLOCKED, BLOCKING_VAULT_STATE }
