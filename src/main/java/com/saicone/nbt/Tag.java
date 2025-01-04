package com.saicone.nbt;

/**
 * Constant class that holds common tag values.
 *
 * @author Rubenicos
 */
public class Tag {

    /**
     * Unknown tag ID, used when any tag representation doesn't belong to a real tag.
     */
    public static final byte UNKNOWN = -1;
    /**
     * End tag ID, a tag that represent the end of stream or a null value.
     */
    public static final byte END = 0;
    /**
     * Byte tag ID, also used for Boolean objects.
     */
    public static final byte BYTE = 1;
    /**
     * Short tag ID
     */
    public static final byte SHORT = 2;
    /**
     * Integer tag ID.
     */
    public static final byte INT = 3;
    /**
     * Long tag ID.
     */
    public static final byte LONG = 4;
    /**
     * Float tag ID.
     */
    public static final byte FLOAT = 5;
    /**
     * Double tag ID.
     */
    public static final byte DOUBLE = 6;
    /**
     * Byte array tag ID, also used for Boolean array objects.
     */
    public static final byte BYTE_ARRAY = 7;
    /**
     * String tag ID.
     */
    public static final byte STRING = 8;
    /**
     * List tag ID.
     */
    public static final byte LIST = 9;
    /**
     * Compound tag ID, also a Map with String keys and tag objects.
     */
    public static final byte COMPOUND = 10;
    /**
     * Integer array tag ID.
     */
    public static final byte INT_ARRAY = 11;
    /**
     * Long array tag ID.
     */
    public static final byte LONG_ARRAY = 12;

    /**
     * Default max stack depth for nested values.
     */
    public static final int MAX_STACK_DEPTH = 512;
    /**
     * Default max NBT quota, around 2MB of data.
     */
    public static final int DEFAULT_NBT_QUOTA  = 2 * 1024 * 1024; // 2MB
    /**
     * Default bedrock file header version.
     */
    public static final int DEFAULT_BEDROCK_VERSION = 8;

    /**
     * Map key size in bytes.
     */
    public static final int MAP_KEY_SIZE = 28;
    /**
     * Map entry pair size in bytes.
     */
    public static final int MAP_ENTRY_SIZE = 32;

    Tag() {
    }
}
