package com.coast.radio.guard.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TranscriptCorrectionDiffUtil {

    private TranscriptCorrectionDiffUtil() {
    }

    public static List<Map<String, String>> build(String rawText, String correctedText) {
        String raw = rawText == null ? "" : rawText;
        String corrected = correctedText == null ? "" : correctedText;
        if (raw.equals(corrected)) {
            return List.of(Map.of("type", "same", "text", raw));
        }
        if ((long) raw.length() * (long) corrected.length() > 1_000_000L) {
            return buildFast(raw, corrected);
        }

        int n = raw.length();
        int m = corrected.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                if (raw.charAt(i) == corrected.charAt(j)) {
                    dp[i][j] = dp[i + 1][j + 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }

        List<Map<String, String>> ops = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < n && j < m) {
            if (raw.charAt(i) == corrected.charAt(j)) {
                appendSame(ops, String.valueOf(raw.charAt(i)));
                i++;
                j++;
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                appendDelete(ops, String.valueOf(raw.charAt(i)));
                i++;
            } else {
                appendInsert(ops, String.valueOf(corrected.charAt(j)));
                j++;
            }
        }
        while (i < n) {
            appendDelete(ops, String.valueOf(raw.charAt(i++)));
        }
        while (j < m) {
            appendInsert(ops, String.valueOf(corrected.charAt(j++)));
        }
        return mergeReplace(ops);
    }

    private static List<Map<String, String>> buildFast(String raw, String corrected) {
        int prefix = 0;
        int minLen = Math.min(raw.length(), corrected.length());
        while (prefix < minLen && raw.charAt(prefix) == corrected.charAt(prefix)) {
            prefix++;
        }
        int rawSuffix = raw.length() - 1;
        int correctedSuffix = corrected.length() - 1;
        while (rawSuffix >= prefix && correctedSuffix >= prefix && raw.charAt(rawSuffix) == corrected.charAt(correctedSuffix)) {
            rawSuffix--;
            correctedSuffix--;
        }

        List<Map<String, String>> parts = new ArrayList<>();
        if (prefix > 0) {
            parts.add(Map.of("type", "same", "text", raw.substring(0, prefix)));
        }
        String from = raw.substring(prefix, rawSuffix + 1);
        String to = corrected.substring(prefix, correctedSuffix + 1);
        if (!from.isEmpty() || !to.isEmpty()) {
            if (from.isEmpty()) {
                parts.add(Map.of("type", "insert", "text", to));
            } else if (to.isEmpty()) {
                parts.add(Map.of("type", "delete", "text", from));
            } else {
                parts.add(Map.of("type", "replace", "from", from, "to", to));
            }
        }
        if (rawSuffix + 1 < raw.length()) {
            parts.add(Map.of("type", "same", "text", raw.substring(rawSuffix + 1)));
        }
        return parts;
    }

    private static void appendSame(List<Map<String, String>> ops, String text) {
        appendText(ops, "same", text);
    }

    private static void appendDelete(List<Map<String, String>> ops, String text) {
        appendText(ops, "delete", text);
    }

    private static void appendInsert(List<Map<String, String>> ops, String text) {
        appendText(ops, "insert", text);
    }

    private static void appendText(List<Map<String, String>> ops, String type, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (!ops.isEmpty()) {
            Map<String, String> last = ops.get(ops.size() - 1);
            if (type.equals(last.get("type")) && last.containsKey("text")) {
                ops.set(ops.size() - 1, Map.of("type", type, "text", last.get("text") + text));
                return;
            }
        }
        ops.add(Map.of("type", type, "text", text));
    }

    private static List<Map<String, String>> mergeReplace(List<Map<String, String>> ops) {
        List<Map<String, String>> merged = new ArrayList<>();
        int index = 0;
        while (index < ops.size()) {
            Map<String, String> current = ops.get(index);
            if ("delete".equals(current.get("type"))) {
                String from = current.getOrDefault("text", "");
                StringBuilder to = new StringBuilder();
                int next = index + 1;
                while (next < ops.size() && "insert".equals(ops.get(next).get("type"))) {
                    to.append(ops.get(next).getOrDefault("text", ""));
                    next++;
                }
                if (to.length() > 0) {
                    merged.add(Map.of("type", "replace", "from", from, "to", to.toString()));
                    index = next;
                    continue;
                }
            }
            if ("insert".equals(current.get("type"))) {
                merged.add(Map.of("type", "insert", "text", current.getOrDefault("text", "")));
            } else if ("delete".equals(current.get("type"))) {
                merged.add(Map.of("type", "delete", "text", current.getOrDefault("text", "")));
            } else {
                merged.add(current);
            }
            index++;
        }
        return merged;
    }
}
