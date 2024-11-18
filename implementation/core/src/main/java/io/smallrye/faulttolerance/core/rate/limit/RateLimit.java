package io.smallrye.faulttolerance.core.rate.limit;

import static io.smallrye.faulttolerance.core.rate.limit.RateLimitLogger.LOG;
import static io.smallrye.faulttolerance.core.util.Preconditions.check;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import io.smallrye.faulttolerance.api.RateLimitException;
import io.smallrye.faulttolerance.api.RateLimitType;
import io.smallrye.faulttolerance.core.Completer;
import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;

public class RateLimit<V> implements FaultToleranceStrategy<V> {
    private final FaultToleranceStrategy<V> delegate;
    private final String description;

    private final TimeWindow timeWindow;

    public RateLimit(FaultToleranceStrategy<V> delegate, String description, int maxInvocations, long timeWindowInMillis,
            long minSpacingInMillis, RateLimitType type, Stopwatch stopwatch) {
        this.delegate = checkNotNull(delegate, "Rate limit delegate must be set");
        this.description = checkNotNull(description, "Rate limit description must be set");
        checkNotNull(type, "Rate limit type must be set");
        check(maxInvocations, maxInvocations > 0, "Max invocations must be > 0");
        check(timeWindowInMillis, timeWindowInMillis > 0, "Time window length must be > 0");
        check(minSpacingInMillis, minSpacingInMillis >= 0, "Min spacing must be >= 0");
        checkNotNull(stopwatch, "Stopwatch must be set");

        if (type == RateLimitType.FIXED) {
            timeWindow = TimeWindow.createFixed(stopwatch, maxInvocations, timeWindowInMillis, minSpacingInMillis);
        } else if (type == RateLimitType.ROLLING) {
            timeWindow = TimeWindow.createRolling(stopwatch, maxInvocations, timeWindowInMillis, minSpacingInMillis);
        } else if (type == RateLimitType.SMOOTH) {
            timeWindow = TimeWindow.createSmooth(stopwatch, maxInvocations, timeWindowInMillis, minSpacingInMillis);
        } else {
            throw new IllegalArgumentException("Unknown rate limit type: " + type);
        }
    }

    @Override
    public Future<V> apply(FaultToleranceContext<V> ctx) {
        LOG.trace("RateLimit started");
        try {
            Completer<V> result = Completer.create();

            long retryAfter = timeWindow.record();
            if (retryAfter == 0) {
                try {
                    LOG.trace("Task permitted by rate limit");
                    ctx.fireEvent(RateLimitEvents.DecisionMade.PERMITTED);
                    delegate.apply(ctx).thenComplete(result);
                } catch (Exception e) {
                    result.completeWithError(e);
                }
            } else {
                LOG.debugf("%s rate limit exceeded", description);
                ctx.fireEvent(RateLimitEvents.DecisionMade.REJECTED);
                result.completeWithError(new RateLimitException(retryAfter, description + " rate limit exceeded"));
            }

            return result.future();
        } finally {
            LOG.trace("RateLimit finished");
        }
    }
}
