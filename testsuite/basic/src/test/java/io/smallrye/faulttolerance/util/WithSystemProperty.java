package io.smallrye.faulttolerance.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Repeatable(WithSystemProperty.List.class)
@ExtendWith(WithSystemPropertyExtension.class)
public @interface WithSystemProperty {
    String key();

    String value();

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @interface List {
        WithSystemProperty[] value();
    }
}
