package com.saicone.nbt.util;

import com.saicone.nbt.TagMapper;
import com.saicone.types.TypeParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * TypeParser implementation to handle NBT values as {@link TagMapper} abstraction.
 *
 * @author Rubenicos
 */
public class TagParser<T> implements TypeParser<T> {

    private static final Map<TagMapper<?>, TagParser<?>> CACHE = new HashMap<>();

    /**
     * Get a {@link TagParser} instance for the given {@link TagMapper}.
     *
     * @param mapper the tag mapper.
     * @param <T>    the tag object implementation.
     * @return       a {@link TagParser} instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> TagParser<T> of(@NotNull TagMapper<T> mapper) {
        TagParser<?> instance = CACHE.get(mapper);
        if (instance == null) {
            instance = new TagParser<>(mapper);
            CACHE.put(mapper, instance);
        }
        return (TagParser<T>) instance;
    }

    private final TagMapper<T> mapper;

    /**
     * Construct a new {@link TagParser} with the given {@link TagMapper}.
     *
     * @param mapper the tag mapper.
     */
    public TagParser(@NotNull TagMapper<T> mapper) {
        this.mapper = mapper;
    }

    /**
     * Get the {@link TagMapper} instance associated with this {@link TagParser}.
     *
     * @return the tag mapper.
     */
    @NotNull
    public TagMapper<T> getMapper() {
        return mapper;
    }

    @Override
    public boolean isInstance(@Nullable Object object) {
        return this.mapper.isType(object);
    }

    @Override
    public @Nullable T parse(@NotNull Object object) {
        return this.mapper.parse(object);
    }
}
