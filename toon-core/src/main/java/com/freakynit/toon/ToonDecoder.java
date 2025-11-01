package com.freakynit.toon;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ToonDecoder {
    private static final Pattern ARRAY_HEADER = Pattern.compile("^\\[(#?\\d+)([^\\]]*)?\\](.*)$");
    private static final Pattern TABULAR_HEADER = Pattern.compile("^\\[(#?\\d+)([^\\]]*)?\\]\\{([^}]+)\\}:$");

    private final ToonConfig config;

    public ToonDecoder() {
        this(new ToonConfig());
    }

    public ToonDecoder(ToonConfig config) {
        this.config = config;
    }

    public Object decode(String toon) {
        if (toon == null || toon.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        String[] lines = toon.split("\n");
        ParseContext ctx = new ParseContext(lines, config);

        if (lines[0].trim().startsWith("[")) {
            return parseRootArray(ctx);
        }

        return parseObject(ctx, 0);
    }

    private Object parseRootArray(ParseContext ctx) {
        String line = ctx.currentLine().trim();

        Matcher tabularMatcher = TABULAR_HEADER.matcher(line);
        if (tabularMatcher.matches()) {
            return parseTabularArray(ctx, 0);
        }

        Matcher arrayMatcher = ARRAY_HEADER.matcher(line);
        if (arrayMatcher.matches()) {
            String rest = arrayMatcher.group(3);
            if (rest.startsWith(": ")) {
                return parseInlineArray(line);
            } else if (rest.equals(":")) {
                ctx.advance();
                return parseListArray(ctx, 0);
            }
        }

        return Collections.emptyList();
    }

    private Map<String, Object> parseObject(ParseContext ctx, int baseIndent) {
        Map<String, Object> result = new LinkedHashMap<>();

        while (ctx.hasMore()) {
            String line = ctx.currentLine();
            int indent = getIndent(line);

            if (indent < baseIndent) break;
            if (indent > baseIndent) {
                ctx.advance();
                continue;
            }

            String trimmed = line.trim();
            if (trimmed.startsWith("- ")) break;

            int colonIdx = findUnquotedColon(trimmed);
            if (colonIdx == -1) {
                ctx.advance();
                continue;
            }

            String key = unquoteString(trimmed.substring(0, colonIdx).trim());
            String valueStr = trimmed.substring(colonIdx + 1).trim();

            if (valueStr.isEmpty()) {
                ctx.advance();
                if (ctx.hasMore() && getIndent(ctx.currentLine()) > indent) {
                    result.put(key, parseObject(ctx, indent + config.getIndent()));
                } else {
                    result.put(key, Collections.emptyMap());
                }
            } else if (valueStr.startsWith("[")) {
                Object arrayValue = parseArrayValue(ctx, valueStr, indent);
                result.put(key, arrayValue);
                ctx.advance();
            } else {
                result.put(key, parseScalar(valueStr));
                ctx.advance();
            }
        }

        return result;
    }

    private Object parseArrayValue(ParseContext ctx, String header, int baseIndent) {
        Matcher tabularMatcher = TABULAR_HEADER.matcher(header);
        if (tabularMatcher.matches()) {
            return parseTabularArray(ctx, baseIndent);
        }

        Matcher arrayMatcher = ARRAY_HEADER.matcher(header);
        if (arrayMatcher.matches()) {
            String rest = arrayMatcher.group(3);
            if (rest.startsWith(": ")) {
                return parseInlineArray(header);
            } else if (rest.equals(":")) {
                ctx.advance();
                return parseListArray(ctx, baseIndent);
            }
        }

        return Collections.emptyList();
    }

    private List<Object> parseInlineArray(String line) {
        int colonIdx = line.indexOf("]: ");
        if (colonIdx == -1) return Collections.emptyList();

        String content = line.substring(colonIdx + 3);
        if (content.isEmpty()) return Collections.emptyList();

        List<Object> result = new ArrayList<>();
        List<String> items = splitDelimited(content, config.getDelimiter());

        for (String item : items) {
            result.add(parseScalar(item.trim()));
        }

        return result;
    }

    private List<Map<String, Object>> parseTabularArray(ParseContext ctx, int baseIndent) {
        String header = ctx.currentLine().trim();
        Matcher matcher = TABULAR_HEADER.matcher(header);
        if (!matcher.matches()) return Collections.emptyList();

        String headersStr = matcher.group(3);
        List<String> headers = splitDelimited(headersStr, config.getDelimiter());
        for (int i = 0; i < headers.size(); i++) {
            headers.set(i, unquoteString(headers.get(i).trim()));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        ctx.advance();

        while (ctx.hasMore()) {
            String line = ctx.currentLine();
            int indent = getIndent(line);

            if (indent <= baseIndent) break;

            String trimmed = line.trim();
            if (trimmed.startsWith("- ")) break;

            List<String> values = splitDelimited(trimmed, config.getDelimiter());
            Map<String, Object> row = new LinkedHashMap<>();

            for (int i = 0; i < headers.size() && i < values.size(); i++) {
                row.put(headers.get(i), parseScalar(values.get(i).trim()));
            }

            result.add(row);
            ctx.advance();
        }

        return result;
    }

    private List<Object> parseListArray(ParseContext ctx, int baseIndent) {
        List<Object> result = new ArrayList<>();

        while (ctx.hasMore()) {
            String line = ctx.currentLine();
            int indent = getIndent(line);

            if (indent <= baseIndent) break;

            String trimmed = line.trim();
            if (!trimmed.startsWith("- ")) break;

            String content = trimmed.substring(2).trim();

            if (content.startsWith("[")) {
                Object item = parseArrayValue(ctx, content, indent);
                result.add(item);
                ctx.advance();
            } else if (content.contains(":")) {
                ctx.advance();
                result.add(parseObject(ctx, indent + config.getIndent()));
            } else {
                result.add(parseScalar(content));
                ctx.advance();
            }
        }

        return result;
    }

    private Object parseScalar(String value) {
        value = value.trim();

        if (value.equals("null")) return null;
        if (value.equals("true")) return true;
        if (value.equals("false")) return false;

        if (value.startsWith("\"") && value.endsWith("\"")) {
            return unescapeString(value.substring(1, value.length() - 1));
        }

        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private String unquoteString(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return unescapeString(s.substring(1, s.length() - 1));
        }
        return s;
    }

    private String unescapeString(String s) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;

        for (char c : s.toCharArray()) {
            if (escaped) {
                switch (c) {
                    case 'n': result.append('\n'); break;
                    case 'r': result.append('\r'); break;
                    case 't': result.append('\t'); break;
                    case '\\': result.append('\\'); break;
                    case '"': result.append('"'); break;
                    default: result.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private List<String> splitDelimited(String s, String delimiter) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (escaped) {
                current.append(c);
                escaped = false;
            } else if (c == '\\') {
                current.append(c);
                escaped = true;
            } else if (c == '"') {
                current.append(c);
                inQuotes = !inQuotes;
            } else if (!inQuotes && s.startsWith(delimiter, i)) {
                result.add(current.toString());
                current = new StringBuilder();
                i += delimiter.length() - 1;
            } else {
                current.append(c);
            }
        }

        result.add(current.toString());
        return result;
    }

    private int findUnquotedColon(String s) {
        boolean inQuotes = false;
        boolean escaped = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && c == ':') {
                return i;
            }
        }

        return -1;
    }

    private int getIndent(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else break;
        }
        return count;
    }

    private static class ParseContext {
        private final String[] lines;
        private final ToonConfig config;
        private int index;

        public ParseContext(String[] lines, ToonConfig config) {
            this.lines = lines;
            this.config = config;
            this.index = 0;
        }

        public boolean hasMore() {
            return index < lines.length;
        }

        public String currentLine() {
            return lines[index];
        }

        public void advance() {
            index++;
        }
    }
}
