package org.totipo.desktop;

import org.totipo.*;
import org.totipo.desktop.ui.TokenManagementPanel;
import javax.swing.*;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.TokenWriteControllerTest.*;
import static org.totipo.desktop.TokenWritesTest.*;

class S3MutationTest {
    private static final TokenDescriptor ORIGINAL = new TokenDescriptor(TokenStatus.ACTIVE, "Service", "account",
            TotpAlgorithm.SHA256, 8, Duration.ofSeconds(42));
    static TokenAlternative alternative(TokenDescriptor d) {
        return new TokenAlternative() {
            public TokenDescriptor descriptor() { return d; }
            public List<TokenHead> heads() { throw new AssertionError("Management must not inspect heads"); }
        };
    }
    static VaultState visible(Recording fake, TokenAlternative... alternatives) {
        TokenState token = new TokenState() {
            public TokenId id() { return saved().tokenId(); }
            public List<TokenAlternative> alternatives() { return List.of(alternatives); }
            public boolean hasConflict() { return alternatives.length > 1; }
            public List<TokenHead> heads() { throw new AssertionError("Head scan"); }
            public List<UnresolvedReference> unresolvedReferences() { throw new AssertionError("Unresolved scan"); }
            public TokenCompetition competingValues() { throw new AssertionError("Secret comparison scan"); }
        };
        return (VaultState) Proxy.newProxyInstance(VaultState.class.getClassLoader(), new Class<?>[]{VaultState.class},
                (p, method, args) -> {
                    if (method.getName().equals("tokens")) { return List.of(token); }
                    if (method.getName().equals("generateTotp")) { throw new AssertionError("Management derived a code"); }
                    return method.invoke(fake.state, args);
                });
    }
    static void uri(TokenManagementPanel panel) {
        password(panel).setText("otpauth://totp/Other:imported?secret=MY&algorithm=SHA512&digits=7&period=90");
        button(panel, "Review").doClick(0);
    }
    @Test void identityEditPreservesWholeSetupAndDoesNotSetExistingSecret() throws Exception {
        Recording fake = new Recording(); fake.expected = alternative(ORIGINAL); fake.results.add(saved());
        var state = visible(fake, fake.expected); AtomicReference<TokenDraft> draft = new AtomicReference<>();
        edt(() -> {
            var panel = new TokenManagementPanel(state, fake.expected, "", () -> state,
                    (base, target, value) -> { assertSame(state, base); assertSame(fake.expected, target); draft.set(value); }, () -> {});
            var fields = components(panel).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast).toList();
            assertEquals(2, fields.size()); fields.get(0).setText("Renamed"); fields.get(1).setText("changed");
            button(panel, "Save").doClick(0); panel.retire();
        });
        var result = assertInstanceOf(SaveResult.Saved.class, TokenWrites.save(fake.state, fake.expected, draft.get()));
        assertEquals(saved().tokenId(), result.tokenId()); assertEquals("Renamed", fake.values.get("issuer"));
        assertEquals("changed", fake.values.get("account")); assertEquals(ORIGINAL.algorithm(), fake.values.get("algorithm"));
        assertEquals(ORIGINAL.digits(), fake.values.get("digits")); assertEquals(ORIGINAL.period(), fake.values.get("period"));
        assertFalse(fake.calls.contains("secret")); assertEquals("update", fake.calls.get(0));
    }
    @Test void changeSetupPreservesTargetIdentityAndReplacesEverySetupFieldTogether() throws Exception {
        Recording fake = new Recording(); fake.expected = alternative(ORIGINAL); fake.results.add(saved());
        var state = visible(fake, fake.expected); AtomicReference<TokenDraft> draft = new AtomicReference<>();
        edt(() -> {
            var panel = new TokenManagementPanel(state, fake.expected, "", () -> state,
                    (base, target, value) -> { assertSame(fake.expected, target); draft.set(value); }, () -> {});
            button(panel, "Change setup…").doClick(0); uri(panel); assertNull(draft.get());
            button(panel, "Save setup").doClick(0); panel.retire();
        });
        assertInstanceOf(SaveResult.Saved.class, TokenWrites.save(fake.state, fake.expected, draft.get()));
        assertEquals("update", fake.calls.get(0)); assertEquals(ORIGINAL.issuer(), fake.values.get("issuer"));
        assertEquals(ORIGINAL.account(), fake.values.get("account")); assertEquals(TotpAlgorithm.SHA512, fake.values.get("algorithm"));
        assertEquals(7, fake.values.get("digits")); assertEquals(Duration.ofSeconds(90), fake.values.get("period")); assertTrue(fake.calls.contains("secret"));
    }
    @Test void duplicateUpdateUsesSelectedLogicalIdentityAndSameReplacementOperation() throws Exception {
        Recording fake = new Recording(); fake.expected = alternative(ORIGINAL); fake.results.add(saved());
        var state = visible(fake, fake.expected); AtomicReference<TokenDraft> draft = new AtomicReference<>();
        edt(() -> {
            var panel = new TokenManagementPanel(state, null, "", () -> state,
                    (base, target, value) -> { assertSame(state, base); assertSame(fake.expected, target); draft.set(value); }, () -> {});
            password(panel).setText("otpauth://totp/Service:account?secret=MY&algorithm=SHA512&digits=7&period=90");
            button(panel, "Review").doClick(0); button(panel, "Add").doClick(0); assertNull(draft.get());
            button(panel, "Update Existing…").doClick(0); assertNull(draft.get()); button(panel, "Save setup").doClick(0); panel.retire();
        });
        assertInstanceOf(SaveResult.Saved.class, TokenWrites.save(fake.state, fake.expected, draft.get()));
        assertFalse(fake.calls.contains("createToken")); assertEquals("update", fake.calls.get(0));
        assertEquals(ORIGINAL.issuer(), fake.values.get("issuer")); assertEquals(ORIGINAL.account(), fake.values.get("account"));
        assertEquals(TotpAlgorithm.SHA512, fake.values.get("algorithm")); assertEquals(7, fake.values.get("digits"));
        assertEquals(Duration.ofSeconds(90), fake.values.get("period")); assertTrue(fake.calls.contains("secret"));
    }
    @Test void deleteUsesLogicalTombstoneRetainingIdentitySetupAndSecret() throws Exception {
        Recording fake = new Recording(); fake.expected = alternative(ORIGINAL); fake.results.add(saved());
        var state = visible(fake, fake.expected); AtomicReference<TokenDraft> draft = new AtomicReference<>();
        edt(() -> {
            var panel = new TokenManagementPanel(state, fake.expected, "", () -> state,
                    (base, target, value) -> { assertSame(fake.expected, target); draft.set(value); }, () -> {});
            button(panel, "Delete TOTP…").doClick(0); assertNull(draft.get()); button(panel, "Delete TOTP").doClick(0); panel.retire();
        });
        assertInstanceOf(SaveResult.Saved.class, TokenWrites.save(fake.state, fake.expected, draft.get()));
        assertEquals("update", fake.calls.get(0)); assertEquals(TokenStatus.TOMBSTONED, fake.values.get("status"));
        assertEquals(ORIGINAL.issuer(), fake.values.get("issuer")); assertEquals(ORIGINAL.account(), fake.values.get("account"));
        assertEquals(ORIGINAL.algorithm(), fake.values.get("algorithm")); assertEquals(ORIGINAL.digits(), fake.values.get("digits"));
        assertEquals(ORIGINAL.period(), fake.values.get("period")); assertFalse(fake.calls.contains("secret"));
    }
    @Test void duplicatesInspectOnlyCurrentActiveExactIdentity() {
        Recording fake = new Recording(); var a = alternative(ORIGINAL);
        var deleted = alternative(new TokenDescriptor(TokenStatus.TOMBSTONED, ORIGINAL.issuer(), ORIGINAL.account(),
                ORIGINAL.algorithm(), ORIGINAL.digits(), ORIGINAL.period()));
        assertEquals(1, IdentityMatches.find(visible(fake, a, deleted), ORIGINAL).size());
        var different = new TokenDescriptor(TokenStatus.ACTIVE, "service", "account", TotpAlgorithm.SHA1, 6, Duration.ofSeconds(30));
        assertTrue(IdentityMatches.find(visible(fake, a), different).isEmpty());
        assertEquals(2, IdentityMatches.find(visible(fake, a, alternative(ORIGINAL)), ORIGINAL).size());
    }
}
