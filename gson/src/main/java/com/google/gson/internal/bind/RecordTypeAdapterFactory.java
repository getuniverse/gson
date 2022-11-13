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
package com.google.gson.internal.bind;

import static com.google.gson.ReflectionAccessFilter.FilterResult.BLOCK_ALL;
import static com.google.gson.ReflectionAccessFilter.FilterResult.BLOCK_INACCESSIBLE;
import static com.google.gson.internal.ReflectionAccessFilterHelper.getFilterResult;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ReflectionAccessFilter;
import com.google.gson.ReflectionAccessFilter.FilterResult;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
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
    private final Excluder excluder;
    private final JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory;
    private final List<ReflectionAccessFilter> reflectionFilters;

    public RecordTypeAdapterFactory(final ConstructorConstructor constructorConstructor,
                                    final FieldNamingStrategy fieldNamingPolicy,
                                    final Excluder excluder,
                                    final JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory,
                                    final List<ReflectionAccessFilter> reflectionFilters) {
        this.constructorConstructor = constructorConstructor;
        this.fieldNamingPolicy = fieldNamingPolicy;
        this.excluder = excluder;
        this.jsonAdapterFactory = jsonAdapterFactory;
        this.reflectionFilters = reflectionFilters;
    }

    @Override
    @SuppressWarnings("CodeBlock2Expr")
    public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
        Class<? super T> raw = type.getRawType();

        if (!Records.isRecord(raw)) {
            return null; // not a record
        }

        final FilterResult filterResult = getFilterResult(reflectionFilters, raw);

        if (filterResult == BLOCK_ALL) {
            throw new JsonIOException("ReflectionAccessFilter does not permit using reflection for " + raw +
                                      ". Register a TypeAdapter for this type or adjust the access filter.");
        }

        final boolean blockInaccessible = filterResult == BLOCK_INACCESSIBLE;

        return Records.components(type.getType(), raw, fieldNamingPolicy, excluder, blockInaccessible, descriptor -> {
            return new Adapter<>(descriptor.names.length,
                                 descriptor.constructor,
                                 getComponents(gson, type, descriptor),
                                 descriptor.constructorName);
        });
    }

    private BoundComponent createBoundComponent(final int index,
                                                final Gson context,
                                                final JsonAdapter adapter,
                                                final String className,
                                                final String name,
                                                final Type fieldType,
                                                final MethodHandle getter,
                                                final boolean serialize,
                                                final boolean deserialize) {
        TypeAdapter<?> typeAdapter = null;

        final TypeToken<?> fieldToken = TypeToken.get(fieldType);

        if (adapter != null) {
            typeAdapter = jsonAdapterFactory.getTypeAdapter(constructorConstructor, context, fieldToken, adapter);
        }

        final boolean wrap = typeAdapter == null;

        if (wrap) {
            typeAdapter = context.getAdapter(fieldToken);
        }

        return new BoundComponent(index, className, name, getter, typeAdapter, wrap, context, fieldType, serialize, deserialize);
    }

    private Map<String, BoundComponent> getComponents(final Gson context,
                                                      final TypeToken<?> type,
                                                      final Descriptor descriptor) {
        final int fieldCount = descriptor.names.length;
        final Map<String, BoundComponent> result = new HashMap<>(fieldCount * 2);

        for (int i = 0; i < fieldCount; ++i) {
            final String[] fieldNames = descriptor.names[i];
            final JsonAdapter adapter = descriptor.adapters[i];
            final Type _type = descriptor.types[i];
            final MethodHandle getter = descriptor.getters[i];
            final String className = descriptor.className;
            final boolean serialize = descriptor.serialized[i];
            final boolean deserialize = descriptor.deserialized[i];

            for (int j = 0, jj = fieldNames.length; j < jj; ++j) {
                final String name = fieldNames[j];
                final BoundComponent boundComponent = createBoundComponent(i,
                                                                           context,
                                                                           adapter,
                                                                           className,
                                                                           name,
                                                                           _type,
                                                                           // only serialize the default name
                                                                           j == 0 ? getter : null,
                                                                           serialize,
                                                                           deserialize);

                if (result.put(name, boundComponent) != null) {
                    throw new IllegalArgumentException(type.getType() + " declares multiple JSON fields named " + name);
                }
            }
        }

        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static final class BoundComponent {

        final String name;
        final MethodHandle getter;
        final String className;

        private final int index;
        private final TypeAdapter<?> adapterIn;
        private final TypeAdapter adapterOut;
        private final boolean serialize;
        private final boolean deserialize;

        BoundComponent(final int index,
                       final String className,
                       final String name,
                       final MethodHandle getter,
                       final TypeAdapter<?> typeAdapter,
                       final boolean wrap,
                       final Gson context,
                       final Type fieldType,
                       final boolean serialize,
                       final boolean deserialize) {
            this.index = index;
            this.name = name;
            this.getter = getter;
            this.className = className;
            this.adapterIn = typeAdapter;
            this.adapterOut = wrap
                              ? new TypeAdapterRuntimeTypeWrapper(context, typeAdapter, fieldType)
                              : typeAdapter;
            this.serialize = serialize;
            this.deserialize = deserialize;
        }

        @SuppressWarnings("unchecked")
        void write(final JsonWriter writer, final Object object) throws IOException {
            if (serialize) {
                try {
                    adapterOut.write(writer, getter.invoke(object));
                } catch (final Error error) {
                    throw error;
                } catch (final Throwable error) {
                    throw new JsonIOException("Accessor method '" + className + "#" + name + "()' threw exception", error);
                }
            } else {
                writer.nullValue();
            }
        }

        void read(final JsonReader reader, final Object[] components) throws Exception {
            final Object value;

            if (deserialize) {
                value = adapterIn.read(reader);
            } else {
                value = null;
                reader.skipValue();
            }

            components[index] = value;
        }
    }

    public static final class Adapter<T> extends TypeAdapter<T> {

        private final int fields;
        private final MethodHandle constructor;
        private final Map<String, BoundComponent> components;
        private final String constructorName;

        Adapter(final int fields,
                final MethodHandle constructor,
                final Map<String, BoundComponent> components,
                final String constructorName) {
            this.constructor = constructor;
            this.fields = fields;
            this.components = components;
            this.constructorName = constructorName;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T read(final JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            final Object[] arguments = new Object[fields];

            try {
                in.beginObject();

                while (in.hasNext()) {
                    final BoundComponent field = components.get(in.nextName());

                    if (field == null) {
                        in.skipValue();
                    } else {
                        field.read(in, arguments);
                    }
                }

                in.endObject();
            } catch (final IllegalStateException error) {
                throw new JsonSyntaxException(error);
            } catch (final Error | RuntimeException error) {
                throw error;
            } catch (final Throwable error) {
                throw new InvalidStateException(error);
            }

            try {
                return (T) constructor.invokeWithArguments(arguments);
            } catch (Error e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException("Failed to invoke constructor '" + constructorName + "' with args " + Arrays.toString(arguments), e);
            }
        }

        @Override
        public void write(final JsonWriter out, final T value) throws IOException {
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
