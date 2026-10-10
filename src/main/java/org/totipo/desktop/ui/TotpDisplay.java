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
 * EDT-owned reveal authorization independent of row visibility. The browser gives each
 * conflicting semantic Alternative its own owner; Token IDs identify entries within an owner.
 * Show Code authorizes the current period. An accepted result with strictly less than ten
 * seconds to its earliest expiry also authorizes exactly one immediately following period.
 * Search visibility never changes that authorization; no further automatic derivation is allowed.
 */
final class TotpDisplay {
    record Display(String label, String code, int remaining, long seconds, boolean urgent) { }
    private record Entry(String label, TotpCode code) { }
    private enum Grace { NONE, AUTHORIZED, CONSUMED }
    private record Request(GraceWindow window, List<String> labels, List<TokenAlternative> alternatives) { }
    private record GraceWindow(Instant from, Instant until) { }
    private record Reveal(VaultState base, TokenState token, List<TokenAlternative> alternatives, List<Entry> entries, Grace grace) {
        TotpCode earliest() { return entries.stream().map(Entry::code).min(Comparator.comparing(TotpCode::validUntil)).orElseThrow(); }
    }
    private final Clock clock;
    private final BiConsumer<TokenId, List<Display>> render;
    private final Consumer<TokenId> unavailable;
    private final Timer timer;
    private final boolean automatic;
    private final Map<TokenId, Reveal> entries = new LinkedHashMap<>();
    private final Map<TokenId, Request> requests = new HashMap<>();
    private final Map<TokenId, Reveal> staged = new HashMap<>();
    private VaultView.TotpAction generate = (base, alternatives, now, done) -> done.accept(List.of());

    TotpDisplay(Clock clock, BiConsumer<TokenId, List<Display>> render) {
        this(clock, render, id -> { });
    }
    TotpDisplay(Clock clock, BiConsumer<TokenId, List<Display>> render, Consumer<TokenId> unavailable) {
        this(clock, render, unavailable, true);
    }
    /** Browser-owned displays disable the local cadence; authorization remains local. */
    TotpDisplay(Clock clock, BiConsumer<TokenId, List<Display>> render, Consumer<TokenId> unavailable, boolean automatic) {
        this.automatic = automatic;
        this.clock = clock; this.render = render;
        this.unavailable = unavailable;
        timer = new Timer(250, event -> tick()); timer.setCoalesce(true);
    }
    void generator(VaultView.TotpAction action) { Edt.require(); generate = action; }

    void reveal(VaultState base, TokenState token) {
        reveal(base, token, codeAlternatives(token));
    }
    void reveal(VaultState base, TokenState token, TokenAlternative alternative) {
        reveal(base, token, List.of(alternative));
    }
    private void reveal(VaultState base, TokenState token, List<TokenAlternative> outcomes) {
        Edt.require();
        requests.remove(token.id()); entries.remove(token.id()); staged.remove(token.id());
        if (entries.isEmpty() && requests.values().stream().noneMatch(r -> r.window() != null)) { timer.stop(); }
        request(base, token, outcomes);
    }

    private void request(VaultState base, TokenState token, List<TokenAlternative> outcomes) {
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < outcomes.size(); i++) {
            String identity = TokenPresentation.identity(outcomes.get(i).descriptor());
            boolean duplicate = outcomes.stream().filter(a -> TokenPresentation.identity(a.descriptor()).equals(identity)).count() > 1;
            labels.add(duplicate || identity.isBlank() ? "Possible code " + (i + 1) : identity);
        }
        Request request = new Request(null, List.copyOf(labels), List.copyOf(outcomes));
        requests.put(token.id(), request);
        generate.generate(base, outcomes, clock.instant(), codes -> {
            Edt.require();
            if (requests.get(token.id()) != request) { return; }
            requests.remove(token.id());
            if (entries.isEmpty() && requests.values().stream().noneMatch(r -> r.window() != null)) { timer.stop(); }
            List<Entry> revealed = new ArrayList<>();
            // A partial result must never look like a single successful conflict outcome.
            if (codes.size() != outcomes.size() || codes.stream().anyMatch(Optional::isEmpty)) {
                render.accept(token.id(), List.of()); unavailable.accept(token.id()); return;
            }
            for (int i = 0; i < Math.min(codes.size(), outcomes.size()); i++) {
                String finalLabel = labels.get(i);
                codes.get(i).ifPresent(code -> revealed.add(new Entry(finalLabel, code)));
            }
            Instant accepted = clock.instant();
            if (!revealed.isEmpty() && revealed.stream().allMatch(e -> valid(e.code(), accepted))) {
                TotpCode earliest = revealed.stream().map(Entry::code).min(Comparator.comparing(TotpCode::validUntil)).orElseThrow();
                Grace grace = Duration.between(accepted, earliest.validUntil()).compareTo(Duration.ofSeconds(10)) < 0
                        ? Grace.AUTHORIZED : Grace.NONE;
                Reveal reveal = new Reveal(base, token, request.alternatives(), List.copyOf(revealed), grace);
                entries.put(token.id(), reveal); if (automatic) { timer.start(); }
                render.accept(token.id(), presentation(revealed, accepted));
                if (grace == Grace.AUTHORIZED && entries.get(token.id()) == reveal) { stage(reveal, outcomes); }
                return;
            }
            render.accept(token.id(), presentation(token.id()));
        });
    }

    private void stage(Reveal reveal, List<TokenAlternative> outcomes) {
        TokenId id = reveal.token().id();
        Instant target = reveal.earliest().validUntil();
        // Bounds only: the callback never retains the previous code material.
        List<GraceWindow> expected = reveal.entries().stream().map(e -> {
            TotpCode code = e.code();
            return valid(code, target) ? new GraceWindow(code.validFrom(), code.validUntil())
                    : new GraceWindow(code.validUntil(), nextExpiry(code));
        }).toList();
        GraceWindow window = new GraceWindow(target, expected.stream().map(GraceWindow::until).min(Comparator.naturalOrder()).orElseThrow());
        List<String> labels = reveal.entries().stream().map(Entry::label).toList();
        VaultState base = reveal.base(); TokenState token = reveal.token();
        Request request = new Request(window, labels, List.copyOf(outcomes)); requests.put(id, request);
        if (!clock.instant().isBefore(target)) { render.accept(id, presentation(id)); }
        if (requests.get(id) != request) { return; }
        generate.generate(base, outcomes, target, codes -> {
            Edt.require();
            if (requests.get(id) != request) { return; }
            requests.remove(id);
            Instant accepted = clock.instant(); Reveal current = entries.get(id);
            boolean acceptable = codes.size() == expected.size() && accepted.isBefore(window.until())
                    && (!accepted.isBefore(target) || current != null && current.entries().stream().allMatch(e -> valid(e.code(), accepted)));
            for (int i = 0; acceptable && i < codes.size(); i++) {
                TotpCode code = codes.get(i).orElse(null); GraceWindow bounds = expected.get(i);
                acceptable = code != null && code.validFrom().equals(bounds.from()) && code.validUntil().equals(bounds.until());
            }
            if (acceptable) {
                List<Entry> next = new ArrayList<>();
                for (int i = 0; i < codes.size(); i++) { next.add(new Entry(labels.get(i), codes.get(i).orElseThrow())); }
                Reveal promoted = new Reveal(base, token, request.alternatives(), List.copyOf(next), Grace.CONSUMED);
                if (!accepted.isBefore(window.from())) { entries.put(id, promoted); staged.remove(id); }
                else { staged.put(id, promoted); }
            } else {
                if (current != null) { entries.put(id, new Reveal(base, token, current.alternatives(), current.entries(), Grace.NONE)); }
                unavailable.accept(id);
            }
            render.accept(id, presentation(id));
            tick();
        });
    }

    /** Retire expired material and promote a complete batch without an intermediate render. */
    private void advance(TokenId id, Instant now) {
        Reveal current = entries.get(id);
        if (current == null) {
            Request pending = requests.get(id);
            if (pending != null && pending.window() != null && now.isBefore(pending.window().from())) { requests.remove(id); }
            return;
        }
        if (current.entries().stream().allMatch(e -> valid(e.code(), now))) { return; }
        entries.remove(id);
        Reveal next = staged.remove(id);
        if (current.grace() == Grace.AUTHORIZED && next != null && next.entries().stream().allMatch(e -> valid(e.code(), now))) {
            entries.put(id, next);
        } else if (now.isBefore(current.earliest().validUntil())) {
            requests.remove(id); // A backwards clock invalidates the authorization too.
        }
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
        tick(clock.instant());
    }
    void tick(Instant now) {
        Edt.require();
        Set<TokenId> ids = new LinkedHashSet<>(entries.keySet()); ids.addAll(staged.keySet());
        requests.forEach((id, request) -> { if (request.window() != null) { ids.add(id); } });
        for (TokenId id : ids) { advance(id, now); render.accept(id, presentation(id, now)); }
        for (TokenId id : List.copyOf(requests.keySet())) {
            Request pending = requests.get(id);
            if (pending.window() != null && !now.isBefore(pending.window().until())) {
                clear(id);
            }
        }
        if (entries.isEmpty() && requests.values().stream().noneMatch(r -> r.window() != null)) { timer.stop(); }
    }
    /** Reconstruct a visible row without deriving, extending, or changing its authorization. */
    List<Display> presentation(TokenId id) {
        return presentation(id, clock.instant());
    }
    List<Display> presentation(TokenId id, Instant now) {
        Edt.require(); advance(id, now); Reveal reveal = entries.get(id);
        return reveal == null || reveal.entries().stream().anyMatch(e -> !valid(e.code(), now))
                ? List.of() : presentation(reveal.entries(), now);
    }
    boolean pending(TokenId id) {
        return pending(id, clock.instant());
    }
    boolean pending(TokenId id, Instant now) {
        Edt.require(); Request request = requests.get(id);
        return request != null && (request.window() == null || inWindow(request.window(), now));
    }
    private boolean inWindow(GraceWindow window, Instant now) {
        return !now.isBefore(window.from()) && now.isBefore(window.until());
    }
    /** Non-secret pending geometry, including expiry observed just before the timer tick. */
    List<String> graceLabels(TokenId id) {
        return graceLabels(id, clock.instant());
    }
    List<String> graceLabels(TokenId id, Instant now) {
        Edt.require();
        Request request = requests.get(id);
        if (request != null && request.window() != null && inWindow(request.window(), now)) { return request.labels(); }
        return List.of();
    }
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
            // This activation cannot copy expired material; presentation may promote an already staged batch.
            render.accept(id, presentation(id)); return "Code unavailable; code was not copied.";
        }
        return action.copy(code.code(), code.validFrom(), code.validUntil(), now);
    }
    private static boolean valid(TotpCode code, Instant now) {
        return !now.isBefore(code.validFrom()) && now.isBefore(code.validUntil());
    }
    private static double seconds(Duration d) { return d.getSeconds() + d.getNano() / 1e9; }
    /** Desktop policy, not Java validity: retain ordinary authorization only for the
     * same complete semantic Alternative (public equality includes the hidden secret).
     * Keep captured bases for pending work and grace; observation never regenerates codes.
     */
    void retainOrdinary(VaultState state) {
        Edt.require();
        Set<TokenId> ids = new HashSet<>(entries.keySet()); ids.addAll(requests.keySet()); ids.addAll(staged.keySet());
        for (TokenId id : ids) {
            Request request = requests.get(id);
            Reveal reveal = entries.getOrDefault(id, staged.get(id));
            List<TokenAlternative> alternatives = request != null ? request.alternatives() : reveal.alternatives();
            TokenState token = state.token(id).orElse(null);
            if (token == null || token.hasConflict() || token.alternatives().size() != 1
                    || alternatives.size() != 1 || alternatives.get(0).descriptor().status() != TokenStatus.ACTIVE
                    || !token.alternatives().get(0).equals(alternatives.get(0))) {
                clear(id);
            }
        }
    }
    void clear(TokenId id) {
        Edt.require(); requests.remove(id); entries.remove(id); staged.remove(id); render.accept(id, List.of());
        if (entries.isEmpty() && requests.values().stream().noneMatch(r -> r.window() != null)) { timer.stop(); }
    }
    void clear() {
        Edt.require();
        Set<TokenId> ids = new HashSet<>(entries.keySet()); ids.addAll(requests.keySet()); ids.addAll(staged.keySet());
        for (TokenId id : ids) { clear(id); }
        timer.stop();
    }
    boolean running() { return automatic ? timer.isRunning() : !entries.isEmpty() || requests.values().stream().anyMatch(r -> r.window() != null); }
}
