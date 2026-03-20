package com.goblin.scheduler.util;

public final class TextSanitizer {
    private static final java.util.regex.Pattern HTML_TAG = java.util.regex.Pattern.compile("<[^>]*>");

    private TextSanitizer() {}

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        String result = HTML_TAG.matcher(input).replaceAll("");
        result = stripControlChars(result);
        return result.trim();
    }

    private static String stripControlChars(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\t' || c == '\n' || c == '\r' || !Character.isISOControl(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
