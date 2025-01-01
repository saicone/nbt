package com.saicone.nbt.io;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class WriteReverseLongBenchmark {


    private static final VarHandle LONG = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    private DataOutputStream stream;
    private final byte[] buffer = new byte[Long.BYTES];

    @Setup(value = Level.Invocation)
    public void setup() {
        stream = new DataOutputStream(new ByteArrayOutputStream());
    }

    @Benchmark
    public void byteArray() throws IOException {
        writeByteArray(1234);
    }

    public void writeByteArray(long v) throws IOException {
        LONG.set(buffer, 0, v);
        stream.write(buffer, 0, Long.BYTES);
    }

    @Benchmark
    public void explicit() throws IOException {
        writeExplicit(1234);
    }

    public void writeExplicit(long v) throws IOException {
        stream.write((int) (0xff & v));
        stream.write((int) (0xff & (v >> 8)));
        stream.write((int) (0xff & (v >> 16)));
        stream.write((int) (0xff & (v >> 24)));
        stream.write((int) (0xff & (v >> 32)));
        stream.write((int) (0xff & (v >> 40)));
        stream.write((int) (0xff & (v >> 48)));
        stream.write((int) (0xff & (v >> 56)));
    }

    @Benchmark
    public void loop() throws IOException {
        writeLoop(1234);
    }

    public void writeLoop(long v) throws IOException {
        for (int shift = 0; shift < Long.SIZE; shift += 8) {
            stream.write((int) (0xff & (v >> shift)));
        }
    }

    @Benchmark
    public void reverse() throws IOException {
        writeReverse(1234);
    }

    public void writeReverse(long v) throws IOException {
        stream.writeLong(Long.reverseBytes(v));
    }
}
