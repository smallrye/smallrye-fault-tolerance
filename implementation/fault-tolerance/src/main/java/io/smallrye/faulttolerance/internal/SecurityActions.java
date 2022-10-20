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
package io.smallrye.faulttolerance.internal;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class SecurityActions {
    private SecurityActions() {
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
     * Finds a fallback method for given guarded method. If the guarded method is present on given {@code beanClass}
     * and is actually declared by given {@code declaringClass} and has given {@code parameterTypes} and {@code returnType},
     * then a fallback method of given {@code name}, with parameter types and return type matching the parameter types
     * and return type of the guarded method, is searched for on the {@code beanClass} and its superclasses and
     * superinterfaces, according to the specification rules. Returns {@code null} if no matching fallback method exists.
     *
     * @param beanClass the class of the bean that has the guarded method
     * @param declaringClass the class that actually declares the guarded method (can be a supertype of bean class)
     * @param name name of the fallback method
     * @param parameterTypes parameter types of the guarded method
     * @param returnType return type of the guarded method
     * @return the fallback method or {@code null} if none exists
     */
    static Method findFallbackMethod(Class<?> beanClass, Class<?> declaringClass,
            String name, Type[] parameterTypes, Type returnType) throws PrivilegedActionException {

        Set<Method> result;
        if (System.getSecurityManager() == null) {
            result = doFindFallbackMethod(beanClass, declaringClass, name, parameterTypes, returnType, false);
        } else {
            result = AccessController.doPrivileged((PrivilegedExceptionAction<Set<Method>>) () -> {
                return doFindFallbackMethod(beanClass, declaringClass, name, parameterTypes, returnType, false);
            });
        }

        return result.isEmpty() ? null : result.iterator().next();
    }

    /**
     * Finds a set of fallback methods with exception parameter for given guarded method. If the guarded method
     * is present on given {@code beanClass} and is actually declared by given {@code declaringClass} and has given
     * {@code parameterTypes} and {@code returnType}, then fallback methods of given {@code name}, with parameter types
     * and return type matching the parameter types and return type of the guarded method, and with one additional
     * parameter assignable to {@code Throwable} at the end of parameter list, is searched for on the {@code beanClass}
     * and its superclasses and superinterfaces, according to the specification rules. Returns {@code null} if no
     * matching fallback method exists.
     *
     * @param beanClass the class of the bean that has the guarded method
     * @param declaringClass the class that actually declares the guarded method (can be a supertype of bean class)
     * @param name name of the fallback method
     * @param parameterTypes parameter types of the guarded method
     * @param returnType return type of the guarded method
     * @return the fallback method or {@code null} if none exists
     */
    static Set<Method> findFallbackMethodsWithExceptionParammeter(Class<?> beanClass, Class<?> declaringClass,
            String name, Type[] parameterTypes, Type returnType) throws PrivilegedActionException {
        if (System.getSecurityManager() == null) {
            return doFindFallbackMethod(beanClass, declaringClass, name, parameterTypes, returnType, true);
        }
        return AccessController.doPrivileged((PrivilegedExceptionAction<Set<Method>>) () -> {
            return doFindFallbackMethod(beanClass, declaringClass, name, parameterTypes, returnType, true);
        });
    }

    private static Set<Method> doFindFallbackMethod(Class<?> beanClass, Class<?> declaringClass, String name,
            Type[] expectedParameterTypes, Type expectedReturnType, boolean expectedExceptionParameter) {

        Set<Method> result = new HashSet<>();

        TypeMapping expectedMapping = TypeMapping.createFor(beanClass, declaringClass);
        TypeMapping actualMapping = new TypeMapping();

        // if we find a matching method on the bean class or one of its superclasses or superinterfaces,
        // then we have to check that the method is either identical to or an override of a method that:
        // - is declared on a class which is a supertype of the declaring class, or
        // - is declared on an interface which implemented by the declaring class
        //
        // this is to satisfy the specification, which says: fallback method must be on the same class, a superclass
        // or an implemented interface of the class which declares the annotated method
        //
        // we fake this by checking that the matching method has the same name as one of the method declared on
        // the declaring class or any of its superclasses or any of its implemented interfaces (this is actually
        // quite precise, the only false positive would occur in presence of overloads)
        Set<String> possibleFallbackMethodNames = findPossibleFallbackMethodNames(declaringClass);

        Class<?> clazz = beanClass;
        while (true) {
            Set<Method> methods = getMethodsFromClass(declaringClass, clazz, name, expectedParameterTypes,
                    expectedReturnType, expectedExceptionParameter, actualMapping, expectedMapping);
            for (Method method : methods) {
                if (possibleFallbackMethodNames.contains(method.getName())) {
                    result.add(method);
                    if (!expectedExceptionParameter) {
                        return result;
                    }
                }
            }

            if (clazz.getSuperclass() == null) {
                break;
            }

            actualMapping = actualMapping.getSuperclassMapping(clazz);
            clazz = clazz.getSuperclass();
        }

        for (Class<?> iface : beanClass.getInterfaces()) {
            Set<Method> methods = getMethodsFromClass(declaringClass, iface, name, expectedParameterTypes,
                    expectedReturnType, expectedExceptionParameter, actualMapping, expectedMapping);
            for (Method method : methods) {
                if (possibleFallbackMethodNames.contains(method.getName())) {
                    result.add(method);
                    if (!expectedExceptionParameter) {
                        return result;
                    }
                }
            }
        }

        return result;
    }

    private static Set<String> findPossibleFallbackMethodNames(Class<?> declaringClass) {
        Set<String> result = new HashSet<>();

        Class<?> clazz = declaringClass;
        while (clazz != null) {
            for (Method m : clazz.getDeclaredMethods()) {
                result.add(m.getName());
            }
            clazz = clazz.getSuperclass();
        }

        for (Class<?> iface : declaringClass.getInterfaces()) {
            for (Method m : iface.getMethods()) {
                result.add(m.getName());
            }
        }

        return result;
    }

    private static Set<Method> getMethodsFromClass(Class<?> guardedMethodDeclaringClass,
            Class<?> classToSearch, String name, Type[] parameterTypes, Type returnType, boolean exceptionParameter,
            TypeMapping actualMapping, TypeMapping expectedMapping) {
        Set<Method> set = new HashSet<>();
        for (Method method : classToSearch.getDeclaredMethods()) {
            if (isAccessibleFrom(method, guardedMethodDeclaringClass)
                    && method.getName().equals(name)
                    && signaturesMatch(method, parameterTypes, returnType, exceptionParameter,
                            actualMapping, expectedMapping)) {
                set.add(method);
            }
        }
        return set;
    }

    private static boolean isAccessibleFrom(Method method, Class<?> guardedMethodDeclaringClass) {
        if (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers())) {
            return true;
        }
        if (Modifier.isPrivate(method.getModifiers())) {
            return method.getDeclaringClass() == guardedMethodDeclaringClass;
        }
        // not public, not protected and not private => default
        // accessible only if in the same package
        return method.getDeclaringClass().getPackage() == guardedMethodDeclaringClass.getPackage();
    }

    private static boolean signaturesMatch(Method method, Type[] expectedParameterTypes, Type expectedReturnType,
            boolean expectedExceptionParameter, TypeMapping actualMapping, TypeMapping expectedMapping) {
        int expectedParameters = expectedParameterTypes.length;
        if (expectedExceptionParameter) {
            expectedParameters++;
        }

        Type[] methodParams = method.getGenericParameterTypes();
        if (expectedParameters != methodParams.length) {
            return false;
        }

        for (int i = 0; i < expectedParameterTypes.length; i++) {
            if (!typeMatches(methodParams[i], expectedParameterTypes[i], actualMapping, expectedMapping)) {
                return false;
            }
        }

        if (expectedExceptionParameter) {
            Type lastParameter = methodParams[methodParams.length - 1];
            boolean isThrowable = lastParameter instanceof Class
                    && Throwable.class.isAssignableFrom((Class<?>) lastParameter);
            if (!isThrowable) {
                return false;
            }
        }

        if (!typeMatches(method.getGenericReturnType(), expectedReturnType, actualMapping, expectedMapping)) {
            return false;
        }

        return true;
    }

    private static boolean typeMatches(Type actualType, Type expectedType,
            TypeMapping actualMapping, TypeMapping expectedMapping) {
        actualType = actualMapping.map(actualType);
        expectedType = expectedMapping.map(expectedType);

        if (actualType instanceof Class) {
            return expectedType == actualType;
        } else if (isArray(actualType) && isArray(expectedType)) {
            return typeMatches(getArrayComponentType(actualType), getArrayComponentType(expectedType),
                    actualMapping, expectedMapping);
        } else if (actualType instanceof ParameterizedType && expectedType instanceof ParameterizedType) {
            return parameterizedTypeMatches((ParameterizedType) actualType, (ParameterizedType) expectedType,
                    actualMapping, expectedMapping);
        } else if (actualType instanceof WildcardType && expectedType instanceof WildcardType) {
            return wildcardTypeMatches((WildcardType) actualType, (WildcardType) expectedType, actualMapping,
                    expectedMapping);
        } else {
            return false;
        }
    }

    private static boolean wildcardTypeMatches(WildcardType actualType, WildcardType expectedType,
            TypeMapping actualMapping, TypeMapping expectedMapping) {
        boolean lowerBoundsMatch = typeArrayMatches(actualType.getLowerBounds(), expectedType.getLowerBounds(),
                actualMapping, expectedMapping);
        boolean upperBoundsMatch = typeArrayMatches(actualType.getUpperBounds(), expectedType.getUpperBounds(),
                actualMapping, expectedMapping);
        return lowerBoundsMatch && upperBoundsMatch;
    }

    private static boolean parameterizedTypeMatches(ParameterizedType actualType, ParameterizedType expectedType,
            TypeMapping actualMapping, TypeMapping expectedMapping) {
        return typeArrayMatches(actualType.getActualTypeArguments(), expectedType.getActualTypeArguments(),
                actualMapping, expectedMapping);
    }

    private static boolean typeArrayMatches(Type[] actualTypes, Type[] expectedTypes,
            TypeMapping actualMapping, TypeMapping expectedMapping) {
        if (actualTypes.length != expectedTypes.length) {
            return false;
        }
        for (int i = 0; i < actualTypes.length; i++) {
            if (!typeMatches(actualTypes[i], expectedTypes[i], actualMapping, expectedMapping)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Accepts only array {@code Class}es and {@code GenericArrayType}s.
     * In other words, {@code isArray(type)} must be {@code true}.
     */
    private static Type getArrayComponentType(Type type) {
        if (type instanceof Class) {
            return ((Class<?>) type).getComponentType();
        } else if (type instanceof GenericArrayType) {
            return ((GenericArrayType) type).getGenericComponentType();
        } else {
            throw new IllegalArgumentException("Not an array: " + type);
        }
    }

    private static boolean isArray(Type parameterType) {
        if (parameterType instanceof Class) {
            return ((Class<?>) parameterType).isArray();
        } else {
            return parameterType instanceof GenericArrayType;
        }
    }

    private static class TypeMapping {
        private final Map<Type, Type> map;

        private TypeMapping() {
            this.map = Collections.emptyMap();
        }

        private TypeMapping(Map<Type, Type> map) {
            this.map = map;
        }

        /**
         * Bean class can be a subclass of the class that declares the guarded method.
         * This method returns a mapping of the type parameters of the method's declaring class
         * to the type arguments provided on the bean class or any class between it and the declaring class.
         *
         * @param beanClass class of the bean which has the guarded method
         * @param declaringClass class that actually declares the guarded method
         * @return type mapping
         */
        private static TypeMapping createFor(Class<?> beanClass, Class<?> declaringClass) {
            TypeMapping result = new TypeMapping();
            if (beanClass == declaringClass) {
                return result;
            }

            Class<?> current = beanClass;
            while (current != declaringClass && current != null) {
                if (current.getSuperclass() == null) {
                    break;
                }
                result = result.getSuperclassMapping(current);
                current = current.getSuperclass();
            }

            return result;
        }

        private Type map(Type type) {
            Type result = map.get(type);
            return result != null ? result : type;
        }

        private TypeMapping getSuperclassMapping(Class<?> current) {
            return new TypeMapping(mappingForSuperclass(current, this.map));
        }

        private static Map<Type, Type> mappingForSuperclass(Class<?> clazz, Map<Type, Type> previousMapping) {
            Class<?> superclass = clazz.getSuperclass();
            TypeVariable<?>[] typeParameters = superclass.getTypeParameters();

            Type genericSuperclass = clazz.getGenericSuperclass();
            Type[] typeArguments = (genericSuperclass instanceof ParameterizedType)
                    ? ((ParameterizedType) genericSuperclass).getActualTypeArguments()
                    : new Type[0];

            Map<Type, Type> result = new HashMap<>();

            for (int i = 0; i < typeArguments.length; i++) {
                Type typeArgument = typeArguments[i];
                if (typeArgument instanceof Class) {
                    result.put(typeParameters[i], typeArgument);
                } else {
                    Type type = previousMapping.get(typeArgument);
                    result.put(typeParameters[i], type != null ? type : typeArgument);
                }
            }

            return result;
        }
    }
}
