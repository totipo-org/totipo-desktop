package org.totipo.desktop.ui;

import org.totipo.*;
import org.totipo.desktop.clipboard.TotpClipboard;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.Timer;

/**
 * EDT-owned reveal authorization, keyed by Token ID rather than row visibility.
 * Show Code authorizes the current period. An accepted result with strictly less than ten
 * seconds to its earliest expiry also authorizes exactly one immediately following period.
 * Search visibility never changes that authorization; no further automatic derivation is allowed.
 */
final class TotpDisplay {
    record Display(String label, String code, int remaining, long seconds, boolean urgent) { }
    private record Entry(String label, TotpCode code) { }
    private enum Grace { NONE, AUTHORIZED, CONSUMED }
    private record Request(Grace grace) { }
    private record GraceWindow(Instant from, Instant until) { }
    private record Reveal(VaultState base, TokenState token, List<Entry> entries, Grace grace) {
        TotpCode earliest() { return entries.stream().map(Entry::code).min(Comparator.comparing(TotpCode::validUntil)).orElseThrow(); }
    }
    private final Clock clock;
    private final BiConsumer<TokenId, List<Display>> render;
    private final Consumer<TokenId> unavailable;
    private final Timer timer;
    private final Map<TokenId, Reveal> entries = new LinkedHashMap<>();
    private final Map<TokenId, Request> requests = new HashMap<>();
    private VaultView.TotpAction generate = (base, alternatives, now, done) -> done.accept(List.of());

    TotpDisplay(Clock clock, BiConsumer<TokenId, List<Display>> render) {
        this(clock, render, id -> { });
    }
    TotpDisplay(Clock clock, BiConsumer<TokenId, List<Display>> render, Consumer<TokenId> unavailable) {
        this.clock = clock; this.render = render;
        this.unavailable = unavailable;
        timer = new Timer(250, event -> tick()); timer.setCoalesce(true);
    }
    void generator(VaultView.TotpAction action) { Edt.require(); generate = action; }

    void reveal(VaultState base, TokenState token) {
        Edt.require();
        requests.remove(token.id()); entries.remove(token.id());
        if (entries.isEmpty()) { timer.stop(); }
        request(base, token, null);
    }

    private void request(VaultState base, TokenState token, GraceWindow previous) {
        List<TokenAlternative> outcomes = codeAlternatives(token);
        Request request = new Request(previous == null ? Grace.NONE : Grace.CONSUMED); requests.put(token.id(), request);
        generate.generate(base, outcomes, clock.instant(), codes -> {
            Edt.require();
            if (requests.get(token.id()) != request) { return; }
            requests.remove(token.id());
            List<Entry> revealed = new ArrayList<>();
            // A partial result must never look like a single successful conflict outcome.
            if (codes.size() != outcomes.size() || codes.stream().anyMatch(Optional::isEmpty)) {
                render.accept(token.id(), List.of()); unavailable.accept(token.id()); return;
            }
            for (int i = 0; i < Math.min(codes.size(), outcomes.size()); i++) {
                String identity = TokenPresentation.identity(outcomes.get(i).descriptor());
                boolean duplicate = outcomes.stream().filter(a -> TokenPresentation.identity(a.descriptor()).equals(identity)).count() > 1;
                String finalLabel = duplicate || identity.isBlank() ? "Possible code " + (i + 1) : identity;
                codes.get(i).ifPresent(code -> revealed.add(new Entry(finalLabel, code)));
            }
            Instant accepted = clock.instant();
            if (!revealed.isEmpty() && revealed.stream().allMatch(e -> valid(e.code(), accepted))) {
                TotpCode earliest = revealed.stream().map(Entry::code).min(Comparator.comparing(TotpCode::validUntil)).orElseThrow();
                // Reject late initial results and grace results outside the immediately following window.
                // For mixed periods, unexpired outcomes may still be in their original interval.
                if (previous != null && (!accepted.isBefore(previous.until())
                        || revealed.stream().noneMatch(e -> e.code().validFrom().equals(previous.from())
                        && e.code().validUntil().equals(previous.until())))) {
                    render.accept(token.id(), List.of()); return;
                }
                Grace grace = request.grace() == Grace.CONSUMED ? Grace.CONSUMED
                        : Duration.between(accepted, earliest.validUntil()).compareTo(Duration.ofSeconds(10)) < 0
                        ? Grace.AUTHORIZED : Grace.NONE;
                entries.put(token.id(), new Reveal(base, token, List.copyOf(revealed), grace)); timer.start();
            }
            render.accept(token.id(), presentation(token.id()));
        });
    }

    private static Instant nextExpiry(TotpCode code) {
        return code.validUntil().plus(Duration.between(code.validFrom(), code.validUntil()));
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
            Reveal reveal = entries.get(id);
            if (reveal == null) { continue; }
            List<Entry> revealed = reveal.entries();
            // Mixed periods retire the entire token reveal at the earliest outcome expiry.
            if (revealed.stream().anyMatch(e -> !valid(e.code(), now))) {
                TotpCode earliest = reveal.earliest();
                // Consume before requesting: no retained expired Strings, timer retries, or per-Head requests.
                clear(id);
                if (reveal.grace() == Grace.AUTHORIZED && !now.isBefore(earliest.validUntil())
                        && now.isBefore(nextExpiry(earliest))) {
                    // The pending request captures only interval bounds, never the expired code String.
                    request(reveal.base(), reveal.token(), new GraceWindow(earliest.validUntil(), nextExpiry(earliest)));
                }
                continue;
            }
            render.accept(id, presentation(revealed, now));
        }
        if (entries.isEmpty()) { timer.stop(); }
    }
    /** Reconstruct a visible row without deriving, extending, or changing its authorization. */
    List<Display> presentation(TokenId id) {
        Edt.require(); Reveal reveal = entries.get(id); Instant now = clock.instant();
        return reveal == null || reveal.entries().stream().anyMatch(e -> !valid(e.code(), now))
                ? List.of() : presentation(reveal.entries(), now);
    }
    boolean pending(TokenId id) { Edt.require(); return requests.containsKey(id); }
    private static List<Display> presentation(List<Entry> revealed, Instant now) {
        return revealed.stream().map(e -> {
                double span = seconds(Duration.between(e.code().validFrom(), e.code().validUntil()));
                double left = seconds(Duration.between(now, e.code().validUntil()));
                return new Display(e.label(), e.code().code(), (int) Math.min(1000, 1000 * left / span),
                        (long) Math.ceil(left), left < 10);
            }).toList();
    }
    String copy(TokenId id, int index, TotpClipboard.Copy action) {
        Edt.require();
        Reveal reveal = entries.get(id);
        List<Entry> revealed = reveal == null ? null : reveal.entries();
        if (revealed == null || index < 0 || index >= revealed.size()) { return "Code unavailable; code was not copied."; }
        TotpCode code = revealed.get(index).code(); Instant now = clock.instant();
        if (revealed.stream().anyMatch(e -> !valid(e.code(), now))) {
            // Copy only validates/hides. The reveal timer alone may consume rollover authorization.
            render.accept(id, List.of()); return "Code unavailable; code was not copied.";
        }
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
