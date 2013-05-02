package de.uniluebeck.itm.tr.rs.persistence.gcal;

import com.google.inject.AbstractModule;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;

import java.util.Properties;

public class GCalRSPersistenceModule extends AbstractModule {

	private final Properties properties;

	public GCalRSPersistenceModule(final Properties properties) {
		this.properties = properties;
	}

	@Override
	protected void configure() {

		final String username = (String) properties.get("username");
		final String password = (String) properties.get("password");

		bind(RSPersistence.class).toInstance(new GCalRSPersistence(username, password));
	}
}
