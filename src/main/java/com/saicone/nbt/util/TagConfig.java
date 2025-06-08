package com.saicone.nbt.util;

import com.saicone.nbt.Tag;
import com.saicone.nbt.TagMapper;
import com.saicone.nbt.TagType;
import com.saicone.nbt.io.TagReader;
import com.saicone.nbt.io.TagWriter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to read/write tags from/as simplified configuration format.
 *
 * @author Rubenicos
 */
public class TagConfig {

    TagConfig() {
    }

    /**
     * Convert nbt-represented java object into config value.
     *
     * @param object the object to convert.
     * @return       a simplified configuration value that represent the object.
     */
    @Nullable
    @Contract("!null -> !null")
    public static Object toConfigValue(@Nullable Object object) {
        return toConfigValue(object, TagMapper.DEFAULT);
    }

    /**
     * Convert tag object into config value with provided {@link TagMapper}.
     *
     * @param object the tag object to convert.
     * @param mapper the mapper to extract value from tag.
     * @return       a simplified configuration value that represent the tag object.
     * @param <T>    the tag object implementation.
     */
    @Nullable
    @Contract("!null, _ -> !null")
    @SuppressWarnings("unchecked")
    public static <T> Object toConfigValue(@Nullable T object, @NotNull TagMapper<T> mapper) {
        if (object == null) {
            return null;
        }
        final Object value = mapper.extract(object);
        if (value == null) {
            return null;
        } else if (value instanceof Boolean) {
            return value;
        }
        final TagType<Object> type = mapper.type(object);
        switch (type.id()) {
            case Tag.END:
                return null;
            case Tag.INT:
            case Tag.DOUBLE:
            case Tag.STRING:
                return value;
            case Tag.BYTE:
            case Tag.SHORT:
            case Tag.LONG:
            case Tag.FLOAT:
            case Tag.BYTE_ARRAY:
            case Tag.INT_ARRAY:
            case Tag.LONG_ARRAY:
                return TagWriter.toString(object, mapper);
            case Tag.LIST:
                final List<Object> list = new ArrayList<>();
                for (Object o : (List<Object>) value) {
                    final Object element = toConfigValue((T) o, mapper);
                    if (element != null) {
                        list.add(element);
                    }
                }
                return list;
            case Tag.COMPOUND:
                final Map<String, Object> map = new HashMap<>();
                for (var entry : ((Map<Object, Object>) value).entrySet()) {
                    final Object element = toConfigValue((T) entry.getValue(), mapper);
                    if (element != null) {
                        map.put(String.valueOf(entry.getKey()), element);
                    }
                }
                return map;
            default:
                throw new IllegalArgumentException("Invalid tag type: " + type.name());
        }
    }

    /**
     * Convert simplified configuration value into nbt-represented java object.
     *
     * @param value  the config value to convert.
     * @return       a nbt-represented java object converted from config value.
     * @param <A>    the implementation of java object.
     */
    @Nullable
    public static <A> A fromConfigValue(@Nullable Object value) {
        return fromConfigValue(value, TagMapper.DEFAULT);
    }

    /**
     * Convert simplified configuration value into tag object with provided {@link TagMapper}.
     *
     * @param value  the config value to convert.
     * @param mapper the mapper to create tag object by providing a value.
     * @return       a tag object converted from config value.
     * @param <T>    the tag object implementation.
     * @param <A>    the implementation of tag object.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T, A extends T> A fromConfigValue(@Nullable Object value, @NotNull TagMapper<T> mapper) {
        if (value == null) {
            return null;
        } else if (value instanceof Boolean) {
            return mapper.buildAny(TagType.BYTE, (Boolean) value ? (byte) 1 : (byte) 0);
        }

        final TagType<?> type = TagType.getType(value);
        switch (type.id()) {
            case Tag.END:
                return null;
            case Tag.BYTE:
            case Tag.SHORT:
            case Tag.INT:
            case Tag.LONG:
            case Tag.FLOAT:
            case Tag.DOUBLE:
            case Tag.BYTE_ARRAY:
            case Tag.INT_ARRAY:
            case Tag.LONG_ARRAY:
                return mapper.buildAny(type, value);
            case Tag.STRING:
                // Do NOT convert to:
                // - Boolean
                // - Integer
                // - List
                // - Compound
                return fromConfigString((String) value, mapper);
            case Tag.LIST:
                final List<T> list = new ArrayList<>();
                for (Object o : (List<Object>) value) {
                    final T v = fromConfigValue(o, mapper);
                    if (v != null) {
                        list.add(v);
                    }
                }
                return mapper.buildAny(type, list);
            case Tag.COMPOUND:
                final Map<String, T> map = new HashMap<>();
                for (var entry : ((Map<Object, Object>) value).entrySet()) {
                    final T v = fromConfigValue(entry.getValue(), mapper);
                    if (v != null) {
                        map.put(String.valueOf(entry.getKey()), v);
                    }
                }
                return mapper.buildAny(type, map);
            default:
                throw new IllegalArgumentException("Invalid tag type: " + type.name());
        }
    }

    @Nullable
    private static <T, A extends T> A fromConfigString(@NotNull String value, @NotNull TagMapper<T> mapper) {
        if (value.length() < 2 || value.charAt(0) == '{' || value.equals("true") || value.equals("false") || isInteger(value)) {
            return mapper.buildAny(TagType.STRING, value);
        }

        if (value.length() > 2 && value.charAt(0) == '[' && value.charAt(value.length() - 1) == ']') {
            if (value.charAt(2) == ';') {
                switch (value.charAt(1)) {
                    case 'b':
                    case 'B':
                    case 'i':
                    case 'I':
                    case 'l':
                    case 'L':
                        break;
                    default:
                        return mapper.buildAny(TagType.STRING, value);
                }
            }
        } else if (!isUnquoted(value)) {
            return mapper.buildAny(TagType.STRING, value);
        }

        return TagReader.fromString(value, mapper);
    }

    private static boolean isInteger(@NotNull String s) {
        int i;
        if (s.charAt(0) == '-') {
            if (s.length() > 11) {
                return false;
            }
            i = 1;
        } else if (s.length() > 10) {
            return false;
        } else {
            i = 0;
        }
        for (; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isUnquoted(@NotNull String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!isUnquoted(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isUnquoted(int c) {
        return c >= '0' && c <= '9'
                || c >= 'A' && c <= 'Z'
                || c >= 'a' && c <= 'z'
                || c == '_' || c == '-'
                || c == '.' || c == '+'
                || c == '(' || c == ')'
                || c == ' ';
    }
}
