package io.smallrye.faulttolerance.autoconfig.processor;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.tools.Diagnostic;

import io.smallrye.config.common.utils.StringUtil;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.ConfigConstants;
import io.smallrye.faulttolerance.autoconfig.ConfigDeclarativeOnly;
import io.smallrye.jdeparser.Expr;
import io.smallrye.jdeparser.JDeparser;
import io.smallrye.jdeparser.Sources;
import io.smallrye.jdeparser.Type;
import io.smallrye.jdeparser.Var;
import io.smallrye.jdeparser.creator.BlockCreator;
import io.smallrye.jdeparser.creator.SourceFileCreator;
import io.smallrye.jdeparser.format.Filer;
import io.smallrye.jdeparser.format.FormatPreferences;

@SupportedAnnotationTypes("io.smallrye.faulttolerance.autoconfig.AutoConfig")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
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

    private Sources newSources() {
        return JDeparser.createSources(Filer.newInstance(processingEnv.getFiler()), FormatPreferences.defaults(),
                io.smallrye.jdeparser.SourceVersion.JAVA_17);
    }

    private void generateConfigImpl(TypeElement configClass, TypeElement annotationDeclaration, boolean declarativeOnly)
            throws IOException {
        AutoConfig autoConfig = configClass.getAnnotation(AutoConfig.class);
        boolean configurable = autoConfig.configurable();
        boolean newConfigAllowed = autoConfig.newConfigAllowed();

        String packageName = configClass.getQualifiedName().toString().replace("." + configClass.getSimpleName(), "");
        String configImplName = configClass.getSimpleName() + "Impl";
        Type configType = Type.of(configClass.asType());
        Type configImplType = Type.named(packageName + "." + configImplName);

        Type annotationType = Type.of(annotationDeclaration.asType());
        String annotationName = annotationDeclaration.getSimpleName().toString();
        String annotationVar = firstToLowerCase(annotationName);
        List<ExecutableElement> annotationMembers = ElementFilter.methodsIn(annotationDeclaration.getEnclosedElements());

        String newSuffix = newAnnotationName(annotationDeclaration) + "." + ConfigConstants.ENABLED;
        String oldSuffix = annotationName + "/" + ConfigConstants.ENABLED;

        Sources sources = newSources();
        sources.createSourceFile(packageName, configImplName, sf -> {
            // TODO sf.originatingElement(configClass);
            sf.import_(Type.of(Annotation.class));
            sf.import_(annotationType);
            if (!declarativeOnly) {
                sf.import_(Type.of(Supplier.class));
            }
            if (configurable) {
                sf.import_(Types.MP_CONFIG);
                sf.import_(Types.MP_CONFIG_PROVIDER);
            }
            sf.import_(Types.FT_DEFINITION_EXCEPTION);
            sf.import_(Types.FAULT_TOLERANCE_METHOD);
            if (declarativeOnly) {
                sf.import_(Types.METHOD_DESCRIPTOR);
                sf.import_(Types.CONFIG_UTIL);
            }
            importMemberTypes(sf, annotationMembers);

            sf.class_(configImplName, cc -> {
                cc.docComment(doc -> {
                    doc.text("Automatically generated from the ");
                    doc.link(configType);
                    doc.text(" config interface, do not modify.");
                });
                cc.public_();
                cc.final_();
                cc.implements_(configType);

                if (declarativeOnly) {
                    cc.field("beanClass", fc -> {
                        fc.private_();
                        fc.final_();
                        fc.type(Types.CLASS);
                    });
                    cc.field("method", fc -> {
                        fc.private_();
                        fc.final_();
                        fc.type(Types.METHOD_DESCRIPTOR);
                    });
                    cc.field("onMethod", fc -> {
                        fc.private_();
                        fc.final_();
                        fc.type(Type.BOOLEAN);
                        fc.docComment(doc -> {
                            doc.code("true");
                            doc.text(" if annotation was placed on a method; ");
                            doc.code("false");
                            doc.text(" if annotation was placed on a class.");
                        });
                    });
                }
                cc.field("description", fc -> {
                    fc.private_();
                    fc.final_();
                    fc.type(Type.STRING);
                    fc.docComment(doc -> doc.text(declarativeOnly
                            ? "Description for the error message: a fully qualified method name."
                            : "Description for the error message: a fully qualified method name or identifier."));
                });
                cc.field("configKey", fc -> {
                    fc.private_();
                    fc.final_();
                    fc.type(Type.STRING);
                    fc.docComment(doc -> {
                        doc.text("Configuration key: either ");
                        doc.code("<classname>/<methodname>");
                        doc.text(" or ");
                        doc.code("<classname>");
                        if (!declarativeOnly) {
                            doc.text(" or ");
                            doc.code("<id>");
                        }
                        doc.text(".");
                    });
                });
                cc.field("instance", fc -> {
                    fc.private_();
                    fc.final_();
                    fc.type(annotationType);
                    fc.docComment(doc -> doc.text(configurable
                            ? "Backing annotation instance. Used when runtime configuration doesn't override it."
                            : "Backing annotation instance."));
                });
                if (configurable) {
                    for (ExecutableElement annotationMember : annotationMembers) {
                        cc.field("_" + annotationMember.getSimpleName(), fc -> {
                            fc.private_();
                            fc.type(type(annotationMember.getReturnType()).box());
                            fc.docComment(doc -> {
                                doc.text("Cached value of the ");
                                doc.code(annotationName + "." + annotationMember.getSimpleName());
                                doc.text(" annotation member; ");
                                doc.code("null");
                                doc.text(" if not looked up yet.");
                            });
                        });
                    }
                }

                cc.constructor(mc -> {
                    mc.private_();
                    Var method = mc.param("method", Types.FAULT_TOLERANCE_METHOD);
                    mc.body(body -> {
                        Expr annotationPresentDirectly = method.field("annotationsPresentDirectly")
                                .call("contains", annotationType.class_());
                        Expr onMethod;
                        if (declarativeOnly) {
                            body.emit(Expr.THIS.field("beanClass").assign(method.field("beanClass")));
                            body.emit(Expr.THIS.field("method").assign(method.field("method")));
                            body.emit(Expr.THIS.field("onMethod").assign(annotationPresentDirectly));
                            onMethod = Expr.$v("onMethod");
                        } else {
                            onMethod = body.var(Type.BOOLEAN, "onMethod", annotationPresentDirectly);
                        }
                        body.emit(Expr.THIS.field("description").assign(method.field("method").call("toString")));
                        Expr declaringClassName = method.field("method").field("declaringClass").call("getName");
                        body.emit(Expr.THIS.field("configKey").assign(onMethod.cond(
                                declaringClassName.add(Expr.str("/")).add(method.field("method").field("name")),
                                declaringClassName)));
                        body.emit(Expr.THIS.field("instance").assign(method.field(annotationVar)));
                    });
                });
                if (!declarativeOnly) {
                    cc.constructor(mc -> {
                        mc.private_();
                        Var id = mc.param("id", Type.STRING);
                        Var instance = mc.param("instance", annotationType);
                        mc.body(body -> {
                            body.emit(Expr.THIS.field("description").assign(
                                    Expr.str("Guard with @Identifier(").add(id).add(Expr.str(")"))));
                            body.emit(Expr.THIS.field("configKey").assign(id));
                            body.emit(Expr.THIS.field("instance").assign(instance));
                        });
                    });
                }

                cc.method("create", mc -> {
                    mc.public_();
                    mc.static_();
                    mc.returning(configImplType);
                    Var method = mc.param("method", Types.FAULT_TOLERANCE_METHOD);
                    mc.body(body -> {
                        body.if_(method.field(annotationVar).eq(Expr.NULL), then -> then.return_(Expr.NULL));
                        if (configurable) {
                            body.if_(Types.CONFIG_UTIL.call("isEnabled",
                                    Expr.str(newSuffix), Expr.str(oldSuffix), method.field("method")).not(),
                                    then -> then.return_(Expr.NULL));
                        }
                        body.return_(configImplType.new_(method));
                    });
                });
                if (!declarativeOnly) {
                    cc.method("create", mc -> {
                        mc.public_();
                        mc.static_();
                        mc.returning(configImplType);
                        Var id = mc.param("id", Type.STRING);
                        Var supplier = mc.param("supplier", Types.supplierOf(annotationType));
                        mc.body(body -> {
                            body.if_(supplier.eq(Expr.NULL), then -> then.return_(Expr.NULL));
                            if (configurable) {
                                body.if_(Types.CONFIG_UTIL.call("isEnabled",
                                        Expr.str(newSuffix), Expr.str(oldSuffix), id).not(),
                                        then -> then.return_(Expr.NULL));
                            }
                            body.return_(configImplType.new_(id, supplier.call("get")));
                        });
                    });
                }

                if (declarativeOnly) {
                    cc.method("isOnMethod", mc -> {
                        mc.annotate(Types.OVERRIDE);
                        mc.public_();
                        mc.returning(Type.BOOLEAN);
                        mc.body(body -> body.return_(Expr.$v("onMethod")));
                    });
                    cc.method("beanClass", mc -> {
                        mc.annotate(Types.OVERRIDE);
                        mc.public_();
                        mc.returning(Types.CLASS);
                        mc.body(body -> body.return_(Expr.$v("beanClass")));
                    });
                    cc.method("method", mc -> {
                        mc.annotate(Types.OVERRIDE);
                        mc.public_();
                        mc.returning(Types.METHOD_DESCRIPTOR);
                        mc.body(body -> body.return_(Expr.$v("method")));
                    });
                }

                cc.method("annotationType", mc -> {
                    mc.annotate(Types.OVERRIDE);
                    mc.public_();
                    mc.returning(Types.CLASS_OF_ANNOTATION);
                    mc.body(body -> body.return_(annotationType.class_()));
                });

                for (ExecutableElement annotationMember : annotationMembers) {
                    cc.method(annotationMember.getSimpleName().toString(), mc -> {
                        mc.annotate(Types.OVERRIDE);
                        mc.public_();
                        mc.returning(type(annotationMember.getReturnType()));
                        mc.body(body -> {
                            if (configurable) {
                                generateConfigurableMethod(body, annotationMember, annotationDeclaration, newConfigAllowed);
                            } else {
                                generateNonconfigurableMethod(body, annotationMember);
                            }
                        });
                    });
                }

                cc.method("materialize", mc -> {
                    mc.annotate(Types.OVERRIDE);
                    mc.public_();
                    mc.returning(Type.VOID);
                    mc.body(body -> {
                        for (ExecutableElement annotationMember : annotationMembers) {
                            body.emit(Expr.callPlain(annotationMember.getSimpleName().toString()));
                        }
                    });
                });

                cc.method("fail", mc -> {
                    mc.annotate(Types.OVERRIDE);
                    mc.public_();
                    mc.returning(Types.FT_DEFINITION_EXCEPTION);
                    Var reason = mc.param("reason", Type.STRING);
                    mc.body(body -> body.return_(Types.FT_DEFINITION_EXCEPTION.new_(
                            Expr.str("Invalid @" + annotationName + " on ")
                                    .add(Expr.$v("description"))
                                    .add(Expr.str(": "))
                                    .add(reason))));
                });
                cc.method("fail", mc -> {
                    mc.annotate(Types.OVERRIDE);
                    mc.public_();
                    mc.returning(Types.FT_DEFINITION_EXCEPTION);
                    Var member = mc.param("member", Type.STRING);
                    Var reason = mc.param("reason", Type.STRING);
                    mc.body(body -> body.return_(Types.FT_DEFINITION_EXCEPTION.new_(
                            Expr.str("Invalid @" + annotationName + ".")
                                    .add(member)
                                    .add(Expr.str(" on "))
                                    .add(Expr.$v("description"))
                                    .add(Expr.str(": "))
                                    .add(reason))));
                });
            });
        });
        sources.writeSources();
    }

    private void generateNoConfigImpl(TypeElement configClass, TypeElement annotationDeclaration) throws IOException {
        String packageName = configClass.getQualifiedName().toString().replace("." + configClass.getSimpleName(), "");
        String configImplName = configClass.getSimpleName().toString().replace("Config", "NoConfig") + "Impl";
        Type configType = Type.of(configClass.asType());
        Type configImplType = Type.named(packageName + "." + configImplName);

        Type annotationType = Type.of(annotationDeclaration.asType());
        String annotationName = annotationDeclaration.getSimpleName().toString();
        List<ExecutableElement> annotationMembers = ElementFilter.methodsIn(annotationDeclaration.getEnclosedElements());

        Sources sources = newSources();
        sources.createSourceFile(packageName, configImplName, sf -> {
            sf.import_(annotationType);
            sf.import_(Type.of(Annotation.class));
            sf.import_(Type.of(Supplier.class));
            sf.import_(Types.FT_DEFINITION_EXCEPTION);
            importMemberTypes(sf, annotationMembers);

            sf.class_(configImplName, cc -> {
                cc.docComment(doc -> {
                    doc.text("Automatically generated from the ");
                    doc.link(configType);
                    doc.text(" config interface, do not modify.");
                });
                cc.public_();
                cc.final_();
                cc.implements_(configType);

                cc.field("instance", fc -> {
                    fc.private_();
                    fc.final_();
                    fc.type(annotationType);
                    fc.docComment(doc -> doc.text("Backing annotation instance."));
                });

                cc.constructor(mc -> {
                    mc.private_();
                    Var instance = mc.param("instance", annotationType);
                    mc.body(body -> body.emit(Expr.THIS.field("instance").assign(instance)));
                });

                cc.method("create", mc -> {
                    mc.public_();
                    mc.static_();
                    mc.returning(configImplType);
                    Var supplier = mc.param("supplier", Types.supplierOf(annotationType));
                    mc.body(body -> {
                        body.if_(supplier.eq(Expr.NULL), then -> then.return_(Expr.NULL));
                        body.return_(configImplType.new_(supplier.call("get")));
                    });
                });

                cc.method("annotationType", mc -> {
                    mc.annotate(Types.OVERRIDE);
                    mc.public_();
                    mc.returning(Types.CLASS_OF_ANNOTATION);
                    mc.body(body -> body.return_(annotationType.class_()));
                });

                for (ExecutableElement annotationMember : annotationMembers) {
                    cc.method(annotationMember.getSimpleName().toString(), mc -> {
                        mc.annotate(Types.OVERRIDE);
                        mc.public_();
                        mc.returning(type(annotationMember.getReturnType()));
                        mc.body(body -> generateNonconfigurableMethod(body, annotationMember));
                    });
                }

                cc.method("materialize", mc -> {
                    mc.annotate(Types.OVERRIDE);
                    mc.public_();
                    mc.returning(Type.VOID);
                    mc.body(body -> body.lineComment("no config, no need to materialize"));
                });

                cc.method("fail", mc -> {
                    mc.annotate(Types.OVERRIDE);
                    mc.public_();
                    mc.returning(Types.FT_DEFINITION_EXCEPTION);
                    Var reason = mc.param("reason", Type.STRING);
                    mc.body(body -> body.return_(Types.FT_DEFINITION_EXCEPTION.new_(
                            Expr.str("Invalid @" + annotationName + ": ").add(reason))));
                });
                cc.method("fail", mc -> {
                    mc.annotate(Types.OVERRIDE);
                    mc.public_();
                    mc.returning(Types.FT_DEFINITION_EXCEPTION);
                    Var member = mc.param("member", Type.STRING);
                    Var reason = mc.param("reason", Type.STRING);
                    mc.body(body -> body.return_(Types.FT_DEFINITION_EXCEPTION.new_(
                            Expr.str("Invalid @" + annotationName + ".")
                                    .add(member)
                                    .add(Expr.str(": "))
                                    .add(reason))));
                });
            });
        });
        sources.writeSources();
    }

    private void generateConfigurableMethod(BlockCreator body, ExecutableElement annotationMember,
            TypeElement annotationDeclaration, boolean newConfigAllowed) {
        String memberName = annotationMember.getSimpleName().toString();
        String annotationName = annotationDeclaration.getSimpleName().toString();
        Var cached = Expr.$v("_" + memberName);
        Expr rawTypeClass = rawType(annotationMember.getReturnType()).class_();

        body.if_(cached.eq(Expr.NULL), outer -> {
            Var config = outer.var(Types.MP_CONFIG, "config", Types.MP_CONFIG_PROVIDER.call("getConfig"));
            if (newConfigAllowed) {
                String newAnnotationName = newAnnotationName(annotationDeclaration);
                String newMemberName = newMemberName(annotationMember);

                outer.block(block -> {
                    block.lineComment("smallrye.faulttolerance.\"<configKey>\".<annotation>.<member>");
                    Var newKey = block.var(Type.STRING, "newKey",
                            Expr.str(ConfigConstants.PREFIX + "\"")
                                    .add(Expr.THIS.field("configKey"))
                                    .add(Expr.str("\"." + newAnnotationName + "." + newMemberName)));
                    block.lineComment("<configKey>/<annotation>/<member>");
                    Var oldKey = block.var(Type.STRING, "oldKey",
                            Expr.THIS.field("configKey").add(Expr.str("/" + annotationName + "/" + memberName)));
                    block.emit(cached.assign(lookupWithFallback(config, newKey, oldKey, rawTypeClass)));
                });
                outer.if_(cached.eq(Expr.NULL), then -> {
                    then.lineComment("smallrye.faulttolerance.global.<annotation>.<member>");
                    Var newKey = then.var(Type.STRING, "newKey",
                            Expr.str(ConfigConstants.PREFIX + ConfigConstants.GLOBAL + "."
                                    + newAnnotationName + "." + newMemberName));
                    then.lineComment("<annotation>/<member>");
                    Var oldKey = then.var(Type.STRING, "oldKey", Expr.str(annotationName + "/" + memberName));
                    then.emit(cached.assign(lookupWithFallback(config, newKey, oldKey, rawTypeClass)));
                });
            } else {
                outer.block(block -> {
                    block.lineComment("<configKey>/<annotation>/<member>");
                    Var key = block.var(Type.STRING, "key",
                            Expr.THIS.field("configKey").add(Expr.str("/" + annotationName + "/" + memberName)));
                    block.emit(cached.assign(lookup(config, key, rawTypeClass)));
                });
                outer.if_(cached.eq(Expr.NULL), then -> {
                    then.lineComment("<annotation>/<member>");
                    Var key = then.var(Type.STRING, "key", Expr.str(annotationName + "/" + memberName));
                    then.emit(cached.assign(lookup(config, key, rawTypeClass)));
                });
            }
            outer.if_(cached.eq(Expr.NULL), then -> {
                then.lineComment("annotation value");
                then.emit(cached.assign(Expr.$v("instance").call(memberName)));
            });
        });
        body.return_(cached);
    }

    private static Expr lookup(Var config, Expr key, Expr rawTypeClass) {
        return config.call("getOptionalValue", key, rawTypeClass).call("orElse", Expr.NULL);
    }

    private static Expr lookupWithFallback(Var config, Expr newKey, Expr oldKey, Expr rawTypeClass) {
        return config.call("getOptionalValue", newKey, rawTypeClass)
                .call("or", Expr.lambda(config.call("getOptionalValue", oldKey, rawTypeClass)))
                .call("orElse", Expr.NULL);
    }

    private void generateNonconfigurableMethod(BlockCreator body, ExecutableElement annotationMember) {
        body.return_(Expr.$v("instance").call(annotationMember.getSimpleName().toString()));
    }

    private static void importMemberTypes(SourceFileCreator sf, List<ExecutableElement> annotationMembers) {
        for (ExecutableElement annotationMember : annotationMembers) {
            importTypes(sf, annotationMember.getReturnType());
        }
    }

    private static void importTypes(SourceFileCreator sf, TypeMirror type) {
        switch (type.getKind()) {
            case ARRAY -> importTypes(sf, ((ArrayType) type).getComponentType());
            case DECLARED -> {
                DeclaredType declared = (DeclaredType) type;
                sf.import_(rawType(declared));
                declared.getTypeArguments().forEach(it -> importTypes(sf, it));
            }
            case WILDCARD -> {
                WildcardType wildcard = (WildcardType) type;
                if (wildcard.getExtendsBound() != null) {
                    importTypes(sf, wildcard.getExtendsBound());
                }
                if (wildcard.getSuperBound() != null) {
                    importTypes(sf, wildcard.getSuperBound());
                }
            }
            default -> {
                // primitive types don't need an import
            }
        }
    }

    /**
     * Converts the given type mirror to a {@link Type} including type arguments. Unlike
     * {@link Type#of(TypeMirror)}, this also supports wildcards.
     */
    private static Type type(TypeMirror type) {
        return type.accept(new SimpleTypeVisitor8<Type, Void>() {
            @Override
            public Type visitArray(ArrayType t, Void unused) {
                return type(t.getComponentType()).array();
            }

            @Override
            public Type visitDeclared(DeclaredType t, Void unused) {
                Type raw = rawType(t);
                if (t.getTypeArguments().isEmpty()) {
                    return raw;
                }
                return raw.typeArg(t.getTypeArguments().stream().map(AutoConfigProcessor::type).toList());
            }

            @Override
            public Type visitWildcard(WildcardType t, Void unused) {
                if (t.getExtendsBound() != null) {
                    return type(t.getExtendsBound()).wildcardExtends();
                }
                if (t.getSuperBound() != null) {
                    return type(t.getSuperBound()).wildcardSuper();
                }
                return Type.WILDCARD;
            }

            @Override
            public Type visitPrimitive(PrimitiveType t, Void unused) {
                return Type.of(t);
            }

            @Override
            protected Type defaultAction(TypeMirror e, Void unused) {
                throw new IllegalArgumentException("Unexpected type mirror: " + e);
            }
        }, null);
    }

    private static Type rawType(TypeMirror type) {
        return type.accept(new SimpleTypeVisitor8<Type, Void>() {
            @Override
            public Type visitArray(ArrayType t, Void unused) {
                return rawType(t.getComponentType()).array();
            }

            @Override
            public Type visitDeclared(DeclaredType t, Void unused) {
                return Type.named(((TypeElement) t.asElement()).getQualifiedName().toString());
            }

            @Override
            public Type visitPrimitive(PrimitiveType t, Void unused) {
                return Type.of(t);
            }

            @Override
            protected Type defaultAction(TypeMirror e, Void unused) {
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
        result = switch (result) {
            case "jitterDelayUnit" -> "jitterUnit";
            case "durationUnit" -> "maxDurationUnit";
            default -> result;
        };
        result = StringUtil.skewer(result);
        return result;
    }
}
