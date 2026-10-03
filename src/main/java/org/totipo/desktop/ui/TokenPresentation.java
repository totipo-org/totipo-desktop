package org.totipo.desktop.ui;

import org.totipo.*;
import java.util.List;
import java.util.stream.Collectors;

/** Text derived only from the public projection; labels have no semantic priority. */
final class TokenPresentation {
    private TokenPresentation() { }

    private static String visibleValues(CompetingField<String> field) {
        return field.values().stream().map(value -> UntrustedText.display(value.value()))
                .filter(value -> !value.isBlank()).collect(Collectors.joining(" / "));
    }

    static String label(int index) { return "Alternative " + (index + 1); }

    static String primary(TokenState token) {
        if (token.alternatives().isEmpty()) { return "Token unavailable"; }
        String issuer = visibleValues(token.competingValues().issuer());
        return issuer.isBlank() ? "Unnamed token" : issuer;
    }
    static String account(TokenState token) {
        return visibleValues(token.competingValues().account());
    }
    static String identity(TokenDescriptor d) {
        return UntrustedText.display(d.issuer()) + (d.account().isBlank() ? "" : " · " + UntrustedText.display(d.account()));
    }
    static String formattedCode(String code) {
        int middle = code.length() / 2;
        return code.substring(0, middle) + " " + code.substring(middle);
    }

    static String detail(TokenState token) {
        StringBuilder out = new StringBuilder("Token ID: " + token.id().hex() + "\n");
        out.append(token.hasConflict() ? "CONFLICT — distinct complete token values\n" : "No semantic conflict observed\n");
        List<TokenAlternative> alternatives = token.alternatives();
        out.append("Complete alternatives shown: ").append(alternatives.size()).append('\n');
        if (alternatives.isEmpty()) {
            out.append("No complete token value is currently available from the local observation.\n");
        } else if (!token.hasConflict() && alternatives.size() == 1 && token.heads().size() > 1) {
            out.append(token.heads().size()).append(" current causal heads carry the same token value.\n");
        }
        out.append("Alternative labels are for presentation only; they do not indicate preference.\n");
        for (int i = 0; i < alternatives.size(); i++) {
            TokenAlternative alternative = alternatives.get(i);
            TokenDescriptor d = alternative.descriptor();
            out.append('\n').append(label(i)).append('\n')
                    .append("Status: ").append(d.status()).append("\nIssuer: ").append(UntrustedText.display(d.issuer()))
                    .append("\nAccount: ").append(UntrustedText.display(d.account())).append("\nAlgorithm: ").append(d.algorithm())
                    .append("\nDigits: ").append(d.digits()).append("\nPeriod: ").append(d.period()).append('\n');
            out.append("  Heads (provenance):\n");
            alternative.heads().forEach(head -> head(out, head, "    "));
        }
        TokenCompetition c = token.competingValues();
        out.append("\nField competition\n");
        field(out, "Status", c.status(), alternatives);
        field(out, "Issuer", c.issuer(), alternatives);
        field(out, "Account", c.account(), alternatives);
        field(out, "Algorithm", c.algorithm(), alternatives);
        field(out, "Digits", c.digits(), alternatives);
        field(out, "Period", c.period(), alternatives);
        int groups = c.secret().groups().size();
        out.append("\nSecret equality\n");
        if (groups == 0) { out.append("No complete observed secret value.\n"); }
        else if (groups == 1) { out.append("All current alternatives use the same secret.\n"); }
        else { out.append("Current alternatives contain ").append(groups).append(" distinct secret values.\n"); }
        for (int i = 0; i < groups; i++) {
            out.append("Secret group ").append(i + 1).append(" — ")
                    .append(labels(c.secret().groups().get(i).alternatives(), alternatives)).append('\n');
        }
        for (TokenHead head : token.heads()) {
            boolean mapped = alternatives.stream().flatMap(a -> a.heads().stream())
                    .anyMatch(member -> member.revision().equals(head.revision()));
            if (!mapped) {
                out.append("\nCurrent Head not mapped to a complete Alternative by the API:\n");
                head(out, head, "  ");
            }
        }
        out.append("\nUnresolved causal references: ").append(token.unresolvedReferences().size()).append('\n');
        token.unresolvedReferences().forEach(ref -> out.append("Child: ").append(ref.child().hex())
                .append("\nReferenced parent: ").append(ref.parent().hex()).append('\n'));
        return out.toString();
    }

    private static void head(StringBuilder out, TokenHead head, String indent) {
        out.append(indent).append("Head ").append(head.revision().hex()).append('\n');
        head.metadata().clientName().ifPresent(name -> out.append(indent).append("Client-provided name: ")
                .append(UntrustedText.display(name)).append('\n'));
        head.metadata().clientTimeBits().ifPresent(bits -> out.append(indent).append("Client-provided raw unsigned time: ")
                .append(Long.toUnsignedString(bits)).append('\n'));
    }

    private static void field(StringBuilder out, String name, CompetingField<?> field,
                              List<TokenAlternative> all) {
        out.append(name).append(field.disagrees() ? " (competing):\n" : ":\n");
        if (field.values().isEmpty()) { out.append("No complete observed value\n"); }
        field.values().forEach(value -> out.append(UntrustedText.display(value.value().toString())).append(" — ")
                .append(labels(value.alternatives(), all)).append('\n'));
    }

    private static String labels(List<TokenAlternative> members, List<TokenAlternative> all) {
        return members.stream().map(member -> label(all.indexOf(member))).collect(Collectors.joining(", "));
    }
}
