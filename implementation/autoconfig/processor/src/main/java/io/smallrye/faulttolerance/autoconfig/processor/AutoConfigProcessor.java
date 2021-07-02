package io.smallrye.faulttolerance.autoconfig.processor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.tools.Diagnostic;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import io.smallrye.faulttolerance.autoconfig.AutoConfig;

@SupportedAnnotationTypes("io.smallrye.faulttolerance.autoconfig.AutoConfig")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AutoConfigProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            return doProcess(annotations, roundEnv);
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unexpected error: " + e.getMessage());
            return false;
        }
    }

    private boolean doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) throws IOException {
        if (!annotations.isEmpty()) {
            // this processor only supports 1 annotation, so this is it
            TypeElement generateConfigForAnnotation = annotations.iterator().next();

            for (Element annotated : roundEnv.getElementsAnnotatedWith(generateConfigForAnnotation)) {
                // the annotation can only be put on types, so the cast here is safe
                processConfigClass((TypeElement) annotated);
            }
        }

        return false;
    }

    private void processConfigClass(TypeElement configClass) throws IOException {
        if (configClass.getKind() != ElementKind.INTERFACE) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@AutoConfig type " + configClass + " must be an interface");
            return;
        }

        if (configClass.getNestingKind().isNested()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@AutoConfig interface " + configClass + " must be top-level");
            return;
        }

        List<? extends TypeMirror> interfaces = configClass.getInterfaces();
        if (interfaces.size() != 2) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@AutoConfig interface " + configClass + " must have 2 super-interfaces: the annotation type"
                            + " and io.smallrye.faulttolerance.autoconfig.Config");
            return;
        }

        Optional<TypeElement> annotationDeclaration = configClass.getInterfaces()
                .stream()
                .filter(it -> it.getKind() == TypeKind.DECLARED)
                .map(it -> ((DeclaredType) it).asElement())
                .filter(it -> it.getKind() == ElementKind.ANNOTATION_TYPE)
                .map(it -> (TypeElement) it)
                .findAny();

        if (!annotationDeclaration.isPresent()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@AutoConfig interface " + configClass + " must extend the annotation type");
            return;
        }

        generateConfigImpl(configClass, annotationDeclaration.get());
    }

    private void generateConfigImpl(TypeElement configClass, TypeElement annotationDeclaration) throws IOException {
        boolean configurable = configClass.getAnnotation(AutoConfig.class).configurable();

        String packageName = configClass.getQualifiedName().toString().replace("." + configClass.getSimpleName(), "");
        ClassName configImplClassName = ClassName.get(packageName, configClass.getSimpleName() + "Impl");

        TypeName annotationType = TypeName.get(annotationDeclaration.asType());

        JavaFile.builder(packageName, TypeSpec.classBuilder(configImplClassName)
                .addOriginatingElement(configClass)
                .addJavadoc("Automatically generated from the {@link $L} config interface, do not modify.",
                        configClass.getSimpleName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(configClass.asType())
                .addField(TypeNames.CLASS, "beanClass", Modifier.PRIVATE, Modifier.FINAL)
                .addField(TypeNames.METHOD_DESCRIPTOR, "method", Modifier.PRIVATE, Modifier.FINAL)
                .addField(FieldSpec.builder(annotationType, "instance", Modifier.PRIVATE, Modifier.FINAL)
                        .addJavadoc(configurable
                                ? "Backing annotation instance. Used when runtime configuration doesn't override it."
                                : "Backing annotation instance.")
                        .build())
                // TODO this is not really necessary for non-configurable annotations
                .addField(FieldSpec.builder(TypeName.BOOLEAN, "onMethod", Modifier.PRIVATE, Modifier.FINAL)
                        .addJavadoc(
                                "{@code true} if annotation was placed on a method; {@code false} if annotation was placed on a class.")
                        .build())
                .addFields(configurable ? ElementFilter.methodsIn(annotationDeclaration.getEnclosedElements())
                        .stream()
                        .map(annotationMember -> FieldSpec.builder(
                                TypeName.get(annotationMember.getReturnType()).box(),
                                "_" + annotationMember.getSimpleName(), Modifier.PRIVATE)
                                .addJavadoc(
                                        "Cached value of the {@code $1L.$2L} annotation member; {@code null} if not looked up yet.",
                                        annotationDeclaration.getSimpleName(), annotationMember.getSimpleName())
                                .build())
                        .collect(Collectors.toList()) : Collections.emptyList())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .addParameter(TypeNames.FAULT_TOLERANCE_METHOD, "method")
                        .addStatement("this.beanClass = method.beanClass")
                        .addStatement("this.method = method.method")
                        .addStatement("this.instance = method.$1L", firstToLowerCase(
                                annotationDeclaration.getSimpleName().toString()))
                        .addStatement("this.onMethod = method.annotationsPresentDirectly.contains($1T.class)", annotationType)
                        .build())
                .addMethod(MethodSpec.methodBuilder("create")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(configImplClassName)
                        .addParameter(TypeNames.FAULT_TOLERANCE_METHOD, "method")
                        .beginControlFlow("if (method.$L == null)",
                                firstToLowerCase(annotationDeclaration.getSimpleName().toString()))
                        .addStatement("return null")
                        .endControlFlow()
                        .addCode(configurable ? CodeBlock.builder()
                                .beginControlFlow("if (!$1T.isEnabled($2T.class, method.method))",
                                        TypeNames.CONFIG, annotationType)
                                .addStatement("return null")
                                .endControlFlow()
                                .build() : CodeBlock.builder().build())
                        .addStatement("return new $T(method)", configImplClassName)
                        .build())
                .addMethod(MethodSpec.methodBuilder("beanClass")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeNames.CLASS)
                        .addStatement("return beanClass")
                        .build())
                .addMethod(MethodSpec.methodBuilder("method")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeNames.METHOD_DESCRIPTOR)
                        .addStatement("return method")
                        .build())
                .addMethod(MethodSpec.methodBuilder("annotationType")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeNames.CLASS_OF_ANNOTATION)
                        .addStatement("return $T.class", annotationType)
                        .build())
                .addMethod(MethodSpec.methodBuilder("isOnMethod")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.BOOLEAN)
                        .addStatement("return onMethod")
                        .build())
                .addMethods(ElementFilter.methodsIn(annotationDeclaration.getEnclosedElements())
                        .stream()
                        .map(annotationMember -> MethodSpec.methodBuilder(annotationMember.getSimpleName().toString())
                                .addAnnotation(Override.class)
                                .addModifiers(Modifier.PUBLIC)
                                .returns(TypeName.get(annotationMember.getReturnType()))
                                .addCode(configurable
                                        ? generateConfigurableMethod(annotationMember, annotationDeclaration)
                                        : generateNonconfigurableMethod(annotationMember))
                                .build())
                        .collect(Collectors.toList()))
                .addMethods(configurable ? Arrays.asList(
                        MethodSpec.methodBuilder("getConfigKeyForMethod")
                                .addModifiers(Modifier.PRIVATE)
                                .returns(TypeNames.STRING)
                                .addParameter(TypeNames.STRING, "key")
                                .addStatement(
                                        "return method.declaringClass.getName() + $1S + method.name + $1S + $2S + $1S + key",
                                        "/", annotationDeclaration.getSimpleName())
                                .build(),
                        MethodSpec.methodBuilder("getConfigKeyForClass")
                                .addModifiers(Modifier.PRIVATE)
                                .returns(TypeNames.STRING)
                                .addParameter(TypeNames.STRING, "key")
                                .addStatement("return method.declaringClass.getName() + $1S + $2S + $1S + key",
                                        "/", annotationDeclaration.getSimpleName())
                                .build())
                        : Collections.emptyList())
                .build())
                .indent("    ") // 4 spaces
                .build()
                .writeTo(processingEnv.getFiler());
    }

    private CodeBlock generateConfigurableMethod(ExecutableElement annotationMember, TypeElement annotationDeclaration) {
        return CodeBlock.builder()
                .beginControlFlow("if (_$L == null)", annotationMember.getSimpleName())
                // TODO maybe cache `Config` in a `private static final` field?
                .addStatement("$1T config = $2T.getConfig()", TypeNames.MP_CONFIG, TypeNames.MP_CONFIG_PROVIDER)
                .beginControlFlow("if (onMethod)")
                .add("// <classname>/<methodname>/<annotation>/<parameter>\n")
                .addStatement(
                        "_$1L = config.getOptionalValue(getConfigKeyForMethod($1S), $2T.class).orElse(null)",
                        annotationMember.getSimpleName(), rawType(annotationMember.getReturnType()))
                .nextControlFlow("else")
                .add("// <classname>/<annotation>/<parameter>\n")
                .addStatement(
                        "_$1L = config.getOptionalValue(getConfigKeyForClass($1S), $2T.class).orElse(null)",
                        annotationMember.getSimpleName(), rawType(annotationMember.getReturnType()))
                .endControlFlow()
                .beginControlFlow("if (_$L == null)", annotationMember.getSimpleName())
                .add("// <annotation>/<parameter>\n")
                .addStatement(
                        "_$1L = config.getOptionalValue($2S + $3S + $1S, $4T.class).orElse(null)",
                        annotationMember.getSimpleName(), annotationDeclaration.getSimpleName(), "/",
                        rawType(annotationMember.getReturnType()))
                .endControlFlow()
                .beginControlFlow("if (_$L == null)", annotationMember.getSimpleName())
                .add("// annotation value\n")
                .addStatement("_$1L = instance.$1L()", annotationMember.getSimpleName())
                .endControlFlow()
                .endControlFlow()
                .addStatement("return _$L", annotationMember.getSimpleName())
                .build();
    }

    private CodeBlock generateNonconfigurableMethod(ExecutableElement annotationMember) {
        return CodeBlock.builder()
                .addStatement("return instance.$1L()", annotationMember.getSimpleName())
                .build();
    }

    private static TypeName rawType(TypeMirror type) {
        return type.accept(new SimpleTypeVisitor8<TypeName, Void>() {
            @Override
            public TypeName visitArray(ArrayType t, Void unused) {
                TypeName componentType = rawType(t.getComponentType());
                return ArrayTypeName.of(componentType);
            }

            @Override
            public TypeName visitDeclared(DeclaredType t, Void unused) {
                return ClassName.get((TypeElement) t.asElement());
            }

            @Override
            public TypeName visitPrimitive(PrimitiveType t, Void unused) {
                return TypeName.get(t);
            }

            @Override
            protected TypeName defaultAction(TypeMirror e, Void unused) {
                throw new IllegalArgumentException("Unexpected type mirror: " + e);
            }
        }, null);
    }

    private static String firstToLowerCase(String str) {
        return str.substring(0, 1).toLowerCase(Locale.ROOT) + str.substring(1);
    }
}
