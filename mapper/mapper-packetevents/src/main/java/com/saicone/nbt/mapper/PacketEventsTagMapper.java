package com.saicone.nbt.mapper;

import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTByte;
import com.github.retrooper.packetevents.protocol.nbt.NBTByteArray;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTDouble;
import com.github.retrooper.packetevents.protocol.nbt.NBTEnd;
import com.github.retrooper.packetevents.protocol.nbt.NBTFloat;
import com.github.retrooper.packetevents.protocol.nbt.NBTInt;
import com.github.retrooper.packetevents.protocol.nbt.NBTIntArray;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTLong;
import com.github.retrooper.packetevents.protocol.nbt.NBTLongArray;
import com.github.retrooper.packetevents.protocol.nbt.NBTNumber;
import com.github.retrooper.packetevents.protocol.nbt.NBTShort;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.nbt.NBTType;
import com.github.retrooper.packetevents.protocol.nbt.serializer.DefaultNBTSerializer;
import com.saicone.nbt.Tag;
import com.saicone.nbt.TagMapper;
import com.saicone.nbt.TagType;
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
 * TagMapper implementation to handle NBT values as PacketEvents code abstraction.
 *
 * @author Rubenicos
 */
public class PacketEventsTagMapper extends DefaultNBTSerializer implements TagMapper<NBT> {

    /**
     * {@link PacketEventsTagMapper} public instance.
     */
    public static final PacketEventsTagMapper INSTANCE = new PacketEventsTagMapper();

    private static final MethodHandle LIST_TAGS;
    private static final MethodHandle COMPOUND_TAGS;

    static {
        MethodHandle listValue = null;
        MethodHandle compoundTags = null;
        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();

            final Field listField = NBTList.class.getDeclaredField("tags");
            listField.setAccessible(true);
            listValue = lookup.unreflectGetter(listField);

            final Field compoundField = NBTCompound.class.getDeclaredField("tags");
            compoundField.setAccessible(true);
            compoundTags = lookup.unreflectGetter(compoundField);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        LIST_TAGS = listValue;
        COMPOUND_TAGS = compoundTags;
    }

    @Override
    public boolean isType(@Nullable Object object) {
        return object instanceof NBT;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public NBT build(@NotNull TagType<?> type, @Nullable Object object) {
        switch (type.id()) {
            case Tag.END:
                return NBTEnd.INSTANCE;
            case Tag.BYTE:
                return object instanceof Boolean ? new NBTByte((boolean) object) : new NBTByte((byte) object);
            case Tag.SHORT:
                return new NBTShort((short) object);
            case Tag.INT:
                return new NBTInt((int) object);
            case Tag.LONG:
                return new NBTLong((long) object);
            case Tag.FLOAT:
                return new NBTFloat((float) object);
            case Tag.DOUBLE:
                return new NBTDouble((double) object);
            case Tag.BYTE_ARRAY:
                return new NBTByteArray(byteArray(object));
            case Tag.STRING:
                return new NBTString((String) object);
            case Tag.LIST:
                final List<NBT> list = (List<NBT>) object;
                if (list.isEmpty()) {
                    return new NBTList<>(NBTType.END);
                }
                return new NBTList(list.get(0).getType(), (List<NBT>) object);
            case Tag.COMPOUND:
                final NBTCompound compound = new NBTCompound();
                for (Map.Entry<String, NBT> entry : ((Map<String, NBT>) object).entrySet()) {
                    compound.setTag(entry.getKey(), entry.getValue());
                }
                return compound;
            case Tag.INT_ARRAY:
                return new NBTIntArray(intArray(object));
            case Tag.LONG_ARRAY:
                return new NBTLongArray(longArray(object));
            default:
                throw new IllegalArgumentException("Invalid tag type: " + type.name());
        }
    }

    @Override
    public Object extract(@Nullable NBT tag) {
        if (tag == null) {
            return null;
        }
        switch (typeToId.get(tag.getType()).byteValue()) {
            case Tag.END:
                return null;
            case Tag.BYTE:
                return ((NBTNumber) tag).getAsByte();
            case Tag.SHORT:
                return ((NBTNumber) tag).getAsShort();
            case Tag.INT:
                return ((NBTNumber) tag).getAsInt();
            case Tag.LONG:
                return ((NBTNumber) tag).getAsLong();
            case Tag.FLOAT:
                return ((NBTNumber) tag).getAsFloat();
            case Tag.DOUBLE:
                return ((NBTNumber) tag).getAsDouble();
            case Tag.BYTE_ARRAY:
                return ((NBTByteArray) tag).getValue();
            case Tag.STRING:
                return ((NBTString) tag).getValue();
            case Tag.LIST:
                try {
                    return LIST_TAGS.invoke(tag);
                } catch (Throwable t) {
                    return new ArrayList<>(((NBTList<?>) tag).getTags());
                }
            case Tag.COMPOUND:
                try {
                    return COMPOUND_TAGS.invoke(tag);
                } catch (Throwable t) {
                    return new HashMap<>(((NBTCompound) tag).getTags());
                }
            case Tag.INT_ARRAY:
                return ((NBTIntArray) tag).getValue();
            case Tag.LONG_ARRAY:
                return ((NBTLongArray) tag).getValue();
            default:
                throw new IllegalArgumentException("Invalid tag type: " + tag);
        }
    }

    @Override
    public @NotNull NBT copy(@NotNull NBT tag) {
        return tag.copy();
    }

    @Override
    public @NotNull <A> TagType<A> type(@Nullable NBT tag) {
        return TagType.getType(typeId(tag));
    }

    @Override
    public byte typeId(@Nullable NBT tag) {
        if (tag == null) {
            return Tag.END;
        }
        return typeToId.get(tag.getType()).byteValue();
    }

    @Override
    public @NotNull <A> TagType<A> listType(@NotNull NBT tag) {
        return TagType.getType(listTypeId(tag));
    }

    @Override
    public byte listTypeId(@NotNull NBT tag) {
        return ((NBTList<?>) tag).isEmpty() ? Tag.END : typeToId.get(((NBTList<?>) tag).getTag(0)).byteValue();
    }
}
