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
package io.smallrye.faulttolerance.config;

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

// this must be kept identical to io.smallrye.faulttolerance.SecurityActions
/**
 * @author Martin Kouba
 */
final class SecurityActions {

    private SecurityActions() {
    }

    static Method getAnnotationMethod(Class<?> clazz, String name) throws PrivilegedActionException, NoSuchMethodException {
        if (System.getSecurityManager() == null) {
            return clazz.getMethod(name);
        }
        return AccessController.doPrivileged((PrivilegedExceptionAction<Method>) () -> clazz.getMethod(name));
    }

    static Field getDeclaredField(Class<?> clazz, String name) throws NoSuchFieldException, PrivilegedActionException {
        if (System.getSecurityManager() == null) {
            return clazz.getDeclaredField(name);
        }
        return AccessController.doPrivileged((PrivilegedExceptionAction<Field>) () -> clazz.getDeclaredField(name));
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

    /**
     * Get method of a given name from beanClass or one of its super-classes or interfaces.
     * The method should have the same parameter types as a method it is intended to be a fallback for (let's call it the original method).
     * The original method can be defined in a superclass and have parameter types provided with generics.
     *
     * @param beanClass the class of the bean that we search for the method for
     * @param declaringClass the class that defines the method that we search for a counterpart for
     * @param name name of the method
     * @param parameterTypes parameters of the method
     * @return the method in search
     * @throws PrivilegedActionException
     */
    static Method getDeclaredMethod(Class<?> beanClass, Class<?> declaringClass, String name, Type[] parameterTypes) throws PrivilegedActionException {
        if (System.getSecurityManager() == null) {
            return doGetMethod(beanClass, declaringClass, name, parameterTypes);
        }
        return AccessController.doPrivileged((PrivilegedExceptionAction<Method>) () -> doGetMethod(beanClass, declaringClass, name, parameterTypes));
    }

    private static Method doGetMethod(final Class<?> beanClass, final Class<?> declaringClass, String name, Type[] expectedParameters) {
        Method method;
        Class<?> current = beanClass;

        ParametrizedParamsTranslator expectedParamsTranslator = prepareParamsTranslator(beanClass, declaringClass);

        ParametrizedParamsTranslator actualParamsTranslator = new ParametrizedParamsTranslator();
        while (true) {
            method = getMethodFromClass(beanClass, current, name, expectedParameters, actualParamsTranslator, expectedParamsTranslator);
            if (method != null) {
                return method;
            } else {
                if (current.getSuperclass() == null) {
                    break;
                }
                actualParamsTranslator = actualParamsTranslator.getSuperclassTranslator(current);
                current = current.getSuperclass();
            }
        }

        for (Class<?> anInterface : beanClass.getInterfaces()) {
            method = getMethodFromClass(beanClass, anInterface, name, expectedParameters, actualParamsTranslator, expectedParamsTranslator);
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    /**
     * Bean class can be a sub-class of the class that defines the method.
     * This method prepares a translator for the eventual generic types of the method
     * to the types provided on the beanClass or any class between it and the class declaring the method
     * @param beanClass class of the bean for which the fallback method has been declared
     * @param clazz class that declares the method
     * @return translator
     */
    private static ParametrizedParamsTranslator prepareParamsTranslator(Class<?> beanClass, Class<?> clazz) {
        ParametrizedParamsTranslator result = new ParametrizedParamsTranslator();
        if (beanClass == clazz) {
            return result;
        }
        Class<?> current = beanClass;

        while (current != clazz && current != null) {
            if (current.getSuperclass() == null) {
                break;
            }
            result = result.getSuperclassTranslator(current);
            current = current.getSuperclass();
        }

        return result;
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

    private static Method getMethodFromClass(Class<?> originatingClass,
                                             Class<?> clazz, String name,
                                             Type[] parameterTypes,
                                             ParametrizedParamsTranslator translator,
                                             ParametrizedParamsTranslator expectedParamsTranslator) {
        Optional<Method> maybeMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> isAccessibleFrom(method, originatingClass))
                .filter(method -> method.getName().equals(name))
                .filter(parametersMatch(parameterTypes, translator, expectedParamsTranslator))
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

    private static Predicate<? super Method> parametersMatch(Type[] parameterTypes,
                                                             ParametrizedParamsTranslator translator,
                                                             ParametrizedParamsTranslator expectedParamsTranslator) {
        return method -> {
            Type[] methodParams = method.getGenericParameterTypes();
            if (parameterTypes.length != methodParams.length) {
                return false;
            }
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!parameterMatches(methodParams[i], parameterTypes[i], translator, expectedParamsTranslator)) {
                    return false;
                }
            }
            return true;
        };
    }

    private static boolean parameterMatches(Type methodParam,
                                            Type parameterType,
                                            ParametrizedParamsTranslator translator,
                                            ParametrizedParamsTranslator expectedParamsTranslator) {
        methodParam = translator.translate(methodParam);
        parameterType = expectedParamsTranslator.translate(parameterType);
        if (methodParam instanceof Class) {
            return parameterType == methodParam;
        } else {
            if (isArray(parameterType)) { // unwrap array
                if (isArray(methodParam)) {
                    Type methodParamComponentType = getArrayComponentType(methodParam);
                    Type componentType = getArrayComponentType(parameterType);
                    return parameterMatches(methodParamComponentType, componentType, translator, expectedParamsTranslator);
                } else {
                    return false;
                }
            }
            if (methodParam instanceof ParameterizedType) {
                if (parameterType instanceof ParameterizedType) {
                    return parameterizedTypeMatches((ParameterizedType) methodParam, (ParameterizedType) parameterType, translator, expectedParamsTranslator);
                } else {
                    return false;
                }
            }
            if (methodParam instanceof WildcardType) {
                if (parameterType instanceof WildcardType) {
                    return wildcardTypeMatches((WildcardType) methodParam, (WildcardType) parameterType, translator, expectedParamsTranslator);
                } else {
                    return false;
                }
            }

            return false;
        }
    }

    private static boolean wildcardTypeMatches(WildcardType methodParam,
                                               WildcardType parameterType,
                                               ParametrizedParamsTranslator translator,
                                               ParametrizedParamsTranslator expectedParamsTranslator) {
        return typeArrayMatches(methodParam.getLowerBounds(), parameterType.getLowerBounds(), translator, expectedParamsTranslator)
                && typeArrayMatches(methodParam.getUpperBounds(), parameterType.getUpperBounds(), translator, expectedParamsTranslator);
    }

    private static boolean typeArrayMatches(Type[] methodTypes,
                                            Type[] paramTypes,
                                            ParametrizedParamsTranslator translator,
                                            ParametrizedParamsTranslator expectedParamsTranslator) {
        if (methodTypes.length != paramTypes.length) {
            return false;
        }
        for (int i = 0; i < methodTypes.length; i++) {
            if (!parameterMatches(methodTypes[i], paramTypes[i], translator, expectedParamsTranslator)) {
                return false;
            }
        }
        return true;
    }

    private static boolean parameterizedTypeMatches(ParameterizedType methodParam,
                                                    ParameterizedType parameterType,
                                                    ParametrizedParamsTranslator translator,
                                                    ParametrizedParamsTranslator expectedParamsTranslator) {
        Type[] methodParameters = methodParam.getActualTypeArguments();
        Type[] actualParameters = parameterType.getActualTypeArguments();
        if (methodParameters.length != actualParameters.length) {
            return false;
        }
        for (int i = 0; i < methodParameters.length; i++) {
            if (!parameterMatches(methodParameters[i], actualParameters[i], translator, expectedParamsTranslator)) {
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

    static Method[] getDeclaredMethods(Class<?> clazz) throws PrivilegedActionException {
        if (System.getSecurityManager() == null) {
            return clazz.getDeclaredMethods();
        }
        return AccessController.doPrivileged((PrivilegedExceptionAction<Method[]>) clazz::getDeclaredMethods);
    }

    private static class ParametrizedParamsTranslator {
        private final List<Type> incomingTypes = new ArrayList<>();
        private final List<Type> typeParameters = new ArrayList<>();

        Type translate(Type paramType) {
            int typeIdx = typeParameters.indexOf(paramType);
            if (typeIdx >= 0) {
                paramType = incomingTypes.get(typeIdx);
            }
            return paramType;
        }

        ParametrizedParamsTranslator getSuperclassTranslator(Class<?> current) {
            ParametrizedParamsTranslator result = new ParametrizedParamsTranslator();
            result.incomingTypes.addAll(incomingTypesForSuperclass(current, typeParameters, incomingTypes));
            result.typeParameters.addAll(asList(current.getSuperclass().getTypeParameters()));
            return result;
        }
    }
}
