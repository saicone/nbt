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

public class TagWriter<T> extends Writer {

    private final Writer writer;
    private final TagMapper<T> mapper;

    @NotNull
    public static TagWriter<Object> of(@NotNull Writer writer) {
        return of(writer, TagMapper.DEFAULT);
    }

    @NotNull
    public static <T> TagWriter<T> of(@NotNull Writer writer, @NotNull TagMapper<T> mapper) {
        return new TagWriter<>(writer, mapper);
    }

    public TagWriter(@NotNull Writer writer, @NotNull TagMapper<T> mapper) {
        this.writer = writer;
        this.mapper = mapper;
    }

    protected boolean isUnquoted(@NotNull String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!isUnquoted(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    protected boolean isUnquoted(char c) {
        return c >= '0' && c <= '9'
                || c >= 'A' && c <= 'Z'
                || c >= 'a' && c <= 'z'
                || c == '_' || c == '-'
                || c == '.' || c == '+';
    }

    @NotNull
    public Writer getWriter() {
        return writer;
    }

    @NotNull
    public TagMapper<T> getMapper() {
        return mapper;
    }

    @SuppressWarnings("unchecked")
    public void writeTag(@Nullable T t) throws IOException {
        if (t == null) {
            return;
        }
        final Object value = mapper.extract(t);
        if (value == null) {
            return;
        }
        final TagType<Object> type = TagType.getType(value);
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

    public <A> void writePrimitiveTag(@NotNull TagType<A> type, @NotNull A a) throws IOException {
        if (type == TagType.DOUBLE) {
            write(String.valueOf(a));
        } else {
            write(String.valueOf(a) + type.getSuffix());
        }
    }

    public void writeStringTag(@NotNull String s) throws IOException {
        write('"' + s.replace("\"", "\\\"") + '"');
    }

    public void writeByteArrayTag(byte[] bytes) throws IOException {
        write('[' + TagType.BYTE_ARRAY.getSuffix() + ";");
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

    public void writeBooleanArrayTag(boolean[] booleans) throws IOException {
        write('[' + TagType.BOOLEAN.getSuffix() + ";");
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

    public void writeIntArrayTag(int[] ints) throws IOException {
        write('[' + TagType.INT.getSuffix() + ";");
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

    public void writeLongArrayTag(long[] longs) throws IOException {
        write('[' + TagType.LONG.getSuffix() + ";");
        boolean delimiter = false;
        for (long l : longs) {
            if (delimiter) {
                write(',');
            }

            write(String.valueOf(l) + TagType.LONG.getSuffix());

            delimiter = true;
        }
        write(']');
    }

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

    @NotNull
    public static String toString(@Nullable Object object) {
        return toString(object, TagMapper.DEFAULT);
    }

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
