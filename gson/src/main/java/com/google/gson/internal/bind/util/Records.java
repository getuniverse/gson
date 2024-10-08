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
package com.google.gson.internal.bind.util;

import static java.lang.invoke.MethodHandles.explicitCastArguments;
import static java.lang.invoke.MethodType.methodType;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.JsonIOException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.reflect.ReflectionHelper;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/** Java record related utilities. */
@SuppressWarnings("MemberName")
public final class Records {

  private static final MethodHandles.Lookup METHODS = MethodHandles.lookup();
  private static final Predicate<Class<?>> IS_RECORD;
  private static final Function<Class<?>, Object[]> GET_RECORD_COMPONENTS;
  private static final Function<Object, String> GET_NAME;
  private static final Function<Object, Type> GET_GENERIC_TYPE;
  private static final Function<Object, Class<?>> GET_TYPE;
  private static final Function<Object, Method> GET_ACCESSOR;

  static {
    Function<Class<?>, Object[]> getRecordComponents = null;
    Function<Object, String> getName = null;
    Function<Object, Type> getGenericType = null;
    Function<Object, Class<?>> getType = null;
    Function<Object, Method> getAccessor = null;

    Predicate<Class<?>> isRecord;

    try {
      final MethodHandle _isRecord = METHODS.unreflect(Class.class.getMethod("isRecord"));

      isRecord =
          type -> {
            try {
              return (boolean) _isRecord.invoke(type);
            } catch (final RuntimeException | Error error) {
              throw error;
            } catch (final Throwable error) {
              throw new IllegalStateException(error);
            }
          };

      final Method method = Class.class.getMethod("getRecordComponents");
      final MethodHandle _getRecordComponents = METHODS.unreflect(method);

      getRecordComponents =
          type -> {
            try {
              return (Object[]) _getRecordComponents.invoke(type);
            } catch (final RuntimeException | Error error) {
              throw error;
            } catch (final Throwable error) {
              throw new IllegalStateException(error);
            }
          };

      final Class<?> recordComponentClass = method.getReturnType().getComponentType();

      final MethodHandle _getName = METHODS.unreflect(recordComponentClass.getMethod("getName"));
      final MethodHandle _getGenericType =
          METHODS.unreflect(recordComponentClass.getMethod("getGenericType"));
      final MethodHandle _getType = METHODS.unreflect(recordComponentClass.getMethod("getType"));
      final MethodHandle _getAccessor =
          METHODS.unreflect(recordComponentClass.getMethod("getAccessor"));

      getName =
          type -> {
            try {
              return (String) _getName.invoke(type);
            } catch (final RuntimeException | Error error) {
              throw error;
            } catch (final Throwable error) {
              throw new IllegalStateException(error);
            }
          };

      getGenericType =
          type -> {
            try {
              return (Type) _getGenericType.invoke(type);
            } catch (final RuntimeException | Error error) {
              throw error;
            } catch (final Throwable error) {
              throw new IllegalStateException(error);
            }
          };

      getType =
          type -> {
            try {
              return (Class<?>) _getType.invoke(type);
            } catch (final RuntimeException | Error error) {
              throw error;
            } catch (final Throwable error) {
              throw new IllegalStateException(error);
            }
          };

      getAccessor =
          type -> {
            try {
              return (Method) _getAccessor.invoke(type);
            } catch (final RuntimeException | Error error) {
              throw error;
            } catch (final Throwable error) {
              throw new IllegalStateException(error);
            }
          };
    } catch (final NoSuchMethodException | IllegalAccessException error) {
      isRecord = ignore -> false;
    }

    IS_RECORD = isRecord;
    GET_RECORD_COMPONENTS = getRecordComponents;
    GET_NAME = getName;
    GET_GENERIC_TYPE = getGenericType;
    GET_TYPE = getType;
    GET_ACCESSOR = getAccessor;
  }

  private Records() {
    throw new UnsupportedOperationException();
  }

  public static boolean isRecord(final Class<?> _class) {
    return IS_RECORD.test(_class);
  }

  private static final Map<Type, Descriptor> descriptorCache =
      new ConcurrentHashMap<>(128, 0.75f, 32);

  public static <T> T components(
      final Type type,
      final Class<?> _class,
      final FieldNamingStrategy fieldNamingPolicy,
      final Excluder excluder,
      final boolean blockInaccessible,
      final Function<Descriptor, T> consumer) {
    return consumer.apply(
        descriptorCache.computeIfAbsent(
            type,
            ignore ->
                new Descriptor(type, _class, fieldNamingPolicy, excluder, blockInaccessible)));
  }

  private static Class<?> boxedType(final Class<?> type) {
    if (type.isPrimitive()) {
      if (type == byte.class) {
        return Byte.class;
      } else if (type == char.class) {
        return Character.class;
      } else if (type == short.class) {
        return Short.class;
      } else if (type == int.class) {
        return Integer.class;
      } else if (type == boolean.class) {
        return Boolean.class;
      } else if (type == long.class) {
        return Long.class;
      } else if (type == float.class) {
        return Float.class;
      } else if (type == double.class) {
        return Double.class;
      } else {
        throw new IllegalArgumentException("Unsupported primitive type: " + type);
      }
    } else {
      return type;
    }
  }

  private static String[] getFieldNames(
      final FieldNamingStrategy fieldNamingPolicy,
      final AnnotatedElement element,
      final Field field) {
    final SerializedName annotation = element.getAnnotation(SerializedName.class);

    if (annotation == null) {
      return new String[] {fieldNamingPolicy.translateName(field)};
    } else if (!field.isAnnotationPresent(SerializedName.class)) {
      // @SerializedName can be placed on accessor method, but it is not supported there
      // If field and method have annotation it is not easily possible to determine if accessor
      // method
      // is implicit and has inherited annotation, or if it is explicitly declared with custom
      // annotation
      throw new JsonIOException(
          "@"
              + SerializedName.class.getSimpleName()
              + " on method"
              + " '"
              + field.getDeclaringClass().getName()
              + "#"
              + field.getName()
              + "()' is not supported");
    }

    final String serializedName = annotation.value();
    final String[] alternates = annotation.alternate();

    if (alternates.length == 0) {
      return new String[] {serializedName};
    }

    final List<String> fieldNames = new ArrayList<>(alternates.length + 1);

    fieldNames.add(serializedName);
    Collections.addAll(fieldNames, alternates);

    return fieldNames.toArray(new String[0]);
  }

  public static final class Descriptor {

    public final String[][] names;
    public final Type[] types;
    public final JsonAdapter[] adapters;
    public final MethodHandle[] getters;
    public final MethodHandle constructor;
    public final String className;
    public final String constructorName;
    public final boolean[] serialized;
    public final boolean[] deserialized;

    private final Excluder excluder;

    Descriptor(
        final Type targetType,
        final Class<?> recordClass,
        final FieldNamingStrategy fieldNamingPolicy,
        final Excluder excluder,
        final boolean blockInaccessible) {
      this.excluder = excluder;

      final Object[] components = GET_RECORD_COMPONENTS.apply(recordClass);
      final Class<?>[] boxed = new Class<?>[components.length];

      final String[][] names = this.names = new String[components.length][];
      final Type[] types = this.types = new Type[components.length];
      final Class<?>[] classes = new Class<?>[components.length];
      final MethodHandle[] getters = this.getters = new MethodHandle[components.length];
      final JsonAdapter[] adapters = this.adapters = new JsonAdapter[components.length];
      final boolean[] serialized = this.serialized = new boolean[components.length];
      final boolean[] deserialized = this.deserialized = new boolean[components.length];

      IllegalAccessException inaccessible = null;

      for (int i = 0, ii = components.length; i < ii; ++i) {
        final Object component = components[i];
        final Method getter = GET_ACCESSOR.apply(component);

        if (!blockInaccessible) {
          getter.setAccessible(true);
        }

        try {
          getters[i] = METHODS.unreflect(getter);
        } catch (final IllegalAccessException error) {
          if (inaccessible == null) {
            inaccessible = error;
          }
        }

        final Class<?> _class = classes[i] = GET_TYPE.apply(component);
        final Type _type = GET_GENERIC_TYPE.apply(component);
        final Field field;

        try {
          field = recordClass.getDeclaredField(GET_NAME.apply(component));
        } catch (final NoSuchFieldException error) {
          throw new IllegalStateException(error);
        }

        types[i] = $Gson$Types.resolve(targetType, recordClass, _type);
        boxed[i] = boxedType(_class);
        names[i] = getFieldNames(fieldNamingPolicy, getter, field);
        adapters[i] = field.getAnnotation(JsonAdapter.class);
        serialized[i] = includeField(field, true);
        deserialized[i] = includeField(field, false);
      }

      try {
        final Constructor<?> _constructor = recordClass.getDeclaredConstructor(classes);

        if (!blockInaccessible && !Modifier.isPublic(_constructor.getModifiers())) {
          _constructor.setAccessible(true);
        }

        try {
          if (inaccessible != null) {
            throw inaccessible;
          }

          this.constructor =
              explicitCastArguments(
                  METHODS.unreflectConstructor(_constructor), methodType(recordClass, boxed));
        } catch (final IllegalAccessException error) {
          throw new JsonIOException(
              ReflectionHelper.getAccessibleObjectDescription(_constructor, true)
                  + " is not accessible and ReflectionAccessFilter does not permit making it"
                  + " accessible. Register a TypeAdapter for the declaring type, adjust the access"
                  + " filter or increase the visibility of the element and its declaring type.",
              error);
        }

        this.className = recordClass.getName();
        this.constructorName = ReflectionHelper.constructorToString(_constructor);
      } catch (final RuntimeException | Error error) {
        throw error;
      } catch (final Throwable error) {
        throw new IllegalStateException(error);
      }
    }

    private boolean includeField(Field field, boolean serialize) {
      return !excluder.excludeClass(field.getType(), serialize)
          && !excluder.excludeField(field, serialize);
    }
  }
}
