package com.google.gson.internal.bind;

import static com.google.gson.ToNumberPolicy.LONG_OR_DOUBLE;

import java.io.IOException;
import java.io.Writer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSnippet;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.MalformedJsonException;

/**
 * Reads and writes {@link JsonSnippet} objects.
 */
public final class JsonSnippetTypeAdapter extends TypeAdapter<JsonSnippet> {

    private static final Gson GSON = new GsonBuilder()
            .setObjectToNumberStrategy(LONG_OR_DOUBLE)
            .setNumberToNumberStrategy(LONG_OR_DOUBLE)
            .disableHtmlEscaping()
            .disableInnerClassSerialization()
            .disableJdkUnsafe().create();

    @Override
    public void write(final JsonWriter out, final JsonSnippet value) throws IOException {
        final String json = JsonSnippet.get(value);

        try {
            out.jsonValue(json);
        } catch (final UnsupportedOperationException ignore) {
            GSON.toJson(GSON.fromJson(json, Object.class), Object.class, out);
        }
    }

    @Override
    public JsonSnippet read(final JsonReader in) throws IOException {
        final StringBuilder buffer = new StringBuilder(1024);
        final Writer text = new StringWriter(buffer);

        try (JsonWriter out = new JsonWriter(text)) {
            parse(in, out);
        }

        return JsonSnippet.with(buffer);
    }

    private void parse(final JsonReader in, final JsonWriter out) throws IOException {
        final JsonToken token = in.peek();

        switch (token) {
            case NULL:
                in.nextNull();
                out.nullValue();
                break;

            case BOOLEAN:
                out.value(in.nextBoolean());
                break;

            case NUMBER:
                out.value(LONG_OR_DOUBLE.readNumber(in));
                break;

            case STRING:
                out.value(in.nextString());
                break;

            case BEGIN_OBJECT:
                in.beginObject();
                out.beginObject();

                while (in.hasNext()) {
                    out.name(in.nextName());
                    parse(in, out);
                }

                out.endObject();
                in.endObject();
                break;

            case BEGIN_ARRAY:
                in.beginArray();
                out.beginArray();

                while (in.hasNext()) {
                    parse(in, out);
                }

                out.endArray();
                in.endArray();
                break;

            default:
                throw new MalformedJsonException("Unexpected token: " + token);
        }
    }

    private static final class StringWriter extends Writer {

        private final StringBuilder buffer;

        StringWriter(final StringBuilder buffer) {
            this.buffer = buffer;
        }

        @Override
        public void write(final char[] buffer, final int offset, final int length) {
            this.buffer.append(buffer, offset, length);
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
