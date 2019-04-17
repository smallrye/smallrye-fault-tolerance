package io.smallrye.faulttolerance.tracing;

import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.Tracer;
import java.util.concurrent.Callable;

/**
 * This strategy configures Hystrix to propagate tracing context (Spans) across threads.
 */
public class TracingConcurrencyStrategy extends HystrixConcurrencyStrategy {

    private HystrixConcurrencyStrategy delegateStrategy;
    private Tracer tracer;

    public TracingConcurrencyStrategy(HystrixConcurrencyStrategy delegateStrategy, Tracer tracer) {
          this.tracer = tracer;
          this.delegateStrategy = delegateStrategy;
    }

    @Override
    public <T> Callable<T> wrapCallable(Callable<T> callable) {
        if (callable instanceof OpenTracingHystrixCallable) {
            return callable;
        }

        Callable<T> delegateCallable = this.delegateStrategy == null ? callable : this.delegateStrategy.wrapCallable(callable);

        if (delegateCallable instanceof OpenTracingHystrixCallable) {
            return delegateCallable;
        }

        if (tracer.scopeManager().active() == null) {
            return delegateCallable;
        }

        return new OpenTracingHystrixCallable<>(delegateCallable, tracer.scopeManager(), tracer.scopeManager().active().span());
    }

    private static class OpenTracingHystrixCallable<S> implements Callable<S> {
        private final Callable<S> delegateCallable;
        private ScopeManager scopeManager;
        private Span span;

        public OpenTracingHystrixCallable(Callable<S> delegate, ScopeManager scopeManager, Span span) {
            if (span == null || delegate == null || scopeManager == null) {
                throw new NullPointerException();
            }
            this.delegateCallable = delegate;
            this.scopeManager = scopeManager;
            this.span = span;
        }

        @Override
        public S call() throws Exception {
            try (Scope scope = scopeManager.activate(span, false)) {
                return delegateCallable.call();
            }
        }
    }
}
