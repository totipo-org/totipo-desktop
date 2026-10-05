package org.totipo.desktop.ui;

import java.nio.file.Path;
import java.util.function.Consumer;
import org.totipo.desktop.ShellState;

/** One persistent application window; chooser/create dialogs may overlay it. EDT only. */
public interface ShellView {
    void actions(Runnable select, Runnable create, Runnable exit);
    void openAction(Consumer<char[]> open);
    void lockAction(Runnable lock);
    void retryAction(Runnable retry);
    void renderShell(ShellState state, Path selected, String message, boolean busy);
    Path chooseDirectory(Path initialLocation, boolean create);
    PasswordPromptResult password(Path directory, boolean create, PasswordPromptContext context);
    boolean confirmEmptyPassword();
    void busy(String status, boolean busy);
    void message(String title, String text);
    void showWindow();
    void retireDialogs();
    void dispose();
}
