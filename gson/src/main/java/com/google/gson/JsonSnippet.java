package com.google.gson;

/**
 * Instances of this class encapsulate a pre-encoded JSON snippet. Use this class to exclude parts, or the entirety, of
 * a JSON input or output from automatic object mapping.
 * <p>
 * Targeted specifically for APIs that deal with client-controlled pieces of data within server-controlled resource
 * state, use of this class allows
 * <em>partially</em> mapping JSON to objects and vice versa.
 * <p>
 * Partial mapping allows a mapped object to receive parts of the input in its original JSON form without building an
 * object graph therefrom: you get the validated, but un-parsed JSON snippet to e.g. save in a database.
 * <p>
 * Partial mapping also allows adding un-parsed JSON, possibly coming from the database where the partial JSON text was
 * previously saved, to an object mapped to JSON.
 * <p>
 * NULL JSON values are represented by Java <code>null</code> values: both <code>JsonSnippet.with(null)</code> and
 * <code>JsonSnippet.with("null")</code> return <code>null</code>, which means that <code>new Gson().fromJson("null",
 * JsonSnippet.class)</code> will also return <code>null</code>.
 */
public final class JsonSnippet {

    /**
     * Creates a new instance with a JSON snippet.
     *
     * @param contents the snippet to encapsulate; may be <code>null</code>.
     *
     * @return a new instance; may be <code>null</code>.
     */
    public static JsonSnippet with(final CharSequence contents) {
        final String value = contents == null ? "null" : contents.toString();
        return value.equals("null") ? null : new JsonSnippet(value);
    }

    /**
     * Convenience method to handle <code>null</code> snippets in JSON input.
     *
     * @param snippet deserialized JSON snippet; may be <code>null</code>.
     *
     * @return the JSON snippet; may be <code>null</code>.
     */
    public static String json(final JsonSnippet snippet) {
        return snippet == null ? null : snippet.contents;
    }

    private final String contents;

    private JsonSnippet(final String contents) {
        this.contents = contents;
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
        return this.contents.equals(that.contents);
    }

    @Override
    public int hashCode() {
        return contents.hashCode();
    }

    @Override
    public String toString() {
        return contents;
    }
}
