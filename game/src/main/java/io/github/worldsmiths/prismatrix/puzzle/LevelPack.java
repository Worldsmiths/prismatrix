package io.github.worldsmiths.prismatrix.puzzle;

import io.github.worldsmiths.core.EngineException;
import io.github.worldsmiths.core.Files;
import io.github.worldsmiths.prismatrix.optics.OrbColorPalette;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Loads packaged level JSON via {@link Files#readString}. */
public final class LevelPack {

    private LevelPack() {}

    /**
     * Loads a level from an internal packaged path.
     *
     * @throws LevelLoadException if the path is missing, JSON is malformed, or fields are invalid
     */
    public static LevelDefinition load(Files files, String path) throws LevelLoadException {
        if (files == null) {
            throw new LevelLoadException("files must not be null");
        }
        if (path == null || path.isEmpty()) {
            throw new LevelLoadException("path must not be null or empty");
        }
        String json;
        try {
            json = files.readString(path);
        } catch (EngineException e) {
            throw new LevelLoadException("Missing level file: " + path, e);
        }
        try {
            return parseLevel(json);
        } catch (LevelLoadException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new LevelLoadException("Malformed level JSON: " + path, e);
        }
    }

    private static LevelDefinition parseLevel(String json) throws LevelLoadException {
        MinimalJson.ObjectValue root = MinimalJson.parseObject(json);
        String id = root.requireString("id");
        int gridSize = root.requireInt("gridSize");
        if (gridSize < 1) {
            throw new LevelLoadException("gridSize must be >= 1, got " + gridSize);
        }
        int mirrorBudget = root.requireInt("mirrorBudget");
        if (mirrorBudget < 0) {
            throw new LevelLoadException("mirrorBudget must be >= 0, got " + mirrorBudget);
        }

        int orb1 = optionalNonNegativeInt(root, "orb1");
        int orb2 = optionalNonNegativeInt(root, "orb2");
        int orb3 = optionalNonNegativeInt(root, "orb3");
        OrbColorPalette.Id orbColors = parseOrbColors(root, orb1, orb2, orb3);

        MinimalJson.ArrayValue goalsJson = root.requireArray("goals");
        List<GoalDefinition> goals = new ArrayList<GoalDefinition>();
        for (int i = 0; i < goalsJson.size(); i++) {
            MinimalJson.ObjectValue goalObj = goalsJson.requireObject(i);
            goals.add(parseGoal(goalObj, gridSize));
        }

        List<FixedPieceDefinition> fixedPieces = parseFixedPieces(root, gridSize);

        return new LevelDefinition(
                id, gridSize, mirrorBudget, orb1, orb2, orb3, orbColors, goals, fixedPieces);
    }

    private static int optionalNonNegativeInt(MinimalJson.ObjectValue root, String key)
            throws LevelLoadException {
        if (!root.has(key) || root.isNull(key)) {
            return 0;
        }
        int value = root.requireInt(key);
        if (value < 0) {
            throw new LevelLoadException(key + " must be >= 0, got " + value);
        }
        return value;
    }

    private static OrbColorPalette.Id parseOrbColors(
            MinimalJson.ObjectValue root, int orb1, int orb2, int orb3) throws LevelLoadException {
        boolean anyOrb = orb1 > 0 || orb2 > 0 || orb3 > 0;
        if (!anyOrb) {
            return null;
        }
        if (!root.has("orbColors") || root.isNull("orbColors")) {
            throw new LevelLoadException("orbColors is required when any orb count is > 0");
        }
        String raw = root.requireString("orbColors");
        try {
            return OrbColorPalette.Id.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new LevelLoadException("Unknown orbColors: " + raw, e);
        }
    }

    private static GoalDefinition parseGoal(MinimalJson.ObjectValue goalObj, int gridSize)
            throws LevelLoadException {
        String entryRaw = goalObj.requireString("entryPort");
        if (!goalObj.has("exitPort") || goalObj.isNull("exitPort")) {
            throw new LevelLoadException("exitPort is required");
        }
        String exitRaw = goalObj.requireString("exitPort");
        int colorMask = goalObj.requireInt("colorMask");
        Port entryPort = parsePort(entryRaw, gridSize, "entryPort");
        Port exitPort = parsePort(exitRaw, gridSize, "exitPort");
        return new GoalDefinition(entryPort, exitPort, colorMask);
    }

    private static List<FixedPieceDefinition> parseFixedPieces(
            MinimalJson.ObjectValue root, int gridSize) throws LevelLoadException {
        if (!root.has("fixedPieces") || root.isNull("fixedPieces")) {
            return new ArrayList<FixedPieceDefinition>();
        }
        MinimalJson.ArrayValue arr = root.requireArray("fixedPieces");
        List<FixedPieceDefinition> pieces = new ArrayList<FixedPieceDefinition>();
        Set<Long> seen = new HashSet<Long>();
        for (int i = 0; i < arr.size(); i++) {
            MinimalJson.ObjectValue obj = arr.requireObject(i);
            int x = obj.requireInt("x");
            int y = obj.requireInt("y");
            if (x < 0 || x >= gridSize || y < 0 || y >= gridSize) {
                throw new LevelLoadException(
                        "fixedPieces[" + i + "] out of bounds for gridSize " + gridSize);
            }
            long key = (((long) x) << 32) ^ (y & 0xffffffffL);
            if (!seen.add(Long.valueOf(key))) {
                throw new LevelLoadException(
                        "fixedPieces duplicate cell (" + x + "," + y + ")");
            }
            String typeRaw = obj.requireString("type");
            PieceType type = parseFixedType(typeRaw, i);
            pieces.add(new FixedPieceDefinition(x, y, type));
        }
        return pieces;
    }

    private static PieceType parseFixedType(String typeRaw, int index) throws LevelLoadException {
        if ("/".equals(typeRaw)) {
            return PieceType.FORWARD;
        }
        if ("\\".equals(typeRaw)) {
            return PieceType.BACK;
        }
        if ("ORB_1".equals(typeRaw)) {
            return PieceType.ORB_1;
        }
        if ("ORB_2".equals(typeRaw)) {
            return PieceType.ORB_2;
        }
        if ("ORB_3".equals(typeRaw)) {
            return PieceType.ORB_3;
        }
        throw new LevelLoadException(
                "fixedPieces[" + index + "] invalid type: " + typeRaw);
    }

    private static Port parsePort(String raw, int gridSize, String field)
            throws LevelLoadException {
        if (raw == null || raw.length() < 2) {
            throw new LevelLoadException("Invalid " + field + ": " + raw);
        }
        Port.Side side;
        try {
            side = Port.Side.valueOf(raw.substring(0, 1));
        } catch (IllegalArgumentException e) {
            throw new LevelLoadException("Invalid " + field + " side: " + raw, e);
        }
        int index;
        try {
            index = Integer.parseInt(raw.substring(1));
        } catch (NumberFormatException e) {
            throw new LevelLoadException("Invalid " + field + " index: " + raw, e);
        }
        try {
            return Port.of(side, index, gridSize);
        } catch (IllegalArgumentException e) {
            throw new LevelLoadException(
                    "Invalid " + field + " for gridSize " + gridSize + ": " + raw, e);
        }
    }

    /**
     * Tiny hand-rolled JSON reader for the locked M2 level schema. Not a general JSON library.
     */
    static final class MinimalJson {

        private MinimalJson() {}

        static ObjectValue parseObject(String json) throws LevelLoadException {
            Parser p = new Parser(json);
            ObjectValue value = p.parseObject();
            p.skipWhitespace();
            if (!p.eof()) {
                throw new LevelLoadException("Trailing content after JSON object");
            }
            return value;
        }

        static final class ObjectValue {
            private final java.util.Map<String, Object> fields;

            ObjectValue(java.util.Map<String, Object> fields) {
                this.fields = fields;
            }

            boolean has(String key) {
                return fields.containsKey(key);
            }

            boolean isNull(String key) {
                return fields.containsKey(key) && fields.get(key) == null;
            }

            String requireString(String key) throws LevelLoadException {
                Object v = require(key);
                if (!(v instanceof String)) {
                    throw new LevelLoadException("Expected string for \"" + key + "\"");
                }
                return (String) v;
            }

            int requireInt(String key) throws LevelLoadException {
                Object v = require(key);
                if (!(v instanceof Integer)) {
                    throw new LevelLoadException("Expected int for \"" + key + "\"");
                }
                return ((Integer) v).intValue();
            }

            ArrayValue requireArray(String key) throws LevelLoadException {
                Object v = require(key);
                if (!(v instanceof ArrayValue)) {
                    throw new LevelLoadException("Expected array for \"" + key + "\"");
                }
                return (ArrayValue) v;
            }

            private Object require(String key) throws LevelLoadException {
                if (!fields.containsKey(key)) {
                    throw new LevelLoadException("Missing field \"" + key + "\"");
                }
                Object v = fields.get(key);
                if (v == null) {
                    throw new LevelLoadException("Field \"" + key + "\" must not be null");
                }
                return v;
            }
        }

        static final class ArrayValue {
            private final List<Object> items;

            ArrayValue(List<Object> items) {
                this.items = items;
            }

            int size() {
                return items.size();
            }

            ObjectValue requireObject(int index) throws LevelLoadException {
                Object v = items.get(index);
                if (!(v instanceof ObjectValue)) {
                    throw new LevelLoadException("Expected object at index " + index);
                }
                return (ObjectValue) v;
            }
        }

        private static final class Parser {
            private final String s;
            private int i;

            Parser(String s) {
                this.s = s == null ? "" : s;
            }

            boolean eof() {
                return i >= s.length();
            }

            void skipWhitespace() {
                while (i < s.length()) {
                    char c = s.charAt(i);
                    if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                        i++;
                    } else {
                        break;
                    }
                }
            }

            ObjectValue parseObject() throws LevelLoadException {
                skipWhitespace();
                expect('{');
                java.util.LinkedHashMap<String, Object> fields =
                        new java.util.LinkedHashMap<String, Object>();
                skipWhitespace();
                if (peek('}')) {
                    i++;
                    return new ObjectValue(fields);
                }
                while (true) {
                    skipWhitespace();
                    String key = parseString();
                    skipWhitespace();
                    expect(':');
                    skipWhitespace();
                    Object value = parseValue();
                    fields.put(key, value);
                    skipWhitespace();
                    if (peek('}')) {
                        i++;
                        break;
                    }
                    expect(',');
                }
                return new ObjectValue(fields);
            }

            ArrayValue parseArray() throws LevelLoadException {
                expect('[');
                List<Object> items = new ArrayList<Object>();
                skipWhitespace();
                if (peek(']')) {
                    i++;
                    return new ArrayValue(items);
                }
                while (true) {
                    skipWhitespace();
                    items.add(parseValue());
                    skipWhitespace();
                    if (peek(']')) {
                        i++;
                        break;
                    }
                    expect(',');
                }
                return new ArrayValue(items);
            }

            Object parseValue() throws LevelLoadException {
                skipWhitespace();
                if (eof()) {
                    throw new LevelLoadException("Unexpected end of JSON");
                }
                char c = s.charAt(i);
                if (c == '{') {
                    return parseObject();
                }
                if (c == '[') {
                    return parseArray();
                }
                if (c == '"') {
                    return parseString();
                }
                if (c == 'n') {
                    return parseNull();
                }
                if (c == '-' || (c >= '0' && c <= '9')) {
                    return parseNumber();
                }
                throw new LevelLoadException("Unexpected character in JSON: " + c);
            }

            Object parseNull() throws LevelLoadException {
                if (i + 4 > s.length() || !s.regionMatches(i, "null", 0, 4)) {
                    throw new LevelLoadException("Expected null");
                }
                i += 4;
                return null;
            }

            Integer parseNumber() throws LevelLoadException {
                int start = i;
                if (peek('-')) {
                    i++;
                }
                if (eof() || s.charAt(i) < '0' || s.charAt(i) > '9') {
                    throw new LevelLoadException("Expected number");
                }
                while (!eof() && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                    i++;
                }
                // Reject fractions / exponents for this schema (ints only).
                if (!eof()) {
                    char c = s.charAt(i);
                    if (c == '.' || c == 'e' || c == 'E') {
                        throw new LevelLoadException("Expected integer, got float/exponent");
                    }
                }
                try {
                    return Integer.valueOf(s.substring(start, i));
                } catch (NumberFormatException e) {
                    throw new LevelLoadException("Invalid integer", e);
                }
            }

            String parseString() throws LevelLoadException {
                expect('"');
                StringBuilder sb = new StringBuilder();
                while (!eof()) {
                    char c = s.charAt(i++);
                    if (c == '"') {
                        return sb.toString();
                    }
                    if (c == '\\') {
                        if (eof()) {
                            throw new LevelLoadException("Unterminated escape in string");
                        }
                        char e = s.charAt(i++);
                        switch (e) {
                            case '"':
                            case '\\':
                            case '/':
                                sb.append(e);
                                break;
                            case 'b':
                                sb.append('\b');
                                break;
                            case 'f':
                                sb.append('\f');
                                break;
                            case 'n':
                                sb.append('\n');
                                break;
                            case 'r':
                                sb.append('\r');
                                break;
                            case 't':
                                sb.append('\t');
                                break;
                            case 'u':
                                if (i + 4 > s.length()) {
                                    throw new LevelLoadException("Bad unicode escape");
                                }
                                try {
                                    int code =
                                            Integer.parseInt(s.substring(i, i + 4), 16);
                                    sb.append((char) code);
                                    i += 4;
                                } catch (NumberFormatException ex) {
                                    throw new LevelLoadException("Bad unicode escape", ex);
                                }
                                break;
                            default:
                                throw new LevelLoadException("Bad escape: \\" + e);
                        }
                    } else {
                        sb.append(c);
                    }
                }
                throw new LevelLoadException("Unterminated string");
            }

            void expect(char expected) throws LevelLoadException {
                skipWhitespace();
                if (eof() || s.charAt(i) != expected) {
                    throw new LevelLoadException(
                            "Expected '"
                                    + expected
                                    + "', got "
                                    + (eof() ? "EOF" : String.valueOf(s.charAt(i))));
                }
                i++;
            }

            boolean peek(char expected) {
                return !eof() && s.charAt(i) == expected;
            }
        }
    }
}
