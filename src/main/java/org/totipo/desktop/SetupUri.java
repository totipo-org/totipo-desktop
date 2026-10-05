package org.totipo.desktop;

import org.totipo.TotpAlgorithm;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import static org.totipo.desktop.SetupValidation.Field.URI;

/** Minimal enrollment parser. No clipboard access, persistence, secret comparison or TOTP derivation. */
public final class SetupUri {
    private SetupUri() { }
    public static SetupDraft parse(char[] input) {
        try {
            java.net.URI uri;
            try { uri = new URI(new String(input)); }
            catch (URISyntaxException invalid) { throw invalid("Malformed setup URI. Check its encoding and punctuation."); }
            if (!"otpauth".equalsIgnoreCase(uri.getScheme())) { throw invalid("Enter an otpauth:// setup URI."); }
            if (!"totp".equalsIgnoreCase(uri.getHost())) { throw invalid("Only TOTP setup is supported; HOTP is not accepted."); }
            if (uri.getUserInfo() != null || uri.getPort() != -1 || uri.getFragment() != null
                    || uri.getRawPath() == null || !uri.getRawPath().startsWith("/")) {
                throw invalid("Malformed TOTP setup URI.");
            }
            String label = decode(uri.getRawPath().substring(1));
            if (label.isEmpty() || label.contains("/")) { throw invalid("The setup URI needs an account label."); }
            Map<String, String> parameters = new HashMap<>();
            if (uri.getRawQuery() != null) {
                for (String pair : uri.getRawQuery().split("&", -1)) {
                    int equals = pair.indexOf('=');
                    if (equals < 1) { throw invalid("Malformed setup URI parameter."); }
                    String key = decode(pair.substring(0, equals));
                    if (!java.util.Set.of("secret", "issuer", "algorithm", "digits", "period").contains(key)) {
                        throw invalid("Unsupported setup URI parameter. Only TOTP enrollment parameters are accepted.");
                    }
                    if (parameters.putIfAbsent(key, decode(pair.substring(equals + 1))) != null) {
                        throw invalid("Repeated setup URI parameter.");
                    }
                }
            }
            int colon = label.indexOf(':');
            String prefix = colon < 0 ? "" : label.substring(0, colon);
            String account = colon < 0 ? label : label.substring(colon + 1);
            String issuer = parameters.getOrDefault("issuer", prefix);
            if (!prefix.isEmpty() && !prefix.equals(issuer)) { throw invalid("The label issuer and issuer parameter disagree."); }
            TotpAlgorithm algorithm;
            try { algorithm = TotpAlgorithm.valueOf(parameters.getOrDefault("algorithm", "SHA1").toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException unsupported) { throw invalid("Unsupported algorithm. Use SHA1, SHA256 or SHA512."); }
            int digits;
            try { digits = Integer.parseInt(parameters.getOrDefault("digits", "6")); }
            catch (NumberFormatException unsupported) { throw invalid("Unsupported digits. Use 6, 7 or 8."); }
            if (digits < 6 || digits > 8) { throw invalid("Unsupported digits. Use 6, 7 or 8."); }
            char[] secret = parameters.getOrDefault("secret", "").toCharArray();
            try {
                // URI secrets use strict Base32; friendly separators are only for manual entry.
                if (secret.length == 0) { throw invalid("The setup URI is missing a secret."); }
                for (char c : secret) {
                    if (!(c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '2' && c <= '7' || c == '=')) {
                        throw invalid("The setup URI secret is malformed Base32.");
                    }
                }
                try { return SetupValidation.manual(issuer, account, secret, algorithm, digits,
                        parameters.getOrDefault("period", "30")); }
                catch (SetupValidation.Invalid invalid) { throw invalid(invalid.getMessage()); }
            } finally { Arrays.fill(secret, '\0'); parameters.clear(); }
        } finally { Arrays.fill(input, '\0'); }
    }
    private static String decode(String value) {
        try {
            // '+' is literal in an enrollment URI, rather than HTML form whitespace.
            String decoded = URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
            if (decoded.indexOf('\ufffd') >= 0) { throw invalid("Malformed UTF-8 in setup URI."); }
            return decoded;
        } catch (IllegalArgumentException malformed) { throw invalid("Malformed encoding in setup URI."); }
    }
    private static SetupValidation.Invalid invalid(String text) { return new SetupValidation.Invalid(URI, text); }
}
