/*
 * Copyright (C) 2022 Happeo Oy.
 * Copyright (C) 2017 The Gson authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file has been modified by Happeo Oy.
 */

package com.google.gson.internal.reflect;

import com.google.gson.JsonIOException;
import com.google.gson.internal.GsonBuildConfig;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionHelper {

  private ReflectionHelper() {}

  private static final MethodHandle trySetAccessible = getTrySetAccessibleMethod();

  /**
   * Tries making the member accessible, wrapping any thrown exception in a
   * {@link JsonIOException} with descriptive message.
   *
   * @param ao member to make accessible
   */
  public static void makeAccessible(AccessibleObject ao) {
    try {
      if (trySetAccessible != null) {
        trySetAccessible.invoke(ao);
      } else if (!ao.isAccessible()) {  // if trySetAccessible() is not available then neither is canAccess()
        ao.setAccessible(true);
      }
    } catch (Throwable error) {
      if (ao instanceof Constructor) {
        Constructor<?> constructor = (Constructor<?>) ao;
        throw new JsonIOException("Failed making constructor '" + constructorToString(constructor) + "' accessible;"
               + " either increase its visibility or write a custom InstanceCreator or TypeAdapter for"
               // Include the message since it might contain more detailed information
               + " its declaring type: " + error.getMessage());
      } else {
        String description = getAccessibleObjectDescription(ao, false);
        throw new JsonIOException("Failed making " + description + " accessible; either increase its visibility"
                                  + " or write a custom TypeAdapter for its declaring type.", error);
      }
    }
  }

  /**
   * Returns a short string describing the {@link AccessibleObject} in a human-readable way.
   * The result is normally shorter than {@link AccessibleObject#toString()} because it omits
   * modifiers (e.g. {@code final}) and uses simple names for constructor and method parameter
   * types.
   *
   * @param object object to describe
   * @param uppercaseFirstLetter whether the first letter of the description should be uppercased
   */
  public static String getAccessibleObjectDescription(AccessibleObject object, boolean uppercaseFirstLetter) {
    String description;

    if (object instanceof Field) {
      description = "field '" + fieldToString((Field) object) + "'";
    } else if (object instanceof Method) {
      Method method = (Method) object;

      StringBuilder methodSignatureBuilder = new StringBuilder(method.getName());
      appendExecutableParameters(method, methodSignatureBuilder);
      String methodSignature = methodSignatureBuilder.toString();

      description = "method '" + method.getDeclaringClass().getName() + "#" + methodSignature + "'";
    } else if (object instanceof Constructor) {
      description = "constructor '" + constructorToString((Constructor<?>) object) + "'";
    } else {
      description = "<unknown AccessibleObject> " + object.toString();
    }

    if (uppercaseFirstLetter && Character.isLowerCase(description.charAt(0))) {
      description = Character.toUpperCase(description.charAt(0)) + description.substring(1);
    }
    return description;
  }

  /**
   * Creates a string representation for a field, omitting modifiers and
   * the field type.
   */
  public static String fieldToString(Field field) {
    return field.getDeclaringClass().getName() + "#" + field.getName();
  }

  /**
   * Creates a string representation for a constructor.
   * E.g.: {@code java.lang.String(char[], int, int)}
   */
  public static String constructorToString(Constructor<?> constructor) {
    StringBuilder stringBuilder = new StringBuilder(constructor.getDeclaringClass().getName());
    appendExecutableParameters(constructor, stringBuilder);

    return stringBuilder.toString();
  }

  // Note: Ideally parameter type would be java.lang.reflect.Executable, but that was added in Java 8
  private static void appendExecutableParameters(AccessibleObject executable, StringBuilder stringBuilder) {
    stringBuilder.append('(');

    Class<?>[] parameters = (executable instanceof Method)
        ? ((Method) executable).getParameterTypes()
        : ((Constructor<?>) executable).getParameterTypes();
    for (int i = 0; i < parameters.length; i++) {
      if (i > 0) {
        stringBuilder.append(", ");
      }
      stringBuilder.append(parameters[i].getSimpleName());
    }

    stringBuilder.append(')');
  }

  /**
   * Tries making the member accessible
   *
   * @param ao member to make accessible
   * @return exception message; {@code null} if successful, non-{@code null} if
   *    unsuccessful
   */
  public static String tryMakeAccessible(AccessibleObject ao) {
    try {
      makeAccessible(ao);
      return null;
    } catch (JsonIOException error) {
      return error.getMessage();
    }
  }

  private static MethodHandle getTrySetAccessibleMethod() {
    try {
      Method method = AccessibleObject.class.getMethod("trySetAccessible");
      return MethodHandles.publicLookup().unreflect(method);
    } catch (Exception e) {
      return null;
    }
  }

  public static RuntimeException createExceptionForUnexpectedIllegalAccess(
      IllegalAccessException exception) {
    throw new RuntimeException("Unexpected IllegalAccessException occurred (Gson " + GsonBuildConfig.VERSION + ")."
        + " Certain ReflectionAccessFilter features require Java >= 9 to work correctly. If you are not using"
        + " ReflectionAccessFilter, report this to the Gson maintainers.",
        exception);
  }

  private static RuntimeException createExceptionForRecordReflectionException(
          ReflectiveOperationException exception) {
    throw new RuntimeException("Unexpected ReflectiveOperationException occurred"
            + " (Gson " + GsonBuildConfig.VERSION + ")."
            + " To support Java records, reflection is utilized to read out information"
            + " about records. All these invocations happens after it is established"
            + " that records exist in the JVM. This exception is unexpected behavior.",
            exception);
  }
}
