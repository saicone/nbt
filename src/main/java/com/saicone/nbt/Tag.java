package com.saicone.nbt;

public class Tag {

    public static final byte UNKNOWN = -1;
    public static final byte END = 0;
    public static final byte BYTE = 1;
    public static final byte SHORT = 2;
    public static final byte INT = 3;
    public static final byte LONG = 4;
    public static final byte FLOAT = 5;
    public static final byte DOUBLE = 6;
    public static final byte BYTE_ARRAY = 7;
    public static final byte STRING = 8;
    public static final byte LIST = 9;
    public static final byte COMPOUND = 10;
    public static final byte INT_ARRAY = 11;
    public static final byte LONG_ARRAY = 12;

    public static final int MAX_STACK_DEPTH = 512;
    public static final int DEFAULT_NBT_QUOTA  = 2 * 1024 * 1024; // 2MB
    public static final int DEFAULT_BEDROCK_VERSION = 8;

    public static final int MAP_KEY_SIZE = 28;
    public static final int MAP_ENTRY_SIZE = 32;

    Tag() {
    }
}
