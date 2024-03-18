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
    private final long retryAfterMillis;

    public RateLimitException() {
        super();
        this.retryAfterMillis = -1;
    }

    public RateLimitException(long retryAfterMillis) {
        super();
        this.retryAfterMillis = retryAfterMillis;
    }

    public RateLimitException(Throwable t) {
        super(t);
        this.retryAfterMillis = -1;
    }

    public RateLimitException(long retryAfterMillis, Throwable t) {
        super(t);
        this.retryAfterMillis = retryAfterMillis;
    }

    public RateLimitException(String message) {
        super(message);
        this.retryAfterMillis = -1;
    }

    public RateLimitException(long retryAfterMillis, String message) {
        super(message);
        this.retryAfterMillis = retryAfterMillis;
    }

    public RateLimitException(String message, Throwable t) {
        super(message, t);
        this.retryAfterMillis = -1;
    }

    public RateLimitException(long retryAfterMillis, String message, Throwable t) {
        super(message, t);
        this.retryAfterMillis = retryAfterMillis;
    }

    /**
     * Returns the number of milliseconds after which the user may retry the rejected invocation.
     * Retrying sooner is guaranteed to be rejected again. Note that this information is accurate
     * only at the time the invocation is rejected. It may be invalidated by any subsequent
     * or concurrent invocations, so there is no guarantee that a retry attempt after the given
     * number of milliseconds will in fact be permitted.
     * <p>
     * Returns a negative number when the information is not known.
     *
     * @return the minimum number of milliseconds after which retrying makes sense
     */
    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }
}
