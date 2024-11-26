package io.smallrye.faulttolerance.async.types.rxjava;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.reactivex.rxjava3.core.Single;
import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;

@ApplicationScoped
public class HelloService {
    static final AtomicInteger COUNTER = new AtomicInteger(0);

    @Asynchronous
    @Retry(jitter = 0)
    @Fallback(HelloFallback.class)
    public Single<String> helloAsynchronous() {
        COUNTER.incrementAndGet();
        return Single.error(IllegalArgumentException::new);
    }

    @AsynchronousNonBlocking
    @Retry(jitter = 0)
    @Fallback(HelloFallback.class)
    public Single<String> helloAsynchronousNonBlocking() {
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
