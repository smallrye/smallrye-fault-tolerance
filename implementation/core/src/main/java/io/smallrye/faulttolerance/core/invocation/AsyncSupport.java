package io.smallrye.faulttolerance.core.invocation;

import java.util.concurrent.CompletionStage;

// V = value type, e.g. String
// AT = async type that eventually produces V, e.g. CompletionStage<String> or Uni<String>
public interface AsyncSupport<V, AT> {
    String description();

    boolean applies(Class<?>[] parameterTypes, Class<?> returnType);

    CompletionStage<V> toCompletionStage(Invoker<AT> invoker) throws Exception;

    AT fromCompletionStage(Invoker<CompletionStage<V>> invoker) throws Exception;

    // ---

    // only used for converting the return value of `FallbackHandler.handle()`,
    // do not use elsewhere!
    CompletionStage<V> fallbackResultToCompletionStage(AT asyncValue);
}
