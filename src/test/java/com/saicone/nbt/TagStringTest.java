package com.saicone.nbt;

import com.saicone.nbt.io.TagReader;
import com.saicone.nbt.io.TagWriter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.saicone.nbt.TagAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

public class TagStringTest {

    private static final String SNBT_WRITE = "{byte:1b,short:2s,int:3,long:4l,float:5.0f,double:6.0,\"byte array\":[B;1B,2B,3B,4B],string:\"test123\",\"integer list\":[1,2,3,4],\"array list\":[[I;1],[I;2],[I;3],[I;4]],\"list list\":[[\"1\"],[\"2\"],[\"3\"],[\"4\"]],\"compound list\":[{test:\"asd\"},{number:1234},{list:[1234s]}],compound:{test:{list:[1234s]}},\"int array\":[I;1,2,3,4],\"long array\":[L;1L,2L,3L,4L]}";
    private static final String SNBT_READ = "{byte:1b,short:2s,int:3,long:4L,float:5.0f,double:6.0,\"byte array\":[B; true,2B, 3B,4B],string:\"test123\",'integer list':[1, 2,3, 4],'array list':[[I; 1],[I;2],[I;3],[I; 4]],\"list list\":[[ \"1\"],['2' ],[ '3'],[\"4\" ]],\"compound list\":[{test:'asd'},{number: 1234},{list:[1234s]}],compound:{test:{list:[1234s]}},'int array':[I; 1, 2, 3, 4],'long array':[L; 1L, 2L, 3L, 4L]}";
    private static final String SNBT_HETEROFENOUS = "{list:['a', {'b':3}], otherList:[{'b':3}, 'a']}";

    @Test
    public void testWrite() {
        final String snbt = TagWriter.toString(TagObjects.MAP);
        assertEquals(SNBT_WRITE, snbt);
    }

    @Test
    public void testRead() {
        final Map<String, Object> map = TagReader.fromString(SNBT_READ);
        assertTagEquals(TagObjects.MAP, map);
    }

    @Test
    public void testHeterogenous() {
        final Map<String, Object> expected = Map.of(
                "list", List.of(
                        Map.of("", "a"),
                        Map.of("b", 3)
                ),
                "otherList", List.of(
                        Map.of("b", 3),
                        Map.of("", "a")
                )
        );
        final Map<String, Object> actual = TagReader.fromString(SNBT_HETEROFENOUS);
        System.out.println(actual);
        assertTagEquals(expected, actual);
    }
}
