package org.totipo.desktop;

import org.totipo.*;
import java.util.List;

/** Exact visible identity matches; never examines secret groups, heads, codes or timestamps. */
public final class IdentityMatches {
    public record Match(TokenId id, TokenAlternative alternative, boolean conflict) { }
    private IdentityMatches() { }
    public static List<Match> find(VaultState state, TokenDescriptor draft) {
        return state.tokens().stream().flatMap(token -> token.alternatives().stream()
                .filter(a -> a.descriptor().status() == TokenStatus.ACTIVE
                        && a.descriptor().issuer().equals(draft.issuer())
                        && a.descriptor().account().equals(draft.account()))
                .map(a -> new Match(token.id(), a, token.hasConflict()))).toList();
    }
}
