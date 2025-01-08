/*
 * Copyright 2018 Red Hat, Inc, and individual contributors.
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

import java.lang.reflect.Method;
import java.security.PrivilegedActionException;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.config.FaultToleranceMethods;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

/**
 * Default implementation of {@link FaultToleranceOperationProvider}.
 *
 * @author Martin Kouba
 * @see FaultToleranceOperationProvider
 */
@Singleton
public class DefaultFaultToleranceOperationProvider implements FaultToleranceOperationProvider {

    private final FaultToleranceExtension extension;

    @Inject
    public DefaultFaultToleranceOperationProvider(BeanManager beanManager) {
        extension = beanManager.getExtension(FaultToleranceExtension.class);
    }

    @Override
    public FaultToleranceOperation get(Class<?> beanClass, Method method) {
        FaultToleranceOperation operation = null;
        beanClass = adaptBeanClass(beanClass, method);
        if (extension != null) {
            operation = extension.getFaultToleranceOperation(beanClass, method);
        }
        if (operation == null) {
            // This is not a bean method - create metadata on the fly
            operation = new FaultToleranceOperation(FaultToleranceMethods.create(beanClass, method));
            operation.validate();
        }
        return operation;
    }

    protected Class<?> adaptBeanClass(Class<?> beanClass, Method method) {
        if (!beanClass.equals(method.getDeclaringClass()) && !isMethodDeclaredInHierarchy(beanClass, method)) {
            // The class hierarchy does not declare the method - the bean class is probably a proxy-like construct, e.g. MP Rest Client proxy
            return method.getDeclaringClass();
        } else {
            return beanClass;
        }
    }

    protected boolean isMethodDeclaredInHierarchy(Class<?> beanClass, Method method) {
        while (beanClass != null) {
            try {
                for (Method declaredMethod : SecurityActions.getDeclaredMethods(beanClass)) {
                    if (declaredMethod.equals(method)) {
                        return true;
                    }
                }
            } catch (PrivilegedActionException e) {
                throw new FaultToleranceDefinitionException(e);
            }
            beanClass = beanClass.getSuperclass();
        }
        return false;
    }

}
