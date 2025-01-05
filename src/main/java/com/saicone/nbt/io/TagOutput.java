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

/**
 * <b>Tag Output</b><br>
 * A tag output provides methods for converting multiples
 * data formats from any tag object to a delegated {@link DataOutput}.
 *
 * @author Rubenicos
 *
 * @param <T> the tag object implementation.
 */
public class TagOutput<T> implements Closeable {

    private final DataOutput output;
    private final TagMapper<T> mapper;

    /**
     * Create a tag output that accepts nbt-represented java objects with provided {@link DataOutput}.
     *
     * @param output the output to write tag objects.
     * @return       a newly generate tag output.
     */
    @NotNull
    public static TagOutput<Object> of(@NotNull DataOutput output) {
        return of(output, TagMapper.DEFAULT);
    }

    /**
     * Create a tag output with provided {@link DataOutput} and {@link TagMapper}.
     *
     * @param output the output to write tag objects.
     * @param mapper the mapper to extract values from tags.
     * @return       a newly generate tag output.
     * @param <T>    the tag object implementation.
     */
    @NotNull
    public static <T> TagOutput<T> of(@NotNull DataOutput output, @NotNull TagMapper<T> mapper) {
        return new TagOutput<>(output, mapper);
    }

    /**
     * Constructs a tag output with provided {@link DataOutput} and {@link TagMapper}.
     *
     * @param output the output to write tag objects.
     * @param mapper the mapper to extract values from tags.
     */
    public TagOutput(@NotNull DataOutput output, @NotNull TagMapper<T> mapper) {
        this.output = output;
        this.mapper = mapper;
    }

    /**
     * Get delegated data output
     *
     * @return a data output that is used to write tag objects.
     */
    @NotNull
    public DataOutput getOutput() {
        return output;
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
     * Write tag object as unnamed tag format.
     *
     * @param t the tag object to write.
     * @throws IOException if any I/O error occurs.
     */
    public void writeUnnamed(@Nullable T t) throws IOException {
        final Object value = t == null ? null : mapper.extract(t);
        final TagType<Object> type = mapper.type(t);
        output.writeByte(type.id());
        if (type == TagType.END) {
            return;
        }
        // Write empty name
        output.writeUTF("");
        writeTag(type, value);
    }

    /**
     * Write tag object as any format.<br>
     * This format is used primarily on network connections.
     *
     * @param t the tag object to write.
     * @throws IOException if any I/O error occurs.
     */
    public void writeAny(@Nullable T t) throws IOException {
        final Object value = t == null ? null : mapper.extract(t);
        final TagType<Object> type = mapper.type(t);
        output.writeByte(type.id());
        writeTag(type, value);
    }

    /**
     * Write tag object as bedrock file format.
     *
     * @param t the tag object to write.
     * @throws IOException if any I/O error occurs.
     */
    public void writeBedrockFile(@Nullable T t) throws IOException {
        writeBedrockFile(mapper.size(t), t);
    }

    /**
     * Write tag object as bedrock file format with provided header size.
     *
     * @param size the byte size of the actual tag.
     * @param t    the tag object to write.
     * @throws IOException if any I/O error occurs.
     */
    public void writeBedrockFile(int size, @Nullable T t) throws IOException {
        writeBedrockFile(Tag.DEFAULT_BEDROCK_VERSION, size, t);
    }

    /**
     * Write tag object as bedrock file format with provided header version and size.
     *
     * @param version the header version of file.
     * @param size    the byte size of the actual tag.
     * @param t       the tag object to write.
     * @throws IOException if any I/O error occurs.
     */
    public void writeBedrockFile(int version, int size, @Nullable T t) throws IOException {
        output.writeInt(version);
        output.writeInt(size);
        writeAny(t);
    }

    /**
     * Write tag object value.<br>
     * This method doesn't perform any tag ID write, it only writes a tag value.
     *
     * @param t the tag object to write.
     * @throws IOException if any I/O error occurs.
     */
    public void writeTag(@Nullable T t) throws IOException {
        final Object value = t == null ? null : mapper.extract(t);
        final TagType<Object> type = mapper.type(t);
        writeTag(type, value);
    }

    /**
     * Write tag value with associated tag type.<br>
     * This method doesn't perform any tag ID write, it only writes a tag value.
     *
     * @param type   the tag type that will be written.
     * @param object the tag value to write.
     * @throws IOException if any I/O error occurs.
     */
    @SuppressWarnings("unchecked")
    public void writeTag(@NotNull TagType<?> type, @Nullable Object object) throws IOException {
        if (type == TagType.END || object == null) {
            return;
        }
        switch (type.id()) {
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
                throw new IllegalArgumentException("Invalid tag type: " + type.name());
        }
    }

    /**
     * Write byte array tag value.
     *
     * @param bytes the tag value to write.
     * @throws IOException if any I/O error occurs.
     */
    protected void writeByteArray(byte[] bytes) throws IOException {
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    /**
     * Write int array tag value.
     *
     * @param ints the tag value to write.
     * @throws IOException if any I/O error occurs.
     */
    protected void writeIntArray(int[] ints) throws IOException {
        output.writeInt(ints.length);
        for (int i : ints) {
            output.writeInt(i);
        }
    }

    /**
     * Write long array tag value.
     *
     * @param longs the tag value to write.
     * @throws IOException if any I/O error occurs.
     */
    protected void writeLongArray(long[] longs) throws IOException {
        output.writeInt(longs.length);
        for (long l : longs) {
            output.writeLong(l);
        }
    }

    /**
     * Write list of tag objects value from list tag type.
     *
     * @param list the tag value to write.
     * @throws IOException if any I/O error occurs.
     */
    protected void writeList(@NotNull List<T> list) throws IOException {
        final TagType<Object> type;
        if (list.isEmpty()) {
            type = TagType.END;
        } else {
            type = mapper.type(list.get(0));
        }

        output.writeByte(type.id());
        output.writeInt(list.size());

        for (T t : list) {
            writeTag(type, t);
        }
    }

    /**
     * Write map of string-tag entries value from compound tag type.
     *
     * @param map the tag value to write.
     * @throws IOException if any I/O exception occurs.
     */
    protected void writeCompound(@NotNull Map<String, T> map) throws IOException {
        for (Map.Entry<String, T> entry : map.entrySet()) {
            final Object value = entry.getValue() == null ? null : mapper.extract(entry.getValue());
            final TagType<Object> type = mapper.type(entry.getValue());
            output.writeByte(type.id());
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
