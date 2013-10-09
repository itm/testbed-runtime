package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceFoundEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceLostEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesConnectedEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesDisconnectedEvent;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Sets.intersection;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newNotificationEvent;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newUpstreamMessageEvent;

class DeviceManagerImpl extends AbstractService implements DeviceManager {

	private static final Logger log = LoggerFactory.getLogger(DeviceManager.class);

	private final Set<DeviceFoundEvent> devicesFoundWithoutConfig = newHashSet();

	private final Multimap<String, DeviceConfig> detectedButNotConnectedDevices = HashMultimap.create();

	private final Set<DeviceAdapter> runningDeviceAdapters = newHashSet();

	private final GatewayEventBus gatewayEventBus;

	private final DeviceAdapterRegistry deviceAdapterRegistry;

	private final Set<DeviceAdapterFactory> builtInDeviceAdapterFactories;

	private final SchedulerService schedulerService;

	private final DeviceDBService deviceDBService;

	private Runnable tryToRetrieveConfigsForFoundDevicesRunnable = new Runnable() {

		@Override
		public void run() {

			log.trace("DeviceManagerImpl.tryToRetrieveConfigsForFoundDevicesRunnable.run()");

			final Set<DeviceFoundEvent> deviceFoundEvents;
			synchronized (devicesFoundWithoutConfig) {
				deviceFoundEvents = newHashSet(devicesFoundWithoutConfig);
			}

			boolean foundConfigs = false;
			for (DeviceFoundEvent deviceFoundEvent : deviceFoundEvents) {
				final DeviceConfig deviceConfig = getDeviceConfigFromDeviceDB(deviceFoundEvent);
				if (deviceConfig == null) {
					log.warn("Ignoring device unknown to DeviceDB: {}", deviceFoundEvent);
				} else {
					foundConfigs = true;
					synchronized (detectedButNotConnectedDevices) {
						detectedButNotConnectedDevices.put(deviceFoundEvent.getPort(), deviceConfig);
					}
					synchronized (devicesFoundWithoutConfig) {
						devicesFoundWithoutConfig.remove(deviceFoundEvent);
					}
				}
			}

			if (foundConfigs) {
				schedulerService.execute(tryToConnectToUnconnectedDevicesRunnable);
			}
		}
	};

	private Runnable tryToConnectToUnconnectedDevicesRunnable = new Runnable() {
		@Override
		public void run() {

			final Multimap<String, DeviceConfig> deviceConfigs;
			synchronized (detectedButNotConnectedDevices) {
				deviceConfigs = HashMultimap.create(detectedButNotConnectedDevices);
			}

			final Function<Map.Entry<String, DeviceConfig>, String> entryToStringFunction =
					new Function<Map.Entry<String, DeviceConfig>, String>() {
						@Override
						public String apply(final Map.Entry<String, DeviceConfig> input) {
							return input.getKey() + "=" + input.getValue().getNodeUrn();
						}
					};

			if (log.isTraceEnabled()) {
				log.trace("Before retrying to connect: detectedButNotConnectedDevices: {}",
						Iterables.transform(detectedButNotConnectedDevices.entries(),
								entryToStringFunction
						)
				);
			}

			for (final String port : deviceConfigs.keys()) {
				for (final DeviceConfig deviceConfig : deviceConfigs.get(port)) {
					if (tryToConnect(port, deviceConfig)) {
						synchronized (detectedButNotConnectedDevices) {
							detectedButNotConnectedDevices.remove(port, deviceConfig);
						}
					}
				}
			}

			if (log.isTraceEnabled()) {
				log.trace("After retrying to connect: detectedButNotConnectedDevices: {}",
						Iterables.transform(detectedButNotConnectedDevices.entries(),
								entryToStringFunction
						)
				);
			}
		}

		private boolean tryToConnect(final String port, final DeviceConfig deviceConfig) {

			log.trace("DeviceManagerImpl.tryToConnect({})", deviceConfig.getNodeUrn());

			// if already connected return true
			synchronized (runningDeviceAdapters) {
				for (final DeviceAdapter deviceAdapter : runningDeviceAdapters) {
					if (deviceAdapter.getNodeUrns().contains(deviceConfig.getNodeUrn())) {
						return true;
					}
				}
			}

			// first try to find a suitable DeviceAdapterFactory from plugins using the registry
			// (allows overriding built-in drivers)
			boolean connected = tryToConnect(port, deviceConfig, deviceAdapterRegistry.getDeviceAdapterFactories());

			// if failed, try with the built-in DeviceAdapterFactory instances
			if (!connected) {
				connected = tryToConnect(port, deviceConfig, builtInDeviceAdapterFactories);
			}

			log.debug("{} to {} device with URN {}",
					(connected ? "Connected" : "Failed connecting"),
					deviceConfig.getNodeType(),
					deviceConfig.getNodeUrn()
			);
			return connected;
		}

		private boolean tryToConnect(final String port,
									 final DeviceConfig deviceConfig,
									 final Set<DeviceAdapterFactory> deviceAdapterFactories) {

			for (DeviceAdapterFactory deviceAdapterFactory : deviceAdapterFactories) {

				if (deviceAdapterFactory.canHandle(deviceConfig)) {

					final DeviceAdapter deviceAdapter = deviceAdapterFactory.create(port, deviceConfig);
					final DeviceAdapterListener deviceAdapterListener = new ManagerDeviceAdapterListener();
					final DeviceAdapterServiceListener listener = new DeviceAdapterServiceListener(
							deviceAdapter,
							deviceAdapterListener
					);

					try {
						deviceAdapter.addListener(deviceAdapterListener);
						deviceAdapter.addListener(listener, sameThreadExecutor());
					} catch (Exception e) {
						log.error("Exception while adding DeviceAdapterListener to DeviceAdapter: ", e);
						return false;
					}

					try {

						deviceAdapter.startAndWait();

					} catch (Exception e) {
						log.error("{} => Could not connect to {} device at {}: {}",
								deviceConfig.getNodeUrn(),
								deviceConfig.getNodeType(),
								deviceConfig.getNodePort(),
								getStackTraceAsString(e)
						);
					}

					// Note that the device will not be marked as connected until the device adapter throws
					// a DevicesConnectedEvent indicating a successful connection.
					// Another method listens for this event and marks the device as connected when the
					// corresponding event is consumed.

					return true;
				}
			}
			return false;
		}
	};

	@Nullable
	private ScheduledFuture<?> tryToConnectToDetectedButUnconnectedDevicesSchedule;

	@Nullable
	private ScheduledFuture<?> tryToRetrieveConfigsForFoundDevicesSchedule;

	private class ManagerDeviceAdapterListener implements DeviceAdapterListener {

		@Override
		public void onDevicesConnected(final DeviceAdapter deviceAdapter,
									   final Set<NodeUrn> nodeUrnsConnected) {
			gatewayEventBus.post(new DevicesConnectedEvent(deviceAdapter, nodeUrnsConnected));
		}

		@Override
		public void onDevicesDisconnected(final DeviceAdapter deviceAdapter,
										  final Set<NodeUrn> nodeUrnsDisconnected) {
			gatewayEventBus.post(new DevicesDisconnectedEvent(deviceAdapter, nodeUrnsDisconnected));
		}

		@Override
		public void onBytesReceivedFromDevice(final DeviceAdapter deviceAdapter, final NodeUrn nodeUrn,
											  final byte[] bytes) {
			gatewayEventBus.post(newUpstreamMessageEvent(nodeUrn, bytes, DateTime.now()));
		}

		@Override
		public void onNotification(final DeviceAdapter deviceAdapter, final NodeUrn nodeUrn,
								   final String notification) {
			gatewayEventBus.post(newNotificationEvent(nodeUrn, notification));
		}
	}

	private class DeviceAdapterServiceListener implements Listener {

		private final DeviceAdapter deviceAdapter;

		private final DeviceAdapterListener deviceAdapterListener;

		private DeviceAdapterServiceListener(final DeviceAdapter deviceAdapter,
											 final DeviceAdapterListener deviceAdapterListener) {
			this.deviceAdapter = deviceAdapter;
			this.deviceAdapterListener = deviceAdapterListener;
		}

		@Override
		public void starting() {
			// do nothing, listener is already attached
		}

		@Override
		public void running() {

			log.trace("DeviceManagerImpl$DeviceAdapterServiceListener.running()");

			synchronized (runningDeviceAdapters) {
				runningDeviceAdapters.add(deviceAdapter);
			}
		}

		@Override
		public void stopping(final State from) {

			log.trace("DeviceManagerImpl.DeviceAdapterListener.stopping()");

			synchronized (runningDeviceAdapters) {
				runningDeviceAdapters.remove(deviceAdapter);
			}
		}

		@Override
		public void terminated(final State from) {
			removeListener();
		}

		@Override
		public void failed(final State from, final Throwable failure) {
			removeListener();
		}

		private void removeListener() {
			try {
				deviceAdapter.removeListener(deviceAdapterListener);
			} catch (Exception e) {
				log.error("Exception while removing DeviceAdapterListener from DeviceAdapter: ", e);
			}
		}
	}

	@Inject
	public DeviceManagerImpl(final GatewayEventBus gatewayEventBus,
							 final SchedulerService schedulerService,
							 final Set<DeviceAdapterFactory> builtInDeviceAdapterFactories,
							 final DeviceAdapterRegistry deviceAdapterRegistry,
							 final DeviceDBService deviceDBService) {
		this.gatewayEventBus = checkNotNull(gatewayEventBus);
		this.schedulerService = checkNotNull(schedulerService);
		this.builtInDeviceAdapterFactories = checkNotNull(builtInDeviceAdapterFactories);
		this.deviceAdapterRegistry = checkNotNull(deviceAdapterRegistry);
		this.deviceDBService = checkNotNull(deviceDBService);
	}

	@Override
	protected void doStart() {

		log.trace("DeviceManagerImpl.doStart()");

		try {

			gatewayEventBus.register(this);

			tryToRetrieveConfigsForFoundDevicesSchedule = schedulerService.scheduleWithFixedDelay(
					tryToRetrieveConfigsForFoundDevicesRunnable,
					15, 30, TimeUnit.SECONDS
			);

			tryToConnectToDetectedButUnconnectedDevicesSchedule = schedulerService.scheduleWithFixedDelay(
					tryToConnectToUnconnectedDevicesRunnable,
					30, 30, TimeUnit.SECONDS
			);

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		log.trace("DeviceManagerImpl.doStop()");

		try {

			try {
				if (tryToRetrieveConfigsForFoundDevicesSchedule != null) {
					tryToRetrieveConfigsForFoundDevicesSchedule.cancel(false);
					tryToRetrieveConfigsForFoundDevicesSchedule = null;
				}
			} catch (Exception e) {
				log.error(
						"Exception while stopping scheduled task that tries to connected with devices that have been detected but not connected to yet: ",
						e
				);
			}

			try {
				if (tryToConnectToDetectedButUnconnectedDevicesSchedule != null) {
					tryToConnectToDetectedButUnconnectedDevicesSchedule.cancel(false);
					tryToConnectToDetectedButUnconnectedDevicesSchedule = null;
				}
			} catch (Exception e) {
				log.error(
						"Exception while stopping scheduled task that tries to connected with devices that have been detected but not connected to yet: ",
						e
				);
			}

			// copy before stopping to avoid concurrent modification exceptions due to service listeners
			final Set<DeviceAdapter> adapters;
			synchronized (runningDeviceAdapters) {
				adapters = newHashSet(runningDeviceAdapters);
			}

			for (DeviceAdapter deviceAdapter : adapters) {
				deviceAdapter.stopAndWait();
			}

			gatewayEventBus.unregister(this);
			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	/**
	 * Consumes for events indicating that a new device was connected to this gateway.<br/> If there is no {@link
	 * DeviceAdapter} responsible for this device yet, a new instance for this device will be created and started.
	 *
	 * @param deviceFoundEvent
	 * 		event indicating that a new device was connected to this gateway
	 */
	@Subscribe
	public void onDeviceFoundEvent(final DeviceFoundEvent deviceFoundEvent) {

		log.trace("DeviceManagerImpl.onDeviceFoundEvent({})", deviceFoundEvent);

		DeviceConfig deviceConfig = deviceFoundEvent.getDeviceConfig();
		if (deviceConfig == null) {
			try {
				deviceConfig = getDeviceConfigFromDeviceDB(deviceFoundEvent);
				if (deviceConfig == null) {
					log.warn("Ignoring device unknown to DeviceDB: {}", deviceFoundEvent);
				}
			} catch (Exception e) {
				log.error("Exception while fetching device configuration from DeviceDB: {}", e.getMessage());
			}
		}

		if (deviceConfig == null) {
			synchronized (devicesFoundWithoutConfig) {
				devicesFoundWithoutConfig.add(deviceFoundEvent);
			}
		} else {
			synchronized (detectedButNotConnectedDevices) {
				detectedButNotConnectedDevices.put(deviceFoundEvent.getPort(), deviceConfig);
			}
		}

		schedulerService.execute(tryToConnectToUnconnectedDevicesRunnable);
	}

	/**
	 * Consumes events indicating that a device was disconnected from this gateway.<br/> If such an event is consumed,
	 * the gateway's node urn is removed from the {@link DeviceAdapter} which logically connects the device to the
	 * gateway. If there are no other devices connected via this {@link DeviceAdapter},
	 *
	 * @param deviceLostEvent
	 * 		Event indicating that a device was disconnected from the hosting Gateway.
	 */
	@Subscribe
	public void onDeviceLostEvent(final DeviceLostEvent deviceLostEvent) {
		log.trace("DeviceManagerImpl.onDeviceLostEvent({})", deviceLostEvent);
		stopDeviceAdapter(deviceLostEvent.getPort());
	}

	private void stopDeviceAdapter(final String port) {

		final Set<DeviceAdapter> copy;

		synchronized (runningDeviceAdapters) {
			copy = newHashSet(runningDeviceAdapters);
		}

		for (DeviceAdapter deviceAdapter : copy) {
			if (deviceAdapter.getPort().equals(port)) {
				deviceAdapter.stopAndWait();
			}
		}
	}

	@Subscribe
	public void onDeviceAdapterFactoryAddedEvent(final DeviceAdapterFactoryAddedEvent event) {
		log.trace("DeviceManagerImpl.onDeviceAdapterFactoryAddedEvent({})", event);

		final Set<DeviceAdapter> copy;
		synchronized (runningDeviceAdapters) {
			copy = newHashSet(runningDeviceAdapters);
		}

		for (DeviceAdapter deviceAdapter : copy) {

			if (event.getDeviceAdapterFactory().canHandle(deviceAdapter.getDeviceConfig())) {

				log.info("A new DeviceAdapterFactory was added at runtime that can handle the device at "
						+ "port {}. Shutting down connection and reconnecting.", deviceAdapter.getPort()
				);

				deviceAdapter.stopAndWait();

				synchronized (detectedButNotConnectedDevices) {
					detectedButNotConnectedDevices.put(deviceAdapter.getPort(), deviceAdapter.getDeviceConfig());
				}
			}
		}

		schedulerService.execute(tryToConnectToUnconnectedDevicesRunnable);
	}

	@Subscribe
	public void onDeviceAdapterFactoryRemovedEvent(final DeviceAdapterFactoryRemovedEvent event) {
		log.trace("DeviceManagerImpl.onDeviceAdapterFactoryRemovedEvent({})", event);

		final Set<DeviceAdapter> copy;
		synchronized (runningDeviceAdapters) {
			copy = newHashSet(runningDeviceAdapters);
		}

		for (DeviceAdapter deviceAdapter : copy) {

			if (deviceAdapter.getClass().equals(event.getDeviceAdapterClass())) {

				log.trace("Stopping DeviceAdapter of type {}", deviceAdapter.getClass());

				deviceAdapter.stop();

				synchronized (detectedButNotConnectedDevices) {
					detectedButNotConnectedDevices.put(deviceAdapter.getPort(), deviceAdapter.getDeviceConfig());
				}
			}
		}
	}

	@Nullable
	@Override
	public DeviceAdapter getDeviceAdapter(final NodeUrn nodeUrn) {
		synchronized (runningDeviceAdapters) {
			for (final DeviceAdapter deviceAdapter : runningDeviceAdapters) {
				if (deviceAdapter.getNodeUrns().contains(nodeUrn)) {
					return deviceAdapter;
				}
			}
		}
		return null;
	}

	@Override
	public Iterable<DeviceAdapter> getAttachedDeviceAdapters() {
		synchronized (runningDeviceAdapters) {
			return Iterables.unmodifiableIterable(runningDeviceAdapters);
		}
	}

	@Override
	public Set<NodeUrn> getConnectedNodeUrns() {
		final Set<NodeUrn> nodeUrns = newHashSet();
		synchronized (runningDeviceAdapters) {
			for (final DeviceAdapter deviceAdapter : runningDeviceAdapters) {
				nodeUrns.addAll(deviceAdapter.getNodeUrns());
			}
		}
		return nodeUrns;
	}

	@Override
	public Multimap<DeviceAdapter, NodeUrn> getConnectedSubset(final Iterable<NodeUrn> nodeUrns) {

		final Multimap<DeviceAdapter, NodeUrn> map = HashMultimap.create();

		synchronized (runningDeviceAdapters) {

			for (final DeviceAdapter deviceAdapter : runningDeviceAdapters) {

				final Sets.SetView<NodeUrn> connected = intersection(
						deviceAdapter.getNodeUrns(),
						newHashSet(nodeUrns)
				);

				if (!connected.isEmpty()) {
					map.putAll(deviceAdapter, filter(nodeUrns, in(deviceAdapter.getNodeUrns())));
				}
			}
		}

		return map;
	}

	@Override
	public Iterable<NodeUrn> getUnconnectedSubset(final Iterable<NodeUrn> nodeUrns) {
		return filter(nodeUrns, not(in(getConnectedNodeUrns())));
	}

	/**
	 * Returns all information about the device from the DeviceDB or {@code null} if no information is found.
	 *
	 * @param deviceFoundEvent
	 * 		the device found event
	 *
	 * @return a {@code DeviceConfig} instance or {@code null} if no info is found
	 */
	@Nullable
	private DeviceConfig getDeviceConfigFromDeviceDB(final DeviceFoundEvent deviceFoundEvent) {
		if (deviceFoundEvent.getMacAddress() != null) {
			return deviceDBService.getConfigByMacAddress(deviceFoundEvent.getMacAddress().toLong());
		} else if (deviceFoundEvent.getReference() != null) {
			return deviceDBService.getConfigByUsbChipId(deviceFoundEvent.getReference());
		}
		return null;
	}

}
