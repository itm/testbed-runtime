package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
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

class GatewayDeviceManagerImpl extends AbstractService implements GatewayDeviceManager {

	private static final Logger log = LoggerFactory.getLogger(GatewayDeviceManager.class);

	private final List<GatewayDeviceAdapter> gatewayDeviceAdapters = newArrayList();

	private final GatewayEventBus gatewayEventBus;

	@Inject
	public GatewayDeviceManagerImpl(final GatewayEventBus gatewayEventBus) {
		this.gatewayEventBus = checkNotNull(gatewayEventBus);
	}

	@Override
	protected void doStart() {

		log.trace("GatewayDeviceManagerImpl.doStart()");

		try {
			gatewayEventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		log.trace("GatewayDeviceManagerImpl.doStop()");

		try {

			synchronized (gatewayDeviceAdapters) {
				for (GatewayDeviceAdapter gatewayDeviceAdapter : gatewayDeviceAdapters) {
					gatewayDeviceAdapter.stopAndWait();
				}
				gatewayDeviceAdapters.clear();
			}

			gatewayEventBus.unregister(this);
			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Nullable
	@Override
	public GatewayDeviceAdapter getGatewayDeviceAdapter(final NodeUrn nodeUrn) {
		synchronized (gatewayDeviceAdapters) {
			for (GatewayDeviceAdapter gatewayDeviceAdapter : gatewayDeviceAdapters) {
				if (gatewayDeviceAdapter.getNodeUrns().contains(nodeUrn)) {
					return gatewayDeviceAdapter;
				}
			}
		}
		return null;
	}

	@Override
	public Iterable<GatewayDeviceAdapter> getGatewayDeviceAdapters() {
		synchronized (gatewayDeviceAdapters) {
			return Iterables.unmodifiableIterable(gatewayDeviceAdapters);
		}
	}

	@Override
	public Set<NodeUrn> getCurrentlyConnectedNodeUrns() {
		synchronized (gatewayDeviceAdapters) {
			final ImmutableSet.Builder<NodeUrn> nodeUrns = ImmutableSet.builder();
			for (GatewayDeviceAdapter gatewayDeviceAdapter : gatewayDeviceAdapters) {
				nodeUrns.addAll(gatewayDeviceAdapter.getNodeUrns());
			}
			return nodeUrns.build();
		}
	}

	@Override
	public Multimap<GatewayDeviceAdapter, NodeUrn> getConnectedSubset(final Iterable<NodeUrn> nodeUrns) {

		final Set<NodeUrn> requestedNodeUrns = newHashSet(nodeUrns);
		final Multimap<GatewayDeviceAdapter, NodeUrn> map = HashMultimap.create();

		synchronized (gatewayDeviceAdapters) {

			for (GatewayDeviceAdapter gatewayDeviceAdapter : gatewayDeviceAdapters) {

				final Iterable<NodeUrn> filtered = filter(
						gatewayDeviceAdapter.getNodeUrns(),
						in(requestedNodeUrns)
				);

				if (!Iterables.isEmpty(filtered)) {
					map.putAll(gatewayDeviceAdapter, filtered);
				}
			}
		}
		return map;
	}

	@Override
	public Iterable<NodeUrn> getUnconnectedSubset(final Iterable<NodeUrn> nodeUrns) {
		return filter(nodeUrns, not(in(getCurrentlyConnectedNodeUrns())));
	}
}
