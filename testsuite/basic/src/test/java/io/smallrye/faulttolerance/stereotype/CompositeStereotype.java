package io.smallrye.faulttolerance.stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.inject.Stereotype;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

@Stereotype
@RetryStereotype
@CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 5000)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CompositeStereotype {
}
