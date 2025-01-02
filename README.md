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
// Java object
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

<details>
  <summary>build.gradle</summary>

```groovy
plugins {
    id 'com.gradleup.shadow' version '8.3.5'
}

repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.saicone.nbt:nbt:1.0'
}

jar.dependsOn (shadowJar)

shadowJar {
    // Relocate nbt
    relocate 'com.saicone.nbt', project.group + '.libs.nbt'
    // Exclude unused classes (optional)
    minimize()
}
```

</details>

<details>
  <summary>build.gradle.kts</summary>

```kotlin
plugins {
    id("com.gradleup.shadow") version "8.3.5"
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.saicone.nbt:nbt:1.0")
}

tasks {
    jar {
        dependsOn(tasks.shadowJar)
    }

    shadowJar {
        // Relocate nbt
        relocate("com.saicone.nbt", "${project.group}.libs.nbt")
        // Exclude unused classes (optional)
        minimize()
    }
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

<build>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>3.3.0</version>
        <configuration>
            <relocations>
                <!-- Relocate nbt -->
                <relocation>
                    <pattern>com.saicone.nbt</pattern>
                    <shadedPattern>${project.groupId}.libs.nbt</shadedPattern>
                </relocation>
            </relocations>
            <!-- Exclude unused classes (optional) -->
            <minimizeJar>true</minimizeJar>
        </configuration>
        <executions>
            <execution>
                <phase>package</phase>
                <goals>
                    <goal>shade</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
</build>
```

</details>
