/*
 * Copyright 2017 Red Hat, Inc, and individual contributors.
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
package io.smallrye.faulttolerance;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 *
 * @author Martin Kouba
 */
public final class SecurityActions {

    private SecurityActions() {
    }

    public static Method getAnnotationMethod(Class<?> clazz, String name) throws PrivilegedActionException, NoSuchMethodException {
        if (System.getSecurityManager() == null) {
            return clazz.getMethod(name);
        }
        return AccessController.doPrivileged(new PrivilegedExceptionAction<Method>() {
            @Override
            public Method run() throws NoSuchMethodException, SecurityException {
                return clazz.getMethod(name);
            }
        });
    }

    static Field getDeclaredField(Class<?> clazz, String name) throws NoSuchFieldException, PrivilegedActionException {
        if (System.getSecurityManager() == null) {
            return clazz.getDeclaredField(name);
        }
        return AccessController.doPrivileged(new PrivilegedExceptionAction<Field>() {
            @Override
            public Field run() throws NoSuchFieldException {
                return clazz.getDeclaredField(name);
            }
        });
    }

    static void setAccessible(final AccessibleObject accessibleObject) {
        if (System.getSecurityManager() == null) {
            accessibleObject.setAccessible(true);
        }
        AccessController.doPrivileged((PrivilegedAction<AccessibleObject>) () -> {
            accessibleObject.setAccessible(true);
            return accessibleObject;
        });
    }
    public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>[] parameterTypes) throws PrivilegedActionException {
        if (System.getSecurityManager() == null) {
            return doGetMethod(clazz, name, parameterTypes);
        }
        return AccessController.doPrivileged((PrivilegedExceptionAction<Method>) () -> doGetMethod(clazz, name, parameterTypes));
    }

    private static Method doGetMethod(final Class<?> clazz, String name, Class<?>[] parameterTypes) {
        Method method = getMethodFromClass(clazz, name, parameterTypes);
        Class current = clazz;
        while (current != null) {
            method = getMethodFromClass(clazz, name, parameterTypes);
            if (method != null) {
                return method;
            } else {
                current = current.getSuperclass();
            }
        }

        for (Class<?> anInterface : clazz.getInterfaces()) {
            method = getMethodFromClass(anInterface, name, parameterTypes);
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    private static Method getMethodFromClass(Class<?> clazz, String name, Class<?>[] parameterTypes) {
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    static Method[] getDeclaredMethods(Class<?> clazz) throws PrivilegedActionException {
        if (System.getSecurityManager() == null) {
            return clazz.getDeclaredMethods();
        }
        return AccessController.doPrivileged(new PrivilegedExceptionAction<Method[]>() {
            @Override
            public Method[] run() {
                return clazz.getDeclaredMethods();
            }
        });
    }

}
