package io.smallrye.faulttolerance.autoconfig.processor;

import java.lang.annotation.Annotation;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;

final class TypeNames {
    static final TypeName CLASS = ParameterizedTypeName.get(
            ClassName.get(Class.class), WildcardTypeName.subtypeOf(TypeName.OBJECT));
    static final TypeName CLASS_OF_ANNOTATION = ParameterizedTypeName.get(
            ClassName.get(Class.class), WildcardTypeName.subtypeOf(ClassName.get(Annotation.class)));
    static final TypeName STRING = ClassName.get(String.class);

    static final TypeName MP_CONFIG = ClassName.get("org.eclipse.microprofile.config", "Config");
    static final TypeName MP_CONFIG_PROVIDER = ClassName.get("org.eclipse.microprofile.config", "ConfigProvider");

    static final TypeName CONFIG = ClassName.get("io.smallrye.faulttolerance.autoconfig",
            "Config");
    static final TypeName METHOD_DESCRIPTOR = ClassName.get("io.smallrye.faulttolerance.autoconfig",
            "MethodDescriptor");
    static final TypeName FAULT_TOLERANCE_METHOD = ClassName.get("io.smallrye.faulttolerance.autoconfig",
            "FaultToleranceMethod");
}
