package io.smallrye.faulttolerance.autoconfig.processor;

import java.io.IOException;
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

import io.smallrye.config.common.utils.StringUtil;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.ConfigConstants;
import io.smallrye.faulttolerance.autoconfig.ConfigDeclarativeOnly;

@SupportedAnnotationTypes("io.smallrye.faulttolerance.autoconfig.AutoConfig")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AutoConfigProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            doProcess(annotations, roundEnv);
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unexpected error: " + e.getMessage());
        }
        return false;
    }

    private void doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) throws IOException {
        if (!annotations.isEmpty()) {
            // this processor only supports 1 annotation, so this is it
            TypeElement generateConfigForAnnotation = annotations.iterator().next();

            for (Element annotated : roundEnv.getElementsAnnotatedWith(generateConfigForAnnotation)) {
                // the annotation can only be put on types, so the cast here is safe
                processConfigClass((TypeElement) annotated);
            }
        }
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
                            + " and io.smallrye.faulttolerance.autoconfig.Config[DeclarativeOnly]");
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

        boolean declarativeOnly = configClass.getInterfaces()
                .stream()
                .filter(it -> it.getKind() == TypeKind.DECLARED)
                .map(it -> ((DeclaredType) it).asElement())
                .filter(it -> it.getKind() != ElementKind.ANNOTATION_TYPE)
                .map(it -> (TypeElement) it)
                .findAny()
                .get().getSimpleName().contentEquals(ConfigDeclarativeOnly.class.getSimpleName());

        generateConfigImpl(configClass, annotationDeclaration.get(), declarativeOnly);
        if (!declarativeOnly) {
            generateNoConfigImpl(configClass, annotationDeclaration.get());
        }
    }

    private void generateConfigImpl(TypeElement configClass, TypeElement annotationDeclaration, boolean declarativeOnly)
            throws IOException {
        AutoConfig autoConfig = configClass.getAnnotation(AutoConfig.class);
        boolean configurable = autoConfig.configurable();
        boolean newConfigAllowed = autoConfig.newConfigAllowed();

        String packageName = configClass.getQualifiedName().toString().replace("." + configClass.getSimpleName(), "");
        ClassName configImplClassName = ClassName.get(packageName, configClass.getSimpleName() + "Impl");

        TypeName annotationType = TypeName.get(annotationDeclaration.asType());

        String newSuffix = newAnnotationName(annotationDeclaration) + "." + ConfigConstants.ENABLED;
        String oldSuffix = annotationDeclaration.getSimpleName() + "/" + ConfigConstants.ENABLED;

        JavaFile.builder(packageName, TypeSpec.classBuilder(configImplClassName)
                .addOriginatingElement(configClass)
                .addJavadoc("Automatically generated from the {@link $L} config interface, do not modify.",
                        configClass.getSimpleName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(configClass.asType())
                .addFields(declarativeOnly ? List.of(
                        FieldSpec.builder(TypeNames.CLASS, "beanClass", Modifier.PRIVATE, Modifier.FINAL).build(),
                        FieldSpec.builder(TypeNames.METHOD_DESCRIPTOR, "method", Modifier.PRIVATE, Modifier.FINAL).build(),
                        FieldSpec.builder(TypeName.BOOLEAN, "onMethod", Modifier.PRIVATE, Modifier.FINAL)
                                .addJavadoc(
                                        "{@code true} if annotation was placed on a method; {@code false} if annotation was placed on a class.")
                                .build())
                        : List.of())
                .addField(FieldSpec.builder(TypeNames.STRING, "description", Modifier.PRIVATE, Modifier.FINAL)
                        .addJavadoc(declarativeOnly
                                ? "Description for the error message: a fully qualified method name."
                                : "Description for the error message: a fully qualified method name or identifier.")
                        .build())
                .addField(FieldSpec.builder(TypeNames.STRING, "configKey", Modifier.PRIVATE, Modifier.FINAL)
                        .addJavadoc(declarativeOnly
                                ? "Configuration key: either {@code <classname>/<methodname>} or {@code <classname>}."
                                : "Configuration key: either {@code <classname>/<methodname>} or {@code <classname>} or {@code <id>}.")
                        .build())
                .addField(FieldSpec.builder(annotationType, "instance", Modifier.PRIVATE, Modifier.FINAL)
                        .addJavadoc(configurable
                                ? "Backing annotation instance. Used when runtime configuration doesn't override it."
                                : "Backing annotation instance.")
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
                        .addCode(declarativeOnly
                                ? CodeBlock.builder()
                                        .addStatement("this.beanClass = method.beanClass")
                                        .addStatement("this.method = method.method")
                                        .addStatement("this.onMethod = method.annotationsPresentDirectly.contains($1T.class)",
                                                annotationType)
                                        .build()
                                : CodeBlock.builder()
                                        .addStatement(
                                                "boolean onMethod = method.annotationsPresentDirectly.contains($1T.class)",
                                                annotationType)
                                        .build())
                        .addStatement("this.description = method.method.toString()")
                        .addStatement(
                                "this.configKey = onMethod ? method.method.declaringClass.getName() + \"/\" + method.method.name : method.method.declaringClass.getName()",
                                annotationType)
                        .addStatement("this.instance = method.$1L", firstToLowerCase(
                                annotationDeclaration.getSimpleName().toString()))
                        .build())
                .addMethods(declarativeOnly
                        ? List.of()
                        : List.of(MethodSpec.constructorBuilder()
                                .addModifiers(Modifier.PRIVATE)
                                .addParameter(TypeNames.STRING, "id")
                                .addParameter(annotationType, "instance")
                                .addStatement("this.description = \"Guard with @Identifier(\" + id + \")\"")
                                .addStatement("this.configKey = id")
                                .addStatement("this.instance = instance")
                                .build()))
                .addMethod(MethodSpec.methodBuilder("create")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(configImplClassName)
                        .addParameter(TypeNames.FAULT_TOLERANCE_METHOD, "method")
                        .beginControlFlow("if (method.$L == null)",
                                firstToLowerCase(annotationDeclaration.getSimpleName().toString()))
                        .addStatement("return null")
                        .endControlFlow()
                        .addCode(configurable ? CodeBlock.builder()
                                .beginControlFlow("if (!$1T.isEnabled($2S, $3S, method.method))",
                                        TypeNames.CONFIG_UTIL, newSuffix, oldSuffix)
                                .addStatement("return null")
                                .endControlFlow()
                                .build() : CodeBlock.builder().build())
                        .addStatement("return new $T(method)", configImplClassName)
                        .build())
                .addMethods(declarativeOnly
                        ? List.of()
                        : List.of(MethodSpec.methodBuilder("create")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(configImplClassName)
                                .addParameter(TypeNames.STRING, "id")
                                .addParameter(TypeNames.supplierOf(annotationType), "supplier")
                                .beginControlFlow("if (supplier == null)")
                                .addStatement("return null")
                                .endControlFlow()
                                .addCode(configurable ? CodeBlock.builder()
                                        .beginControlFlow("if (!$1T.isEnabled($2S, $3S, id))",
                                                TypeNames.CONFIG_UTIL, newSuffix, oldSuffix)
                                        .addStatement("return null")
                                        .endControlFlow()
                                        .build() : CodeBlock.builder().build())
                                .addStatement("return new $T(id, supplier.get())", configImplClassName)
                                .build()))
                .addMethods(declarativeOnly ? List.of(
                        MethodSpec.methodBuilder("isOnMethod")
                                .addAnnotation(Override.class)
                                .addModifiers(Modifier.PUBLIC)
                                .returns(TypeName.BOOLEAN)
                                .addStatement("return onMethod")
                                .build(),
                        MethodSpec.methodBuilder("beanClass")
                                .addAnnotation(Override.class)
                                .addModifiers(Modifier.PUBLIC)
                                .returns(TypeNames.CLASS)
                                .addStatement("return beanClass")
                                .build(),
                        MethodSpec.methodBuilder("method")
                                .addAnnotation(Override.class)
                                .addModifiers(Modifier.PUBLIC)
                                .returns(TypeNames.METHOD_DESCRIPTOR)
                                .addStatement("return method")
                                .build())
                        : List.of())
                .addMethod(MethodSpec.methodBuilder("annotationType")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeNames.CLASS_OF_ANNOTATION)
                        .addStatement("return $T.class", annotationType)
                        .build())
                .addMethods(ElementFilter.methodsIn(annotationDeclaration.getEnclosedElements())
                        .stream()
                        .map(annotationMember -> MethodSpec.methodBuilder(annotationMember.getSimpleName().toString())
                                .addAnnotation(Override.class)
                                .addModifiers(Modifier.PUBLIC)
                                .returns(TypeName.get(annotationMember.getReturnType()))
                                .addCode(configurable
                                        ? generateConfigurableMethod(annotationMember, annotationDeclaration, newConfigAllowed)
                                        : generateNonconfigurableMethod(annotationMember))
                                .build())
                        .collect(Collectors.toList()))
                .addMethod(MethodSpec.methodBuilder("materialize")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.VOID)
                        .addCode(generateMaterializeMethod(annotationDeclaration))
                        .build())
                .addMethod(MethodSpec.methodBuilder("fail")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeNames.FT_DEFINITION_EXCEPTION)
                        .addParameter(TypeNames.STRING, "reason")
                        .addStatement("return new $1T(\"Invalid @$2T on \" + description + \": \" + reason)",
                                TypeNames.FT_DEFINITION_EXCEPTION, annotationType)
                        .build())
                .addMethod(MethodSpec.methodBuilder("fail")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeNames.FT_DEFINITION_EXCEPTION)
                        .addParameter(TypeNames.STRING, "member")
                        .addParameter(TypeNames.STRING, "reason")
                        .addStatement("return new $1T(\"Invalid @$2T.\" + member + \" on \" + description + \": \" + reason)",
                                TypeNames.FT_DEFINITION_EXCEPTION, annotationType)
                        .build())
                .build())
                .indent("    ") // 4 spaces
                .build()
                .writeTo(processingEnv.getFiler());
    }

    private void generateNoConfigImpl(TypeElement configClass, TypeElement annotationDeclaration) throws IOException {
        String packageName = configClass.getQualifiedName().toString().replace("." + configClass.getSimpleName(), "");
        ClassName configImplClassName = ClassName.get(packageName, configClass.getSimpleName().toString()
                .replace("Config", "NoConfig") + "Impl");

        TypeName annotationType = TypeName.get(annotationDeclaration.asType());

        JavaFile.builder(packageName, TypeSpec.classBuilder(configImplClassName)
                .addOriginatingElement(configClass)
                .addJavadoc("Automatically generated from the {@link $L} config interface, do not modify.",
                        configClass.getSimpleName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(configClass.asType())
                .addField(FieldSpec.builder(annotationType, "instance", Modifier.PRIVATE, Modifier.FINAL)
                        .addJavadoc("Backing annotation instance.")
                        .build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .addParameter(annotationType, "instance")
                        .addStatement("this.instance = instance")
                        .build())
                .addMethod(MethodSpec.methodBuilder("create")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(configImplClassName)
                        .addParameter(TypeNames.supplierOf(annotationType), "supplier")
                        .beginControlFlow("if (supplier == null)")
                        .addStatement("return null")
                        .endControlFlow()
                        .addStatement("return new $T(supplier.get())", configImplClassName)
                        .build())
                .addMethod(MethodSpec.methodBuilder("annotationType")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeNames.CLASS_OF_ANNOTATION)
                        .addStatement("return $T.class", annotationType)
                        .build())
                .addMethods(ElementFilter.methodsIn(annotationDeclaration.getEnclosedElements())
                        .stream()
                        .map(annotationMember -> MethodSpec.methodBuilder(annotationMember.getSimpleName().toString())
                                .addAnnotation(Override.class)
                                .addModifiers(Modifier.PUBLIC)
                                .returns(TypeName.get(annotationMember.getReturnType()))
                                .addCode(generateNonconfigurableMethod(annotationMember))
                                .build())
                        .collect(Collectors.toList()))
                .addMethod(MethodSpec.methodBuilder("materialize")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.VOID)
                        .addComment("no config, no need to materialize")
                        .build())
                .addMethod(MethodSpec.methodBuilder("fail")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeNames.FT_DEFINITION_EXCEPTION)
                        .addParameter(TypeNames.STRING, "reason")
                        .addStatement("return new $1T(\"Invalid @$2T: \" + reason)",
                                TypeNames.FT_DEFINITION_EXCEPTION, annotationType)
                        .build())
                .addMethod(MethodSpec.methodBuilder("fail")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeNames.FT_DEFINITION_EXCEPTION)
                        .addParameter(TypeNames.STRING, "member")
                        .addParameter(TypeNames.STRING, "reason")
                        .addStatement("return new $1T(\"Invalid @$2T.\" + member + \": \" + reason)",
                                TypeNames.FT_DEFINITION_EXCEPTION, annotationType)
                        .build())
                .build())
                .indent("    ") // 4 spaces
                .build()
                .writeTo(processingEnv.getFiler());
    }

    private CodeBlock generateConfigurableMethod(ExecutableElement annotationMember, TypeElement annotationDeclaration,
            boolean newConfigAllowed) {
        if (newConfigAllowed) {
            String newAnnotationName = newAnnotationName(annotationDeclaration);
            String newMemberName = newMemberName(annotationMember);

            return CodeBlock.builder()
                    .beginControlFlow("if (_$L == null)", annotationMember.getSimpleName())
                    .addStatement("$1T config = $2T.getConfig()", TypeNames.MP_CONFIG, TypeNames.MP_CONFIG_PROVIDER)
                    .beginControlFlow("")
                    .add("// smallrye.faulttolerance.\"<configKey>\".<annotation>.<member>\n")
                    .addStatement("String newKey = \"" + ConfigConstants.PREFIX + "\\\"\" + this.configKey + \"\\\"."
                            + newAnnotationName + "." + newMemberName + "\"")
                    .add("// <configKey>/<annotation>/<member>\n")
                    .addStatement("String oldKey = this.configKey + " + "\"/" + annotationDeclaration.getSimpleName()
                            + "/" + annotationMember.getSimpleName() + "\"")
                    .addStatement(
                            "_$1L = config.getOptionalValue(newKey, $2T.class).or(() -> config.getOptionalValue(oldKey, $2T.class)).orElse(null)",
                            annotationMember.getSimpleName(), rawType(annotationMember.getReturnType()))
                    .endControlFlow()
                    .beginControlFlow("if (_$L == null)", annotationMember.getSimpleName())
                    .add("// smallrye.faulttolerance.global.<annotation>.<member>\n")
                    .addStatement("String newKey = \"" + ConfigConstants.PREFIX + ConfigConstants.GLOBAL + "."
                            + newAnnotationName + "." + newMemberName + "\"")
                    .add("// <annotation>/<member>\n")
                    .addStatement("String oldKey = \"" + annotationDeclaration.getSimpleName()
                            + "/" + annotationMember.getSimpleName() + "\"")
                    .addStatement(
                            "_$1L = config.getOptionalValue(newKey, $2T.class).or(() -> config.getOptionalValue(oldKey, $2T.class)).orElse(null)",
                            annotationMember.getSimpleName(), rawType(annotationMember.getReturnType()))
                    .endControlFlow()
                    .beginControlFlow("if (_$L == null)", annotationMember.getSimpleName())
                    .add("// annotation value\n")
                    .addStatement("_$1L = instance.$1L()", annotationMember.getSimpleName())
                    .endControlFlow()
                    .endControlFlow()
                    .addStatement("return _$L", annotationMember.getSimpleName())
                    .build();
        } else {
            return CodeBlock.builder()
                    .beginControlFlow("if (_$L == null)", annotationMember.getSimpleName())
                    .addStatement("$1T config = $2T.getConfig()", TypeNames.MP_CONFIG, TypeNames.MP_CONFIG_PROVIDER)
                    .beginControlFlow("")
                    .add("// <configKey>/<annotation>/<member>\n")
                    .addStatement("String key = this.configKey + " + "\"/" + annotationDeclaration.getSimpleName()
                            + "/" + annotationMember.getSimpleName() + "\"")
                    .addStatement(
                            "_$1L = config.getOptionalValue(key, $2T.class).orElse(null)",
                            annotationMember.getSimpleName(), rawType(annotationMember.getReturnType()))
                    .endControlFlow()
                    .beginControlFlow("if (_$L == null)", annotationMember.getSimpleName())
                    .add("// <annotation>/<member>\n")
                    .addStatement("String key = \"" + annotationDeclaration.getSimpleName()
                            + "/" + annotationMember.getSimpleName() + "\"")
                    .addStatement(
                            "_$1L = config.getOptionalValue(key, $2T.class).orElse(null)",
                            annotationMember.getSimpleName(), rawType(annotationMember.getReturnType()))
                    .endControlFlow()
                    .beginControlFlow("if (_$L == null)", annotationMember.getSimpleName())
                    .add("// annotation value\n")
                    .addStatement("_$1L = instance.$1L()", annotationMember.getSimpleName())
                    .endControlFlow()
                    .endControlFlow()
                    .addStatement("return _$L", annotationMember.getSimpleName())
                    .build();
        }
    }

    private CodeBlock generateNonconfigurableMethod(ExecutableElement annotationMember) {
        return CodeBlock.builder()
                .addStatement("return instance.$1L()", annotationMember.getSimpleName())
                .build();
    }

    private CodeBlock generateMaterializeMethod(TypeElement annotationDeclaration) {
        CodeBlock.Builder builder = CodeBlock.builder();
        for (ExecutableElement annotationElement : ElementFilter.methodsIn(annotationDeclaration.getEnclosedElements())) {
            builder.addStatement("$L()", annotationElement.getSimpleName());
        }
        return builder.build();
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

    private static String newAnnotationName(TypeElement annotationDeclaration) {
        return StringUtil.skewer(annotationDeclaration.getSimpleName().toString());
    }

    private static String newMemberName(ExecutableElement annotationMember) {
        String result = annotationMember.getSimpleName().toString();
        switch (result) {
            case "jitterDelayUnit":
                result = "jitterUnit";
                break;
            case "durationUnit":
                result = "maxDurationUnit";
                break;
        }
        result = StringUtil.skewer(result);
        return result;
    }
}
