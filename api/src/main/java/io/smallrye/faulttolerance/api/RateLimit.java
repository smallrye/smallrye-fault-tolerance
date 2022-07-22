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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

import io.smallrye.common.annotation.Experimental;

/**
 * Rate limit restricts the number of invocations in a time window of some length. An invocation is permitted
 * when the number of recent invocations is below the configured limit. An invocation that would cause the number
 * of recent invocations to exceed the limit is rejected with {@link RateLimitException}.
 * <p>
 * What "recent" means differs based on the type of the time windows configured. By default, the time windows
 * are {@linkplain RateLimitType#FIXED fixed}, which means that time is divided into a series of consecutive intervals of
 * given length (time windows) and the limit is compared against the number of invocations in the current window.
 * {@linkplain RateLimitType#ROLLING Rolling} time windows may be configured, which means that each invocation has its
 * own time window of given length. This is more precise but requires more memory and may be slower.
 * <p>
 * Additionally, a minimum spacing of invocations may be configured. If set, an invocation that happens too
 * quickly after a previous invocation is always rejected with {@link RateLimitException}, even if the limit
 * has not been exceeded yet.
 * <p>
 * A rejected invocation always counts towards the limit, so if a caller continuously invokes the guarded method
 * faster than the configuration allows, all invocations are rejected until the caller slows down.
 *
 * @see #value()
 * @see #window()
 * @see #windowUnit()
 * @see #minSpacing()
 * @see #minSpacingUnit()
 * @see #type()
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Documented
@InterceptorBinding
@Target({ ElementType.METHOD, ElementType.TYPE })
@Experimental("first attempt at providing rate limit")
public @interface RateLimit {
    /**
     * Maximum number of invocations in a time window.
     * <p>
     * Value must be greater than {@code 0}.
     *
     * @return maximum number of invocations in a time window
     * @see #window()
     */
    @Nonbinding
    int value() default 100;

    /**
     * The time window length.
     * <p>
     * Value must be greater than {@code 0}.
     *
     * @return the time window length
     * @see #value()
     * @see #windowUnit()
     */
    @Nonbinding
    long window() default 1;

    /**
     * The unit of length of a time window.
     *
     * @return the unit of length
     * @see #window()
     */
    @Nonbinding
    ChronoUnit windowUnit() default ChronoUnit.SECONDS;

    /**
     * Minimum time between two consecutive invocations.
     * If the time between two consecutive invocations is shorter, the second invocation is rejected.
     * <p>
     * Value must be greater than or equal to {@code 0}.
     * When {@code 0}, the minimum time check is effectively disabled.
     *
     * @return minimum time between two consecutive invocations
     * @see #minSpacingUnit()
     */
    @Nonbinding
    long minSpacing() default 0;

    /**
     * The unit of the minimum time between two consecutive invocations.
     *
     * @return the unit of minimum time
     * @see #minSpacing()
     */
    @Nonbinding
    ChronoUnit minSpacingUnit() default ChronoUnit.SECONDS;

    /**
     * Type of time windows used for rate limiting.
     *
     * @return the time window type
     * @see RateLimitType
     */
    @Nonbinding
    RateLimitType type() default RateLimitType.FIXED;
}
