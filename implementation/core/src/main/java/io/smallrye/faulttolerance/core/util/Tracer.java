package io.smallrye.faulttolerance.core.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jboss.logging.Logger;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

/**
 * Debugging utility to show how the chain of fault tolerance strategies progresses.
 * This should eventually be replaced by some form of debug logging present directly in the code.
 */
public class Tracer<V> implements FaultToleranceStrategy<V> {

    private static final Logger log = Logger.getLogger(Tracer.class);

    private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private final FaultToleranceStrategy<V> delegate;

    public Tracer(FaultToleranceStrategy<V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug(df.format(new Date()) + " [" + Thread.currentThread().getName() + "] invoking "
                    + delegate.getClass().getSimpleName());
        }
        try {
            return delegate.apply(ctx);
        } finally {
            if (log.isDebugEnabled()) {
                log.debug(df.format(new Date()) + " [" + Thread.currentThread().getName() + "] finished "
                        + delegate.getClass().getSimpleName());
            }
        }
    }
}
