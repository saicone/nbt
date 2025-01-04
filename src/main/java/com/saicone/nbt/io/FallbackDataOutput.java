package com.saicone.nbt.io;

import org.jetbrains.annotations.NotNull;

import java.io.DataOutput;
import java.io.IOException;
import java.io.UTFDataFormatException;

/**
 * A delegated-first data output implementation that executes all the methods into delegated {@link DataOutput}.<br>
 * The main difference is that any {@link UTFDataFormatException} will make the method to be executed again
 * with an empty string instead.
 *
 * @author Rubenicos
 */
public class FallbackDataOutput implements DataOutput {

    private final DataOutput output;

    /**
     * Constructs a fallback data output with delegated {@link DataOutput}.
     *
     * @param output the data output to delegate all the methods.
     */
    public FallbackDataOutput(@NotNull DataOutput output) {
        this.output = output;
    }

    @Override
    public void write(int b) throws IOException {
        this.output.write(b);
    }

    @Override
    public void write(@NotNull byte[] b) throws IOException {
        this.output.write(b);
    }

    @Override
    public void write(@NotNull byte[] b, int off, int len) throws IOException {
        this.output.write(b, off, len);
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        this.output.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        this.output.writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        this.output.writeShort(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        this.output.writeChar(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        this.output.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        this.output.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        this.output.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        this.output.writeDouble(v);
    }

    @Override
    public void writeBytes(@NotNull String s) throws IOException {
        try {
            this.output.writeBytes(s);
        } catch (UTFDataFormatException e) {
            this.output.writeBytes("");
        }
    }

    @Override
    public void writeChars(@NotNull String s) throws IOException {
        try {
            this.output.writeChars(s);
        } catch (UTFDataFormatException e) {
            this.output.writeChars("");
        }
    }

    @Override
    public void writeUTF(@NotNull String s) throws IOException {
        try {
            this.output.writeUTF(s);
        } catch (UTFDataFormatException e) {
            this.output.writeUTF("");
        }
    }
}
