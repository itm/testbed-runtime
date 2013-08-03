package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.WisemlProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerFactory;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.RequestIdProvider;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.*;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws.EventWebSocketFactory;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws.WsnWebSocketFactory;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.snaa.SNAA;

public class RestApiModule extends AbstractModule {

	@Override
	protected void configure() {

		requireBinding(CommonConfig.class);
		requireBinding(PortalServerConfig.class);
		requireBinding(RS.class);
		requireBinding(SNAA.class);
		requireBinding(ResponseTrackerFactory.class);
		requireBinding(PortalEventBus.class);
		requireBinding(RequestIdProvider.class);
		requireBinding(ReservationManager.class);
		requireBinding(ServicePublisher.class);
		requireBinding(WisemlProvider.class);

		bind(RestApiService.class).to(RestApiServiceImpl.class);
		bind(ExperimentResource.class).to(ExperimentResourceImpl.class);
		bind(CookieResource.class).to(CookieResourceImpl.class);
		bind(RemoteExperimentConfigurationResource.class).to(RemoteExperimentConfigurationResourceImpl.class);
		bind(RsResource.class).to(RsResourceImpl.class);
		bind(SnaaResource.class).to(SnaaResourceImpl.class);
		bind(TestbedsResource.class).to(TestbedsResourceImpl.class);

		install(new FactoryModuleBuilder().build(WsnWebSocketFactory.class));
		install(new FactoryModuleBuilder().build(EventWebSocketFactory.class));
	}
}
