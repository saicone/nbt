package com.saicone.nbt.io;

import org.jetbrains.annotations.NotNull;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

public class ReverseDataOutputStream extends FilterOutputStream implements DataOutput {

    private static final VarHandle SHORT = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle INT = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle LONG = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    private final byte[] shortBuffer = new byte[Short.BYTES];
    private final byte[] intBuffer = new byte[Integer.BYTES];
    private final byte[] longBuffer = new byte[Long.BYTES];

    public ReverseDataOutputStream(@NotNull OutputStream out) {
        super(out);
    }

    @Override
    public void write(@NotNull byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(@NotNull byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        out.write(v ? 1 : 0);
    }

    @Override
    public void writeByte(int v) throws IOException {
        out.write(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        SHORT.set(shortBuffer, 0, v);
        out.write(shortBuffer, 0, Short.BYTES);
    }

    @Override
    public void writeChar(int v) throws IOException {
        SHORT.set(shortBuffer, 0, v);
        out.write(shortBuffer, 0, Short.BYTES);
    }

    @Override
    public void writeInt(int v) throws IOException {
        INT.set(intBuffer, 0, v);
        out.write(intBuffer, 0, Integer.BYTES);
    }

    @Override
    public void writeLong(long v) throws IOException {
        LONG.set(longBuffer, 0, v);
        out.write(longBuffer, 0, Long.BYTES);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        INT.set(intBuffer, 0, Float.floatToIntBits(v));
        out.write(intBuffer, 0, Integer.BYTES);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        LONG.set(longBuffer, 0, Double.doubleToLongBits(v));
        out.write(longBuffer, 0, Long.BYTES);
    }

    @Override
    public void writeBytes(@NotNull String s) throws IOException {
        int len = s.length();
        for (int i = 0 ; i < len ; i++) {
            out.write((byte) s.charAt(i));
        }
    }

    @Override
    public void writeChars(@NotNull String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            SHORT.set(shortBuffer, 0, i);
            out.write(shortBuffer, 0, Short.BYTES);
        }
    }

    @Override
    public void writeUTF(@NotNull String s) throws IOException {
        if (out instanceof DataOutputStream) {
            ((DataOutputStream) out).writeUTF(s);
        } else {
            new DataOutputStream(out).writeUTF(s);
        }
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
