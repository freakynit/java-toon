package com.freakynit.toon;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

public class ToonEncoder {
    private static final Pattern SAFE_STRING_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?$");
    private static final Pattern LEADING_ZERO_PATTERN = Pattern.compile("^0\\d+$");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    private final ToonConfig config;

    public ToonEncoder() {
        this(new ToonConfig());
    }

    public ToonEncoder(ToonConfig config) {
        this.config = config;
    }

    public String encode(Object data) {
        StringBuilder sb = new StringBuilder();
        encodeValue(data, sb, 0, false);
        return sb.toString();
    }

    private void encodeValue(Object value, StringBuilder sb, int depth, boolean isListItem) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Boolean) {
            sb.append(value.toString().toLowerCase());
        } else if (value instanceof Number) {
            encodeNumber((Number) value, sb);
        } else if (value instanceof String) {
            encodeString((String) value, sb, depth);
        } else if (value instanceof Date) {
            encodeDate((Date) value, sb);
        } else if (value instanceof Map) {
            encodeMap((Map<?, ?>) value, sb, depth, isListItem);
        } else if (value instanceof List) {
            encodeList((List<?>) value, sb, depth, isListItem);
        } else {
            sb.append("null");
        }
    }

    private void encodeNumber(Number num, StringBuilder sb) {
        if (num instanceof Double || num instanceof Float) {
            double d = num.doubleValue();
            if (Double.isInfinite(d) || Double.isNaN(d)) {
                sb.append("null");
                return;
            }
            if (d == (long) d) {
                sb.append((long) d);
            } else {
                sb.append(d);
            }
        } else {
            sb.append(num.toString());
        }
    }

    private void encodeString(String str, StringBuilder sb, int depth) {
        if (needsQuoting(str)) {
            sb.append('"').append(escapeString(str)).append('"');
        } else {
            sb.append(str);
        }
    }

    private boolean needsQuoting(String str) {
        if (str.isEmpty()) return true;
        if (str.trim().length() != str.length()) return true;
        if ("true".equals(str) || "false".equals(str) || "null".equals(str)) return true;
        if (NUMERIC_PATTERN.matcher(str).matches() || LEADING_ZERO_PATTERN.matcher(str).matches()) return true;
        if (str.contains(config.getDelimiter()) || str.contains(":") || str.contains("[") ||
                str.contains("]") || str.contains("{") || str.contains("}") || str.startsWith("-")) return true;
        if (!SAFE_STRING_PATTERN.matcher(str).matches()) {
            for (char c : str.toCharArray()) {
                if (Character.isISOControl(c)) return true;
            }
        }
        return false;
    }

    private String escapeString(String str) {
        StringBuilder result = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '\\': result.append("\\\\"); break;
                case '"': result.append("\\\""); break;
                case '\n': result.append("\\n"); break;
                case '\r': result.append("\\r"); break;
                case '\t':
                    if (!"\t".equals(config.getDelimiter())) {
                        result.append("\\t");
                    } else {
                        result.append(c);
                    }
                    break;
                default: result.append(c);
            }
        }
        return result.toString();
    }

    private void encodeDate(Date date, StringBuilder sb) {
        sb.append('"').append(ISO_FORMATTER.format(date.toInstant())).append('"');
    }

    private void encodeMap(Map<?, ?> map, StringBuilder sb, int depth, boolean isListItem) {
        if (map.isEmpty()) {
            return;
        }

        List<String> keys = new ArrayList<>();
        for (Object key : map.keySet()) {
            keys.add(key.toString());
        }

        boolean first = true;
        for (String key : keys) {
            if (!first) {
                sb.append('\n').append(indent(depth));
            }
            first = false;

            encodeKey(key, sb);
            sb.append(": ");

            Object value = map.get(key);
            if (value instanceof Map && !((Map<?, ?>) value).isEmpty()) {
                sb.append('\n').append(indent(depth + 1));
                encodeValue(value, sb, depth + 1, false);
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                if (list.isEmpty()) {
                    sb.append('[').append(config.getLengthMarker()).append("0]:");
                } else if (shouldUseTabularFormat(list)) {
                    encodeTabularArray(key, list, sb, depth);
                } else if (isHomogeneousPrimitives(list)) {
                    encodeInlineArray(list, sb, depth);
                } else {
                    sb.append('[').append(config.getLengthMarker()).append(list.size())
                            .append(config.getDelimiterDisplay()).append("]:");
                    for (Object item : list) {
                        sb.append('\n').append(indent(depth + 1));
                        if (item instanceof List || item instanceof Map) {
                            sb.append("- ");
                            encodeValue(item, sb, depth + 2, true);
                        } else {
                            sb.append("- ");
                            encodeValue(item, sb, depth + 1, false);
                        }
                    }
                }
            } else {
                encodeValue(value, sb, depth, false);
            }
        }
    }

    private void encodeKey(String key, StringBuilder sb) {
        if (keyNeedsQuoting(key)) {
            sb.append('"').append(escapeString(key)).append('"');
        } else {
            sb.append(key);
        }
    }

    private boolean keyNeedsQuoting(String key) {
        if (key.isEmpty()) return true;
        if (key.trim().length() != key.length()) return true;
        if (key.startsWith("-")) return true;
        if (NUMERIC_PATTERN.matcher(key).matches()) return true;
        if (key.contains(":") || key.contains("[") || key.contains("]") ||
                key.contains("{") || key.contains("}") || key.contains(config.getDelimiter())) return true;
        for (char c : key.toCharArray()) {
            if (Character.isISOControl(c)) return true;
        }
        return false;
    }

    private void encodeList(List<?> list, StringBuilder sb, int depth, boolean isListItem) {
        if (list.isEmpty()) {
            sb.append('[').append(config.getLengthMarker()).append("0]:");
            return;
        }

        if (shouldUseTabularFormat(list)) {
            encodeTabularArrayRoot(list, sb, depth);
        } else if (isHomogeneousPrimitives(list)) {
            encodeInlineArrayRoot(list, sb);
        } else {
            sb.append('[').append(config.getLengthMarker()).append(list.size())
                    .append(config.getDelimiterDisplay()).append("]:");
            for (Object item : list) {
                sb.append('\n').append(indent(depth));
                if (item instanceof List || item instanceof Map) {
                    sb.append("- ");
                    encodeValue(item, sb, depth + 1, true);
                } else {
                    sb.append("- ");
                    encodeValue(item, sb, depth, false);
                }
            }
        }
    }

    private boolean shouldUseTabularFormat(List<?> list) {
        if (list.isEmpty()) return false;

        Set<String> firstKeys = null;
        for (Object item : list) {
            if (!(item instanceof Map)) return false;
            Map<?, ?> map = (Map<?, ?>) item;
            if (map.isEmpty()) return false;

            Set<String> keys = new LinkedHashSet<>();
            for (Object key : map.keySet()) {
                keys.add(key.toString());
                Object value = map.get(key);
                if (value instanceof Map || value instanceof List) {
                    return false;
                }
            }

            if (firstKeys == null) {
                firstKeys = keys;
            } else if (!firstKeys.equals(keys)) {
                return false;
            }
        }
        return true;
    }

    private boolean isHomogeneousPrimitives(List<?> list) {
        for (Object item : list) {
            if (item instanceof Map || item instanceof List) {
                return false;
            }
        }
        return true;
    }

    private void encodeInlineArray(List<?> list, StringBuilder sb, int depth) {
        sb.append('[').append(config.getLengthMarker()).append(list.size())
                .append(config.getDelimiterDisplay()).append("]: ");
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(config.getDelimiter());
            first = false;
            if (item instanceof String && needsQuoting((String) item)) {
                sb.append('"').append(escapeString((String) item)).append('"');
            } else {
                encodeValue(item, sb, depth, false);
            }
        }
    }

    private void encodeInlineArrayRoot(List<?> list, StringBuilder sb) {
        sb.append('[').append(config.getLengthMarker()).append(list.size())
                .append(config.getDelimiterDisplay()).append("]: ");
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(config.getDelimiter());
            first = false;
            if (item instanceof String && needsQuoting((String) item)) {
                sb.append('"').append(escapeString((String) item)).append('"');
            } else {
                encodeValue(item, sb, 0, false);
            }
        }
    }

    private void encodeTabularArray(String key, List<?> list, StringBuilder sb, int depth) {
        Map<?, ?> firstMap = (Map<?, ?>) list.get(0);
        List<String> headers = new ArrayList<>();
        for (Object k : firstMap.keySet()) {
            headers.add(k.toString());
        }

        sb.append('[').append(config.getLengthMarker()).append(list.size())
                .append(config.getDelimiterDisplay()).append("]{");
        for (int i = 0; i < headers.size(); i++) {
            if (i > 0) sb.append(config.getDelimiter());
            String header = headers.get(i);
            if (keyNeedsQuoting(header)) {
                sb.append('"').append(escapeString(header)).append('"');
            } else {
                sb.append(header);
            }
        }
        sb.append("}:");

        for (Object item : list) {
            sb.append('\n').append(indent(depth + 1));
            Map<?, ?> map = (Map<?, ?>) item;
            for (int i = 0; i < headers.size(); i++) {
                if (i > 0) sb.append(config.getDelimiter());
                Object value = map.get(headers.get(i));
                if (value instanceof String && needsQuoting((String) value)) {
                    sb.append('"').append(escapeString((String) value)).append('"');
                } else {
                    encodeValue(value, sb, depth + 1, false);
                }
            }
        }
    }

    private void encodeTabularArrayRoot(List<?> list, StringBuilder sb, int depth) {
        Map<?, ?> firstMap = (Map<?, ?>) list.get(0);
        List<String> headers = new ArrayList<>();
        for (Object k : firstMap.keySet()) {
            headers.add(k.toString());
        }

        sb.append('[').append(config.getLengthMarker()).append(list.size())
                .append(config.getDelimiterDisplay()).append("]{");
        for (int i = 0; i < headers.size(); i++) {
            if (i > 0) sb.append(config.getDelimiter());
            String header = headers.get(i);
            if (keyNeedsQuoting(header)) {
                sb.append('"').append(escapeString(header)).append('"');
            } else {
                sb.append(header);
            }
        }
        sb.append("}:");

        for (Object item : list) {
            sb.append('\n').append(indent(depth));
            Map<?, ?> map = (Map<?, ?>) item;
            for (int i = 0; i < headers.size(); i++) {
                if (i > 0) sb.append(config.getDelimiter());
                Object value = map.get(headers.get(i));
                if (value instanceof String && needsQuoting((String) value)) {
                    sb.append('"').append(escapeString((String) value)).append('"');
                } else {
                    encodeValue(value, sb, depth, false);
                }
            }
        }
    }

    private String indent(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth * config.getIndent(); i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
