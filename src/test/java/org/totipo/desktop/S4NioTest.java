package org.totipo.desktop;

import org.totipo.*;
import org.totipo.storage.nio.NioTotipo;
import org.totipo.desktop.ui.MergeEditorPanel;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.TokenWriteControllerTest.*;

/** Real consumed Java boundary, including semantic collapse and captured-basis freshness. */
class S4NioTest {
    @TempDir Path directory;
    static VaultState observed(VaultSession session, TokenId id, Predicate<TokenState> predicate) throws Exception {
        session.requestRefresh(); long until = System.nanoTime() + 10_000_000_000L;
        while (true) {
            VaultState state = session.state();
            if (state.token(id).filter(predicate).isPresent()) { return state; }
            if (System.nanoTime() > until) { fail("State not established"); } Thread.sleep(20);
        }
    }
    static SaveResult.Saved create(VaultSession session) {
        try (CreateToken builder = session.state().createToken(); NewSecret secret = NewSecret.copyOf(new byte[]{102})) {
            return assertInstanceOf(SaveResult.Saved.class, builder.issuer("Base").account("account").secret(secret).save());
        }
    }
    static void branch(VaultState base, TokenAlternative original, String issuer, TokenStatus status) {
        try (UpdateToken builder = base.update(original)) {
            assertInstanceOf(SaveResult.Saved.class, builder.issuer(issuer).status(status).metadata(new ClientMetadata(java.util.Optional.of(java.util.UUID.randomUUID().toString()), java.util.Optional.empty())).save());
        }
    }
    @Test void multiHeadAlternativeAppearsOnceAndWholeKeepResolvesWithoutFieldReconstruction() throws Exception {
        try (VaultSession session = assertInstanceOf(CreateVaultResult.Created.class, NioTotipo.create(directory, "s4".toCharArray())).session()) {
            TokenId id = create(session).tokenId(); VaultState base = observed(session, id, t -> !t.hasConflict());
            TokenAlternative original = base.token(id).orElseThrow().alternatives().getFirst();
            branch(base, original, "Alpha", TokenStatus.ACTIVE); branch(base, original, "Alpha", TokenStatus.ACTIVE);
            branch(base, original, "Beta", TokenStatus.TOMBSTONED);
            VaultState conflict = observed(session, id, t -> t.heads().size() == 3 && t.alternatives().size() == 2);
            TokenState token = conflict.token(id).orElseThrow(); assertTrue(token.hasConflict());
            assertEquals(2, token.alternatives().stream().filter(a -> a.descriptor().issuer().equals("Alpha")).findFirst().orElseThrow().heads().size());
            edt(() -> {
                var panel = new MergeEditorPanel(MergeInputs.capture(conflict, token), MergeDraft::close, () -> {});
                var choices = components(panel).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).toList();
                assertEquals(2, choices.size()); assertTrue(choices.stream().noneMatch(AbstractButton::isSelected)); panel.retire();
            });
            TokenAlternative selected = token.alternatives().stream().filter(a -> a.descriptor().issuer().equals("Beta")).findFirst().orElseThrow();
            assertInstanceOf(SaveResult.Saved.class, MergeWrites.save(MergeDraft.keep(MergeInputs.capture(conflict, token), selected)));
            VaultState resolved = observed(session, id, t -> !t.hasConflict() && t.alternatives().getFirst().descriptor().issuer().equals("Beta"));
            assertEquals(selected.descriptor(), resolved.token(id).orElseThrow().alternatives().getFirst().descriptor());
        }
    }
    @Test void newAlternativeAfterCaptureReturnsAdditionalConflictWithoutPublishingResolution() throws Exception {
        try (VaultSession session = assertInstanceOf(CreateVaultResult.Created.class, NioTotipo.create(directory, "s4".toCharArray())).session()) {
            TokenId id = create(session).tokenId(); VaultState base = observed(session, id, t -> !t.hasConflict());
            TokenAlternative original = base.token(id).orElseThrow().alternatives().getFirst();
            branch(base, original, "Alpha", TokenStatus.ACTIVE); branch(base, original, "Beta", TokenStatus.ACTIVE);
            VaultState conflict = observed(session, id, t -> t.alternatives().size() == 2);
            TokenState token = conflict.token(id).orElseThrow(); MergeDraft draft = MergeDraft.keep(MergeInputs.capture(conflict, token), token.alternatives().getFirst());
            branch(base, original, "Gamma", TokenStatus.ACTIVE); observed(session, id, t -> t.alternatives().size() == 3);
            SaveResult.AdditionalConflict result = assertInstanceOf(SaveResult.AdditionalConflict.class, MergeWrites.save(draft));
            result.resolution().close(); assertEquals(3, result.latest().token(id).orElseThrow().alternatives().size());
            assertEquals(3, session.state().token(id).orElseThrow().alternatives().size());
        }
    }
    @Test void publishedApiContainsWholeAlternativeSelector() throws Exception {
        assertEquals(MergeToken.class, MergeToken.class.getMethod("keep", TokenAlternative.class).getReturnType());
        String location = MergeToken.class.getProtectionDomain().getCodeSource().getLocation().toString();
        assertTrue(location.endsWith("totipo-core-0.1.3.jar"), location);
    }
}
