package io.smallrye.faulttolerance.rxjava3.impl;

import io.reactivex.rxjava3.core.Maybe;
import io.smallrye.faulttolerance.core.Completer;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.Invoker;

public class MaybeSupport<T> implements AsyncSupport<T, Maybe<T>> {
    @Override
    public String mustDescription() {
        return "return " + Maybe.class.getSimpleName();
    }

    @Override
    public String doesDescription() {
        return "returns " + Maybe.class.getSimpleName();
    }

    @Override
    public boolean applies(Class<?>[] parameterTypes, Class<?> returnType) {
        return Maybe.class.equals(returnType);
    }

    @Override
    public Maybe<T> createComplete(T value) {
        return Maybe.just(value);
    }

    @Override
    public Future<T> toFuture(Invoker<Maybe<T>> invoker) {
        Completer<T> completer = Completer.create();
        try {
            invoker.proceed().subscribe(completer::complete, completer::completeWithError);
        } catch (Exception e) {
            completer.completeWithError(e);
        }
        return completer.future();
    }

    @Override
    public Maybe<T> fromFuture(Invoker<Future<T>> invoker) {
        return Maybe.defer(() -> Maybe.create(sub -> {
            try {
                invoker.proceed().then((value, error) -> {
                    if (error == null && value == null) {
                        sub.onComplete();
                    } else if (error == null) {
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
