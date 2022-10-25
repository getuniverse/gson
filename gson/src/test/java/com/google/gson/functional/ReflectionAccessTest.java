package com.google.gson.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
    assertEquals("[]", json);

    // But deserialization should fail
    Class<?> internalClass = Collections.emptyList().getClass();
    try {
      gson.fromJson("[]", internalClass);
      fail("Missing exception; test has to be run with `--illegal-access=deny`");
    } catch (JsonSyntaxException e) {
      fail("Unexpected exception; test has to be run with `--illegal-access=deny`");
    } catch (JsonIOException expected) {
      assertTrue(expected.getMessage().startsWith(
          "Failed making constructor 'java.util.Collections$EmptyList()' accessible;"
          + " either increase its visibility or write a custom InstanceCreator or TypeAdapter for its declaring type: "
      ));
    }
  }
}
