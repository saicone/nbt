package com.saicone.nbt.io;

import com.saicone.nbt.Tag;
import com.saicone.nbt.TagMapper;
import com.saicone.nbt.TagType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Tag Output</b><br>
 * A tag input provides methods for reading multiples data formats from a
 * delegated {@link DataInput} and reconstructing from them data in any tag object type.
 *
 * @author Rubenicos
 *
 * @param <T> the tag object implementation.
 */
public class TagInput<T> implements Closeable {

    private final DataInput input;
    private final TagMapper<T> mapper;

    private long maxQuota = Tag.DEFAULT_NBT_QUOTA;
    private long remainingQuota = Tag.DEFAULT_NBT_QUOTA;
    private int remainingDepth = Tag.MAX_STACK_DEPTH;

    /**
     * Create a tag input that create nbt-represented java objects with provided {@link DataInput} and {@link TagMapper}.
     *
     * @param input  the input to read tag objects.
     * @return       a newly generated tag input.
     */
    @NotNull
    public static TagInput<Object> of(@NotNull DataInput input) {
        return of(input, TagMapper.DEFAULT);
    }

    /**
     * Create a tag input with provided {@link DataInput} and {@link TagMapper}.
     *
     * @param input  the input to read tag objects.
     * @param mapper the mapper to create tag objects by providing a value.
     * @return       a newly generated tag input.
     * @param <T> the tag object implementation.
     */
    @NotNull
    public static <T> TagInput<T> of(@NotNull DataInput input, @NotNull TagMapper<T> mapper) {
        return new TagInput<>(input, mapper);
    }

    /**
     * Constructs a tag input with provided {@link DataInput} and {@link TagMapper}.
     *
     * @param input  the input to read tag objects.
     * @param mapper the mapper to create tag objects by providing a value.
     */
    public TagInput(@NotNull DataInput input, @NotNull TagMapper<T> mapper) {
        this.input = input;
        this.mapper = mapper;
    }

    /**
     * Set the maximum byte size allowed by this instance to an unlimited one.
     *
     * @return this instance.
     */
    @NotNull
    @Contract("-> this")
    public TagInput<T> unlimited() {
        return maxQuota(Long.MAX_VALUE);
    }

    /**
     * Set the maximum byte size allowed by this instance.
     *
     * @param quota the size on bytes.
     * @return      this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagInput<T> maxQuota(long quota) {
        this.maxQuota = quota;
        this.remainingQuota = quota;
        return this;
    }

    /**
     * Set the maximum nested value depth allowed by this instance.
     *
     * @param maxDepth the maximum depth.
     * @return         this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagInput<T> maxDepth(int maxDepth) {
        this.remainingDepth = maxDepth;
        return this;
    }

    /**
     * Get the delegated data input.
     *
     * @return the data input used to write tag objects.
     */
    @NotNull
    public DataInput getInput() {
        return input;
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
     * Get the remaining quota for tag byte size.
     *
     * @return a byte size.
     */
    public long getRemainingQuota() {
        return remainingQuota;
    }

    /**
     * Get the remaining nested value depth.
     *
     * @return a integer value.
     */
    public int getRemainingDepth() {
        return remainingDepth;
    }

    /**
     * Use an amount of bytes with a static size.
     *
     * @param size   the size of the bytes.
     * @param amount the amount of bytes to count.
     */
    protected void useBytes(long size, long amount) {
        useBytes(size * amount);
    }

    /**
     * Use a byte size.
     *
     * @param bytes the byte size to count.
     */
    protected void useBytes(long bytes) {
        if (this.maxQuota == Long.MAX_VALUE) {
            return;
        }
        if ((remainingQuota -= bytes) < 0) {
            throw new IllegalArgumentException("Cannot read tag bigger than " + this.maxQuota + " bytes");
        }
    }

    /**
     * Increment nested value depth usage.
     */
    protected void incrementDepth() {
        if (--remainingDepth < 0) {
            throw new IllegalArgumentException("Cannot read tag with too many nested values");
        }
    }

    /**
     * Decrement nested value depth usage.
     */
    protected void decrementDepth() {
        remainingDepth++;
    }

    /**
     * Read tag object with unnamed tag format.
     *
     * @return    a tag object.
     * @param <A> the implementation of tag object.
     * @throws IOException if any I/O error occurs.
     */
    public <A extends T> A readUnnamed() throws IOException {
        final byte id = input.readByte();
        final TagType<Object> type = TagType.getType(id);
        if (type == TagType.END) {
            return getMapper().buildAny(type, null);
        }
        // Skip name
        // For network stream compatibility use:
        // input.readUTF();
        input.skipBytes(input.readUnsignedShort());
        return readTag(type);
    }

    /**
     * Read tag object with any format.<br>
     * This format is used primarily on network connections.
     *
     * @return    a tag object.
     * @param <A> the implementation of tag object.
     * @throws IOException if any I/O error occurs.
     */
    public <A extends T> A readAny() throws IOException {
        final byte id = input.readByte();
        final TagType<Object> type = TagType.getType(id);
        if (type == TagType.END) {
            return getMapper().buildAny(type, null);
        }
        return readTag(type);
    }

    /**
     * Read tag object with bedrock file format.<br>
     * This method will skip the header.
     *
     * @return    a tag object.
     * @param <A> the implementation of tag object.
     * @throws IOException if any I/O error occurs.
     */
    public <A extends T> A readBedrockFile() throws IOException {
        // Skip header
        // For network stream compatibility use:
        // input.readInt();
        // input.readInt();
        input.skipBytes(8);
        return readAny();
    }

    /**
     * Read tag object by providing a tag type.<br>
     * This method assumes that tag ID was already skipped / read.
     *
     * @param type the type of tag to read.
     * @return     a tag object.
     * @param <A>  the implementation of tag object.
     * @throws IOException if any I/O error occurs.
     */
    public <A extends T> A readTag(@NotNull TagType<?> type) throws IOException {
        useBytes(type.getSize());
        switch (type.getId()) {
            case Tag.END:
                return null;
            case Tag.BYTE:
                if (type == TagType.BOOLEAN) {
                    return mapper.buildAny(type, input.readByte() > 0);
                }
                return mapper.buildAny(type, input.readByte());
            case Tag.SHORT:
                return mapper.buildAny(type, input.readShort());
            case Tag.INT:
                return mapper.buildAny(type, input.readInt());
            case Tag.LONG:
                return mapper.buildAny(type, input.readLong());
            case Tag.FLOAT:
                return mapper.buildAny(type, input.readFloat());
            case Tag.DOUBLE:
                return mapper.buildAny(type, input.readDouble());
            case Tag.BYTE_ARRAY:
                final Object array;
                if (type == TagType.BOOLEAN_ARRAY) {
                    array = mapper.booleanArray(readByteArray());
                } else {
                    array = readByteArray();
                }
                return mapper.buildAny(type, array);
            case Tag.STRING:
                final String s = input.readUTF();
                useBytes(Short.BYTES, s.length());
                return mapper.buildAny(type, s);
            case Tag.LIST:
                return mapper.buildAny(type, readList());
            case Tag.COMPOUND:
                return mapper.buildAny(type, readCompound());
            case Tag.INT_ARRAY:
                return mapper.buildAny(type, readIntArray());
            case Tag.LONG_ARRAY:
                return mapper.buildAny(type, readLongArray());
            default:
                throw new IllegalArgumentException("Invalid tag type: " + type.getName());
        }
    }

    /**
     * Read tag array value.
     *
     * @return a byte array.
     * @throws IOException if any I/O error occurs.
     */
    protected byte[] readByteArray() throws IOException {
        final int size = input.readInt();
        if (size >= 16L * 1024 * 1024) {
            throw new IllegalArgumentException("Cannot read byte array with more than 16MB of data");
        }
        useBytes(Byte.BYTES, size);
        final byte[] array = new byte[size];
        input.readFully(array);
        return array;
    }

    /**
     * Read tag array value.
     *
     * @return a int array.
     * @throws IOException if any I/O error occurs.
     */
    protected int[] readIntArray() throws IOException {
        final int size = input.readInt();
        if (size >= 16L * 1024 * 1024) {
            throw new IllegalArgumentException("Cannot read int array with more than 64MB of data");
        }
        useBytes(Integer.BYTES, size);
        final int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = input.readInt();
        }
        return array;
    }

    /**
     * Read tag array value.
     *
     * @return a long array.
     * @throws IOException if any I/O error occurs.
     */
    protected long[] readLongArray() throws IOException {
        final int size = input.readInt();
        useBytes(Long.BYTES, size);
        final long[] array = new long[size];
        for (int i = 0; i < size; i++) {
            array[i] = input.readLong();
        }
        return array;
    }

    /**
     * Read list of tags value.
     *
     * @return a list of tag objects.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    protected List<T> readList() throws IOException {
        incrementDepth();

        final byte id = input.readByte();
        final int size = input.readInt();
        if (id == Tag.END && size > 0) {
            throw new IllegalArgumentException("Cannot read list without tag type");
        }

        useBytes(Integer.BYTES, size);
        final TagType<?> type = TagType.getType(id);
        final List<T> list = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            list.add(readTag(type));
        }

        decrementDepth();

        return list;
    }

    /**
     * Read map of string keys and tag object values.
     *
     * @return a compound map value.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    protected Map<String, T> readCompound() throws IOException {
        incrementDepth();

        final Map<String, T> map = new HashMap<>();

        byte id;
        while ((id = input.readByte()) != Tag.END) {
            final TagType<?> type = TagType.getType(id);

            final String key = readKey();
            final T value = readTag(type);
            if (map.put(key, value) == null) {
                useBytes(Tag.MAP_ENTRY_SIZE + Integer.BYTES);
            }
        }

        decrementDepth();

        return map;
    }

    /**
     * Read map key and account its size into current instance.
     *
     * @return a map string key.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    protected String readKey() throws IOException {
        final String key = input.readUTF();
        useBytes(Tag.MAP_KEY_SIZE);
        useBytes(Short.BYTES, key.length());
        return key;
    }

    @Override
    public void close() throws IOException {
        if (input instanceof Closeable) {
            ((Closeable) input).close();
        }
    }
}
