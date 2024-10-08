/*
 * Copyright (C) 2022 Happeo Oy.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.gson.functional;

import static com.google.gson.ToNumberPolicy.LONG_OR_DOUBLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSnippet;
import com.google.gson.internal.bind.JsonTreeWriter;
import java.util.List;
import java.util.Map;
import org.junit.Test;

@SuppressWarnings({"RedundantThrows", "MemberName"})
public final class JsonSnippetTest {

  private static final Gson GSON =
      new GsonBuilder().setObjectToNumberStrategy(LONG_OR_DOUBLE).create();

  @Test
  public void serializationFull() {
    final String json =
        GSON.toJson(
            Map.of("a", List.of(1, 2), "b", true, "c", List.of(Map.of("x", "y", "v", "w"))));
    final JsonSnippet in = JsonSnippet.with(json);
    final JsonSnippet.Writer out = JsonSnippet.writer();

    assertThat(GSON.toJson(in), is(json));

    GSON.toJson(GSON.fromJson(JsonSnippet.reader(in), JsonSnippet.class), JsonSnippet.class, out);

    assertThat(JsonSnippet.get(out.get()), is(JsonSnippet.get(in)));
  }

  @Test
  public void nullValue() {
    verifyNull(null);
    verifyNull("null");
    verifyNull("");
    verifyNull(" ");
  }

  private void verifyNull(final String nullOrBlank) {
    assertThat(GSON.toJson(JsonSnippet.with(nullOrBlank)), is("null"));
    assertNull(JsonSnippet.get(GSON.fromJson(nullOrBlank, JsonSnippet.class)));

    final JsonSnippet in = JsonSnippet.with(nullOrBlank);
    final JsonSnippet.Writer out = JsonSnippet.writer();

    GSON.toJson(GSON.fromJson(JsonSnippet.reader(in), JsonSnippet.class), JsonSnippet.class, out);

    assertThat(JsonSnippet.get(out.get()), is(JsonSnippet.get(in)));
  }

  @Test
  public void serializationParts() {
    final JsonSnippet _true = JsonSnippet.with("true");
    final JsonSnippet _false = JsonSnippet.with("false");
    final JsonSnippet _int = JsonSnippet.with("1234");
    final JsonSnippet _string = JsonSnippet.with("\"abcd\"");
    final JsonSnippet _list = JsonSnippet.with(GSON.toJson(List.of(1, 2)));
    final JsonSnippet _map = JsonSnippet.with(GSON.toJson(Map.of("x", "y")));
    final JsonSnippet _nested = JsonSnippet.with(GSON.toJson(Map.of("x", _list)));

    final String json =
        GSON.toJson(
            Map.of(
                "a", _true,
                "b", _false,
                "c", "null",
                "d", _int,
                "e", _string,
                "f", _list,
                "g", _map,
                "h", _nested));

    final JsonSnippet in = JsonSnippet.with(json);
    final JsonSnippet.Writer out = JsonSnippet.writer();

    assertThat(GSON.toJson(in), is(json));
    GSON.toJson(GSON.fromJson(JsonSnippet.reader(in), JsonSnippet.class), JsonSnippet.class, out);

    assertThat(JsonSnippet.get(out.get()), is(JsonSnippet.get(in)));
  }

  @Test
  public void treeWriter() throws Exception {
    final String json =
        GSON.toJson(
            Map.of("a", List.of(1, 2), "b", true, "c", List.of(Map.of("x", "y", "v", "w"))));

    final JsonTreeWriter writer = new JsonTreeWriter();
    GSON.toJson(JsonSnippet.with(json), Object.class, writer);

    assertThat(GSON.toJson(writer.get()), is(json));
  }

  @Test
  public void parsingFull() throws Exception {
    final String json =
        """
                            {"x": "text", "d": {"a": "b", "c": [1, 2]}, "y": 12345678,
                             "r2":{"a":"a", "b": 0, "d":[1, "2", 3]}}""";

    final JsonSnippet text = GSON.fromJson(json, JsonSnippet.class);

    assertThat(JsonSnippet.get(text), is(json.replaceAll("\\s+", "")));
  }

  @Test
  public void parsingPartial() throws Exception {
    final R1 r1 =
        GSON.fromJson(
            """
                                    {"x": "text", "d": {"a": "b", "c": [1, 2]}, "y": 12345678,
                                     "r2":{"a":"a", "b": 0, "d":[1, "2", 3]}}""",
            R1.class);

    assertThat(r1.x, is("text"));
    assertThat(r1.y, is(12345678L));
    assertThat(
        JsonSnippet.get(r1.d), is("""
                               {"a":"b","c":[1,2]}"""));

    assertThat(r1.r2.a, is("a"));
    assertThat(r1.r2.b, is(0L));
    assertThat(JsonSnippet.get(r1.r2.d), is("""
                               [1,"2",3]"""));
  }

  @Test
  public void parsingPartialNull() throws Exception {
    final R1 r0 = new R1("text", null, 12345678, new R2("a", null, 0));
    final R1 r1 = GSON.fromJson(GSON.toJson(r0), R1.class);

    assertThat(r1.x, is("text"));
    assertThat(r1.y, is(12345678L));
    assertNull(JsonSnippet.get(r1.d));

    assertThat(r1.r2.a, is("a"));
    assertThat(r1.r2.b, is(0L));
    assertNull(JsonSnippet.get(r1.r2.d));
  }

  @SuppressWarnings("ClassNamedLikeTypeParameter")
  public record R1(String x, JsonSnippet d, long y, R2 r2) {}

  @SuppressWarnings("ClassNamedLikeTypeParameter")
  public record R2(String a, JsonSnippet d, long b) {}
}
