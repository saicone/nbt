package com.saicone.nbt;

import com.saicone.nbt.io.NetworkDataInputStream;
import com.saicone.nbt.io.NetworkDataOutputStream;
import com.saicone.nbt.io.ReverseDataInputStream;
import com.saicone.nbt.io.ReverseDataOutputStream;
import com.saicone.nbt.io.TagInput;
import com.saicone.nbt.io.TagOutput;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import static com.saicone.nbt.TagAssertions.*;

public class TagIoTest {

    @Test
    public void testDefault() throws IOException {
        byte[] bytes;
        Map<String, Object> map;

        // Unnamed
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); TagOutput<Object> output = TagOutput.of(new DataOutputStream(out))) {
            output.writeUnnamed(TagObjects.MAP);
            bytes = out.toByteArray();
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes); TagInput<Object> input = TagInput.of(new DataInputStream(in))) {
            map = input.readUnnamed();
        }
        assertTagEquals(TagObjects.MAP, map);

        // Any
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); TagOutput<Object> output = TagOutput.of(new DataOutputStream(out))) {
            output.writeAny(TagObjects.MAP);
            bytes = out.toByteArray();
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes); TagInput<Object> input = TagInput.of(new DataInputStream(in))) {
            map = input.readAny();
        }
        assertTagEquals(TagObjects.MAP, map);

        // Bedrock File
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); TagOutput<Object> output = TagOutput.of(new DataOutputStream(out))) {
            output.writeBedrockFile(TagObjects.MAP);
            bytes = out.toByteArray();
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes); TagInput<Object> input = TagInput.of(new DataInputStream(in))) {
            map = input.readBedrockFile();
        }
        assertTagEquals(TagObjects.MAP, map);
    }

    @Test
    public void testReverse() throws IOException {
        byte[] bytes;
        Map<String, Object> map;

        // Unnamed
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); TagOutput<Object> output = TagOutput.of(new ReverseDataOutputStream(out))) {
            output.writeUnnamed(TagObjects.MAP);
            bytes = out.toByteArray();
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes); TagInput<Object> input = TagInput.of(new ReverseDataInputStream(in))) {
            map = input.readUnnamed();
        }
        assertTagEquals(TagObjects.MAP, map);

        // Any
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); TagOutput<Object> output = TagOutput.of(new ReverseDataOutputStream(out))) {
            output.writeAny(TagObjects.MAP);
            bytes = out.toByteArray();
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes); TagInput<Object> input = TagInput.of(new ReverseDataInputStream(in))) {
            map = input.readAny();
        }
        assertTagEquals(TagObjects.MAP, map);

        // Bedrock File
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); TagOutput<Object> output = TagOutput.of(new ReverseDataOutputStream(out))) {
            output.writeBedrockFile(TagObjects.MAP);
            bytes = out.toByteArray();
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes); TagInput<Object> input = TagInput.of(new ReverseDataInputStream(in))) {
            map = input.readBedrockFile();
        }
        assertTagEquals(TagObjects.MAP, map);
    }

    @Test
    public void testNetwork() throws IOException {
        byte[] bytes;
        Map<String, Object> map;

        // Unnamed
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); TagOutput<Object> output = TagOutput.of(new NetworkDataOutputStream(out))) {
            output.writeUnnamed(TagObjects.MAP);
            bytes = out.toByteArray();
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes); TagInput<Object> input = TagInput.of(new NetworkDataInputStream(in))) {
            map = input.readUnnamed();
        }
        assertTagEquals(TagObjects.MAP, map);

        // Any
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); TagOutput<Object> output = TagOutput.of(new NetworkDataOutputStream(out))) {
            output.writeAny(TagObjects.MAP);
            bytes = out.toByteArray();
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes); TagInput<Object> input = TagInput.of(new NetworkDataInputStream(in))) {
            map = input.readAny();
        }
        assertTagEquals(TagObjects.MAP, map);

        // Bedrock File
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); TagOutput<Object> output = TagOutput.of(new NetworkDataOutputStream(out))) {
            output.writeBedrockFile(TagObjects.MAP);
            bytes = out.toByteArray();
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes); TagInput<Object> input = TagInput.of(new NetworkDataInputStream(in))) {
            map = input.readBedrockFile();
        }
        assertTagEquals(TagObjects.MAP, map);
    }
}
