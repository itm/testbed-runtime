package de.uniluebeck.itm.tr.iwsn.gateway.rest;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import javax.ws.rs.core.Application;
import java.util.Set;

public class RestApplication extends Application {

	private final RestService restService;

	@Inject
	public RestApplication(final RestService restService) {
		this.restService = restService;
	}

	@Override
	public Set<Object> getSingletons() {
		return Sets.<Object>newHashSet(restService);
	}
}
