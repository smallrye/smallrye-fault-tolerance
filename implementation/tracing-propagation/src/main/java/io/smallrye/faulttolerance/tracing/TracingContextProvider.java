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

public class TracingContextProvider implements ThreadContextProvider {
    private static final ThreadContextController DO_NOTHING = () -> {
    };

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        Tracer tracer = GlobalTracer.get();
        ScopeManager scopeManager = tracer.scopeManager();
        Span span = scopeManager.activeSpan();

        if (span != null) {
            return () -> {
                Scope propagated = scopeManager.activate(span);
                return propagated::close;
            };
        }

        return () -> DO_NOTHING;
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        // The OpenTracing API is apparently meant to be used in a way that
        // never leaves scopes active. That is generally fine, but the API
        // also doesn't provide a way to guard against misuse. That's because
        // since OpenTracing 0.33, it is impossible to find "current" scope.
        // So here, we just assume that the API is used correctly and there
        // are no "dangling" scopes.
        //
        // Note that "active scope" is different from "active span". Span
        // can be active for a long time, but once finished, it can't be
        // reactivated. During the time a span is active, there are generally
        // several slices of time when you actually work with it, and these
        // slices are delimited by scopes. (In other words, creating a scope
        // is akin to resuming a "paused" span, and closing a scope is akin
        // to suspending a "running" span.)

        return () -> DO_NOTHING;
    }

    @Override
    public String getThreadContextType() {
        return "OpenTracing";
    }
}
