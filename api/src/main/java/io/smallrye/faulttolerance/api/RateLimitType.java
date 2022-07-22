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

/**
 * Type of the time window used for rate limiting.
 *
 * @see #FIXED
 * @see #ROLLING
 */
public enum RateLimitType {
    /**
     * Divides time into non-overlapping intervals of given length (time windows) and enforces the invocation
     * limit for each interval independently. This means that short bursts of invocations occuring near
     * the time window boundaries may temporarily exceed the configured rate limit.
     * <p>
     * This type of time windows requires constant memory and time.
     */
    FIXED,

    /**
     * Enforces the limit continuously, instead of dividing time into independent windows. The invocation
     * limit is enforced for all possible time intervals of given length, regardless of overlap.
     * <p>
     * This type of time windows must remember timestamps of recent invocations and so requires
     * memory and time proportional to the maximum number of invocations in the time window.
     */
    ROLLING,
}
