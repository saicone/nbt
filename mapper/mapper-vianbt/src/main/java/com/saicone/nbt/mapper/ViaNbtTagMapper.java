package com.saicone.nbt.mapper;

import com.saicone.nbt.TagMapper;
import com.saicone.nbt.TagType;
import com.viaversion.nbt.io.TagRegistry;
import com.viaversion.nbt.tag.ByteArrayTag;
import com.viaversion.nbt.tag.ByteTag;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.DoubleTag;
import com.viaversion.nbt.tag.FloatTag;
import com.viaversion.nbt.tag.IntArrayTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.LongArrayTag;
import com.viaversion.nbt.tag.LongTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.ShortTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * TagMapper implementation to handle NBT values as ViaNBT code abstraction.
 *
 * @author Rubenicos
 */
public class ViaNbtTagMapper implements TagMapper<Tag> {

    /**
     * {@link ViaNbtTagMapper} public instance.
     */
    public static final ViaNbtTagMapper INSTANCE = new ViaNbtTagMapper();

    @Override
    public boolean isType(@Nullable Object object) {
        return object instanceof Tag;
    }

    @Override
    public Tag build(@NotNull TagType<?> type, @Nullable Object object) {
        switch (type.id()) {
            case TagRegistry.END:
                return null;
            case ByteTag.ID:
                if (object instanceof Boolean) {
                    return new ByteTag((boolean) object ? (byte) 1 : (byte) 0);
                }
                return new ByteTag((byte) object);
            case ShortTag.ID:
                return new ShortTag((short) object);
            case IntTag.ID:
                return new IntTag((int) object);
            case LongTag.ID:
                return new LongTag((long) object);
            case FloatTag.ID:
                return new FloatTag((float) object);
            case DoubleTag.ID:
                return new DoubleTag((double) object);
            case ByteArrayTag.ID:
                return new ByteArrayTag(byteArray(object));
            case StringTag.ID:
                return new StringTag((String) object);
            case ListTag.ID:
                return new ListTag<>((List<Tag>) object);
            case CompoundTag.ID:
                return new CompoundTag((Map<String, Tag>) object);
            case IntArrayTag.ID:
                return new IntArrayTag(intArray(object));
            case LongArrayTag.ID:
                return new LongArrayTag(longArray(object));
            default:
                throw new IllegalArgumentException("Invalid tag type: " + type.name());
        }
    }

    @Override
    public Object extract(@Nullable Tag tag) {
        if (tag == null) {
            return null;
        }
        switch (tag.getTagId()) {
            case TagRegistry.END:
                return null;
            case ByteTag.ID:
                return ((NumberTag) tag).asByte();
            case ShortTag.ID:
                return ((NumberTag) tag).asShort();
            case IntTag.ID:
                return ((NumberTag) tag).asInt();
            case LongTag.ID:
                return ((NumberTag) tag).asLong();
            case FloatTag.ID:
                return ((NumberTag) tag).asFloat();
            case DoubleTag.ID:
                return ((NumberTag) tag).asDouble();
            case ByteArrayTag.ID:
                return ((ByteArrayTag) tag).getValue();
            case StringTag.ID:
                return ((StringTag) tag).getValue();
            case ListTag.ID:
                // Use reflection in case this value become unmodifiable
                return ((ListTag<?>) tag).getValue();
            case CompoundTag.ID:
                return ((CompoundTag) tag).getValue();
            case IntArrayTag.ID:
                return ((IntArrayTag) tag).getValue();
            case LongArrayTag.ID:
                return ((LongArrayTag) tag).getValue();
            default:
                throw new IllegalArgumentException("Invalid tag type: " + tag);
        }
    }

    @Override
    public @NotNull Tag copy(@NotNull Tag tag) {
        return tag.copy();
    }

    @Override
    public @NotNull <A> TagType<A> type(@Nullable Tag tag) {
        return tag == null ? TagType.getType(TagRegistry.END) : TagType.getType(tag.getTagId());
    }

    @Override
    public byte typeId(@Nullable Tag tag) {
        if (tag == null) {
            return TagRegistry.END;
        }
        return (byte) tag.getTagId();
    }

    @Override
    public @NotNull <A> TagType<A> listType(@NotNull Tag tag) {
        return TagType.getType(listTypeId(tag));
    }

    @Override
    public byte listTypeId(@NotNull Tag tag) {
        return (byte) TagRegistry.getIdFor(((ListTag<?>) tag).getElementType());
    }
}
