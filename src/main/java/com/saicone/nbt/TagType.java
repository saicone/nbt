package com.saicone.nbt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Define a tag type with associated object representation.<br>
 * A tag type can be used to get common information about tags like
 * ID, name, pretty name and size in bytes.
 *
 * @author Rubenicos
 *
 * @param <T> the type of object represented by NBT value.
 */
public class TagType<T> {

    /**
     * End tag type, a tag that represent the end of stream or a null value.
     */
    public static final TagType<Object> END = new TagType<>(Tag.END, "END", "TAG_End", 8);
    /**
     * Byte tag type.
     */
    public static final TagType<Byte> BYTE = new TagType<>(Tag.BYTE, "BYTE", "TAG_Byte", 'b', 9);
    /**
     * Byte tag type represented as boolean value, only for compatibility purposes, this is not even a real tag type.
     */
    public static final TagType<Boolean> BOOLEAN = new TagType<>(Tag.BYTE, "BYTE", "TAG_Byte", 9);
    /**
     * Short tag type.
     */
    public static final TagType<Short> SHORT = new TagType<>(Tag.SHORT, "SHORT", "TAG_Short", 's', 10);
    /**
     * Integer tag type.
     */
    public static final TagType<Integer> INT = new TagType<>(Tag.INT, "INT", "TAG_Int", 12);
    /**
     * Long tag type.
     */
    public static final TagType<Long> LONG = new TagType<>(Tag.LONG, "LONG", "TAG_Long", 'l', 16);
    /**
     * Float tag type.
     */
    public static final TagType<Float> FLOAT = new TagType<>(Tag.FLOAT, "FLOAT", "TAG_Float", 'f', 12);
    /**
     * Double tag type.
     */
    public static final TagType<Double> DOUBLE = new TagType<>(Tag.DOUBLE, "DOUBLE", "TAG_Double", 'd', 16);
    /**
     * Byte array tag type.
     */
    public static final TagType<byte[]> BYTE_ARRAY = new TagType<>(Tag.BYTE_ARRAY, "BYTE[]", "TAG_Byte_Array", 'B', 24) {
        @Override
        public int size(byte[] bytes) {
            return size() + Byte.BYTES * bytes.length;
        }
    };
    /**
     * Byte array tag type represented as boolean array value, only for compatibility purposes, this is not even a real tag type.
     */
    public static final TagType<boolean[]> BOOLEAN_ARRAY = new TagType<>(Tag.BYTE_ARRAY, "BYTE[]", "TAG_Byte_Array", 'B', 24) {
        @Override
        public int size(boolean[] booleans) {
            return size() + Byte.BYTES * booleans.length;
        }
    };
    /**
     * String tag type.
     */
    public static final TagType<String> STRING = new TagType<>(Tag.STRING, "STRING", "TAG_String", 36) {
        @Override
        public int size(String s) {
            return size() + Short.BYTES * s.length();
        }
    };
    /**
     * List tag type.
     */
    public static final TagType<List<Object>> LIST = new TagType<>(Tag.LIST, "LIST", "TAG_List", 37) {
        @Override
        public int size(List<Object> list) {
            int size = size();
            size += Integer.BYTES * list.size();

            for (Object object : list) {
                size += TagType.getType(object).size(object);
            }

            return size;
        }
    };
    /**
     * Compound tag type.
     */
    public static final TagType<Map<String, Object>> COMPOUND = new TagType<>(Tag.COMPOUND, "COMPOUND", "TAG_Compound", 48) {
        @Override
        public int size(Map<String, Object> map) {
            int size = size();

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                size += Tag.MAP_KEY_SIZE + Short.BYTES * entry.getKey().length();
                size += Tag.MAP_ENTRY_SIZE + Integer.BYTES;
                size += TagType.getType(entry.getValue()).size(entry.getValue());
            }

            return size;
        }
    };
    /**
     * Integer array tag type.
     */
    public static final TagType<int[]> INT_ARRAY = new TagType<>(Tag.INT_ARRAY, "INT[]", "TAG_Int_Array", 'I', 24) {
        @Override
        public int size(int[] ints) {
            return size() + Integer.BYTES * ints.length;
        }
    };
    /**
     * Long array tag type.
     */
    public static final TagType<long[]> LONG_ARRAY = new TagType<>(Tag.LONG_ARRAY, "LONG[]", "TAG_Long_Array", 'L', 24) {
        @Override
        public int size(long[] longs) {
            return size() + Long.BYTES * longs.length;
        }
    };

    private static final TagType<?>[] TYPES = new TagType<?>[] {
            END,
            BYTE,
            SHORT,
            INT,
            LONG,
            FLOAT,
            DOUBLE,
            BYTE_ARRAY,
            STRING,
            LIST,
            COMPOUND,
            INT_ARRAY,
            LONG_ARRAY
    };
    private static final Map<Class<?>, TagType<?>> CLASS_TYPES = new HashMap<>();

    static {
        CLASS_TYPES.put(Object.class, END);
        CLASS_TYPES.put(byte.class, BYTE);
        CLASS_TYPES.put(Byte.class, BYTE);
        CLASS_TYPES.put(boolean.class, BOOLEAN);
        CLASS_TYPES.put(Boolean.class, BOOLEAN);
        CLASS_TYPES.put(short.class, SHORT);
        CLASS_TYPES.put(Short.class, SHORT);
        CLASS_TYPES.put(int.class, INT);
        CLASS_TYPES.put(Integer.class, INT);
        CLASS_TYPES.put(long.class, LONG);
        CLASS_TYPES.put(Long.class, LONG);
        CLASS_TYPES.put(float.class, FLOAT);
        CLASS_TYPES.put(Float.class, FLOAT);
        CLASS_TYPES.put(double.class, DOUBLE);
        CLASS_TYPES.put(Double.class, DOUBLE);
        CLASS_TYPES.put(byte[].class, BYTE_ARRAY);
        CLASS_TYPES.put(Byte[].class, BYTE_ARRAY);
        CLASS_TYPES.put(boolean[].class, BOOLEAN_ARRAY);
        CLASS_TYPES.put(Boolean[].class, BOOLEAN_ARRAY);
        CLASS_TYPES.put(String.class, STRING);
        CLASS_TYPES.put(List.class, LIST);
        CLASS_TYPES.put(Map.class, COMPOUND);
        CLASS_TYPES.put(int[].class, INT_ARRAY);
        CLASS_TYPES.put(Integer[].class, INT_ARRAY);
        CLASS_TYPES.put(long[].class, LONG_ARRAY);
        CLASS_TYPES.put(Long[].class, LONG_ARRAY);
    }

    private final byte id;
    private final String name;
    private final String prettyName;
    private final char suffix;
    private final int size;

    TagType(int id, @NotNull String name, @NotNull String prettyName, int size) {
        this(id, name, prettyName, '\0', size);
    }

    TagType(int id, @NotNull String name, @NotNull String prettyName, char suffix, int size) {
        this.id = (byte) id;
        this.name = name;
        this.prettyName = prettyName;
        this.suffix = suffix;
        this.size = size;
    }

    /**
     * Check if the current tag type is a valid tag.
     *
     * @return true is this is a valid tag, false otherwise.
     */
    public boolean isValid() {
        return true;
    }

    /**
     * Check if the current tag type represents a primitive object type.
     *
     * @return true if this a primitive object representation.
     */
    public boolean isPrimitive() {
        return id >= Tag.BYTE && id <= Tag.DOUBLE;
    }

    /**
     * Check if the current tag type is a single value object type.
     *
     * @return true if this a single value object representation.
     */
    public boolean isValue() {
        return (id >= Tag.END && id <= Tag.DOUBLE) || id == Tag.STRING;
    }

    /**
     * Check if the current tag type is a number value object type.
     *
     * @return true if this a number value object representation.
     */
    public boolean isInteger() {
        return id == Tag.BYTE || id == Tag.SHORT || id == Tag.INT;
    }

    /**
     * Check if the current tag type is a decimal number type.
     *
     * @return true if this a decimal number representation.
     */
    public boolean isDecimal() {
        return id == Tag.FLOAT || id == Tag.DOUBLE;
    }

    /**
     * Check if the current tag type is an array type.
     *
     * @return true if this an array type representation.
     */
    public boolean isArray() {
        switch (id) {
            case Tag.BYTE_ARRAY:
            case Tag.INT_ARRAY:
            case Tag.LONG_ARRAY:
                return true;
            default:
                return false;
        }
    }

    /**
     * Check if the current tag type has a static byte size.
     *
     * @return true if the object represented by this tag has static size.
     */
    public boolean hasStaticSize() {
        return id >= Tag.END && id <= Tag.DOUBLE;
    }

    /**
     * Get the current tag ID.
     *
     * @return a tag ID.
     */
    public byte id() {
        return id;
    }

    /**
     * Get the current tag name, for some types, the name is different from an allowed enum name.
     *
     * @return a tag name.
     */
    @NotNull
    public String name() {
        return name;
    }

    /**
     * Get the current pretty name of tag.
     *
     * @return a pretty name defined by this tag type.
     */
    @NotNull
    public String prettyName() {
        return prettyName;
    }

    /**
     * Get the current suffix of tag, this may differ on array types or fake representations.
     *
     * @return a suffix defined by this tag type, {@code \0} otherwise.
     */
    public char suffix() {
        return suffix;
    }

    /**
     * Get the current base or static size of tag.
     *
     * @return a size of bytes.
     */
    public int size() {
        return size;
    }

    /**
     * Get the current size if object represented by this tag.
     *
     * @param t an object type.
     * @return  a size of bytes.
     */
    public int size(T t) {
        return size();
    }

    @Override
    public final boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof TagType)) return false;

        final TagType<?> type = (TagType<?>) object;
        return id() == type.id();
    }

    @Override
    public int hashCode() {
        return id();
    }

    /**
     * Get tag type by ID, the provided type must be a valid ID or invalid tag type will be return.
     *
     * @param id  the tag ID.
     * @return    a tag type defined by ID, invalid tag otherwise.
     * @param <T> the type of object represented by NBT value.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> TagType<T> getType(byte id) {
        if (id >= 0 && id < TYPES.length) {
            return (TagType<T>) TYPES[id];
        } else {
            return new TagType<>(Tag.UNKNOWN, "INVALID[" + id + "]", "UNKNOWN_" + id, 0) {
                @Override
                public boolean isValid() {
                    return false;
                }
            };
        }
    }

    /**
     * Get tag type by associated java object type, the provided object must be an object
     * that can be represented with NBT value or invalid tag type will be return.
     *
     * @param object the object type.
     * @return       a tag type associated with java object, invalid tag otherwise.
     * @param <T>    the type of object represented by NBT value.
     */
    @NotNull
    public static <T> TagType<T> getType(@Nullable Object object) {
        if (object instanceof Class) {
            return getType((Class<?>) object);
        } else if (object == null) {
            return getType(Object.class);
        } else {
            return getType(object.getClass());
        }
    }

    /**
     * Get tag type by associated class type, the provided class type must be represented
     * with NBT value or invalid tag type will be return.
     *
     * @param type the class type of object.
     * @return     a tag type associated with class type, invalid tag otherwise.
     * @param <T>  the type of object represented by NBT value.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> TagType<T> getType(@NotNull Class<?> type) {
        TagType<?> result = CLASS_TYPES.get(type);
        if (result == null) {
            if (List.class.isAssignableFrom(type)) {
                result = LIST;
            } else if (Map.class.isAssignableFrom(type)) {
                result = COMPOUND;
            } else {
                result = new TagType<>(Tag.UNKNOWN, "INVALID(" + type.getName() + ")", "UNKNOWN_" + type.getSimpleName(), 0) {
                    @Override
                    public boolean isValid() {
                        return false;
                    }
                };
            }
        }
        return (TagType<T>) result;
    }
}
