/*
 * Copyright (C) 2022 Happeo Oy.
 * Copyright (C) 2021 Google Inc.
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

package com.google.gson.functional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.ReflectPermission;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Permission;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

public class ReflectionAccessTest {
  @SuppressWarnings("unused")
  private static class ClassWithPrivateMembers {
    private String s;

    private ClassWithPrivateMembers() {
    }
  }

  private static Class<?> loadClassWithDifferentClassLoader(Class<?> c) throws Exception {
    URL url = c.getProtectionDomain().getCodeSource().getLocation();
    URLClassLoader classLoader = new URLClassLoader(new URL[] { url }, null);
    return classLoader.loadClass(c.getName());
  }

  /**
   * Test serializing an instance of a non-accessible internal class, but where
   * Gson supports serializing one of its superinterfaces.
   *
   * <p>Here {@link Collections#emptyList()} is used which returns an instance
   * of the internal class {@code java.util.Collections.EmptyList}. Gson should
   * serialize the object as {@code List} despite the internal class not being
   * accessible.
   *
   * <p>See https://github.com/google/gson/issues/1875
   */
  @Test
  public void testSerializeInternalImplementationObject() {
    Gson gson = new Gson();
    String json = gson.toJson(Collections.emptyList());
    assertThat(json).isEqualTo("[]");

    // But deserialization should fail
    Class<?> internalClass = Collections.emptyList().getClass();
    try {
      gson.fromJson("[]", internalClass);
      fail("Missing exception; test has to be run with `--illegal-access=deny`");
    } catch (JsonSyntaxException e) {
      fail("Unexpected exception; test has to be run with `--illegal-access=deny`");
    } catch (JsonIOException expected) {
      assertThat(expected).hasMessageThat().startsWith("Failed making constructor 'java.util.Collections$EmptyList()' accessible;"
          + " either increase its visibility or write a custom InstanceCreator or TypeAdapter for its declaring type: ");
    }
  }
}
