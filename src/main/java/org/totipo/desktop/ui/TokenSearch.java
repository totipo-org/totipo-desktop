package org.totipo.desktop.ui;

import org.totipo.TokenState;
import java.util.Locale;

/** Literal presentation-only matching; never chooses an alternative. */
final class TokenSearch {
    private TokenSearch() { }
    static boolean matches(TokenState token, String query) {
        var terms = java.util.Arrays.stream(query.toLowerCase(Locale.ROOT).split("(?U)\\s+"))
                .filter(term -> !term.isEmpty()).toList();
        return terms.isEmpty() || token.alternatives().stream().anyMatch(a -> {
            String issuer = UntrustedText.display(a.descriptor().issuer()).toLowerCase(Locale.ROOT);
            String account = UntrustedText.display(a.descriptor().account()).toLowerCase(Locale.ROOT);
            return terms.stream().allMatch(term -> issuer.contains(term) || account.contains(term));
        });
    }
}
