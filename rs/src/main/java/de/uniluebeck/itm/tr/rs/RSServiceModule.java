package de.uniluebeck.itm.tr.rs;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.PrivateModule;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.DecoratedImpl;
import de.uniluebeck.itm.tr.common.EventBusService;
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
		requireBinding(TimeLimiter.class);
		requireBinding(ServedNodeUrnsProvider.class);

		requireBinding(EventBusService.class);

		switch (rsServiceConfig.getRsType()) {
			case GCAL:
				install(new GCalRSPersistenceModule());
				bindToSingleUrnPrefixRS();
				break;
			case IN_MEMORY:
				install(new InMemoryRSPersistenceModule());
				bindToSingleUrnPrefixRS();
				break;
			case JPA:
				install(new RSPersistenceJPAModule(commonConfig, rsServiceConfig));
				bindToSingleUrnPrefixRS();
				break;
			default:
				throw new RuntimeException("Unknown RS persistence type: \"" + rsServiceConfig.getRsType() + "\"");
		}

		expose(RS.class);
		expose(RSService.class);
	}

	private void bindToSingleUrnPrefixRS() {
		bind(RSService.class).to(SingleUrnPrefixRSService.class);
		bind(RS.class).annotatedWith(DecoratedImpl.class).to(SingleUrnPrefixRS.class);
		bind(RS.class).to(RSAuthorizationDecorator.class);
	}
}
