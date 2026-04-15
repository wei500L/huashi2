package com.huashi.eftransfer.app.modules.lexicon.support;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PinyinSearchIndexSupport {

    public record SearchIndex(String pinyin, String initials) {
    }

    private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();

    static {
        FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }

    private PinyinSearchIndexSupport() {
    }

    public static SearchIndex build(List<String> values) {
        Set<String> pinyinTokens = new LinkedHashSet<>();
        Set<String> initialTokens = new LinkedHashSet<>();
        for (String value : values) {
            appendIndex(value, pinyinTokens, initialTokens);
        }
        return new SearchIndex(
                SearchableTextBuilder.concat(pinyinTokens),
                SearchableTextBuilder.concat(initialTokens)
        );
    }

    private static void appendIndex(String value, Set<String> pinyinTokens, Set<String> initialTokens) {
        if (value == null || value.isBlank()) {
            return;
        }
        StringBuilder syllables = new StringBuilder();
        StringBuilder initials = new StringBuilder();
        StringBuilder asciiToken = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (isChinese(current)) {
                flushAsciiToken(asciiToken, pinyinTokens, initialTokens);
                String pinyin = resolvePinyin(current);
                if (pinyin == null || pinyin.isBlank()) {
                    continue;
                }
                syllables.append(pinyin);
                initials.append(pinyin.charAt(0));
                continue;
            }
            if (Character.isLetterOrDigit(current)) {
                asciiToken.append(Character.toLowerCase(current));
                continue;
            }
            flushAsciiToken(asciiToken, pinyinTokens, initialTokens);
            flushChineseToken(syllables, initials, pinyinTokens, initialTokens);
        }
        flushAsciiToken(asciiToken, pinyinTokens, initialTokens);
        flushChineseToken(syllables, initials, pinyinTokens, initialTokens);
    }

    private static void flushAsciiToken(StringBuilder asciiToken, Set<String> pinyinTokens, Set<String> initialTokens) {
        if (asciiToken.length() == 0) {
            return;
        }
        String token = asciiToken.toString();
        pinyinTokens.add(token);
        initialTokens.add(token);
        asciiToken.setLength(0);
    }

    private static void flushChineseToken(
            StringBuilder syllables,
            StringBuilder initials,
            Set<String> pinyinTokens,
            Set<String> initialTokens
    ) {
        if (syllables.length() > 0) {
            pinyinTokens.add(syllables.toString());
            syllables.setLength(0);
        }
        if (initials.length() > 0) {
            initialTokens.add(initials.toString());
            initials.setLength(0);
        }
    }

    private static boolean isChinese(char value) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(value);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }

    private static String resolvePinyin(char value) {
        try {
            String[] pinyins = PinyinHelper.toHanyuPinyinStringArray(value, FORMAT);
            if (pinyins == null || pinyins.length == 0) {
                return null;
            }
            return pinyins[0].toLowerCase(Locale.ROOT);
        } catch (BadHanyuPinyinOutputFormatCombination exception) {
            return null;
        }
    }
}
