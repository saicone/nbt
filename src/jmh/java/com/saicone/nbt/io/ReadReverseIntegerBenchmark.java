package com.saicone.nbt.io;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class ReadReverseIntegerBenchmark {

    private static final VarHandle INT = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);

    private DataInputStream stream;
    private final byte[] buffer = new byte[Integer.BYTES];

    @Setup(value = Level.Invocation)
    public void setup() {
        final byte[] bytes = new byte[] { 0x00, 0x00, 0x04, (byte) 0xD2 };
        stream = new DataInputStream(new ByteArrayInputStream(bytes));
    }

    @Benchmark
    public void byteArray(Blackhole bh) throws IOException {
        bh.consume(readByteArray());
    }

    public int readByteArray() throws IOException {
        stream.readFully(buffer, 0, Integer.BYTES);
        return (int) INT.get(buffer, 0);
    }

    @Benchmark
    public void explicit(Blackhole bh) throws IOException {
        bh.consume(readExplicit());
    }

    public int readExplicit() throws IOException {
        final int ch1 = stream.readUnsignedByte();
        final int ch2 = stream.readUnsignedByte();
        final int ch3 = stream.readUnsignedByte();
        final int ch4 = stream.readUnsignedByte();

        return ch4 << 24
                | ch3 << 16
                | ch2 << 8
                | ch1;
    }

    @Benchmark
    public void loop(Blackhole bh) throws IOException {
        bh.consume(readLoop());
    }

    public int readLoop() throws IOException {
        int result = 0;
        for (int shift = 0; shift < Integer.SIZE; shift += 8) {
            result |= stream.readUnsignedByte() << shift;
        }
        return result;
    }

    @Benchmark
    public void reverse(Blackhole bh) throws IOException {
        bh.consume(readReverse());
    }

    public int readReverse() throws IOException {
        return Integer.reverseBytes(stream.readInt());
    }
}
