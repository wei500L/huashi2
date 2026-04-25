package com.huashi.eftransfer.app.modules.user.service;

import com.huashi.eftransfer.app.common.util.TextMojibakeNormalizer;

public final class DisplayNameNormalizer {

    private DisplayNameNormalizer() {
    }

    public static String normalize(String value) {
        return TextMojibakeNormalizer.normalize(value);
    }

    public static boolean looksLikeMojibake(String value) {
        return TextMojibakeNormalizer.looksLikeMojibake(value);
    }
}
