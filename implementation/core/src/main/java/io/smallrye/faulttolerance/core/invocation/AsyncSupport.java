package io.smallrye.faulttolerance.core.invocation;

import io.smallrye.faulttolerance.core.Future;

// V = value type, e.g. String
// AT = async type that eventually produces V, e.g. CompletionStage<String> or Uni<String>
public interface AsyncSupport<V, AT> {
    String mustDescription();

    String doesDescription();

    boolean applies(Class<?>[] parameterTypes, Class<?> returnType);

    AT createComplete(V value);

    Future<V> toFuture(Invoker<AT> invoker);

    AT fromFuture(Invoker<Future<V>> invoker);
}
