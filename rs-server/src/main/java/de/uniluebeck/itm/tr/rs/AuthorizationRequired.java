package de.uniluebeck.itm.tr.rs;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Methods annotated with this annotation indicate
 * that the method call needs to be authenticated.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@BindingAnnotation
public @interface AuthorizationRequired {

	/**
	 * @return The first string parameter provided by the annotation.
	 */
	public String value();
}
