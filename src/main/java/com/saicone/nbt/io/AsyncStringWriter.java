package com.saicone.nbt.io;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

/**
 * A character stream that do the same functionality as {@link java.io.StringWriter} but using a {@link StringBuilder}
 * instead, that provides an asynchronous implementation of string writing.<br>
 * Same as {@link java.io.StringWriter}, closing this implementation has no effect since string builder is not
 * a closeable implementation.
 *
 * @author Rubenicos
 */
public class AsyncStringWriter extends Writer {

    private final StringBuilder builder;

    /**
     * Constructs an asynchronous string writer with newly generated string builder.
     */
    public AsyncStringWriter() {
        this(new StringBuilder());
    }

    /**
     * Constructs an asynchronous string writer.
     *
     * @param builder the string builder to append all characters.
     */
    public AsyncStringWriter(@NotNull StringBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void write(int c) throws IOException {
        builder.append((char) c);
    }

    @Override
    public void write(@NotNull char[] cbuf, int off, int len) throws IOException {
        builder.append(cbuf, off, len);
    }

    @Override
    public void write(@NotNull String str) throws IOException {
        builder.append(str);
    }

    @Override
    public void write(@NotNull String str, int off, int len) throws IOException {
        builder.append(str, off, off + len);
    }

    @Override
    public AsyncStringWriter append(@Nullable CharSequence csq) throws IOException {
        write(String.valueOf(csq));
        return this;
    }

    @Override
    public AsyncStringWriter append(@Nullable CharSequence csq, int start, int end) throws IOException {
        return append(Objects.requireNonNullElse(csq, "null").subSequence(start, end));
    }

    @Override
    public AsyncStringWriter append(char c) throws IOException {
        write(c);
        return this;
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    /**
     * Get current string builder used to append characters.
     *
     * @return a string builder.
     */
    @NotNull
    public StringBuilder getBuilder() {
        return builder;
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
