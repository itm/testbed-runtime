package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.slf4j.Logger;

import java.lang.reflect.Field;

public class Log4JTypeListener implements TypeListener {

	public <T> void hear(TypeLiteral<T> typeLiteral, TypeEncounter<T> typeEncounter) {
		for (Field field : typeLiteral.getRawType().getDeclaredFields()) {
			if (field.getType() == Logger.class && field.isAnnotationPresent(InjectLogger.class)) {
				typeEncounter.register(new Log4JMembersInjector<T>(field));
			}
		}
	}
}