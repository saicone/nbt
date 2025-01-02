package com.saicone.nbt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TagObjects {

    protected static final Map<String, Object> MAP = new LinkedHashMap<>();

    static {
        MAP.put("byte", (byte) 1);
        MAP.put("short", (short) 2);
        MAP.put("int", (int) 3);
        MAP.put("long", (long) 4);
        MAP.put("float", (float) 5f);
        MAP.put("double", (double) 6d);
        MAP.put("byte array", new byte[] { 1, 2, 3, 4 });
        MAP.put("string", "test123");
        MAP.put("integer list", List.of(1, 2, 3, 4));
        MAP.put("array list", List.of(new int[] { 1 }, new int[] { 2 }, new int[] { 3 }, new int[] { 4 }));
        MAP.put("list list", List.of(List.of("1"), List.of("2"), List.of("3"), List.of("4")));
        MAP.put("compound list", List.of(Map.of("test", "asd"), Map.of("number", 1234), Map.of("list", List.of((short) 1234))));
        MAP.put("compound", Map.of("test", Map.of("list", List.of((short) 1234))));
        MAP.put("int array", new int[] { 1, 2, 3, 4 });
        MAP.put("long array", new long[] { 1, 2, 3, 4 });
    }
}
