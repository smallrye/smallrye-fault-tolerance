package io.smallrye.faulttolerance.vertx;

import java.util.UUID;

import io.vertx.core.Context;
import io.vertx.core.impl.ContextInternal;

// assumes that verticles are not used and no context is created explicitly,
// which means that all contexts are event loop contexts
public class VertxContext {
    private final ContextInternal context;

    public static VertxContext current() {
        return new VertxContext(ContextInternal.current());
    }

    private VertxContext(ContextInternal context) {
        this.context = context;
    }

    public VertxContext duplicate() {
        return new VertxContext(context.duplicate());
    }

    public void execute(ExecutionStyle style, Runnable runnable) {
        switch (style) {
            case EVENT_LOOP:
                context.runOnContext(ignored -> {
                    runnable.run();
                });
                break;
            case WORKER:
                context.executeBlocking(() -> {
                    runnable.run();
                    return null;
                });
                break;
            default:
                throw new UnsupportedOperationException("" + style);
        }
    }

    public void setTimer(long delayInMillis, Runnable runnable) {
        boolean moveToWorker = Context.isOnWorkerThread();
        context.setTimer(delayInMillis, ignored -> {
            if (moveToWorker) {
                context.executeBlocking(() -> {
                    runnable.run();
                    return null;
                });
            } else {
                runnable.run();
            }
        });
    }

    public ContextDescription describe() {
        String uuid = context.getLocal("my-uuid");
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            context.putLocal("my-uuid", uuid);
        }

        ExecutionStyle executionStyle = Context.isOnEventLoopThread()
                ? ExecutionStyle.EVENT_LOOP
                : (Context.isOnWorkerThread() ? ExecutionStyle.WORKER : ExecutionStyle.UNKNOWN);

        return new ContextDescription(executionStyle, context.getClass().getSimpleName(), uuid,
                "" + System.identityHashCode(context));
    }
}
