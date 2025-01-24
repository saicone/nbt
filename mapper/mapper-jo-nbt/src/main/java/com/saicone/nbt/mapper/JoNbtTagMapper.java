package com.saicone.nbt.mapper;

import com.saicone.nbt.TagMapper;
import com.saicone.nbt.TagType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import se.llbit.nbt.ByteArrayTag;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.DoubleTag;
import se.llbit.nbt.FloatTag;
import se.llbit.nbt.IntArrayTag;
import se.llbit.nbt.IntTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.LongArrayTag;
import se.llbit.nbt.LongTag;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.ShortTag;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.StringTag;
import se.llbit.nbt.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TagMapper implementation to handle NBT values as jo-nbt code abstraction.
 *
 * @author Rubenicos
 */
public class JoNbtTagMapper implements TagMapper<Tag> {

    /**
     * {@link JoNbtTagMapper} public instance.
     */
    public static final JoNbtTagMapper INSTANCE = new JoNbtTagMapper();

    @Override
    public boolean isType(@Nullable Object object) {
        return object instanceof Tag;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Tag build(@NotNull TagType<?> type, @Nullable Object object) {
        switch (type.id()) {
            case Tag.TAG_END:
                return Tag.END;
            case Tag.TAG_BYTE:
                return new ByteTag(object instanceof Boolean ? (((Boolean) object) ? 1 : 0) : (int) object);
            case Tag.TAG_SHORT:
                return new ShortTag((short) object);
            case Tag.TAG_INT:
                return new IntTag((int) object);
            case Tag.TAG_LONG:
                return new LongTag((long) object);
            case Tag.TAG_FLOAT:
                return new FloatTag((float) object);
            case Tag.TAG_DOUBLE:
                return new DoubleTag((double) object);
            case Tag.TAG_BYTE_ARRAY:
                return new ByteArrayTag(byteArray(object));
            case Tag.TAG_STRING:
                return new StringTag((String) object);
            case Tag.TAG_LIST:
                return new ListTag(type((Iterable<Tag>) object).id(), (List<? extends SpecificTag>) object);
            case Tag.TAG_COMPOUND:
                final Map<String, SpecificTag> map = (Map<String, SpecificTag>) object;
                final CompoundTag compound = new CompoundTag();
                for (Map.Entry<String, SpecificTag> entry : map.entrySet()) {
                    compound.add(entry.getKey(), entry.getValue());
                }
                return compound;
            case Tag.TAG_INT_ARRAY:
                return new IntArrayTag(intArray(object));
            case Tag.TAG_LONG_ARRAY:
                return new LongArrayTag(longArray(object));
            default:
                throw new IllegalArgumentException("Invalid tag type: " + type.name());
        }
    }

    @Override
    public Object extract(@Nullable Tag tag) {
        final SpecificTag sTag;
        if (tag instanceof SpecificTag) {
            sTag = (SpecificTag) tag;
        } else if (tag instanceof NamedTag) {
            final NamedTag named = (NamedTag) tag;
            sTag = named.getTag();
        } else if (tag == null) {
            return null;
        } else {
            throw new IllegalArgumentException("Invalid tag type: " + tag);
        }
        switch (sTag.tagType()) {
            case Tag.TAG_END:
                return null;
            case Tag.TAG_BYTE:
                return sTag.byteValue();
            case Tag.TAG_SHORT:
                return sTag.shortValue();
            case Tag.TAG_INT:
                return sTag.intValue();
            case Tag.TAG_LONG:
                return sTag.longValue();
            case Tag.TAG_FLOAT:
                return sTag.floatValue();
            case Tag.TAG_DOUBLE:
                return sTag.doubleValue();
            case Tag.TAG_BYTE_ARRAY:
                return sTag.byteArray();
            case Tag.TAG_STRING:
                return sTag.stringValue();
            case Tag.TAG_LIST:
                return sTag.asList().items;
            case Tag.TAG_COMPOUND:
                final Map<String, SpecificTag> map = new HashMap<>();
                for (NamedTag namedTag : sTag.asCompound()) {
                    map.put(namedTag.name(), namedTag.getTag());
                }
                return map;
            case Tag.TAG_INT_ARRAY:
                return sTag.intArray();
            case Tag.TAG_LONG_ARRAY:
                return sTag.longArray();
            default:
                throw new IllegalArgumentException("Invalid tag type: " + tag);
        }
    }

    @Override
    public int size(@Nullable Tag tag) {
        if (tag instanceof ListTag) {
            final ListTag list = (ListTag) tag;
            int size = TagType.LIST.size();
            size += Integer.BYTES * list.size();

            for (SpecificTag element : list) {
                size += size(element);
            }

            return size;
        } else if (tag instanceof CompoundTag) {
            final CompoundTag compound = (CompoundTag) tag;
            int size = TagType.COMPOUND.size();

            for (NamedTag entry : compound) {
                size += com.saicone.nbt.Tag.MAP_KEY_SIZE + Short.BYTES * entry.name().length();
                size += com.saicone.nbt.Tag.MAP_ENTRY_SIZE + Integer.BYTES;
                size += size(entry.getTag());
            }

            return size;
        } else {
            return type(tag).size(extract(tag));
        }
    }

    @Override
    public @NotNull <A> TagType<A> type(@Nullable Tag tag) {
        if (tag instanceof SpecificTag) {
            final SpecificTag specific = (SpecificTag) tag;
            return TagType.getType(specific.tagType());
        } else if (tag instanceof NamedTag) {
            final NamedTag named = (NamedTag) tag;
            return TagType.getType(named.getTag().tagType());
        } else {
            throw new IllegalArgumentException("Invalid tag type: " + tag);
        }
    }

    @Override
    public @NotNull <A> TagType<A> listType(@NotNull Tag tag) {
        return TagType.getType(((ListTag) tag).getType());
    }
}
