package de.uniluebeck.itm.tr.iwsn.gateway.rest;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import javax.ws.rs.core.Application;
import java.util.Set;

public class GatewayRestApplication extends Application {

	private final GatewayRestService gatewayRestService;

	@Inject
	public GatewayRestApplication(final GatewayRestService gatewayRestService) {
		this.gatewayRestService = gatewayRestService;
	}

	@Override
	public Set<Object> getSingletons() {
		return Sets.<Object>newHashSet(gatewayRestService);
	}
}
