package com.saicone.nbt.nio;

import com.saicone.nbt.Tag;
import com.saicone.nbt.TagMapper;
import com.saicone.nbt.TagType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Tag Buffer</b><br>
 * A tag buffer is a full implementation of NBT format using
 * Java NIO API by providing methods to get/put tag objects
 * from/to a delegated {@link ByteBuffer}.
 *
 * @author Rubenicos
 *
 * @param <T> the tag object implementation.
 */
public class TagBuffer<T> {

    /**
     * Create a tag buffer that use nbt-represented java objects with provided {@link ByteBuffer}.
     *
     * @param buffer the buffer that will provide/receive data.
     * @return       a newly generated tag buffer.
     */
    @NotNull
    public static TagBuffer<Object> of(@NotNull ByteBuffer buffer) {
        return of(buffer, TagMapper.DEFAULT);
    }

    /**
     * Create a tag buffer with provided {@link ByteBuffer} and {@link TagMapper}.
     *
     * @param buffer the buffer that will provide/receive data.
     * @param mapper the mapper for tag object implementation.
     * @return       a newly generated tag buffer.
     * @param <T>    the tag object implementation.
     */
    @NotNull
    public static <T> TagBuffer<T> of(@NotNull ByteBuffer buffer, @NotNull TagMapper<T> mapper) {
        return new TagBuffer<>(buffer, mapper);
    }

    private final ByteBuffer buffer;
    private final TagMapper<T> mapper;

    private int remainingDepth = Tag.MAX_STACK_DEPTH;

    /**
     * Construct a tag buffer with provided {@link ByteBuffer} and {@link TagMapper}.
     *
     * @param buffer the buffer that will provide/receive data.
     * @param mapper the mapper for tag object implementation.
     */
    public TagBuffer(@NotNull ByteBuffer buffer, @NotNull TagMapper<T> mapper) {
        this.buffer = buffer;
        this.mapper = mapper;
    }

    /**
     * Get delegated byte buffer.
     *
     * @return a buffer that will provide/receive data.
     */
    @NotNull
    public ByteBuffer buffer() {
        return buffer;
    }

    /**
     * Get the mapper for tag object implementation.
     *
     * @return a tag mapper.
     */
    @NotNull
    public TagMapper<T> mapper() {
        return mapper;
    }

    /**
     * Get the remaining nested value depth.
     *
     * @return a integer value.
     */
    public int remainingDepth() {
        return remainingDepth;
    }

    /**
     * Sets the delegated buffer byte order.
     *
     * @param order the byte order to use.
     * @return      this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> order(@NotNull ByteOrder order) {
        buffer.order(order);
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
    public TagBuffer<T> maxDepth(int maxDepth) {
        this.remainingDepth = maxDepth;
        return this;
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
     * Reads a byte from delegated byte buffer according nbt format.
     *
     * @return the byte tag value at buffer position.
     */
    public byte get() {
        return buffer.get();
    }

    /**
     * Reads a boolean from delegated byte buffer by reading a byte value.
     *
     * @return true if the byte is more than 0, false otherwise.
     */
    public boolean getBoolean() {
        return buffer.get() > 0;
    }

    /**
     * Reads a byte from delegated byte buffer according nbt format.
     *
     * @return the byte tag value at buffer position.
     */
    public short getShort() {
        return buffer.getShort();
    }

    /**
     * Reads a integer from delegated byte buffer according nbt format.
     *
     * @return the integer tag value at buffer position.
     */
    public int getInt() {
        return buffer.getInt();
    }

    /**
     * Reads a long from delegated byte buffer according nbt format.
     *
     * @return the long tag value at buffer position.
     */
    public long getLong() {
        return buffer.getLong();
    }

    /**
     * Reads a float from delegated byte buffer according nbt format.
     *
     * @return the float tag value at buffer position.
     */
    public float getFloat() {
        return buffer.getFloat();
    }

    /**
     * Reads a double from delegated byte buffer according nbt format.
     *
     * @return the double tag value at buffer position.
     */
    public double getDouble() {
        return buffer.getDouble();
    }

    /**
     * Reads a String from delegated byte buffer according nbt format.
     *
     * @return the String tag value at buffer position.
     */
    @NotNull
    public String getString() {
        final byte[] bytes = new byte[buffer.getShort()];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Reads a byte array from delegated byte buffer according nbt format.
     *
     * @return the byte array tag value at buffer position.
     */
    public byte[] getByteArray() {
        final int size = this.getInt();
        if (size >= 16L * 1024 * 1024) {
            throw new IllegalArgumentException("Cannot read byte array with more than 16MB of data");
        }
        final byte[] array = new byte[size];
        buffer.get(array);
        return array;
    }

    /**
     * Reads an integer array from delegated byte buffer according nbt format.
     *
     * @return the integer array tag value at buffer position.
     */
    public int[] getIntArray() {
        final int size = this.getInt();
        if (size >= 16L * 1024 * 1024) {
            throw new IllegalArgumentException("Cannot read int array with more than 64MB of data");
        }
        final int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = this.getInt();
        }
        return array;
    }

    /**
     * Reads a long array from delegated byte buffer according nbt format.
     *
     * @return the long array tag value at buffer position.
     */
    public long[] getLongArray() {
        final int size = this.getInt();
        final long[] array = new long[size];
        for (int i = 0; i < size; i++) {
            array[i] = this.getLong();
        }
        return array;
    }

    /**
     * Reads a list full of tag objects from delegated byte buffer according nbt format.
     *
     * @return the list tag value at buffer position.
     */
    @NotNull
    public List<T> getList() {
        incrementDepth();

        final byte id = this.get();
        final int size = this.getInt();
        if (id == Tag.END && size > 0) {
            throw new IllegalArgumentException("Cannot read list without tag type");
        }

        final TagType<?> type = TagType.getType(id);
        final List<T> list = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            list.add(getTag(type));
        }

        decrementDepth();

        return list;
    }

    /**
     * Reads a compound of tag values from delegated byte buffer according nbt format.
     *
     * @return the compound tag map at buffer position.
     */
    @NotNull
    public Map<String, T> getCompound() {
        incrementDepth();

        final Map<String, T> map = new HashMap<>();

        byte id;
        while ((id = this.get()) != Tag.END) {
            final TagType<?> type = TagType.getType(id);

            final String key = this.getString();
            final T value = this.getTag(type);
            map.put(key, value);
        }

        decrementDepth();

        return map;
    }

    /**
     * Reads a tag by providing a tag type.<br>
     * This method assumes that tag ID was already skipped / read.
     *
     * @param type the type of tag to read.
     * @return     a tag object.
     * @param <A>  the implementation of tag object.
     */
    public <A extends T> A getTag(@NotNull TagType<?> type) {
        switch (type.id()) {
            case Tag.END:
                return null;
            case Tag.BYTE:
                if (type == TagType.BOOLEAN) {
                    return mapper.buildAny(type, this.getBoolean());
                }
                return mapper.buildAny(type, this.get());
            case Tag.SHORT:
                return mapper.buildAny(type, this.getShort());
            case Tag.INT:
                return mapper.buildAny(type, this.getInt());
            case Tag.LONG:
                return mapper.buildAny(type, this.getLong());
            case Tag.FLOAT:
                return mapper.buildAny(type, this.getFloat());
            case Tag.DOUBLE:
                return mapper.buildAny(type, this.getDouble());
            case Tag.BYTE_ARRAY:
                final Object array;
                if (type == TagType.BOOLEAN_ARRAY) {
                    array = mapper.booleanArray(this.getByteArray());
                } else {
                    array = this.getByteArray();
                }
                return mapper.buildAny(type, array);
            case Tag.STRING:
                return mapper.buildAny(type, this.getString());
            case Tag.LIST:
                return mapper.buildAny(type, this.getList());
            case Tag.COMPOUND:
                return mapper.buildAny(type, this.getCompound());
            case Tag.INT_ARRAY:
                return mapper.buildAny(type, this.getIntArray());
            case Tag.LONG_ARRAY:
                return mapper.buildAny(type, this.getLongArray());
            default:
                throw new IllegalArgumentException("Invalid tag type: " + type.name());
        }
    }

    /**
     * Reads a tag with unnamed tag format.
     *
     * @return    a tag object.
     * @param <A> the implementation of tag object.
     */
    public <A extends T> A getUnnamedTag() {
        final byte id = this.get();
        final TagType<Object> type = TagType.getType(id);
        if (type == TagType.END) {
            return mapper.buildAny(type, null);
        }
        // Skip name
        // For network stream compatibility use:
        // this.getString();
        buffer.position(buffer.position() + Short.toUnsignedInt(buffer.getShort()));
        return getTag(type);
    }

    /**
     * Reads a tag with any tag format.<br>
     * This format is mostly used on network connections.
     *
     * @return    a tag object.
     * @param <A> the implementation of tag object.
     */
    public <A extends T> A getAnyTag() {
        final byte id = this.get();
        final TagType<Object> type = TagType.getType(id);
        if (type == TagType.END) {
            return mapper.buildAny(type, null);
        }
        return getTag(type);
    }

    /**
     * Reads a tag with bedrock file format.<br>
     * This method will skip the header.
     *
     * @return    a tag object.
     * @param <A> the implementation of tag object.
     */
    public <A extends T> A getBedrockFile() {
        // Skip header
        // For network stream compatibility use:
        // this.getInt();
        // this.getInt();
        buffer.position(buffer.position() + 8);
        return getAnyTag();
    }

    /**
     * Writes the provided byte into delegated byte buffer according nbt format.
     *
     * @param b the byte to write.
     * @return  this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> put(byte b) {
        buffer.put(b);
        return this;
    }

    /**
     * Writes the provided boolean into delegated byte buffer according nbt format.
     *
     * @param value the boolean to write.
     * @return      this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> putBoolean(boolean value) {
        return put((byte) (value ? 1 : 0));
    }

    /**
     * Writes the provided short into delegated byte buffer according nbt format.
     *
     * @param value the short to write.
     * @return      this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> putShort(short value) {
        buffer.putShort(value);
        return this;
    }

    /**
     * Writes the provided integer into delegated byte buffer according nbt format.
     *
     * @param value the integer to write.
     * @return      this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> putInt(int value) {
        buffer.putInt(value);
        return this;
    }

    /**
     * Writes the provided long into delegated byte buffer according nbt format.
     *
     * @param value the long to write.
     * @return      this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> putLong(long value) {
        buffer.putLong(value);
        return this;
    }

    /**
     * Writes the provided float into delegated byte buffer according nbt format.
     *
     * @param value the float to write.
     * @return      this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> putFloat(float value) {
        buffer.putFloat(value);
        return this;
    }

    /**
     * Writes the provided double into delegated byte buffer according nbt format.
     *
     * @param value the double to write.
     * @return      this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> putDouble(double value) {
        buffer.putDouble(value);
        return this;
    }

    /**
     * Writes the provided string into delegated byte buffer according nbt format.
     *
     * @param value the string to write.
     * @return      this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> putString(@NotNull String value) {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        this.putShort((short) bytes.length);
        buffer.put(bytes);
        return this;
    }

    /**
     * Writes the provided byte array into delegated byte buffer according nbt format.
     *
     * @param value the byte array to write.
     * @return      this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> putByteArray(byte[] value) {
        this.putInt(value.length);
        buffer.put(value);
        return this;
    }

    /**
     * Writes the provided int array into delegated byte buffer according nbt format.
     *
     * @param value the int array to write.
     * @return      this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> putIntArray(int[] value) {
        this.putInt(value.length);
        for (int i : value) {
            this.putInt(i);
        }
        return this;
    }

    /**
     * Writes the provided long array into delegated byte buffer according nbt format.
     *
     * @param value the long array to write.
     * @return      this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> putLongArray(long[] value) {
        this.putInt(value.length);
        for (long l : value) {
            this.putLong(l);
        }
        return this;
    }

    /**
     * Writes the provided list of tags into delegated byte buffer according nbt format.
     *
     * @param list the list to write.
     * @return     this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> putList(@NotNull List<T> list) {
        final TagType<Object> type;
        if (list.isEmpty()) {
            type = TagType.END;
        } else {
            type = mapper.type(list.get(0));
        }

        this.put(type.id());
        this.putInt(list.size());

        for (T t : list) {
            this.putTag(type, t);
        }
        return this;
    }

    /**
     * Writes the provided compound map into delegated byte buffer according nbt format.
     *
     * @param map the compound map to write.
     * @return    this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> putCompound(@NotNull Map<String, T> map) {
        for (Map.Entry<String, T> entry : map.entrySet()) {
            final Object value = entry.getValue() == null ? null : mapper.extract(entry.getValue());
            final TagType<Object> type = mapper.type(entry.getValue());
            this.put(type.id());
            if (type != TagType.END) {
                this.putString(entry.getKey());
                this.putTag(type, value);
            }
        }
        this.put(Tag.END);
        return this;
    }

    /**
     * Writes a tag object.<br>
     * This method doesn't perform any tag ID write, it only writes a tag value.
     *
     * @param tag the tag object to write.
     * @return    this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> putTag(@Nullable T tag) {
        final Object value = tag == null ? null : mapper.extract(tag);
        final TagType<Object> type = mapper.type(tag);
        this.putTag(type, value);
        return this;
    }

    /**
     * Writes a tag value with associated tag type.<br>
     * This method doesn't perform any tag ID write, it only writes a tag value.
     *
     * @param type the tag type that will be written.
     * @param tag  the tag value to write.
     * @return     this instance.
     */
    @NotNull
    @Contract("_, _ -> this")
    @SuppressWarnings("unchecked")
    public TagBuffer<T> putTag(@NotNull TagType<?> type, @Nullable Object tag) {
        if (type == TagType.END || tag == null) {
            return this;
        }
        switch (type.id()) {
            case Tag.BYTE:
                if (tag instanceof Boolean) {
                    return this.putBoolean((Boolean) tag);
                } else {
                    return this.put((byte) tag);
                }
            case Tag.SHORT:
                return this.putShort((short) tag);
            case Tag.INT:
                return this.putInt((int) tag);
            case Tag.LONG:
                return this.putLong((long) tag);
            case Tag.FLOAT:
                return this.putFloat((float) tag);
            case Tag.DOUBLE:
                return this.putDouble((double) tag);
            case Tag.BYTE_ARRAY:
                if (type == TagType.BOOLEAN_ARRAY) {
                    return this.putByteArray(mapper.byteArray(tag));
                } else {
                    return this.putByteArray((byte[]) tag);
                }
            case Tag.STRING:
                return this.putString((String) tag);
            case Tag.LIST:
                return this.putList((List<T>) tag);
            case Tag.COMPOUND:
                return this.putCompound((Map<String, T>) tag);
            case Tag.INT_ARRAY:
                return this.putIntArray((int[]) tag);
            case Tag.LONG_ARRAY:
                return this.putLongArray((long[]) tag);
            default:
                throw new IllegalArgumentException("Invalid tag type: " + type.name());
        }
    }

    /**
     * Writes a tag object with unnamed tag format.
     *
     * @param tag the tag object to write.
     * @return    this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> putUnnamedTag(@Nullable T tag) {
        final Object value = tag == null ? null : mapper.extract(tag);
        final TagType<Object> type = mapper.type(tag);
        this.put(type.id());
        if (type == TagType.END) {
            return this;
        }
        // Write empty name
        this.putString("");
        return this.putTag(type, value);
    }

    /**
     * Writes a tag object with any tag format.
     *
     * @param tag the tag object to write.
     * @return    this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> putAnyTag(@Nullable T tag) {
        final Object value = tag == null ? null : mapper.extract(tag);
        final TagType<Object> type = mapper.type(tag);
        this.put(type.id());
        return this.putTag(type, value);
    }

    /**
     * Writes a tag object as bedrock file format.
     *
     * @param tag the tag object to write.
     * @return    this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> putBedrockFile(@Nullable T tag) {
        return putBedrockFile(mapper.size(tag), tag);
    }

    /**
     * Writes a tag object as bedrock file format with provided header size.
     *
     * @param size the byte size of the actual tag.
     * @param tag  the tag object to write.
     * @return     this instance.
     */
    @NotNull
    @Contract("_, _ -> this")
    public TagBuffer<T> putBedrockFile(int size, @Nullable T tag) {
        return putBedrockFile(Tag.DEFAULT_BEDROCK_VERSION, size, tag);
    }

    /**
     * Writes a tag object as bedrock file format with provided header version and size.
     *
     * @param version the header version of file.
     * @param size    the byte size of the actual tag.
     * @param tag     the tag object to write.
     * @return        this instance.
     */
    @NotNull
    @Contract("_, _, _ -> this")
    public TagBuffer<T> putBedrockFile(int version, int size, @Nullable T tag) {
        this.putInt(version);
        this.putInt(size);
        return putAnyTag(tag);
    }

    /**
     * Flips the delegated buffer.
     *
     * @return this instance.
     */
    @NotNull
    @Contract("-> this")
    public TagBuffer<T> flip() {
        buffer.flip();
        return this;
    }
}
