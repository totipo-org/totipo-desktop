package org.totipo.desktop;

/** Persistent navigation states; operation failures do not introduce a global read-only mode. */
public enum ShellState { NO_VAULT, LOCKED, UNLOCKED, BLOCKING_VAULT_STATE }
