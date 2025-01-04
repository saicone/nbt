package com.saicone.nbt.io;

import com.saicone.nbt.Tag;
import com.saicone.nbt.TagType;
import com.saicone.nbt.TagMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

/**
 * Writes tag objects into delegated {@link Writer} as SNBT format.<br>
 * The formatting method aims to be compatible with older Minecraft versions by avoiding the usage of single quotes.
 *
 * @see AsyncStringWriter
 *
 * @author Rubenicos
 *
 * @param <T> the tag object implementation.
 */
public class TagWriter<T> extends Writer {

    private final Writer writer;
    private final TagMapper<T> mapper;

    /**
     * Create a tag writer that accepts nbt-represented java objects with provided {@link Writer}.
     *
     * @param writer the delegated writer to append characters.
     * @return       a newly generated tag writer.
     */
    @NotNull
    public static TagWriter<Object> of(@NotNull Writer writer) {
        return of(writer, TagMapper.DEFAULT);
    }

    /**
     * Create a tag writer with provided {@link Writer} and {@link TagMapper}.
     *
     * @param writer the delegated writer to append characters.
     * @param mapper the mapper to extract values from tags
     * @return       a newly generated tag writer.
     * @param <T>    the tag object implementation.
     */
    @NotNull
    public static <T> TagWriter<T> of(@NotNull Writer writer, @NotNull TagMapper<T> mapper) {
        return new TagWriter<>(writer, mapper);
    }

    /**
     * Constructs a tag writer with provided {@link Writer} and {@link TagMapper}.
     *
     * @param writer the delegated writer to append characters.
     * @param mapper the mapper to extract values from tags.
     */
    public TagWriter(@NotNull Writer writer, @NotNull TagMapper<T> mapper) {
        this.writer = writer;
        this.mapper = mapper;
    }

    /**
     * Check if the provided string should be unquoted.
     *
     * @param s the string to check.
     * @return  true if the string is unquoted, false otherwise.
     */
    protected boolean isUnquoted(@NotNull String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!isUnquoted(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the provided char is an allowed unquoted character.
     *
     * @param c the char to check.
     * @return  true if the char should be unquoted, false otherwise.
     */
    protected boolean isUnquoted(char c) {
        return c >= '0' && c <= '9'
                || c >= 'A' && c <= 'Z'
                || c >= 'a' && c <= 'z'
                || c == '_' || c == '-'
                || c == '.' || c == '+';
    }

    /**
     * Get the delegated writer.
     *
     * @return a writer that is used to append characters.
     */
    @NotNull
    public Writer getWriter() {
        return writer;
    }

    /**
     * Get the mapper that is used to extract values.
     *
     * @return a tag mapper.
     */
    @NotNull
    public TagMapper<T> getMapper() {
        return mapper;
    }

    /**
     * Write to provided tag object.
     *
     * @param t the tag object to write.
     * @throws IOException if any I/O exception occurs.
     */
    @SuppressWarnings("unchecked")
    public void writeTag(@Nullable T t) throws IOException {
        if (t == null) {
            return;
        }
        final Object value = mapper.extract(t);
        if (value == null) {
            return;
        }
        final TagType<Object> type = mapper.type(t);
        if (!type.isValid()) {
            throw new IOException("Cannot write invalid tag: " + type.getName());
        }
        switch (type.getId()) {
            case Tag.END:
            case Tag.BYTE:
            case Tag.SHORT:
            case Tag.INT:
            case Tag.LONG:
            case Tag.FLOAT:
            case Tag.DOUBLE:
                writePrimitiveTag(type, value);
                break;
            case Tag.BYTE_ARRAY:
                if (value instanceof boolean[]) {
                    writeBooleanArrayTag((boolean[]) value);
                } else {
                    writeByteArrayTag((byte[]) value);
                }
                break;
            case Tag.STRING:
                writeStringTag((String) value);
                break;
            case Tag.INT_ARRAY:
                writeIntArrayTag((int[]) value);
                break;
            case Tag.LONG_ARRAY:
                writeLongArrayTag((long[]) value);
                break;
            case Tag.LIST:
                writeListTag((List<T>) value);
                break;
            case Tag.COMPOUND:
                writeCompoundTag((Map<String, T>) value);
                break;
            default:
                throw new IllegalArgumentException("Invalid tag type: " + type.getName());
        }
    }

    /**
     * Write to provided primitive tag value with associated type.
     *
     * @param type the type of tag.
     * @param v    the tag value to write.
     * @param <V>  the nbt-represented value type implementation.
     * @throws IOException if any I/O exception occurs.
     */
    public <V> void writePrimitiveTag(@NotNull TagType<V> type, @NotNull V v) throws IOException {
        if (type == TagType.DOUBLE || type == TagType.INT) {
            write(String.valueOf(v));
        } else {
            write(String.valueOf(v) + type.getSuffix());
        }
    }

    /**
     * Write string tag value, any {@code "} will be replaced with {@code \"} to match SNBT compatibility.
     *
     * @param s the tag value to write.
     * @throws IOException if any I/O exception occurs.
     */
    public void writeStringTag(@NotNull String s) throws IOException {
        write("\"" + s.replace("\"", "\\\"") + "\"");
    }

    /**
     * Write byte array tag value.
     *
     * @param bytes the tag value to write.
     * @throws IOException if any I/O exception occurs.
     */
    public void writeByteArrayTag(byte[] bytes) throws IOException {
        write("[" + TagType.BYTE_ARRAY.getSuffix() + ";");
        boolean delimiter = false;
        for (byte b : bytes) {
            if (delimiter) {
                write(',');
            }

            write(String.valueOf(b) + TagType.BYTE_ARRAY.getSuffix());

            delimiter = true;
        }
        write(']');
    }

    /**
     * Write fake boolean array tag value.
     *
     * @param booleans the tag value to write.
     * @throws IOException if any I/O exception occurs.
     */
    public void writeBooleanArrayTag(boolean[] booleans) throws IOException {
        write("[" + TagType.BOOLEAN_ARRAY.getSuffix() + ";");
        boolean delimiter = false;
        for (boolean b : booleans) {
            if (delimiter) {
                write(',');
            }

            write(String.valueOf(b));

            delimiter = true;
        }
        write(']');
    }

    /**
     * Write int array tag value.
     *
     * @param ints the tag value to write.
     * @throws IOException if any I/O exception occurs.
     */
    public void writeIntArrayTag(int[] ints) throws IOException {
        write("[" + TagType.INT_ARRAY.getSuffix() + ";");
        boolean delimiter = false;
        for (int i : ints) {
            if (delimiter) {
                write(',');
            }

            write(String.valueOf(i));

            delimiter = true;
        }
        write(']');
    }

    /**
     * Write long array tag value.
     *
     * @param longs the tag value to write.
     * @throws IOException if any I/O exception occurs.
     */
    public void writeLongArrayTag(long[] longs) throws IOException {
        write("[" + TagType.LONG_ARRAY.getSuffix() + ";");
        boolean delimiter = false;
        for (long l : longs) {
            if (delimiter) {
                write(',');
            }

            write(String.valueOf(l) + TagType.LONG_ARRAY.getSuffix());

            delimiter = true;
        }
        write(']');
    }

    /**
     * Write list of tag objects value from list tag type.
     *
     * @param list the tag value to write.
     * @throws IOException if any I/O exception occurs.
     */
    public void writeListTag(@NotNull List<T> list) throws IOException {
        write('[');
        boolean delimiter = false;
        for (T t : list) {
            if (delimiter) {
                write(',');
            }

            writeTag(t);

            delimiter = true;
        }
        write(']');
    }

    /**
     * Write map of string-tag entries value from compound tag type.
     *
     * @param map the tag value to write.
     * @throws IOException if any I/O exception occurs.
     */
    public void writeCompoundTag(@NotNull Map<String, T> map) throws IOException {
        write('{');
        boolean delimiter = false;
        for (Map.Entry<String, T> entry : map.entrySet()) {
            if (delimiter) {
                write(',');
            }

            if (isUnquoted(entry.getKey())) {
                write(entry.getKey());
            } else {
                writeStringTag(entry.getKey());
            }
            write(':');
            writeTag(entry.getValue());

            delimiter = true;
        }
        write('}');
    }

    @Override
    public void write(@NotNull char[] cbuf, int off, int len) throws IOException {
        writer.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    /**
     * Convert nbt-represented java object into SNBT.
     *
     * @param object the nbt-represented java object to convert.
     * @return       a SNBT that represent the java object.
     */
    @NotNull
    public static String toString(@Nullable Object object) {
        return toString(object, TagMapper.DEFAULT);
    }

    /**
     * Convert tag object into SNBT with provided {@link TagMapper}.
     *
     * @param t      the tag object to convert.
     * @param mapper the mapper to extract value from tag.
     * @return       a SNBT that represent the tag object.
     * @param <T>    the tag object implementation.
     */
    @NotNull
    public static <T> String toString(@Nullable T t, @NotNull TagMapper<T> mapper) {
        try (AsyncStringWriter writer = new AsyncStringWriter(); TagWriter<T> tagWriter = new TagWriter<>(writer, mapper)) {
            tagWriter.writeTag(t);
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException("Cannot write object to SNBT", e);
        }
    }
}
