package com.google.gson.functional;

import static com.google.gson.ToNumberPolicy.LONG_OR_DOUBLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSnippet;
import com.google.gson.internal.bind.JsonTreeWriter;

@SuppressWarnings("RedundantThrows")
public final class JsonSnippetTest {

    private static final Gson GSON = new GsonBuilder()
            .setObjectToNumberStrategy(LONG_OR_DOUBLE)
            .create();

    @Test
    public void serializationFull() {
        final String json = GSON.toJson(Map.of("a", List.of(1, 2), "b", true, "c", List.of(Map.of("x", "y", "v", "w"))));

        assertThat(GSON.toJson(new JsonSnippet(json)), is(json));
    }

    @Test
    public void nullValue() {
        final String json = "null";

        assertThat(GSON.toJson(new JsonSnippet(null)), is(json));
        assertThat(GSON.toJson(new JsonSnippet(json)), is(json));
        assertThat(GSON.fromJson(json, JsonSnippet.class).get(), is(json));
    }

    @Test
    public void serializationParts() {
        final JsonSnippet _true = new JsonSnippet("true");
        final JsonSnippet _false = new JsonSnippet("false");
        final JsonSnippet _null = new JsonSnippet("null");
        final JsonSnippet _int = new JsonSnippet("1234");
        final JsonSnippet _string = new JsonSnippet("\"abcd\"");
        final JsonSnippet _list = new JsonSnippet(GSON.toJson(List.of(1, 2)));
        final JsonSnippet _map = new JsonSnippet(GSON.toJson(Map.of("x", "y")));
        final JsonSnippet _nested = new JsonSnippet(GSON.toJson(Map.of("x", _list)));

        final String json = GSON.toJson(Map.of("a", _true,
                                               "b", _false,
                                               "c", _null,
                                               "d", _int,
                                               "e", _string,
                                               "f", _list,
                                               "g", _map,
                                               "h", _nested));

        assertThat(GSON.toJson(new JsonSnippet(json)), is(json));
    }

    @Test
    public void treeWriter() throws Exception {
        final String json = GSON.toJson(Map.of("a", List.of(1, 2), "b", true, "c", List.of(Map.of("x", "y", "v", "w"))));

        final JsonTreeWriter writer = new JsonTreeWriter();
        GSON.toJson(new JsonSnippet(json), Object.class, writer);

        assertThat(GSON.toJson(writer.get()), is(json));
    }

    @Test
    public void parsingFull() throws Exception {
        final String json = """
                            {"x": "text", "d": {"a": "b", "c": [1, 2]}, "y": 12345678,
                             "r2":{"a":"a", "b": 0, "d":[1, "2", 3]}}""";

        final JsonSnippet text = GSON.fromJson(json, JsonSnippet.class);

        assertThat(text.get(), is(json.replaceAll("\\s+", "")));
    }

    @Test
    public void parsingPartial() throws Exception {
        final R1 r1 = GSON.fromJson("""
                                    {"x": "text", "d": {"a": "b", "c": [1, 2]}, "y": 12345678,
                                     "r2":{"a":"a", "b": 0, "d":[1, "2", 3]}}""",
                                    R1.class);

        assertThat(r1.x, is("text"));
        assertThat(r1.y, is(12345678L));
        assertThat(r1.d.get(), is("""
                               {"a":"b","c":[1,2]}"""));

        assertThat(r1.r2.a, is("a"));
        assertThat(r1.r2.b, is(0L));
        assertThat(r1.r2.d.get(), is("""
                               [1,"2",3]"""));
    }

    public record R1(String x, JsonSnippet d, long y, R2 r2) {}

    public record R2(String a, JsonSnippet d, long b) {}
}
