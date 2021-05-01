package com.google.gson.internal.bind.util;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import com.google.gson.internal.InvalidStateException;

/**
 * Java record related utilities.
 */
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

            isRecord = type -> {
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

            getRecordComponents = type -> {
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
            final MethodHandle _getGenericType = METHODS.unreflect(recordComponentClass.getMethod("getGenericType"));
            final MethodHandle _getType = METHODS.unreflect(recordComponentClass.getMethod("getType"));
            final MethodHandle _getAccessor = METHODS.unreflect(recordComponentClass.getMethod("getAccessor"));

            getName = type -> {
                try {
                    return (String) _getName.invoke(type);
                } catch (final RuntimeException | Error error) {
                    throw error;
                } catch (final Throwable error) {
                    throw new IllegalStateException(error);
                }
            };

            getGenericType = type -> {
                try {
                    return (Type) _getGenericType.invoke(type);
                } catch (final RuntimeException | Error error) {
                    throw error;
                } catch (final Throwable error) {
                    throw new IllegalStateException(error);
                }
            };

            getType = type -> {
                try {
                    return (Class<?>) _getType.invoke(type);
                } catch (final RuntimeException | Error error) {
                    throw error;
                } catch (final Throwable error) {
                    throw new IllegalStateException(error);
                }
            };

            getAccessor = type -> {
                try {
                    return (Method) _getAccessor.invoke(type);
                } catch (final RuntimeException | Error error) {
                    throw error;
                } catch (final Throwable error) {
                    throw new IllegalStateException(error);
                }
            };
        } catch (final NoSuchMethodException | IllegalAccessException error) {
            error.printStackTrace();
            isRecord = ignore -> false;
        }

        IS_RECORD = isRecord;
        GET_RECORD_COMPONENTS = getRecordComponents;
        GET_NAME = getName;
        GET_GENERIC_TYPE = getGenericType;
        GET_TYPE = getType;
        GET_ACCESSOR = getAccessor;
    }

    public static boolean isRecord(final Class _class) {
        return IS_RECORD.test(_class);
    }

    public static <T> T components(final Class _class, final Consumer<T> consumer) {
        final Object[] components = GET_RECORD_COMPONENTS.apply(_class);
        final String[] names = new String[components.length];
        final Type[] types = new Class[components.length];
        final Class<?>[] classes = new Class[components.length];
        final Class<?>[] boxed = new Class[components.length];
        final AnnotatedElement[] annotations = new AnnotatedElement[components.length];
        final MethodHandle[] getters = new MethodHandle[components.length];

        for (int i = 0, ii = components.length; i < ii; ++i) {
            final Object component = components[i];

            annotations[i] = ((AnnotatedElement) component);
            types[i] = GET_GENERIC_TYPE.apply(component);
            classes[i] = GET_TYPE.apply(component);
            names[i] = GET_NAME.apply(component);
            boxed[i] = boxedType(classes[i]);

            final Method getter = GET_ACCESSOR.apply(component);

            getter.setAccessible(true);

            try {
                getters[i] = METHODS.unreflect(getter);
            } catch (final IllegalAccessException e) {
                throw new InvalidStateException(e);
            }
        }

        return consumer.accept(names, classes, types, boxed, annotations, getters);
    }

    private static Class<?> boxedType(final Class<?> type) {
        if (type.isPrimitive()) {
            if (type == int.class) {
                return Integer.class;
            } else if (type == boolean.class) {
                return Boolean.class;
            } else if (type == long.class) {
                return Long.class;
            } else if (type == double.class) {
                return Double.class;
            } else {
                throw new IllegalArgumentException("Unsupported primitive type: " + type);
            }
        } else {
            return type;
        }
    }

    public interface Consumer<T> {

        T accept(String[] names,
                 Class<?>[] classes,
                 Type[] types, Class<?>[] boxed,
                 AnnotatedElement[] annotations,
                 MethodHandle[] getters);
    }
}
