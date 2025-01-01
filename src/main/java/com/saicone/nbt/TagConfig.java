package com.saicone.nbt;

import com.saicone.nbt.io.TagWriter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TagConfig {

    private static final Set<Character> NUMBER_SUFFIX = Set.of('b', 'B', 's', 'S', 'l', 'L', 'f', 'F', 'd', 'D');

    @Nullable
    @Contract("!null -> !null")
    public static Object toConfigValue(@Nullable Object object) {
        return toConfigValue(object, TagMapper.DEFAULT);
    }

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
        final TagType<Object> type = TagType.getType(value);
        switch (type.getId()) {
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
                throw new IllegalArgumentException("Invalid tag type: " + type.getName());
        }
    }

    @Nullable
    public <A> A fromConfigValue(@Nullable Object value) {
        return fromConfigValue(value, TagMapper.DEFAULT);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T, A extends T> A fromConfigValue(@Nullable Object value, @NotNull TagMapper<T> mapper) {
        if (value == null) {
            return null;
        } else if (value instanceof Boolean) {
            return mapper.buildAny(TagType.BYTE, (Boolean) value ? (byte) 1 : (byte) 0);
        }

        final TagType<?> type = TagType.getType(value);
        switch (type.getId()) {
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
                throw new IllegalArgumentException("Invalid tag type: " + type.getName());
        }
    }

    @Nullable
    private <T, A extends T> A fromConfigString(@NotNull String value, @NotNull TagMapper<T> mapper) {
        if (value.length() < 2) {
            return mapper.buildAny(TagType.STRING, value);
        }
        if (value.startsWith("[") && value.endsWith("]")) {
            if (value.startsWith("[B;")) {
                final List<Byte> list = new ArrayList<>();
                for (String s : value.substring(4).split(",")) {
                    if (!s.endsWith("B")) {
                        list.clear();
                        return mapper.buildAny(TagType.STRING, value);
                    }
                    list.add(Byte.parseByte(s.trim().substring(0, s.length() - 1)));
                }
                final byte[] array = new byte[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    array[i] = list.get(i);
                }
                return mapper.buildAny(TagType.BYTE_ARRAY, array);
            } else if (value.startsWith("[L;")) {
                final List<Long> list = new ArrayList<>();
                for (String s : value.substring(4).split(",")) {
                    if (!s.endsWith("L")) {
                        list.clear();
                        return mapper.buildAny(TagType.STRING, value);
                    }
                    list.add(Long.parseLong(s.trim().substring(0, s.length() - 1)));
                }
                final long[] array = new long[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    array[i] = list.get(i);
                }
                return mapper.buildAny(TagType.LONG_ARRAY, array);
            } else if (value.startsWith("[I;")) {
                final List<Integer> list = new ArrayList<>();
                for (String s : value.substring(4).split(",")) {
                    list.add(Integer.parseInt(s.trim()));
                }
                final int[] array = new int[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    array[i] = list.get(i);
                }
                return mapper.buildAny(TagType.INT_ARRAY, array);
            } else {
                return mapper.buildAny(TagType.STRING, value);
            }
        }

        final char suffix = value.charAt(value.length() - 1);
        if (NUMBER_SUFFIX.contains(suffix)) {
            final String s = value.substring(0, value.length() - 1);
            if (isNumber(s)) {
                switch (suffix) {
                    case 'b':
                    case 'B':
                        return mapper.buildAny(TagType.BYTE, Byte.parseByte(s));
                    case 's':
                    case 'S':
                        return mapper.buildAny(TagType.SHORT, Short.parseShort(s));
                    case 'l':
                    case 'L':
                        return mapper.buildAny(TagType.LONG, Long.parseLong(s));
                    case 'f':
                    case 'F':
                        return mapper.buildAny(TagType.FLOAT, Float.parseFloat(s));
                    case 'd':
                    case 'D':
                        return mapper.buildAny(TagType.DOUBLE, Double.parseDouble(s));
                }
            }
        }
        return mapper.buildAny(TagType.STRING, value);
    }

    private static boolean isNumber(@NotNull String s) {
        if (s.isBlank()) {
            return false;
        }
        boolean decimal = false;
        for (char c : (s.charAt(0) == '-' ? s.substring(1) : s).toCharArray()) {
            if (!Character.isDigit(c)) {
                if (!decimal && c == '.') {
                    decimal = true;
                    continue;
                }
                return false;
            }
        }
        return true;
    }
}
