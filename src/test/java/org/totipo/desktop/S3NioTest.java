package org.totipo.desktop;

import org.totipo.*;
import org.totipo.storage.nio.NioTotipo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

class S3NioTest {
    @TempDir Path directory;
    @Test void realPublishedApiCreatesEditsReplacesAndDeletesSameLogicalTotpWithoutCodeDerivation() throws Exception {
        char[] password = {'s', '3'};
        try (VaultSession session = assertInstanceOf(CreateVaultResult.Created.class, NioTotipo.create(directory, password)).session()) {
            SaveResult.Saved added;
            try (SetupDraft setup = SetupUri.parse("otpauth://totp/Service:user?secret=MY".toCharArray())) {
                added = assertInstanceOf(SaveResult.Saved.class, TokenWrites.save(session.state(), null, setup.transfer(null)));
            }
            VaultState first = observed(session, added.tokenId(), d -> d.account().equals("user"));
            TokenAlternative target = first.token(added.tokenId()).orElseThrow().alternatives().getFirst();
            TokenDescriptor identity = new TokenDescriptor(TokenStatus.ACTIVE, "Renamed", "changed", target.descriptor().algorithm(),
                    target.descriptor().digits(), target.descriptor().period());
            var edited = assertInstanceOf(SaveResult.Saved.class, TokenWrites.save(first, target, new TokenDraft(identity, null)));
            assertEquals(added.tokenId(), edited.tokenId());
            VaultState second = observed(session, added.tokenId(), d -> d.account().equals("changed"));
            target = second.token(added.tokenId()).orElseThrow().alternatives().getFirst(); assertEquals(identity, target.descriptor());
            try (SetupDraft setup = SetupUri.parse("otpauth://totp/Other:ignored?secret=MZXQ&algorithm=SHA512&digits=8&period=60".toCharArray())) {
                var replaced = assertInstanceOf(SaveResult.Saved.class, TokenWrites.save(second, target, setup.transfer(target.descriptor())));
                assertEquals(added.tokenId(), replaced.tokenId());
            }
            VaultState third = observed(session, added.tokenId(), d -> d.algorithm() == TotpAlgorithm.SHA512);
            target = third.token(added.tokenId()).orElseThrow().alternatives().getFirst();
            assertEquals("Renamed", target.descriptor().issuer()); assertEquals("changed", target.descriptor().account());
            assertEquals(8, target.descriptor().digits()); assertEquals(Duration.ofSeconds(60), target.descriptor().period());
            TokenDescriptor d = target.descriptor();
            var deleted = assertInstanceOf(SaveResult.Saved.class, TokenWrites.save(third, target,
                    new TokenDraft(new TokenDescriptor(TokenStatus.TOMBSTONED, d.issuer(), d.account(), d.algorithm(), d.digits(), d.period()), null)));
            assertEquals(added.tokenId(), deleted.tokenId());
            VaultState fourth = observed(session, added.tokenId(), value -> value.status() == TokenStatus.TOMBSTONED);
            assertEquals(1, fourth.tokens().size()); assertFalse(fourth.token(added.tokenId()).orElseThrow().hasConflict());
        } finally { java.util.Arrays.fill(password, '\0'); }
    }
    private static VaultState observed(VaultSession session, TokenId id, Predicate<TokenDescriptor> predicate) throws Exception {
        CompletableFuture<VaultState> result = new CompletableFuture<>();
        session.states().subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;
            public void onSubscribe(Flow.Subscription value) { subscription = value; value.request(Long.MAX_VALUE); }
            public void onNext(VaultState value) {
                if (value.token(id).stream().flatMap(t -> t.alternatives().stream()).anyMatch(a -> predicate.test(a.descriptor()))) {
                    result.complete(value); subscription.cancel();
                }
            }
            public void onError(Throwable error) { result.completeExceptionally(error); }
            public void onComplete() { result.completeExceptionally(new IllegalStateException("Session retired")); }
        });
        return result.get(10, TimeUnit.SECONDS);
    }
}
