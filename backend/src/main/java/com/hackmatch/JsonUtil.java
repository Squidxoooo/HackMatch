package com.hackmatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Small JSON reader/writer for this project. It supports objects, arrays, strings,
 * numbers, booleans, and null, which is all the HTTP API needs.
 */
public final class JsonUtil {
    private JsonUtil() { }

    public static String toJson(Object value) {
        StringBuilder json = new StringBuilder();
        writeValue(json, value);
        return json.toString();
    }

    public static Map<String, Object> parseObject(String json) {
        Object value = new Parser(json).parse();
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("JSON request body must be an object.");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> object = (Map<String, Object>) value;
        return object;
    }

    private static void writeValue(StringBuilder json, Object value) {
        if (value == null) {
            json.append("null");
        } else if (value instanceof String) {
            writeString(json, (String) value);
        } else if (value instanceof Number || value instanceof Boolean) {
            json.append(value);
        } else if (value instanceof Map) {
            json.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first) json.append(',');
                writeString(json, String.valueOf(entry.getKey()));
                json.append(':');
                writeValue(json, entry.getValue());
                first = false;
            }
            json.append('}');
        } else if (value instanceof Iterable) {
            json.append('[');
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) json.append(',');
                writeValue(json, item);
                first = false;
            }
            json.append(']');
        } else {
            writeString(json, String.valueOf(value));
        }
    }

    private static void writeString(StringBuilder json, String value) {
        json.append('"');
        for (char character : value.toCharArray()) {
            switch (character) {
                case '"': json.append("\\\""); break;
                case '\\': json.append("\\\\"); break;
                case '\b': json.append("\\b"); break;
                case '\f': json.append("\\f"); break;
                case '\n': json.append("\\n"); break;
                case '\r': json.append("\\r"); break;
                case '\t': json.append("\\t"); break;
                default:
                    if (character < 0x20) json.append(String.format("\\u%04x", (int) character));
                    else json.append(character);
            }
        }
        json.append('"');
    }

    private static class Parser {
        private final String text;
        private int position;

        Parser(String text) {
            this.text = text == null ? "" : text;
        }

        Object parse() {
            skipWhitespace();
            Object value = readValue();
            skipWhitespace();
            if (position != text.length()) throw error("Unexpected text after JSON value");
            return value;
        }

        private Object readValue() {
            skipWhitespace();
            if (position >= text.length()) throw error("Expected a JSON value");
            char next = text.charAt(position);
            if (next == '{') return readObject();
            if (next == '[') return readArray();
            if (next == '"') return readString();
            if (next == 't') return readLiteral("true", Boolean.TRUE);
            if (next == 'f') return readLiteral("false", Boolean.FALSE);
            if (next == 'n') return readLiteral("null", null);
            if (next == '-' || Character.isDigit(next)) return readNumber();
            throw error("Invalid JSON value");
        }

        private Map<String, Object> readObject() {
            Map<String, Object> object = new java.util.LinkedHashMap<>();
            position++;
            skipWhitespace();
            if (consume('}')) return object;
            while (true) {
                skipWhitespace();
                if (position >= text.length() || text.charAt(position) != '"') throw error("Expected an object key");
                String key = readString();
                skipWhitespace();
                expect(':');
                object.put(key, readValue());
                skipWhitespace();
                if (consume('}')) return object;
                expect(',');
            }
        }

        private List<Object> readArray() {
            List<Object> array = new ArrayList<>();
            position++;
            skipWhitespace();
            if (consume(']')) return array;
            while (true) {
                array.add(readValue());
                skipWhitespace();
                if (consume(']')) return array;
                expect(',');
            }
        }

        private String readString() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (position < text.length()) {
                char character = text.charAt(position++);
                if (character == '"') return value.toString();
                if (character != '\\') {
                    value.append(character);
                    continue;
                }
                if (position >= text.length()) throw error("Invalid string escape");
                char escaped = text.charAt(position++);
                switch (escaped) {
                    case '"': value.append('"'); break;
                    case '\\': value.append('\\'); break;
                    case '/': value.append('/'); break;
                    case 'b': value.append('\b'); break;
                    case 'f': value.append('\f'); break;
                    case 'n': value.append('\n'); break;
                    case 'r': value.append('\r'); break;
                    case 't': value.append('\t'); break;
                    case 'u': value.append(readUnicode()); break;
                    default: throw error("Invalid string escape");
                }
            }
            throw error("Unclosed string");
        }

        private char readUnicode() {
            if (position + 4 > text.length()) throw error("Invalid Unicode escape");
            String hex = text.substring(position, position + 4);
            position += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException exception) {
                throw error("Invalid Unicode escape");
            }
        }

        private Object readLiteral(String expected, Object value) {
            if (!text.startsWith(expected, position)) throw error("Invalid JSON literal");
            position += expected.length();
            return value;
        }

        private Number readNumber() {
            int start = position;
            if (consume('-')) { }
            consumeDigits();
            if (consume('.')) consumeDigits();
            if (position < text.length() && (text.charAt(position) == 'e' || text.charAt(position) == 'E')) {
                position++;
                if (position < text.length() && (text.charAt(position) == '+' || text.charAt(position) == '-')) position++;
                consumeDigits();
            }
            String number = text.substring(start, position);
            try {
                return number.contains(".") || number.contains("e") || number.contains("E")
                        ? Double.parseDouble(number) : Long.parseLong(number);
            } catch (NumberFormatException exception) {
                throw error("Invalid number");
            }
        }

        private void consumeDigits() {
            int start = position;
            while (position < text.length() && Character.isDigit(text.charAt(position))) position++;
            if (start == position) throw error("Expected a digit");
        }

        private boolean consume(char expected) {
            if (position < text.length() && text.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!consume(expected)) throw error("Expected '" + expected + "'");
        }

        private void skipWhitespace() {
            while (position < text.length() && Character.isWhitespace(text.charAt(position))) position++;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at character " + position + ".");
        }
    }
}
