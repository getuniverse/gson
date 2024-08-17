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

import java.io.Reader;
import java.io.StringReader;
import java.util.function.Supplier;

/**
 * Instances of this class encapsulate a pre-encoded JSON snippet. Use this class to exclude parts,
 * or the entirety, of a JSON input or output from <em>automatic</em> object mapping.
 *
 * <p>Targeted specifically for HTTP APIs that deal with client-controlled pieces of data within
 * server-controlled resource state, use of this class allows <em>partially</em> mapping JSON to
 * objects and vice versa.
 *
 * <p>Partial mapping allows a mapped object to receive parts of the input in its original JSON form
 * without building an object graph therefrom: you get the validated, but un-parsed JSON snippet to
 * e.g. save in a database.
 *
 * <p>Partial mapping also allows adding un-parsed JSON, possibly coming from the database where the
 * partial JSON text was previously saved, to an object mapped to JSON and returned as a
 * representation of resource state.
 *
 * <h2>Usage</h2>
 *
 * Define client-controlled JSON snippets as {@code JsonSnippet} fields:
 *
 * <pre><code>
 * public class MyJsonType {
 *     &hellip;
 *     public JsonSnippet customDetails;
 *     &hellip;
 * }
 * </code></pre>
 *
 * To set the value of such field, use {@link #with(CharSequence)}:
 *
 * <pre><code>
 * MyJsonType data = new MyJsonType();
 * &hellip;
 * data.customDetails = JsonSnippet.with("{\"name\":[\"value1\",\"value2\"]}");
 * </code></pre>
 *
 * To read the value of such field, use {@link #get(JsonSnippet)}:
 *
 * <pre><code>
 * MyJsonType data = new Gson().fromJson(&hellip;, MyJsonType.class);
 * String customDetails = JsonSnippet.get(data.customDetails);
 * </code></pre>
 *
 * <p>NULL JSON values are represented by Java {@code null} values: both {@code
 * JsonSnippet.with(null)} and {@code JsonSnippet.with("null")} return {@code null}, which means
 * that {@code new Gson().fromJson("null", JsonSnippet.class)} will also return {@code null}, as
 * will {@code JsonSnippet.get(new Gson().fromJson("null", JsonSnippet.class))}.
 */
public final class JsonSnippet {

  /**
   * Creates a new JSON snippet instance. Use {@link #get(JsonSnippet)} to access the snippet.
   *
   * @param json the JSON snippet to encapsulate; may be {@code null}.
   * @return a new instance; may be {@code null}.
   */
  public static JsonSnippet with(final CharSequence json) {
    final String snippet = json == null ? "" : json.toString().trim();
    return snippet.isEmpty() || snippet.equals("null") ? null : new JsonSnippet(snippet);
  }

  /**
   * Reads the JSON snippet encapsulated in the provided instance.
   *
   * @param snippet deserialized JSON snippet; may be {@code null}.
   * @return the JSON snippet; may be {@code null}.
   */
  public static String get(final JsonSnippet snippet) {
    return snippet == null ? null : snippet.json;
  }

  /**
   * Creates a {@code Reader} from the given JSON {@code snippet}.
   *
   * @param snippet the JSON snippet to read; may be {@code null}, in which case {@code "null"} will
   *     be read.
   * @return a new readr; never <code>null</code>.
   */
  public static Reader reader(final JsonSnippet snippet) {
    return new StringReader(String.valueOf(get(snippet)));
  }

  /**
   * Creates a new {@code Writer} that writes into a new {@code JsonSnippet}, which can then be
   * retrieved by calling {@link Writer#get()}.
   *
   * @return a new writer; never {@code null}.
   */
  public static Writer writer() {
    return new Writer();
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

  /**
   * A {@code java.io.Writer} that {@link #get() supplies} a {@link JsonSnippet} from the written.
   */
  public static final class Writer extends java.io.Writer implements Supplier<JsonSnippet> {

    private final StringBuilder json = new StringBuilder(1024);

    Writer() {
      // empty
    }

    /**
     * Creates a new {@link JsonSnippet} from what has been {@link #flush() flushed} so far to this
     * writer.
     *
     * @return a new {@link JsonSnippet}; may be <code>null</code>.
     */
    @Override
    public JsonSnippet get() {
      return JsonSnippet.with(json);
    }

    @Override
    public void write(final char[] characters, final int offset, final int length) {
      json.append(characters, offset, length);
    }

    @Override
    public void flush() {
      // empty
    }

    @Override
    public void close() {
      // empty
    }
  }
}
