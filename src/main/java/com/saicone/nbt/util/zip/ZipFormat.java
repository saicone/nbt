package com.saicone.nbt.util.zip;

import org.jetbrains.annotations.NotNull;

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
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public abstract class ZipFormat {

    private static final StandardOpenOption[] DEFAULT_OPEN_OPTIONS = new StandardOpenOption[] {
            StandardOpenOption.SYNC,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
    };

    @NotNull
    public static Gzip gzip() {
        return Gzip.INSTANCE;
    }

    @NotNull
    public static Zlib zlib() {
        return Zlib.INSTANCE;
    }

    @NotNull
    public static Lz4 lz4() {
        return Lz4.INSTANCE;
    }

    public boolean isFormatted(@NotNull File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            final int[] bytes = new int[getByteSize()];
            for (int i = 0; i < bytes.length; i++) {
                final int b = raf.read();
                if (b <= -1 || b > 255) {
                    return false;
                }
                bytes[i] = b;
            }
            return isFormatted(bytes);
        }
    }

    public boolean isFormatted(@NotNull Path path) throws IOException {
        try (SeekableByteChannel byteChannel = Files.newByteChannel(path, StandardOpenOption.READ)) {
            final ByteBuffer buffer = ByteBuffer.allocate(getByteSize());
            byteChannel.read(buffer);
            buffer.flip();

            final int[] bytes = new int[getByteSize()];
            for (int i = 0; i < bytes.length; i++) {
                final int b = buffer.get();
                if (b <= -1) {
                    return false;
                }
                bytes[i] = b;
            }
            return isFormatted(bytes);
        }
    }

    public boolean isFormatted(@NotNull InputStream input) throws IOException {
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
                return false;
            }
            bytes[i] = b;
        }
        in.reset();

        return isFormatted(bytes);
    }

    public abstract boolean isFormatted(int[] bytes);

    protected abstract int getByteSize();

    @NotNull
    public InputStream newInputStream(@NotNull File file) throws IOException {
        return newInputStream(new FileInputStream(file));
    }

    @NotNull
    public InputStream newInputStream(@NotNull Path path) throws IOException {
        return newInputStream(Files.newInputStream(path));
    }

    @NotNull
    public abstract InputStream newInputStream(@NotNull InputStream input) throws IOException;

    @NotNull
    public OutputStream newOutputStream(@NotNull File file) throws IOException {
        return newOutputStream(new FileOutputStream(file));
    }

    @NotNull
    public OutputStream newOutputStream(@NotNull Path path) throws IOException {
        return newOutputStream(Files.newOutputStream(path, DEFAULT_OPEN_OPTIONS));
    }

    @NotNull
    public abstract OutputStream newOutputStream(@NotNull OutputStream output) throws IOException;

    public static class Gzip extends ZipFormat {

        public static final Gzip INSTANCE = new Gzip();

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

    public static class Zlib extends ZipFormat {

        public static final Zlib INSTANCE = new Zlib();

        private static final Map<Integer, Integer> LEVELS = Map.of(
                0x7801, Deflater.NO_COMPRESSION,
                0x785E, Deflater.BEST_SPEED,
                0x789C, Deflater.DEFAULT_COMPRESSION,
                0x78DA, Deflater.BEST_COMPRESSION
        );

        @Override
        public boolean isFormatted(int[] bytes) {
            return getCompressionLevel(bytes[0], bytes[1]) != Integer.MIN_VALUE;
        }

        public int getCompressionLevel(int ID1, int ID2) {
            return LEVELS.getOrDefault((ID1 << 8) | ID2, Integer.MIN_VALUE);
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

    public static class Lz4 extends ZipFormat {

        public static final Lz4 INSTANCE = new Lz4();

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
