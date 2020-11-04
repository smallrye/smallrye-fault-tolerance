package io.smallrye.faulttolerance.async.types.rxjava;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.reactivex.rxjava3.core.Single;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;

@ApplicationScoped
public class HelloService {
    static final AtomicInteger COUNTER = new AtomicInteger(0);

    @NonBlocking
    @Retry(jitter = 0)
    @Fallback(HelloFallback.class)
    public Single<String> helloNonblocking() {
        COUNTER.incrementAndGet();
        return Single.error(IllegalArgumentException::new);
    }

    @Blocking
    @Retry(jitter = 0)
    @Fallback(HelloFallback.class)
    public Single<String> helloBlocking() {
        COUNTER.incrementAndGet();
        return Single.error(IllegalArgumentException::new);
    }

    @Asynchronous
    @Retry(jitter = 0)
    @Fallback(HelloFallback.class)
    public Single<String> helloAsynchronous() {
        COUNTER.incrementAndGet();
        return Single.error(IllegalArgumentException::new);
    }

    @Asynchronous
    @NonBlocking
    @Retry(jitter = 0)
    @Fallback(HelloFallback.class)
    public Single<String> helloAsynchronousNonblocking() {
        COUNTER.incrementAndGet();
        return Single.error(IllegalArgumentException::new);
    }

    @Asynchronous
    @Blocking
    @Retry(jitter = 0)
    @Fallback(HelloFallback.class)
    public Single<String> helloAsynchronousBlocking() {
        COUNTER.incrementAndGet();
        return Single.error(IllegalArgumentException::new);
    }

    public static class HelloFallback implements FallbackHandler<Single<String>> {
        @Override
        public Single<String> handle(ExecutionContext context) {
            return Single.fromSupplier(() -> "hello");
        }
    }
}
