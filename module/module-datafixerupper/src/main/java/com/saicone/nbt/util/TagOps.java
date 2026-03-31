package com.saicone.nbt.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.saicone.nbt.Tag;
import com.saicone.nbt.TagMapper;
import com.saicone.nbt.TagType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * DynamicOps implementation to handle NBT values as DataFixerUpper code abstraction.
 *
 * @author Rubenicos
 */
public class TagOps<T> implements DynamicOps<T> {

    // Inspired by PaperMC (GPLv3) implementation choices:
    // using initial capacity of 8 and load factor of 0.8 to reduce memory footprint
    private static final int DEFAULT_INITIAL_CAPACITY = 8;
    private static final float DEFAULT_LOAD_FACTOR = 0.8f;

    private static final Map<TagMapper<?>, DynamicOps<?>> CACHE = new HashMap<>();

    /**
     * Lookup or create a {@link DynamicOps} instance for the given {@link TagMapper}.<br>
     * This method detects if the mapper has a static {@code DYNAMIC_OPS} field and uses it if present, otherwise creates a new instance.
     *
     * @param mapper the tag mapper.
     * @param <T>    the tag object implementation.
     * @return       a {@link DynamicOps} instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> DynamicOps<T> of(@NotNull TagMapper<T> mapper) {
        DynamicOps<?> instance = CACHE.get(mapper);
        if (instance == null) {
            if (mapper == TagMapper.DEFAULT) {
                instance = JavaOps.INSTANCE;
            } else {
                try {
                    final Field field = mapper.getClass().getDeclaredField("DYNAMIC_OPS");
                    field.setAccessible(true);
                    instance = (DynamicOps<?>) field.get(null);
                } catch (Throwable t) {
                    instance = new TagOps<>(mapper);
                }
            }
            CACHE.put(mapper, instance);
        }
        return (DynamicOps<T>) instance;
    }

    private final TagMapper<T> mapper;
    private final T empty;
    private final T emptyMap;
    private final T emptyList;

    /**
     * Construct a new {@link TagOps} instance for the given {@link TagMapper}.
     *
     * @param mapper the tag mapper.
     */
    public TagOps(@NotNull TagMapper<T> mapper) {
        this.mapper = mapper;
        this.empty = mapper.build(TagType.END, null);
        this.emptyMap = mapper.build(TagType.COMPOUND, Map.of());
        this.emptyList = mapper.build(TagType.LIST, List.of());
    }

    /**
     * Get the {@link TagMapper} associated with this {@link TagOps}.
     *
     * @return the tag mapper.
     */
    @NotNull
    public TagMapper<T> getMapper() {
        return mapper;
    }

    @Override
    public T empty() {
        return empty;
    }

    @Override
    public T emptyMap() {
        return emptyMap;
    }

    @Override
    public T emptyList() {
        return emptyList;
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, T input) {
        final byte id = this.mapper.typeId(input);
        final Object value = this.mapper.extract(input);
        return switch (id) {
            case Tag.END -> outOps.empty();
            case Tag.BYTE -> outOps.createByte((byte) value);
            case Tag.SHORT -> outOps.createShort((short) value);
            case Tag.INT -> outOps.createInt((int) value);
            case Tag.LONG -> outOps.createLong((long) value);
            case Tag.FLOAT -> outOps.createFloat((float) value);
            case Tag.DOUBLE -> outOps.createDouble((double) value);
            case Tag.BYTE_ARRAY -> outOps.createByteList(ByteBuffer.wrap((byte[]) value));
            case Tag.STRING -> outOps.createString((String) value);
            case Tag.LIST -> convertList(outOps, input);
            case Tag.COMPOUND -> convertMap(outOps, input);
            case Tag.INT_ARRAY -> outOps.createIntList(IntStream.of((int[]) value));
            case Tag.LONG_ARRAY -> outOps.createLongList(LongStream.of((long[]) value));
            default -> throw new IllegalArgumentException("Invalid tag type: " + id);
        };
    }

    @Override
    public DataResult<Number> getNumberValue(T input) {
        final Object value = this.mapper.extract(input);
        if (value instanceof Number num) {
            return DataResult.success(num);
        }
        return DataResult.error(() -> "Not a number: " + value);
    }

    @Override
    public T createNumeric(Number i) {
        return this.mapper.build(i);
    }

    @Override
    public T createByte(byte value) {
        return this.mapper.build(TagType.BYTE, value);
    }

    @Override
    public T createShort(short value) {
        return this.mapper.build(TagType.SHORT, value);
    }

    @Override
    public T createInt(int value) {
        return this.mapper.build(TagType.INT, value);
    }

    @Override
    public T createLong(long value) {
        return this.mapper.build(TagType.LONG, value);
    }

    @Override
    public T createFloat(float value) {
        return this.mapper.build(TagType.FLOAT, value);
    }

    @Override
    public T createDouble(double value) {
        return this.mapper.build(TagType.DOUBLE, value);
    }

    @Override
    public T createBoolean(boolean value) {
        return this.mapper.build(TagType.BYTE, value);
    }

    @Override
    public DataResult<String> getStringValue(T input) {
        final Object value = this.mapper.extract(input);
        if (value instanceof String s) {
            return DataResult.success(s);
        }
        return DataResult.error(() -> "Not a string: " + value);
    }

    @Override
    public T createString(String value) {
        return this.mapper.build(TagType.STRING, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataResult<T> mergeToList(T list, T value) {
        final List<T> result;

        final Object listValue = this.mapper.extract(list);
        if (listValue == null) {
            result = new ArrayList<>();
        } else if (listValue instanceof List<?>) {
            result = new ArrayList<>((List<T>) listValue);
        } else {
            return DataResult.error(() -> "Not a list: " + list);
        }

        result.add(value);

        return DataResult.success(this.mapper.build(TagType.LIST, result));
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataResult<T> mergeToList(T list, List<T> values) {
        final List<T> result;

        final Object listValue = this.mapper.extract(list);
        if (listValue == null) {
            return DataResult.success(this.mapper.build(TagType.LIST, values));
        } else if (listValue instanceof List<?>) {
            if (values.isEmpty()) {
                return DataResult.success(list);
            }
            result = new ArrayList<>((List<T>) listValue);
        } else {
            return DataResult.error(() -> "Not a list: " + list);
        }

        result.addAll(values);

        return DataResult.success(this.mapper.build(TagType.LIST, result));
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataResult<T> mergeToMap(T map, T key, T value) {
        final Map<String, T> result;

        final Object mapValue = this.mapper.extract(map);
        if (mapValue == null) {
            result = newMap();
        } else if (mapValue instanceof Map<?,?>) {
            result = newMap((Map<String, T>) mapValue);
        } else {
            return DataResult.error(() -> "Not a compound: " + map);
        }

        if (!(this.mapper.extract(key) instanceof String keyValue)) {
            return DataResult.error(() -> "Not a string: " + key);
        }

        result.put(keyValue, value);

        return DataResult.success(this.mapper.build(TagType.COMPOUND, result));
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataResult<T> mergeToMap(T map, MapLike<T> values) {
        final Map<String, T> result;

        final Object mapValue = this.mapper.extract(map);
        if (mapValue == null) {
            result = newMap();
        } else if (mapValue instanceof Map<?,?>) {
            result = newMap((Map<String, T>) mapValue);
        } else {
            return DataResult.error(() -> "Not a compound: " + map);
        }

        boolean error = false;
        final Iterator<Pair<T, T>> iterator = values.entries().iterator();
        while (iterator.hasNext()) {
            final Pair<T, T> entry = iterator.next();
            if (this.mapper.extract(entry.getFirst()) instanceof String key) {
                result.put(key, entry.getSecond());
            } else {
                error = true;
            }
        }

        final T compound = this.mapper.build(TagType.COMPOUND, result);
        return error ? DataResult.error(() -> "Not full of String key: " + values, compound) : DataResult.success(compound);
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataResult<T> mergeToMap(T map, Map<T, T> values) {
        final Map<String, T> result;

        final Object mapValue = this.mapper.extract(map);
        if (mapValue == null) {
            result = newMap();
        } else if (mapValue instanceof Map<?,?>) {
            result = newMap((Map<String, T>) mapValue);
        } else {
            return DataResult.error(() -> "Not a compound: " + map);
        }

        boolean error = false;
        for (Map.Entry<T, T> entry : values.entrySet()) {
            if (this.mapper.extract(entry.getKey()) instanceof String key) {
                result.put(key, entry.getValue());
            } else {
                error = true;
            }
        }

        final T compound = this.mapper.build(TagType.COMPOUND, result);
        return error ? DataResult.error(() -> "Not full of String key: " + values, compound) : DataResult.success(compound);
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataResult<Stream<Pair<T, T>>> getMapValues(T input) {
        final Object value = this.mapper.extract(input);
        if (value instanceof Map<?,?>) {
            return DataResult.success(getMapValues0((Map<String, T>) value));
        }
        return DataResult.error(() -> "Not a compound: " + input);
    }

    @NotNull
    private Stream<Pair<T, T>> getMapValues0(@NotNull Map<String, T> map) {
        return map.entrySet().stream().map((entry) -> Pair.of(this.mapper.build(TagType.STRING, entry.getKey()), entry.getValue()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataResult<Consumer<BiConsumer<T, T>>> getMapEntries(T input) {
        final Object value = this.mapper.extract(input);
        if (value instanceof Map<?,?>) {
            final Map<String, T> map = (Map<String, T>) value;
            return DataResult.success(consumer -> {
                for (Map.Entry<String, T> entry : map.entrySet()) {
                    final T key = this.mapper.build(TagType.STRING, entry.getKey());
                    consumer.accept(key, entry.getValue());
                }
            });
        }
        return DataResult.error(() -> "Not a compound: " + input);
    }

    @Override
    public T createMap(Stream<Pair<T, T>> map) {
        final Map<String, T> result = newMap();
        map.forEach(pair -> {
            if (this.mapper.extract(pair.getFirst()) instanceof String key) {
                final T value = pair.getSecond();
                result.put(key, value);
            } else {
                throw new IllegalArgumentException("Not a string: " + pair.getFirst());
            }
        });
        return this.mapper.build(TagType.COMPOUND, result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataResult<MapLike<T>> getMap(T input) {
        final Object value = this.mapper.extract(input);
        if (value instanceof Map<?,?>) {
            return DataResult.success(new CompoundMapLike(input, (Map<String, T>) value));
        }
        return DataResult.error(() -> "Not a compound: " + input);
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataResult<Stream<T>> getStream(T input) {
        final Object value = this.mapper.extract(input);
        if (value instanceof List<?>) {
            final List<T> list = (List<T>) value;
            return DataResult.success(list.stream());
        }
        return DataResult.error(() -> "Not a list: " + input);
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataResult<Consumer<Consumer<T>>> getList(T input) {
        final Object value = this.mapper.extract(input);
        if (value instanceof List<?>) {
            final List<T> list = (List<T>) value;
            return DataResult.success(list::forEach);
        }
        return DataResult.error(() -> "Not a list: " + input);
    }

    @Override
    public T createList(Stream<T> input) {
        return this.mapper.build(TagType.LIST, input.collect(Collectors.toCollection(ArrayList::new)));
    }

    @Override
    public DataResult<ByteBuffer> getByteBuffer(T input) {
        final Object value = this.mapper.extract(input);
        if (value instanceof byte[] array) {
            return DataResult.success(ByteBuffer.wrap(array));
        }
        return DynamicOps.super.getByteBuffer(input);
    }

    @Override
    public T createByteList(ByteBuffer input) {
        final ByteBuffer duplicate = input.duplicate().clear();
        final byte[] array = new byte[duplicate.capacity()];
        duplicate.get(array);
        return this.mapper.build(TagType.BYTE_ARRAY, array);
    }

    @Override
    public DataResult<IntStream> getIntStream(T input) {
        final Object value = this.mapper.extract(input);
        if (value instanceof int[] array) {
            return DataResult.success(Arrays.stream(array));
        }
        return DynamicOps.super.getIntStream(input);
    }

    @Override
    public T createIntList(IntStream input) {
        return this.mapper.build(TagType.INT_ARRAY, input.toArray());
    }

    @Override
    public DataResult<LongStream> getLongStream(T input) {
        final Object value = this.mapper.extract(input);
        if (value instanceof long[] array) {
            return DataResult.success(Arrays.stream(array));
        }
        return DynamicOps.super.getLongStream(input);
    }

    @Override
    public T createLongList(LongStream input) {
        return this.mapper.build(TagType.LONG_ARRAY, input.toArray());
    }

    @Override
    @SuppressWarnings("unchecked")
    public T remove(T input, String key) {
        final Object value = this.mapper.extract(input);
        if (value instanceof Map<?, ?>) {
            final Map<String, T> result = newMap((Map<String, T>) value);
            result.remove(key);
            return this.mapper.build(TagType.COMPOUND, result);
        } else {
            return input;
        }
    }

    @Override
    public RecordBuilder<T> mapBuilder() {
        return new CompoundMapBuilder();
    }

    @Override
    public String toString() {
        return "Tag";
    }

    @NotNull
    private Map<String, T> newMap() {
        return newMap(DEFAULT_INITIAL_CAPACITY);
    }

    @NotNull
    private Map<String, T> newMap(int initialCapacity) {
        try {
            return new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>(initialCapacity, DEFAULT_LOAD_FACTOR);
        } catch (Throwable t) {
            return new HashMap<>(initialCapacity);
        }
    }

    @NotNull
    private Map<String, T> newMap(@NotNull Map<String, T> map) {
        try {
            return new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>(map, DEFAULT_LOAD_FACTOR);
        } catch (Throwable t) {
            return new HashMap<>(map);
        }
    }

    class CompoundMapLike implements MapLike<T> {

        private final T compound;
        private final Map<String, T> map;

        protected CompoundMapLike(@NotNull T compound, @NotNull Map<String, T> map) {
            this.compound = compound;
            this.map = map;
        }

        @Override
        public T get(T key) {
            final Object value = TagOps.this.mapper.extract(key);
            if (value instanceof String s) {
                return map.get(s);
            }
            throw new IllegalArgumentException("Not a string: " + key);
        }

        @Override
        public T get(String key) {
            return map.get(key);
        }

        @Override
        public Stream<Pair<T, T>> entries() {
            return TagOps.this.getMapValues0(map);
        }

        @Override
        public String toString() {
            return "MapLike[" + compound + "]";
        }
    }

    class CompoundMapBuilder extends RecordBuilder.AbstractStringBuilder<T, Map<String, T>> {

        protected CompoundMapBuilder() {
            super(TagOps.this);
        }

        @Override
        protected Map<String, T> append(String key, T value, Map<String, T> builder) {
            builder.put(key, value);
            return builder;
        }

        @Override
        protected Map<String, T> initBuilder() {
            return newMap();
        }

        @Override
        @SuppressWarnings("unchecked")
        protected DataResult<T> build(Map<String, T> builder, T prefix) {
            final byte id;
            if (prefix == null || (id = TagOps.this.mapper.typeId(prefix)) == Tag.END) {
                return DataResult.success(TagOps.this.mapper.build(TagType.COMPOUND, builder));
            } else if (id != Tag.COMPOUND) {
                return DataResult.error(() -> "Not a compound: " + prefix, prefix);
            }

            final Map<String, T> value = (Map<String, T>) TagOps.this.mapper.extract(prefix);
            final Map<String, T> result = newMap(value);

            result.putAll(builder);

            return DataResult.success(TagOps.this.mapper.build(TagType.COMPOUND, result));
        }
    }
}
