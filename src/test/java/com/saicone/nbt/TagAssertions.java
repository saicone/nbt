package com.saicone.nbt;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TagAssertions {

    @SuppressWarnings("unchecked")
    public static void assertTagEquals(Object expected, Object actual) {
        if (expected instanceof byte[]) {
            assertInstanceOf(byte[].class, actual);
            assertArrayEquals((byte[]) expected, (byte[]) actual);
        } else if (expected instanceof boolean[]) {
            assertInstanceOf(boolean[].class, actual);
            assertArrayEquals((boolean[]) expected, (boolean[]) actual);
        } else if (expected instanceof int[]) {
            assertInstanceOf(int[].class, actual);
            assertArrayEquals((int[]) expected, (int[]) actual);
        } else if (expected instanceof long[]) {
            assertInstanceOf(long[].class, actual);
            assertArrayEquals((long[]) expected, (long[]) actual);
        } else if (expected instanceof List) {
            assertInstanceOf(List.class, actual);
            assertTagEquals((List<Object>) expected, (List<Object>) actual);
        } else if (expected instanceof Map) {
            assertInstanceOf(Map.class, actual);
            assertTagEquals((Map<String, Object>) expected, (Map<String, Object>) actual);
        } else {
            assertEquals(expected, actual);
        }
    }

    public static void assertTagEquals(List<Object> expected, List<Object> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertTagEquals(expected.get(i), actual.get(i));
        }
    }

    public static void assertTagEquals(Map<String, Object> expected, Map<String, Object> actual) {
        for (Map.Entry<String, Object> entry : expected.entrySet()) {
            assertTrue(actual.containsKey(entry.getKey()));
            assertTagEquals(entry.getValue(), actual.get(entry.getKey()));
        }
    }

    @SuppressWarnings("unchecked")
    public static void assertTagSimilar(Object expected, Object actual) {
        if (expected instanceof List) {
            if (actual instanceof byte[]) {
                assertTagEquals(TagMapper.DEFAULT.byteArray(expected), actual);
            } else if (actual instanceof boolean[]) {
                assertTagEquals(TagMapper.DEFAULT.booleanArray(expected), actual);
            } else if (actual instanceof int[]) {
                assertTagEquals(TagMapper.DEFAULT.intArray(expected), actual);
            } else if (actual instanceof long[]) {
                assertTagEquals(TagMapper.DEFAULT.longArray(expected), actual);
            } else {
                assertTagEquals((List<Object>) expected, (List<Object>) actual);
            }
        } else {
            assertTagEquals(expected, actual);
        }
    }

    public static void assertTagSimilar(List<Object> expected, List<Object> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertTagSimilar(expected.get(i), actual.get(i));
        }
    }

    public static void assertTagSimilar(Map<String, Object> expected, Map<String, Object> actual) {
        for (Map.Entry<String, Object> entry : expected.entrySet()) {
            assertTrue(actual.containsKey(entry.getKey()));
            assertTagSimilar(entry.getValue(), actual.get(entry.getKey()));
        }
    }
}
