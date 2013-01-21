package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

class DeviceManagerImpl extends AbstractService implements DeviceManager {

	private static final Logger log = LoggerFactory.getLogger(DeviceManager.class);

	private final List<DeviceAdapter> deviceAdapters = newArrayList();

	private final GatewayEventBus gatewayEventBus;

	@Inject
	public DeviceManagerImpl(final GatewayEventBus gatewayEventBus) {
		this.gatewayEventBus = checkNotNull(gatewayEventBus);
	}

	@Override
	protected void doStart() {

		log.trace("DeviceManagerImpl.doStart()");

		try {
			gatewayEventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		log.trace("DeviceManagerImpl.doStop()");

		try {

			synchronized (deviceAdapters) {
				for (DeviceAdapter deviceAdapter : deviceAdapters) {
					deviceAdapter.stopAndWait();
				}
				deviceAdapters.clear();
			}

			gatewayEventBus.unregister(this);
			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Subscribe
	public void onDevicesAttachedEvent(final DevicesAttachedEvent event) {
		log.trace("DeviceManagerImpl.onDevicesAttachedEvent({})", event);
		synchronized (deviceAdapters) {
			if (!deviceAdapters.contains(event.getDeviceAdapter())) {
				deviceAdapters.add(event.getDeviceAdapter());
			}
		}
	}

	@Subscribe
	public void onDevicesDetachedEvent(final DevicesDetachedEvent event) {
		log.trace("DeviceManagerImpl.onDevicesDetachedEvent({})", event);
		if (event.getDeviceAdapter().getNodeUrns().isEmpty()) {
			synchronized (deviceAdapters) {
				deviceAdapters.remove(event.getDeviceAdapter());
			}
		}
	}

	@Nullable
	@Override
	public DeviceAdapter getGatewayDeviceAdapter(final NodeUrn nodeUrn) {
		synchronized (deviceAdapters) {
			for (DeviceAdapter deviceAdapter : deviceAdapters) {
				if (deviceAdapter.getNodeUrns().contains(nodeUrn)) {
					return deviceAdapter;
				}
			}
		}
		return null;
	}

	@Override
	public Iterable<DeviceAdapter> getDeviceAdapters() {
		synchronized (deviceAdapters) {
			return Iterables.unmodifiableIterable(deviceAdapters);
		}
	}

	@Override
	public Set<NodeUrn> getConnectedNodeUrns() {
		synchronized (deviceAdapters) {
			final ImmutableSet.Builder<NodeUrn> nodeUrns = ImmutableSet.builder();
			for (DeviceAdapter deviceAdapter : deviceAdapters) {
				nodeUrns.addAll(deviceAdapter.getNodeUrns());
			}
			return nodeUrns.build();
		}
	}

	@Override
	public Multimap<DeviceAdapter, NodeUrn> getConnectedSubset(final Iterable<NodeUrn> nodeUrns) {

		final Set<NodeUrn> requestedNodeUrns = newHashSet(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> map = HashMultimap.create();

		synchronized (deviceAdapters) {

			for (DeviceAdapter deviceAdapter : deviceAdapters) {

				final Iterable<NodeUrn> filtered = filter(
						deviceAdapter.getNodeUrns(),
						in(requestedNodeUrns)
				);

				if (!Iterables.isEmpty(filtered)) {
					map.putAll(deviceAdapter, filtered);
				}
			}
		}
		return map;
	}

	@Override
	public Iterable<NodeUrn> getUnconnectedSubset(final Iterable<NodeUrn> nodeUrns) {
		return filter(nodeUrns, not(in(getConnectedNodeUrns())));
	}
}
