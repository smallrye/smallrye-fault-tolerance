package io.smallrye.faulttolerance.mutiny.impl;

import io.smallrye.faulttolerance.core.Completer;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.Invoker;
import io.smallrye.mutiny.Uni;

public class UniSupport<T> implements AsyncSupport<T, Uni<T>> {
    @Override
    public String mustDescription() {
        return "return " + Uni.class.getSimpleName();
    }

    @Override
    public String doesDescription() {
        return "returns " + Uni.class.getSimpleName();
    }

    @Override
    public boolean applies(Class<?>[] parameterTypes, Class<?> returnType) {
        return Uni.class.equals(returnType);
    }

    @Override
    public Uni<T> createComplete(T value) {
        return Uni.createFrom().item(value);
    }

    @Override
    public Future<T> toFuture(Invoker<Uni<T>> invoker) {
        Completer<T> completer = Completer.create();
        try {
            invoker.proceed().subscribe().with(completer::complete, completer::completeWithError);
        } catch (Exception e) {
            completer.completeWithError(e);
        }
        return completer.future();
    }

    @Override
    public Uni<T> fromFuture(Invoker<Future<T>> invoker) {
        return Uni.createFrom().emitter(em -> {
            try {
                invoker.proceed().then((value, error) -> {
                    if (error == null) {
                        em.complete(value);
                    } else {
                        em.fail(error);
                    }
                });
            } catch (Exception e) {
                em.fail(e);
            }
        });
    }
}
