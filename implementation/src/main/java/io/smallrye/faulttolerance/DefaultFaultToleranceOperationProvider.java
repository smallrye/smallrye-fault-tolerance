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

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import io.smallrye.faulttolerance.config.FaultToleranceOperation;

/**
 * 
 * <p>
 * An integrator is allowed to provide a custom implementation of {@link FaultToleranceOperationProvider}. The bean should be {@link Dependent}, must be marked
 * as alternative and selected globally for an application.
 * </p>
 * 
 * @author Martin Kouba
 */
@Dependent
public class DefaultFaultToleranceOperationProvider implements FaultToleranceOperationProvider {

    private final HystrixExtension extension;

    @Inject
    public DefaultFaultToleranceOperationProvider(BeanManager beanManager) {
        this.extension = beanManager.getExtension(HystrixExtension.class);
    }

    @Override
    public FaultToleranceOperation apply(Method method) {
        String methodKey = method.toGenericString();
        FaultToleranceOperation operation = null;
        if (extension != null) {
            operation = extension.getFaultToleranceOperation(methodKey);
        }
        if (operation == null) {
            // This is not a bean method - create metadata on the fly
            operation = FaultToleranceOperation.of(method);
            operation.validate();
        }
        return operation;
    }

}
