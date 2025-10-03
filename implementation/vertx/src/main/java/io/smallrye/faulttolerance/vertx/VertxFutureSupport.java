package io.smallrye.faulttolerance.vertx;

import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.Invoker;

public class VertxFutureSupport<T> implements AsyncSupport<T, io.vertx.core.Future<T>> {
    @Override
    public String mustDescription() {
        return "return Vert.x " + io.vertx.core.Future.class.getSimpleName();
    }

    @Override
    public String doesDescription() {
        return "returns Vert.x " + io.vertx.core.Future.class.getSimpleName();
    }

    @Override
    public boolean applies(Class<?>[] parameterTypes, Class<?> returnType) {
        return io.vertx.core.Future.class.equals(returnType);
    }

    @Override
    public io.vertx.core.Future<T> createComplete(T value) {
        return io.vertx.core.Future.succeededFuture(value);
    }

    @Override
    public io.smallrye.faulttolerance.core.Future<T> toFuture(Invoker<io.vertx.core.Future<T>> invoker) {
        io.smallrye.faulttolerance.core.Completer<T> completer = io.smallrye.faulttolerance.core.Completer.create();
        try {
            invoker.proceed().onComplete(completer::complete, completer::completeWithError);
        } catch (Exception e) {
            completer.completeWithError(e);
        }
        return completer.future();
    }

    @Override
    public io.vertx.core.Future<T> fromFuture(Invoker<io.smallrye.faulttolerance.core.Future<T>> invoker) {
        io.vertx.core.Promise<T> promise = io.vertx.core.Promise.promise();
        try {
            invoker.proceed().then((value, error) -> {
                if (error == null) {
                    promise.complete(value);
                } else {
                    promise.fail(error);
                }
            });
        } catch (Exception e) {
            promise.fail(e);
        }
        return promise.future();
    }
}
