package io.smallrye.faulttolerance.rxjava3.impl;

import io.reactivex.rxjava3.core.Single;
import io.smallrye.faulttolerance.core.Completer;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.Invoker;

public class SingleSupport<T> implements AsyncSupport<T, Single<T>> {
    @Override
    public String mustDescription() {
        return "return " + Single.class.getSimpleName();
    }

    @Override
    public String doesDescription() {
        return "returns " + Single.class.getSimpleName();
    }

    @Override
    public boolean applies(Class<?>[] parameterTypes, Class<?> returnType) {
        return Single.class.equals(returnType);
    }

    @Override
    public Single<T> createComplete(T value) {
        return Single.just(value);
    }

    @Override
    public Future<T> toFuture(Invoker<Single<T>> invoker) {
        Completer<T> completer = Completer.create();
        try {
            invoker.proceed().subscribe(completer::complete, completer::completeWithError);
        } catch (Exception e) {
            completer.completeWithError(e);
        }
        return completer.future();
    }

    @Override
    public Single<T> fromFuture(Invoker<Future<T>> invoker) {
        return Single.defer(() -> Single.create(sub -> {
            try {
                invoker.proceed().then((value, error) -> {
                    if (error == null) {
                        sub.onSuccess(value);
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
