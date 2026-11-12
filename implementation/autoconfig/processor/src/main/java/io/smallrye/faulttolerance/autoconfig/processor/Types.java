package io.smallrye.faulttolerance.autoconfig.processor;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

import io.smallrye.jdeparser.Type;

final class Types {
    static final Type ANNOTATION = Type.of(Annotation.class);
    static final Type CLASS = Type.of(Class.class).typeArg(Type.WILDCARD);
    static final Type CLASS_OF_ANNOTATION = Type.of(Class.class).typeArg(ANNOTATION.wildcardExtends());
    static final Type OVERRIDE = Type.of(Override.class);

    static final Type MP_CONFIG = Type.named("org.eclipse.microprofile.config.Config");
    static final Type MP_CONFIG_PROVIDER = Type.named("org.eclipse.microprofile.config.ConfigProvider");

    static final Type FT_DEFINITION_EXCEPTION = Type
            .named("org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException");

    static final Type METHOD_DESCRIPTOR = Type.named("io.smallrye.faulttolerance.autoconfig.MethodDescriptor");
    static final Type FAULT_TOLERANCE_METHOD = Type.named("io.smallrye.faulttolerance.autoconfig.FaultToleranceMethod");
    static final Type CONFIG_UTIL = Type.named("io.smallrye.faulttolerance.apiimpl.basicconfig.ConfigUtil");

    static Type supplierOf(Type type) {
        return Type.of(Supplier.class).typeArg(type);
    }
}
