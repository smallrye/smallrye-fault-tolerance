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
 * @see #SMOOTH
 */
public enum RateLimitType {
    /**
     * Divides time into non-overlapping intervals of given length (time windows) and enforces the invocation
     * limit for each interval independently. This means that short bursts of invocations occuring near
     * the time window boundaries may temporarily exceed the configured rate limit.
     * <p>
     * This is also called <em>fixed window</em> rate limiting.
     * <p>
     * Requires constant memory and time.
     */
    FIXED,

    /**
     * Enforces the limit continuously, instead of dividing time into independent windows. The invocation
     * limit is enforced for all possible time intervals of given length, regardless of overlap.
     * <p>
     * This is also called <em>sliding log</em> rate limiting.
     * <p>
     * Requires memory and time proportional to the maximum number of invocations in the time window,
     * due to the need to store and check timestamps of recent invocations.
     */
    ROLLING,

    /**
     * Calculates the maximum rate of invocations from given time window length and given limit and
     * enforces a uniform distribution of invocations over time under the calculated rate. If recent
     * rate of invocations is under the limit, a subsequent burst of invocations is allowed during
     * a shorter time span, but the calculated rate is never exceeded.
     * <p>
     * This is also called <em>token bucket</em> or <em>leaky bucket (as a meter)</em> rate limiting,
     * with the additional property that all work units are considered to have the same size.
     * <p>
     * Requires constant memory and time.
     */
    SMOOTH,
}
