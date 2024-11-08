package io.smallrye.faulttolerance.standalone.test;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.util.TypeLiteral;

class Types {
    static final TypeLiteral<CompletionStage<String>> CS_STRING = new TypeLiteral<>() {
    };

    static final TypeLiteral<CompletionStage<Integer>> CS_INTEGER = new TypeLiteral<>() {
    };
}
