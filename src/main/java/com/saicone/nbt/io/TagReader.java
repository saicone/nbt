package com.saicone.nbt.io;

import com.saicone.nbt.Tag;
import com.saicone.nbt.TagType;
import com.saicone.nbt.TagMapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Reads SNBT formated tag objects from delegated reader.<br>
 * The compatible format aims to be the same as Minecraft.
 *
 * @author Rubenicos
 *
 * @param <T> the tag object implementation.
 */
public class TagReader<T> extends Reader {

    public static final int LATEST = 3;
    public static final int V1_21_5 = 3;
    public static final int V1_14 = 2;
    public static final int V1_0 = 1;

    private static final int UNKNOWN_CHARACTER = -1;

    private final Reader reader;
    private final TagMapper<T> mapper;

    private transient int version = LATEST;

    /**
     * Create a tag reader that create nbt-represented java objects with provided string to read.
     *
     * @param s the string to read.
     * @return  a newly generated tag reader.
     */
    @NotNull
    public static TagReader<Object> of(@NotNull String s) {
        return of(s, TagMapper.DEFAULT);
    }

    /**
     * Create a tag reader with provided string to read and {@link TagMapper}.
     *
     * @param s      the string to read.
     * @param mapper the mapper to create tag objects by providing a value.
     * @return       a newly generated tag reader.
     * @param <T>    the tag object implementation
     */
    @NotNull
    public static <T> TagReader<T> of(@NotNull String s, @NotNull TagMapper<T> mapper) {
        return of(new StringReader(s), mapper);
    }

    /**
     * Create a tag reader that create nbt-represented java objects with provided {@link Reader}.
     *
     * @param reader the delegated reader to read characters.
     * @return       a newly generated tag reader.
     */
    @NotNull
    public static TagReader<Object> of(@NotNull Reader reader) {
        return of(reader, TagMapper.DEFAULT);
    }

    /**
     * Create a tag reader with provided {@link Reader} and {@link TagMapper}.
     *
     * @param reader the delegated reader to read characters.
     * @param mapper the mapper to create tag objects by providing a value.
     * @return       a newly generated tag reader.
     * @param <T>    the tag object implementation
     */
    @NotNull
    public static <T> TagReader<T> of(@NotNull Reader reader, @NotNull TagMapper<T> mapper) {
        return new TagReader<>(reader, mapper);
    }

    /**
     * Constructs a tag writer with provided {@link Reader} and {@link TagMapper}.
     *
     * @param reader the delegated reader to read characters.
     * @param mapper the mapper to create tag objects by providing a value.
     */
    public TagReader(@NotNull Reader reader, @NotNull TagMapper<T> mapper) {
        this.reader = reader.markSupported() ? reader : new BufferedReader(reader);
        this.mapper = mapper;
    }

    @NotNull
    @Contract("_ -> this")
    public TagReader<T> version(int version) {
        this.version = version;
        return this;
    }

    /**
     * Check if provided char is a quote.
     *
     * @param c the char to check.
     * @return  true if the char is a quote, false otherwise.
     */
    protected boolean isQuote(int c) {
        return c == '"' || (version >= V1_14 && c == '\'');
    }

    /**
     * Check if provided char is an unquoted character.
     *
     * @param c the char to check.
     * @return  true if the char is unquoted, false otherwise.
     */
    protected boolean isUnquoted(int c) {
        return c >= '0' && c <= '9'
                || c >= 'A' && c <= 'Z'
                || c >= 'a' && c <= 'z'
                || c == '_' || c == '-'
                || c == '.' || c == '+';
    }

    /**
     * Check if provided char is a number leading sign.
     *
     * @param c the char to check.
     * @return  true if the char is a leading sign, false otherwise.
     */
    protected boolean isLeadingSign(int c) {
        return c == '-' || c == '+';
    }

    /**
     * Check if the provided string is a integer number and also a decimal number.
     *
     * @param s the string to check.
     * @return  true for integer number, false for decimal number, null otherwise.
     */
    @Nullable
    protected Boolean isNumber(@NotNull String s) {
        if (s.isBlank()) {
            return false;
        }
        return isNumber(s, 0, s.length());
    }

    @Nullable
    protected Boolean isNumber(@NotNull String s, int start, int end) {
        final int result = checkInteger(s, start, end);
        if (result == end) {
            return true;
        } else if (checkDecimal(s, result, end) == end) {
            return false;
        } else {
            return null;
        }
    }

    protected boolean isNumberSuffix(char c) {
        return isIntegerSuffix(c) || isDecimalSuffix(c);
    }

    protected boolean isIntegerSuffix(char c) {
        switch (c) {
            case 'b':
            case 'B':
            case 's':
            case 'S':
            case 'i':
            case 'I':
            case 'l':
            case 'L':
                return true;
            default:
                return false;
        }
    }

    protected boolean isDecimalSuffix(char c) {
        switch (c) {
            case 'f':
            case 'F':
            case 'd':
            case 'D':
                return true;
            default:
                return false;
        }
    }

    protected boolean isSignednessSuffix(char c) {
        switch (c) {
            case 'u':
            case 'U':
            case 's':
            case 'S':
                return true;
            default:
                return false;
        }
    }

    private boolean isCodePoint(int c) {
        return c >= '0' && c <= '9'
                || c >= 'A' && c <= 'Z'
                || c >= 'a' && c <= 'z'
                || c == '_' || c == '-'
                || c == ' ';
    }

    protected int checkInteger(@NotNull String s, int start, int end) {
        if (isLeadingSign(s.charAt(start))) {
            start++;
        }
        for (int i = start; i < end; i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return i;
            }
        }
        return end;
    }

    protected int checkDecimal(@NotNull String s, int start, int end) {
        boolean decimal = false;
        for (int i = start; i < end; i++) {
            final char c = s.charAt(i);
            if (!Character.isDigit(c)) {
                if (decimal) {
                    // Check for E notation
                    if ((c == 'e' || c == 'E') && version >= V1_21_5 && i + 1 < end) {
                        return checkInteger(s, i + 1, end);
                    }
                } else if (c == '.') {
                    decimal = true;
                    continue;
                }
                return end;
            }
        }
        return end;
    }

    /**
     * Get the delegated reader.
     *
     * @return a reader that is used to read character.
     */
    @NotNull
    public Reader getReader() {
        return reader;
    }

    /**
     * Get the mapper that is used to create tag objects.
     *
     * @return a tag mapper.
     */
    @NotNull
    public TagMapper<T> getMapper() {
        return mapper;
    }

    /**
     * Get associated tag type with provided suffix.<br>
     * This method will only return single-value tag types.
     *
     * @param suffix the suffix to compare.
     * @return       a tag type if the suffix is valid, null otherwise.
     */
    @Nullable
    public TagType<?> getType(char suffix) {
        switch (suffix) {
            case 'b':
            case 'B':
                return TagType.BYTE;
            case 's':
            case 'S':
                return TagType.SHORT;
            case 'i':
            case 'I':
                return version >= V1_21_5 ? TagType.INT : null;
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

    /**
     * Get associated array tag type with provided suffix.<br>
     * This method will only return array-value tag types.
     *
     * @param suffix the suffix to compare.
     * @return       a tag type if the suffix is valid, null otherwise.
     */
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

    /**
     * Read tag object.
     *
     * @return    a tag object if it's applicable, null otherwise.
     * @param <A> the implementation of tag object.
     * @throws IOException if any I/O error occurs.
     */
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

    /**
     * Read single-value tag object.
     *
     * @return    a tag object if it's applicable, null otherwise.
     * @param <A> the implementation of tag object.
     * @throws IOException if any I/O error occurs.
     */
    @Nullable
    public <A extends T> A readValueTag() throws IOException {
        return readValueTag(read());
    }

    /**
     * Read single-value tag object.
     *
     * @param first the first character to check.
     * @return      a tag object if it's applicable, null otherwise.
     * @param <A>   the implementation of tag object.
     * @throws IOException if any I/O error occurs.
     */
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

    /**
     * Read compound key.
     *
     * @return a string value key.
     * @throws IOException if any I/O error occurs.
     */
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

    /**
     * Read single-value unquoted tag object.
     *
     * @return    a tag object.
     * @param <A> the implementation of tag object.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    public <A extends T> A readUnquotedTag() throws IOException {
        final int first = read();
        if (isUnquoted(first)) {
            return readUnquotedTag(first);
        }
        throw new IOException("Cannot read '" + first + "' as quoted tag");
    }

    /**
     * Read single-value unquoted tag object.
     *
     * @param first the first character to append.
     * @return      a tag object.
     * @param <A>   the implementation of tag object.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    protected <A extends T> A readUnquotedTag(int first) throws IOException {
        final String unquoted = readUnquoted(first);
        // Early empty string check
        if (unquoted.isBlank()) {
            return mapper.buildAny(TagType.STRING, unquoted);
        }

        String trim = unquoted.trim();
        // Early boolean check, case-sensitive
        if (trim.equals("true")) {
            return mapper.buildAny(TagType.BYTE, (byte) 1);
        } else if (trim.equals("false")) {
            return mapper.buildAny(TagType.BYTE, (byte) 0);
        }

        final char last = trim.charAt(trim.length() - 1);
        final char tolast = trim.length() > 1 ? trim.charAt(trim.length() - 2) : '\0';
        // Early invalid number check
        if (trim.charAt(0) == '_' || last == '_') {
            return mapper.buildAny(TagType.STRING, unquoted);
        }

        TagType<?> type;
        boolean unsigned = false;
        int end;
        if (version >= V1_21_5) {
            if (trim.startsWith("bool(") && trim.endsWith(")")) { // Parse boolean, case-insensitive
                final String argument = trim.substring(5, trim.length() - 1).trim();
                if (argument.equalsIgnoreCase("true")) {
                    return mapper.buildAny(TagType.BYTE, (byte) 1);
                } else if (argument.equalsIgnoreCase("false")) {
                    return mapper.buildAny(TagType.BYTE, (byte) 0);
                } else {
                    try {
                        if (Integer.parseInt(argument) == 0) {
                            return mapper.buildAny(TagType.BYTE, (byte) 0);
                        } else {
                            return mapper.buildAny(TagType.BYTE, (byte) 1);
                        }
                    } catch (NumberFormatException e) {
                        throw new IOException("Invalid boolean value: '" + argument + "'");
                    }
                }
            } else if (trim.startsWith("uuid(") && trim.endsWith(")")) { // Parse unique id
                final String argument = trim.substring(5, trim.length() - 1).trim();
                try {
                    // Test UUID
                    UUID.fromString(argument);
                    // Convert UUID into int[]
                    final int[] array = new int[4];
                    final String rawUUID = argument.replace("-", "");
                    for (int i = 0; i < 32; i = i + 8) {
                        array[i / 8] = new BigInteger(rawUUID.substring(i, i + 8), 16).intValue();
                    }
                    return mapper.buildAny(TagType.INT_ARRAY, array);
                } catch (Throwable e) {
                    throw new IOException("Invalid UUID value: '" + argument + "'");
                }
            } else if (unquoted.indexOf('(') >= 0 && unquoted.indexOf(')') >= 0) { // Check for non-unquoted characters
                throw new IOException("Invalid operation: '" + unquoted + "'");
            } else if (trim.charAt(0) == '0' && trim.length() >= 3) { // Normalize integer number
                // Extract radix by prefix
                final int radix;
                switch (trim.charAt(1)) {
                    case 'x':
                    case 'X':
                        // hex
                        radix = 16;
                        break;
                    case 'b':
                    case 'B':
                        // binary
                        radix = 2;
                        break;
                    default:
                        radix = 0;
                        break;
                }
                if (radix > 0) {
                    // Ignore suffixes
                    if (isIntegerSuffix(last)) {
                        if (isSignednessSuffix(tolast)) {
                            end = trim.length() - 2;
                        } else {
                            end = trim.length() - 1;
                        }
                    } else if (isSignednessSuffix(last)) {
                        end = trim.length() - 1;
                    } else {
                        end = trim.length();
                    }
                    // Parse integer number
                    if (end > 3) {
                        try {
                            final long number = Long.parseLong(trim.substring(2, end).replace('_', '\0'), radix);
                            trim = number + trim.substring(end);
                        } catch (NumberFormatException ignored) { }
                    }
                }
            }

            trim = trim.replace('_', '\0');

            type = getType(last);
            if (type != null) {
                if (type.isInteger() && isSignednessSuffix(tolast)) {
                    unsigned = tolast == 'u' || tolast == 'U';
                    end = trim.length() - 2;
                } else {
                    end = trim.length() - 1;
                }
            } else if (isSignednessSuffix(last)) {
                unsigned = tolast == 'u' || tolast == 'U';
                end = trim.length() - 1;
            } else {
                end = trim.length();
            }
        } else {
            type = getType(last);
            end = type == null ? trim.length() : trim.length() - 1;
        }

        final Boolean integer = isNumber(trim, 0, end);
        if (integer == null || (!integer && unsigned) || (integer && type != null && type.isDecimal())) {
            return mapper.buildAny(TagType.STRING, unquoted);
        }

        if (type == null) {
            if (integer) {
                type = TagType.INT;
            } else {
                type = TagType.DOUBLE;
            }
        }

        switch (type.id()) {
            case Tag.BYTE:
                if (unsigned) {
                    return mapper.buildAny(TagType.BYTE, (byte) Integer.parseUnsignedInt(unquoted));
                }
                return mapper.buildAny(TagType.BYTE, Byte.parseByte(unquoted));
            case Tag.SHORT:
                if (unsigned) {
                    return mapper.buildAny(TagType.SHORT, (short) Integer.parseUnsignedInt(unquoted));
                }
                return mapper.buildAny(TagType.SHORT, Short.parseShort(unquoted));
            case Tag.INT:
                if (unsigned) {
                    return mapper.buildAny(TagType.INT, Integer.parseUnsignedInt(unquoted));
                }
                return mapper.buildAny(TagType.INT, Integer.parseInt(unquoted));
            case Tag.LONG:
                return mapper.buildAny(TagType.LONG, Long.parseLong(unquoted));
            case Tag.FLOAT:
                return mapper.buildAny(TagType.FLOAT, Float.parseFloat(unquoted));
            case Tag.DOUBLE:
                return mapper.buildAny(TagType.DOUBLE, Double.parseDouble(unquoted));
            default:
                throw new IOException("Invalid unquoted tag type: " + type.name());
        }
    }

    /**
     * Read unquoted value.
     *
     * @return a string.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    protected String readUnquoted() throws IOException {
        skipSpaces();
        final int first = read();
        if (first == UNKNOWN_CHARACTER || !isUnquoted(first)) {
            throw new IOException("Cannot read unquoted string");
        }
        return readUnquoted(first);
    }

    /**
     * Read unquoted value.
     *
     * @param first the first character to append.
     * @return      a string.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    protected String readUnquoted(int first) throws IOException {
        final StringBuilder builder = new StringBuilder();
        builder.append((char) first);
        mark(1);
        int i;
        while ((i = read()) != UNKNOWN_CHARACTER) {
            if (isUnquoted(i) || ((i == '(' || i == ')') && version >= V1_21_5)) {
                mark(1);
                builder.append((char) i);
            } else {
                break;
            }
        }
        reset();
        return builder.toString();
    }

    /**
     * Read quoted tag value, this only reads a String tag type.
     *
     * @return    a tag object.
     * @param <A> the implementation of tag object.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    public <A extends T> A readQuotedTag() throws IOException {
        final int first = read();
        if (isQuote(first)) {
            return readQuotedTag(first);
        }
        throw new IOException("Cannot read quoted tag without leading quote character");
    }

    /**
     * Read quoted tag value, this only reads a String tag type.
     *
     * @param quote the closing quote char.
     * @return      a tag object.
     * @param <A>   the implementation of tag object.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    protected <A extends T> A readQuotedTag(int quote) throws IOException {
        return mapper.buildAny(TagType.STRING, readQuoted(quote));
    }

    /**
     * Read quoted value.
     *
     * @param quote the closing quote char.
     * @return      a string.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    @SuppressWarnings("fallthrough")
    protected String readQuoted(int quote) throws IOException {
        final StringBuilder builder = new StringBuilder();
        boolean escape = false;
        int i;
        while ((i = read()) != UNKNOWN_CHARACTER) {
            if (escape) {
                escape = false;
                if (version >= V1_21_5) {
                    switch (i) {
                        case 'b':
                            builder.append('\b');
                            continue;
                        case 'f':
                            builder.append('\f');
                            continue;
                        case 'n':
                            builder.append('\n');
                            continue;
                        case 'r':
                            builder.append('\r');
                            continue;
                        case 's':
                            builder.append(' ');
                            continue;
                        case 't':
                            builder.append('\t');
                            continue;
                        case '\\':
                            builder.append('\\');
                            continue;
                        case '\'':
                            builder.append('\'');
                            continue;
                        case '"':
                            builder.append('"');
                            continue;
                        case 'x':
                            final Optional<String> unicode2 = readUnicode(2);
                            if (unicode2.isPresent()) {
                                builder.append(unicode2.get());
                                continue;
                            }
                            break;
                        case 'u':
                            final Optional<String> unicode4 = readUnicode(4);
                            if (unicode4.isPresent()) {
                                builder.append(unicode4.get());
                                continue;
                            }
                            break;
                        case 'U':
                            final Optional<String> unicode8 = readUnicode(8);
                            if (unicode8.isPresent()) {
                                builder.append(unicode8.get());
                                continue;
                            }
                            break;
                        case 'N':
                            if (skip('{')) {
                                final StringBuilder name = new StringBuilder();
                                while ((i = read()) != UNKNOWN_CHARACTER) {
                                    if (i == '}') {
                                        try {
                                            final int codePoint = Character.codePointOf(name.toString());
                                            builder.append((char) codePoint);
                                        } catch (IllegalArgumentException e) {
                                            builder.append('\\').append('N').append('{').append(name).append('}');
                                        }
                                        break;
                                    } else if (isCodePoint(i)) {
                                        name.append((char) i);
                                    } else {
                                        builder.append('\\').append('N').append('{').append(name);
                                        if (i == quote) {
                                            return builder.toString();
                                        }
                                        builder.append((char) i);
                                        break;
                                    }
                                }
                                continue;
                            }
                            break;
                        default:
                            break;
                    }
                } else if (i == quote) {
                    builder.append((char) i);
                    continue;
                }
                builder.append('\\');
                builder.append((char) i);
            } else if (i == '\\') {
                escape = true;
            } else if (i == quote) {
                return builder.toString();
            }
        }
        throw new IOException("Non closed quoted string: " + builder);
    }

    @NotNull
    protected Optional<String> readUnicode(int length) throws IOException {
        mark(length);
        int codePoint = 0;
        for (int i = 0; i < length; i++) {
            // Read and get value of hexadecimal character or codepoint
            int c = read();
            if (c == UNKNOWN_CHARACTER || (c = Character.digit(c, 16)) == UNKNOWN_CHARACTER) {
                reset();
                return Optional.empty();
            }
            codePoint = (codePoint << 4) + c;
        }
        if (Character.isValidCodePoint(codePoint)) {
            return Optional.of(Character.toString(codePoint));
        } else {
            reset();
            return Optional.empty();
        }
    }

    /**
     * Read array tag.
     *
     * @return    an array tag object.
     * @param <A> the implementation of tag object.
     * @throws IOException if any I/O error occurs.
     */
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

    /**
     * Read array tag with associated type ID.
     *
     * @param id  the array type ID character / suffix.
     * @return    an array tag object.
     * @param <A> the implementation of tag object.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    protected <A extends T> A readArrayTag(int id) throws IOException {
        final TagType<?> type = getArrayType((char) id);
        if (type != null) {
            switch (type.id()) {
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

    /**
     * Read array tag with associated type.
     *
     * @param type          the type of tag array.
     * @param suffix        the accepted suffix value.
     * @param valueFunction the function that convert string-represented value into required values.
     * @param arrayFunction the function that convert list of array type into required array.
     * @return              an array tag object.
     * @param <A>           the implementation of tag object.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    protected <A extends T> A readArrayTag(@NotNull TagType<?> type, boolean suffix, @NotNull Function<String, Object> valueFunction, @NotNull Function<List<Object>, Object> arrayFunction) throws IOException {
        final List<Object> array = new ArrayList<>();
        String unquoted;
        while (!(unquoted = readUnquoted()).isEmpty()) {
            if (suffix) {
                final char last = unquoted.charAt(unquoted.length() - 1);
                if (last == type.suffix() || last == Character.toLowerCase(type.suffix())) {
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

    /**
     * Read list tag.
     *
     * @return    a list tag object.
     * @param <A> the implementation of tag object.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    public <A extends T> A readListTag() throws IOException {
        if (read() != '[') {
            throw new IOException("List tag must start with '['");
        }
        final List<T> list = new ArrayList<>();
        T value;
        boolean heterogenous = false;
        while ((value = readTag()) != null) {
            if (mapper.type(value).equals(TagType.COMPOUND)) {
                heterogenous = true;
                if (!list.isEmpty()) {
                    for (int i = 0; i < list.size(); i++) {
                        final T element = list.get(i);
                        if (!mapper.type(element).equals(TagType.COMPOUND)) {
                            final Map<String, T> wrapped = new HashMap<>();
                            wrapped.put("", element);
                            list.set(i, mapper.build(TagType.COMPOUND, wrapped));
                        }
                    }
                }
                list.add(value);
            } else if (heterogenous) {
                final Map<String, T> wrapped = new HashMap<>();
                wrapped.put("", value);
                list.add(mapper.build(TagType.COMPOUND, wrapped));
            } else {
                list.add(value);
            }
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

    /**
     * Read compound tag.
     *
     * @return    a compound tag object.
     * @param <A> the implementation of tag object.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    public <A extends T> A readCompoundTag() throws IOException {
        if (read() != '{') {
            throw new IOException("Compound tag must start with '{'");
        }
        return readCompoundTag0();
    }

    /**
     * Read compound tag after first bracket.
     *
     * @return    a compound tag object.
     * @param <A> the implementation of tag object.
     * @throws IOException if any I/O error occurs.
     */
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

    /**
     * Skips specific character from reader, this method will ignore any whitespace before character.
     *
     * @param c the character to skip.
     * @return  true if the character was actually skipped, false otherwise.
     * @throws IOException if any I/O error occurs.
     */
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

    public boolean skip(@NotNull String s) throws IOException {
        return skip(s, 0);
    }

    public boolean skip(@NotNull String s, int start) throws IOException {
        mark(s.length());
        for (int i = start; i < s.length(); i++) {
            if (read() != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Skips whitespaces.
     *
     * @return the number of characters actually skipped.
     * @throws IOException if any I/O error occurs.
     */
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

    /**
     * Convert nbt-represented java object with provided {@link TagMapper}.
     *
     * @param s      the SNBT to convert.
     * @return       a nbt-represented java object converted from SNBT.
     * @param <A>    the implementation of tag object.
     */
    public static <A> A fromString(@NotNull String s) {
        return fromString(s, TagMapper.DEFAULT);
    }

    /**
     * Convert SNBT into tag object with provided {@link TagMapper}.
     *
     * @param s      the SNBT to convert.
     * @param mapper the mapper to create tag objects by providing a value.
     * @return       a tag object converted from SNBT.
     * @param <T>    the tag object implementation.
     * @param <A>    the implementation of tag object.
     */
    public static <T, A extends T> A fromString(@NotNull String s, @NotNull TagMapper<T> mapper) {
        try (StringReader reader = new StringReader(s); TagReader<T> tagReader = new TagReader<>(reader, mapper)) {
            return tagReader.readTag();
        } catch (IOException e) {
            throw new RuntimeException("Cannot read object from SNBT", e);
        }
    }
}
