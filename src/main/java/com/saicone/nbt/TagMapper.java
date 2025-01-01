package com.saicone.nbt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@FunctionalInterface
public interface TagMapper<T> {

    TagMapper<Object> DEFAULT = (type, object) -> object;

    default boolean isType(@Nullable Object object) {
        return true;
    }

    T build(@NotNull TagType<?> type, @Nullable Object object);

    @SuppressWarnings("unchecked")
    default <A extends T> A buildAny(@NotNull TagType<?> type, @Nullable Object object) {
        return (A) build(type, object);
    }

    default Object extract(@Nullable T t) {
        if (t instanceof Byte[]) {
            return byteArray(t);
        } else if (t instanceof Boolean[]) {
            return booleanArray(t);
        } else if (t instanceof Integer[]) {
            return intArray(t);
        } else if (t instanceof Long[]) {
            return longArray(t);
        }
        return t;
    }

    default int size(@Nullable T t) {
        return TagType.getType(t).getSize(t);
    }

    default byte[] byteArray(@NotNull Object object) {
        final byte[] array;
        if (object instanceof byte[]) {
            return (byte[]) object;
        } else if (object instanceof Byte[]) {
            final Byte[] bytes = (Byte[]) object;
            array = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                array[i] = bytes[i];
            }
        } else if (object instanceof boolean[]) {
            final boolean[] booleans = (boolean[]) object;
            array = new byte[booleans.length];
            for (int i = 0; i < booleans.length; i++) {
                array[i] = booleans[i] ? (byte) 1 : (byte) 0;
            }
        } else if (object instanceof List) {
            final List<?> list = (List<?>) object;
            array = new byte[list.size()];
            for (int i = 0; i < list.size(); i++) {
                array[i] = (Byte) list.get(i);
            }
        } else {
            throw new IllegalArgumentException("Invalid byte array: " + object);
        }
        return array;
    }

    default boolean[] booleanArray(@NotNull Object object) {
        final boolean[] array;
        if (object instanceof boolean[]) {
            return (boolean[]) object;
        } else if (object instanceof Boolean[]) {
            final Boolean[] booleans = (Boolean[]) object;
            array = new boolean[booleans.length];
            for (int i = 0; i < booleans.length; i++) {
                array[i] = booleans[i];
            }
        } else if (object instanceof byte[]) {
            final byte[] bytes = (byte[]) object;
            array = new boolean[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                array[i] = bytes[i] == (byte) 1;
            }
        } else if (object instanceof List) {
            final List<?> list = (List<?>) object;
            array = new boolean[list.size()];
            for (int i = 0; i < list.size(); i++) {
                array[i] = (Boolean) list.get(i);
            }
        } else {
            throw new IllegalArgumentException("Invalid boolean array: " + object);
        }
        return array;
    }

    default int[] intArray(@NotNull Object object) {
        final int[] array;
        if (object instanceof int[]) {
            return (int[]) object;
        } else if (object instanceof Integer[]) {
            final Integer[] integers = (Integer[]) object;
            array = new int[integers.length];
            for (int i = 0; i < integers.length; i++) {
                array[i] = integers[i];
            }
        } else if (object instanceof List) {
            final List<?> list = (List<?>) object;
            array = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                array[i] = (Integer) list.get(i);
            }
        } else {
            throw new IllegalArgumentException("Invalid int array: " + object);
        }
        return array;
    }

    default long[] longArray(@NotNull Object object) {
        final long[] array;
        if (object instanceof long[]) {
            return (long[]) object;
        } else if (object instanceof Long[]) {
            final Long[] longs = (Long[]) object;
            array = new long[longs.length];
            for (int i = 0; i < longs.length; i++) {
                array[i] = longs[i];
            }
        } else if (object instanceof List) {
            final List<?> list = (List<?>) object;
            array = new long[list.size()];
            for (int i = 0; i < list.size(); i++) {
                array[i] = (Long) list.get(i);
            }
        } else {
            throw new IllegalArgumentException("Invalid long array: " + object);
        }
        return array;
    }
}
