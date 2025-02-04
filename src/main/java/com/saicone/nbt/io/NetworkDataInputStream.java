package com.saicone.nbt.io;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * A network data input stream do the same functionality as {@link ReverseDataInputStream} but read every
 * integer as VarInt32 and long as VarInt64.<br>
 * This implementation aims to be same as Minecraft Bedrock network data decoding.<br>
 *
 * @author Rubenicos
 */
public class NetworkDataInputStream extends ReverseDataInputStream {

    private static final int MASK_32 = 0x7F;
    private static final long MASK_64 = 0x7FL;
    private static final int CON = 0x80;

    /**
     * Constructs a network data input stream.
     *
     * @param in the delegated input stream to write bytes.
     */
    public NetworkDataInputStream(@NotNull InputStream in) {
        super(in);
    }

    /**
     * Read an unsigned VarInt32 as integer.
     *
     * @return a integer value.
     * @throws IOException if any I/O error occurs.
     */
    public int readUnsignedVarInt32() throws IOException {
        int result = 0;
        for (int shift = 0; shift < Integer.SIZE; shift += 7) {
            final byte value = this.readByte();
            result |= (value & MASK_32) << shift;
            if ((value & CON) == 0) {
                return result;
            }
        }
        throw new IOException("VarInt32 exceeds 32 bits");
    }

    /**
     * Read an unsigned VarInt64 as integer.
     *
     * @return a integer value.
     * @throws IOException if any I/O error occurs.
     */
    public long readUnsignedVarInt64() throws IOException {
        long result = 0;
        for (int shift = 0; shift < Long.SIZE; shift += 7) {
            final byte value = this.readByte();
            result |= (value & MASK_64) << shift;
            if ((value & CON) == 0) {
                return result;
            }
        }
        throw new IOException("VarInt64 exceeds 64 bits");
    }

    @Override
    public int readInt() throws IOException {
        final int result = readUnsignedVarInt32();
        // ZigZag decode
        return (result >>> 1) ^ -(result & 1);
    }

    @Override
    public long readLong() throws IOException {
        final long result = readUnsignedVarInt64();
        // ZigZag decode
        return (result >>> 1) ^ -(result & 1);
    }

    @Override
    public @NotNull String readUTF() throws IOException {
        // For some reason, the Strings use an unsigned VarInt32 for length
        final byte[] bytes = new byte[readUnsignedVarInt32()];
        readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
