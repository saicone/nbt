package com.saicone.nbt;

import com.saicone.nbt.util.TagConfig;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.saicone.nbt.TagAssertions.*;

public class TagConfigTest {

    protected static final Map<String, Object> CONFIG_MAP = new LinkedHashMap<>();

    static {
        CONFIG_MAP.put("byte", "1b");
        CONFIG_MAP.put("short", "2s");
        CONFIG_MAP.put("int", 3);
        CONFIG_MAP.put("long", "4l");
        CONFIG_MAP.put("float", "5.0f");
        CONFIG_MAP.put("double", "6.0");
        CONFIG_MAP.put("byte array", "[B;1B,2B,3B,4B]");
        CONFIG_MAP.put("string", "test123");
        CONFIG_MAP.put("integer list", List.of(1, 2, 3, 4));
        CONFIG_MAP.put("array list", List.of("[I;1]", "[I;2]", "[I;3]", "[I;4]"));
        CONFIG_MAP.put("list list", List.of(List.of("1"), List.of("2"), List.of("3"), List.of("4")));
        CONFIG_MAP.put("compound list", List.of(Map.of("test", "asd"), Map.of("number", 1234), Map.of("list", List.of("1234s"))));
        CONFIG_MAP.put("compound", Map.of("test", Map.of("list", List.of("1234s"))));
        CONFIG_MAP.put("int array", "[I;1,2,3,4]");
        CONFIG_MAP.put("long array", "[L;1L,2L,3L,4L]");
    }

    @Test
    public void testWrite() {
        final Object map = TagConfig.toConfigValue(TagObjects.MAP);
        assertTagEquals(CONFIG_MAP, map);
    }

    @Test
    public void testRead() {
        final Object map = TagConfig.fromConfigValue(CONFIG_MAP);
        assertTagEquals(TagObjects.MAP, map);
    }
}
