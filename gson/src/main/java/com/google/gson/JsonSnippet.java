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
package com.google.gson;

/**
 * Instances of this class encapsulate a pre-encoded JSON snippet. Use this class to exclude parts, or the entirety, of
 * a JSON input or output from <em>automatic</em> object mapping.
 * <p>
 * Targeted specifically for HTTP APIs that deal with client-controlled pieces of data within server-controlled resource
 * state, use of this class allows <em>partially</em> mapping JSON to objects and vice versa.
 * <p>
 * Partial mapping allows a mapped object to receive parts of the input in its original JSON form without building an
 * object graph therefrom: you get the validated, but un-parsed JSON snippet to e.g. save in a database.
 * <p>
 * Partial mapping also allows adding un-parsed JSON, possibly coming from the database where the partial JSON text was
 * previously saved, to an object mapped to JSON and returned as a representation of resource state.
 * <h3>Usage</h3>
 * Define client-controlled JSON snippets as <code>JsonSnippet</code> fields:
 * <pre><code>
 * public class MyJsonType {
 *     &hellip;
 *     public JsonSnippet customDetails;
 *     &hellip;
 * }
 * </code></pre>
 * To set the value of such field, use {@link #with(CharSequence)}:
 * <pre><code>
 * MyJsonType data = new MyJsonType();
 * &hellip;
 * data.customDetails = JsonSnippet.with("{\"name\":[\"value1\",\"value2\"]}");
 * </code></pre>
 * To read the value of such field, use {@link #get(JsonSnippet)}:
 * <pre><code>
 * MyJsonType data = new Gson().fromJson(&hellip;, MyJsonType.class);
 * String customDetails = JsonSnippet.get(data.customDetails);
 * </code></pre>
 * <p>
 * NULL JSON values are represented by Java <code>null</code> values: both <code>JsonSnippet.with(null)</code> and
 * <code>JsonSnippet.with("null")</code> return <code>null</code>, which means that <code>new Gson().fromJson("null",
 * JsonSnippet.class)</code> will also return <code>null</code>, as will <code>JsonSnippet.get(new Gson().fromJson("null",
 * JsonSnippet.class))</code>.
 */
public final class JsonSnippet {

    /**
     * Creates a new JSON snippet instance. Use {@link #get(JsonSnippet)} to access the snippet.
     *
     * @param json the JSON snippet to encapsulate; may be <code>null</code>.
     *
     * @return a new instance; may be <code>null</code>.
     */
    public static JsonSnippet with(final CharSequence json) {
        final String snippet = json == null ? "null" : json.toString();
        return snippet.equals("null") ? null : new JsonSnippet(snippet);
    }

    /**
     * Reads the JSON snippet encapsulated in the provided instance.
     *
     * @param snippet deserialized JSON snippet; may be <code>null</code>.
     *
     * @return the JSON snippet; may be <code>null</code>.
     */
    public static String get(final JsonSnippet snippet) {
        return snippet == null ? null : snippet.json;
    }

    private final String json;

    private JsonSnippet(final String json) {
        assert json != null;
        this.json = json;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        final JsonSnippet that = (JsonSnippet) other;
        return this.json.equals(that.json);
    }

    @Override
    public int hashCode() {
        return json.hashCode();
    }

    @Override
    public String toString() {
        return json;
    }
}
