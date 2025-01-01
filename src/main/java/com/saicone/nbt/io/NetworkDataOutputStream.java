package com.saicone.nbt.io;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class NetworkDataOutputStream extends ReverseDataOutputStream {

    private static final int SHIFT_32 = Integer.SIZE - 1;
    private static final long SHIFT_64 = Long.SIZE - 1;
    private static final int MASK_32 = 0x7F;
    private static final long MASK_64 = 0x7FL;
    private static final int CON_32 = 0x80;
    private static final long CON_64 = 0x80L;

    public NetworkDataOutputStream(@NotNull OutputStream out) {
        super(out);
    }

    public void writeUnsignedVarInt32(int v) throws IOException {
        // Taken from https://steinborn.me/posts/performance/how-fast-can-you-write-a-varint/
        if ((v >>> 7) == 0) {
            write((byte) v);
        } else if ((v >>> 14) == 0) {
            write((byte) (v & MASK_32 | CON_32));
            write((byte) (v >>> 7));
        } else if ((v >>> 21) == 0) {
            write((byte) (v & MASK_32 | CON_32));
            write((byte) ((v >>> 7) & MASK_32 | CON_32));
            write((byte) (v >>> 14));
        } else if ((v >>> 28) == 0) {
            write((byte) (v & MASK_32 | CON_32));
            write((byte) ((v >>> 7) & MASK_32 | CON_32));
            write((byte) ((v >>> 14) & MASK_32 | CON_32));
            write((byte) (v >>> 21));
        } else {
            write((byte) (v & MASK_32 | CON_32));
            write((byte) ((v >>> 7) & MASK_32 | CON_32));
            write((byte) ((v >>> 14) & MASK_32 | CON_32));
            write((byte) ((v >>> 21) & MASK_32 | CON_32));
            write((byte) (v >>> 28));
        }
    }

    public void writeUnsignedVarInt64(long v) throws IOException {
        // Taken & extended from https://steinborn.me/posts/performance/how-fast-can-you-write-a-varint/
        if ((v >>> 7) == 0) {
            write((byte) v);
        } else if ((v >>> 14) == 0) {
            write((byte) (v & MASK_64 | CON_64));
            write((byte) (v >>> 7));
        } else if ((v >>> 21) == 0) {
            write((byte) (v & MASK_64 | CON_64));
            write((byte) ((v >>> 7) & MASK_64 | CON_64));
            write((byte) (v >>> 14));
        } else if ((v >>> 28) == 0) {
            write((byte) (v & MASK_64 | CON_64));
            write((byte) ((v >>> 7) & MASK_64 | CON_64));
            write((byte) ((v >>> 14) & MASK_64 | CON_64));
            write((byte) (v >>> 21));
        } else if ((v >>> 35) == 0) {
            write((byte) (v & MASK_64 | CON_64));
            write((byte) ((v >>> 7) & MASK_64 | CON_64));
            write((byte) ((v >>> 14) & MASK_64 | CON_64));
            write((byte) ((v >>> 21) & MASK_64 | CON_64));
            write((byte) (v >>> 28));
        } else if ((v >>> 42) == 0) {
            write((byte) (v & MASK_64 | CON_64));
            write((byte) ((v >>> 7) & MASK_64 | CON_64));
            write((byte) ((v >>> 14) & MASK_64 | CON_64));
            write((byte) ((v >>> 21) & MASK_64 | CON_64));
            write((byte) ((v >>> 28) & MASK_64 | CON_64));
            write((byte) (v >>> 35));
        } else if ((v >>> 49) == 0) {
            write((byte) (v & MASK_64 | CON_64));
            write((byte) ((v >>> 7) & MASK_64 | CON_64));
            write((byte) ((v >>> 14) & MASK_64 | CON_64));
            write((byte) ((v >>> 21) & MASK_64 | CON_64));
            write((byte) ((v >>> 28) & MASK_64 | CON_64));
            write((byte) ((v >>> 35) & MASK_64 | CON_64));
            write((byte) (v >>> 42));
        } else if ((v >>> 56) == 0) {
            write((byte) (v & MASK_64 | CON_64));
            write((byte) ((v >>> 7) & MASK_64 | CON_64));
            write((byte) ((v >>> 14) & MASK_64 | CON_64));
            write((byte) ((v >>> 21) & MASK_64 | CON_64));
            write((byte) ((v >>> 28) & MASK_64 | CON_64));
            write((byte) ((v >>> 35) & MASK_64 | CON_64));
            write((byte) ((v >>> 42) & MASK_64 | CON_64));
            write((byte) (v >>> 49));
        } else if ((v >>> 63) == 0) {
            write((byte) (v & MASK_64 | CON_64));
            write((byte) ((v >>> 7) & MASK_64 | CON_64));
            write((byte) ((v >>> 14) & MASK_64 | CON_64));
            write((byte) ((v >>> 21) & MASK_64 | CON_64));
            write((byte) ((v >>> 28) & MASK_64 | CON_64));
            write((byte) ((v >>> 35) & MASK_64 | CON_64));
            write((byte) ((v >>> 42) & MASK_64 | CON_64));
            write((byte) ((v >>> 49) & MASK_64 | CON_64));
            write((byte) (v >>> 56));
        } else {
            write((byte) (v & MASK_64 | CON_64));
            write((byte) ((v >>> 7) & MASK_64 | CON_64));
            write((byte) ((v >>> 14) & MASK_64 | CON_64));
            write((byte) ((v >>> 21) & MASK_64 | CON_64));
            write((byte) ((v >>> 28) & MASK_64 | CON_64));
            write((byte) ((v >>> 35) & MASK_64 | CON_64));
            write((byte) ((v >>> 42) & MASK_64 | CON_64));
            write((byte) ((v >>> 49) & MASK_64 | CON_64));
            write((byte) ((v >>> 56) & MASK_64 | CON_64));
            write((byte) (v >>> 63));
        }
    }

    @Override
    public void writeInt(int v) throws IOException {
        // ZigZag encode
        writeUnsignedVarInt32( (v >> SHIFT_32) ^ (v << 1));
    }

    @Override
    public void writeLong(long v) throws IOException {
        // ZigZag encode
        writeUnsignedVarInt64( (v >> SHIFT_64) ^ (v << 1));
    }

    @Override
    public void writeUTF(@NotNull String s) throws IOException {
        // For some reason, the Strings use an unsigned VarInt32 for length
        final byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeUnsignedVarInt32(bytes.length);
        this.write(bytes);
    }
}
