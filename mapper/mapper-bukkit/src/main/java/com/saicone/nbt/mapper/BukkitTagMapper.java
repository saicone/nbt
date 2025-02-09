package com.saicone.nbt.mapper;

import com.saicone.nbt.Tag;
import com.saicone.nbt.TagMapper;
import com.saicone.nbt.TagType;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * TagMapper implementation to handle NBT values on Bukkit server implementations,
 * including compatibility with mojang-mapped servers and older Bukkit versions like {@code 1.7.x}.<br>
 * Unlike other mapper implementations, this one use {@link Object} as the main tag representation
 * because every NBT object interaction is completely handled with a {@link MethodHandle}.
 *
 * @author Rubenicos
 */
@SuppressWarnings("fallthrough")
public class BukkitTagMapper implements TagMapper<Object> {

    /**
     * {@link BukkitTagMapper} public instance.
     */
    public static final BukkitTagMapper INSTANCE = new BukkitTagMapper();

    // --- READ THIS BEFORE CONTINUE
    // The usage of MethodHandle in static final fields make the reflection
    // to act like direct calls, so this implementation is probably faster
    // than Minecraft mapper implementation itself.
    //
    // This class was written into the most descriptive way possible,
    // there are better and shorter ways to create a static final MethodHandle,
    // but I didn't take advantage of that to make anyone able to read this
    // mapper implementation as a simple and repetitive code.

    // Initial NBT format
    private static final int OLDER = -1;
    // Added LongArrayTag
    private static final int V_1_12 = 1139;
    private static final int V_1_13 = 1519;
    private static final int V_1_14_4 = 1976;
    // Added cached objects to reduce memory usage
    private static final int V_1_15 = 2225;
    // Introduce mojang mappings
    private static final int V_1_17 = 2724;
    private static final int V_1_18 = 2860;
    // Added method to calculate size in bytes
    private static final int V_1_19_3 = 3218;

    // Mod Coder Pack class names, used by very old Bukkit versions and nowadays by Spigot mappings
    private static final Map<String, String> MPC_MAPPINGS = Map.ofEntries(
            Map.entry("Tag", "NBTBase"),
            Map.entry("EndTag", "NBTTagEnd"),
            Map.entry("ByteTag", "NBTTagByte"),
            Map.entry("ShortTag", "NBTTagShort"),
            Map.entry("IntTag", "NBTTagInt"),
            Map.entry("LongTag", "NBTTagLong"),
            Map.entry("FloatTag", "NBTTagFloat"),
            Map.entry("DoubleTag", "NBTTagDouble"),
            Map.entry("ByteArrayTag", "NBTTagByteArray"),
            Map.entry("StringTag", "NBTTagString"),
            Map.entry("ListTag", "NBTTagList"),
            Map.entry("CompoundTag", "NBTTagCompound"),
            Map.entry("IntArrayTag", "NBTTagIntArray"),
            Map.entry("LongArrayTag", "NBTTagLongArray")
    );

    private static final int VERSION;

    private static final Class<?> TAG_TYPE;

    private static final MethodHandle TAG_ID;
    private static final MethodHandle TAG_SIZE;

    private static final MethodHandle NEW_END;
    private static final MethodHandle NEW_BYTE;
    private static final MethodHandle NEW_BOOLEAN;
    private static final MethodHandle NEW_SHORT;
    private static final MethodHandle NEW_INT;
    private static final MethodHandle NEW_LONG;
    private static final MethodHandle NEW_FLOAT;
    private static final MethodHandle NEW_DOUBLE;
    private static final MethodHandle NEW_BYTE_ARRAY;
    private static final MethodHandle NEW_STRING;
    private static final MethodHandle NEW_LIST;
    private static final MethodHandle NEW_COMPOUND;
    private static final MethodHandle NEW_INT_ARRAY;
    private static final MethodHandle NEW_LONG_ARRAY;

    private static final MethodHandle GET_BYTE;
    private static final MethodHandle GET_SHORT;
    private static final MethodHandle GET_INT;
    private static final MethodHandle GET_LONG;
    private static final MethodHandle GET_FLOAT;
    private static final MethodHandle GET_DOUBLE;
    private static final MethodHandle GET_BYTE_ARRAY;
    private static final MethodHandle GET_STRING;
    private static final MethodHandle GET_LIST;
    private static final MethodHandle GET_COMPOUND;
    private static final MethodHandle GET_INT_ARRAY;
    private static final MethodHandle GET_LONG_ARRAY;

    private static final MethodHandle GET_LIST_TYPE;
    private static final MethodHandle SET_LIST_TYPE;

    static {
        final int version;
        final String[] split = Bukkit.getServer().getBukkitVersion().replace("MC:", "").trim().split("\\.");
        if (split.length < 2) {
            version = OLDER;
        } else {
            final int feature;
            final int minor;
            if (split[1].contains("-") || split[1].contains("_")) {
                feature = Integer.parseInt(split[1].split("[-_]")[0]);
                minor = 0;
            } else {
                feature = Integer.parseInt(split[1]);
                minor = split.length > 2 ? Integer.parseInt(split[2].split("[-_]")[0]) : 0;
            }
            if (feature > 19) {
                version = V_1_19_3;
            } else {
                switch (feature) {
                    case 19:
                        if (minor == 3) {
                            version = V_1_19_3;
                            break;
                        }
                    case 18:
                        version = V_1_18;
                        break;
                    case 17:
                        version = V_1_17;
                        break;
                    case 16:
                    case 15:
                        version = V_1_15;
                        break;
                    case 14:
                        version = V_1_14_4;
                        break;
                    case 13:
                        version = V_1_13;
                        break;
                    case 12:
                        version = V_1_12;
                        break;
                    default:
                        version = OLDER;
                        break;
                }
            }
        }

        Function<String, String> mapper;
        if (version >= V_1_17) {
            mapper = name -> "net.minecraft." + name;
        } else {
            final String packageVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            mapper = name ->
                    "net.minecraft.server." +
                            packageVersion + "." +
                            (name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : name);
        }

        boolean isMojangMapped = false;
        try {
            Class.forName(mapper.apply("nbt.CompoundTag"));
            isMojangMapped = true;
        } catch (ClassNotFoundException e) {
            mapper = mapper.compose(name -> {
                if (name.contains(".")) {
                    final int index = name.lastIndexOf('.') + 1;
                    final String key = name.substring(index);
                    return name.substring(0, index) + MPC_MAPPINGS.getOrDefault(key, key);
                }
                return MPC_MAPPINGS.getOrDefault(name, name);
            });
        }

        Class<?> Tag = null;
        Class<?> EndTag = null;
        Class<?> ByteTag = null;
        Class<?> ShortTag = null;
        Class<?> IntTag = null;
        Class<?> LongTag = null;
        Class<?> FloatTag = null;
        Class<?> DoubleTag = null;
        Class<?> ByteArrayTag = null;
        Class<?> StringTag = null;
        Class<?> ListTag = null;
        Class<?> CompoundTag = null;
        Class<?> IntArrayTag = null;
        Class<?> LongArrayTag = null;
        try {
            Tag = Class.forName(mapper.apply("nbt.Tag"));
            EndTag = Class.forName(mapper.apply("nbt.EndTag"));
            ByteTag = Class.forName(mapper.apply("nbt.ByteTag"));
            ShortTag = Class.forName(mapper.apply("nbt.ShortTag"));
            IntTag = Class.forName(mapper.apply("nbt.IntTag"));
            LongTag = Class.forName(mapper.apply("nbt.LongTag"));
            FloatTag = Class.forName(mapper.apply("nbt.FloatTag"));
            DoubleTag = Class.forName(mapper.apply("nbt.DoubleTag"));
            ByteArrayTag = Class.forName(mapper.apply("nbt.ByteArrayTag"));
            StringTag = Class.forName(mapper.apply("nbt.StringTag"));
            ListTag = Class.forName(mapper.apply("nbt.ListTag"));
            CompoundTag = Class.forName(mapper.apply("nbt.CompoundTag"));
            IntArrayTag = Class.forName(mapper.apply("nbt.IntArrayTag"));
            if (version >= V_1_12) {
                LongArrayTag = Class.forName(mapper.apply("nbt.LongArrayTag"));
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        MethodHandle tag$id = null;
        MethodHandle tag$size = null;
        MethodHandle new$EndTag = null;
        MethodHandle new$ByteTag = null;
        MethodHandle new$Boolean = null;
        MethodHandle new$ShortTag = null;
        MethodHandle new$IntTag = null;
        MethodHandle new$LongTag = null;
        MethodHandle new$FloatTag = null;
        MethodHandle new$DoubleTag = null;
        MethodHandle new$ByteArrayTag = null;
        MethodHandle new$StringTag = null;
        MethodHandle new$ListTag = null;
        MethodHandle new$CompoundTag = null;
        MethodHandle new$IntArrayTag = null;
        MethodHandle new$LongArrayTag = null;
        MethodHandle get$ByteTag = null;
        MethodHandle get$ShortTag = null;
        MethodHandle get$IntTag = null;
        MethodHandle get$LongTag = null;
        MethodHandle get$FloatTag = null;
        MethodHandle get$DoubleTag = null;
        MethodHandle get$ByteArrayTag = null;
        MethodHandle get$StringTag = null;
        MethodHandle get$ListTag = null;
        MethodHandle get$CompoundTag = null;
        MethodHandle get$IntArrayTag = null;
        MethodHandle get$LongArrayTag = null;
        MethodHandle get$listType = null;
        MethodHandle set$listType = null;
        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();

            if (isMojangMapped) {
                tag$id = lookup.findVirtual(Tag, "getId", MethodType.methodType(byte.class));
                if (version >= V_1_19_3) {
                    tag$size = lookup.findVirtual(Tag, "sizeInBytes", MethodType.methodType(int.class));
                }
            } else if (version >= V_1_19_3) {
                tag$id = lookup.findVirtual(Tag, "b", MethodType.methodType(byte.class));
                tag$size = lookup.findVirtual(Tag, "a", MethodType.methodType(int.class));
            } else if (version >= V_1_18) {
                tag$id = lookup.findVirtual(Tag, "a", MethodType.methodType(byte.class));
            } else {
                tag$id = lookup.findVirtual(Tag, "getTypeId", MethodType.methodType(byte.class));
            }

            if (isMojangMapped && version >= V_1_15) {
                final Constructor<?> constructor$List = ListTag.getDeclaredConstructor(List.class, byte.class);
                final Constructor<?> constructor$Compound = CompoundTag.getDeclaredConstructor(Map.class);
                constructor$List.setAccessible(true);
                constructor$Compound.setAccessible(true);

                new$EndTag = lookup.findStaticGetter(EndTag, "INSTANCE", EndTag);
                new$ByteTag = lookup.findStatic(ByteTag, "valueOf", MethodType.methodType(ByteTag, byte.class));
                new$Boolean = lookup.findStatic(ByteTag, "valueOf", MethodType.methodType(ByteTag, boolean.class));
                new$ShortTag = lookup.findStatic(ShortTag, "valueOf", MethodType.methodType(ShortTag, short.class));
                new$IntTag = lookup.findStatic(IntTag, "valueOf", MethodType.methodType(IntTag, int.class));
                new$LongTag = lookup.findStatic(LongTag, "valueOf", MethodType.methodType(LongTag, long.class));
                new$FloatTag = lookup.findStatic(FloatTag, "valueOf", MethodType.methodType(FloatTag, float.class));
                new$DoubleTag = lookup.findStatic(DoubleTag, "valueOf", MethodType.methodType(DoubleTag, double.class));
                new$ByteArrayTag = lookup.findConstructor(ByteArrayTag, MethodType.methodType(void.class, byte[].class));
                new$StringTag = lookup.findStatic(StringTag, "valueOf", MethodType.methodType(StringTag, String.class));
                new$ListTag = lookup.unreflectConstructor(constructor$List);
                new$CompoundTag = lookup.unreflectConstructor(constructor$Compound);
                new$IntArrayTag = lookup.findConstructor(IntArrayTag, MethodType.methodType(void.class, int[].class));
                new$LongArrayTag = lookup.findConstructor(LongArrayTag, MethodType.methodType(void.class, long[].class));
            } else if (version >= V_1_15) {
                final Constructor<?> constructor$List = ListTag.getDeclaredConstructor(List.class, byte.class);
                final Constructor<?> constructor$Compound = CompoundTag.getDeclaredConstructor(Map.class);
                constructor$List.setAccessible(true);
                constructor$Compound.setAccessible(true);

                new$EndTag = lookup.findStaticGetter(EndTag, "b", EndTag);
                new$ByteTag = lookup.findStatic(ByteTag, "a", MethodType.methodType(ByteTag, byte.class));
                new$Boolean = lookup.findStatic(ByteTag, "a", MethodType.methodType(ByteTag, boolean.class));
                new$ShortTag = lookup.findStatic(ShortTag, "a", MethodType.methodType(ShortTag, short.class));
                new$IntTag = lookup.findStatic(IntTag, "a", MethodType.methodType(IntTag, int.class));
                new$LongTag = lookup.findStatic(LongTag, "a", MethodType.methodType(LongTag, long.class));
                new$FloatTag = lookup.findStatic(FloatTag, "a", MethodType.methodType(FloatTag, float.class));
                new$DoubleTag = lookup.findStatic(DoubleTag, "a", MethodType.methodType(DoubleTag, double.class));
                new$ByteArrayTag = lookup.findConstructor(ByteArrayTag, MethodType.methodType(void.class, byte[].class));
                new$StringTag = lookup.findStatic(StringTag, "a", MethodType.methodType(StringTag, String.class));
                new$ListTag = lookup.unreflectConstructor(constructor$List);
                new$CompoundTag = lookup.unreflectConstructor(constructor$Compound);
                new$IntArrayTag = lookup.findConstructor(IntArrayTag, MethodType.methodType(void.class, int[].class));
                new$LongArrayTag = lookup.findConstructor(LongArrayTag, MethodType.methodType(void.class, long[].class));
            } else {
                final Constructor<?> constructor$End = EndTag.getDeclaredConstructor();
                constructor$End.setAccessible(true);

                new$EndTag = lookup.unreflectConstructor(constructor$End);
                new$ByteTag = lookup.findConstructor(ByteTag, MethodType.methodType(void.class, byte.class));
                new$ShortTag = lookup.findConstructor(ShortTag, MethodType.methodType(void.class, short.class));
                new$IntTag = lookup.findConstructor(IntTag, MethodType.methodType(void.class, int.class));
                new$LongTag = lookup.findConstructor(LongTag, MethodType.methodType(void.class, long.class));
                new$FloatTag = lookup.findConstructor(FloatTag, MethodType.methodType(void.class, float.class));
                new$DoubleTag = lookup.findConstructor(DoubleTag, MethodType.methodType(void.class, double.class));
                new$ByteArrayTag = lookup.findConstructor(ByteArrayTag, MethodType.methodType(void.class, byte[].class));
                new$StringTag = lookup.findConstructor(StringTag, MethodType.methodType(void.class, String.class));
                new$ListTag = lookup.findConstructor(ListTag, MethodType.methodType(void.class));
                new$CompoundTag = lookup.findConstructor(CompoundTag, MethodType.methodType(void.class));
                new$IntArrayTag = lookup.findConstructor(IntArrayTag, MethodType.methodType(void.class, int[].class));
                if (version >= V_1_12) {
                    new$LongArrayTag = lookup.findConstructor(LongArrayTag, MethodType.methodType(void.class, long[].class));
                }
            }

            final Field field$byte;
            final Field field$short;
            final Field field$int;
            final Field field$long;
            final Field field$float;
            final Field field$double;
            final Field field$byteArray;
            final Field field$String;
            final Field field$List;
            final Field field$Map;
            final Field field$intArray;
            final Field field$longArray;
            if (isMojangMapped && version >= V_1_17) {
                field$byte = ByteTag.getDeclaredField("data");
                field$short = ShortTag.getDeclaredField("data");
                field$int = IntTag.getDeclaredField("data");
                field$long = LongTag.getDeclaredField("data");
                field$float = FloatTag.getDeclaredField("data");
                field$double = DoubleTag.getDeclaredField("data");
                field$byteArray = ByteArrayTag.getDeclaredField("data");
                field$String = StringTag.getDeclaredField("data");
                field$List = ListTag.getDeclaredField("list");
                field$Map = CompoundTag.getDeclaredField("tags");
                field$intArray = IntArrayTag.getDeclaredField("data");
                field$longArray = LongArrayTag.getDeclaredField("data");
            } else if (version >= V_1_17) {
                field$byte = ByteTag.getDeclaredField("x");
                field$short = ShortTag.getDeclaredField("c");
                field$int = IntTag.getDeclaredField("c");
                field$long = LongTag.getDeclaredField("c");
                field$float = FloatTag.getDeclaredField("w");
                field$double = DoubleTag.getDeclaredField("w");
                field$byteArray = ByteArrayTag.getDeclaredField("c");
                field$String = StringTag.getDeclaredField("A");
                field$List = ListTag.getDeclaredField("c");
                field$Map = CompoundTag.getDeclaredField("x");
                field$intArray = IntArrayTag.getDeclaredField("c");
                field$longArray = LongArrayTag.getDeclaredField("c");
            } else {
                field$byte = ByteTag.getDeclaredField("data");
                field$short = ShortTag.getDeclaredField("data");
                field$int = IntTag.getDeclaredField("data");
                field$long = LongTag.getDeclaredField("data");
                field$float = FloatTag.getDeclaredField("data");
                field$double = DoubleTag.getDeclaredField("data");
                field$byteArray = ByteArrayTag.getDeclaredField("data");
                field$String = StringTag.getDeclaredField("data");
                field$List = ListTag.getDeclaredField("list");
                field$Map = CompoundTag.getDeclaredField("map");
                field$intArray = IntArrayTag.getDeclaredField("data");
                if (version >= V_1_13 && version <= V_1_14_4) {
                    field$longArray = LongArrayTag.getDeclaredField("f");
                } else {
                    field$longArray = LongArrayTag.getDeclaredField("b");
                }
            }
            field$byte.setAccessible(true);
            field$short.setAccessible(true);
            field$int.setAccessible(true);
            field$long.setAccessible(true);
            field$float.setAccessible(true);
            field$double.setAccessible(true);
            field$byteArray.setAccessible(true);
            field$String.setAccessible(true);
            field$List.setAccessible(true);
            field$Map.setAccessible(true);
            field$intArray.setAccessible(true);
            field$longArray.setAccessible(true);

            get$ByteTag = lookup.unreflectGetter(field$byte);
            get$ShortTag = lookup.unreflectGetter(field$short);
            get$IntTag = lookup.unreflectGetter(field$int);
            get$LongTag = lookup.unreflectGetter(field$long);
            get$FloatTag = lookup.unreflectGetter(field$float);
            get$DoubleTag = lookup.unreflectGetter(field$double);
            get$ByteArrayTag = lookup.unreflectGetter(field$byteArray);
            get$StringTag = lookup.unreflectGetter(field$String);
            get$ListTag = lookup.unreflectGetter(field$List);
            get$CompoundTag = lookup.unreflectGetter(field$Map);
            get$IntArrayTag = lookup.unreflectGetter(field$intArray);
            get$LongArrayTag = lookup.unreflectGetter(field$longArray);

            final Field field$type;
            if (isMojangMapped && version >= V_1_17) {
                field$type = ListTag.getDeclaredField("type");
            } else if (version >= V_1_17) {
                field$type = ListTag.getDeclaredField("w");
            } else {
                field$type = ListTag.getDeclaredField("type");
            }
            field$type.setAccessible(true);

            get$listType = lookup.unreflectGetter(field$type);
            set$listType = lookup.unreflectSetter(field$type);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        VERSION = version;

        TAG_TYPE = Tag;

        TAG_ID = tag$id;
        TAG_SIZE = tag$size;

        NEW_END = new$EndTag;
        NEW_BYTE = new$ByteTag;
        NEW_BOOLEAN = new$Boolean;
        NEW_SHORT = new$ShortTag;
        NEW_INT = new$IntTag;
        NEW_LONG = new$LongTag;
        NEW_FLOAT = new$FloatTag;
        NEW_DOUBLE = new$DoubleTag;
        NEW_BYTE_ARRAY = new$ByteArrayTag;
        NEW_STRING = new$StringTag;
        NEW_LIST = new$ListTag;
        NEW_COMPOUND = new$CompoundTag;
        NEW_INT_ARRAY = new$IntArrayTag;
        NEW_LONG_ARRAY = new$LongArrayTag;

        GET_BYTE = get$ByteTag;
        GET_SHORT = get$ShortTag;
        GET_INT = get$IntTag;
        GET_LONG = get$LongTag;
        GET_FLOAT = get$FloatTag;
        GET_DOUBLE = get$DoubleTag;
        GET_BYTE_ARRAY = get$ByteArrayTag;
        GET_STRING = get$StringTag;
        GET_LIST = get$ListTag;
        GET_COMPOUND = get$CompoundTag;
        GET_INT_ARRAY = get$IntArrayTag;
        GET_LONG_ARRAY = get$LongArrayTag;
        GET_LIST_TYPE = get$listType;
        SET_LIST_TYPE = set$listType;
    }

    private static final boolean CACHE_COMPATIBLE = VERSION >= V_1_15;

    private static final int HIGH = 1024;
    private static final int LOW = -128;

    private static final Object CACHE_END;
    private static final Object CACHE_FALSE;
    private static final Object CACHE_TRUE;
    private static final Object[] CACHE_BYTE = new Object[256];
    private static final Object[] CACHE_SHORT = new Object[HIGH + Math.abs(LOW) + 1];
    private static final Object[] CACHE_INT = new Object[HIGH + Math.abs(LOW) + 1];
    private static final Object[] CACHE_LONG = new Object[HIGH + Math.abs(LOW) + 1];
    private static final Object CACHE_FLOAT;
    private static final Object CACHE_DOUBLE;
    private static final Object CACHE_STRING;

    static {
        if (CACHE_COMPATIBLE) {
            CACHE_END = null;
            CACHE_FALSE = null;
            CACHE_TRUE = null;
            CACHE_FLOAT = null;
            CACHE_DOUBLE = null;
            CACHE_STRING = null;
        } else {
            Object cache$End = null;
            Object cache$False = null;
            Object cache$True = null;
            Object cache$Float = null;
            Object cache$Double = null;
            Object cache$String = null;
            try {
                cache$End = NEW_END.invoke();
                cache$False = NEW_BYTE.invoke((byte) 0);
                cache$True = NEW_BYTE.invoke((byte) 1);
                for (int i = 0; i < CACHE_BYTE.length; i++) {
                    CACHE_BYTE[i] = NEW_BYTE.invoke((byte) (i + LOW));
                }
                for (int i = 0; i < CACHE_SHORT.length; i++) {
                    CACHE_SHORT[i] = NEW_SHORT.invoke((short) (LOW + i));
                }
                for (int i = 0; i < CACHE_INT.length; i++) {
                    CACHE_INT[i] = NEW_INT.invoke(LOW + i);
                }
                for (int i = 0; i < CACHE_LONG.length; i++) {
                    CACHE_LONG[i] = NEW_LONG.invoke((long) (LOW + i));
                }
                cache$Float = NEW_FLOAT.invoke(0f);
                cache$Double = NEW_DOUBLE.invoke(0d);
                cache$String = NEW_STRING.invoke("");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            CACHE_END = cache$End;
            CACHE_FALSE = cache$False;
            CACHE_TRUE = cache$True;
            CACHE_FLOAT = cache$Float;
            CACHE_DOUBLE = cache$Double;
            CACHE_STRING = cache$String;
        }
    }

    @Override
    public boolean isType(@Nullable Object object) {
        return TAG_TYPE.isInstance(object);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object build(@NotNull TagType<?> type, @Nullable Object object) {
        try {
            if (CACHE_COMPATIBLE) {
                switch (type.id()) {
                    case Tag.END:
                        return NEW_END.invoke();
                    case Tag.BYTE:
                        if (object instanceof Boolean) {
                            return NEW_BOOLEAN.invoke(object);
                        }
                        return NEW_BYTE.invoke(object);
                    case Tag.SHORT:
                        return NEW_SHORT.invoke(object);
                    case Tag.INT:
                        return NEW_INT.invoke(object);
                    case Tag.LONG:
                        return NEW_LONG.invoke(object);
                    case Tag.FLOAT:
                        return NEW_FLOAT.invoke(object);
                    case Tag.DOUBLE:
                        return NEW_DOUBLE.invoke(object);
                    case Tag.BYTE_ARRAY:
                        return NEW_BYTE_ARRAY.invoke(object);
                    case Tag.STRING:
                        return NEW_STRING.invoke(object);
                    case Tag.LIST:
                        return NEW_LIST.invoke(object, typeId((Iterable<Object>) object));
                    case Tag.COMPOUND:
                        return NEW_COMPOUND.invoke(object);
                    case Tag.INT_ARRAY:
                        return NEW_INT_ARRAY.invoke(object);
                    case Tag.LONG_ARRAY:
                        return NEW_LONG_ARRAY.invoke(object);
                    default:
                        throw new IllegalArgumentException("Invalid tag type: " + type.name());
                }
            } else {
                switch (type.id()) {
                    case Tag.END:
                        return CACHE_END;
                    case Tag.BYTE:
                        if (object instanceof Boolean) {
                            return (Boolean) object ? CACHE_TRUE : CACHE_FALSE;
                        }
                        // Induced overflow
                        return CACHE_BYTE[128 + (byte) object];
                    case Tag.SHORT:
                        if (((short) object) <= HIGH && ((short) object) >= LOW) {
                            return CACHE_SHORT[((short) object) - LOW];
                        }
                        return NEW_SHORT.invoke(object);
                    case Tag.INT:
                        if (((int) object) <= HIGH && ((int) object) >= LOW) {
                            return CACHE_INT[((int) object) - LOW];
                        }
                        return NEW_INT.invoke(object);
                    case Tag.LONG:
                        if (((long) object) <= HIGH && ((long) object) >= LOW) {
                            return CACHE_LONG[((int) object) - LOW];
                        }
                        return NEW_LONG.invoke(object);
                    case Tag.FLOAT:
                        if (((float) object) == 0.0f) {
                            return CACHE_FLOAT;
                        }
                        return NEW_FLOAT.invoke(object);
                    case Tag.DOUBLE:
                        if (((double) object) == 0.0d) {
                            return CACHE_DOUBLE;
                        }
                        return NEW_DOUBLE.invoke(object);
                    case Tag.BYTE_ARRAY:
                        return NEW_BYTE_ARRAY.invoke(object);
                    case Tag.STRING:
                        if (((String) object).isEmpty()) {
                            return CACHE_STRING;
                        }
                        return NEW_STRING.invoke(object);
                    case Tag.LIST:
                        final Object list = NEW_LIST.invoke();
                        final List<Object> value = (List<Object>) GET_LIST.invoke(list);
                        value.addAll((Collection<Object>) object);
                        SET_LIST_TYPE.invoke(list, typeId((Iterable<Object>) object));
                        return list;
                    case Tag.COMPOUND:
                        final Object compound = NEW_COMPOUND.invoke();
                        final Map<String, Object> map = (Map<String, Object>) GET_COMPOUND.invoke(compound);
                        map.putAll((Map<String, Object>) object);
                        return compound;
                    case Tag.INT_ARRAY:
                        return NEW_INT_ARRAY.invoke(object);
                    case Tag.LONG_ARRAY:
                        return NEW_LONG_ARRAY.invoke(object);
                    default:
                        throw new IllegalArgumentException("Invalid tag type: " + type.name());
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public Object parse(@NotNull List<?> list) {
        if (CACHE_COMPATIBLE && !list.isEmpty() && isType(list.get(0))) {
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
    public Object parse(@NotNull Map<String, ?> map) {
        if (CACHE_COMPATIBLE && !map.isEmpty() && isType(map.values().iterator().next())) {
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
    public Object extract(@Nullable Object object) {
        if (object == null) {
            return null;
        }
        try {
            switch ((byte) TAG_ID.invoke(object)) {
                case Tag.END:
                    return null;
                case Tag.BYTE:
                    return GET_BYTE.invoke(object);
                case Tag.SHORT:
                    return GET_SHORT.invoke(object);
                case Tag.INT:
                    return GET_INT.invoke(object);
                case Tag.LONG:
                    return GET_LONG.invoke(object);
                case Tag.FLOAT:
                    return GET_FLOAT.invoke(object);
                case Tag.DOUBLE:
                    return GET_DOUBLE.invoke(object);
                case Tag.BYTE_ARRAY:
                    return GET_BYTE_ARRAY.invoke(object);
                case Tag.STRING:
                    return GET_STRING.invoke(object);
                case Tag.LIST:
                    return GET_LIST.invoke(object);
                case Tag.COMPOUND:
                    return GET_COMPOUND.invoke(object);
                case Tag.INT_ARRAY:
                    return GET_INT_ARRAY.invoke(object);
                case Tag.LONG_ARRAY:
                    return GET_LONG_ARRAY.invoke(object);
                default:
                    throw new IllegalArgumentException("Invalid tag type: " + object);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public int size(@Nullable Object object) {
        if (TAG_SIZE != null) {
            try {
                return (int) TAG_SIZE.invoke(object);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        } else {
            return TagMapper.super.size(object);
        }
    }

    @Override
    public @NotNull <A> TagType<A> type(@Nullable Object object) {
        return TagType.getType(typeId(object));
    }

    @Override
    public byte typeId(@Nullable Object object) {
        if (object == null) {
            return Tag.END;
        }
        try {
            return (byte) TAG_ID.invoke(object);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public @NotNull <A> TagType<A> listType(@NotNull Object object) {
        return TagType.getType(listTypeId(object));
    }

    @Override
    public byte listTypeId(@NotNull Object object) {
        try {
            return (byte) GET_LIST_TYPE.invoke(object);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
