package com.huashi.eftransfer.app.modules.user.service;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public final class DisplayNameNormalizer {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private DisplayNameNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank() || !looksLikeMojibake(value)) {
            return value;
        }
        String decoded = decodeMojibake(value);
        return decoded == null || decoded.indexOf('\uFFFD') >= 0 || decoded.isBlank() ? value : decoded;
    }

    private static String decodeMojibake(String value) {
        if (WINDOWS_1252.newEncoder().canEncode(value)) {
            return new String(value.getBytes(WINDOWS_1252), StandardCharsets.UTF_8);
        }
        StringBuilder hexBytes = new StringBuilder(value.length() * 2);
        for (int index = 0; index < value.length(); index++) {
            int codePoint = value.charAt(index);
            if (codePoint <= 0xFF) {
                hexBytes.append(HexFormat.of().toHexDigits((byte) codePoint));
                continue;
            }
            byte[] encoded = String.valueOf((char) codePoint).getBytes(WINDOWS_1252);
            if (encoded.length != 1 || encoded[0] == '?') {
                return null;
            }
            hexBytes.append(HexFormat.of().toHexDigits(encoded[0]));
        }
        return new String(HexFormat.of().parseHex(hexBytes), StandardCharsets.UTF_8);
    }

    public static boolean looksLikeMojibake(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.indexOf('Ã') >= 0
                || value.indexOf('Â') >= 0
                || value.contains("â")
                || value.contains("ç")
                || value.contains("å")
                || value.contains("è")
                || value.contains("æ");
    }
}
