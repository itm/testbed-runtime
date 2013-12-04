package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceFoundEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceLostEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesConnectedEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesDisconnectedEvent;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import eu.wisebed.api.v3.common.NodeUrn;
import gnu.io.PortInUseException;
import org.apache.cxf.interceptor.Fault;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.client.ClientException;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.intersection;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newNotificationEvent;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newUpstreamMessageEvent;

class DeviceManagerImpl extends AbstractService implements DeviceManager {

	private static final Logger log = LoggerFactory.getLogger(DeviceManager.class);

	private final Deque<DeviceFoundEvent> deviceFoundEvents = newLinkedList();

	private final Set<DeviceAdapter> runningDeviceAdapters = newHashSet();

	private final Set<DeviceAdapterFactory> builtInDeviceAdapterFactories;

	private final GatewayEventBus gatewayEventBus;

	private final DeviceAdapterRegistry deviceAdapterRegistry;

	private final SchedulerService schedulerService;

	private final DeviceDBService deviceDBService;

	private Runnable tryToConnectToUnconnectedDevicesRunnable = new Runnable() {
		@Override
		public void run() {

			if (log.isTraceEnabled()) {
				synchronized (deviceFoundEvents) {
					log.trace("Before retrying to connect: deviceFoundEvents: {}", deviceFoundEvents);
				}
			}

			// try to retrieve missing configs
			while (!deviceFoundEvents.isEmpty()) {

				DeviceFoundEvent event;
				synchronized (deviceFoundEvents) {
					event = deviceFoundEvents.removeFirst();
				}

				if (event.getDeviceConfig() == null) {
					event.setDeviceConfig(getDeviceConfigFromDeviceDB(event));
				}

				if (tryToConnect(event)) {
					log.trace("Successfully connected to {}", event);
				} else {
					log.trace("Failed to connect, putting back in queue: {},", event);
					synchronized (deviceFoundEvents) {
						deviceFoundEvents.addLast(event);
					}
				}
			}

			if (log.isTraceEnabled()) {
				synchronized (deviceFoundEvents) {
					log.trace("After  retrying to connect: deviceFoundEvents: {}", deviceFoundEvents);
				}
			}
		}

		private boolean tryToConnect(final DeviceFoundEvent deviceFoundEvent) {

			log.trace("DeviceManagerImpl.tryToConnect({})", deviceFoundEvent);

			// if already connected return true
			synchronized (runningDeviceAdapters) {
				for (final DeviceAdapter deviceAdapter : runningDeviceAdapters) {
					if (deviceFoundEvent.getDeviceConfig() != null &&
							deviceAdapter.getNodeUrns().contains(deviceFoundEvent.getDeviceConfig().getNodeUrn())) {
						return true;
					}
				}
			}

			// first try to find a suitable DeviceAdapterFactory from plugins using the registry
			// (allows overriding built-in drivers)
			boolean connected = tryToConnect(deviceFoundEvent, deviceAdapterRegistry.getDeviceAdapterFactories());

			// if failed, try with the built-in DeviceAdapterFactory instances
			if (!connected) {
				connected = tryToConnect(deviceFoundEvent, builtInDeviceAdapterFactories);
			}

			log.debug("{} to device {}", (connected ? "Connected" : "Failed connecting"), deviceFoundEvent);

			return connected;
		}

		private boolean tryToConnect(final DeviceFoundEvent deviceFoundEvent,
									 final Set<DeviceAdapterFactory> deviceAdapterFactories) {

			for (DeviceAdapterFactory deviceAdapterFactory : deviceAdapterFactories) {

				final boolean canHandle = deviceAdapterFactory.canHandle(
						deviceFoundEvent.getDeviceType(),
						deviceFoundEvent.getDevicePort(),
						deviceFoundEvent.getDeviceConfiguration(),
						deviceFoundEvent.getDeviceConfig()
				);

				if (canHandle) {

					final DeviceAdapter deviceAdapter = deviceAdapterFactory.create(
							deviceFoundEvent.getDeviceType(),
							deviceFoundEvent.getDevicePort(),
							deviceFoundEvent.getDeviceConfiguration(),
							deviceFoundEvent.getDeviceConfig()
					);

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
						if (e instanceof UncheckedExecutionException &&
								e.getCause() instanceof IOException &&
								e.getCause().getCause() instanceof PortInUseException) {
							log.info("Not connecting to {} because port is already in use by \"{}\"",
									deviceFoundEvent.getDevicePort(),
									((PortInUseException) e.getCause().getCause()).currentOwner
							);
						} else {
							log.error("Could not connect to {}: {}", deviceFoundEvent, getStackTraceAsString(e));
						}
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

		synchronized (deviceFoundEvents) {
			deviceFoundEvents.add(deviceFoundEvent);
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
		stopDeviceAdapter(deviceLostEvent);
	}

	private void stopDeviceAdapter(final DeviceLostEvent deviceLostEvent) {

		final Set<DeviceAdapter> copy;

		synchronized (runningDeviceAdapters) {
			copy = newHashSet(runningDeviceAdapters);
		}

		for (DeviceAdapter deviceAdapter : copy) {
			if (deviceAdapter.getDevicePort().equals(deviceLostEvent.getDevicePort())) {
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

			final Map<NodeUrn, DeviceConfig> deviceConfigs = deviceAdapter.getDeviceConfigs();

			if (deviceConfigs != null) {

				for (@Nonnull DeviceConfig deviceConfig : deviceConfigs.values()) {

					final boolean canHandle = event.getDeviceAdapterFactory().canHandle(
							deviceAdapter.getDeviceType(),
							deviceAdapter.getDevicePort(),
							deviceAdapter.getDeviceConfiguration(),
							deviceConfig
					);

					if (canHandle) {

						log.info("A new DeviceAdapterFactory was added at runtime that can handle the device at "
								+ "port {}. Shutting down connection and reconnecting.", deviceAdapter.getDevicePort()
						);

						deviceAdapter.stopAndWait();

						synchronized (deviceFoundEvents) {

							@SuppressWarnings("ConstantConditions")
							final String reference = deviceConfig == null ? null : deviceConfig.getNodeUSBChipID();

							deviceFoundEvents.add(new DeviceFoundEvent(
									deviceAdapter.getDeviceType(),
									deviceAdapter.getDevicePort(),
									deviceConfig.getNodeConfiguration(),
									reference,
									null,
									deviceConfig
							)
							);
						}
					}
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

				final Map<NodeUrn, DeviceConfig> deviceConfigs = deviceAdapter.getDeviceConfigs();

				deviceAdapter.stop();

				if (deviceConfigs != null) {

					for (NodeUrn nodeUrn : deviceConfigs.keySet()) {

						@Nonnull
						final DeviceConfig deviceConfig = deviceConfigs.get(nodeUrn);

						@SuppressWarnings("ConstantConditions")
						final String reference = (deviceConfig == null) ? null : deviceConfig.getNodeUSBChipID();

						deviceFoundEvents.add(new DeviceFoundEvent(
								deviceAdapter.getDeviceType(),
								deviceAdapter.getDevicePort(),
								deviceConfig.getNodeConfiguration(),
								reference,
								null,
								deviceConfig
						)
						);
					}
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
		try {
			if (deviceFoundEvent.getMacAddress() != null) {
				return deviceDBService.getConfigByMacAddress(deviceFoundEvent.getMacAddress().toLong());
			} else if (deviceFoundEvent.getReference() != null) {
				return deviceDBService.getConfigByUsbChipId(deviceFoundEvent.getReference());
			}
		} catch (ClientException e) {
			if (e.getCause() instanceof Fault && e.getCause().getCause() instanceof ConnectException) {
				log.trace("Not able to connect to DeviceDB while trying to fetch DeviceConfig");
			} else {
				log.warn("Exception while trying to fetch DeviceConfig from DeviceDB: ", e);
			}
			return null;
		}
		return null;
	}

}
