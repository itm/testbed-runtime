package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.Set;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.devicedb.DeviceConfig.TO_NODE_URN_FUNCTION;

public class DeviceDBServedNodeUrnsProvider implements ServedNodeUrnsProvider {

	private final DeviceDBService deviceDBService;

	@Inject
	public DeviceDBServedNodeUrnsProvider(final DeviceDBService deviceDBService) {
		this.deviceDBService = deviceDBService;
	}

	@Override
	public Set<NodeUrn> get() {
		return newHashSet(transform(deviceDBService.getAll(), TO_NODE_URN_FUNCTION));
	}
}
