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
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.util.Collections;

import org.junit.Test;

public class ReflectionAccessTest {
  @SuppressWarnings("unused")
  private static class ClassWithPrivateMembers {
    private String s;

    private ClassWithPrivateMembers() {
    }
  }

  private static JsonIOException assertInaccessibleException(String json, Class<?> toDeserialize) {
    Gson gson = new Gson();
    try {
      gson.fromJson(json, toDeserialize);
      throw new AssertionError("Missing exception; test has to be run with `--illegal-access=deny`");
    } catch (JsonSyntaxException e) {
      throw new AssertionError("Unexpected exception; test has to be run with `--illegal-access=deny`", e);
    } catch (JsonIOException expected) {
      assertThat(expected).hasMessageThat().endsWith("\nSee https://github.com/google/gson/blob/main/Troubleshooting.md#reflection-inaccessible");
      // Return exception for further assertions
      return expected;
    }
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
    JsonIOException exception = assertInaccessibleException("[]", internalClass);
    // Don't check exact class name because it is a JDK implementation detail
    assertThat(exception).hasMessageThat().startsWith("Failed making constructor '");
    assertThat(exception).hasMessageThat().contains("' accessible; either increase its visibility or"
        + " write a custom InstanceCreator or TypeAdapter for its declaring type: ");
  }

  @Test
  public void testInaccessibleField() {
    JsonIOException exception = assertInaccessibleException("{}", Throwable.class);
    // Don't check exact field name because it is a JDK implementation detail
    assertThat(exception).hasMessageThat().startsWith("Failed making field 'java.lang.Throwable#");
    assertThat(exception).hasMessageThat().contains("' accessible; either increase its visibility or"
        + " write a custom TypeAdapter for its declaring type.");
  }
}
