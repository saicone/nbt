package com.saicone.nbt;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.saicone.nbt.util.TagJson;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.saicone.nbt.TagAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

public class TagJsonTest {

    private static final JsonObject JSON = new JsonObject();

    static {
        JSON.add("byte", new JsonPrimitive((byte) 1));
        JSON.add("short", new JsonPrimitive((short) 2));
        JSON.add("int", new JsonPrimitive((int) 3));
        JSON.add("long", new JsonPrimitive((long) 4));
        JSON.add("float", new JsonPrimitive((float) 5f));
        JSON.add("double", new JsonPrimitive((double) 6d));
        JSON.add("byte array", array((byte) 1, (byte) 2, (byte) 3, (byte) 4));
        JSON.add("string", new JsonPrimitive("test123"));
        JSON.add("integer list", array(1, 2, 3, 4));
        JSON.add("array list", array(array(1), array(2), array(3), array(4)));
        JSON.add("list list", array(array("1"), array("2"), array("3"), array("4")));
        JSON.add("compound list", array(object("test", new JsonPrimitive("asd")), object("number", new JsonPrimitive(1234)), object("list", array((short) 1234))));
        JSON.add("compound", object("test", object("list", array((short) 1234))));
        JSON.add("int array", array(1, 2, 3, 4));
        JSON.add("long array", array(1L, 2L, 3L, 4L));
    }

    private static JsonArray array(Number... numbers) {
        final JsonArray array = new JsonArray();
        for (Number number : numbers) {
            array.add(new JsonPrimitive(number));
        }
        return array;
    }

    private static JsonArray array(String... strings) {
        final JsonArray array = new JsonArray();
        for (String s : strings) {
            array.add(new JsonPrimitive(s));
        }
        return array;
    }

    private static JsonArray array(JsonElement... elements) {
        final JsonArray array = new JsonArray();
        for (JsonElement element : elements) {
            array.add(element);
        }
        return array;
    }

    private static JsonObject object(String key, JsonElement element) {
        final JsonObject object = new JsonObject();
        object.add(key, element);
        return object;
    }

    @Test
    public void testWrite() {
        final JsonElement element = TagJson.toJson(TagObjects.MAP);
        assertEquals(JSON, element);
    }

    @Test
    public void testRead() {
        final Map<String, Object> map = TagJson.fromJson(JSON);
        assertTagEquals(TagObjects.MAP, map);
    }
}
