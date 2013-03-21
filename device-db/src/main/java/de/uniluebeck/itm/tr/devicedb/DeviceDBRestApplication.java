package de.uniluebeck.itm.tr.devicedb;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import javax.ws.rs.core.Application;
import java.util.Set;

public class DeviceDBRestApplication extends Application {

	private final DeviceDBRestResourceImpl resource;

	@Inject
	public DeviceDBRestApplication(final DeviceDBRestResourceImpl resource) {
		this.resource = resource;
	}

	@Override
	public Set<Object> getSingletons() {
		return Sets.<Object>newHashSet(resource);
	}
}
