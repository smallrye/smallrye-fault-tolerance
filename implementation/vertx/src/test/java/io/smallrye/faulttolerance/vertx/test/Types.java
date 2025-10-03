package io.smallrye.faulttolerance.vertx.test;

import jakarta.enterprise.util.TypeLiteral;

import io.vertx.core.Future;

class Types {
    static final TypeLiteral<Future<String>> FUTURE_STRING = new TypeLiteral<>() {
    };

    static final TypeLiteral<Future<Integer>> FUTURE_INTEGER = new TypeLiteral<>() {
    };
}
