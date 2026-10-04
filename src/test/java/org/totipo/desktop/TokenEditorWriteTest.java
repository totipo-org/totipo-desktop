package org.totipo.desktop;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.totipo.*;
import org.totipo.desktop.ui.TokenEditorPanel;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.TokenWriteControllerTest.*;
import static org.totipo.desktop.TokenWritesTest.*;

/** Exercise the actual form-to-existing-builder path, including exact values and secret opt-in. */
class TokenEditorWriteTest {
    private static final String ISSUER = " <html>issuer\n\0e\u0301\\ ";
    private static final String ACCOUNT = "Account\r\n\t\u202E ";

    @Test void createAndEditChoicesWriteExactDomainValuesAndOptionalSecret() throws Exception {
        for (boolean create : List.of(false, true)) {
            for (TokenStatus status : create ? new TokenStatus[]{TokenStatus.ACTIVE} : TokenStatus.values()) {
                for (TotpAlgorithm algorithm : TotpAlgorithm.values()) {
                    for (int digits : new int[]{6, 7, 8}) {
                        for (boolean replace : List.of(false, true)) {
                            Recording fake = new Recording(); fake.results.add(saved());
                            var original = new TokenDescriptor(TokenStatus.ACTIVE, "old issuer", "old account", TotpAlgorithm.SHA1, 6, Duration.ofSeconds(30));
                            fake.expected = new TokenAlternative() {
                                public TokenDescriptor descriptor() { return original; }
                                public List<TokenHead> heads() { return List.of(); }
                            };
                            AtomicReference<TokenDraft> submitted = new AtomicReference<>();
                            edt(() -> {
                                var panel = new TokenEditorPanel(create ? null : original, "Context", submitted::set, () -> {});
                                var text = components(panel).stream().filter(c -> c instanceof JTextField && !(c instanceof JPasswordField)
                                                && !(c instanceof JFormattedTextField)).map(JTextField.class::cast).toList();
                                text.get(0).setText(ISSUER); text.get(1).setText(ACCOUNT);
                                if (!create) { choose(panel, "Status " + (status == TokenStatus.ACTIVE ? "Active" : "Deleted")); }
                                choose(panel, "Algorithm " + algorithm.name()); choose(panel, "Digits " + digits);
                                JSpinner period = components(panel).stream().filter(JSpinner.class::isInstance).map(JSpinner.class::cast).findFirst().orElseThrow();
                                ((JSpinner.DefaultEditor) period.getEditor()).getTextField().setText("4294967295");
                                if (!create && replace) {
                                    components(panel).stream().filter(JCheckBox.class::isInstance).map(JCheckBox.class::cast).findFirst().orElseThrow().doClick(0);
                                }
                                password(panel).setText(create || replace ? "MY" : "invalid abandoned input");
                                button(panel, create ? "Create" : "Save").doClick(0); panel.retire();
                            });
                            assertNotNull(submitted.get());
                            assertInstanceOf(SaveResult.Saved.class, TokenWrites.save(fake.state, create ? null : fake.expected, submitted.get()));
                            assertEquals(status, fake.values.get("status")); assertEquals(ISSUER, fake.values.get("issuer"));
                            assertEquals(ACCOUNT, fake.values.get("account")); assertEquals(algorithm, fake.values.get("algorithm"));
                            assertEquals(digits, fake.values.get("digits")); assertEquals(Duration.ofSeconds(4294967295L), fake.values.get("period"));
                            assertEquals(create || replace, fake.calls.contains("secret"));
                            assertEquals(create ? "createToken" : "update", fake.calls.get(0));
                            if (create || replace) { assertThrows(IllegalStateException.class, () -> ((NewSecret) fake.values.get("wrapper")).copy()); }
                        }
                    }
                }
            }
        }
    }

    @Test void creationStillAllowsEmptyIssuerAndAccountWithValidRequiredSecret() throws Exception {
        Recording fake = new Recording(); fake.results.add(saved());
        AtomicReference<TokenDraft> submitted = new AtomicReference<>();
        edt(() -> {
            var panel = new TokenEditorPanel(null, "Create", submitted::set, () -> {});
            password(panel).setText("MY"); button(panel, "Create").doClick(0); panel.retire();
        });
        assertInstanceOf(SaveResult.Saved.class, TokenWrites.save(fake.state, null, submitted.get()));
        assertEquals("", fake.values.get("issuer")); assertEquals("", fake.values.get("account"));
        assertEquals(TokenStatus.ACTIVE, fake.values.get("status")); assertEquals(TotpAlgorithm.SHA1, fake.values.get("algorithm"));
        assertEquals(6, fake.values.get("digits")); assertEquals(Duration.ofSeconds(30), fake.values.get("period"));
    }

    private static void choose(TokenEditorPanel panel, String accessibleName) {
        components(panel).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
                .filter(button -> accessibleName.equals(button.getAccessibleContext().getAccessibleName()))
                .findFirst().orElseThrow().doClick(0);
    }
}
