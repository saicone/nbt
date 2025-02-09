package com.saicone.nbt.mapper;

import com.saicone.nbt.Tag;
import com.saicone.nbt.TagMapper;
import com.saicone.nbt.TagType;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * TagMapper implementation to handle NBT values as CloudburstMC code abstraction.
 *
 * @author Rubenicos
 */
public class CloudburstTagMapper implements TagMapper<Object> {

    /**
     * {@link CloudburstTagMapper} public instance.
     */
    public static final CloudburstTagMapper INSTANCE = new CloudburstTagMapper();

    @Override
    @SuppressWarnings("unchecked")
    public Object build(@NotNull TagType<?> type, @Nullable Object object) {
        if (type == TagType.LIST) {
            return new NbtList(NbtType.byId(typeId((Iterable<Object>) object)), (Collection<?>) object);
        } else if (type == TagType.COMPOUND) {
            return NbtMap.fromMap((Map<String, Object>) object);
        } else {
            return object;
        }
    }

    @Override
    public Object parse(@Nullable Object object) {
        if (object instanceof NbtList || object instanceof NbtMap) {
            return object;
        }
        return parse(TagType.getType(object), object);
    }

    @Override
    public Object extract(@Nullable Object object) {
        return object;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int size(@Nullable Object object) {
        if (object instanceof NbtList) {
            return size((List<Object>) object);
        } else if (object instanceof NbtMap) {
            return size((Map<String, Object>) object);
        } else {
            return type(object).size(object);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull <A> TagType<A> type(@Nullable Object object) {
        if (object instanceof NbtList) {
            return (TagType<A>) TagType.LIST;
        } else if (object instanceof NbtMap) {
            return (TagType<A>) TagType.COMPOUND;
        } else {
            return TagMapper.super.type(object);
        }
    }

    @Override
    public byte typeId(@Nullable Object object) {
        if (object == null) {
            return Tag.END;
        }
        if (object instanceof NbtList) {
            return Tag.LIST;
        } else if (object instanceof NbtMap) {
            return Tag.COMPOUND;
        } else {
            return TagType.getType(object).id();
        }
    }

    @Override
    public @NotNull <A> TagType<A> listType(@NotNull Object object) {
        return TagType.getType(listTypeId(object));
    }

    @Override
    public byte listTypeId(@NotNull Object object) {
        return (byte) ((NbtList<?>) object).getType().getId();
    }
}
