package org.totipo.desktop.ui;

import org.totipo.*;
import org.totipo.desktop.clipboard.TotpClipboard;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import javax.swing.Timer;

/** EDT-owned derived codes only. One timer for all explicit reveals; ticks never derive. */
final class TotpDisplay {
    record Display(String label, String code, int remaining, long seconds, boolean urgent) { }
    private record Entry(String label, TotpCode code) { }
    private final Clock clock;
    private final BiConsumer<TokenId, List<Display>> render;
    private final Timer timer;
    private final Map<TokenId, List<Entry>> entries = new LinkedHashMap<>();
    private final Map<TokenId, Object> requests = new HashMap<>();
    private VaultView.TotpAction generate = (base, alternatives, now, done) -> done.accept(List.of());

    TotpDisplay(Clock clock, BiConsumer<TokenId, List<Display>> render) {
        this.clock = clock; this.render = render;
        timer = new Timer(250, event -> tick()); timer.setCoalesce(true);
    }
    void generator(VaultView.TotpAction action) { Edt.require(); generate = action; }

    void reveal(VaultState base, TokenState token) {
        Edt.require();
        requests.remove(token.id()); entries.remove(token.id());
        if (entries.isEmpty()) { timer.stop(); }
        List<TokenAlternative> outcomes = codeAlternatives(token);
        Object request = new Object(); requests.put(token.id(), request);
        generate.generate(base, outcomes, clock.instant(), codes -> {
            Edt.require();
            if (requests.get(token.id()) != request) { return; }
            requests.remove(token.id());
            List<Entry> revealed = new ArrayList<>();
            // A partial result must never look like a single successful conflict outcome.
            if (codes.size() != outcomes.size() || codes.stream().anyMatch(Optional::isEmpty)) {
                render.accept(token.id(), List.of()); return;
            }
            for (int i = 0; i < Math.min(codes.size(), outcomes.size()); i++) {
                String identity = TokenPresentation.identity(outcomes.get(i).descriptor());
                boolean duplicate = outcomes.stream().filter(a -> TokenPresentation.identity(a.descriptor()).equals(identity)).count() > 1;
                String finalLabel = duplicate || identity.isBlank() ? "Possible code " + (i + 1) : identity;
                codes.get(i).ifPresent(code -> revealed.add(new Entry(finalLabel, code)));
            }
            if (!revealed.isEmpty()) { entries.put(token.id(), List.copyOf(revealed)); timer.start(); }
            tick();
            if (revealed.isEmpty()) { render.accept(token.id(), List.of()); }
        });
    }

    /** Public secret-equality groups plus descriptor fields prove code-producing equality. */
    static List<TokenAlternative> codeAlternatives(TokenState token) {
        List<TokenAlternative> result = new ArrayList<>();
        for (TokenAlternative candidate : token.alternatives()) {
            if (candidate.descriptor().status() != TokenStatus.ACTIVE) { continue; }
            boolean same = result.stream().anyMatch(previous -> {
                TokenDescriptor a = previous.descriptor(), b = candidate.descriptor();
                return a.algorithm() == b.algorithm() && a.digits() == b.digits() && a.period().equals(b.period())
                        && token.competingValues().secret().groups().stream().anyMatch(group ->
                        group.alternatives().contains(previous) && group.alternatives().contains(candidate));
            });
            if (!same) { result.add(candidate); }
        }
        return List.copyOf(result);
    }
    void tick() {
        Edt.require();
        Instant now = clock.instant();
        for (TokenId id : List.copyOf(entries.keySet())) {
            List<Entry> revealed = entries.get(id);
            // Mixed periods retire the entire token reveal at the earliest outcome expiry.
            if (revealed.stream().anyMatch(e -> !valid(e.code(), now))) { clear(id); continue; }
            render.accept(id, revealed.stream().map(e -> {
                double span = seconds(Duration.between(e.code().validFrom(), e.code().validUntil()));
                double left = seconds(Duration.between(now, e.code().validUntil()));
                return new Display(e.label(), e.code().code(), (int) Math.min(1000, 1000 * left / span),
                        (long) Math.ceil(left), left < 10);
            }).toList());
        }
        if (entries.isEmpty()) { timer.stop(); }
    }
    String copy(TokenId id, int index, TotpClipboard.Copy action) {
        Edt.require(); tick();
        List<Entry> revealed = entries.get(id);
        if (revealed == null || index < 0 || index >= revealed.size()) { return "Code unavailable; code was not copied."; }
        TotpCode code = revealed.get(index).code(); Instant now = clock.instant();
        if (!valid(code, now)) { clear(id); return "Code unavailable; code was not copied."; }
        return action.copy(code.code(), code.validFrom(), code.validUntil(), now);
    }
    private static boolean valid(TotpCode code, Instant now) {
        return !now.isBefore(code.validFrom()) && now.isBefore(code.validUntil());
    }
    private static double seconds(Duration d) { return d.getSeconds() + d.getNano() / 1e9; }
    void clear(TokenId id) {
        Edt.require(); requests.remove(id); entries.remove(id); render.accept(id, List.of());
        if (entries.isEmpty()) { timer.stop(); }
    }
    void clear() {
        Edt.require(); requests.clear();
        for (TokenId id : List.copyOf(entries.keySet())) { clear(id); }
        timer.stop();
    }
    boolean running() { return timer.isRunning(); }
}
