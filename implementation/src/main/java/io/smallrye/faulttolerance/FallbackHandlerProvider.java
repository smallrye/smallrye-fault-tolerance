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

import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import io.smallrye.faulttolerance.config.FaultToleranceOperation;

/**
 * 
 * @author Martin Kouba
 */
public interface FallbackHandlerProvider {

    /**
     * 
     * @param operation Fault tolerance operation
     * @return a new fallback handler or {@code null}
     */
    <T> FallbackHandler<T> get(FaultToleranceOperation operation);

}
