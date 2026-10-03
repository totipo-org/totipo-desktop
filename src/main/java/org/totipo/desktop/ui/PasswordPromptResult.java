package org.totipo.desktop.ui;

/** Presentation decision; submitted characters transfer to the application for clearing. */
public final class PasswordPromptResult implements AutoCloseable {
    public enum Action { SUBMIT, EXIT, CHANGE_VAULT, CANCEL }

    private final Action action;
    private char[] password;

    private PasswordPromptResult(Action action, char[] password) {
        this.action = action;
        this.password = password;
    }

    public static PasswordPromptResult submitted(char[] password) {
        return new PasswordPromptResult(Action.SUBMIT, java.util.Objects.requireNonNull(password));
    }

    public static PasswordPromptResult dismissed(Action action) {
        java.util.Objects.requireNonNull(action);
        if (action == Action.SUBMIT) { throw new IllegalArgumentException("Submission requires a password"); }
        return new PasswordPromptResult(action, null);
    }

    public Action action() { return action; }
    /** Transfer once to the immediate operation; the decision retains no array after handoff. */
    public char[] takePassword() {
        char[] owned = password;
        password = null;
        return owned;
    }

    /** Clear an abandoned submission that was never transferred to an operation. */
    @Override public void close() {
        if (password != null) { java.util.Arrays.fill(password, '\0'); password = null; }
    }
}
