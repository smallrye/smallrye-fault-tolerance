package io.smallrye.faulttolerance.rxjava3.impl;

import io.reactivex.rxjava3.core.Completable;
import io.smallrye.faulttolerance.core.Completer;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.Invoker;

public class CompletableSupport<T> implements AsyncSupport<T, Completable> {
    @Override
    public String mustDescription() {
        return "return " + Completable.class.getSimpleName();
    }

    @Override
    public String doesDescription() {
        return "returns " + Completable.class.getSimpleName();
    }

    @Override
    public boolean applies(Class<?>[] parameterTypes, Class<?> returnType) {
        return Completable.class.equals(returnType);
    }

    @Override
    public Completable createComplete(T value) {
        return Completable.complete();
    }

    @Override
    public Future<T> toFuture(Invoker<Completable> invoker) {
        Completer<T> completer = Completer.create();
        try {
            invoker.proceed().subscribe(() -> completer.complete(null), completer::completeWithError);
        } catch (Exception e) {
            completer.completeWithError(e);
        }
        return completer.future();
    }

    @Override
    public Completable fromFuture(Invoker<Future<T>> invoker) {
        return Completable.defer(() -> Completable.create(sub -> {
            try {
                invoker.proceed().then((value, error) -> {
                    if (error == null) {
                        sub.onComplete();
                    } else {
                        sub.onError(error);
                    }
                });
            } catch (Exception e) {
                sub.onError(e);
            }
        }));
    }
}
