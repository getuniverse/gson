/*
 * Copyright (C) 2021-2022 Happeo Oy.
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

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public final class JavaRecordTest {

    private static final Gson GSON = new GsonBuilder()
            .setNumberToNumberStrategy(LONG_OR_DOUBLE)
            .setObjectToNumberStrategy(LONG_OR_DOUBLE)
            .create();

    @Test
    public void simpleRecords() {
        verifyJSON(Simple.class, new Simple("text", 1234L));
        assertThat(GSON.fromJson("{}", Simple.class), is(new Simple(null, 0L)));
    }

    @Test
    public void nestedRecords() {
        verifyJSON(Nested.class, new Nested("abcd", new Simple("text", 1234L)));
        assertThat(GSON.fromJson("{}", Nested.class), is(new Nested(null, null)));
    }

    @Test
    public void recursiveRecords() {
        verifyJSON(Recursive.class, new Recursive("abcd", new Recursive("text", new Recursive("xxx", null))));
        assertThat(GSON.fromJson("{}", Recursive.class), is(new Recursive(null, null)));
    }

    @Test
    public void genericRecords() {
        verifyJSON(Generic.class, new Generic("xxx", List.of(new Nested("abcd", new Simple("text1", 1234L)),
                                                             new Nested("efgh", new Simple("text2", 5678L)))));

        assertThat(GSON.fromJson("{}", Generic.class), is(new Generic(null, null)));
    }

    @Test
    public void complexRecords() {
        verifyJSON(Complex.class, new Complex(
                new Recursive("abcd", new Recursive("text", new Recursive("xxx", null))),
                Map.of("a", new Generic("xxx1", List.of(new Nested("abcd1", new Simple("text1", 1234L)),
                                                       new Nested("efgh1", new Simple("text2", 5678L)))),
                       "b", new Generic("xxx2", List.of(new Nested("abcd2", new Simple("text3", 4321L)),
                                                       new Nested("efgh2", new Simple("text4", 8765L)))))
        ));

        assertThat(GSON.fromJson("{}", Complex.class), is(new Complex(null, null)));
    }

    @Test
    public void parameterizedRecords() {
        verifyJSON(TypeToken.getParameterized(Parameterized.class, String.class, String.class).getType(),
                   new Parameterized<>("string", List.of("1234", "2345")));
        verifyJSON(TypeToken.getParameterized(Parameterized.class, String.class, Simple.class).getType(),
                   new Parameterized<>(new Simple("abcd", 12), List.of("1234", "2345")));
        verifyJSON(TypeToken.getParameterized(Parameterized.class, Simple.class, String.class).getType(),
                   new Parameterized<>("xxxx", List.of(new Simple("abcd", 12), new Simple("efgh", 23))));
    }

    private <T> void verifyJSON(final Type type, final T original) {
        final String json = GSON.toJson(original);
        final Object parsed = GSON.fromJson(json, type);

        assertThat(parsed, is(original));
    }

    public record Simple(String text, long number) {}

    public record Nested(String text, Simple simple) {}

    public record Recursive(String text, Recursive nested) {}

    public record Generic(String text, List<Nested> list) {}

    public record Complex(Recursive simple, Map<String, Generic> map) {}

    public record Parameterized<T, S>(S simple, List<T> list) {}
}
