package io.smallrye.faulttolerance.mutiny.test;

import jakarta.enterprise.util.TypeLiteral;

import io.smallrye.mutiny.Uni;

class Types {
    static final TypeLiteral<Uni<String>> UNI_STRING = new TypeLiteral<>() {
    };

    static final TypeLiteral<Uni<Integer>> UNI_INTEGER = new TypeLiteral<>() {
    };
}
