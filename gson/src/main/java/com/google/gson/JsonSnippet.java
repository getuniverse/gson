package com.google.gson;

/**
 * Instances of this class encapsulate a pre-encoded JSON snippet. Use this
 * class to exclude parts, or the entirety, of a JSON input or output from
 * automatic object mapping.
 * <p>
 * Targeted specifically for APIs that deal with client-controlled pieces of
 * data within server-controlled resource state, use of this class allows
 * <em>partially</em> mapping JSON to objects and vice versa.
 * <p>
 * Partial mapping allows a mapped object to receive parts of the input in its
 * original JSON form without building an object graph therefrom: you get the
 * validated, but un-parsed JSON snippet to e.g. save in a database.
 * <p>
 * Partial mapping also allows adding un-parsed JSON, possibly coming from the
 * database where the partial JSON text was previously saved, to an object
 * mapped to JSON.
 */
public final class JsonSnippet {

    private final String contents;

    public JsonSnippet(final CharSequence contents) {
        this.contents = String.valueOf(contents);
    }

    public String get() {
        return contents;
    }

    @Override
    public String toString() {
        return contents;
    }
}
