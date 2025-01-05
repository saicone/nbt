package com.saicone.nbt.util;

import com.saicone.nbt.Tag;
import com.saicone.nbt.TagMapper;
import com.saicone.nbt.TagType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Utility class to colorize tag objects with provided or custom palettes.
 *
 * @author Rubenicos
 */
public class TagPalette {

    /**
     * Tag palette that provide color names.
     */
    public static final TagPalette COLORS = new TagPalette() {
        @Override
        public @NotNull <T> String color(@Nullable T object, @Nullable String indent, @NotNull TagMapper<T> mapper) {
            throw new IllegalStateException("The current tag palette instance is only to provide color names");
        }
    }
            .base("white")
            .key("aqua")
            .type("gold")
            .type(TagType.STRING, "green")
            .type(TagType.BOOLEAN, "red")
            .type(TagType.END, "dark_red")
            .suffix("red")
            .suffix(TagType.BOOLEAN, "")
            .end("reset");

    /**
     * Tag palette with default legacy color codes.
     */
    public static final TagPalette DEFAULT = new TagPalette()
            .base("\u00a7f%s")
            .key("\u00a7b%s")
            .type("\u00a76%s")
            .type(TagType.STRING, "\u00a7a%s")
            .type(TagType.BOOLEAN, "\u00a7c%s")
            .type(TagType.END, "\u00a74%s")
            .suffix("\u00a7c%s")
            .suffix(TagType.BOOLEAN, "")
            .end("\u00a7r");
    /**
     * Tag palette with raw json color formatting.
     */
    public static final TagPalette JSON = new TagPalette() {
        @Override
        public @NotNull <T> String color(@Nullable T object, @Nullable String indent, @NotNull TagMapper<T> mapper) {
            final String result = super.color(object, indent, mapper);
            if (result.isBlank()) {
                return "{}";
            }
            return "[" + result.substring(0, result.length() - 1) + "]";
        }
    }
            .base("{\"type\":\"text\",\"color\":\"white\",\"text\":\"%s\"},")
            .quote("{\"type\":\"text\",\"color\":\"white\",\"text\":\"\\%s\"},")
            .key("{\"type\":\"text\",\"color\":\"aqua\",\"text\":\"%s\"},")
            .type("{\"type\":\"text\",\"color\":\"gold\",\"text\":\"%s\"},")
            .type(TagType.STRING, "{\"type\":\"text\",\"color\":\"green\",\"text\":\"%s\"},")
            .type(TagType.BOOLEAN, "{\"type\":\"text\",\"color\":\"red\",\"text\":\"%s\"},")
            .type(TagType.END, "{\"type\":\"text\",\"color\":\"dark_red\",\"text\":\"%s\"},")
            .suffix("{\"type\":\"text\",\"color\":\"red\",\"text\":\"%s\"},")
            .suffix(TagType.BOOLEAN, "");
    /**
     * Tag palette with ANSI console color formatting.
     */
    public static final TagPalette ANSI = new TagPalette()
            .base("\u001B[0m%s")
            .key("\u001B[96m%s")
            .type("\u001B[33m%s")
            .type(TagType.STRING, "\u001B[92m%s")
            .type(TagType.BOOLEAN, "\u001B[91m%s")
            .type(TagType.END, "\u001B[31m%s")
            .suffix("\u001B[91m%s")
            .suffix(TagType.BOOLEAN, "")
            .end("\u001B[0m");
    /**
     * Tag palette with MiniMessage color formatting.
     */
    public static final TagPalette MINI_MESSAGE = new TagPalette()
            .base("<white>%s")
            .key("<aqua>%s")
            .type("<gold>%s")
            .type(TagType.STRING, "<green>%s")
            .type(TagType.BOOLEAN, "<red>%s")
            .type(TagType.END, "<dark_red>%s")
            .suffix("<red>%s")
            .suffix(TagType.BOOLEAN, "")
            .end("<reset>");

    private String base = "";
    private String bracket;
    private String colon;
    private String comma;
    private String quote;
    private String key;

    private String defaultType = "";
    private final Map<TagType<?>, String> types = new HashMap<>();

    private String defaultSuffix = "";
    private final Map<TagType<?>, String> suffixes = new HashMap<>();

    private String end = "";

    /**
     * Constructs a tag palette.
     */
    public TagPalette() {
    }

    /**
     * Get base color format for {@code {}} and any other part that doesn't have a defined color.
     *
     * @return a string to format.
     */
    @NotNull
    public String base() {
        return base;
    }

    /**
     * Get color format for {@code []}.
     *
     * @return a string to format.
     */
    @NotNull
    public String bracket() {
        return bracket != null ? bracket : base;
    }
    /**
     * Get color format for {@code :}.
     *
     * @return a string to format.
     */
    @NotNull
    public String colon() {
        return colon != null ? colon : base;
    }

    /**
     * Get color format for {@code ,}.
     *
     * @return a string to format.
     */
    @NotNull
    public String comma() {
        return comma != null ? comma : base;
    }

    /**
     * Get color format for {@code "}.
     *
     * @return a string to format.
     */
    @NotNull
    public String quote() {
        return quote != null ? quote : base;
    }

    /**
     * Get color format for compound keys.
     *
     * @return a string to format.
     */
    @NotNull
    public String key() {
        return key != null ? key : base;
    }

    /**
     * Get base color format for provided tag type.
     *
     * @param type the type to get format.
     * @return     a string to format.
     */
    @NotNull
    public String type(@NotNull TagType<?> type) {
        return types.getOrDefault(type, defaultType);
    }

    /**
     * Get suffix color format for provided tag.
     *
     * @param type the type to get format.
     * @return     a string to format.
     */
    @NotNull
    public String suffix(@NotNull TagType<?> type) {
        return suffixes.getOrDefault(type, defaultSuffix);
    }

    /**
     * Get end string to append.
     *
     * @return a string.
     */
    @NotNull
    public String end() {
        return end;
    }

    /**
     * Set base color format for {@code {}} and any other part that doesn't have a defined color.
     *
     * @param base a string to format.
     * @return     the current tag palette.
     */
    @NotNull
    @Contract("_ -> this")
    public TagPalette base(@NotNull String base) {
        this.base = base;
        return this;
    }

    /**
     * Set color format for {@code []}.
     *
     * @param bracket a string to format.
     * @return        the current tag palette.
     */
    @NotNull
    @Contract("_ -> this")
    public TagPalette bracket(@Nullable String bracket) {
        this.bracket = bracket;
        return this;
    }

    /**
     * Set color format for {@code :}.
     *
     * @param colon a string to format.
     * @return      the current tag palette.
     */
    @NotNull
    @Contract("_ -> this")
    public TagPalette colon(@Nullable String colon) {
        this.colon = colon;
        return this;
    }

    /**
     * Set color format for {@code ,}.
     *
     * @param comma a string to format.
     * @return      the current tag palette.
     */
    @NotNull
    @Contract("_ -> this")
    public TagPalette comma(@Nullable String comma) {
        this.comma = comma;
        return this;
    }

    /**
     * Set color format for {@code "}.
     *
     * @param quote a string to format.
     * @return      the current tag palette.
     */
    @NotNull
    @Contract("_ -> this")
    public TagPalette quote(@Nullable String quote) {
        this.quote = quote;
        return this;
    }

    /**
     * Set color format for compound keys.
     *
     * @param key a string to format.
     * @return    the current tag palette.
     */
    @NotNull
    @Contract("_ -> this")
    public TagPalette key(@Nullable String key) {
        this.key = key;
        return this;
    }

    /**
     * Set default color format for tag types.
     *
     * @param defaultType a string to format.
     * @return            the current tag palette.
     */
    @NotNull
    @Contract("_ -> this")
    public TagPalette type(@Nullable String defaultType) {
        this.defaultType = defaultType;
        return this;
    }

    /**
     * Set color format for defined tag type.
     *
     * @param type  the type to associate with format.
     * @param color a string to format.
     * @return      the current tag palette.
     */
    @NotNull
    @Contract("_, _ -> this")
    public TagPalette type(@NotNull TagType<?> type, @Nullable String color) {
        if (color == null) {
            this.types.remove(type);
        } else {
            this.types.put(type, color);
        }
        return this;
    }
    /**
     * Set default color format for tag suffixes.
     *
     * @param defaultSuffix a string to format.
     * @return              the current tag palette.
     */
    @NotNull
    @Contract("_ -> this")
    public TagPalette suffix(@Nullable String defaultSuffix) {
        this.defaultSuffix = defaultSuffix;
        return this;
    }

    /**
     * Set color format for defined tag suffix.
     *
     * @param type  the type to associate with format.
     * @param color a string to format.
     * @return      the current tag palette.
     */
    @NotNull
    @Contract("_, _ -> this")
    public TagPalette suffix(@NotNull TagType<?> type, @Nullable String color) {
        if (color == null) {
            this.suffixes.remove(type);
        } else {
            this.suffixes.put(type, color);
        }
        return this;
    }

    /**
     * Set end string to append.
     *
     * @param end a string to format.
     * @return    the current tag palette.
     */
    @NotNull
    @Contract("_ -> this")
    public TagPalette end(@Nullable String end) {
        this.end = Objects.requireNonNullElse(end, "");
        return this;
    }

    /**
     * Copy the current tag palette.
     *
     * @return a newly generated tag palette.
     */
    @NotNull
    @Contract("-> new")
    public TagPalette copy() {
        final TagPalette palette = new TagPalette();
        palette.base = this.base;
        palette.bracket = this.bracket;
        palette.colon = this.colon;
        palette.comma = this.comma;
        palette.quote = this.quote;
        palette.key = this.key;
        palette.defaultType = this.defaultType;
        palette.types.putAll(this.types);
        palette.defaultSuffix = this.defaultSuffix;
        palette.suffixes.putAll(this.suffixes);
        palette.end = this.end;
        return palette;
    }

    /**
     * Color nbt-represented java object with provided indent and {@link TagMapper}.
     *
     * @param object the nbt-represented java object to color.
     * @param indent the indent for new lines.
     * @return       a colored tag object using the current tag palette.
     */
    @NotNull
    public String color(@Nullable Object object, @Nullable String indent) {
        return color(object, indent, TagMapper.DEFAULT);
    }

    /**
     * Color tag object with provided indent and {@link TagMapper}.
     *
     * @param object the tag object to color.
     * @param indent the indent for new lines.
     * @param mapper the mapper to extract value from tag.
     * @return       a colored tag object using the current tag palette.
     * @param <T>    the tag object implementation.
     */
    @NotNull
    public <T> String color(@Nullable T object, @Nullable String indent, @NotNull TagMapper<T> mapper) {
        return color(object, indent == null ? "" : indent, mapper, 0) + end();
    }

    /**
     * Color tag object with provided indent and {@link TagMapper}.
     *
     * @param object the tag object to color.
     * @param indent the indent for new lines.
     * @param mapper the mapper to extract value from tag.
     * @param count  the current tag depth count.
     * @return       a colored tag object using the current tag palette.
     * @param <T>    the tag object implementation.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    protected <T> String color(@Nullable T object, @NotNull String indent, @NotNull TagMapper<T> mapper, int count) {
        final TagType<?> type = mapper.type(object);
        if (!type.isValid()) {
            throw new IllegalArgumentException("Invalid tag type: " + type.name());
        }

        final Object value = mapper.extract(object);
        switch (type.id()) {
            case Tag.END:
                // null
                return String.format(type(TagType.END), "null");
            case Tag.INT:
                // <value>
                return String.format(type(type), String.valueOf(value));
            case Tag.BYTE:
            case Tag.SHORT:
            case Tag.LONG:
            case Tag.FLOAT:
            case Tag.DOUBLE:
                // <value><suffix>
                return String.format(type(type) + suffix(type), String.valueOf(value), String.valueOf(type.suffix()));
            case Tag.STRING:
                // "<value>"
                return String.format(quote() + type(TagType.STRING) + quote(), "\"", value, "\"");
            case Tag.BYTE_ARRAY:
            case Tag.INT_ARRAY:
            case Tag.LONG_ARRAY:
                // [<suffix>; <value><suffix>, <value><suffix>, <value><suffix>...]
                return colorArray(type, value);
            case Tag.LIST:
                // [<pretty value>, <pretty value>, <pretty value>...]
                return colorList((List<T>) value, indent, mapper, count);
            case Tag.COMPOUND:
                // {<key>: <pretty value>, <key>: <pretty value>, <key>: <pretty value>...}
                return colorCompound((Map<String, T>) value, indent, mapper, count);
            default:
                throw new IllegalArgumentException("Invalid tag type: " + type.name());
        }
    }

    /**
     * Color tag array with provided type.
     *
     * @param type  the type of array.
     * @param array the array to color.
     * @return      a colored array using the current tag palette.
     */
    @NotNull
    protected String colorArray(@NotNull TagType<?> type, @NotNull Object array) {
        final StringJoiner joiner = new StringJoiner(String.format(comma(), ", "), String.format(bracket() + suffix(type) + comma(), "[", String.valueOf(type.suffix()), "; "), String.format(bracket(), "]"));
        final String format;
        if (type == TagType.INT_ARRAY) {
            format = type(type);
        } else {
            format = type(type) + suffix(type);
        }
        final int size = Array.getLength(array);
        for (int i = 0; i < size; i++) {
            final Object value = Array.get(array, i);
            joiner.add(String.format(format, value, String.valueOf(type.suffix())));
        }
        return joiner.toString();
    }

    /**
     * Color tag list with provided indent and {@link TagMapper}.
     *
     * @param list   the list to color.
     * @param indent the indent for new lines.
     * @param mapper the mapper to extract value from tag.
     * @param count  the current tag depth count.
     * @return       a colored tag list using the current tag palette.
     * @param <T>    the tag object implementation.
     */
    @NotNull
    protected <T> String colorList(@NotNull List<T> list, @NotNull String indent, @NotNull TagMapper<T> mapper, int count) {
        if (list.isEmpty()) {
            return String.format(bracket(), "[]");
        }

        final StringJoiner joiner;
        if (indent.isEmpty() || (list.size() <= 8 && mapper.type(list.get(0)).isPrimitive())) {
            joiner = new StringJoiner(
                    String.format(comma(), ", "),
                    String.format(bracket(), "["),
                    String.format(bracket(), "]"));
        } else {
            final String s = indent.repeat(count + 1);
            joiner = new StringJoiner(
                    String.format(comma(), ",\n" + s),
                    String.format(bracket(), "[\n" + s),
                    String.format(bracket(), "\n" + indent.repeat(count) + "]"));
        }

        for (T t : list) {
            joiner.add(color(t, indent, mapper, count + 1));
        }
        return joiner.toString();
    }

    /**
     * Color tag compound with provided indent and {@link TagMapper}.
     *
     * @param map    the compound to color.
     * @param indent the indent for new lines.
     * @param mapper the mapper to extract value from tag.
     * @param count  the current tag depth count.
     * @return       a colored tag compound using the current tag palette.
     * @param <T>    the tag object implementation.
     */
    @NotNull
    protected <T> String colorCompound(@NotNull Map<String, T> map, @NotNull String indent, @NotNull TagMapper<T> mapper, int count) {
        if (map.isEmpty()) {
            return String.format(base(), "{}");
        }

        final StringJoiner joiner;
        if (indent.isEmpty()) {
            joiner = new StringJoiner(
                    String.format(comma(), ", "),
                    String.format(base(), "{"),
                    String.format(base(), "}"));
        } else {
            final String s = indent.repeat(count + 1);
            joiner = new StringJoiner(
                    String.format(comma(), ",\n" + s),
                    String.format(base(), "{\n" + s),
                    String.format(base(), "\n" + indent.repeat(count) + "}"));
        }

        for (var entry : map.entrySet()) {
            joiner.add(String.format(key() + colon(), entry.getKey(), ": ") + color(entry.getValue(), indent, mapper, count + 1));
        }
        return joiner.toString();
    }
}
