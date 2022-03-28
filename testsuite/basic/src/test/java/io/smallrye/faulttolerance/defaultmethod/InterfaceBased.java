package io.smallrye.faulttolerance.defaultmethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

@Qualifier
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InterfaceBased {
    final class Literal extends AnnotationLiteral<InterfaceBased> implements InterfaceBased {
        public static final Literal INSTANCE = new Literal();
    }
}
