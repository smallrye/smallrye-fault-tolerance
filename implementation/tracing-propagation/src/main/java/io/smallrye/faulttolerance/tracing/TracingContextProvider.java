package io.smallrye.faulttolerance.tracing;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class TracingContextProvider implements ThreadContextProvider {

    private static final ThreadContextController DO_NOTHING = () -> {
    };

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        Tracer tracer = GlobalTracer.get();
        ScopeManager scopeManager = tracer.scopeManager();
        Scope activeScope = scopeManager.active();

        if (activeScope != null) {
            Span span = activeScope.span();
            return () -> {
                Scope propagated = scopeManager.activate(span, false);
                return propagated::close;
            };
        }
        return () -> DO_NOTHING;
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return () -> {
            Tracer tracer = GlobalTracer.get();
            ScopeManager scopeManager = tracer.scopeManager();
            Scope activeScope = scopeManager.active();
            if (activeScope != null) {
                activeScope.close();
            }
            return () -> {
                // TODO: we should bring back the span here
            };
        };
    }

    @Override
    public String getThreadContextType() {
        return "OpenTracing";
    }
}
