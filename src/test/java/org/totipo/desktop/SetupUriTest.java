package org.totipo.desktop;

import org.junit.jupiter.api.Test;
import org.totipo.TotpAlgorithm;
import java.time.Duration;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class SetupUriTest {
    private static final String URI = "otpauth://totp/GitHub:niki%40example.com?secret=MY&issuer=GitHub";
    @Test void supportedEnrollmentAndNondefaultParametersAreReviewedWithoutSecret() {
        try (SetupDraft draft = SetupUri.parse((URI + "&algorithm=SHA512&digits=8&period=60").toCharArray())) {
            assertEquals("GitHub", draft.review().issuer()); assertEquals("niki@example.com", draft.review().account());
            assertEquals(TotpAlgorithm.SHA512, draft.review().algorithm()); assertEquals(8, draft.review().digits());
            assertEquals(Duration.ofSeconds(60), draft.review().period());
            assertFalse(draft.review().toString().contains("MY")); assertFalse(draft.toString().contains("MY"));
        }
    }
    @Test void everySupportedAlgorithmAndDigitCountIsPreserved() {
        for (TotpAlgorithm algorithm : TotpAlgorithm.values()) {
            for (int digits : new int[]{6, 7, 8}) {
                try (SetupDraft draft = SetupUri.parse((URI + "&algorithm=" + algorithm.name().toLowerCase(java.util.Locale.ROOT) + "&digits=" + digits).toCharArray())) {
                    assertEquals(algorithm, draft.review().algorithm()); assertEquals(digits, draft.review().digits());
                }
            }
        }
    }
    @Test void defaultsAndLabelOnlyIssuerAndLiteralPlusAreSupported() {
        try (SetupDraft draft = SetupUri.parse("otpauth://totp/Service:user+tag?secret=MY".toCharArray())) {
            assertEquals("Service", draft.review().issuer()); assertEquals("user+tag", draft.review().account());
            assertEquals(TotpAlgorithm.SHA1, draft.review().algorithm()); assertEquals(6, draft.review().digits());
            assertEquals(Duration.ofSeconds(30), draft.review().period());
        }
    }
    @Test void malformedAndNonTotpEnrollmentHasSpecificRedactedFailures() {
        String[][] cases = {
            {"https://example.com", "otpauth://"}, {"otpauth://hotp/user?secret=MY", "Only TOTP"},
            {"otpauth://totp/user%ZZ?secret=MY", "Malformed"}, {"otpauth://totp/user", "missing a secret"},
            {"otpauth://totp/user?secret=M0", "Base32"}, {"otpauth://totp/user?secret=MZ", "Base32"},
            {URI + "&algorithm=MD5", "Unsupported algorithm"}, {URI + "&digits=5", "Unsupported digits"},
            {URI + "&digits=9", "Unsupported digits"}, {URI + "&digits=x", "Unsupported digits"},
            {URI + "&period=0", "whole seconds"}, {URI + "&period=4294967296", "whole seconds"},
            {URI + "&period=1.5", "whole seconds"}, {URI + "&secret=MY", "Repeated"},
            {URI + "&counter=1", "Unsupported"}, {"otpauth://totp/Other:user?secret=MY&issuer=Service", "disagree"},
            {"otpauth://totp/user?secret=M%20Y", "Base32"}
        };
        for (String[] test : cases) {
            char[] input = test[0].toCharArray();
            var failure = assertThrows(SetupValidation.Invalid.class, () -> SetupUri.parse(input));
            assertTrue(failure.getMessage().contains(test[1]), failure.getMessage());
            assertEquals(SetupValidation.Field.URI, failure.field()); assertNull(failure.getCause());
            assertFalse(failure.getMessage().contains(test[0])); assertArrayEquals(new char[input.length], input);
        }
    }
    @Test void manualFriendlyNormalizationAndDomainPeriodBounds() {
        for (String period : new String[]{"1", "30", "4294967295"}) {
            try (SetupDraft draft = SetupValidation.manual("", "", " m y \n".toCharArray(), TotpAlgorithm.SHA1, 6, period)) {
                assertEquals(Long.parseLong(period), draft.review().period().getSeconds());
            }
        }
        for (String period : new String[]{"", "0", "-1", "4294967296", "1.5", "30junk"}) {
            assertEquals(SetupValidation.Field.PERIOD, assertThrows(SetupValidation.Invalid.class,
                    () -> SetupValidation.manual("", "", "MY".toCharArray(), TotpAlgorithm.SHA1, 6, period)).field());
        }
    }
    @Test void retirementAndTransferWipeOwnedBytesAndPreventReuse() {
        byte[] cancelled = {102}; var a = new SetupDraft(TokenWritesTest.FIELDS, cancelled); a.close();
        assertArrayEquals(new byte[1], cancelled); assertThrows(IllegalStateException.class, () -> a.transfer(null));
        byte[] transferred = {102}; var b = new SetupDraft(TokenWritesTest.FIELDS, transferred);
        var mutation = b.transfer(null); b.close(); assertArrayEquals(new byte[]{102}, transferred);
        mutation.close(); assertArrayEquals(new byte[1], transferred);
    }
    @Test void identityValidatesJavaUtf8BoundsWithoutInventingRequiredIdentity() {
        SetupValidation.identity("", ""); SetupValidation.identity("é".repeat(128), "x".repeat(256));
        assertEquals(SetupValidation.Field.ISSUER, assertThrows(SetupValidation.Invalid.class,
                () -> SetupValidation.identity("é".repeat(129), "")).field());
        assertEquals(SetupValidation.Field.ACCOUNT, assertThrows(SetupValidation.Invalid.class,
                () -> SetupValidation.identity("", "\ud800")).field());
    }
}
