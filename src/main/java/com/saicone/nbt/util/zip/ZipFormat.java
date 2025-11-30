/*
 * This file is part of saicone/gama, licensed under the MIT License
 *
 * Copyright (c) Rubenicos
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.saicone.nbt.util.zip;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Utility class for compression algorithms related methods.<br>
 * Compatibility with gzip, zlib and lz4 is provided by default.
 *
 * @author Rubenicos
 */
public abstract class ZipFormat {

    private static final StandardOpenOption[] DEFAULT_OPEN_OPTIONS = new StandardOpenOption[] {
            StandardOpenOption.SYNC,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
    };
    private static final ZipFormat EMPTY = new ZipFormat() {
        @Override
        public boolean isFormatted(int[] bytes) {
            return true;
        }

        @Override
        protected int getByteSize() {
            return 0;
        }

        @Override
        public @NotNull InputStream newInputStream(@NotNull InputStream input) {
            return input;
        }

        @Override
        public @NotNull OutputStream newOutputStream(@NotNull OutputStream output) {
            return output;
        }
    };

    /**
     * Get empty zip format implementation, that doesn't perform any compression.
     *
     * @return a zip format implementation.
     */
    @NotNull
    public static ZipFormat empty() {
        return EMPTY;
    }

    /**
     * Get gzip compression algorithm implementation.
     *
     * @return a zip format utility implementation.
     */
    @NotNull
    public static Gzip gzip() {
        return Gzip.INSTANCE;
    }

    /**
     * Get zlib compression algorithm implementation.
     *
     * @return a zip format utility implementation.
     */
    @NotNull
    public static Zlib zlib() {
        return Zlib.INSTANCE;
    }

    /**
     * Get lz4 compression algorithm implementation.
     *
     * @return a zip format utility implementation.
     */
    @NotNull
    public static Lz4 lz4() {
        return Lz4.INSTANCE;
    }

    /**
     * Get a zip format implementation, based on provided file.<br>
     * If the file doesn't contain any compression format, an {@link ZipFormat#empty()} instance will be return.
     *
     * @param file the file to check
     * @return     a zip format implementation.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    public static ZipFormat of(@NotNull File file) throws IOException {
        if (gzip().isFormatted(file)) {
            return gzip();
        } else if (zlib().isFormatted(file)) {
            return zlib();
        } else if (lz4().isLoaded() && lz4().isFormatted(file)) {
            return lz4();
        } else {
            return empty();
        }
    }

    /**
     * Get a zip format implementation, based on provided path.<br>
     * If the path doesn't contain any compression format, an {@link ZipFormat#empty()} instance will be return.
     *
     * @param path the path to check
     * @return     a zip format implementation.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    public static ZipFormat of(@NotNull Path path) throws IOException {
        if (gzip().isFormatted(path)) {
            return gzip();
        } else if (zlib().isFormatted(path)) {
            return zlib();
        } else if (lz4().isLoaded() && lz4().isFormatted(path)) {
            return lz4();
        } else {
            return empty();
        }
    }

    /**
     * Get a zip format implementation, based on provided {@link InputStream}.<br>
     * If the {@link InputStream} doesn't contain any compression format, an {@link ZipFormat#empty()} instance will be return.
     *
     * @param input the path to check
     * @return      a zip format implementation.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    public static ZipFormat of(@NotNull InputStream input) throws IOException {
        if (gzip().isFormatted(input)) {
            return gzip();
        } else if (zlib().isFormatted(input)) {
            return zlib();
        } else if (lz4().isLoaded() && lz4().isFormatted(input)) {
            return lz4();
        } else {
            return empty();
        }
    }

    /**
     * Constructs a zip format.
     */
    public ZipFormat() {
    }

    /**
     * Check if the provided file is formatted with the current algorithm implementation.
     *
     * @param file the file to check.
     * @return     true is the file is formatted, false otherwise.
     * @throws IOException if any I/O error occurs.
     */
    public boolean isFormatted(@NotNull File file) throws IOException {
        final Optional<int[]> bytes = getByteHeader(file);
        return bytes.isPresent() && isFormatted(bytes.get());
    }

    /**
     * Check if the provided path is formatted with the current algorithm implementation.
     *
     * @param path the path to check.
     * @return     true is the path is formatted, false otherwise.
     * @throws IOException if any I/O error occurs.
     */
    public boolean isFormatted(@NotNull Path path) throws IOException {
        final Optional<int[]> bytes = getByteHeader(path);
        return bytes.isPresent() && isFormatted(bytes.get());
    }

    /**
     * Check if the provided {@link InputStream} is formatted with the current algorithm implementation.
     *
     * @param input the {@link InputStream} to check.
     * @return      true is the {@link InputStream} is formatted, false otherwise.
     * @throws IOException if any I/O error occurs.
     */
    public boolean isFormatted(@NotNull InputStream input) throws IOException {
        final Optional<int[]> bytes = getByteHeader(input);
        return bytes.isPresent() && isFormatted(bytes.get());
    }

    /**
     * Check if the provided byte array is formatted with the current algorithm implementation.
     *
     * @param bytes the byte array to check.
     * @return      true is the byte array is formatted, false otherwise.
     */
    public abstract boolean isFormatted(int[] bytes);

    /**
     * Get size of header bytes that the current algorithm implementation have.
     *
     * @return a size of bytes.
     */
    protected abstract int getByteSize();

    /**
     * Get the byte array header required by the current algorithm implementation from file.
     *
     * @param file the file to read with random file access.
     * @return     a byte array header.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    protected Optional<int[]> getByteHeader(@NotNull File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            final int[] bytes = new int[getByteSize()];
            for (int i = 0; i < bytes.length; i++) {
                final int b = raf.read();
                if (b <= -1 || b > 255) {
                    return Optional.empty();
                }
                bytes[i] = b;
            }
            return Optional.of(bytes);
        }
    }

    /**
     * Get the byte array header required by the current algorithm implementation from path.
     *
     * @param path the path to read with limited size byte channel.
     * @return     a byte array header.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    protected Optional<int[]> getByteHeader(@NotNull Path path) throws IOException {
        try (SeekableByteChannel byteChannel = Files.newByteChannel(path, StandardOpenOption.READ)) {
            final ByteBuffer buffer = ByteBuffer.allocate(getByteSize());
            byteChannel.read(buffer);
            buffer.flip();

            final int[] bytes = new int[getByteSize()];
            for (int i = 0; i < bytes.length; i++) {
                final int b = buffer.get();
                if (b <= -1) {
                    return Optional.empty();
                }
                bytes[i] = b;
            }
            return Optional.of(bytes);
        }
    }

    /**
     * Get the byte array header required by the current algorithm implementation from {@link InputStream}.
     *
     * @param input the {@link InputStream} to read with position marks.
     * @return      a byte array header.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    protected Optional<int[]> getByteHeader(@NotNull InputStream input) throws IOException {
        final InputStream in;
        if (input.markSupported()) {
            in = input;
        } else {
            in = new BufferedInputStream(input);
        }
        in.mark(getByteSize());

        final int[] bytes = new int[getByteSize()];
        for (int i = 0; i < bytes.length; i++) {
            final int b = in.read();
            if (b == -1) {
                in.reset();
                return Optional.empty();
            }
            bytes[i] = b;
        }
        in.reset();

        return Optional.of(bytes);
    }

    /**
     * Create an {@link InputStream} with the current algorithm implementation.
     *
     * @param file the file to be opened for reading.
     * @return     a newly generated {@link InputStream}.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    public InputStream newInputStream(@NotNull File file) throws IOException {
        return newInputStream(new FileInputStream(file));
    }

    /**
     * Create an {@link InputStream} with the current algorithm implementation.
     *
     * @param path the path to the file to open.
     * @return     a newly generated {@link InputStream}.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    public InputStream newInputStream(@NotNull Path path) throws IOException {
        return newInputStream(Files.newInputStream(path));
    }

    /**
     * Create an {@link InputStream} with the current algorithm implementation.
     *
     * @param input the input stream to encapsulate.
     * @return      a newly generated {@link InputStream}.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    public abstract InputStream newInputStream(@NotNull InputStream input) throws IOException;

    /**
     * Create an {@link OutputStream} with the current algorithm implementation.
     *
     * @param file the file to be opened for writing.
     * @return     a newly generated {@link OutputStream}.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    public OutputStream newOutputStream(@NotNull File file) throws IOException {
        return newOutputStream(new FileOutputStream(file));
    }

    /**
     * Create an {@link OutputStream} with the current algorithm implementation.
     *
     * @param path the path to the file to open or create.
     * @return     a newly generated {@link OutputStream}.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    public OutputStream newOutputStream(@NotNull Path path) throws IOException {
        return newOutputStream(Files.newOutputStream(path, DEFAULT_OPEN_OPTIONS));
    }

    /**
     * Create an {@link OutputStream} with the current algorithm implementation.
     *
     * @param output the output stream to encapsulate.
     * @return       a newly generated {@link OutputStream}.
     * @throws IOException if any I/O error occurs.
     */
    @NotNull
    public abstract OutputStream newOutputStream(@NotNull OutputStream output) throws IOException;

    /**
     * {@link ZipFormat} implementation for gzip algorithm.
     */
    public static class Gzip extends ZipFormat {

        /**
         * {@link Gzip} public instance.
         */
        public static final Gzip INSTANCE = new Gzip();

        /**
         * Constructs a gzip format.
         */
        public Gzip() {
        }

        /**
         * Check if the provided bytes are the same has {@link GZIPInputStream#GZIP_MAGIC}.
         *
         * @param ID1 the first id.
         * @param ID2 the second id.
         * @return    true if the IDs are the same has {@link GZIPInputStream#GZIP_MAGIC}.
         */
        public boolean isGzipHeader(int ID1, int ID2) {
            // For some reason, GZIP_MAGIC is defined as little-endian
            return ((ID2 << 8) | ID1) == GZIPInputStream.GZIP_MAGIC;
        }

        @Override
        public boolean isFormatted(int[] bytes) {
            return isGzipHeader(bytes[0], bytes[1]);
        }

        @Override
        protected int getByteSize() {
            return 2;
        }

        @Override
        public @NotNull InputStream newInputStream(@NotNull InputStream input) throws IOException {
            return new GZIPInputStream(input);
        }

        @Override
        public @NotNull OutputStream newOutputStream(@NotNull OutputStream output) throws IOException {
            return new GZIPOutputStream(output);
        }
    }

    /**
     * {@link ZipFormat} implementation for zlib algorithm.
     */
    public static class Zlib extends ZipFormat {

        /**
         * {@link Zlib} public instance.
         */
        public static final Zlib INSTANCE = new Zlib();

        private static final Map<Integer, Integer> LEVELS = new HashMap<>();

        static {
            LEVELS.put(0x7801, Deflater.NO_COMPRESSION);
            LEVELS.put(0x785E, Deflater.BEST_SPEED);
            LEVELS.put(0x789C, Deflater.DEFAULT_COMPRESSION);
            LEVELS.put(0x78DA, Deflater.BEST_COMPRESSION);
        }

        /**
         * Constructs a zlib format.
         */
        public Zlib() {
        }

        @Override
        public boolean isFormatted(int[] bytes) {
            return getCompressionLevel(bytes) != null;
        }

        /**
         * Get zlib compression level from file.
         *
         * @param file the file to check.
         * @return     a valid compression level if file is zlib formatted, null otherwise.
         * @throws IOException if any I/O occurs.
         */
        @Nullable
        public Integer getCompressionLevel(@NotNull File file) throws IOException {
            return getByteHeader(file).map(this::getCompressionLevel).orElse(null);
        }

        /**
         * Get zlib compression level from path.
         *
         * @param path the path to check.
         * @return     a valid compression level if path is zlib formatted, null otherwise.
         * @throws IOException if any I/O occurs.
         */
        @Nullable
        public Integer getCompressionLevel(@NotNull Path path) throws IOException {
            return getByteHeader(path).map(this::getCompressionLevel).orElse(null);
        }


        /**
         * Get zlib compression level from {@link InputStream}.
         *
         * @param input the {@link InputStream} to check.
         * @return      a valid compression level if {@link InputStream} is zlib formatted, null otherwise.
         * @throws IOException if any I/O occurs.
         */
        @Nullable
        public Integer getCompressionLevel(@NotNull InputStream input) throws IOException {
            return getByteHeader(input).map(this::getCompressionLevel).orElse(null);
        }

        /**
         * Get zlib compression level from byte array header.
         *
         * @param bytes the bytes to check.
         * @return      a valid compression level if found, null otherwise.
         */
        @Nullable
        protected Integer getCompressionLevel(int[] bytes) {
            return LEVELS.get((bytes[0] << 8) | bytes[1]);
        }

        @Override
        protected int getByteSize() {
            return 2;
        }

        @Override
        public @NotNull InputStream newInputStream(@NotNull InputStream input) {
            return new InflaterInputStream(input);
        }

        @Override
        public @NotNull OutputStream newOutputStream(@NotNull OutputStream output) {
            return new DeflaterOutputStream(output);
        }
    }

    /**
     * {@link ZipFormat} implementation for lz4 algorithm.
     */
    public static class Lz4 extends ZipFormat {

        /**
         * {@link Lz4} public instance.
         */
        public static final Lz4 INSTANCE = new Lz4();

        /**
         * Lz4 header magic number.
         */
        public static final int MAGIC = 0x184d2204; // 04 22 4d 18 header as little-endian

        private static final MethodHandle newFrameInputStream;
        private static final MethodHandle newFrameOutputStream;

        static {
            MethodHandle new$FrameInputStream = null;
            MethodHandle new$FrameOutputStream = null;
            try {
                final Class<?> inputClass = Class.forName("net.jpountz.lz4.LZ4FrameInputStream");
                final Class<?> outputClass = Class.forName("net.jpountz.lz4.LZ4FrameOutputStream");

                final MethodHandles.Lookup lookup = MethodHandles.lookup();
                new$FrameInputStream = lookup.findConstructor(inputClass, MethodType.methodType(void.class, InputStream.class));
                new$FrameOutputStream = lookup.findConstructor(outputClass, MethodType.methodType(void.class, OutputStream.class));
            } catch (Throwable ignored) { }
            newFrameInputStream = new$FrameInputStream;
            newFrameOutputStream = new$FrameOutputStream;
        }

        /**
         * Constructs a lz4 format.
         */
        public Lz4() {
        }

        /**
         * Check if lz4 library is loaded on classpath.
         *
         * @return true if lz4 library is loaded, false otherwise.
         */
        public boolean isLoaded() {
            return newFrameInputStream != null && newFrameOutputStream != null;
        }

        @Override
        public boolean isFormatted(int[] bytes) {
            return ((bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3]) == MAGIC;
        }

        @Override
        protected int getByteSize() {
            return 4;
        }

        @Override
        public @NotNull InputStream newInputStream(@NotNull InputStream input) throws IOException {
            try {
                return (InputStream) newFrameInputStream.invoke(input);
            } catch (IOException e) {
                throw e;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        @Override
        public @NotNull OutputStream newOutputStream(@NotNull OutputStream output) throws IOException {
            try {
                return (OutputStream) newFrameOutputStream.invoke(output);
            } catch (IOException e) {
                throw e;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }
}
