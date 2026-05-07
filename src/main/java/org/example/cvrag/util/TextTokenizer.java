package org.example.cvrag.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextTokenizer {

    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{L}\\p{N}]{2,}");

    private TextTokenizer() {
    }

    public static List<String> tokens(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        List<String> tokens = new ArrayList<>();

        Matcher matcher = WORD_PATTERN.matcher(normalized);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                tokens.add(String.valueOf(c));
            }
        }
        return tokens;
    }
}
