package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@BindingAnnotation
@Target({TYPE, FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface WSNFederator {

}
