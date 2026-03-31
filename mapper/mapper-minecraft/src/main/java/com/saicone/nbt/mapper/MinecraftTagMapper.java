package com.saicone.nbt.mapper;

import com.saicone.nbt.TagMapper;
import com.saicone.nbt.TagType;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TagMapper implementation to handle NBT values as Minecraft code abstraction.
 *
 * @author Rubenicos
 */
public class MinecraftTagMapper implements TagMapper<Tag> {

    /**
     * {@link MinecraftTagMapper} public instance.
     */
    public static final MinecraftTagMapper INSTANCE = new MinecraftTagMapper();

    private static final NbtOps DYNAMIC_OPS = NbtOps.INSTANCE;

    // old version
    private static final MethodHandle BYTE_VALUE = method(NumericTag.class, "getAsByte");
    private static final MethodHandle SHORT_VALUE = method(NumericTag.class, "getAsShort");
    private static final MethodHandle INT_VALUE = method(NumericTag.class, "getAsInt");
    private static final MethodHandle LONG_VALUE = method(NumericTag.class, "getAsLong");
    private static final MethodHandle FLOAT_VALUE = method(NumericTag.class, "getAsFloat");
    private static final MethodHandle DOUBLE_VALUE = method(NumericTag.class, "getAsDouble");
    private static final MethodHandle STRING_VALUE = method(StringTag.class, "getAsString");
    private static final MethodHandle LIST_TYPE = method(ListTag.class, "getElementType");
    private static final MethodHandle COMPOUND_KEYS = method(CompoundTag.class, "getAllKeys");
    // non-accessible
    private static final MethodHandle LIST_VALUE = getter(ListTag.class, "list");
    private static final MethodHandle COMPOUND_VALUE = getter(CompoundTag.class, "tags");

    @Override
    public boolean isType(@Nullable Object object) {
        return object instanceof Tag;
    }

    @Override
    public Tag build(@NotNull TagType<?> type, @Nullable Object object) {
        switch (type.id()) {
            case Tag.TAG_END:
                return EndTag.INSTANCE;
            case Tag.TAG_BYTE:
                return object instanceof Boolean ? ByteTag.valueOf((boolean) object) : ByteTag.valueOf((byte) object);
            case Tag.TAG_SHORT:
                return ShortTag.valueOf((short) object);
            case Tag.TAG_INT:
                return IntTag.valueOf((int) object);
            case Tag.TAG_LONG:
                return LongTag.valueOf((long) object);
            case Tag.TAG_FLOAT:
                return FloatTag.valueOf((float) object);
            case Tag.TAG_DOUBLE:
                return DoubleTag.valueOf((double) object);
            case Tag.TAG_BYTE_ARRAY:
                return new ByteArrayTag(byteArray(object));
            case Tag.TAG_STRING:
                return StringTag.valueOf((String) object);
            case Tag.TAG_LIST:
                return new ListTag((List<Tag>) object);
            case Tag.TAG_COMPOUND:
                final CompoundTag tag = new CompoundTag();
                for (Map.Entry<String, Tag> entry : ((Map<String, Tag>) object).entrySet()) {
                    tag.put(entry.getKey(), entry.getValue());
                }
                return tag;
            case Tag.TAG_INT_ARRAY:
                return new IntArrayTag(intArray(object));
            case Tag.TAG_LONG_ARRAY:
                return new LongArrayTag(longArray(object));
            default:
                throw new IllegalArgumentException("Invalid tag type: " + type.name());
        }
    }

    @Override
    public Tag parse(@NotNull List<?> list) {
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
    public Object extract(@Nullable Tag tag) {
        if (tag == null) {
            return null;
        }
        if (LIST_TYPE == null) { // +1.21.5
            switch (tag.getId()) {
                case Tag.TAG_END:
                    return null;
                case Tag.TAG_BYTE:
                    return ((NumericTag) tag).byteValue();
                case Tag.TAG_SHORT:
                    return ((NumericTag) tag).shortValue();
                case Tag.TAG_INT:
                    return ((NumericTag) tag).intValue();
                case Tag.TAG_LONG:
                    return ((NumericTag) tag).longValue();
                case Tag.TAG_FLOAT:
                    return ((NumericTag) tag).floatValue();
                case Tag.TAG_DOUBLE:
                    return ((NumericTag) tag).doubleValue();
                case Tag.TAG_BYTE_ARRAY:
                    return ((ByteArrayTag) tag).getAsByteArray();
                case Tag.TAG_STRING:
                    return ((StringTag) tag).value();
                case Tag.TAG_LIST:
                    try {
                        return LIST_VALUE.invoke(tag);
                    } catch (Throwable t) {
                        return new ArrayList<>((ListTag) tag);
                    }
                case Tag.TAG_COMPOUND:
                    try {
                        return COMPOUND_VALUE.invoke(tag);
                    } catch (Throwable t) {
                        final Map<String, Tag> map = new HashMap<>(((CompoundTag) tag).size());
                        for (Map.Entry<String, Tag> entry : ((CompoundTag) tag).entrySet()) {
                            map.put(entry.getKey(), entry.getValue());
                        }
                        return map;
                    }
                case Tag.TAG_INT_ARRAY:
                    return ((IntArrayTag) tag).getAsIntArray();
                case Tag.TAG_LONG_ARRAY:
                    return ((LongArrayTag) tag).getAsLongArray();
                default:
                    throw new IllegalArgumentException("Invalid tag type: " + tag);
            }
        } else {
            switch (tag.getId()) {
                case Tag.TAG_END:
                    return null;
                case Tag.TAG_BYTE:
                    return invoke(BYTE_VALUE, tag);
                case Tag.TAG_SHORT:
                    return invoke(SHORT_VALUE, tag);
                case Tag.TAG_INT:
                    return invoke(INT_VALUE, tag);
                case Tag.TAG_LONG:
                    return invoke(LONG_VALUE, tag);
                case Tag.TAG_FLOAT:
                    return invoke(FLOAT_VALUE, tag);
                case Tag.TAG_DOUBLE:
                    return invoke(DOUBLE_VALUE, tag);
                case Tag.TAG_BYTE_ARRAY:
                    return ((ByteArrayTag) tag).getAsByteArray();
                case Tag.TAG_STRING:
                    return invoke(STRING_VALUE, tag);
                case Tag.TAG_LIST:
                    try {
                        return LIST_VALUE.invoke(tag);
                    } catch (Throwable t) {
                        return new ArrayList<>((ListTag) tag);
                    }
                case Tag.TAG_COMPOUND:
                    try {
                        return COMPOUND_VALUE.invoke(tag);
                    } catch (Throwable t) {
                        final Set<String> keys = invoke(COMPOUND_KEYS, tag);
                        final Map<String, Tag> map = new HashMap<>(keys.size());
                        for (String key : keys) {
                            map.put(key, ((CompoundTag) tag).get(key));
                        }
                        return map;
                    }
                case Tag.TAG_INT_ARRAY:
                    return ((IntArrayTag) tag).getAsIntArray();
                case Tag.TAG_LONG_ARRAY:
                    return ((LongArrayTag) tag).getAsLongArray();
                default:
                    throw new IllegalArgumentException("Invalid tag type: " + tag);
            }
        }
    }

    @Override
    public @NotNull Tag copy(@NotNull Tag tag) {
        return tag.copy();
    }

    @Override
    public int size(@Nullable Tag tag) {
        try {
            return tag == null ? EndTag.INSTANCE.sizeInBytes() : tag.sizeInBytes();
        } catch (NoSuchMethodError e) {
            return TagMapper.super.size(tag);
        }
    }

    @Override
    public @NotNull <A> TagType<A> type(@Nullable Tag tag) {
        return tag == null ? TagType.getType(Tag.TAG_END) : TagType.getType(tag.getId());
    }

    @Override
    public byte typeId(@Nullable Tag tag) {
        if (tag == null) {
            return Tag.TAG_END;
        }
        return tag.getId();
    }

    @Override
    public @NotNull <A> TagType<A> listType(@NotNull Tag tag) {
        return TagType.getType(listTypeId(tag));
    }

    @Override
    public byte listTypeId(@NotNull Tag tag) {
        if (LIST_TYPE == null) { // +1.21.5
            return ((ListTag) tag).isEmpty() ? Tag.TAG_END : ((ListTag) tag).get(0).getId();
        } else {
            return invoke(LIST_TYPE, tag);
        }
    }

    @Nullable
    private static MethodHandle getter(@NotNull Class<?> clazz, @NotNull String name) {
        try {
            final Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return MethodHandles.lookup().unreflectGetter(field);
        } catch (Throwable t) {
            return null;
        }
    }

    @Nullable
    private static MethodHandle method(@NotNull Class<?> clazz, @NotNull String name, @NotNull Class<?>... parameters) {
        try {
            final Method method = clazz.getDeclaredMethod(name, parameters);
            method.setAccessible(true);
            return MethodHandles.lookup().unreflect(method);
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T invoke(@NotNull MethodHandle handle, @NotNull Object arg1) {
        try {
            return (T) handle.invoke(arg1);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
