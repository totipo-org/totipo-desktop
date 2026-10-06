package org.totipo.desktop;

import org.junit.jupiter.api.Test;
import org.totipo.*;
import org.totipo.desktop.ui.*;
import javax.swing.*;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.TokenWriteControllerTest.*;

class TextRoundTripTest {
    private static final String ISSUER = " <html>e\u0301\n\0\\ ";
    private static final String ACCOUNT = "Account\r\n\t\u202E ";

    @Test void unchangedEditorPassesExactStoredStringsToUpdateBuilder() throws Exception {
        var fields = new TokenDescriptor(TokenStatus.ACTIVE, ISSUER, ACCOUNT, TotpAlgorithm.SHA1, 6, Duration.ofSeconds(30));
        var fake = new TokenWritesTest.Recording(); fake.results.add(TokenWritesTest.saved());
        fake.expected = new TokenAlternative() {
            public TokenDescriptor descriptor() { return fields; }
            public java.util.List<TokenHead> heads() { return java.util.List.of(); }
        };
        var submitted = new AtomicReference<TokenDraft>();
        edt(() -> {
            var panel = new TokenEditorPanel(fields, "Update", submitted::set, () -> {});
            button(panel, "Save").doClick(); panel.retire();
        });
        assertInstanceOf(SaveResult.Saved.class, TokenWrites.save(fake.state, fake.expected, submitted.get()));
        assertEquals(ISSUER, fake.values.get("issuer")); assertEquals(ACCOUNT, fake.values.get("account"));
    }

    @Test void customMergeFieldsPassExactStringsRatherThanDisplayEscapesToBuilder() throws Exception {
        var fake = new MergeFixtures.Recording(); fake.results.add(TokenWritesTest.saved());
        var submitted = new AtomicReference<MergeDraft>();
        edt(() -> {
            var panel = new MergeEditorPanel(fake.inputs(), submitted::set, () -> {});
            button(panel, "Combine details…").doClick(); MergeEditorTest.chooseFirst(panel);
            MergeEditorTest.customText(panel, "issuer").setText(ISSUER);
            MergeEditorTest.customText(panel, "account").setText(ACCOUNT);
            button(panel, "Save Resolution").doClick(); panel.retire();
        });
        assertInstanceOf(SaveResult.Saved.class, MergeWrites.save(submitted.get()));
        assertEquals(ISSUER, fake.values.get("issuer")); assertEquals(ACCOUNT, fake.values.get("account"));
    }
}
