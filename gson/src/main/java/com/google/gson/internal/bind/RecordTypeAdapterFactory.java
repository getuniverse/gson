/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.gson.internal.bind;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.InvalidStateException;
import com.google.gson.internal.bind.util.Records;
import com.google.gson.internal.bind.util.Records.Descriptor;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Type adapter that reflects over the fields and methods of a class.
 */
public final class RecordTypeAdapterFactory implements TypeAdapterFactory {

    private final ConstructorConstructor constructorConstructor;
    private final FieldNamingStrategy fieldNamingPolicy;
    private final JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory;

    public RecordTypeAdapterFactory(ConstructorConstructor constructorConstructor,
                                    FieldNamingStrategy fieldNamingPolicy,
                                    JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory) {
        this.constructorConstructor = constructorConstructor;
        this.fieldNamingPolicy = fieldNamingPolicy;
        this.jsonAdapterFactory = jsonAdapterFactory;
    }

    @Override
    @SuppressWarnings("CodeBlock2Expr")
    public <T> TypeAdapter<T> create(Gson gson, final TypeToken<T> type) {
        Class<? super T> raw = type.getRawType();

        if (!Records.isRecord(raw)) {
            return null; // not a record
        }

        return Records.components(type.getType(), raw, fieldNamingPolicy, descriptor -> {
            return new Adapter<>(descriptor.constructor, getComponents(gson, type, descriptor));
        });
    }

    private BoundComponent createBoundComponent(final int index,
                                                final Gson context,
                                                final JsonAdapter jsonAdapter,
                                                final String name,
                                                final Class<?> fieldClass,
                                                final Type fieldType,
                                                final MethodHandle getter) {
        TypeAdapter<?> typeAdapter = null;

        if (jsonAdapter != null) {
            typeAdapter = jsonAdapterFactory.getTypeAdapter(constructorConstructor,
                                                            context,
                                                            TypeToken.get(fieldType),
                                                            jsonAdapter);
        }

        final boolean wrap = typeAdapter == null;

        if (wrap) {
            typeAdapter = context.getAdapter(fieldClass);
        }

        return new BoundComponent(index, name, getter, typeAdapter, wrap, context, fieldType);
    }

    private Map<String, BoundComponent> getComponents(Gson context, TypeToken<?> type, Descriptor descriptor) {
        final Map<String, BoundComponent> result = new LinkedHashMap<>();

        for (int i = 0, ii = descriptor.names.length; i < ii; ++i) {
            final String[] fieldNames = descriptor.names[i];
            final JsonAdapter adapter = descriptor.adapters[i];
            final Class<?> _class = descriptor.classes[i];
            final Type _type = descriptor.types[i];
            final MethodHandle getter = descriptor.getters[i];

            for (int j = 0, jj = fieldNames.length; j < jj; ++j) {
                final String name = fieldNames[j];
                final BoundComponent boundComponent = createBoundComponent(i,
                                                                           context,
                                                                           adapter,
                                                                           name,
                                                                           _class,
                                                                           _type,
                                                                           // only serialize the default name
                                                                           j == 0 ? getter : null
                );

                final BoundComponent previous = result.put(name, boundComponent);

                if (previous != null) {
                    throw new IllegalArgumentException(type.getType()
                                                       + " declares multiple JSON fields named " + previous.name);
                }
            }
        }

        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static final class BoundComponent {

        final String name;
        final MethodHandle getter;

        private final int index;
        private final TypeAdapter<?> adapterIn;
        private final TypeAdapter adapterOut;

        BoundComponent(int index,
                       String name,
                       MethodHandle getter,
                       TypeAdapter<?> typeAdapter,
                       boolean wrap,
                       Gson context,
                       Type fieldType) {
            this.index = index;
            this.name = name;
            this.getter = getter;
            this.adapterIn = typeAdapter;
            this.adapterOut = wrap
                              ? new TypeAdapterRuntimeTypeWrapper(context, typeAdapter, fieldType)
                              : typeAdapter;
        }
        @SuppressWarnings("unchecked")
        void write(JsonWriter writer, Object object) throws IOException {
            try {
                adapterOut.write(writer, getter.invoke(object));
            } catch (final Error e) {
                throw e;
            } catch (final Throwable e) {
                throw new InvalidStateException(e);
            }
        }

        void read(JsonReader reader, Object[] components) throws Exception {
            components[index] = adapterIn.read(reader);
        }
    }

    public static final class Adapter<T> extends TypeAdapter<T> {

        private final MethodHandle constructor;
        private final Map<String, BoundComponent> components;

        Adapter(MethodHandle constructor, Map<String, BoundComponent> components) {
            this.constructor = constructor;
            this.components = components;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            final Map<String, BoundComponent> components = this.components;
            final Object[] _components = new Object[components.size()];

            try {
                in.beginObject();

                while (in.hasNext()) {
                    final BoundComponent field = components.get(in.nextName());

                    if (field == null) {
                        in.skipValue();
                    } else {
                        field.read(in, _components);
                    }
                }

                in.endObject();

                return (T) constructor.invokeWithArguments(_components);
            } catch (IllegalStateException e) {
                throw new JsonSyntaxException(e);
            } catch (Error | RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new InvalidStateException(e);
            }
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }

            out.beginObject();
            for (BoundComponent component : components.values()) {
                if (component.getter != null) {
                    out.name(component.name);
                    component.write(out, value);
                }
            }
            out.endObject();
        }
    }
}
