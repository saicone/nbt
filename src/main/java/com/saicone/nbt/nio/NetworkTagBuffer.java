package com.saicone.nbt.nio;

import com.saicone.nbt.TagMapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * A tag buffer that do the same functionality as {@link TagBuffer} but manages
 * integers as VarInt32, longs as VarInt64 and String size as unsigned VarInt32.<br>
 * This implementation aims to be same as Minecraft Bedrock network data decoding/encoding.
 *
 * @author Rubenicos
 *
 * @param <T> the tag object implementation.
 */
public class NetworkTagBuffer<T> extends TagBuffer<T> {

    private static final int SHIFT_32 = Integer.SIZE - 1;
    private static final long SHIFT_64 = Long.SIZE - 1;
    private static final int MASK_32 = 0x7F;
    private static final long MASK_64 = 0x7FL;
    private static final int CON_32 = 0x80;
    private static final long CON_64 = 0x80L;

    /**
     * Create a network tag buffer that use nbt-represented java objects with provided {@link ByteBuffer}.
     *
     * @param buffer the buffer that will provide/receive data.
     * @return       a newly generated tag buffer.
     */
    @NotNull
    public static TagBuffer<Object> of(@NotNull ByteBuffer buffer) {
        return of(buffer, TagMapper.DEFAULT);
    }

    /**
     * Create a network tag buffer with provided {@link ByteBuffer} and {@link TagMapper}.
     *
     * @param buffer the buffer that will provide/receive data.
     * @param mapper the mapper for tag object implementation.
     * @return       a newly generated tag buffer.
     * @param <T>    the tag object implementation.
     */
    @NotNull
    public static <T> TagBuffer<T> of(@NotNull ByteBuffer buffer, @NotNull TagMapper<T> mapper) {
        return new NetworkTagBuffer<>(buffer, mapper);
    }

    /**
     * Construct a network tag buffer with provided {@link ByteBuffer} and {@link TagMapper}.
     *
     * @param buffer the buffer that will provide/receive data.
     * @param mapper the mapper for tag object implementation.
     */
    public NetworkTagBuffer(@NotNull ByteBuffer buffer, @NotNull TagMapper<T> mapper) {
        super(buffer, mapper);
    }

    /**
     * Reads an unsigned VarInt32 as integer.
     *
     * @return a integer value.
     */
    public int getUnsignedVarInt32() {
        int result = 0;
        for (int shift = 0; shift < Integer.SIZE; shift += 7) {
            final byte value = this.get();
            result |= (value & MASK_32) << shift;
            if ((value & CON_32) == 0) {
                return result;
            }
        }
        throw new IllegalArgumentException("VarInt32 exceeds 32 bits");
    }

    /**
     * Reads an unsigned VarInt64 as long.
     *
     * @return a long value.
     */
    public long getUnsignedVarInt64() {
        long result = 0;
        for (int shift = 0; shift < Long.SIZE; shift += 7) {
            final byte value = this.get();
            result |= (value & MASK_64) << shift;
            if ((value & CON_32) == 0) {
                return result;
            }
        }
        throw new IllegalArgumentException("VarInt64 exceeds 64 bits");
    }

    @Override
    public int getInt() {
        final int result = getUnsignedVarInt32();
        // ZigZag decode
        return (result >>> 1) ^ -(result & 1);
    }

    @Override
    public long getLong() {
        final long result = getUnsignedVarInt64();
        // ZigZag decode
        return (result >>> 1) ^ -(result & 1);
    }

    @Override
    public @NotNull String getString() {
        // The Strings use an unsigned VarInt32 without ZigZag decode for length
        final byte[] bytes = new byte[getUnsignedVarInt32()];
        this.buffer().get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Writes the provided integer as VarInt32 using Andrew Steinborn blended method with a little optimization.
     *
     * @param value the int to write.
     * @return      this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> putUnsignedVarInt32(int value) {
        // Taken from https://steinborn.me/posts/performance/how-fast-can-you-write-a-varint/
        if ((value >>> 7) == 0) {
            this.put((byte) value);
        } else if ((value >>> 14) == 0) {
            this.put((byte) (value & MASK_32 | CON_32));
            this.put((byte) (value >>> 7));
        } else if ((value >>> 21) == 0) {
            this.put((byte) (value & MASK_32 | CON_32));
            this.put((byte) ((value >>> 7) & MASK_32 | CON_32));
            this.put((byte) (value >>> 14));
        } else if ((value >>> 28) == 0) {
            this.put((byte) (value & MASK_32 | CON_32));
            this.put((byte) ((value >>> 7) & MASK_32 | CON_32));
            this.put((byte) ((value >>> 14) & MASK_32 | CON_32));
            this.put((byte) (value >>> 21));
        } else {
            this.put((byte) (value & MASK_32 | CON_32));
            this.put((byte) ((value >>> 7) & MASK_32 | CON_32));
            this.put((byte) ((value >>> 14) & MASK_32 | CON_32));
            this.put((byte) ((value >>> 21) & MASK_32 | CON_32));
            this.put((byte) (value >>> 28));
        }
        return this;
    }

    /**
     * Writes the provided long as VarInt64 using Andrew Steinborn blended method extended for long values with a little optimization.
     *
     * @param value the long to write.
     * @return      this instance.
     */
    @NotNull
    @Contract("_ -> this")
    public TagBuffer<T> putUnsignedVarInt64(long value) {
        // Taken & extended from https://steinborn.me/posts/performance/how-fast-can-you-write-a-varint/
        if ((value >>> 7) == 0) {
            this.put((byte) value);
        } else if ((value >>> 14) == 0) {
            this.put((byte) (value & MASK_64 | CON_64));
            this.put((byte) (value >>> 7));
        } else if ((value >>> 21) == 0) {
            this.put((byte) (value & MASK_64 | CON_64));
            this.put((byte) ((value >>> 7) & MASK_64 | CON_64));
            this.put((byte) (value >>> 14));
        } else if ((value >>> 28) == 0) {
            this.put((byte) (value & MASK_64 | CON_64));
            this.put((byte) ((value >>> 7) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 14) & MASK_64 | CON_64));
            this.put((byte) (value >>> 21));
        } else if ((value >>> 35) == 0) {
            this.put((byte) (value & MASK_64 | CON_64));
            this.put((byte) ((value >>> 7) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 14) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 21) & MASK_64 | CON_64));
            this.put((byte) (value >>> 28));
        } else if ((value >>> 42) == 0) {
            this.put((byte) (value & MASK_64 | CON_64));
            this.put((byte) ((value >>> 7) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 14) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 21) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 28) & MASK_64 | CON_64));
            this.put((byte) (value >>> 35));
        } else if ((value >>> 49) == 0) {
            this.put((byte) (value & MASK_64 | CON_64));
            this.put((byte) ((value >>> 7) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 14) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 21) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 28) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 35) & MASK_64 | CON_64));
            this.put((byte) (value >>> 42));
        } else if ((value >>> 56) == 0) {
            this.put((byte) (value & MASK_64 | CON_64));
            this.put((byte) ((value >>> 7) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 14) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 21) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 28) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 35) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 42) & MASK_64 | CON_64));
            this.put((byte) (value >>> 49));
        } else if ((value >>> 63) == 0) {
            this.put((byte) (value & MASK_64 | CON_64));
            this.put((byte) ((value >>> 7) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 14) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 21) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 28) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 35) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 42) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 49) & MASK_64 | CON_64));
            this.put((byte) (value >>> 56));
        } else {
            this.put((byte) (value & MASK_64 | CON_64));
            this.put((byte) ((value >>> 7) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 14) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 21) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 28) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 35) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 42) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 49) & MASK_64 | CON_64));
            this.put((byte) ((value >>> 56) & MASK_64 | CON_64));
            this.put((byte) (value >>> 63));
        }
        return this;
    }

    @Override
    public @NotNull TagBuffer<T> putInt(int value) {
        // ZigZag encode
        return putUnsignedVarInt32((value >> SHIFT_32) ^ (value << 1));
    }

    @Override
    public @NotNull TagBuffer<T> putLong(long value) {
        // ZigZag encode
        return putUnsignedVarInt64((value >> SHIFT_64) ^ (value << 1));
    }

    @Override
    public @NotNull TagBuffer<T> putString(@NotNull String value) {
        // The Strings use an unsigned VarInt32 without ZigZag encode for length
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        putUnsignedVarInt32(bytes.length);
        this.buffer().put(bytes);
        return this;
    }
}