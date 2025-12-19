package com.saicone.nbt.mapper;

import com.saicone.nbt.TagMapper;
import com.saicone.nbt.TagType;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtEnd;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtString;
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
 * TagMapper implementation to handle NBT values as yarn-mapped Minecraft code abstraction.
 *
 * @author Rubenicos
 */
public class MinecraftYarnTagMapper implements TagMapper<NbtElement> {

    /**
     * {@link MinecraftYarnTagMapper} public instance.
     */
    public static final MinecraftYarnTagMapper INSTANCE = new MinecraftYarnTagMapper();

    private static final MethodHandle STRING_VALUE;
    private static final MethodHandle LIST_VALUE;
    private static final MethodHandle COMPOUND_ENTRIES;

    static {
        MethodHandle stringValue = null;
        MethodHandle listValue = null;
        MethodHandle compoundTags = null;
        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();

            for (Field field : NbtString.class.getDeclaredFields()) {
                if (field.getType() == String.class) {
                    field.setAccessible(true);
                    stringValue = lookup.unreflectGetter(field);
                }
            }

            final Field listField = NbtList.class.getDeclaredField("value");
            listField.setAccessible(true);
            listValue = lookup.unreflectGetter(listField);

            final Field compoundField = NbtCompound.class.getDeclaredField("entries");
            compoundField.setAccessible(true);
            compoundTags = lookup.unreflectGetter(compoundField);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        STRING_VALUE = stringValue;
        LIST_VALUE = listValue;
        COMPOUND_ENTRIES = compoundTags;
    }

    @Override
    public boolean isType(@Nullable Object object) {
        return object instanceof NbtElement;
    }

    @Override
    @SuppressWarnings("unchecked")
    public NbtElement build(@NotNull TagType<?> type, @Nullable Object object) {
        switch (type.id()) {
            case NbtElement.END_TYPE:
                return NbtEnd.INSTANCE;
            case NbtElement.BYTE_TYPE:
                return object instanceof Boolean ? NbtByte.of((boolean) object) : NbtByte.of((byte) object);
            case NbtElement.SHORT_TYPE:
                return NbtShort.of((short) object);
            case NbtElement.INT_TYPE:
                return NbtInt.of((int) object);
            case NbtElement.LONG_TYPE:
                return NbtLong.of((long) object);
            case NbtElement.FLOAT_TYPE:
                return NbtFloat.of((float) object);
            case NbtElement.DOUBLE_TYPE:
                return NbtDouble.of((double) object);
            case NbtElement.BYTE_ARRAY_TYPE:
                return new NbtByteArray(byteArray(object));
            case NbtElement.STRING_TYPE:
                return NbtString.of((String) object);
            case NbtElement.LIST_TYPE:
                final NbtList list = new NbtList();
                list.addAll((List<NbtElement>) object);
                return list;
            case NbtElement.COMPOUND_TYPE:
                final NbtCompound compound = new NbtCompound();
                for (Map.Entry<String, NbtElement> entry : ((Map<String, NbtElement>) object).entrySet()) {
                    compound.put(entry.getKey(), entry.getValue());
                }
                return compound;
            case NbtElement.INT_ARRAY_TYPE:
                return new NbtIntArray(intArray(object));
            case NbtElement.LONG_ARRAY_TYPE:
                return new NbtLongArray(longArray(object));
            default:
                throw new IllegalArgumentException("Invalid tag type: " + type.name());
        }
    }

    @Override
    public Object extract(@Nullable NbtElement element) {
        if (element == null) {
            return null;
        }
        switch (element.getType()) {
            case NbtElement.END_TYPE:
                return null;
            case NbtElement.BYTE_TYPE:
                return ((AbstractNbtNumber) element).byteValue();
            case NbtElement.SHORT_TYPE:
                return ((AbstractNbtNumber) element).shortValue();
            case NbtElement.INT_TYPE:
                return ((AbstractNbtNumber) element).intValue();
            case NbtElement.LONG_TYPE:
                return ((AbstractNbtNumber) element).longValue();
            case NbtElement.FLOAT_TYPE:
                return ((AbstractNbtNumber) element).floatValue();
            case NbtElement.DOUBLE_TYPE:
                return ((AbstractNbtNumber) element).doubleValue();
            case NbtElement.BYTE_ARRAY_TYPE:
                return ((NbtByteArray) element).getByteArray();
            case NbtElement.STRING_TYPE:
                try {
                    return ((NbtString) element).value();
                } catch (Throwable ignored) {
                    try {
                        return STRING_VALUE.invoke(element);
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                }
            case NbtElement.LIST_TYPE:
                try {
                    return LIST_VALUE.invoke(element);
                } catch (Throwable t) {
                    return new ArrayList<>((NbtList) element);
                }
            case NbtElement.COMPOUND_TYPE:
                try {
                    return COMPOUND_ENTRIES.invoke(element);
                } catch (Throwable t) {
                    final Map<String, NbtElement> map = new HashMap<>();
                    for (Map.Entry<String, NbtElement> entry : ((NbtCompound) element).entrySet()) {
                        map.put(entry.getKey(), entry.getValue());
                    }
                    return map;
                }
            case NbtElement.INT_ARRAY_TYPE:
                return ((NbtIntArray) element).getIntArray();
            case NbtElement.LONG_ARRAY_TYPE:
                return ((NbtLongArray) element).getLongArray();
            default:
                throw new IllegalArgumentException("Invalid tag type: " + element);
        }
    }

    @Override
    public @NotNull NbtElement copy(@NotNull NbtElement element) {
        return element.copy();
    }

    @Override
    public int size(@Nullable NbtElement element) {
        try {
            return element == null ? NbtEnd.INSTANCE.getSizeInBytes() : element.getSizeInBytes();
        } catch (Throwable t) {
            return TagMapper.super.size(element);
        }
    }

    @Override
    public @NotNull <A> TagType<A> type(@Nullable NbtElement element) {
        return element == null ? TagType.getType(NbtElement.END_TYPE) : TagType.getType(element.getType());
    }

    @Override
    public byte typeId(@Nullable NbtElement element) {
        if (element == null) {
            return NbtElement.END_TYPE;
        }
        return element.getType();
    }

    @Override
    public @NotNull <A> TagType<A> listType(@NotNull NbtElement element) {
        return TagType.getType(listTypeId(element));
    }

    @Override
    public byte listTypeId(@NotNull NbtElement element) {
        return ((NbtList) element).isEmpty() ? NbtElement.END_TYPE : ((NbtList) element).getFirst().getType();
    }
}
