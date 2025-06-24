package com.saicone.nbt.mapper;

import com.saicone.nbt.Tag;
import com.saicone.nbt.TagMapper;
import com.saicone.nbt.TagType;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.ByteBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.EndBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.LongArrayBinaryTag;
import net.kyori.adventure.nbt.LongBinaryTag;
import net.kyori.adventure.nbt.ShortBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TagMapper implementation to handle NBT values as Adventure code abstraction.
 *
 * @author Rubenicos
 */
public class AdventureTagMapper implements TagMapper<BinaryTag> {

    /**
     * {@link AdventureTagMapper} public instance.
     */
    public static final AdventureTagMapper INSTANCE = new AdventureTagMapper();

    private static final MethodHandle LIST_TAGS;
    private static final MethodHandle COMPOUND_TAGS;

    static {
        MethodHandle listTags = null;
        MethodHandle compoundTags = null;
        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();

            final Class<? extends ListBinaryTag> listClass = Class.forName("net.kyori.adventure.nbt.ListBinaryTagImpl").asSubclass(ListBinaryTag.class);
            final Field listField = listClass.getDeclaredField("tags");
            listField.setAccessible(true);
            listTags = lookup.unreflectGetter(listField);

            final Class<? extends CompoundBinaryTag> compoundClass = Class.forName("net.kyori.adventure.nbt.CompoundBinaryTagImpl").asSubclass(CompoundBinaryTag.class);
            final Field compoundField = compoundClass.getDeclaredField("tags");
            compoundField.setAccessible(true);
            compoundTags = lookup.unreflectGetter(compoundField);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        LIST_TAGS = listTags;
        COMPOUND_TAGS = compoundTags;
    }

    @Override
    public boolean isType(@Nullable Object object) {
        return object instanceof BinaryTag;
    }

    @Override
    @SuppressWarnings("unchecked")
    public BinaryTag build(@NotNull TagType<?> type, @Nullable Object object) {
        switch (type.id()) {
            case Tag.END:
                return EndBinaryTag.endBinaryTag();
            case Tag.BYTE:
                return object instanceof Boolean ? (((Boolean) object) ? ByteBinaryTag.ONE : ByteBinaryTag.ZERO) : ByteBinaryTag.byteBinaryTag((byte) object);
            case Tag.SHORT:
                return ShortBinaryTag.shortBinaryTag((short) object);
            case Tag.INT:
                return IntBinaryTag.intBinaryTag((int) object);
            case Tag.LONG:
                return LongBinaryTag.longBinaryTag((long) object);
            case Tag.FLOAT:
                return FloatBinaryTag.floatBinaryTag((float) object);
            case Tag.DOUBLE:
                return DoubleBinaryTag.doubleBinaryTag((double) object);
            case Tag.BYTE_ARRAY:
                return ByteArrayBinaryTag.byteArrayBinaryTag(byteArray(object));
            case Tag.STRING:
                return StringBinaryTag.stringBinaryTag((String) object);
            case Tag.LIST:
                return ListBinaryTag.from((Iterable<? extends BinaryTag>) object);
            case Tag.COMPOUND:
                return CompoundBinaryTag.from((Map<String, ? extends BinaryTag>) object);
            case Tag.INT_ARRAY:
                return IntArrayBinaryTag.intArrayBinaryTag(intArray(object));
            case Tag.LONG_ARRAY:
                return LongArrayBinaryTag.longArrayBinaryTag(longArray(object));
            default:
                throw new IllegalArgumentException("Invalid tag type: " + type.name());
        }
    }

    @Override
    public Object extract(@Nullable BinaryTag tag) {
        if (tag == null) {
            return null;
        }
        switch (tag.type().id()) {
            case Tag.END:
                return null;
            case Tag.BYTE:
                return ((ByteBinaryTag) tag).value();
            case Tag.SHORT:
                return ((ShortBinaryTag) tag).value();
            case Tag.INT:
                return ((IntBinaryTag) tag).value();
            case Tag.LONG:
                return ((LongBinaryTag) tag).value();
            case Tag.FLOAT:
                return ((FloatBinaryTag) tag).value();
            case Tag.DOUBLE:
                return ((DoubleBinaryTag) tag).value();
            case Tag.BYTE_ARRAY:
                return ((ByteArrayBinaryTag) tag).value();
            case Tag.STRING:
                return ((StringBinaryTag) tag).value();
            case Tag.LIST:
                try {
                    return LIST_TAGS.invoke(tag);
                } catch (Throwable t) {
                    final List<BinaryTag> list = new ArrayList<>();
                    for (BinaryTag element : ((ListBinaryTag) tag)) {
                        list.add(element);
                    }
                    return list;
                }
            case Tag.COMPOUND:
                try {
                    return COMPOUND_TAGS.invoke(tag);
                } catch (Throwable t) {
                    final Map<String, BinaryTag> map = new HashMap<>();
                    for (Map.Entry<String, ? extends BinaryTag> entry : ((CompoundBinaryTag) tag)) {
                        map.put(entry.getKey(), entry.getValue());
                    }
                    return map;
                }
            case Tag.INT_ARRAY:
                return ((IntArrayBinaryTag) tag).value();
            case Tag.LONG_ARRAY:
                return ((LongArrayBinaryTag) tag).value();
            default:
                throw new IllegalArgumentException("Invalid tag type: " + tag);
        }
    }

    @Override
    public @NotNull <A> TagType<A> type(@Nullable BinaryTag tag) {
        return tag == null ? TagType.getType(Tag.END) : TagType.getType(tag.type().id());
    }

    @Override
    public byte typeId(@Nullable BinaryTag tag) {
        if (tag == null) {
            return Tag.END;
        }
        return tag.type().id();
    }

    @Override
    public @NotNull <A> TagType<A> listType(@NotNull BinaryTag tag) {
        return TagType.getType(((ListBinaryTag) tag).elementType().id());
    }

    @Override
    public byte listTypeId(@NotNull BinaryTag tag) {
        return ((ListBinaryTag) tag).elementType().id();
    }
}
