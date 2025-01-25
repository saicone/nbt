package com.saicone.nbt.mapper;

import cn.nukkit.nbt.tag.ByteArrayTag;
import cn.nukkit.nbt.tag.ByteTag;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.EndTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.IntArrayTag;
import cn.nukkit.nbt.tag.IntTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.nbt.tag.LongTag;
import cn.nukkit.nbt.tag.ShortTag;
import cn.nukkit.nbt.tag.StringTag;
import cn.nukkit.nbt.tag.Tag;
import com.saicone.nbt.TagMapper;
import com.saicone.nbt.TagType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * TagMapper implementation to handle NBT values as Nukkit code abstraction.
 *
 * @author Rubenicos
 */
public class NukkitTagMapper implements TagMapper<Tag> {

    /**
     * {@link NukkitTagMapper} public instance.
     */
    public static final NukkitTagMapper INSTANCE = new NukkitTagMapper();

    private static final MethodHandle LIST_VALUE;
    private static final MethodHandle COMPOUND_VALUE;

    static {
        MethodHandle listValue = null;
        MethodHandle compoundTags = null;
        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();

            final Field listField = ListTag.class.getDeclaredField("list");
            listField.setAccessible(true);
            listValue = lookup.unreflectGetter(listField);

            final Field compoundField = CompoundTag.class.getDeclaredField("tags");
            compoundField.setAccessible(true);
            compoundTags = lookup.unreflectGetter(compoundField);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        LIST_VALUE = listValue;
        COMPOUND_VALUE = compoundTags;
    }

    @Override
    public boolean isType(@Nullable Object object) {
        return object instanceof Tag;
    }

    @Override
    public Tag build(@NotNull TagType<?> type, @Nullable Object object) {
        switch (type.id()) {
            case Tag.TAG_End:
                return new EndTag();
            case Tag.TAG_Byte:
                if (object instanceof Boolean) {
                    return new ByteTag(null, (Boolean) object ? (byte) 1 : (byte) 0);
                }
                return new ByteTag(null, (byte) object);
            case Tag.TAG_Short:
                return new ShortTag(null, (short) object);
            case Tag.TAG_Int:
                return new IntTag(null, (int) object);
            case Tag.TAG_Long:
                return new LongTag(null, (long) object);
            case Tag.TAG_Float:
                return new FloatTag(null, (float) object);
            case Tag.TAG_Double:
                return new DoubleTag(null, (double) object);
            case Tag.TAG_Byte_Array:
                return new ByteArrayTag(null, byteArray(object));
            case Tag.TAG_String:
                return new StringTag(null, (String) object);
            case Tag.TAG_List:
                final ListTag<Tag> list = new ListTag<>();
                list.setAll((List<Tag>) object);
                list.type = typeId((Iterable<Tag>) object);
                return list;
            case Tag.TAG_Compound:
                final CompoundTag tag = new CompoundTag();
                for (Map.Entry<String, Tag> entry : ((Map<String, Tag>) object).entrySet()) {
                    tag.put(entry.getKey(), entry.getValue());
                }
                return tag;
            case Tag.TAG_Int_Array:
                return new IntArrayTag(null, intArray(object));
            default:
                throw new IllegalArgumentException("Invalid tag type: " + type.name());
        }
    }

    @Override
    public Object extract(@Nullable Tag tag) {
        if (tag == null) {
            return null;
        }
        switch (tag.getId()) {
            case Tag.TAG_End:
                return null;
            case Tag.TAG_Byte:
                return ((ByteTag) tag).getData().byteValue();
            case Tag.TAG_Short:
                return ((ShortTag) tag).getData().shortValue();
            case Tag.TAG_Int:
                return ((IntTag) tag).data;
            case Tag.TAG_Long:
                return ((LongTag) tag).data;
            case Tag.TAG_Float:
                return ((FloatTag) tag).data;
            case Tag.TAG_Double:
                return ((DoubleTag) tag).data;
            case Tag.TAG_Byte_Array:
                return ((ByteArrayTag) tag).data;
            case Tag.TAG_String:
                return ((StringTag) tag).data;
            case Tag.TAG_List:
                try {
                    return LIST_VALUE.invoke(tag);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            case Tag.TAG_Compound:
                try {
                    return COMPOUND_VALUE.invoke(tag);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            case Tag.TAG_Int_Array:
                return ((IntArrayTag) tag).data;
            default:
                throw new IllegalArgumentException("Invalid tag type: " + tag);
        }
    }

    @Override
    public @NotNull <A> TagType<A> type(@Nullable Tag tag) {
        return TagType.getType(typeId(tag));
    }

    @Override
    public byte typeId(@Nullable Tag tag) {
        if (tag == null) {
            return com.saicone.nbt.Tag.END;
        }
        return tag.getId();
    }

    @Override
    public @NotNull <A> TagType<A> listType(@NotNull Tag tag) {
        return TagType.getType(listTypeId(tag));
    }

    @Override
    public byte listTypeId(@NotNull Tag tag) {
        return ((ListTag<?>) tag).type;
    }
}
