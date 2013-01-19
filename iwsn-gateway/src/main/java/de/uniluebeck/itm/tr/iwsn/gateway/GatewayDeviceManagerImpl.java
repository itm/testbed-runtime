package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfigDB;
import de.uniluebeck.itm.tr.iwsn.messages.DevicesAttachedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceEvent;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceInfo;
import de.uniluebeck.itm.wsn.drivers.core.Device;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

class GatewayDeviceManagerImpl extends AbstractService implements GatewayDeviceManager {

	private static final Logger log = LoggerFactory.getLogger(GatewayDeviceManager.class);

	private final List<GatewayDeviceAdapter> gatewayDeviceAdapters = newArrayList();

	private final GatewayEventBus gatewayEventBus;

	private final DeviceConfigDB deviceConfigDB;

	private final GatewayDeviceAdapterFactory gatewayDeviceAdapterFactory;

	private final DeviceFactory deviceFactory;

	private ExecutorService deviceDriverExecutorService;

	@Inject
	public GatewayDeviceManagerImpl(final GatewayEventBus gatewayEventBus,
									final DeviceConfigDB deviceConfigDB,
									final GatewayDeviceAdapterFactory gatewayDeviceAdapterFactory,
									final DeviceFactory deviceFactory) {

		this.gatewayEventBus = checkNotNull(gatewayEventBus);
		this.deviceConfigDB = checkNotNull(deviceConfigDB);
		this.gatewayDeviceAdapterFactory = checkNotNull(gatewayDeviceAdapterFactory);
		this.deviceFactory = checkNotNull(deviceFactory);
	}

	@Override
	protected void doStart() {

		log.trace("GatewayDeviceManagerImpl.doStart()");

		try {
			final ThreadFactory tf = new ThreadFactoryBuilder().setNameFormat("DeviceDriverExecutor %d").build();
			deviceDriverExecutorService = Executors.newCachedThreadPool(tf);
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
			shutdownAllDevices();
			ExecutorUtils.shutdown(deviceDriverExecutorService, 10, TimeUnit.SECONDS);
			gatewayEventBus.unregister(this);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	private void shutdownAllDevices() {
		synchronized (gatewayDeviceAdapters) {
			for (GatewayDeviceAdapter gatewayDeviceAdapter : gatewayDeviceAdapters) {
				gatewayDeviceAdapter.stopAndWait();
				postDevicesDetachedEvent(gatewayDeviceAdapter.getNodeUrns());
			}
			gatewayDeviceAdapters.clear();
		}
	}

	@Subscribe
	public void onEventFromDeviceObserver(final DeviceEvent event) {
		switch (event.getType()) {
			case ATTACHED:
				onDeviceAttached(event.getDeviceInfo());
				break;
			case REMOVED:
				onDeviceDetached(event.getDeviceInfo());
				break;
		}
	}

	private void onDeviceAttached(final DeviceInfo deviceInfo) {

		log.trace("GatewayDeviceManagerImpl.onDeviceAttached({})", deviceInfo);

		DeviceConfig deviceConfig = getDeviceConfigFromDeviceDB(deviceInfo);

		if (deviceConfig == null) {
			log.warn("Ignoring attached event of unknown device: {}", deviceInfo);
			return;
		}

		if (getGatewayDeviceAdapter(deviceConfig.getNodeUrn()) == null) {

			try {

				final Device device = deviceFactory.create(
						deviceDriverExecutorService,
						deviceConfig.getNodeType(),
						deviceConfig.getNodeConfiguration()
				);

				device.connect(deviceInfo.getPort());

				log.info("{} => Successfully connected to {} device on serial port {}",
						deviceConfig.getNodeUrn(), deviceConfig.getNodeType(), deviceInfo.getPort()
				);

				synchronized (gatewayDeviceAdapters) {

					final GatewayDeviceAdapter gatewayDeviceAdapter = gatewayDeviceAdapterFactory.create(
							deviceConfig,
							device
					);

					gatewayDeviceAdapter.startAndWait();
					gatewayDeviceAdapters.add(gatewayDeviceAdapter);
				}

				postDevicesAttachedEvent(deviceConfig.getNodeUrn());

			} catch (Exception e) {
				log.error("{} => Could not connect to {} device at {}.", e);
				throw new RuntimeException(e);
			}
		}
	}

	private void onDeviceDetached(final DeviceInfo deviceInfo) {

		log.trace("GatewayDeviceManagerImpl.onDeviceDetached({})", deviceInfo);

		final DeviceConfig deviceConfig = getDeviceConfigFromDeviceDB(deviceInfo);

		if (deviceConfig == null) {
			log.warn("Ignoring detached event of unknown device: {}", deviceInfo);
			return;
		}

		final GatewayDeviceAdapter gatewayDeviceAdapter = getGatewayDeviceAdapter(deviceConfig.getNodeUrn());

		if (gatewayDeviceAdapter != null) {

			synchronized (gatewayDeviceAdapters) {

				gatewayDeviceAdapters.remove(gatewayDeviceAdapter);
				gatewayDeviceAdapter.stopAndWait();
				postDevicesDetachedEvent(deviceConfig.getNodeUrn());
			}
		}
	}

	/**
	 * Returns all information about the device from the DeviceDB or {@code null} if no information is found.
	 *
	 * @param deviceInfo
	 * 		the device observer event
	 *
	 * @return a {@code DeviceConfig} instance or {@code null} if no info is found
	 */
	@Nullable
	private DeviceConfig getDeviceConfigFromDeviceDB(final DeviceInfo deviceInfo) {
		if (deviceInfo.getMacAddress() != null) {
			return deviceConfigDB.getByMacAddress(deviceInfo.getMacAddress().toLong());
		} else if (deviceInfo.getReference() != null) {
			return deviceConfigDB.getByUsbChipId(deviceInfo.getReference());
		}
		return null;
	}

	private void postDevicesDetachedEvent(final NodeUrn nodeUrn) {
		postDevicesDetachedEvent(newHashSet(nodeUrn));
	}

	private void postDevicesAttachedEvent(final NodeUrn nodeUrn) {
		postDevicesAttachedEvent(newHashSet(nodeUrn));
	}

	private void postDevicesAttachedEvent(final Set<NodeUrn> nodeUrns) {
		gatewayEventBus.post(DevicesAttachedEvent
				.newBuilder()
				.addAllNodeUrns(transform(nodeUrns, toStringFunction()))
				.setTimestamp(new DateTime().getMillis())
				.build()
		);
	}

	private void postDevicesDetachedEvent(final Set<NodeUrn> nodeUrns) {
		gatewayEventBus.post(DevicesDetachedEvent
				.newBuilder()
				.addAllNodeUrns(transform(nodeUrns, toStringFunction()))
				.setTimestamp(new DateTime().getMillis())
				.build()
		);
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
