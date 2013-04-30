package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.common.rest.RestApplicationBase;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public class DeviceDBRestApplication extends RestApplicationBase {

	private final DeviceDBRestResourceImpl resource;

	@Inject
	public DeviceDBRestApplication(final DeviceDBRestResourceImpl resource) {
		this.resource = resource;
	}

	@Override
	public Set<?> getSingletonsInternal() {
		return newHashSet(resource);
	}
}
