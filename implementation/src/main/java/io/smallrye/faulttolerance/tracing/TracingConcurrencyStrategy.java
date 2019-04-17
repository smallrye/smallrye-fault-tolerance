package io.smallrye.faulttolerance.tracing;

import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.Tracer;
import java.util.concurrent.Callable;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

/**
 * This strategy configures Hystrix to propagate tracing context (Spans) across threads.
 */
public class TracingConcurrencyStrategy extends HystrixConcurrencyStrategy {
    private static Logger log = Logger.getLogger(TracingConcurrencyStrategy.class.getName());

    private HystrixConcurrencyStrategy delegateStrategy;
    private Tracer tracer;

    public static TracingConcurrencyStrategy register(Tracer tracer) {
        return new TracingConcurrencyStrategy(tracer);
    }

    private TracingConcurrencyStrategy(Tracer tracer) {
        this.tracer = tracer;
        try {
            this.delegateStrategy = HystrixPlugins.getInstance().getConcurrencyStrategy();
            if (this.delegateStrategy instanceof TracingConcurrencyStrategy) {
                return;
            }

            HystrixCommandExecutionHook commandExecutionHook = HystrixPlugins.getInstance().getCommandExecutionHook();
            HystrixEventNotifier eventNotifier = HystrixPlugins.getInstance().getEventNotifier();
            HystrixMetricsPublisher metricsPublisher = HystrixPlugins.getInstance().getMetricsPublisher();
            HystrixPropertiesStrategy propertiesStrategy = HystrixPlugins.getInstance().getPropertiesStrategy();

            HystrixPlugins.reset();
            HystrixPlugins.getInstance().registerConcurrencyStrategy(this);
            HystrixPlugins.getInstance().registerCommandExecutionHook(commandExecutionHook);
            HystrixPlugins.getInstance().registerEventNotifier(eventNotifier);
            HystrixPlugins.getInstance().registerMetricsPublisher(metricsPublisher);
            HystrixPlugins.getInstance().registerPropertiesStrategy(propertiesStrategy);
        } catch (Exception ex) {
            log.log(Level.TRACE, "Failed to register " + TracingConcurrencyStrategy.class +
                    ", to HystrixPlugins", ex);
        }
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
