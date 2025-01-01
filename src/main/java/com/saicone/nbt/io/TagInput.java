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

public class TagInput<T> implements Closeable {

    private final DataInput input;
    private final TagMapper<T> mapper;

    private long maxQuota = Tag.DEFAULT_NBT_QUOTA;
    private long remainingQuota = Tag.DEFAULT_NBT_QUOTA;
    private int remainingDepth = Tag.MAX_STACK_DEPTH;

    @NotNull
    public static TagInput<Object> of(@NotNull DataInput input) {
        return of(input, TagMapper.DEFAULT);
    }

    @NotNull
    public static <T> TagInput<T> of(@NotNull DataInput input, @NotNull TagMapper<T> mapper) {
        return new TagInput<>(input, mapper);
    }

    public TagInput(@NotNull DataInput input, @NotNull TagMapper<T> mapper) {
        this.input = input;
        this.mapper = mapper;
    }

    @NotNull
    @Contract("_ -> this")
    public TagInput<T> quota(long quota) {
        this.maxQuota = quota;
        this.remainingQuota = quota;
        return this;
    }

    @NotNull
    @Contract("_ -> this")
    public TagInput<T> maxDepth(int maxDepth) {
        this.remainingDepth = maxDepth;
        return this;
    }

    @NotNull
    public DataInput getInput() {
        return input;
    }

    @NotNull
    public TagMapper<T> getMapper() {
        return mapper;
    }

    public long getRemainingQuota() {
        return remainingQuota;
    }

    public int getRemainingDepth() {
        return remainingDepth;
    }

    protected void useBytes(long size, long amount) {
        useBytes(size * amount);
    }

    protected void useBytes(long bytes) {
        if ((remainingQuota -= bytes) < 0) {
            throw new IllegalArgumentException("Cannot read tag bigger than " + this.maxQuota + " bytes");
        }
    }

    protected void incrementDepth() {
        if (--remainingDepth < 0) {
            throw new IllegalArgumentException("Cannot read tag with too many nested values");
        }
    }

    protected void decrementDepth() {
        remainingDepth++;
    }

    public <A extends T> A readUnnamed() throws IOException {
        final byte id = input.readByte();
        final TagType<Object> type = TagType.getType(id);
        if (type == TagType.END) {
            return getMapper().buildAny(type, null);
        }
        // Skip ""
        input.skipBytes(input.readUnsignedShort());
        return readTag(type);
    }

    public <A extends T> A readAny() throws IOException {
        final byte id = input.readByte();
        final TagType<Object> type = TagType.getType(id);
        if (type == TagType.END) {
            return getMapper().buildAny(type, null);
        }
        return readTag(type);
    }

    public <A extends T> A readBedrockFile() throws IOException {
        // Skip header
        input.skipBytes(8);
        return readAny();
    }

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

    protected long[] readLongArray() throws IOException {
        final int size = input.readInt();
        useBytes(Long.BYTES, size);
        final long[] array = new long[size];
        for (int i = 0; i < size; i++) {
            array[i] = input.readLong();
        }
        return array;
    }

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
