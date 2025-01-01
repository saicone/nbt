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
public class WriteReverseIntegerBenchmark {

    private static final VarHandle INT = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);

    private DataOutputStream stream;
    private final byte[] buffer = new byte[Integer.BYTES];

    @Setup(value = Level.Invocation)
    public void setup() {
        stream = new DataOutputStream(new ByteArrayOutputStream());
    }

    @Benchmark
    public void byteArray() throws IOException {
        writeByteArray(1234);
    }

    public void writeByteArray(int v) throws IOException {
        INT.set(buffer, 0, v);
        stream.write(buffer, 0, Integer.BYTES);
    }

    @Benchmark
    public void explicit() throws IOException {
        writeExplicit(1234);
    }

    public void writeExplicit(int v) throws IOException {
        stream.write(0xff & v);
        stream.write(0xff & (v >> 8));
        stream.write(0xff & (v >> 16));
        stream.write(0xff & (v >> 24));
    }

    @Benchmark
    public void loop() throws IOException {
        writeLoop(1234);
    }

    public void writeLoop(int v) throws IOException {
        for (int shift = 0; shift < Integer.SIZE; shift += 8) {
            stream.write(0xff & (v >> shift));
        }
    }

    @Benchmark
    public void reverse() throws IOException {
        writeReverse(1234);
    }

    public void writeReverse(int v) throws IOException {
        stream.writeInt(Integer.reverseBytes(v));
    }
}
