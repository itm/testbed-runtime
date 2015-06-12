package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
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
import de.uniluebeck.itm.tr.iwsn.messages.DeviceConfigDeletedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.DeviceConfigUpdatedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.MessageFactory;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import eu.wisebed.api.v3.common.NodeUrn;
import gnu.io.PortInUseException;
import org.apache.cxf.interceptor.Fault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.client.ClientException;
import java.io.IOException;
import java.net.ConnectException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.intersection;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

class DeviceManagerImpl extends AbstractService implements DeviceManager {

	protected static final Logger log = LoggerFactory.getLogger(DeviceManager.class);

	@Nonnull
	protected final Deque<DeviceFoundEvent> deviceFoundEvents;

	protected final Set<NodeUrn> staticDevices = newHashSet();

	protected final Set<DeviceAdapter> runningDeviceAdapters = newHashSet();

	protected final Set<DeviceAdapterFactory> builtInDeviceAdapterFactories;

	protected final GatewayEventBus gatewayEventBus;

	protected final DeviceAdapterRegistry deviceAdapterRegistry;

	protected final SchedulerService schedulerService;

	protected final DeviceDBService deviceDBService;

	protected final GatewayConfig gatewayConfig;

	protected final Stopwatch lastConnectionAttempt;

	protected final MessageFactory mf;

	protected final DeviceAdapterListener deviceAdapterListener = new ManagerDeviceAdapterListener();

	protected Runnable tryToConnectToUnconnectedDevicesRunnable = new Runnable() {
		@Override
		public void run() {

			synchronized (lastConnectionAttempt) {
				lastConnectionAttempt.reset();
			}

			try {

				if (log.isTraceEnabled()) {
					synchronized (deviceFoundEvents) {
						log.trace("Before retrying to connect: deviceFoundEvents: {}", deviceFoundEvents);
					}
				}

				generateDeviceFoundEventsForUnconnectedStaticDevices();

				final List<DeviceFoundEvent> deviceFoundEventsTried = newArrayList();

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
						deviceFoundEventsTried.add(event);
					}
				}

				synchronized (deviceFoundEvents) {
					deviceFoundEvents.addAll(deviceFoundEventsTried);
				}

				if (log.isTraceEnabled()) {
					synchronized (deviceFoundEvents) {
						log.trace("After  retrying to connect: deviceFoundEvents: {}", deviceFoundEvents);
					}
				}
			} catch (Exception e) {
				log.error("Uncaught exception occurred while trying to connect to unconnected devices: ", e);
			} finally {
				synchronized (lastConnectionAttempt) {
					lastConnectionAttempt.start();
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

					log.trace("Calling deviceAdapterFactory.create() for deviceType:{}",
							deviceFoundEvent.getDeviceType()
					);
					final DeviceAdapter deviceAdapter = deviceAdapterFactory.create(
							deviceFoundEvent.getDeviceType(),
							deviceFoundEvent.getDevicePort(),
							deviceFoundEvent.getDeviceConfiguration(),
							deviceFoundEvent.getDeviceConfig()
					);

					final DeviceAdapterServiceListener listener = new DeviceAdapterServiceListener(
							deviceAdapter,
							deviceAdapterListener
					);

					try {
						deviceAdapter.addListener(deviceAdapterListener);
						deviceAdapter.addListener(listener, directExecutor());
					} catch (Exception e) {
						log.error("Exception while adding DeviceAdapterListener to DeviceAdapter: ", e);
						return false;
					}

					try {
						deviceAdapter.startAsync().awaitRunning();
					} catch (Exception e) {
						if (e instanceof UncheckedExecutionException &&
								e.getCause() instanceof IOException &&
								e.getCause().getCause() instanceof PortInUseException) {
							log.info("Not connecting to {} because port is already in use by \"{}\"",
									deviceFoundEvent.getDevicePort(),
									((PortInUseException) e.getCause().getCause()).currentOwner
							);
							return false;
						} else {
							log.error("Could not connect to {}:\n{}", deviceFoundEvent, getStackTraceAsString(e));
							return false;
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
	protected ScheduledFuture<?> tryToConnectToDetectedButUnconnectedDevicesSchedule;

	@Inject
	public DeviceManagerImpl(final GatewayConfig gatewayConfig,
							 final GatewayEventBus gatewayEventBus,
							 final SchedulerService schedulerService,
							 final Set<DeviceAdapterFactory> builtInDeviceAdapterFactories,
							 final DeviceAdapterRegistry deviceAdapterRegistry,
							 final DeviceDBService deviceDBService,
							 final Deque<DeviceFoundEvent> deviceFoundEvents,
							 final MessageFactory mf) {
		this.gatewayConfig = gatewayConfig;
		this.gatewayEventBus = checkNotNull(gatewayEventBus);
		this.schedulerService = checkNotNull(schedulerService);
		this.builtInDeviceAdapterFactories = checkNotNull(builtInDeviceAdapterFactories);
		this.deviceAdapterRegistry = checkNotNull(deviceAdapterRegistry);
		this.deviceDBService = checkNotNull(deviceDBService);
		if (deviceFoundEvents == null) {
			this.deviceFoundEvents = newLinkedList();
		} else {
			this.deviceFoundEvents = deviceFoundEvents;
		}
		this.mf = checkNotNull(mf);
		this.lastConnectionAttempt = Stopwatch.createStarted();
	}

	@Override
	protected void doStart() {

		log.trace("DeviceManagerImpl.doStart()");

		try {

			gatewayEventBus.register(this);

			tryToConnectToDetectedButUnconnectedDevicesSchedule = schedulerService.scheduleWithFixedDelay(
					tryToConnectToUnconnectedDevicesRunnable,
					10, 30, TimeUnit.SECONDS
			);

			generateDeviceFoundEventsForUnconnectedStaticDevices();
			runTryToConnectIfLastAttemptLongerAgoThan(2, TimeUnit.SECONDS);
			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	private void runTryToConnectIfLastAttemptLongerAgoThan(int time, TimeUnit timeUnit) {
		synchronized (lastConnectionAttempt) {
			if (lastConnectionAttempt.elapsed(timeUnit) > time) {
				schedulerService.execute(tryToConnectToUnconnectedDevicesRunnable);
			}
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
				deviceAdapter.stopAsync().awaitTerminated();
			}

			gatewayEventBus.unregister(this);
			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Subscribe
	public void onDeviceConfigDeletedEvent(final DeviceConfigDeletedEvent deletedEvent) {
		log.trace("DeviceManagerImpl.onDeviceConfigDeletedEvent({})", deletedEvent);
		deletedEvent.getHeader().getNodeUrnsList().stream()
				.map(NodeUrn::new)
				.forEach(nodeUrn -> disconnectAndScheduleReconnect(nodeUrn, false));
	}

	@Subscribe
	public void onDeviceConfigUpdatedEvent(final DeviceConfigUpdatedEvent updatedEvent) {
		log.trace("DeviceManagerImpl.onDeviceConfigUpdatedEvent({})", updatedEvent);
		updatedEvent.getHeader().getNodeUrnsList().stream().map(NodeUrn::new).forEach(nodeUrn -> {
			disconnectAndScheduleReconnect(nodeUrn, true);
			updateDeviceFoundEventForStaticDevicesIfEnqueued(nodeUrn);
		});
	}

	private void updateDeviceFoundEventForStaticDevicesIfEnqueued(NodeUrn nodeUrn) {
		if (staticDevices.contains(nodeUrn)) {

			synchronized (deviceFoundEvents) {
				Iterator<DeviceFoundEvent> iterator = deviceFoundEvents.iterator();
				while (iterator.hasNext()) {
					DeviceFoundEvent e = iterator.next();
					if (e.getDeviceConfig() != null && nodeUrn.equals(e.getDeviceConfig().getNodeUrn())) {
						iterator.remove();
						tryToGenerateDeviceFoundEventForStaticNode(nodeUrn);
					}
				}
			}
		}
	}

	private void disconnectAndScheduleReconnect(final NodeUrn nodeUrn, final boolean immediateReconnect) {

		final Set<DeviceAdapter> copy;
		synchronized (runningDeviceAdapters) {
			copy = newHashSet(runningDeviceAdapters);
		}

		for (DeviceAdapter deviceAdapter : copy) {
			if (deviceAdapter.getNodeUrns().contains(nodeUrn)) {

				// if no configuration exists we can't stay connected
				deviceAdapter.stopAsync().awaitTerminated();

				// assuming that devices are still attached there should be events leading to an eventual reconnect in
				// the future, therefore fake these events
				synchronized (deviceFoundEvents) {

					if (deviceAdapter.getDeviceConfigs() != null) {

						for (DeviceConfig deviceConfig : deviceAdapter.getDeviceConfigs().values()) {

							final DeviceFoundEvent deviceFoundEvent = new DeviceFoundEvent(
									deviceConfig.getNodeType(),
									deviceAdapter.getDevicePort(),
									deviceConfig.getNodeConfiguration(),
									deviceConfig.getNodeUSBChipID(),
									new MacAddress(deviceConfig.getNodeUrn().getSuffix()),
									null
							);

							if (immediateReconnect) {
								onDeviceFoundEvent(deviceFoundEvent);
							} else {
								deviceFoundEvents.add(deviceFoundEvent);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * For every statically configured node {@link de.uniluebeck.itm.tr.iwsn.gateway.GatewayConfig#getStaticDevices()}
	 * this method checks:
	 * <p/>
	 * 1.) if the node is already connected. if not:
	 * 2.) it will try to retrieve the configuration from the DeviceDB and
	 * 3.) generate a DeviceFoundEvent to emulate the detection of a freshly attached device if there's none yet
	 * <p/>
	 * The method is safe to call repeatedly as it only does something if there is something to do (i.e. yet unconnected
	 * devices exist or devices for which no configuration was available so far do exist).
	 */
	protected void generateDeviceFoundEventsForUnconnectedStaticDevices() {

		log.trace("DeviceManagerImpl.generateDeviceFoundEventsForUnconnectedStaticDevices(): looking for static devices...");

		final String staticDevicesConfigParam = gatewayConfig.getStaticDevices();
		if (staticDevicesConfigParam != null && !"".equals(staticDevicesConfigParam)) {
			for (String urnString : Splitter.on(",").omitEmptyStrings().trimResults().split(staticDevicesConfigParam)) {
				staticDevices.add(new NodeUrn(urnString));
			}
		}

		if (log.isTraceEnabled()) {
			if (staticDevices.isEmpty()) {
				log.trace("DeviceManagerImpl.generateDeviceFoundEventsForUnconnectedStaticDevices(): no static devices configured");
			} else {
				log.trace("DeviceManagerImpl.generateDeviceFoundEventsForUnconnectedStaticDevices(): found static devices: {}", staticDevices);
			}
		}

		Iterable<NodeUrn> unconnectedStaticDevices = getUnconnectedSubset(staticDevices);

		if (log.isInfoEnabled() && !isEmpty(unconnectedStaticDevices)) {
			log.info("Found currently unconnected statically configured devices: {}", unconnectedStaticDevices);
		}

		synchronized (deviceFoundEvents) {
			for (NodeUrn unconnectedStaticDevice : unconnectedStaticDevices) {
				boolean deviceFoundEventExisting = false;
				for (DeviceFoundEvent deviceFoundEvent : deviceFoundEvents) {
					if (deviceFoundEvent.getDeviceConfig() != null &&
							deviceFoundEvent.getDeviceConfig().getNodeUrn() != null &&
							unconnectedStaticDevice.equals(deviceFoundEvent.getDeviceConfig().getNodeUrn())) {
						deviceFoundEventExisting = true;
						log.trace("DeviceManagerImpl.generateDeviceFoundEventsForUnconnectedStaticDevices(): DeviceFoundEvent for {} already existing", unconnectedStaticDevice);
						break;
					}
				}
				if (!deviceFoundEventExisting) {
					log.trace("DeviceManagerImpl.generateDeviceFoundEventsForUnconnectedStaticDevices(): no DeviceFoundEvent for {} found, creating one", unconnectedStaticDevice);
					tryToGenerateDeviceFoundEventForStaticNode(unconnectedStaticDevice);
				}
			}
		}
	}

	/**
	 * Tries to retrieve the DeviceConfig object for nodeUrn from the DeviceDB. If available it will generated a
	 * DeviceFoundEvent and add it to the queue of devices to be connected to so that the connection to the device
	 * can be created asynchronously by another thread.
	 *
	 * @param nodeUrn the nodeURN
	 */
	protected void tryToGenerateDeviceFoundEventForStaticNode(NodeUrn nodeUrn) {

		DeviceConfig config = null;
		try {
			config = deviceDBService.getConfigByNodeUrn(nodeUrn);
		} catch (Exception e) {
			if (e.getCause() instanceof Fault && e.getCause().getCause() instanceof ConnectException) {
				log.info("The DeviceDB seems not to be running, will try again later.");
			} else {
				log.info("Failed to get configuration for node URN \"{}\" from DeviceDB. Trying again later. Reason: {}", nodeUrn, e.getMessage());
			}
		}

		boolean hasType = config != null && config.getNodeType() != null && !"".equals(config.getNodeType());
		boolean hasPort = config != null && config.getNodePort() != null && !"".equals(config.getNodePort());

		if (config != null && hasType && hasPort) {

			DeviceFoundEvent e = new DeviceFoundEvent(
					config.getNodeType(),
					config.getNodePort(),
					config.getNodeConfiguration(),
					config.getNodeUSBChipID(),
					new MacAddress(config.getNodeUrn().getSuffix()),
					config
			);

			log.trace("DeviceManagerImpl.tryToGenerateDeviceFoundEventForStaticNode(): adding DeviceFoundEvent for {}", nodeUrn);

			synchronized (deviceFoundEvents) {
				deviceFoundEvents.add(e);
			}
		}
	}

	/**
	 * Consumes for events indicating that a new device was connected to this gateway.<br/> If there is no {@link
	 * DeviceAdapter} responsible for this device yet, a new instance for this device will be created and started.
	 *
	 * @param deviceFoundEvent event indicating that a new device was connected to this gateway
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

		runTryToConnectIfLastAttemptLongerAgoThan(2, TimeUnit.SECONDS);
	}

	/**
	 * Consumes events indicating that a device was disconnected from this gateway.<br/> If such an event is consumed,
	 * the gateway's node urn is removed from the {@link DeviceAdapter} which logically connects the device to the
	 * gateway. If there are no other devices connected via this {@link DeviceAdapter},
	 *
	 * @param deviceLostEvent Event indicating that a device was disconnected from the hosting Gateway.
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
				deviceAdapter.stopAsync().awaitTerminated();
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
										+ "port {}. Shutting down connection and reconnecting.",
								deviceAdapter.getDevicePort()
						);

						deviceAdapter.stopAsync().awaitTerminated();

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

		runTryToConnectIfLastAttemptLongerAgoThan(2, TimeUnit.SECONDS);
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

				deviceAdapter.stopAsync().awaitTerminated();

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
	public Multimap<DeviceAdapter, NodeUrn> getConnectedSubset(final Set<NodeUrn> nodeUrns) {

		final Multimap<DeviceAdapter, NodeUrn> map = HashMultimap.create();

		synchronized (runningDeviceAdapters) {

			for (final DeviceAdapter deviceAdapter : runningDeviceAdapters) {

				final Sets.SetView<NodeUrn> connected = intersection(
						deviceAdapter.getNodeUrns(),
						nodeUrns
				);

				if (!connected.isEmpty()) {
					map.putAll(deviceAdapter, filter(nodeUrns, in(deviceAdapter.getNodeUrns())));
				}
			}
		}

		return map;
	}

	@Override
	public Set<NodeUrn> getUnconnectedSubset(final Set<NodeUrn> nodeUrns) {
		return Sets.filter(nodeUrns, not(in(getConnectedNodeUrns())));
	}

	/**
	 * Returns all information about the device from the DeviceDB or {@code null} if no information is found.
	 *
	 * @param deviceFoundEvent the device found event
	 * @return a {@code DeviceConfig} instance or {@code null} if no info is found
	 */
	@Nullable
	private DeviceConfig getDeviceConfigFromDeviceDB(final DeviceFoundEvent deviceFoundEvent) {
		try {

			DeviceConfig config = null;

			// try with reference first (MAC addresses can be corrupted!)
			if (deviceFoundEvent.getReference() != null) {
				config = deviceDBService.getConfigByUsbChipId(deviceFoundEvent.getReference());
			}

			// if not found try to find config using MAC address
			if (config == null && deviceFoundEvent.getMacAddress() != null) {
				config = deviceDBService.getConfigByMacAddress(deviceFoundEvent.getMacAddress().toLong());
			}

			return config;

		} catch (ClientException e) {
			if (e.getCause() instanceof Fault && e.getCause().getCause() instanceof ConnectException) {
				log.trace("Not able to connect to DeviceDB while trying to fetch DeviceConfig");
			} else {
				log.warn("Exception while trying to fetch DeviceConfig from DeviceDB: ", e);
			}
			return null;
		}
	}

	protected class ManagerDeviceAdapterListener implements DeviceAdapterListener {

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
			gatewayEventBus.post(mf.upstreamMessageEvent(Optional.empty(), nodeUrn, bytes));
		}

		@Override
		public void onNotification(final DeviceAdapter deviceAdapter, final NodeUrn nodeUrn,
								   final String notification) {
			gatewayEventBus.post(mf.notificationEvent(Optional.of(newArrayList(nodeUrn)), Optional.empty(), notification));
		}
	}

	protected class DeviceAdapterServiceListener extends Listener {

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

}
