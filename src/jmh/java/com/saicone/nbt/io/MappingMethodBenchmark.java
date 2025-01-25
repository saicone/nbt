package com.saicone.nbt.io;

import com.saicone.nbt.Tag;
import com.saicone.nbt.TagType;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class MappingMethodBenchmark {

    private static final Map<Byte, Function<Object, Wrapped>> BUILD_ID = new HashMap<>();
    private static final Map<Class<?>, Function<Object, Wrapped>> BUILD_TYPE = new HashMap<>();

    private static final Map<Byte, Function<Wrapped, Object>> EXTRACT_ID = new HashMap<>();
    private static final Map<Class<?>, Function<Wrapped, Object>> EXTRACT_TYPE = new HashMap<>();

    private static final byte BYTE = (byte) 123;
    private static final short SHORT = (short) 123;
    private static final int INT = 123;
    private static final long LONG = 123L;
    private static final float FLOAT = 123.4f;
    private static final double DOUBLE = 123.4d;
    private static final byte[] BYTE_ARRAY = new byte[] { (byte) 1, (byte) 2, (byte) 3, (byte) 4};
    private static final String STRING = "123";
    private static final List<Wrapped> LIST = List.of(new WrappedInt(1), new WrappedInt(2), new WrappedInt(3), new WrappedInt(4));
    private static final Map<String, Wrapped> MAP = Map.of("key", new WrappedString("value"));
    private static final int[] INT_ARRAY = new int[] { 1, 2, 3, 4};
    private static final long[] LONG_ARRAY = new long[] { 1, 2, 3, 4};

    private static final WrappedByte WRAPPED_BYTE = new WrappedByte(BYTE);
    private static final WrappedShort WRAPPED_SHORT = new WrappedShort(SHORT);
    private static final WrappedInt WRAPPED_INT = new WrappedInt(INT);
    private static final WrappedLong WRAPPED_LONG = new WrappedLong(LONG);
    private static final WrappedFloat WRAPPED_FLOAT = new WrappedFloat(FLOAT);
    private static final WrappedDouble WRAPPED_DOUBLE = new WrappedDouble(DOUBLE);
    private static final WrappedByteArray WRAPPED_BYTE_ARRAY = new WrappedByteArray(BYTE_ARRAY);
    private static final WrappedString WRAPPED_STRING = new WrappedString(STRING);
    private static final WrappedList WRAPPED_LIST = new WrappedList(LIST);
    private static final WrappedMap WRAPPED_MAP = new WrappedMap(MAP);
    private static final WrappedIntArray WRAPPED_INT_ARRAY = new WrappedIntArray(INT_ARRAY);
    private static final WrappedLongArray WRAPPED_LONG_ARRAY = new WrappedLongArray(LONG_ARRAY);

    static {
        build(0, void.class, o -> WrappedEmpty.INSTANCE);
        build(1, Byte.class, WrappedByte::new);
        build(2, Short.class, WrappedShort::new);
        build(3, Integer.class, WrappedInt::new);
        build(4, Long.class, WrappedLong::new);
        build(5, Float.class, WrappedFloat::new);
        build(6, Double.class, WrappedDouble::new);
        build(7, byte[].class, WrappedByteArray::new);
        build(8, String.class, WrappedString::new);
        build(9, List.class, o -> new WrappedList((List<Wrapped>) o));
        build(10, Map.class, o -> new WrappedMap((Map<String, Wrapped>) o));
        build(11, int[].class, WrappedIntArray::new);
        build(12, long[].class, WrappedLongArray::new);

        extract(0, WrappedEmpty.class, (wrapped) -> null);
        extract(1, WrappedByte.class, WrappedByte::getValue);
        extract(2, WrappedShort.class, WrappedShort::getValue);
        extract(3, WrappedInt.class, WrappedInt::getValue);
        extract(4, WrappedLong.class, WrappedLong::getValue);
        extract(5, WrappedFloat.class, WrappedFloat::getValue);
        extract(6, WrappedDouble.class, WrappedDouble::getValue);
        extract(7, WrappedByteArray.class, WrappedByteArray::getValue);
        extract(8, WrappedString.class, WrappedString::getValue);
        extract(9, WrappedList.class, WrappedList::getList);
        extract(10, WrappedMap.class, WrappedMap::getMap);
        extract(11, WrappedIntArray.class, WrappedIntArray::getValue);
        extract(12, WrappedLongArray.class, WrappedLongArray::getValue);
    }

    @SuppressWarnings("unchecked")
    private static <T> void build(int id, Class<T> type, Function<T, Wrapped> function) {
        BUILD_ID.put((byte) id, (Function<Object, Wrapped>) function);
        BUILD_TYPE.put(type, (Function<Object, Wrapped>) function);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Wrapped> void extract(int id, Class<T> type, Function<T, Object> function) {
        EXTRACT_ID.put((byte) id, (Function<Wrapped, Object>) function);
        EXTRACT_TYPE.put(type, (Function<Wrapped, Object>) function);
    }

    @Benchmark
    public void buildSwitch(Blackhole bh) {
        bh.consume(buildSwitch(TagType.BYTE, BYTE));
        bh.consume(buildSwitch(TagType.SHORT, SHORT));
        bh.consume(buildSwitch(TagType.INT, INT));
        bh.consume(buildSwitch(TagType.LONG, LONG));
        bh.consume(buildSwitch(TagType.FLOAT, FLOAT));
        bh.consume(buildSwitch(TagType.DOUBLE, DOUBLE));
        bh.consume(buildSwitch(TagType.BYTE_ARRAY, BYTE_ARRAY));
        bh.consume(buildSwitch(TagType.STRING, STRING));
        bh.consume(buildSwitch(TagType.LIST, LIST));
        bh.consume(buildSwitch(TagType.COMPOUND, MAP));
        bh.consume(buildSwitch(TagType.INT_ARRAY, INT_ARRAY));
        bh.consume(buildSwitch(TagType.LONG_ARRAY, LONG_ARRAY));
    }

    public Wrapped buildSwitch(TagType<?> type, Object object) {
        switch (type.id()) {
            case Tag.END:
                return WrappedEmpty.INSTANCE;
            case Tag.BYTE:
                return new WrappedByte((byte) object);
            case Tag.SHORT:
                return new WrappedShort((short) object);
            case Tag.INT:
                return new WrappedInt((int) object);
            case Tag.LONG:
                return new WrappedLong((long) object);
            case Tag.FLOAT:
                return new WrappedFloat((float) object);
            case Tag.DOUBLE:
                return new WrappedDouble((double) object);
            case Tag.BYTE_ARRAY:
                return new WrappedByteArray((byte[]) object);
            case Tag.STRING:
                return new WrappedString((String) object);
            case Tag.LIST:
                return new WrappedList((List<Wrapped>) object);
            case Tag.COMPOUND:
                return new WrappedMap((Map<String, Wrapped>) object);
            case Tag.INT_ARRAY:
                return new WrappedIntArray((int[]) object);
            case Tag.LONG_ARRAY:
                return new WrappedLongArray((long[]) object);
            default:
                throw new IllegalArgumentException("Invalid tag type: " + type.name());
        }
    }

    @Benchmark
    public void buildId(Blackhole bh) {
        bh.consume(buildId(TagType.BYTE, BYTE));
        bh.consume(buildId(TagType.SHORT, SHORT));
        bh.consume(buildId(TagType.INT, INT));
        bh.consume(buildId(TagType.LONG, LONG));
        bh.consume(buildId(TagType.FLOAT, FLOAT));
        bh.consume(buildId(TagType.DOUBLE, DOUBLE));
        bh.consume(buildId(TagType.BYTE_ARRAY, BYTE_ARRAY));
        bh.consume(buildId(TagType.STRING, STRING));
        bh.consume(buildId(TagType.LIST, LIST));
        bh.consume(buildId(TagType.COMPOUND, MAP));
        bh.consume(buildId(TagType.INT_ARRAY, INT_ARRAY));
        bh.consume(buildId(TagType.LONG_ARRAY, LONG_ARRAY));
    }

    public Wrapped buildId(TagType<?> type, Object object) {
        return BUILD_ID.get(type.id()).apply(object);
    }

    @Benchmark
    public void buildType(Blackhole bh) {
        bh.consume(buildType(BYTE));
        bh.consume(buildType(SHORT));
        bh.consume(buildType(INT));
        bh.consume(buildType(LONG));
        bh.consume(buildType(FLOAT));
        bh.consume(buildType(DOUBLE));
        bh.consume(buildType(BYTE_ARRAY));
        bh.consume(buildType(STRING));
        bh.consume(buildType(INT_ARRAY));
        bh.consume(buildType(LONG_ARRAY));
    }

    public Wrapped buildType(Object object) {
        return BUILD_TYPE.get(object.getClass()).apply(object);
    }

    @Benchmark
    public void extractSwitch(Blackhole bh) {
        bh.consume(extractSwitch(WRAPPED_BYTE));
        bh.consume(extractSwitch(WRAPPED_SHORT));
        bh.consume(extractSwitch(WRAPPED_INT));
        bh.consume(extractSwitch(WRAPPED_LONG));
        bh.consume(extractSwitch(WRAPPED_FLOAT));
        bh.consume(extractSwitch(WRAPPED_DOUBLE));
        bh.consume(extractSwitch(WRAPPED_BYTE_ARRAY));
        bh.consume(extractSwitch(WRAPPED_STRING));
        bh.consume(extractSwitch(WRAPPED_LIST));
        bh.consume(extractSwitch(WRAPPED_MAP));
        bh.consume(extractSwitch(WRAPPED_INT_ARRAY));
        bh.consume(extractSwitch(WRAPPED_LONG_ARRAY));
    }

    public Object extractSwitch(Wrapped wrapped) {
        switch (wrapped.getId()) {
            case Tag.END:
                return null;
            case Tag.BYTE:
                return ((WrappedByte) wrapped).getValue();
            case Tag.SHORT:
                return ((WrappedShort) wrapped).getValue();
            case Tag.INT:
                return ((WrappedInt) wrapped).getValue();
            case Tag.LONG:
                return ((WrappedLong) wrapped).getValue();
            case Tag.FLOAT:
                return ((WrappedFloat) wrapped).getValue();
            case Tag.DOUBLE:
                return ((WrappedDouble) wrapped).getValue();
            case Tag.BYTE_ARRAY:
                return ((WrappedByteArray) wrapped).getValue();
            case Tag.STRING:
                return ((WrappedString) wrapped).getValue();
            case Tag.LIST:
                return ((WrappedList) wrapped).getList();
            case Tag.COMPOUND:
                return ((WrappedMap) wrapped).getMap();
            case Tag.INT_ARRAY:
                return ((WrappedIntArray) wrapped).getValue();
            case Tag.LONG_ARRAY:
                return ((WrappedLongArray) wrapped).getValue();
            default:
                throw new IllegalArgumentException("Invalid tag type: " + wrapped);
        }
    }

    @Benchmark
    public void extractId(Blackhole bh) {
        bh.consume(extractId(WRAPPED_BYTE));
        bh.consume(extractId(WRAPPED_SHORT));
        bh.consume(extractId(WRAPPED_INT));
        bh.consume(extractId(WRAPPED_LONG));
        bh.consume(extractId(WRAPPED_FLOAT));
        bh.consume(extractId(WRAPPED_DOUBLE));
        bh.consume(extractId(WRAPPED_BYTE_ARRAY));
        bh.consume(extractId(WRAPPED_STRING));
        bh.consume(extractId(WRAPPED_LIST));
        bh.consume(extractId(WRAPPED_MAP));
        bh.consume(extractId(WRAPPED_INT_ARRAY));
        bh.consume(extractId(WRAPPED_LONG_ARRAY));
    }

    public Object extractId(Wrapped wrapped) {
        return EXTRACT_ID.get(wrapped.getId()).apply(wrapped);
    }

    @Benchmark
    public void extractType(Blackhole bh) {
        bh.consume(extractType(WRAPPED_BYTE));
        bh.consume(extractType(WRAPPED_SHORT));
        bh.consume(extractType(WRAPPED_INT));
        bh.consume(extractType(WRAPPED_LONG));
        bh.consume(extractType(WRAPPED_FLOAT));
        bh.consume(extractType(WRAPPED_DOUBLE));
        bh.consume(extractType(WRAPPED_BYTE_ARRAY));
        bh.consume(extractType(WRAPPED_STRING));
        bh.consume(extractType(WRAPPED_LIST));
        bh.consume(extractType(WRAPPED_MAP));
        bh.consume(extractType(WRAPPED_INT_ARRAY));
        bh.consume(extractType(WRAPPED_LONG_ARRAY));
    }

    public Object extractType(Wrapped wrapped) {
        return EXTRACT_TYPE.get(wrapped.getClass()).apply(wrapped);
    }

    public interface Wrapped {
        byte getId();
    }

    public static class WrappedEmpty implements Wrapped {

        public static final WrappedEmpty INSTANCE = new WrappedEmpty();

        @Override
        public byte getId() {
            return 0;
        }
    }

    public static class WrappedByte implements Wrapped {

        private final byte value;

        public WrappedByte(byte value) {
            this.value = value;
        }

        @Override
        public byte getId() {
            return 1;
        }

        public byte getValue() {
            return value;
        }
    }

    public static class WrappedShort implements Wrapped {

        private final short value;

        public WrappedShort(short value) {
            this.value = value;
        }

        @Override
        public byte getId() {
            return 2;
        }

        public short getValue() {
            return value;
        }
    }

    public static class WrappedInt implements Wrapped {

        private final int value;

        public WrappedInt(int value) {
            this.value = value;
        }

        @Override
        public byte getId() {
            return 3;
        }

        public int getValue() {
            return value;
        }
    }

    public static class WrappedLong implements Wrapped {

        private final long value;

        public WrappedLong(long value) {
            this.value = value;
        }

        @Override
        public byte getId() {
            return 4;
        }

        public long getValue() {
            return value;
        }
    }

    public static class WrappedFloat implements Wrapped {

        private final float value;

        public WrappedFloat(float value) {
            this.value = value;
        }

        @Override
        public byte getId() {
            return 5;
        }

        public float getValue() {
            return value;
        }
    }

    public static class WrappedDouble implements Wrapped {

        private final double value;

        public WrappedDouble(double value) {
            this.value = value;
        }

        @Override
        public byte getId() {
            return 6;
        }

        public double getValue() {
            return value;
        }
    }

    public static class WrappedByteArray implements Wrapped {

        private final byte[] value;

        public WrappedByteArray(byte[] value) {
            this.value = value;
        }

        @Override
        public byte getId() {
            return 7;
        }

        public byte[] getValue() {
            return value;
        }
    }

    public static class WrappedString implements Wrapped {

        private final String value;

        public WrappedString(String value) {
            this.value = value;
        }

        @Override
        public byte getId() {
            return 8;
        }

        public String getValue() {
            return value;
        }
    }

    public static class WrappedList implements Wrapped {

        private final List<Wrapped> list;

        public WrappedList(List<Wrapped> list) {
            this.list = list;
        }

        @Override
        public byte getId() {
            return 9;
        }

        public List<Wrapped> getList() {
            return list;
        }
    }

    public static class WrappedMap implements Wrapped {

        private final Map<String, Wrapped> map;

        public WrappedMap(Map<String, Wrapped> map) {
            this.map = map;
        }

        @Override
        public byte getId() {
            return 10;
        }

        public Map<String, Wrapped> getMap() {
            return map;
        }
    }

    public static class WrappedIntArray implements Wrapped {

        private final int[] value;

        public WrappedIntArray(int[] value) {
            this.value = value;
        }

        @Override
        public byte getId() {
            return 11;
        }

        public int[] getValue() {
            return value;
        }
    }

    public static class WrappedLongArray implements Wrapped {

        private final long[] value;

        public WrappedLongArray(long[] value) {
            this.value = value;
        }

        @Override
        public byte getId() {
            return 12;
        }

        public long[] getValue() {
            return value;
        }
    }
}
