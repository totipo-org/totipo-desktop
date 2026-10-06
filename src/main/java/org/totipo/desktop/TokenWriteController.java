package org.totipo.desktop;

import org.totipo.*;
import org.totipo.desktop.ui.*;
import java.util.concurrent.Executor;
import javax.swing.SwingUtilities;

/** EDT workflow owner. The retry field is exclusively accessed by the session executor. */
final class TokenWriteController {
    private enum Outcome { SAVED, UNCERTAIN, FAILED, INTERNAL_FAILURE, INTERNAL_PREPARATION_FAILURE }
    private final Executor executor;
    private final VaultView view;
    private final MutationGate gate;
    private final Runnable closeSession;
    private TokenManagementPanel editor;
    private volatile VaultState current;
    private SaveResult.Saved savedResult;
    private final java.util.concurrent.atomic.AtomicReference<TokenDraft> queuedDraft = new java.util.concurrent.atomic.AtomicReference<>();
    private MergeEditorPanel mergeEditor;
    private boolean merge;
    private TokenId mergeId;
    private volatile MergeInputs mergeInputs;
    private final java.util.concurrent.atomic.AtomicReference<MergeDraft> queuedMerge = new java.util.concurrent.atomic.AtomicReference<>();
    private boolean active;
    private boolean pending;
    private volatile boolean closing;
    private boolean abandoned;
    private boolean create;
    private PublicationRetry retry; // Session executor only, including cleanup.

    TokenWriteController(Executor executor, VaultView view, Runnable closeSession, MutationGate gate) {
        this.executor = executor; this.view = view; this.closeSession = closeSession; this.gate = gate;
    }

    void open(VaultState base, TokenAlternative alternative, String explanation) {
        open(base, alternative, explanation, TokenManagementPanel.DeleteOrigin.EDIT);
    }
    private void open(VaultState base, TokenAlternative alternative, String explanation, TokenManagementPanel.DeleteOrigin origin) {
        Edt.require();
        if (closing || active || base == null || !gate.acquire(this)) { return; }
        active = true;
        merge = false;
        create = alternative == null;
        if (current == null) { current = base; }
        editor = new TokenManagementPanel(base, alternative, explanation, () -> current,
                this::submit, this::cancel, origin);
        view.manageToken(editor);
    }

    void current(VaultState state) {
        Edt.require(); current = state;
        if (mergeEditor != null && !sameConflict(state)) {
            MergeDraft abandonedDraft = queuedMerge.getAndSet(null);
            if (abandonedDraft != null) { abandonedDraft.close(); pending = false; }
            if (!pending) { changedConflict(); }
        }
    }
    void openDelete(VaultState base, TokenAlternative alternative, String explanation) {
        if (closing || active || alternative == null) { return; }
        open(base, alternative, explanation, TokenManagementPanel.DeleteOrigin.DIRECT);
    }
    private boolean sameConflict(VaultState state) {
        MergeInputs captured = mergeInputs;
        if (state == null || captured == null) { return false; }
        TokenState token = state.token(mergeId).orElse(null);
        return token != null && token.hasConflict() && token.unresolvedReferences().isEmpty()
                && token.alternatives().size() == captured.captured().size()
                && token.alternatives().containsAll(captured.captured());
    }
    private void changedConflict() {
        if (mergeEditor != null) { mergeEditor.changed(this::reviewCurrent); }
    }
    private void reviewCurrent() {
        Edt.require(); if (closing || pending || !active) { return; }
        VaultState base = current; TokenId id = mergeId;
        finish();
        TokenState token = base == null ? null : base.token(id).orElse(null);
        if (token != null && token.hasConflict() && token.alternatives().size() >= 2 && token.unresolvedReferences().isEmpty()) {
            openMerge(base, token);
        } else { view.writeWarning("The conflict changed. Review the current TOTP state.",
                "The conflict changed. Review the current TOTP state."); }
    }

    void openMerge(VaultState base, TokenState token) {
        Edt.require();
        if (closing || active || base == null || token == null || !token.hasConflict()
                || token.alternatives().size() < 2 || !token.unresolvedReferences().isEmpty() || !gate.acquire(this)) { return; }
        active = true; merge = true; create = false;
        mergeId = token.id(); current = base; mergeInputs = MergeInputs.capture(base, token);
        mergeEditor = new MergeEditorPanel(mergeInputs, this::submitMerge, this::cancel);
        view.editMerge(mergeEditor);
    }

    private void submitMerge(MergeDraft draft) {
        Edt.require();
        if (closing || pending || !active || !sameConflict(current)) {
            draft.close(); if (!closing && active && !pending) { changedConflict(); } return;
        }
        pending = true; queuedMerge.set(draft);
        executor.execute(() -> {
            MergeDraft owned = queuedMerge.getAndSet(null);
            if (owned == null) { return; }
            if (closing) { owned.close(); return; }
            if (!sameConflict(current)) {
                owned.close();
                SwingUtilities.invokeLater(() -> { if (!closing) { pending = false; changedConflict(); } }); return;
            }
            try {
                SaveResult result = MergeWrites.save(owned);
                if (result instanceof SaveResult.AdditionalConflict conflict) {
                    closePartialQuietly(conflict.resolution());
                    VaultState latest = conflict.latest();
                    SwingUtilities.invokeLater(() -> {
                        if (!closing) { pending = false; current = latest;
                            mergeEditor.changed("New conflict information appeared while you were resolving this conflict.\nNothing from this resolution was published.\nReview the updated conflict before continuing.", this::reviewCurrent); }
                    });
                } else { acceptMergeResult((PartialSaveResult) result); }
            } catch (MergeWrites.InconsistentDraft mismatch) {
                deliver(Outcome.INTERNAL_PREPARATION_FAILURE, null, "Internal resolution draft problem. Nothing was published.");
            } catch (RuntimeException unexpected) {
                deliver(Outcome.INTERNAL_FAILURE, null, "The resolution could not be completed; publication outcome could not be determined.");
            }
        });
    }
    private void acceptMergeResult(PartialSaveResult result) {
        if (result instanceof SaveResult.Saved saved) { savedResult = saved; deliver(Outcome.SAVED, null, null); }
        else if (result instanceof SaveResult.PublicationUncertain uncertain) {
            retry = uncertain.retry(); deliver(Outcome.UNCERTAIN, null, null);
        } else if (result instanceof SaveResult.Failed failed) { deliver(Outcome.FAILED, failed.reason(), null); }
    }
    private static boolean closePartialQuietly(PartialResolution owned) {
        try { if (owned != null) { owned.close(); } return true; }
        catch (RuntimeException cleanupFailure) {
            System.err.println("Totipo: discarded resolution cleanup failure (details redacted)."); return false;
        }
    }

    private void submit(VaultState base, TokenAlternative alternative, TokenDraft draft) {
        Edt.require();
        if (closing || pending || !active) { draft.close(); return; }
        create = alternative == null;
        pending = true;
        queuedDraft.set(draft);
        executor.execute(() -> {
            TokenDraft owned = queuedDraft.getAndSet(null);
            if (owned == null) { return; }
            if (closing) { owned.close(); return; }
            try {
                SaveResult result = TokenWrites.save(base, alternative, owned);
                if (result instanceof SaveResult.AdditionalConflict conflict) {
                    closePartialQuietly(conflict.resolution());
                    deliver(Outcome.INTERNAL_FAILURE, null, "Internal token operation failure.");
                } else if (result instanceof SaveResult.Saved saved) {
                    savedResult = saved;
                    deliver(Outcome.SAVED, null, null);
                } else if (result instanceof SaveResult.PublicationUncertain uncertain) {
                    retry = uncertain.retry();
                    deliver(Outcome.UNCERTAIN, null, null);
                } else if (result instanceof SaveResult.Failed failed) {
                    deliver(Outcome.FAILED, failed.reason(), null);
                } else {
                    throw new IllegalStateException("Unsupported save result");
                }
            } catch (RuntimeException unexpected) {
                deliver(Outcome.INTERNAL_FAILURE, null, "Internal token operation failure; publication outcome could not be determined.");
            }
        });
    }

    // Only classification reaches EDT; result capabilities stay on the executor.
    private void deliver(Outcome outcome, SaveResult.Reason reason, String error) {
        SwingUtilities.invokeLater(() -> {
            if (closing) { return; }
            pending = false;
            if (outcome == Outcome.SAVED) {
                finish();
                if (savedResult != null) { view.mutationAcknowledged(savedResult); savedResult = null; }
                view.writeMessage(merge ? "Resolution publication acknowledged."
                        : create ? "Token publication acknowledged." : "Token update publication acknowledged.");
            } else if (outcome == Outcome.UNCERTAIN) {
                retireEditor();
                showUncertainty(false);
            } else if (reason == SaveResult.Reason.SESSION_CLOSING) {
                closeSession.run();
            } else if (reason != null) {
                String message = switch (reason) {
                    case PREPARATION_FAILED -> "The token operation could not be prepared for publication. Nothing was published.";
                    case OBSERVATION_UNAVAILABLE -> "The required local observation was unavailable; this operation was not published.";
                    case UNRESOLVED_FIELDS -> "Required token fields were unresolved; this operation was not published.";
                    case SESSION_CLOSING -> throw new IllegalStateException("Handled above");
                };
                if (merge) { if (!sameConflict(current)) { changedConflict(); }
                    else { mergeEditor.busy(false, message + " Re-enter a new secret if required before saving."); } }
                else { editor.busy(false, message + " Re-enter a new secret if required before saving."); }
            } else {
                finish();
                view.writeWarning(error, outcome == Outcome.INTERNAL_PREPARATION_FAILURE
                        ? "The change could not be prepared. Nothing was saved."
                        : "The change could not be completed. It may already have been saved.");
            }
        });
    }

    private void showUncertainty(boolean busy) {
        if (merge) { view.mergePublicationUncertain(busy, this::retry, this::stop); }
        else { view.publicationUncertain(create, busy, this::retry, this::stop); }
    }

    void retry() {
        Edt.require();
        if (closing || pending || !active || editor != null || mergeEditor != null) { return; }
        pending = true;
        showUncertainty(true);
        executor.execute(() -> {
            PublicationRetry previous = retry;
            retry = null;
            try {
                RetryResult result;
                try {
                    result = previous.retryPublication();
                    if (result instanceof SaveResult.PublicationUncertain uncertain) { retry = uncertain.retry(); }
                }
                finally {
                    try { previous.close(); }
                    catch (RuntimeException cleanupFailure) {
                        // A consumed handle's cleanup must not overwrite the retry result
                        // or discard its independently owned successor capability.
                        System.err.println("Totipo: retry capability cleanup failure (details redacted).");
                    }
                }
                if (result instanceof SaveResult.PublicationUncertain) {
                    deliver(Outcome.UNCERTAIN, null, null);
                } else if (result instanceof SaveResult.Saved saved) {
                    savedResult = saved;
                    deliver(Outcome.SAVED, null, null);
                } else {
                    throw new IllegalStateException("Unsupported retry result");
                }
            } catch (RuntimeException unexpected) {
                cleanupQuietly();
                SwingUtilities.invokeLater(() -> {
                    if (!closing) {
                        abandoned = true;
                        finish();
                        view.writeWarning("Internal retry operation failure. The earlier publication remains uncertain.",
                                "The retry could not be completed. The earlier change may already have been saved.");
                    }
                });
            }
        });
    }

    void stop() {
        Edt.require();
        if (closing || pending || !active || editor != null || mergeEditor != null) { return; }
        pending = true;
        showUncertainty(true);
        executor.execute(() -> {
            boolean failed = false;
            try { cleanup(); } catch (RuntimeException unexpected) { failed = true; }
            boolean cleanupFailed = failed;
            SwingUtilities.invokeLater(() -> {
                if (!closing) {
                    abandoned = true;
                    finish();
                    if (cleanupFailed) { view.writeWarning("Internal operation cleanup failure; publication remains uncertain.",
                            "The save operation could not be closed cleanly. The change may already have been saved."); }
                }
            });
        });
    }

    private void cancel() {
        if (!closing && !pending) { finish(); }
    }
    private void retireEditor() {
        if (editor != null) { editor.retire(); editor = null; view.retireEditor(); }
        if (mergeEditor != null) { mergeEditor.retire(); mergeEditor = null; view.retireEditor(); }
    }
    private void finish() {
        retireEditor(); active = false; pending = false; mergeInputs = null;
        view.clearUncertainty();
        view.abandonedPublication(abandoned);
        gate.release(this);
    }
    void closing() {
        Edt.require(); closing = true;
        MergeDraft abandonedMerge = queuedMerge.getAndSet(null);
        if (abandonedMerge != null) { abandonedMerge.close(); }
        TokenDraft abandonedDraft = queuedDraft.getAndSet(null);
        if (abandonedDraft != null) { abandonedDraft.close(); }
        retireEditor(); active = false; pending = false; mergeInputs = null; current = null;
        view.clearUncertainty(); view.writeAvailability(false);
        executor.execute(this::cleanupQuietly);
    }
    private void cleanupQuietly() {
        try { cleanup(); }
        catch (RuntimeException unexpected) {
            System.err.println("Totipo: operation cleanup failure (details redacted).");
        }
    }
    private void cleanup() {
        PublicationRetry owned = retry; retry = null;
        if (owned != null) { owned.close(); }
    }
}
