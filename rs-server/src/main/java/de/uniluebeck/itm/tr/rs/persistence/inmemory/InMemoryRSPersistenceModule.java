package de.uniluebeck.itm.tr.rs.persistence.inmemory;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;

public class InMemoryRSPersistenceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(RSPersistence.class).to(InMemoryRSPersistence.class).in(Scopes.SINGLETON);
	}
}
