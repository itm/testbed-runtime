package de.uniluebeck.itm.tr.rs;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.PrivateModule;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.DecoratedImpl;
import de.uniluebeck.itm.tr.common.EndpointManager;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.rs.persistence.gcal.GCalRSPersistenceModule;
import de.uniluebeck.itm.tr.rs.persistence.inmemory.InMemoryRSPersistenceModule;
import de.uniluebeck.itm.tr.rs.persistence.jpa.RSPersistenceJPAModule;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.snaa.SNAA;

public class RSServiceModule extends PrivateModule {

	protected final RSServiceConfig rsServiceConfig;

	protected final CommonConfig commonConfig;

	public RSServiceModule(final CommonConfig commonConfig, final RSServiceConfig rsServiceConfig) {
		this.commonConfig = commonConfig;
		this.rsServiceConfig = rsServiceConfig;
	}

	@Override
	protected void configure() {

		requireBinding(CommonConfig.class);
		requireBinding(RSServiceConfig.class);

		requireBinding(ServicePublisher.class);
		requireBinding(SNAA.class);
		requireBinding(SessionManagement.class);
		requireBinding(EndpointManager.class);
		requireBinding(TimeLimiter.class);
		requireBinding(ServedNodeUrnsProvider.class);

		switch (rsServiceConfig.getRsPersistenceType()) {
			case GCAL:
				install(new GCalRSPersistenceModule(rsServiceConfig.getRsPersistenceConfig()));
				break;
			case IN_MEMORY:
				install(new InMemoryRSPersistenceModule());
				break;
			case JPA:
				install(new RSPersistenceJPAModule(rsServiceConfig.getRsPersistenceConfig(), commonConfig.getTimeZone()));
				break;
			default:
				throw new RuntimeException("Unknown RS persistence type: \"" + rsServiceConfig.getRsPersistenceType() + "\"");
		}

		bind(RSService.class).to(SingleUrnPrefixRSService.class);
		bind(RS.class).annotatedWith(DecoratedImpl.class).to(SingleUrnPrefixRS.class);
		bind(RS.class).to(RSAuthorizationDecorator.class);

		expose(RSService.class);
		expose(RS.class);
	}
}
