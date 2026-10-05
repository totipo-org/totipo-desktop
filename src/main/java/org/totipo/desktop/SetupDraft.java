package org.totipo.desktop;

import org.totipo.*;
import java.util.Arrays;

/** Acquired setup owns new secret bytes; its descriptive review is deliberately secret-free. */
public final class SetupDraft implements AutoCloseable {
    private final TokenDescriptor review;
    private byte[] secret;
    public SetupDraft(TokenDescriptor review, byte[] secret) {
        this.review = java.util.Objects.requireNonNull(review);
        this.secret = java.util.Objects.requireNonNull(secret);
    }
    public TokenDescriptor review() { return review; }
    /** Transfers the complete setup, retaining the target's identity/lifecycle for an update. */
    public TokenDraft transfer(TokenDescriptor target) {
        if (secret == null) { throw new IllegalStateException("Setup retired"); }
        TokenDescriptor fields = target == null ? review : new TokenDescriptor(target.status(), target.issuer(),
                target.account(), review.algorithm(), review.digits(), review.period());
        byte[] owned = secret; secret = null;
        return new TokenDraft(fields, owned);
    }
    @Override public void close() { if (secret != null) { Arrays.fill(secret, (byte) 0); secret = null; } }
    @Override public String toString() { return "Acquired authenticator setup (secret omitted)"; }
}
