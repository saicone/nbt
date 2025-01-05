package com.saicone.nbt.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.saicone.nbt.Tag;
import com.saicone.nbt.TagMapper;
import com.saicone.nbt.TagType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to read/write tags from/as json format.
 *
 * @author Rubenicos
 */
public class TagJson {

    TagJson() {
    }

    /**
     * Convert nbt-represented java object into {@link JsonElement}.
     *
     * @param object the object to convert.
     * @return       a {@link JsonElement} that nearly represent the provided object.
     */
    @NotNull
    public static JsonElement toJson(@Nullable Object object) {
        return toJson(object, TagMapper.DEFAULT);
    }

    /**
     * Convert tag object into {@link JsonElement} with provided {@link TagMapper}.
     *
     * @param object the tag object to convert.
     * @param mapper the mapper to extract value from tag.
     * @return       a {@link JsonElement} that nearly represent the provided tag.
     * @param <T>    the tag object implementation.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> JsonElement toJson(@Nullable T object, @NotNull TagMapper<T> mapper) {
        if (object == null) {
            return JsonNull.INSTANCE;
        }
        final Object value = mapper.extract(object);
        if (value instanceof Boolean) {
            return new JsonPrimitive((Boolean) value);
        }
        final TagType<?> type = mapper.type(object);
        switch (type.id()) {
            case Tag.END:
                return JsonNull.INSTANCE;
            case Tag.BYTE:
            case Tag.SHORT:
            case Tag.INT:
            case Tag.LONG:
            case Tag.FLOAT:
            case Tag.DOUBLE:
                return new JsonPrimitive((Number) value);
            case Tag.STRING:
                return new JsonPrimitive((String) value);
            case Tag.BYTE_ARRAY:
            case Tag.INT_ARRAY:
            case Tag.LONG_ARRAY:
                final int size = Array.getLength(value);
                final JsonArray array = new JsonArray(size);
                for (int i = 0; i < size; i++) {
                    final Object element = Array.get(value, i);
                    array.add(new JsonPrimitive((Number) element));
                }
                return array;
            case Tag.LIST:
                final JsonArray list = new JsonArray(((List<Object>) value).size());
                for (Object o : (List<Object>) value) {
                    final JsonElement element = toJson((T) o, mapper);
                    list.add(element);
                }
                return list;
            case Tag.COMPOUND:
                final JsonObject map = new JsonObject();
                for (var entry : ((Map<Object, Object>) value).entrySet()) {
                    final JsonElement element = toJson((T) entry.getValue(), mapper);
                    map.add(String.valueOf(entry.getKey()), element);
                }
                return map;
            default:
                throw new IllegalArgumentException("Invalid tag type: " + type.name());
        }
    }

    /**
     * Convert {@link JsonElement} into nbt-represented java object.
     *
     * @param element the {@link JsonElement} to convert.
     * @return        a nbt-represented java object converted from json.
     * @param <A>     the implementation of java object.
     */
    @Nullable
    public static <A> A fromJson(@NotNull JsonElement element) {
        return fromJson(element, TagMapper.DEFAULT);
    }

    /**
     * Convert {@link JsonElement} into tag object with provided {@link TagMapper}.
     *
     * @param element the {@link JsonElement} to convert.
     * @param mapper  the mapper to create tag object by providing a value.
     * @return        a tag object nearly converted from json.
     * @param <T>     the tag object implementation.
     * @param <A>     the implementation of tag object.
     */
    @Nullable
    public static <T, A extends T> A fromJson(@NotNull JsonElement element, @NotNull TagMapper<T> mapper) {
        if (element.isJsonNull()) {
            return mapper.buildAny(TagType.END, null);
        } else if (element.isJsonPrimitive()) {
            final JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return mapper.buildAny(TagType.BYTE, primitive.getAsBoolean() ? (byte) 1 : (byte) 0);
            } else if (primitive.isNumber()) {
                final Number number = primitive.getAsNumber();
                return mapper.buildAny(TagType.getType(number), number);
            } else if (primitive.isString()) {
                return mapper.buildAny(TagType.STRING, primitive.getAsString());
            }
        } else if (element.isJsonArray()) {
            final JsonArray array = element.getAsJsonArray();
            if (array.isEmpty()) {
                return mapper.buildAny(TagType.LIST, new ArrayList<T>());
            }
            final JsonElement first = array.get(0);
            if (first.isJsonPrimitive()) {
                final JsonPrimitive primitive = first.getAsJsonPrimitive();
                if (primitive.isNumber()) {
                    final Number number = primitive.getAsNumber();
                    if (number instanceof Byte) {
                        final byte[] bytes = new byte[array.size()];
                        int i = 0;
                        for (JsonElement e : array) {
                            bytes[i] = e.getAsByte();
                            i++;
                        }
                        return mapper.buildAny(TagType.BYTE_ARRAY, bytes);
                    } else if (number instanceof Integer) {
                        final int[] integers = new int[array.size()];
                        int i = 0;
                        for (JsonElement e : array) {
                            integers[i] = e.getAsInt();
                            i++;
                        }
                        return mapper.buildAny(TagType.INT_ARRAY, integers);
                    } else if (number instanceof Long) {
                        final long[] longs = new long[array.size()];
                        int i = 0;
                        for (JsonElement e : array) {
                            longs[i] = e.getAsLong();
                            i++;
                        }
                        return mapper.buildAny(TagType.LONG_ARRAY, longs);
                    }
                }
            }
            final List<T> list = new ArrayList<>();
            for (JsonElement e : array) {
                final T t = fromJson(e, mapper);
                if (t != null) {
                    list.add(t);
                }
            }
            return mapper.buildAny(TagType.LIST, list);
        } else if (element.isJsonObject()) {
            final JsonObject json = element.getAsJsonObject();
            final Map<String, T> map = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                final T e = fromJson(entry.getValue(), mapper);
                if (e != null) {
                    map.put(entry.getKey(), e);
                }
            }
            return mapper.buildAny(TagType.COMPOUND, map);
        }
        throw new IllegalArgumentException("Cannot get value from json: " + element);
    }
}
