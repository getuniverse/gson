/*
 * Copyright (C) 2021-2022 Happeo Oy.
 * Copyright (C) 2008 Google Inc.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

/**
 * Tests for ensuring Gson thread-safety.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public class ConcurrencyTest {
  private Gson gson;

  @Before
  public void setUp() throws Exception {
    gson = new Gson();
  }

  /**
   * Source-code based on
   * http://groups.google.com/group/google-gson/browse_thread/thread/563bb51ee2495081
   */
  @Test
  public void testSingleThreadSerialization() {
    MyObject myObj = new MyObject();
    for (int i = 0; i < 10; i++) {
      gson.toJson(myObj);
    }
  }

  /**
   * Source-code based on
   * http://groups.google.com/group/google-gson/browse_thread/thread/563bb51ee2495081
   */
  @Test
  public void testSingleThreadDeserialization() {
    for (int i = 0; i < 10; i++) {
      gson.fromJson("{'a':'hello','b':'world','i':1}", MyObject.class);
    }
  }

  /**
   * Source-code based on
   * http://groups.google.com/group/google-gson/browse_thread/thread/563bb51ee2495081
   */
  @Test
  public void testMultiThreadSerialization() throws InterruptedException {
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch finishedLatch = new CountDownLatch(10);
    final AtomicBoolean failed = new AtomicBoolean(false);
    ExecutorService executor = Executors.newFixedThreadPool(10);
    for (int taskCount = 0; taskCount < 10; taskCount++) {
      executor.execute(new Runnable() {
        @Override public void run() {
          MyObject myObj = new MyObject();
          try {
            startLatch.await();
            for (int i = 0; i < 10; i++) {
              gson.toJson(myObj);
            }
          } catch (Throwable t) {
            failed.set(true);
          } finally {
            finishedLatch.countDown();
          }
        }
      });
    }
    startLatch.countDown();
    finishedLatch.await();
    assertFalse(failed.get());
  }

  /**
   * Source-code based on
   * http://groups.google.com/group/google-gson/browse_thread/thread/563bb51ee2495081
   */
  @Test
  public void testMultiThreadDeserialization() throws InterruptedException {
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch finishedLatch = new CountDownLatch(10);
    final AtomicBoolean failed = new AtomicBoolean(false);
    ExecutorService executor = Executors.newFixedThreadPool(10);
    for (int taskCount = 0; taskCount < 10; taskCount++) {
      executor.execute(new Runnable() {
        @Override public void run() {
          try {
            startLatch.await();
            for (int i = 0; i < 10; i++) {
              gson.fromJson("{'a':'hello','b':'world','i':1}", MyObject.class);
            }
          } catch (Throwable t) {
            failed.set(true);
          } finally {
            finishedLatch.countDown();
          }
        }
      });
    }
    startLatch.countDown();
    finishedLatch.await();
    assertFalse(failed.get());
  }

  /**
   * Test for:
   * https://github.com/google/gson/issues/764
   */
  public void testMultiThreadRecursiveObjectSerialization() throws InterruptedException {
    final int threads = 4;
    final ExecutorService executor = Executors.newFixedThreadPool(threads);
    final AtomicReference<Throwable> throwable = new AtomicReference<Throwable>();

    for (int i = 0; i < 1000; i++) {
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch finishedLatch = new CountDownLatch(threads);
      final Gson gson = new Gson();
      final MyRecursiveObject obj = new MyRecursiveObject();

      for (int j = 0; j < threads; j++) {
        executor.execute(new Runnable() {
          public void run() {
            try {
              startLatch.await();
              gson.toJson(obj);
            } catch (Throwable t) {
              throwable.set(t);
            } finally {
              finishedLatch.countDown();
            }
          }
        });
      }

      startLatch.countDown();
      finishedLatch.await();
      assertNull(throwable.get());
    }
  }

  @SuppressWarnings("unused")
  private static class MyObject {
    String a;
    String b;
    int i;

    MyObject() {
      this("hello", "world", 42);
    }

    public MyObject(String a, String b, int i) {
      this.a = a;
      this.b = b;
      this.i = i;
    }
  }

  private static class MyRecursiveObject {
    MyNestedObject obj;

    MyRecursiveObject() {
      this(true);
    }

    MyRecursiveObject(boolean init) {
      if (init) {
        this.obj = new MyNestedObject();
      }
    }

    private static class MyNestedObject {
      MyRecursiveObject obj;

      MyNestedObject() {
        this(true);
      }

      MyNestedObject(boolean init) {
        if (init) {
          this.obj = new MyRecursiveObject(false);
        }
      }
    }
  }

}
