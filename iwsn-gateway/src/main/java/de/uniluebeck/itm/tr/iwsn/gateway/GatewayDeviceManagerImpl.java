package de.uniluebeck.itm.tr.iwsn.gateway;

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
import java.util.Map;
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
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

class GatewayDeviceManagerImpl extends AbstractService implements GatewayDeviceManager {

	private static final Logger log = LoggerFactory.getLogger(GatewayDeviceManager.class);

	private final Map<NodeUrn, GatewayDevice> devices = newHashMap();

	private final GatewayEventBus gatewayEventBus;

	private final DeviceConfigDB deviceConfigDB;

	private final GatewayDeviceFactory gatewayDeviceFactory;

	private final DeviceFactory deviceFactory;

	private ExecutorService deviceDriverExecutorService;

	@Inject
	public GatewayDeviceManagerImpl(final GatewayEventBus gatewayEventBus,
									final DeviceConfigDB deviceConfigDB,
									final GatewayDeviceFactory gatewayDeviceFactory,
									final DeviceFactory deviceFactory) {

		this.gatewayEventBus = checkNotNull(gatewayEventBus);
		this.deviceConfigDB = checkNotNull(deviceConfigDB);
		this.gatewayDeviceFactory = checkNotNull(gatewayDeviceFactory);
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
		synchronized (devices) {
			for (GatewayDevice gatewayDevice : devices.values()) {
				gatewayDevice.stopAndWait();
			}
			final Set<NodeUrn> detachedDevices = devices.keySet();
			devices.clear();
			postDevicesDetachedEvent(detachedDevices);
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

		DeviceConfig deviceConfig = getDeviceConfigFromDeviceDB(deviceInfo);

		if (deviceConfig == null) {
			log.warn("Ignoring attached event of unknown device: {}", deviceInfo);
			return;
		}

		final NodeUrn nodeUrn = new NodeUrn(deviceConfig.getNodeUrn());

		synchronized (devices) {

			if (!devices.containsKey(nodeUrn)) {

				try {

					final Device device = deviceFactory.create(
							deviceDriverExecutorService,
							deviceConfig.getNodeType(),
							deviceConfig.getNodeConfiguration()
					);

					device.connect(deviceInfo.getPort());

					log.info("{} => Successfully connected to {} device on serial port {}",
							nodeUrn, deviceConfig.getNodeType(), deviceInfo.getPort()
					);

					final GatewayDevice gatewayDevice = gatewayDeviceFactory.create(deviceConfig, device);
					devices.put(nodeUrn, gatewayDevice);

					postDevicesAttachedEvent(nodeUrn);

				} catch (Exception e) {
					log.error("{} => Could not connect to {} device at {}.", e);
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void onDeviceDetached(final DeviceInfo deviceInfo) {

		final DeviceConfig deviceConfig = getDeviceConfigFromDeviceDB(deviceInfo);

		if (deviceConfig == null) {
			log.warn("Ignoring detached event of unknown device: {}", deviceInfo);
			return;
		}

		final NodeUrn nodeUrn = new NodeUrn(deviceConfig.getNodeUrn());

		synchronized (devices) {

			if (devices.containsKey(nodeUrn)) {

				devices.remove(nodeUrn).stopAndWait();
				postDevicesDetachedEvent(nodeUrn);
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

	@Override
	public boolean isConnected(final NodeUrn nodeUrn) {
		synchronized (devices) {
			return devices.containsKey(nodeUrn);
		}
	}

	@Nullable
	@Override
	public GatewayDevice getDevice(final NodeUrn nodeUrn) {
		return devices.get(nodeUrn);
	}

	@Override
	public Map<NodeUrn, GatewayDevice> getConnectedSubset(final Iterable<NodeUrn> nodeUrns) {
		Map<NodeUrn, GatewayDevice> map = newHashMap();
		synchronized (devices) {
			for (NodeUrn nodeUrn : nodeUrns) {
				final GatewayDevice device = devices.get(nodeUrn);
				if (device != null) {
					map.put(nodeUrn, device);
				}
			}
		}
		return map;
	}

	@Override
	public Iterable<NodeUrn> getUnconnectedSubset(final Iterable<NodeUrn> nodeUrns) {
		synchronized (devices) {
			return filter(nodeUrns, not(in(devices.keySet())));
		}
	}
}
