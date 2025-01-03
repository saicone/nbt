package com.saicone.nbt.io;

import com.saicone.nbt.Tag;
import com.saicone.nbt.TagMapper;
import com.saicone.nbt.TagType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TagOutput<T> implements Closeable {

    private final DataOutput output;
    private final TagMapper<T> mapper;

    @NotNull
    public static TagOutput<Object> of(@NotNull DataOutput output) {
        return of(output, TagMapper.DEFAULT);
    }

    @NotNull
    public static <T> TagOutput<T> of(@NotNull DataOutput output, @NotNull TagMapper<T> mapper) {
        return new TagOutput<>(output, mapper);
    }

    public TagOutput(@NotNull DataOutput output, @NotNull TagMapper<T> mapper) {
        this.output = output;
        this.mapper = mapper;
    }

    @NotNull
    public DataOutput getOutput() {
        return output;
    }

    @NotNull
    public TagMapper<T> getMapper() {
        return mapper;
    }

    public void writeUnnamed(@Nullable T t) throws IOException {
        final Object value = t == null ? null : mapper.extract(t);
        final TagType<Object> type = mapper.type(t);
        output.writeByte(type.getId());
        if (type == TagType.END) {
            return;
        }
        // Write empty name
        output.writeUTF("");
        writeTag(type, value);
    }

    public void writeAny(@Nullable T t) throws IOException {
        final Object value = t == null ? null : mapper.extract(t);
        final TagType<Object> type = mapper.type(t);
        output.writeByte(type.getId());
        if (type == TagType.END) {
            return;
        }
        writeTag(type, value);
    }

    public void writeBedrockFile(@Nullable T t) throws IOException {
        writeBedrockFile(mapper.size(t), t);
    }

    public void writeBedrockFile(int size, @Nullable T t) throws IOException {
        writeBedrockFile(Tag.DEFAULT_BEDROCK_VERSION, size, t);
    }

    public void writeBedrockFile(int version, int size, @Nullable T t) throws IOException {
        output.writeInt(version);
        output.writeInt(size);
        writeAny(t);
    }

    public void writeTag(@Nullable T t) throws IOException {
        final Object value = t == null ? null : mapper.extract(t);
        final TagType<Object> type = mapper.type(t);
        writeTag(type, value);
    }

    @SuppressWarnings("unchecked")
    public void writeTag(@NotNull TagType<?> type, @Nullable Object object) throws IOException {
        if (type == TagType.END || object == null) {
            return;
        }
        switch (type.getId()) {
            case Tag.BYTE:
                if (object instanceof Boolean) {
                    output.writeByte((Boolean) object ? 1 : 0);
                } else {
                    output.writeByte((byte) object);
                }
                break;
            case Tag.SHORT:
                output.writeShort((short) object);
                break;
            case Tag.INT:
                output.writeInt((int) object);
                break;
            case Tag.LONG:
                output.writeLong((long) object);
                break;
            case Tag.FLOAT:
                output.writeFloat((float) object);
                break;
            case Tag.DOUBLE:
                output.writeDouble((double) object);
                break;
            case Tag.BYTE_ARRAY:
                if (type == TagType.BOOLEAN_ARRAY) {
                    writeByteArray(mapper.byteArray(object));
                } else {
                    writeByteArray((byte[]) object);
                }
                break;
            case Tag.STRING:
                output.writeUTF((String) object);
                break;
            case Tag.LIST:
                writeList((List<T>) object);
                break;
            case Tag.COMPOUND:
                writeCompound((Map<String, T>) object);
                break;
            case Tag.INT_ARRAY:
                writeIntArray((int[]) object);
                break;
            case Tag.LONG_ARRAY:
                writeLongArray((long[]) object);
                break;
            default:
                throw new IllegalArgumentException("Invalid tag type: " + type.getName());
        }
    }

    protected void writeByteArray(byte[] bytes) throws IOException {
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    protected void writeIntArray(int[] ints) throws IOException {
        output.writeInt(ints.length);
        for (int i : ints) {
            output.writeInt(i);
        }
    }

    protected void writeLongArray(long[] longs) throws IOException {
        output.writeInt(longs.length);
        for (long l : longs) {
            output.writeLong(l);
        }
    }

    protected void writeList(@NotNull List<T> list) throws IOException {
        final TagType<Object> type;
        if (list.isEmpty()) {
            type = TagType.END;
        } else {
            type = mapper.type(list.get(0));
        }

        output.writeByte(type.getId());
        output.writeInt(list.size());

        for (T t : list) {
            writeTag(type, t);
        }
    }

    protected void writeCompound(@NotNull Map<String, T> map) throws IOException {
        for (Map.Entry<String, T> entry : map.entrySet()) {
            final Object value = entry.getValue() == null ? null : mapper.extract(entry.getValue());
            final TagType<Object> type = mapper.type(entry.getValue());
            output.writeByte(type.getId());
            if (type != TagType.END) {
                output.writeUTF(entry.getKey());
                writeTag(type, value);
            }
        }
        output.writeByte(Tag.END);
    }

    @Override
    public void close() throws IOException {
        if (output instanceof Closeable) {
            ((Closeable) output).close();
        }
    }
}
