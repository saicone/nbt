package com.saicone.nbt.util;

import com.saicone.nbt.TagMapper;
import com.saicone.types.TypeWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * {@link TypeWrapper} implementation to convert NBT values between two different {@link TagMapper} implementations.
 *
 * @author Rubenicos
 */
public class TagWrapper<A, B> extends TypeWrapper<A, B> {

    private static final Map<Integer, TagWrapper<?, ?>> CACHE = new HashMap<>();

    /**
     * Get a {@link TagWrapper} instance for the given {@link TagMapper} implementations.
     *
     * @param mapperA the first {@link TagMapper} implementation
     * @param mapperB the second {@link TagMapper} implementation
     * @return        a {@link TagWrapper} instance for the given {@link TagMapper} implementations
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <A, B> TagWrapper<A, B> of(@NotNull TagMapper<A> mapperA, @NotNull TagMapper<B> mapperB) {
        final Integer key = Objects.hash(mapperA, mapperB);
        TagWrapper<?, ?> instance = CACHE.get(key);
        if (instance == null) {
            instance = new TagWrapper<>(mapperA, mapperB);
            CACHE.put(key, instance);
        }
        return (TagWrapper<A, B>) instance;
    }

    private final TagMapper<A> mapperA;
    private final TagMapper<B> mapperB;

    /**
     * Create a new {@link TagWrapper} instance for the given {@link TagMapper} implementations.
     *
     * @param mapperA the first {@link TagMapper} implementation.
     * @param mapperB the second {@link TagMapper} implementation.
     */
    public TagWrapper(@NotNull TagMapper<A> mapperA, @NotNull TagMapper<B> mapperB) {
        this.mapperA = mapperA;
        this.mapperB = mapperB;
    }

    /**
     * Get the first {@link TagMapper} implementation.
     *
     * @return a {@link TagMapper} implementation.
     */
    @NotNull
    public TagMapper<A> getMapperA() {
        return mapperA;
    }

    /**
     * Get the second {@link TagMapper} implementation.
     *
     * @return a {@link TagMapper} implementation.
     */
    @NotNull
    public TagMapper<B> getMapperB() {
        return mapperB;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B wrap(Object object) {
        return mapperB.parse(mapperA.deepExtract((A) object));
    }

    @Override
    @SuppressWarnings("unchecked")
    public A unwrap(Object object) {
        return mapperA.parse(mapperB.deepExtract((B) object));
    }
}
