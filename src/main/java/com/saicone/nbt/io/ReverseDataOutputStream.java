package com.saicone.nbt.io;

import org.jetbrains.annotations.NotNull;

import java.io.DataOutput;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * A reverse data output stream do the same functionality as {@link java.io.DataOutputStream} but writes every
 * number with its bytes reversed, in other words, using little endian encoding.<br>
 * Instead of {@link java.io.DataOutputStream}, it uses a simplified method to write Strings as UTF-8 format.
 *
 * @author Rubenicos
 */
public class ReverseDataOutputStream extends FilterOutputStream implements DataOutput {

    private static final VarHandle SHORT = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle INT = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle LONG = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    private final byte[] shortBuffer = new byte[Short.BYTES];
    private final byte[] intBuffer = new byte[Integer.BYTES];
    private final byte[] longBuffer = new byte[Long.BYTES];

    /**
     * Constructs a reverse data output stream.
     *
     * @param out the delegated output stream to write bytes.
     */
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
        SHORT.set(shortBuffer, 0, (short) v);
        out.write(shortBuffer, 0, Short.BYTES);
    }

    @Override
    public void writeChar(int v) throws IOException {
        SHORT.set(shortBuffer, 0, (short) v);
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
        // We can't use DataOutputStream.writeUTF(String, DataOutput) due it doesn't call DataOutput#writeShort(int)
        final byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeShort(bytes.length);
        this.write(bytes);
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
