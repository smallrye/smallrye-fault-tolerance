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
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.Arrays.asList;

/**
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

    public static Method getDeclaredMethod(Class<?> clazz, String name, Type[] parameterTypes) throws PrivilegedActionException {
        if (System.getSecurityManager() == null) {
            return doGetMethod(clazz, name, parameterTypes);
        }
        return AccessController.doPrivileged((PrivilegedExceptionAction<Method>) () -> doGetMethod(clazz, name, parameterTypes));
    }

    private static Method doGetMethod(final Class<?> clazz, String name, Type[] parameterClasses) {
        Method method;
        Class<?> current = clazz;
        List<Type> incomingTypes = Collections.emptyList();
        List<Type> typeParameters = Collections.emptyList();
        while (true) {
            method = getMethodFromClass(clazz, current, name, parameterClasses, incomingTypes, typeParameters);
            if (method != null) {
                return method;
            } else {
                if (current.getSuperclass() == null) {
                    break;
                }
                incomingTypes = incomingTypesForSuperclass(current, typeParameters, incomingTypes);
                current = current.getSuperclass();
                typeParameters = asList(current.getTypeParameters());
            }
        }

        for (Class<?> anInterface : clazz.getInterfaces()) {
            method = getMethodFromClass(clazz, anInterface, name, parameterClasses, incomingTypes, typeParameters);
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    private static List<Type> incomingTypesForSuperclass(Class<?> current, List<Type> typeParameters, List<Type> incomingTypes) {
        Type genericSuperclass = current.getGenericSuperclass();
        List<Type> actualTypeParams = (genericSuperclass instanceof ParameterizedType)
                ? asList(((ParameterizedType) genericSuperclass).getActualTypeArguments())
                : Collections.emptyList();

        List<Type> result = new ArrayList<>(actualTypeParams.size());

        int i = 0;
        for (Type type : actualTypeParams) {
            if (type instanceof Class) {
                result.add(type);
            } else {
                int paramIdx = typeParameters.indexOf(type);
                result.add(paramIdx >= 0 ? incomingTypes.get(paramIdx) : type);
            }
        }
        return result;
    }

    private static Method getMethodFromClass(Class<?> originatingClass, Class<?> clazz, String name, Type[] parameterTypes, List<Type> incomingTypes, List<Type> typeParameters) {
        Optional<Method> maybeMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> isAccessibleFrom(method, originatingClass))
                .filter(method -> method.getName().equals(name))
                .filter(parametersMatch(parameterTypes, incomingTypes, typeParameters))
                .findAny();
        return maybeMethod.orElse(null);
    }

    private static boolean isAccessibleFrom(Method method, Class<?> originatingClass) {
        if (isAccessible(method, Modifier.PUBLIC) || isAccessible(method, Modifier.PROTECTED)) {
            return true;
        }
        if (isAccessible(method, Modifier.PRIVATE)) {
            return method.getDeclaringClass() == originatingClass;
        }
        // not public and not protected and not private => default
        // accessible only if in the same package
        return method.getDeclaringClass().getPackage() == originatingClass.getPackage();
    }

    private static boolean isAccessible(Method method, int accessLevel) {
        return (method.getModifiers() & accessLevel) != 0;
    }

    private static Predicate<? super Method> parametersMatch(Type[] parameterTypes, List<Type> incomingTypes, List<Type> typeParameters) {
        return method -> {
            Type[] methodParams = method.getGenericParameterTypes();
            if (parameterTypes.length != methodParams.length) {
                return false;
            }
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!parameterMatches(methodParams[i], parameterTypes[i], incomingTypes, typeParameters)) {
                    return false;
                }
            }
            return true;
        };
    }

    private static boolean parameterMatches(Type methodParam, Type parameterType, List<Type> incomingTypes, List<Type> typeParameters) {
        if (methodParam instanceof Class) {
            return parameterType == methodParam;
        } else {
            if (isArray(parameterType)) { // unwrap array
                if (isArray(methodParam)) {
                    Type methodParamComponentType = getArrayComponentType(methodParam);
                    Type componentType = getArrayComponentType(parameterType);
                    return parameterMatches(methodParamComponentType, componentType, incomingTypes, typeParameters);
                } else {
                    return false;
                }
            }
            if (methodParam instanceof ParameterizedType) {
                if (parameterType instanceof ParameterizedType) {
                    return parameterizedTypeMatches((ParameterizedType) methodParam, (ParameterizedType) parameterType, incomingTypes, typeParameters);
                } else {
                    return false;
                }
            }
            if (methodParam instanceof WildcardType) {
                if (parameterType instanceof WildcardType) {
                    return wildcardTypeMatches((WildcardType) methodParam, (WildcardType) parameterType, incomingTypes, typeParameters);
                } else {
                    return false;
                }
            }

            return typeMatches(methodParam, parameterType, incomingTypes, typeParameters);
        }
    }

    private static boolean wildcardTypeMatches(WildcardType methodParam,
                                               WildcardType parameterType,
                                               List<Type> incomingTypes,
                                               List<Type> typeParameters) {
        return typeArrayMatches(methodParam.getLowerBounds(), parameterType.getLowerBounds(), incomingTypes, typeParameters)
                && typeArrayMatches(methodParam.getUpperBounds(), parameterType.getUpperBounds(), incomingTypes, typeParameters);
    }

    private static boolean typeArrayMatches(Type[] methodTypes, Type[] paramTypes, List<Type> incomingTypes, List<Type> typeParameters) {
        if (methodTypes.length != paramTypes.length) {
            return false;
        }
        for (int i = 0; i < methodTypes.length; i++) {
            if (!typeMatches(methodTypes[i], paramTypes[i], incomingTypes, typeParameters)) {
                return false;
            }
        }
        return true;
    }

    private static boolean parameterizedTypeMatches(ParameterizedType methodParam,
                                                    ParameterizedType parameterType,
                                                    List<Type> incomingTypes,
                                                    List<Type> typeParameters) {
        Type[] methodParameters = methodParam.getActualTypeArguments();
        Type[] actualParameters = parameterType.getActualTypeArguments();
        if (methodParameters.length != actualParameters.length) {
            return false;
        }
        for (int i = 0; i < methodParameters.length; i++) {
            if (!parameterMatches(methodParameters[i], actualParameters[i], incomingTypes, typeParameters)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Accepts only Classes of arrays and GenericArrayType subclasses
     *
     * @param parameterType
     * @return
     */
    private static Type getArrayComponentType(Type parameterType) {
        if (parameterType instanceof Class) {
            return ((Class) parameterType).getComponentType();
        } else {
            return ((GenericArrayType) parameterType).getGenericComponentType();
        }
    }

    private static boolean isArray(Type parameterType) {
        if (parameterType instanceof Class) {
            return ((Class) parameterType).isArray();
        } else {
            return parameterType instanceof GenericArrayType;
        }
    }

    private static boolean typeMatches(Type methodParam, Type parameterType, List<Type> incomingTypes, List<Type> typeParameters) {
        int typeIdx = typeParameters.indexOf(methodParam);
        if (typeIdx >= 0) {
            methodParam = incomingTypes.get(typeIdx);
        }
        if (methodParam instanceof Class) {
            return parameterType == methodParam;
        } else {
            // unable to determine the type
            return parameterType == Object.class;
        }
    }

    static Method[] getDeclaredMethods(Class<?> clazz) throws PrivilegedActionException {
        if (System.getSecurityManager() == null) {
            return clazz.getDeclaredMethods();
        }
        return AccessController.doPrivileged((PrivilegedExceptionAction<Method[]>) clazz::getDeclaredMethods);
    }

}
