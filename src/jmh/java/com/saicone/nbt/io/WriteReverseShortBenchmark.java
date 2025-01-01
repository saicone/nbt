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
public class WriteReverseShortBenchmark {

    private static final VarHandle SHORT = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);

    private DataOutputStream stream;
    private final byte[] buffer = new byte[Short.BYTES];

    @Setup(value = Level.Invocation)
    public void setup() {
        stream = new DataOutputStream(new ByteArrayOutputStream());
    }

    @Benchmark
    public void byteArray() throws IOException {
        writeByteArray((short) 1234);
    }

    public void writeByteArray(short v) throws IOException {
        SHORT.set(buffer, 0, v);
        stream.write(buffer, 0, Short.BYTES);
    }

    @Benchmark
    public void explicit() throws IOException {
        writeExplicit((short) 1234);
    }

    public void writeExplicit(short v) throws IOException {
        stream.write(0xff & v);
        stream.write(0xff & (v >> 8));
    }

    @Benchmark
    public void loop() throws IOException {
        writeLoop((short) 1234);
    }

    public void writeLoop(short v) throws IOException {
        for (int shift = 0; shift < Short.SIZE; shift += 8) {
            stream.write(0xff & (v >> shift));
        }
    }

    @Benchmark
    public void reverse() throws IOException {
        writeReverse((short) 1234);
    }

    public void writeReverse(short v) throws IOException {
        stream.writeShort(Short.reverseBytes(v));
    }
}
