package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1;

import com.google.inject.AbstractModule;

public class RestApiModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(RestApiService.class).to(RestApiServiceImpl.class);
	}
}
