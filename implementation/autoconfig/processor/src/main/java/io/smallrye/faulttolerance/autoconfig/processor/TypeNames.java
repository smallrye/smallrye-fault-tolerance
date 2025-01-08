package io.smallrye.faulttolerance.autoconfig.processor;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.function.Supplier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;

final class TypeNames {
    static final TypeName ANNOTATION = ClassName.get(Annotation.class);
    static final TypeName CLASS = ParameterizedTypeName.get(
            ClassName.get(Class.class), WildcardTypeName.subtypeOf(TypeName.OBJECT));
    static final TypeName CLASS_OF_ANNOTATION = ParameterizedTypeName.get(
            ClassName.get(Class.class), WildcardTypeName.subtypeOf(ANNOTATION));
    static final TypeName STRING = ClassName.get(String.class);
    static final TypeName HASHMAP_OF_STRING_TO_STRING = ParameterizedTypeName.get(ClassName.get(HashMap.class), STRING, STRING);

    static final TypeName MP_CONFIG = ClassName.get("org.eclipse.microprofile.config", "Config");
    static final TypeName MP_CONFIG_PROVIDER = ClassName.get("org.eclipse.microprofile.config", "ConfigProvider");

    static final TypeName FT_DEFINITION_EXCEPTION = ClassName.get("org.eclipse.microprofile.faulttolerance.exceptions",
            "FaultToleranceDefinitionException");

    static final TypeName METHOD_DESCRIPTOR = ClassName.get("io.smallrye.faulttolerance.autoconfig",
            "MethodDescriptor");
    static final TypeName FAULT_TOLERANCE_METHOD = ClassName.get("io.smallrye.faulttolerance.autoconfig",
            "FaultToleranceMethod");

    static final TypeName CONFIG_UTIL = ClassName.get("io.smallrye.faulttolerance.basicconfig", "ConfigUtil");

    static TypeName supplierOf(TypeName type) {
        return ParameterizedTypeName.get(ClassName.get(Supplier.class), type);
    }
}
