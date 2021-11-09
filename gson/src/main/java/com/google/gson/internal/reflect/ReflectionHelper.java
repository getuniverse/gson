/*
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
 */
package com.google.gson.internal.reflect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.google.gson.JsonIOException;

public class ReflectionHelper {
  private ReflectionHelper() { }

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
        throw new JsonIOException("Failed making " + ao + " accessible;" +
                                  " either change its visibility or write a custom "
                                  + (ao instanceof Constructor ? "InstanceCreator or " : "")
                                  + "TypeAdapter for its declaring type", error);
    }
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
}
