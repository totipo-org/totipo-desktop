package org.totipo.desktop;

import org.totipo.*;
import java.util.Objects;

/** Submitted desktop values; owns caller secret ingress, never a core capability. */
public final class MergeDraft implements AutoCloseable {
    private final MergeInputs inputs;
    private final TokenAlternative secretRepresentative;
    private final TokenDraft values;
    private final TokenAlternative wholeVersion;

    /** Whole-version intent stays opaque: only Java keep() transfers its semantic value. */
    public static MergeDraft keep(MergeInputs inputs, TokenAlternative alternative) {
        return new MergeDraft(inputs, alternative);
    }
    private MergeDraft(MergeInputs inputs, TokenAlternative alternative) {
        this.inputs = Objects.requireNonNull(inputs);
        if (!inputs.captured().contains(Objects.requireNonNull(alternative)) || !inputs.fullFrontier()) {
            throw new IllegalArgumentException("Choose a captured version of the complete conflict.");
        }
        wholeVersion = alternative; secretRepresentative = null; values = null;
    }

    public MergeDraft(MergeInputs inputs, TokenDescriptor fields, TokenAlternative representative, byte[] newSecret) {
        this.inputs = Objects.requireNonNull(inputs);
        if ((representative == null) == (newSecret == null)
                || (representative != null && !inputs.selected().contains(representative))) {
            if (newSecret != null) { java.util.Arrays.fill(newSecret, (byte) 0); }
            throw new IllegalArgumentException("Choose an existing secret group or enter a new secret.");
        }
        secretRepresentative = representative;
        wholeVersion = null;
        values = new TokenDraft(fields, newSecret);
    }
    public MergeDraft(MergeInputs inputs, TokenDraft values) {
        this.inputs = Objects.requireNonNull(inputs);
        this.values = Objects.requireNonNull(values);
        secretRepresentative = null; wholeVersion = null;
    }
    MergeInputs inputs() { return inputs; }
    void apply(MergeToken builder) {
        if (wholeVersion != null) { builder.keep(wholeVersion); return; }
        // Builder-issued descriptions/capabilities stay within this executor operation.
        Objects.requireNonNull(builder.competingValues());
        var choices = builder.secretChoices();
        builder.unresolvedFields();
        if (secretRepresentative != null) {
            var matches = choices.stream().filter(choice -> choice.alternatives().contains(secretRepresentative)).toList();
            if (matches.size() != 1) { throw new MergeWrites.InconsistentDraft(); }
            builder.secret(matches.get(0));
        }
        values.apply(builder);
        if (!builder.unresolvedFields().isEmpty()) { throw new MergeWrites.InconsistentDraft(); }
    }
    @Override public void close() { if (values != null) { values.close(); } }
}
