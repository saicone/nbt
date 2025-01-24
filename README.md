<h1 align="center">NBT</h1>

<h4 align="center">Multiplatform Named Binary Tag library with customizable abstraction.</h4>

<p align="center">
    <a href="https://saic.one/discord">
        <img src="https://img.shields.io/discord/974288218839191612.svg?style=flat-square&label=discord&logo=discord&logoColor=white&color=7289da"/>
    </a>
    <a href="https://www.codefactor.io/repository/github/saicone/nbt">
        <img src="https://img.shields.io/codefactor/grade/github/saicone/nbt?style=flat-square&logo=codefactor&logoColor=white&label=codefactor&color=00b16a"/>
    </a>
    <a href="https://github.com/saicone/nbt">
        <img src="https://img.shields.io/github/languages/code-size/saicone/nbt?logo=github&logoColor=white&style=flat-square"/>
    </a>
    <a href="https://jitpack.io/#com.saicone/nbt">
        <img src="https://img.shields.io/github/v/tag/saicone/nbt?style=flat-square&logo=jitpack&logoColor=white&label=JitPack&color=brigthgreen"/>
    </a>
    <a href="https://javadoc.saicone.com/nbt/">
        <img src="https://img.shields.io/badge/JavaDoc-Online-green?style=flat-square"/>
    </a>
</p>

NBT library that provides a full compatibility with Java and Bedrock, also compatible with I/O operations, SNBT read/write, json conversion and simplified configuration format.

```java
// Tag compound (map representation)
Map<String, Object> map = Map.of(
        "someKey", "someValue",
        "otherKey", 1234,
        "aList", List.of(1, 2, 3, 4)
    );


File file = new File("myfile.nbt");

// Write to file
try (TagOutput<Object> output = TagOutput.of(new DataOutputStream(new FileOutputStream(file)))) {
    output.writeUnnamed(map);
}

// Read from file
Map<String, Object> map;
try (TagInput<Object> input = TagInput.of(new DataInputStream(new FileInputStream(file)))) {
    map = input.readUnnamed();
}


// Convert to SNBT
String snbt = TagWriter.toString(map);

// Get from SNBT
Map<String, Object> map = TagReader.fromString(snbt);


// Convert to Json
JsonElement json = TagJson.toJson(map);

// Get from Json
Map<String, Object> map = TagJson.fromJson(json);


// Convert to simplified configuration
Object config = TagConfig.toConfigValue(map);

// Get from simplified configuration
Map<String, Object> map = TagConfig.fromConfigValue(config);
```

## Dependency

How to implement NBT library in your project.

This library contains the following artifacts:

* `nbt` - The main project.
* `mapper-adventure` - NBT mapper for [adventure nbt](https://github.com/KyoriPowered/adventure) objects, can be used for [Velocity](https://github.com/PaperMC/Velocity) and [Minestom](https://github.com/Minestom/Minestom).
* `mapper-bukkit` - NBT mapper for bukkit-based projects ([SpigotMC](https://www.spigotmc.org/), [PaperMC](https://papermc.io/)... etc).
* `mapper-cloudburst` - NBT mapper for [CloudburstMC nbt](https://github.com/CloudburstMC/NBT) objects.
* `mapper-jo-nbt` - NBT mapper for [jo-nbt](https://github.com/llbit/jo-nbt) objects, can be used for [Bungeecord](https://github.com/SpigotMC/BungeeCord).
* `mapper-minecraft` - NBT mapper for Minecraft code with Mojang mappings, can be used for [PaperMC](https://papermc.io/) +1.20.5.

<details>
  <summary>build.gradle</summary>

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.saicone.nbt:nbt:1.0'
}
```

</details>

<details>
  <summary>build.gradle.kts</summary>

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.saicone.nbt:nbt:1.0")
}
```

</details>

<details>
  <summary>pom.xml</summary>

```xml
<repositories>
    <repository>
        <id>Jitpack</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.saicone.nbt</groupId>
        <artifactId>nbt</artifactId>
        <version>1.0</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

</details>

## Tag mappers

A tag mapper is a customizable tag abstraction, for example, most NBT implementations use classes like "TagString" or "NbtCompound" referring the kind of value that is handled.

With a tag mapper this library can build NBT objects as that custom implementation and extract the values from them.

This library by default is compatible with any Java object that is represented on [NBT format](https://minecraft.wiki/w/NBT_format):

0. END = `null`
1. BYTE = `byte`
2. SHORT = `short`
3. INT = `int`
4. LONG = `long`
5. FLOAT = `float`
6. DOUBLE = `double`
7. BYTE_ARRAY = `byte[]`
8. STRING = `String`
9. LIST = `List<Object>` (Objects must be represented on [NBT format](https://minecraft.wiki/w/NBT_format))
10. COMPOUND = `Map<String, Object>` (Objects must be represented on [NBT format](https://minecraft.wiki/w/NBT_format))
11. INT_ARRAY = `int[]`
12. LONG_ARRAY = `long[]`

You can also provide your own mapping by creating a custom implementation of `TagMapper`.

## How to read/write NBT data

Between Minecraft platforms there are important differences, creating a `TagInput` or `TagOutput` is not that "easy" due NBT formatting changes depending on your needs.

You should understand that "input" is when you are reading bytes and "output" is when you are writing or sending them.

We will use the following object as example:
```java
// Tag compound (map representation)
Map<String, Object> map = Map.of(
        "someKey", "someValue",
        "otherKey", 1234,
        "aList", List.of(1, 2, 3, 4)
    );
```

### Java

"Java" will be referred as "Minecraft Java Edition", basically the default way to read/write NBT data on the following locations:
* Files, such as world regions, player data and also used by third-party file formats (.schematic and .schem).
* Network, sending and receiving packets with data like texts and items.

Files (go below for more information about read/write compressed data):
```java
// Using File
OutputStream out = new FileOutputStream(new File("myfile.nbt"));
// Using Path
OutputStream out = Files.newOutputStream(Path.of("myfile.nbt"));

// Write to file
try (TagOutput<Object> output = TagOutput.of(new DataOutputStream(out))) {
    output.writeUnnamed(map);
}


// Using File
InputStream in = new FileInputStream(new File("myfile.nbt"));
// Using Path
InputStream in = Files.newInputStream(Path.of("myfile.nbt"));

// Read from file
Map<String, Object> map;
try (TagInput<Object> input = TagInput.of(new DataInputStream(in))) {
    map = input.readUnnamed();
}
```
Network (Only compatible with +1.20.3 clients):
```java
ByteBuf buffer = ...;

// Write to buffer
try (TagOutput<Object> output = TagOutput.of(new DataOutputStream(new ByteBufOutputStream(buffer)))) {
    output.writeAny(map);
}

// Read from buffer
Map<String, Object> map;
try (TagInput<Object> input = TagInput.of(new DataInputStream(new ByteBufInputStream(buffer)))) {
    map = input.readAny();
}
```
Network (For older clients):
```java
ByteBuf buffer = ...;

// Write to buffer
try (TagOutput<Object> output = TagOutput.of(new DataOutputStream(new ByteBufOutputStream(buffer)))) {
    output.writeUnnamed(map);
}

// Read from buffer
Map<String, Object> map;
try (TagInput<Object> input = TagInput.of(new DataInputStream(new ByteBufInputStream(buffer)))) {
    map = input.readUnnamed();
}
```

### Bedrock

"Bedrock" will be referred as "Minecraft Bedrock Edition" where NBT data is present on the following locations:
* Bedrock files, similar to Java, but the bytes are encoded in "reverse" (little-endian) and every file has a header.
* Bedrock network, similar to Java, but the bytes are encoded in "reverse" and numbers are encoded using VarInt32 and VarInt64 (with ZigZag encoding for signed values).

Bedrock files (go below for more information about read/write compressed data):
```java
// Using File
OutputStream out = new FileOutputStream(new File("myfile.nbt"));
// Using Path
OutputStream out = Files.newOutputStream(Path.of("myfile.nbt"));

// Write to file
try (TagOutput<Object> output = TagOutput.of(new ReverseDataOutputStream(out))) {
    output.writeBedrockFile(map);
}


// Using File
InputStream in = new FileInputStream(new File("myfile.nbt"));
// Using Path
InputStream in = Files.newInputStream(Path.of("myfile.nbt"));

// Read from file
Map<String, Object> map;
try (TagInput<Object> input = TagInput.of(new ReverseDataInputStream(in))) {
    map = input.readBedrockFile();
}
```
Bedrock network:
```java
ByteBuf buffer = ...;

// Write to buffer
try (TagOutput<Object> output = TagOutput.of(new NetworkDataOutputStream(new ByteBufOutputStream(buffer)))) {
    output.writeAny(map);
}

// Read from buffer
Map<String, Object> map;
try (TagInput<Object> input = TagInput.of(new NetworkDataInputStream(new ByteBufInputStream(buffer)))) {
    map = input.readAny();
}
```

### Compressed data

Minecraft (sometimes) use multiple types of compression algorithms:
* gzip, for files.
* zlib, for network packets.
* lz4, for files (since 1.20.5).

So you may think: "How I will ever know what algorithm use the data that I'm reading"

For your luck, this library brings an easy-to-use utility class named `ZipFormat` to detect and create input/output streams for files and paths with a compression algorithm.
```java
File data = ...;
Path data = ...;
OutputStream data = ...;
InputStream data = ...;

// Create compressed output to write (using gzip for example)
OutputStream out = ZipFormat.gzip().newOutputStream(data);

// Detect compression algorithm and create InputStream to read (using gzip for example)
InputStream in;
if (ZipFormat.gzip().isFormatted(data)) {
    in = ZipFormat.gzip().newInputStream(data);
} else {
    // Assuming that "data" is an InputStream, otherwise you should create an InputStream for File or Path.
    in = data;
}


// zlib offers the option to see what compression level is used (null if is not compressed)
Integer level = ZipFormat.zlib().getCompressionLevel(data);
```

> [!IMPORTANT]
> ZipFormat class offers an instance for lz4 compression algorithm, but the [lz4 library](https://github.com/lz4/lz4-java) must be present on classpath to work correctly.

### Other

If you have a `DataOuput` instance, you can also create a delegated class with fallback writing for malformed Strings.
```java
DataOutput output = ...;

FallbackDataOutput fallbackOutput = new FallbackDataOutput(output);
```

## Tag input/output in depth

You may have seen method names like "writeUnnamed" and "readAny". Why they have names like that? And why there's no a simplified "write" and "read" methods that accept File and Path?

That's because Minecraft have changed across the time (it's a 15th year-old game of course) and the actual encoding options have too many combinations that is practically impossible to support them all with overloaded methods for multiple platforms (Java & Bedrock).

* unnamed, mean a tag that doesn't have a name associated with it (oldest way to encode/decode NBT).
* any, mean a tag, introduced for Java on Minecraft 1.20.2.
* bedrock file, mean a tag written in a bedrock file with its [header](https://wiki.bedrock.dev/nbt/nbt-in-depth.html#bedrock-nbt-file-header).

Following Minecraft format, there's no limit to write with a tag output, but tag input is limited by default with a maximum of 2MB size for tag (due network limitations) and 512 stack depth for nested values (since MC 1.20.2).

To change that limits you can also modify the recently created TagInput instance, for example:
```java
TagInput<Object> input = ...;

// For unlimited size
input.unlimited();

// For different size
input.maxQuota(10 * 1024 * 1024); // 10MB

// For different stack depth
input.maxDepth(128);
```

## Secondary formats

NBT format can be (nearly) represented on other data formats, this library support them all (as we know).

### SNBT

It's like Json, but most keys are unquoted, and more specific tags has their own String representation, visit [Minecraft Wiki](https://minecraft.wiki/w/NBT_format#SNBT_format) for more information.
```java
// Convert to SNBT
String snbt = TagWriter.toString(map);

// Get from SNBT
Map<String, Object> map = TagReader.fromString(snbt);
```
> [!NOTE]
> TagWriter use an AsyncStringWriter to append strings asynchronously, it's like java StringWriter, but it uses a StringBuilder instead of StringBuffer.

### Json

It's Json!, some data types may differ between conversion due any NBT array is converted to `JsonArray` and json arrays of `byte`, `int` and `long` are converted into `byte[]`, `int[]` and `long[]` instead of NBT List.
```java
// Convert to Json
JsonElement json = TagJson.toJson(map);

// Get from Json
Map<String, Object> map = TagJson.fromJson(json);
```

### Config

"What is this? Why you never heard of this secondary format?"

That's because this format is "new", was implemented on this library if you want to read/write java objects into configuration files (like YAML) without loosing formatting with specific NBT types (like byte and short).
```java
// Convert to simplified configuration
Object config = TagConfig.toConfigValue(map);

// Get from simplified configuration
Map<String, Object> map = TagConfig.fromConfigValue(config);
```

## Prettify NBT

It's just like `/data` command, a simplified way to "colorize" tags with provided/customizable palettes.
```java
// Default palette, using legacy colors
String colored = TagPalette.DEFAULT.color(map, null);

// Json palette, using raw json formatting
String colored = TagPalette.JSON.color(map, null);

// ansi palette, using ansi console colors (if you want to print it)
String colored = TagPalette.ANSI.color(map, null);

// Minimessage palette, using minimessage formatting
String colored = TagPalette.MINI_MESSAGE.color(map, null);


// You can also provide an indent, for example a 2-space indent for multi-line formatted string
String colored = TagPalette.DEFAULT.color(map, "  ");
```

> [!NOTE]
> This implementation is NOT "pretty print", it is just a "tag colorization".
