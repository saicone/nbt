package com.saicone.nbt.mapper;

import com.saicone.nbt.TagMapper;
import com.saicone.nbt.TagType;
import net.md_5.bungee.nbt.Tag;
import net.md_5.bungee.nbt.TypedTag;
import net.md_5.bungee.nbt.type.ByteArrayTag;
import net.md_5.bungee.nbt.type.ByteTag;
import net.md_5.bungee.nbt.type.CompoundTag;
import net.md_5.bungee.nbt.type.DoubleTag;
import net.md_5.bungee.nbt.type.EndTag;
import net.md_5.bungee.nbt.type.FloatTag;
import net.md_5.bungee.nbt.type.IntArrayTag;
import net.md_5.bungee.nbt.type.IntTag;
import net.md_5.bungee.nbt.type.ListTag;
import net.md_5.bungee.nbt.type.LongArrayTag;
import net.md_5.bungee.nbt.type.LongTag;
import net.md_5.bungee.nbt.type.ShortTag;
import net.md_5.bungee.nbt.type.StringTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TagMapper implementation to handle NBT values as Bungeecord code abstraction.
 *
 * @author Rubenicos
 */
public class BungeecordTagMapper implements TagMapper<TypedTag> {

    /**
     * {@link BungeecordTagMapper} public instance.
     */
    public static final BungeecordTagMapper INSTANCE = new BungeecordTagMapper();

    @Override
    public boolean isType(@Nullable Object object) {
        return object instanceof TypedTag;
    }

    @Override
    @SuppressWarnings("unchecked")
    public TypedTag build(@NotNull TagType<?> type, @Nullable Object object) {
        switch (type.id()) {
            case Tag.END:
                return EndTag.INSTANCE;
            case Tag.BYTE:
                return object instanceof Boolean ? (((Boolean) object) ? new ByteTag((byte) 1) : new ByteTag((byte) 0)) : new ByteTag((byte) object);
            case Tag.SHORT:
                return new ShortTag((short) object);
            case Tag.INT:
                return new IntTag((int) object);
            case Tag.LONG:
                return new LongTag((long) object);
            case Tag.FLOAT:
                return new FloatTag((float) object);
            case Tag.DOUBLE:
                return new DoubleTag((double) object);
            case Tag.BYTE_ARRAY:
                return new ByteArrayTag(byteArray(object));
            case Tag.STRING:
                return new StringTag((String) object);
            case Tag.LIST:
                return new ListTag((List<TypedTag>) object, typeId((Iterable<TypedTag>) object));
            case Tag.COMPOUND:
                return new CompoundTag((Map<String, TypedTag>) object);
            case Tag.INT_ARRAY:
                return new IntArrayTag(intArray(object));
            case Tag.LONG_ARRAY:
                return new LongArrayTag(longArray(object));
            default:
                throw new IllegalArgumentException("Invalid tag type: " + type.name());
        }
    }

    @Override
    public TypedTag parse(@NotNull List<?> list) {
        if (!list.isEmpty() && isType(list.get(0))) {
            // Check if list is mutable
            try {
                list.addAll(List.of());
                return build(TagType.LIST, list);
            } catch (UnsupportedOperationException e) {
                return build(TagType.LIST, new ArrayList<>(list));
            }
        }
        return TagMapper.super.parse(list);
    }

    @Override
    public TypedTag parse(@NotNull Map<String, ?> map) {
        if (!map.isEmpty() && isType(map.values().iterator().next())) {
            // Check if map is mutable
            try {
                map.putAll(Map.of());
                return build(TagType.COMPOUND, map);
            } catch (UnsupportedOperationException e) {
                return build(TagType.COMPOUND, new HashMap<>(map));
            }
        }
        return TagMapper.super.parse(map);
    }

    @Override
    public Object extract(@Nullable TypedTag tag) {
        if (tag == null) {
            return null;
        }
        switch (tag.getId()) {
            case Tag.END:
                return null;
            case Tag.BYTE:
                return ((ByteTag) tag).getValue();
            case Tag.SHORT:
                return ((ShortTag) tag).getValue();
            case Tag.INT:
                return ((IntTag) tag).getValue();
            case Tag.LONG:
                return ((LongTag) tag).getValue();
            case Tag.FLOAT:
                return ((FloatTag) tag).getValue();
            case Tag.DOUBLE:
                return ((DoubleTag) tag).getValue();
            case Tag.BYTE_ARRAY:
                return ((ByteArrayTag) tag).getValue();
            case Tag.STRING:
                return ((StringTag) tag).getValue();
            case Tag.LIST:
                return ((ListTag) tag).getValue();
            case Tag.COMPOUND:
                return ((CompoundTag) tag).getValue();
            case Tag.INT_ARRAY:
                return ((IntArrayTag) tag).getValue();
            case Tag.LONG_ARRAY:
                return ((LongArrayTag) tag).getValue();
            default:
                throw new IllegalArgumentException("Invalid tag type: " + tag);
        }
    }

    @Override
    public @NotNull <A> TagType<A> type(@Nullable TypedTag tag) {
        return tag == null ? TagType.getType(Tag.END) : TagType.getType(tag.getId());
    }

    @Override
    public byte typeId(@Nullable TypedTag tag) {
        if (tag == null) {
            return Tag.END;
        }
        return tag.getId();
    }

    @Override
    public @NotNull <A> TagType<A> listType(@NotNull TypedTag tag) {
        return TagType.getType(((ListTag) tag).getListType());
    }

    @Override
    public byte listTypeId(@NotNull TypedTag tag) {
        return ((ListTag) tag).getListType();
    }
}
