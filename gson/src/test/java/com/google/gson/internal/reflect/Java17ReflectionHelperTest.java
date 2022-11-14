/*
/*
 * Copyright (C) 2022 Happeo Oy.
 * Copyright (C) 2022 The Gson authors
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

import static org.junit.Assert.assertTrue;

import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.util.Objects;
import org.junit.Test;

import com.google.gson.internal.bind.util.Records;

public class Java17ReflectionHelperTest {
  @Test
  public void testJava17Record() throws ClassNotFoundException {
    Class<?> unixDomainPrincipalClass = Class.forName("jdk.net.UnixDomainPrincipal");
    // UnixDomainPrincipal is a record
    assertTrue(Records.isRecord(unixDomainPrincipalClass));
  }

  /** Implementation of {@link UserPrincipal} and {@link GroupPrincipal} just for record tests. */
  public static class PrincipalImpl implements UserPrincipal, GroupPrincipal {
    private final String name;

    public PrincipalImpl(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof PrincipalImpl) {
        return Objects.equals(name, ((PrincipalImpl) o).name);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }
}
