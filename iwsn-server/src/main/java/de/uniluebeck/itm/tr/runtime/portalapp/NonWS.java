package de.uniluebeck.itm.tr.runtime.portalapp;


import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/**
 * Fields, parameters and methods annotated with this annotation do not provide web service
 * accessibility.
 */
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER, METHOD })
@BindingAnnotation
public @interface NonWS {
	// nothing to do here
}
