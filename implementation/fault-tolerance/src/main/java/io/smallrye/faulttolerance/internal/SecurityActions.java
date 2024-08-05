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
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
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
            result = findMethod(beanClass, declaringClass, name, parameterTypes, returnType, false);
        } else {
            result = AccessController.doPrivileged((PrivilegedExceptionAction<Set<Method>>) () -> {
                return findMethod(beanClass, declaringClass, name, parameterTypes, returnType, false);
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
    static Set<Method> findFallbackMethodsWithExceptionParameter(Class<?> beanClass, Class<?> declaringClass,
            String name, Type[] parameterTypes, Type returnType) throws PrivilegedActionException {
        if (System.getSecurityManager() == null) {
            return findMethod(beanClass, declaringClass, name, parameterTypes, returnType, true);
        }
        return AccessController.doPrivileged((PrivilegedExceptionAction<Set<Method>>) () -> {
            return findMethod(beanClass, declaringClass, name, parameterTypes, returnType, true);
        });
    }

    /**
     * Finds a before retry method for given guarded method. If the guarded method is present on given {@code beanClass}
     * and is actually declared by given {@code declaringClass}, then a before retry method of given {@code name},
     * with no parameters and return type of {@code void}, is searched for on the {@code beanClass} and its superclasses and
     * superinterfaces, according to the specification rules. Returns {@code null} if no matching before retry method exists.
     *
     * @param beanClass the class of the bean that has the guarded method
     * @param declaringClass the class that actually declares the guarded method (can be a supertype of bean class)
     * @param name name of the before retry method
     * @return the before retry method or {@code null} if none exists
     */
    static Method findBeforeRetryMethod(Class<?> beanClass, Class<?> declaringClass, String name)
            throws PrivilegedActionException {
        Set<Method> result;
        if (System.getSecurityManager() == null) {
            result = findMethod(beanClass, declaringClass, name, new Type[0], void.class, false);
        } else {
            result = AccessController.doPrivileged((PrivilegedExceptionAction<Set<Method>>) () -> {
                return findMethod(beanClass, declaringClass, name, new Type[0], void.class, false);
            });
        }

        return result.isEmpty() ? null : result.iterator().next();
    }

    private static Set<Method> findMethod(Class<?> beanClass, Class<?> declaringClass, String name,
            Type[] expectedParameterTypes, Type expectedReturnType, boolean expectedExceptionParameter) {

        Set<Method> result = new HashSet<>();

        TypeMapping expectedMapping = TypeMapping.createFor(beanClass, declaringClass);

        // if we find a matching method on the bean class or one of its superclasses or superinterfaces,
        // then we have to check that the method is either identical to or an override of a method that:
        // - is declared on a class which is a superclass of the declaring class, or
        // - is declared on an interface which implemented by the declaring class
        //
        // this is to satisfy the specification, which says: fallback method must be on the same class, a superclass
        // or an implemented interface of the class which declares the annotated method
        //
        // we fake this by checking that the matching method has the same name as one of the method declared on
        // the declaring class or any of its superclasses or any of its implemented interfaces (this is actually
        // quite precise, the only false positive would occur in presence of overloads)
        Set<String> declaredMethodNames = findDeclaredMethodNames(declaringClass);

        Deque<ClassWithTypeMapping> worklist = new ArrayDeque<>();
        {
            // add all superclasses first, so that they're preferred
            // interfaces are added during worklist iteration
            Class<?> clazz = beanClass;
            TypeMapping typeMapping = new TypeMapping();
            worklist.add(new ClassWithTypeMapping(clazz, typeMapping));
            while (clazz.getSuperclass() != null) {
                Class<?> superclass = clazz.getSuperclass();
                Type genericSuperclass = clazz.getGenericSuperclass();
                typeMapping = typeMapping.getDirectSupertypeMapping(superclass, genericSuperclass);
                worklist.add(new ClassWithTypeMapping(superclass, typeMapping));

                clazz = clazz.getSuperclass();
            }
        }
        while (!worklist.isEmpty()) {
            ClassWithTypeMapping classWithTypeMapping = worklist.removeFirst();
            Class<?> clazz = classWithTypeMapping.clazz;
            TypeMapping actualMapping = classWithTypeMapping.typeMapping;

            Set<Method> methods = getMethodsFromClass(clazz, name, expectedParameterTypes, expectedReturnType,
                    expectedExceptionParameter, declaringClass, actualMapping, expectedMapping);
            for (Method method : methods) {
                if (declaredMethodNames.contains(method.getName())) {
                    result.add(method);
                    if (!expectedExceptionParameter) {
                        return result;
                    }
                }
            }

            for (int i = 0; i < clazz.getInterfaces().length; i++) {
                Class<?> iface = clazz.getInterfaces()[i];
                Type genericIface = clazz.getGenericInterfaces()[i];
                worklist.add(new ClassWithTypeMapping(iface,
                        actualMapping.getDirectSupertypeMapping(iface, genericIface)));
            }
        }

        return result;
    }

    private static Set<String> findDeclaredMethodNames(Class<?> declaringClass) {
        Set<String> result = new HashSet<>();

        Deque<Class<?>> worklist = new ArrayDeque<>();
        worklist.add(declaringClass);
        while (!worklist.isEmpty()) {
            Class<?> clazz = worklist.removeFirst();
            for (Method m : clazz.getDeclaredMethods()) {
                result.add(m.getName());
            }

            if (clazz.getSuperclass() != null) {
                worklist.add(clazz.getSuperclass());
            }
            Collections.addAll(worklist, clazz.getInterfaces());
        }

        return result;
    }

    /**
     * Returns all methods that:
     * <ul>
     * <li>are declared directly on given {@code classToSearch},</li>
     * <li>have given {@code name},</li>
     * <li>have matching {@code parameterTypes},</li>
     * <li>have matching {@code returnType},</li>
     * <li>have an additional {@code exceptionParameter} if required,</li>
     * <li>are accessible from given {@code guardedMethodDeclaringClass}.</li>
     * </ul>
     */
    private static Set<Method> getMethodsFromClass(Class<?> classToSearch, String name, Type[] parameterTypes,
            Type returnType, boolean exceptionParameter, Class<?> guardedMethodDeclaringClass,
            TypeMapping actualMapping, TypeMapping expectedMapping) {
        Set<Method> set = new HashSet<>();
        for (Method method : classToSearch.getDeclaredMethods()) {
            if (method.getName().equals(name)
                    && isAccessibleFrom(method, guardedMethodDeclaringClass)
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
            // need to figure this out _before_ expanding the `expectedParameterTypes` array
            boolean kotlinSuspendingFunction = KotlinSupport.isSuspendingFunction(expectedParameterTypes);
            // adjust `expectedParameterTypes` so that there's one more element on the position
            // where the exception parameter should be, and the value on that position is `null`
            expectedParameterTypes = Arrays.copyOfRange(expectedParameterTypes, 0, expectedParameters + 1);
            if (kotlinSuspendingFunction) {
                expectedParameterTypes[expectedParameters] = expectedParameterTypes[expectedParameters - 1];
                expectedParameterTypes[expectedParameters - 1] = null;
            }
            expectedParameters++;
        }

        Type[] methodParams = method.getGenericParameterTypes();
        if (expectedParameters != methodParams.length) {
            return false;
        }

        for (int i = 0; i < expectedParameters; i++) {
            Type methodParam = methodParams[i];
            Type expectedParamType = expectedParameterTypes[i];
            if (expectedParamType != null) {
                if (!typeMatches(methodParam, expectedParamType, actualMapping, expectedMapping)) {
                    return false;
                }
            } else { // exception parameter
                boolean isThrowable = methodParam instanceof Class
                        && Throwable.class.isAssignableFrom((Class<?>) methodParam);
                if (!isThrowable) {
                    return false;
                }
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
        boolean genericClassMatch = typeMatches(actualType.getRawType(), expectedType.getRawType(),
                actualMapping, expectedMapping);
        boolean typeArgumentsMatch = typeArrayMatches(actualType.getActualTypeArguments(),
                expectedType.getActualTypeArguments(), actualMapping, expectedMapping);
        return genericClassMatch && typeArgumentsMatch;
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

    private static class ClassWithTypeMapping {
        private final Class<?> clazz;
        private final TypeMapping typeMapping;

        private ClassWithTypeMapping(Class<?> clazz, TypeMapping typeMapping) {
            this.clazz = clazz;
            this.typeMapping = typeMapping;
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
                result = result.getDirectSupertypeMapping(current.getSuperclass(), current.getGenericSuperclass());
                current = current.getSuperclass();
            }

            return result;
        }

        private Type map(Type type) {
            Type result = map.get(type);
            return result != null ? result : type;
        }

        private TypeMapping getDirectSupertypeMapping(Class<?> supertype, Type genericSupertype) {
            TypeVariable<?>[] typeParameters = supertype.getTypeParameters();
            Type[] typeArguments = (genericSupertype instanceof ParameterizedType)
                    ? ((ParameterizedType) genericSupertype).getActualTypeArguments()
                    : new Type[0];

            Map<Type, Type> result = new HashMap<>();

            for (int i = 0; i < typeArguments.length; i++) {
                Type typeArgument = typeArguments[i];
                if (typeArgument instanceof Class) {
                    result.put(typeParameters[i], typeArgument);
                } else {
                    Type type = map.get(typeArgument);
                    result.put(typeParameters[i], type != null ? type : typeArgument);
                }
            }

            return new TypeMapping(result);
        }
    }
}
