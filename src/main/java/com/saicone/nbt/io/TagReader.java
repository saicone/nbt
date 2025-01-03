package com.saicone.nbt.io;

import com.saicone.nbt.Tag;
import com.saicone.nbt.TagType;
import com.saicone.nbt.TagMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class TagReader<T> extends Reader {

    private static final int UNKNOWN_CHARACTER = -1;
    private static final Set<Character> NUMBER_SUFFIX = Set.of('b', 'B', 's', 'S', 'l', 'L', 'f', 'F', 'd', 'D');

    private final Reader reader;
    private final TagMapper<T> mapper;

    @NotNull
    public static TagReader<Object> of(@NotNull String s) {
        return of(s, TagMapper.DEFAULT);
    }

    @NotNull
    public static <T> TagReader<T> of(@NotNull String s, @NotNull TagMapper<T> mapper) {
        return of(new StringReader(s), mapper);
    }

    @NotNull
    public static TagReader<Object> of(@NotNull Reader reader) {
        return of(reader, TagMapper.DEFAULT);
    }

    @NotNull
    public static <T> TagReader<T> of(@NotNull Reader reader, @NotNull TagMapper<T> mapper) {
        return new TagReader<>(reader, mapper);
    }

    public TagReader(@NotNull Reader reader, @NotNull TagMapper<T> mapper) {
        this.reader = reader.markSupported() ? reader : new BufferedReader(reader);
        this.mapper = mapper;
    }

    protected boolean isQuote(int c) {
        return c == '"' || c == '\'';
    }

    protected boolean isUnquoted(int c) {
        return c >= '0' && c <= '9'
                || c >= 'A' && c <= 'Z'
                || c >= 'a' && c <= 'z'
                || c == '_' || c == '-'
                || c == '.' || c == '+';
    }

    protected boolean isLeadingSign(int c) {
        return c == '-' || c == '+';
    }

    @Nullable
    protected Boolean isNumber(@NotNull String s) {
        if (s.isBlank()) {
            return false;
        }
        boolean decimal = false;
        for (char c : (isLeadingSign(s.charAt(0)) ? s.substring(1) : s).toCharArray()) {
            if (!Character.isDigit(c)) {
                if (!decimal && c == '.') {
                    decimal = true;
                    continue;
                }
                return false;
            }
        }
        return decimal ? null : true;
    }

    @NotNull
    public Reader getReader() {
        return reader;
    }

    @NotNull
    public TagMapper<T> getMapper() {
        return mapper;
    }

    @Nullable
    public TagType<?> getType(char suffix) {
        switch (suffix) {
            case 'b':
            case 'B':
                return TagType.BYTE;
            case 's':
            case 'S':
                return TagType.SHORT;
            case 'l':
            case 'L':
                return TagType.LONG;
            case 'f':
            case 'F':
                return TagType.FLOAT;
            case 'd':
            case 'D':
                return TagType.DOUBLE;
            default:
                return null;
        }
    }

    @Nullable
    public TagType<?> getArrayType(char suffix) {
        switch (suffix) {
            case 'b':
            case 'B':
                return TagType.BYTE_ARRAY;
            case 'i':
            case 'I':
                return TagType.INT_ARRAY;
            case 'l':
            case 'L':
                return TagType.LONG_ARRAY;
            default:
                return null;
        }
    }

    @Nullable
    public <A extends T> A readTag() throws IOException {
        skipSpaces();
        mark(3);
        final int first = read();
        if (first == '{') {
            return readCompoundTag0();
        } else if (first == '[') {
            final int id = read();
            if (id == 'B' || id == 'I' || id == 'L') {
                final int separator = read();
                if (separator == ';') {
                    return readArrayTag(id);
                }
            }
            reset();
            return readListTag();
        } else {
            return readValueTag(first);
        }
    }

    @Nullable
    public <A extends T> A readValueTag() throws IOException {
        return readValueTag(read());
    }

    @Nullable
    protected <A extends T> A readValueTag(int first) throws IOException {
        if (first == UNKNOWN_CHARACTER) {
            return null;
        }
        if (isQuote(first)) {
            return readQuotedTag(first);
        } else if (isUnquoted(first)) {
            return readUnquotedTag(first);
        } else {
            return null;
        }
    }

    @NotNull
    public String readKey() throws IOException {
        mark(1);
        final int first = read();
        if (first == UNKNOWN_CHARACTER) {
            return "";
        }
        if (isQuote(first)) {
            return readQuoted(first);
        } else if (isUnquoted(first)) {
            return readUnquoted(first);
        } else {
            reset();
            return "";
        }
    }

    @NotNull
    public <A extends T> A readUnquotedTag() throws IOException {
        final int first = read();
        if (isUnquoted(first)) {
            return readUnquotedTag(first);
        }
        throw new IOException("Cannot read '" + first + "' as quoted tag");
    }

    @NotNull
    protected <A extends T> A readUnquotedTag(int first) throws IOException {
        String unquoted = readUnquoted(first);

        final char last = unquoted.charAt(unquoted.length() - 1);

        final Boolean result;
        final TagType<?> type;
        if (unquoted.length() > 1 && NUMBER_SUFFIX.contains(last)) { // Number with suffix
            final String s = unquoted.substring(0, unquoted.length() - 1);
            result = isNumber(s);
            if (!Boolean.FALSE.equals(result)) {
                unquoted = s;
                type = getType(last);
            } else {
                type = null;
            }
        } else if ((result = isNumber(unquoted)) == null) { // Decimal
            type = TagType.DOUBLE;
        } else if (result) { // Integer
            type = TagType.INT;
        } else if (unquoted.equals("true")) { // boolean
            return mapper.buildAny(TagType.BYTE, (byte) 1);
        } else if (unquoted.equals("false")) { // boolean
            return mapper.buildAny(TagType.BYTE, (byte) 0);
        } else {
            type = null;
        }

        if (type != null) {
            if (result == null && !type.isDecimal()) {
                throw new IOException("Cannot read decimal number '" + unquoted + "' as " + type.getPrettyName());
            }
            switch (type.getId()) {
                case Tag.BYTE:
                    return mapper.buildAny(TagType.BYTE, Byte.parseByte(unquoted));
                case Tag.SHORT:
                    return mapper.buildAny(TagType.SHORT, Short.parseShort(unquoted));
                case Tag.INT:
                    return mapper.buildAny(TagType.INT, Integer.parseInt(unquoted));
                case Tag.LONG:
                    return mapper.buildAny(TagType.LONG, Long.parseLong(unquoted));
                case Tag.FLOAT:
                    return mapper.buildAny(TagType.FLOAT, Float.parseFloat(unquoted));
                case Tag.DOUBLE:
                    return mapper.buildAny(TagType.DOUBLE, Double.parseDouble(unquoted));
            }
        }
        return mapper.buildAny(TagType.STRING, unquoted);
    }

    @NotNull
    protected String readUnquoted() throws IOException {
        skipSpaces();
        final int first = read();
        if (first == UNKNOWN_CHARACTER || !isUnquoted(first)) {
            throw new IOException("Cannot read unquoted string");
        }
        return readUnquoted(first);
    }

    @NotNull
    protected String readUnquoted(int first) throws IOException {
        final StringBuilder builder = new StringBuilder();
        builder.append((char) first);
        mark(1);
        int i;
        while ((i = read()) != -1) {
            if (isUnquoted(i)) {
                mark(1);
                builder.append((char) i);
            } else {
                break;
            }
        }
        reset();
        return builder.toString();
    }

    @NotNull
    public <A extends T> A readQuotedTag() throws IOException {
        final int first = read();
        if (isQuote(first)) {
            return readQuotedTag(first);
        }
        throw new IOException("Cannot read quoted tag without leading quote character");
    }

    @NotNull
    protected <A extends T> A readQuotedTag(int quote) throws IOException {
        return mapper.buildAny(TagType.STRING, readQuoted(quote));
    }

    @NotNull
    protected String readQuoted(int quote) throws IOException {
        final StringBuilder builder = new StringBuilder();
        boolean escape = false;
        int i;
        while ((i = read()) != UNKNOWN_CHARACTER) {
            if (i == quote) {
                if (escape) {
                    escape = false;
                    builder.setCharAt(builder.length() - 1, (char) i);
                } else {
                    return builder.toString();
                }
            } else if (i == '\\') {
                escape = true;
            }
            builder.append((char) i);
        }
        throw new IOException("Non closed quoted string: " + builder);
    }

    @NotNull
    public <A extends T> A readArrayTag() throws IOException {
        if (read() != '[') {
            throw new IOException("Array tag must start with '['");
        }
        final int id = read();
        if (id == UNKNOWN_CHARACTER) {
            throw new IOException("Array tag must have id");
        }
        if (read() != ';') {
            throw new IOException("Array tag must have ';' after id");
        }
        return readArrayTag(id);
    }

    @NotNull
    protected <A extends T> A readArrayTag(int id) throws IOException {
        final TagType<?> type = getArrayType((char) id);
        if (type != null) {
            switch (type.getId()) {
                case Tag.BYTE_ARRAY:
                    return readArrayTag(type, true, Byte::parseByte, mapper::byteArray);
                case Tag.INT_ARRAY:
                    return readArrayTag(type, false, Integer::parseInt, mapper::intArray);
                case Tag.LONG_ARRAY:
                    return readArrayTag(type, true, Long::parseLong, mapper::longArray);
            }
        }
        throw new IOException("Cannot read invalid tag array for suffix: " + (char) id);
    }

    @NotNull
    protected <A extends T> A readArrayTag(@NotNull TagType<?> type, boolean suffix, @NotNull Function<String, Object> valueFunction, @NotNull Function<List<Object>, Object> arrayFunction) throws IOException {
        final List<Object> array = new ArrayList<>();
        String unquoted;
        while (!(unquoted = readUnquoted()).isEmpty()) {
            if (suffix) {
                final char last = unquoted.charAt(unquoted.length() - 1);
                if (last == type.getSuffix() || last == Character.toLowerCase(type.getSuffix())) {
                    unquoted = unquoted.substring(0, unquoted.length() - 1);
                }
            }
            if (unquoted.equals("true")) {
                array.add(valueFunction.apply("1"));
            } else if (unquoted.equals("false")) {
                array.add(valueFunction.apply("0"));
            } else {
                array.add(valueFunction.apply(unquoted));
            }
            if (skip(',')) {
                skipSpaces();
            } else {
                break;
            }
        }
        if (!skip(']')) {
            throw new IOException("Array tag must end with ']': " + array);
        }
        return mapper.buildAny(type, arrayFunction.apply(array));
    }

    @NotNull
    public <A extends T> A readListTag() throws IOException {
        if (read() != '[') {
            throw new IOException("List tag must start with '['");
        }
        final List<T> list = new ArrayList<>();
        T value;
        while ((value = readTag()) != null) {
            list.add(value);
            if (skip(',')) {
                skipSpaces();
            } else {
                break;
            }
        }
        if (!skip(']')) {
            throw new IOException("List tag must end with ']': " + list);
        }
        return mapper.buildAny(TagType.LIST, list);
    }

    @NotNull
    public <A extends T> A readCompoundTag() throws IOException {
        if (read() != '{') {
            throw new IOException("Compound tag must start with '{'");
        }
        return readCompoundTag0();
    }

    @NotNull
    protected <A extends T> A readCompoundTag0() throws IOException {
        final Map<String, T> map = new HashMap<>();
        String key;
        while (!(key = readKey()).isEmpty()) {
            if (!skip(':')) {
                throw new IOException("Compound key must have colon separator: " + key);
            }
            final T value = readTag();
            if (value == null) {
                break;
            }
            map.put(key, value);
            if (skip(',')) {
                skipSpaces();
            } else {
                break;
            }
        }
        if (!skip('}')) {
            throw new IOException("Compound tag must end with '}': " + map);
        }
        return mapper.buildAny(TagType.COMPOUND, map);
    }

    @Override
    public int read() throws IOException {
        return reader.read();
    }

    @Override
    public int read(@NotNull char[] cbuf, int off, int len) throws IOException {
        return reader.read(cbuf, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return reader.skip(n);
    }

    public boolean skip(char c) throws IOException {
        mark(1);
        int i;
        while (Character.isWhitespace((i = read()))) {
            mark(1);
        }
        if (i != c) {
            reset();
            return false;
        }
        return true;
    }

    public long skipSpaces() throws IOException {
        mark(1);
        long count = 0;
        while (Character.isWhitespace(read())) {
            count++;
            mark(1);
        }
        reset();
        return count;
    }

    @Override
    public boolean ready() throws IOException {
        return reader.ready();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        reader.mark(readAheadLimit);
    }

    @Override
    public void reset() throws IOException {
        reader.reset();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    public static <A> A fromString(@NotNull String s) {
        return fromString(s, TagMapper.DEFAULT);
    }

    public static <T, A extends T> A fromString(@NotNull String s, @NotNull TagMapper<T> mapper) {
        try (StringReader reader = new StringReader(s); TagReader<T> tagReader = new TagReader<>(reader, mapper)) {
            return tagReader.readTag();
        } catch (IOException e) {
            throw new RuntimeException("Cannot read object from SNBT", e);
        }
    }
}
