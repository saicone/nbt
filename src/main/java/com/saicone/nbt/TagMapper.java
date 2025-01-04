package com.saicone.nbt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>Tag Mapper</b><br>
 * A tag mapper is a customizable tag abstraction, for example, most NBT implementations
 * use classes like "TagString" or "NbtCompound" referring the kind of value that is handled.<br>
 * With a tag mapper this library can build NBT objects as that custom implementation and extract the values from them.
 *
 * @author Rubenicos
 *
 * @param <T> the tag object implementation.
 */
@FunctionalInterface
public interface TagMapper<T> {

    /**
     * A default mapper compatible with any Java object that is represented on {@link TagType}.
     */
    TagMapper<Object> DEFAULT = (type, object) -> object;

    /**
     * Check if the provided object is part of custom tag object implementation used on this instance.
     *
     * @param object the object to check.
     * @return       true if the object is a tag object.
     */
    default boolean isType(@Nullable Object object) {
        return true;
    }

    /**
     * Create a tag object from its value represented as java object.
     *
     * @param type   the type of tag that will be converted.
     * @param object the object value to convert.
     * @return       a tag object containing the object value.
     */
    T build(@NotNull TagType<?> type, @Nullable Object object);

    /**
     * Create an unchecked tag object from its value represented as java object.<br>
     * This method assumes that the required object is the type of that is returned.
     *
     * @param type   the type of tag that will be converted.
     * @param object the object value to convert.
     * @return       an unchecked tag object containing the object value.
     * @param <A>    the implementation of tag object.
     */
    @SuppressWarnings("unchecked")
    default <A extends T> A buildAny(@NotNull TagType<?> type, @Nullable Object object) {
        return (A) build(type, object);
    }

    /**
     * Extracts the inner value that tag object implementation contains.
     *
     * @param t the tag object.
     * @return  a java value that was represented from tag object.
     */
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

    /**
     * Get the size of bytes from tag object implementation.
     *
     * @param t the tag object.
     * @return  a size of bytes.
     */
    default int size(@Nullable T t) {
        return TagType.getType(t).getSize(t);
    }

    /**
     * Get a {@link TagType} that represents the tag object implementation.
     *
     * @param t   the tag object.
     * @return    a {@link TagType} that represents the tag object.
     * @param <A> the type of java object represented by NBT value.
     */
    @NotNull
    default <A> TagType<A> type(@Nullable T t) {
        return TagType.getType(t);
    }

    /**
     * Get the provided object as primitive byte array.<br>
     * This includes compatibility with object array, primitive boolean array and list of bytes.
     *
     * @param object the object to convert or cast as primitive byte array.
     * @return       a primitive byte array.
     */
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

    /**
     * Get the provided object as primitive boolean array.<br>
     * This includes compatibility with object array, primitive byte array and list of booleans.
     *
     * @param object the object to convert or cast as primitive boolean array.
     * @return       a primitive boolean array.
     */
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

    /**
     * Get the provided object as primitive int array.<br>
     * This includes compatibility with object array and list of integers.
     *
     * @param object the object to convert or cast as primitive int array.
     * @return       a primitive int array.
     */
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

    /**
     * Get the provided object as primitive long array.<br>
     * This includes compatibility with object array and list of longs.
     *
     * @param object the object to convert or cast as primitive long array.
     * @return       a primitive long array.
     */
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
