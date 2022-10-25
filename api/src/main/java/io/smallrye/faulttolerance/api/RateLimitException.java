/*
 * Copyright 2022 Red Hat, Inc, and individual contributors.
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
package io.smallrye.faulttolerance.api;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

/**
 * The exception thrown when an invocation exceeds the configured rate limit.
 */
public class RateLimitException extends FaultToleranceException {
    public RateLimitException() {
    }

    public RateLimitException(Throwable t) {
        super(t);
    }

    public RateLimitException(String message) {
        super(message);
    }

    public RateLimitException(String message, Throwable t) {
        super(message, t);
    }
}
