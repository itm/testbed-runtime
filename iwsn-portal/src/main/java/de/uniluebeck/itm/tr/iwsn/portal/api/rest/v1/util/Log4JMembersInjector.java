/**
 */
package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util;

import com.google.inject.MembersInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

class Log4JMembersInjector<T> implements MembersInjector<T> {

	private final Field field;

	private final Logger logger;

	Log4JMembersInjector(Field field) {
		this.field = field;
		this.logger = LoggerFactory.getLogger(field.getDeclaringClass());
		field.setAccessible(true);
	}

	public void injectMembers(T t) {
		try {
			field.set(t, logger);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}