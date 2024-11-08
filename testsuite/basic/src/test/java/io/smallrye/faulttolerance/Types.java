package io.smallrye.faulttolerance;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.util.TypeLiteral;

import io.smallrye.mutiny.Uni;

public class Types {
    public static final TypeLiteral<CompletionStage<String>> CS_STRING = new TypeLiteral<>() {
    };

    public static final TypeLiteral<Uni<String>> UNI_STRING = new TypeLiteral<>() {
    };
}
