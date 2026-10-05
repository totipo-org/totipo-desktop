package org.totipo.desktop;

import org.totipo.*;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Enrollment/identity ingress rules from the consumed Java domain, without code generation. */
public final class SetupValidation {
    public enum Field { URI, ISSUER, ACCOUNT, SECRET, ALGORITHM, DIGITS, PERIOD }
    public static final class Invalid extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
        private final Field field;
        public Invalid(Field field, String message) { super(message); this.field = field; }
        public Field field() { return field; }
    }
    private SetupValidation() { }
    public static void identity(String issuer, String account) {
        text(issuer, Field.ISSUER); text(account, Field.ACCOUNT);
    }
    private static void text(String value, Field field) {
        try {
            if (StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(value)).remaining() <= 256) { return; }
        } catch (CharacterCodingException invalid) {
            throw new Invalid(field, "Use valid Unicode text.");
        }
        throw new Invalid(field, "Use at most 256 UTF-8 bytes.");
    }
    public static long period(String text) {
        try {
            long value = Long.parseLong(text);
            if (value >= 1 && value <= 4294967295L) { return value; }
        } catch (NumberFormatException invalid) { /* Specific local message below. */ }
        throw new Invalid(Field.PERIOD, "Enter whole seconds from 1 to 4294967295.");
    }
    public static SetupDraft manual(String issuer, String account, char[] secret,
                                    TotpAlgorithm algorithm, int digits, String period) {
        identity(issuer, account);
        if (algorithm == null) { throw new Invalid(Field.ALGORITHM, "Choose SHA1, SHA256 or SHA512."); }
        if (digits < 6 || digits > 8) { throw new Invalid(Field.DIGITS, "Choose 6, 7 or 8 digits."); }
        long seconds = period(period);
        byte[] decoded;
        try { decoded = Base32.decode(secret); }
        catch (IllegalArgumentException invalid) { throw new Invalid(Field.SECRET, invalid.getMessage()); }
        return new SetupDraft(new TokenDescriptor(TokenStatus.ACTIVE, issuer, account, algorithm,
                digits, Duration.ofSeconds(seconds)), decoded);
    }
}
