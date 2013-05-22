package de.uniluebeck.itm.tr.rs.persistence.jpa;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.persist.jpa.JpaPersistModule;
import de.uniluebeck.itm.tr.common.jpa.JPAInitializer;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;

import java.util.Properties;
import java.util.TimeZone;

public class RSPersistenceJPAModule extends AbstractModule {

	private final Properties properties;

	private final TimeZone timeZone;

	@Inject
	public RSPersistenceJPAModule(final Properties properties, final TimeZone timeZone) {
		this.properties = properties;
		this.timeZone = timeZone;
	}

	@Override
	protected void configure() {
		bind(TimeZone.class).toInstance(timeZone);
		install(new JpaPersistModule("RS").properties(properties));
		bind(JPAInitializer.class).asEagerSingleton();
		bind(RSPersistence.class).to(RSPersistenceJPA.class);
	}
}
