package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Inject;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;

import javax.ws.rs.core.Application;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public class DeviceDBRestApplication extends Application {

	private final DeviceDBRestResourceImpl resource;

	@Inject
	public DeviceDBRestApplication(final DeviceDBRestResourceImpl resource) {
		this.resource = resource;
	}

	@Override
	public Set<Object> getSingletons() {
		final JSONProvider jsonProvider = new JSONProvider();
		jsonProvider.setDropRootElement(true);
		jsonProvider.setSupportUnwrapped(true);
		jsonProvider.setDropCollectionWrapperElement(true);
		jsonProvider.setSerializeAsArray(true);
		return newHashSet(resource, jsonProvider);
	}
}
