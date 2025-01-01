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
public class ReadReverseLongBenchmark {

    private static final VarHandle LONG = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    private DataInputStream stream;
    private final byte[] buffer = new byte[Long.BYTES];

    @Setup(value = Level.Invocation)
    public void setup() {
        final byte[] bytes = new byte[] {
                (byte) 0x15, (byte) 0xCD, (byte) 0x5B, (byte) 0x07,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x11
        };
        stream = new DataInputStream(new ByteArrayInputStream(bytes));
    }

    @Benchmark
    public void byteArray(Blackhole bh) throws IOException {
        bh.consume(readByteArray());
    }

    public long readByteArray() throws IOException {
        stream.readFully(buffer, 0, Long.BYTES);
        return (long) LONG.get(buffer, 0);
    }


    @Benchmark
    public void explicit(Blackhole bh) throws IOException {
        bh.consume(readExplicit());
    }

    public long readExplicit() throws IOException {
        final long ch1 = stream.readUnsignedByte();
        final long ch2 = stream.readUnsignedByte();
        final long ch3 = stream.readUnsignedByte();
        final long ch4 = stream.readUnsignedByte();
        final long ch5 = stream.readUnsignedByte();
        final long ch6 = stream.readUnsignedByte();
        final long ch7 = stream.readUnsignedByte();
        final long ch8 = stream.readUnsignedByte();

        return ch8 << 56
                | ch7 << 48
                | ch6 << 40
                | ch5 << 32
                | ch4 << 24
                | ch3 << 16
                | ch2 << 8
                | ch1;
    }

    @Benchmark
    public void loop(Blackhole bh) throws IOException {
        bh.consume(readLoop());
    }

    public long readLoop() throws IOException {
        long result = 0;
        for (int shift = 0; shift < Long.SIZE; shift += 8) {
            result |= (long) stream.readUnsignedByte() << shift;
        }
        return result;
    }

    @Benchmark
    public void reverse(Blackhole bh) throws IOException {
        bh.consume(readReverse());
    }

    public long readReverse() throws IOException {
        return Long.reverseBytes(stream.readLong());
    }
}
