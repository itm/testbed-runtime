package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.WisemlProvider;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerFactory;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.RequestIdProvider;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.*;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws.EventWebSocketFactory;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws.WsnWebSocketFactory;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.snaa.SNAA;

public class RestApiModule extends AbstractModule {

	public static final String IS_FEDERATOR = "RestApiModule.IS_FEDERATOR";

	private final boolean federator;

	public RestApiModule(final boolean federator) {
		this.federator = federator;
	}

	@Override
	protected void configure() {

		// services
		requireBinding(RS.class);
		requireBinding(SNAA.class);
		requireBinding(WisemlProvider.class);

		// internals
		requireBinding(PortalEventBus.class);
		requireBinding(ReservationManager.class);

		// helpers
		requireBinding(ResponseTrackerFactory.class);
		requireBinding(RequestIdProvider.class);
		requireBinding(ServicePublisher.class);

		bind(RestApiService.class).to(RestApiServiceImpl.class);
		bind(ExperimentResource.class).to(ExperimentResourceImpl.class);
		bind(CookieResource.class).to(CookieResourceImpl.class);
		bind(RemoteExperimentConfigurationResource.class).to(RemoteExperimentConfigurationResourceImpl.class);
		bind(RsResource.class).to(RsResourceImpl.class);
		bind(SnaaResource.class).to(SnaaResourceImpl.class);
		bind(EventStoreResource.class).to(EventStoreResourceImpl.class);
		bind(RootResource.class).to(RootResourceImpl.class);

		bindConstant().annotatedWith(Names.named(IS_FEDERATOR)).to(federator);

		install(new FactoryModuleBuilder().build(WsnWebSocketFactory.class));
		install(new FactoryModuleBuilder().build(EventWebSocketFactory.class));
	}
}
