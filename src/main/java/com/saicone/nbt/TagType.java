package com.saicone.nbt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TagType<T> {

    public static final TagType<Object> END = new TagType<>(Tag.END, "END", "TAG_End", 8);
    public static final TagType<Byte> BYTE = new TagType<>(Tag.BYTE, "BYTE", "TAG_Byte", 'b', 9);
    public static final TagType<Boolean> BOOLEAN = new TagType<>(Tag.BYTE, "BYTE", "TAG_Byte", 9);
    public static final TagType<Short> SHORT = new TagType<>(Tag.SHORT, "SHORT", "TAG_Short", 's', 10);
    public static final TagType<Integer> INT = new TagType<>(Tag.INT, "INT", "TAG_Int", 12);
    public static final TagType<Long> LONG = new TagType<>(Tag.LONG, "LONG", "TAG_Long", 'l', 16);
    public static final TagType<Float> FLOAT = new TagType<>(Tag.FLOAT, "FLOAT", "TAG_Float", 'f', 12);
    public static final TagType<Double> DOUBLE = new TagType<>(Tag.DOUBLE, "DOUBLE", "TAG_Double", 'd', 16);
    public static final TagType<byte[]> BYTE_ARRAY = new TagType<>(Tag.BYTE_ARRAY, "BYTE[]", "TAG_Byte_Array", 'B', 24) {
        @Override
        public int getSize(byte[] bytes) {
            return getSize() + Byte.BYTES * bytes.length;
        }
    };
    public static final TagType<boolean[]> BOOLEAN_ARRAY = new TagType<>(Tag.BYTE_ARRAY, "BYTE[]", "TAG_Byte_Array", 'B', 24) {
        @Override
        public int getSize(boolean[] booleans) {
            return getSize() + Byte.BYTES * booleans.length;
        }
    };
    public static final TagType<String> STRING = new TagType<>(Tag.STRING, "STRING", "TAG_String", 36) {
        @Override
        public int getSize(String s) {
            return getSize() + Short.BYTES * s.length();
        }
    };
    public static final TagType<List<Object>> LIST = new TagType<>(Tag.LIST, "LIST", "TAG_List", 37) {
        @Override
        public int getSize(List<Object> list) {
            int size = getSize();
            size += Integer.BYTES * list.size();

            for (Object object : list) {
                size += TagType.getType(object).getSize(object);
            }

            return size;
        }
    };
    public static final TagType<Map<String, Object>> COMPOUND = new TagType<>(Tag.COMPOUND, "COMPOUND", "TAG_Compound", 48) {
        @Override
        public int getSize(Map<String, Object> map) {
            int size = getSize();

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                size += Tag.MAP_KEY_SIZE + Short.BYTES * entry.getKey().length();
                size += Tag.MAP_ENTRY_SIZE + Integer.BYTES;
                size += TagType.getType(entry.getValue()).getSize(entry.getValue());
            }

            return size;
        }
    };
    public static final TagType<int[]> INT_ARRAY = new TagType<>(Tag.INT_ARRAY, "INT[]", "TAG_Int_Array", 'I', 24) {
        @Override
        public int getSize(int[] ints) {
            return getSize() + Integer.BYTES * ints.length;
        }
    };
    public static final TagType<long[]> LONG_ARRAY = new TagType<>(Tag.LONG_ARRAY, "LONG[]", "TAG_Long_Array", 'L', 24) {
        @Override
        public int getSize(long[] longs) {
            return getSize() + Long.BYTES * longs.length;
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

    public boolean isValid() {
        return true;
    }

    public boolean isPrimitive() {
        return id >= Tag.BYTE && id <= Tag.DOUBLE;
    }

    public boolean isValue() {
        return isPrimitive() || id == Tag.STRING;
    }

    public boolean isDecimal() {
        return id == Tag.FLOAT || id == Tag.DOUBLE;
    }

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

    public byte getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getPrettyName() {
        return prettyName;
    }

    public char getSuffix() {
        return suffix;
    }

    public int getSize() {
        return size;
    }

    public int getSize(T t) {
        return getSize();
    }

    @Override
    public final boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof TagType)) return false;

        final TagType<?> type = (TagType<?>) object;
        return isValid() ? getId() == type.getId() : getName().equals(type.getName());
    }

    @Override
    public int hashCode() {
        return isValid() ? getId() : getName().hashCode();
    }

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
