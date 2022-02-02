package io.smallrye.faulttolerance.config;

import java.util.StringJoiner;
import java.util.concurrent.Future;

import io.smallrye.faulttolerance.core.async.types.AsyncTypeConverter;
import io.smallrye.faulttolerance.core.async.types.AsyncTypes;

final class AsyncValidation {
    static boolean isAcceptableReturnType(Class<?> returnType) {
        return Future.class.equals(returnType) || AsyncTypes.isKnown(returnType);
    }

    static String describeKnownAsyncTypes() {
        StringJoiner result = new StringJoiner(" or ");
        for (AsyncTypeConverter<?, ?> converter : AsyncTypes.allKnown()) {
            result.add(converter.type().getName());
        }
        return result.toString();
    }
}
