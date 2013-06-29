package de.uniluebeck.itm.tr.rs.persistence.gcal;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import de.uniluebeck.itm.tr.rs.RSServiceConfig;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;

public class GCalRSPersistenceModule extends AbstractModule {

	@Override
	protected void configure() {
		requireBinding(RSServiceConfig.class);
		bind(RSPersistence.class).to(GCalRSPersistence.class).in(Scopes.SINGLETON);
	}
}
