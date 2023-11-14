/*
 * Copyright (C) 2021-2022 Happeo Oy.
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
 *
 * This file has been modified by Happeo Oy.
 */

package com.google.gson.internal.bind;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ReflectionAccessFilter;
import com.google.gson.ReflectionAccessFilter.FilterResult;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.ObjectConstructor;
import com.google.gson.internal.Primitives;
import com.google.gson.internal.ReflectionAccessFilterHelper;
import com.google.gson.internal.TroubleshootingGuide;
import com.google.gson.internal.reflect.ReflectionHelper;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Type adapter that reflects over the fields and methods of a class. */
public final class ReflectiveTypeAdapterFactory implements TypeAdapterFactory {
  private final ConstructorConstructor constructorConstructor;
  private final FieldNamingStrategy fieldNamingPolicy;
  private final Excluder excluder;
  private final JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory;
  private final List<ReflectionAccessFilter> reflectionFilters;

  public ReflectiveTypeAdapterFactory(
      ConstructorConstructor constructorConstructor,
      FieldNamingStrategy fieldNamingPolicy,
      Excluder excluder,
      JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory,
      List<ReflectionAccessFilter> reflectionFilters) {
    this.constructorConstructor = constructorConstructor;
    this.fieldNamingPolicy = fieldNamingPolicy;
    this.excluder = excluder;
    this.jsonAdapterFactory = jsonAdapterFactory;
    this.reflectionFilters = reflectionFilters;
  }

  private boolean includeField(Field f, boolean serialize) {
    return !excluder.excludeClass(f.getType(), serialize) && !excluder.excludeField(f, serialize);
  }

  /** first element holds the default name */
  @SuppressWarnings("MixedMutabilityReturnType")
  private List<String> getFieldNames(Field f) {
    SerializedName annotation = f.getAnnotation(SerializedName.class);
    if (annotation == null) {
      String name = fieldNamingPolicy.translateName(f);
      return Collections.singletonList(name);
    }

    String serializedName = annotation.value();
    String[] alternates = annotation.alternate();
    if (alternates.length == 0) {
      return Collections.singletonList(serializedName);
    }

    List<String> fieldNames = new ArrayList<>(alternates.length + 1);
    fieldNames.add(serializedName);
    Collections.addAll(fieldNames, alternates);
    return fieldNames;
  }

  @Override
  public <T> TypeAdapter<T> create(Gson gson, final TypeToken<T> type) {
    Class<? super T> raw = type.getRawType();

    if (!Object.class.isAssignableFrom(raw)) {
      return null; // it's a primitive!
    }

    FilterResult filterResult =
        ReflectionAccessFilterHelper.getFilterResult(reflectionFilters, raw);
    if (filterResult == FilterResult.BLOCK_ALL) {
      throw new JsonIOException(
          "ReflectionAccessFilter does not permit using reflection for "
              + raw
              + ". Register a TypeAdapter for this type or adjust the access filter.");
    }
    boolean blockInaccessible = filterResult == FilterResult.BLOCK_INACCESSIBLE;

    ObjectConstructor<T> constructor = constructorConstructor.get(type);
    return new FieldReflectionAdapter<>(
        constructor, getBoundFields(gson, type, raw, blockInaccessible));
  }

  private static <M extends AccessibleObject & Member> void checkAccessible(
      Object object, M member) {
    if (!ReflectionAccessFilterHelper.canAccess(
        member, Modifier.isStatic(member.getModifiers()) ? null : object)) {
      String memberDescription = ReflectionHelper.getAccessibleObjectDescription(member, true);
      throw new JsonIOException(
          memberDescription
              + " is not accessible and ReflectionAccessFilter does not permit making it"
              + " accessible. Register a TypeAdapter for the declaring type, adjust the access"
              + " filter or increase the visibility of the element and its declaring type.");
    }
  }

  private BoundField createBoundField(
      final Gson context,
      final Field field,
      final String serializedName,
      final TypeToken<?> fieldType,
      final boolean serialize,
      final boolean blockInaccessible) {

    final boolean isPrimitive = Primitives.isPrimitive(fieldType.getRawType());

    int modifiers = field.getModifiers();
    final boolean isStaticFinalField = Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers);

    JsonAdapter annotation = field.getAnnotation(JsonAdapter.class);
    TypeAdapter<?> mapped = null;
    if (annotation != null) {
      // This is not safe; requires that user has specified correct adapter class for @JsonAdapter
      mapped =
          jsonAdapterFactory.getTypeAdapter(
              constructorConstructor, context, fieldType, annotation, false);
    }
    final boolean jsonAdapterPresent = mapped != null;
    if (mapped == null) mapped = context.getAdapter(fieldType);

    @SuppressWarnings("unchecked")
    final TypeAdapter<Object> typeAdapter = (TypeAdapter<Object>) mapped;
    final TypeAdapter<Object> writeTypeAdapter;
    if (serialize) {
      writeTypeAdapter =
          jsonAdapterPresent
              ? typeAdapter
              : new TypeAdapterRuntimeTypeWrapper<>(context, typeAdapter, fieldType.getType());
    } else {
      // Will never actually be used, but we set it to avoid confusing nullness-analysis tools
      writeTypeAdapter = typeAdapter;
    }
    return new BoundField(serializedName, field) {
      @Override
      void write(JsonWriter writer, Object source) throws IOException, IllegalAccessException {
        if (blockInaccessible) {
          checkAccessible(source, field);
        }

        Object fieldValue;
        fieldValue = field.get(source);
        if (fieldValue == source) {
          // avoid direct recursion
          return;
        }
        writer.name(serializedName);
        writeTypeAdapter.write(writer, fieldValue);
      }

      @Override
      void readIntoField(JsonReader reader, Object target)
          throws IOException, IllegalAccessException {
        Object fieldValue = typeAdapter.read(reader);
        if (fieldValue != null || !isPrimitive) {
          if (blockInaccessible) {
            checkAccessible(target, field);
          } else if (isStaticFinalField) {
            // Reflection does not permit setting value of `static final` field, even after calling
            // `setAccessible`
            // Handle this here to avoid causing IllegalAccessException when calling `Field.set`
            String fieldDescription = ReflectionHelper.getAccessibleObjectDescription(field, false);
            throw new JsonIOException("Cannot set value of 'static final' " + fieldDescription);
          }
          field.set(target, fieldValue);
        }
      }
    };
  }

  private static class FieldsData {
    public static final FieldsData EMPTY =
        new FieldsData(
            Collections.<String, BoundField>emptyMap(), Collections.<BoundField>emptyList());

    /** Maps from JSON member name to field */
    public final Map<String, BoundField> deserializedFields;

    public final List<BoundField> serializedFields;

    public FieldsData(
        Map<String, BoundField> deserializedFields, List<BoundField> serializedFields) {
      this.deserializedFields = deserializedFields;
      this.serializedFields = serializedFields;
    }
  }

  private static IllegalArgumentException createDuplicateFieldException(
      Class<?> declaringType, String duplicateName, Field field1, Field field2) {
    throw new IllegalArgumentException(
        "Class "
            + declaringType.getName()
            + " declares multiple JSON fields named '"
            + duplicateName
            + "'; conflict is caused by fields "
            + ReflectionHelper.fieldToString(field1)
            + " and "
            + ReflectionHelper.fieldToString(field2)
            + "\nSee "
            + TroubleshootingGuide.createUrl("duplicate-fields"));
  }

  private FieldsData getBoundFields(
      Gson context, TypeToken<?> type, Class<?> raw, boolean blockInaccessible) {
    if (raw.isInterface()) {
      return FieldsData.EMPTY;
    }

    Map<String, BoundField> deserializedFields = new LinkedHashMap<>();
    // For serialized fields use a Map to track duplicate field names; otherwise this could be a
    // List<BoundField> instead
    Map<String, BoundField> serializedFields = new LinkedHashMap<>();

    Class<?> originalRaw = raw;
    while (raw != Object.class) {
      Field[] fields = raw.getDeclaredFields();

      // For inherited fields, check if access to their declaring class is allowed
      if (raw != originalRaw && fields.length > 0) {
        FilterResult filterResult =
            ReflectionAccessFilterHelper.getFilterResult(reflectionFilters, raw);
        if (filterResult == FilterResult.BLOCK_ALL) {
          throw new JsonIOException(
              "ReflectionAccessFilter does not permit using reflection for "
                  + raw
                  + " (supertype of "
                  + originalRaw
                  + "). Register a TypeAdapter for this type or adjust the access filter.");
        }
        blockInaccessible = filterResult == FilterResult.BLOCK_INACCESSIBLE;
      }

      for (Field field : fields) {
        boolean serialize = includeField(field, true);
        boolean deserialize = includeField(field, false);
        if (!serialize && !deserialize) {
          continue;
        }

        // If blockInaccessible, skip and perform access check later
        if (!blockInaccessible) {
          ReflectionHelper.makeAccessible(field);
        }

        Type fieldType = $Gson$Types.resolve(type.getType(), raw, field.getGenericType());
        List<String> fieldNames = getFieldNames(field);
        String serializedName = fieldNames.get(0);
        BoundField boundField =
            createBoundField(
                context,
                field,
                serializedName,
                TypeToken.get(fieldType),
                serialize,
                blockInaccessible);

        if (deserialize) {
          for (String name : fieldNames) {
            BoundField replaced = deserializedFields.put(name, boundField);

            if (replaced != null) {
              throw createDuplicateFieldException(originalRaw, name, replaced.field, field);
            }
          }
        }

        if (serialize) {
          BoundField replaced = serializedFields.put(serializedName, boundField);
          if (replaced != null) {
            throw createDuplicateFieldException(originalRaw, serializedName, replaced.field, field);
          }
        }
      }
      type = TypeToken.get($Gson$Types.resolve(type.getType(), raw, raw.getGenericSuperclass()));
      raw = type.getRawType();
    }
    return new FieldsData(deserializedFields, new ArrayList<>(serializedFields.values()));
  }

  abstract static class BoundField {
    /** Name used for serialization (but not for deserialization) */
    final String serializedName;

    final Field field;

    /** Name of the underlying field */
    final String fieldName;

    protected BoundField(String serializedName, Field field) {
      this.serializedName = serializedName;
      this.field = field;
      this.fieldName = field.getName();
    }

    /** Read this field value from the source, and append its JSON value to the writer */
    abstract void write(JsonWriter writer, Object source)
        throws IOException, IllegalAccessException;

    /**
     * Read the value from the reader, and set it on the corresponding field on target via
     * reflection
     */
    abstract void readIntoField(JsonReader reader, Object target)
        throws IOException, IllegalAccessException;
  }

  /**
   * Base class for Adapters produced by this factory.
   *
   * <p>This class encapsulates the common logic for serialization and deserialization. During
   * deserialization, we construct an accumulator A, which we use to accumulate values from the
   * source JSON. After the object has been read in full, the {@link #finalize(Object)} method is
   * used to convert the accumulator to an instance of T.
   *
   * @param <T> type of objects that this Adapter creates.
   * @param <A> type of accumulator used to build the deserialization result.
   */
  // This class is public because external projects check for this class with `instanceof` (even
  // though it is internal)
  public abstract static class Adapter<T, A> extends TypeAdapter<T> {
    private final FieldsData fieldsData;

    Adapter(FieldsData fieldsData) {
      this.fieldsData = fieldsData;
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }

      out.beginObject();
      try {
        for (BoundField boundField : fieldsData.serializedFields) {
          boundField.write(out, value);
        }
      } catch (IllegalAccessException e) {
        throw ReflectionHelper.createExceptionForUnexpectedIllegalAccess(e);
      }
      out.endObject();
    }

    @Override
    public T read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }

      A accumulator = createAccumulator();
      Map<String, BoundField> deserializedFields = fieldsData.deserializedFields;

      try {
        in.beginObject();
        while (in.hasNext()) {
          String name = in.nextName();
          BoundField field = deserializedFields.get(name);
          if (field == null) {
            in.skipValue();
          } else {
            readField(accumulator, in, field);
          }
        }
      } catch (IllegalStateException e) {
        throw new JsonSyntaxException(e);
      } catch (IllegalAccessException e) {
        throw ReflectionHelper.createExceptionForUnexpectedIllegalAccess(e);
      }
      in.endObject();
      return finalize(accumulator);
    }

    /** Create the Object that will be used to collect each field value */
    abstract A createAccumulator();

    /**
     * Read a single BoundField into the accumulator. The JsonReader will be pointed at the start of
     * the value for the BoundField to read from.
     */
    abstract void readField(A accumulator, JsonReader in, BoundField field)
        throws IllegalAccessException, IOException;

    /** Convert the accumulator to a final instance of T. */
    abstract T finalize(A accumulator);
  }

  private static final class FieldReflectionAdapter<T> extends Adapter<T, T> {
    private final ObjectConstructor<T> constructor;

    FieldReflectionAdapter(ObjectConstructor<T> constructor, FieldsData fieldsData) {
      super(fieldsData);
      this.constructor = constructor;
    }

    @Override
    T createAccumulator() {
      return constructor.construct();
    }

    @Override
    void readField(T accumulator, JsonReader in, BoundField field)
        throws IllegalAccessException, IOException {
      field.readIntoField(in, accumulator);
    }

    @Override
    T finalize(T accumulator) {
      return accumulator;
    }
  }
}
