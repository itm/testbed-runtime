package de.uniluebeck.itm.tr.rs.persistence.jpa;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.persist.jpa.JpaPersistModule;
import de.uniluebeck.itm.tr.common.jpa.JPAInitializer;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;

import java.util.Properties;

public class RSPersistenceJPAModule extends AbstractModule {

	private final Properties properties;

	@Inject
	public RSPersistenceJPAModule(final Properties properties) {
		this.properties = properties;
	}

	@Override
	protected void configure() {
		install(new JpaPersistModule("RS").properties(properties));
		bind(JPAInitializer.class).asEagerSingleton();
		bind(RSPersistence.class).to(RSPersistenceJPA.class);
	}
}
