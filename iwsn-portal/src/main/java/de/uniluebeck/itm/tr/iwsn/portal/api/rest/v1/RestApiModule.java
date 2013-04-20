package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws.WsnWebSocketFactory;

public class RestApiModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(RestApiService.class).to(RestApiServiceImpl.class);
		install(new FactoryModuleBuilder().build(WsnWebSocketFactory.class));
	}
}
