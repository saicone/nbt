package com.saicone.nbt.io;

import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/**
 * A reverse data input stream do the same functionality as {@link java.io.DataInputStream} but read every
 * number with its bytes reversed, in other words, using little endian decoding.
 *
 * @author Rubenicos
 */
public class ReverseDataInputStream extends FilterInputStream implements DataInput {

    private static final VarHandle INT = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle LONG = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    private final byte[] intBuffer = new byte[Integer.BYTES];
    private final byte[] longBuffer = new byte[Long.BYTES];

    /**
     * Constructs a reverse data input stream.
     *
     * @param in the delegated input stream to write bytes.
     */
    public ReverseDataInputStream(@NotNull InputStream in) {
        super(in);
    }

    @Override
    public void readFully(@NotNull byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    @Override
    public void readFully(@NotNull byte[] b, int off, int len) throws IOException {
        int n = 0;
        while (n < len) {
            int count = in.read(b, off + n, len - n);
            if (count < 0)
                throw new EOFException();
            n += count;
        }
    }

    @Override
    public int skipBytes(int n) throws IOException {
        int total = 0;
        int cur;

        while ((total<n) && ((cur = (int) in.skip(n-total)) > 0)) {
            total += cur;
        }

        return total;
    }

    @Override
    public boolean readBoolean() throws IOException {
        return readUnsignedByte() != 0;
    }

    @Override
    public byte readByte() throws IOException {
        return (byte) readUnsignedByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        final int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return ch;
    }

    @Override
    public short readShort() throws IOException {
        final int ch1 = readUnsignedByte();
        final int ch2 = readUnsignedByte();

        return (short) (ch2 << 8 | ch1);
    }

    @Override
    public int readUnsignedShort() throws IOException {
        final int ch1 = readUnsignedByte();
        final int ch2 = readUnsignedByte();

        return (ch2 << 8 | ch1) & 0xffff;
    }

    @Override
    public char readChar() throws IOException {
        final int ch1 = readUnsignedByte();
        final int ch2 = readUnsignedByte();

        return (char) ((ch2 << 8 | ch1) & 0xffff);
    }

    @Override
    public int readInt() throws IOException {
        readFully(intBuffer, 0, Integer.BYTES);
        return (int) INT.get(intBuffer, 0);
    }

    @Override
    public long readLong() throws IOException {
        readFully(longBuffer, 0, Long.BYTES);
        return (long) LONG.get(longBuffer, 0);
    }

    @Override
    public float readFloat() throws IOException {
        readFully(intBuffer, 0, Integer.BYTES);
        return Float.intBitsToFloat((int) INT.get(intBuffer, 0));
    }

    @Override
    public double readDouble() throws IOException {
        readFully(longBuffer, 0, Long.BYTES);
        return Double.longBitsToDouble((long) LONG.get(longBuffer, 0));
    }

    @Override
    public String readLine() {
        throw new IllegalStateException();
    }

    @Override
    public @NotNull String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }
}
