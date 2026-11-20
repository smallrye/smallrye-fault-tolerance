package io.smallrye.faulttolerance.retry.beforeretry.call.context;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.BeforeRetry;
import io.smallrye.faulttolerance.api.BeforeRetryHandler;
import io.smallrye.faulttolerance.api.CallContext;
import io.smallrye.faulttolerance.api.RetryWhen;

@ApplicationScoped
public class BeforeRetryCallContextService {
    static final List<String> methods = new CopyOnWriteArrayList<>();
    static final List<List<Object>> parameters = new CopyOnWriteArrayList<>();
    static final List<Object> results = new CopyOnWriteArrayList<>();
    static final List<Throwable> failures = new CopyOnWriteArrayList<>();
    static final AtomicInteger attempts = new AtomicInteger();

    static void clear() {
        methods.clear();
        parameters.clear();
        results.clear();
        failures.clear();
        attempts.set(0);
    }

    @Retry(maxRetries = 2, delay = 0, jitter = 0)
    @BeforeRetry(RecordingHandler.class)
    public String onFailure(String argument) {
        attempts.incrementAndGet();
        throw new IllegalArgumentException("Simulated failure");
    }

    @Retry(maxRetries = 3, delay = 0, jitter = 0)
    @RetryWhen(result = IsBad.class)
    @BeforeRetry(RecordingHandler.class)
    public String onResult() {
        return attempts.incrementAndGet() < 4 ? "bad" : "good";
    }

    @Retry(maxRetries = 2, delay = 0, jitter = 0)
    @RetryWhen(result = IsNull.class)
    @BeforeRetry(RecordingHandler.class)
    public String onNullResult() {
        return attempts.incrementAndGet() < 3 ? null : "good";
    }

    @Asynchronous
    @Retry(maxRetries = 3, delay = 0, jitter = 0)
    @RetryWhen(result = IsBad.class)
    @BeforeRetry(RecordingHandler.class)
    public CompletionStage<String> onResultAsync() {
        return completedFuture(attempts.incrementAndGet() < 4 ? "bad" : "good");
    }

    @Retry(maxRetries = 3, delay = 0, jitter = 0)
    @RetryWhen(result = IsFalse.class)
    @BeforeRetry(RecordingHandler.class)
    public boolean onPrimitiveResult() {
        return attempts.incrementAndGet() >= 4;
    }

    @Retry(maxRetries = 2, delay = 0, jitter = 0)
    @BeforeRetry(RecordingHandler.class)
    public int onPrimitiveFailure() {
        attempts.incrementAndGet();
        throw new IllegalArgumentException("Simulated failure");
    }

    @Retry(maxRetries = 2, delay = 0, jitter = 0)
    @BeforeRetry(RecordingHandler.class)
    public void onVoidFailure() {
        attempts.incrementAndGet();
        throw new IllegalArgumentException("Simulated failure");
    }

    public static class RecordingHandler implements BeforeRetryHandler {
        @Override
        public void handle(CallContext context) {
            methods.add(context.getMethod().getName());
            parameters.add(Arrays.asList(context.getParameters()));
            results.add(context.getResult());
            failures.add(context.getFailure());
        }
    }
}
