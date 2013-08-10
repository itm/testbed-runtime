package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceFoundEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceLostEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesConnectedEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesDisconnectedEvent;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;

class DeviceManagerImpl extends AbstractService implements DeviceManager {

	private static final Logger log = LoggerFactory.getLogger(DeviceManager.class);

	private final Map<String, DeviceConfig> detectedButNotConnectedDevices = newHashMap();

	private final Map<NodeUrn, DeviceAdapter> connectedDevices = newHashMap();

	private final GatewayEventBus gatewayEventBus;

	private final DeviceAdapterRegistry deviceAdapterRegistry;

	private ScheduledExecutorService scheduler;

	private Runnable tryToConnectToDetectedButUnconnectedDevicesRunnable = new Runnable() {
		@Override
		public void run() {
			synchronized (detectedButNotConnectedDevices) {

				log.trace("Before retrying to connect: detectedButNotConnectedDevices: {}",
						detectedButNotConnectedDevices
				);

				for (Iterator<Map.Entry<String, DeviceConfig>> iterator =
							 detectedButNotConnectedDevices.entrySet().iterator(); iterator.hasNext(); ) {

					final Map.Entry<String, DeviceConfig> entry = iterator.next();
					final String port = entry.getKey();
					final DeviceConfig deviceConfig = entry.getValue();

					if (tryToConnectToDevice(port, deviceConfig)) {
						iterator.remove();
					}
				}

				log.trace("After retrying to connect: detectedButNotConnectedDevices: {}",
						detectedButNotConnectedDevices
				);

				synchronized (tryToConnectToDetectedButUnconnectedDevicesScheduleLock) {

					if (detectedButNotConnectedDevices.isEmpty() &&
							tryToConnectToDetectedButUnconnectedDevicesSchedule != null) {
						tryToConnectToDetectedButUnconnectedDevicesSchedule.cancel(false);
					}
				}
			}
		}
	};

	private final Object tryToConnectToDetectedButUnconnectedDevicesScheduleLock = new Object();

	@Nullable
	private ScheduledFuture<?> tryToConnectToDetectedButUnconnectedDevicesSchedule;

	@Inject
	public DeviceManagerImpl(final GatewayEventBus gatewayEventBus,
							 final DeviceAdapterRegistry deviceAdapterRegistry) {
		this.deviceAdapterRegistry = checkNotNull(deviceAdapterRegistry);
		this.gatewayEventBus = checkNotNull(gatewayEventBus);
	}

	@Override
	protected void doStart() {

		log.trace("DeviceManagerImpl.doStart()");

		try {

			final ThreadFactory schedulerThreadFactory = new ThreadFactoryBuilder()
					.setNameFormat(DeviceObserverWrapper.class.getSimpleName() + " %d")
					.build();
			scheduler = Executors.newScheduledThreadPool(1, schedulerThreadFactory);

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

			final Collection<DeviceAdapter> adapters;
			synchronized (connectedDevices) {
				adapters = connectedDevices.values();
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

		if (!tryToConnectToDevice(deviceFoundEvent.getPort(), deviceFoundEvent.getDeviceConfig())) {
			scheduleToRetryConnectionToDevice(deviceFoundEvent.getPort(), deviceFoundEvent.getDeviceConfig());
		}
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

		final NodeUrn nodeUrn = deviceLostEvent.getNodeUrn();

		synchronized (connectedDevices) {
			final DeviceAdapter deviceAdapter = connectedDevices.get(nodeUrn);
			if (deviceAdapter != null) {
				if (deviceAdapter.getNodeUrns().equals(newHashSet(nodeUrn))) {
					deviceAdapter.stopAndWait();
				}
			}
		}
	}

	/**
	 * Listen for events indicating that a new device was logically attached.<br/> In this case, logically attached
	 * means that a corresponding {@link DeviceAdapter} was created and started.
	 *
	 * @param event
	 * 		Event indicating that a new device was logically attached.
	 */
	@Subscribe
	public void onDevicesConnectedEvent(final DevicesConnectedEvent event) {

		log.trace("DeviceManagerImpl.onDevicesConnectedEvent({})", event);

		final DeviceAdapter deviceAdapter = event.getDeviceAdapter();
		deviceAdapter.addListener(new Listener() {
			@Override
			public void starting() {
				// nothing to do
			}

			@Override
			public void running() {
				// nothing to do
			}

			@Override
			public void stopping(final State from) {
				log.trace("DeviceManagerImpl.DeviceAdapterListener.stopping()");
				synchronized (connectedDevices) {
					for (Iterator<Map.Entry<NodeUrn, DeviceAdapter>> iterator = connectedDevices.entrySet().iterator();
						 iterator.hasNext(); ) {

						final Map.Entry<NodeUrn, DeviceAdapter> entry = iterator.next();
						if (entry.getValue() == deviceAdapter) {
							iterator.remove();
						}
					}
				}
			}

			@Override
			public void terminated(final State from) {
				// nothing to do
			}

			@Override
			public void failed(final State from, final Throwable failure) {
				// nothing to do
			}
		}, sameThreadExecutor()
		);

		synchronized (connectedDevices) {
			for (NodeUrn nodeUrn : event.getNodeUrns()) {
				connectedDevices.put(nodeUrn, deviceAdapter);
			}
		}
	}


	/**
	 * Consumes events indicating that a {@link DeviceAdapter} was stopped since it does not connect any device to this
	 * gateway (any more).
	 *
	 * @param event
	 * 		Event indicating that a {@link DeviceAdapter} was stopped.
	 */
	@Subscribe
	public void onDevicesDisconnectedEvent(final DevicesDisconnectedEvent event) {
		log.trace("DeviceManagerImpl.onDevicesDisconnectedEvent({})", event);
		synchronized (connectedDevices) {
			for (NodeUrn nodeUrn : event.getNodeUrns()) {
				connectedDevices.remove(nodeUrn);
			}
		}
	}

	@Subscribe
	public void onDeviceAdapterFactoryRemovedEvent(final DeviceAdapterFactoryRemovedEvent event) {
		log.trace("DeviceManagerImpl.onDeviceAdapterFactoryRemovedEvent({})", event);
		synchronized (connectedDevices) {
			for (DeviceAdapter deviceAdapter : connectedDevices.values()) {
				if (deviceAdapter.getClass().equals(event.getDeviceAdapterClass())) {
					log.trace("Stopping DeviceAdapter of type {}", deviceAdapter.getClass());
					deviceAdapter.stop();
				}
			}
		}
	}


	@Nullable
	@Override
	public DeviceAdapter getDeviceAdapter(final NodeUrn nodeUrn) {
		synchronized (connectedDevices) {
			return connectedDevices.get(nodeUrn);
		}
	}

	@Override
	public Iterable<DeviceAdapter> getAttachedDeviceAdapters() {
		synchronized (connectedDevices) {
			return Iterables.unmodifiableIterable(connectedDevices.values());
		}
	}

	@Override
	public Set<NodeUrn> getConnectedNodeUrns() {
		synchronized (connectedDevices) {
			return ImmutableSet.copyOf(connectedDevices.keySet());
		}

	}

	@Override
	public Multimap<DeviceAdapter, NodeUrn> getConnectedSubset(final Iterable<NodeUrn> nodeUrns) {

		final Multimap<DeviceAdapter, NodeUrn> map = HashMultimap.create();

		synchronized (connectedDevices) {

			for (NodeUrn nodeUrn : nodeUrns) {
				map.put(connectedDevices.get(nodeUrn), nodeUrn);
			}

		}

		return map;
	}

	@Override
	public Iterable<NodeUrn> getUnconnectedSubset(final Iterable<NodeUrn> nodeUrns) {
		return filter(nodeUrns, not(in(getConnectedNodeUrns())));
	}

	private boolean tryToConnectToDevice(final String port, final DeviceConfig deviceConfig) {


		log.trace("DeviceManagerImpl.tryToConnectToDevice({})", deviceConfig);

		final boolean deviceAlreadyConnected;
		synchronized (connectedDevices) {
			deviceAlreadyConnected = connectedDevices.containsKey(deviceConfig.getNodeUrn());
		}

		if (!deviceAlreadyConnected) {

			try {
				for (DeviceAdapterFactory deviceAdapterFactory : deviceAdapterRegistry.getDeviceAdapterFactories()) {

					if (deviceAdapterFactory.canHandle(deviceConfig)) {

						final DeviceAdapter deviceAdapter = deviceAdapterFactory.create(port, deviceConfig);
						deviceAdapter.startAndWait();

						// Note that the device will not be marked as connected until the device adapter throws
						// a DevicesConnectedEvent indicating a successful connection.
						// Another method listens for this event and marks the device as connected when the
						// corresponding event is consumed.

						return true;
					}
				}
				return false;

			} catch (Exception e) {
				if (log.isErrorEnabled()) {
					log.error("{} => Could not connect to {} device at {}: {}",
							deviceConfig.getNodeUrn(),
							deviceConfig.getNodeType(),
							deviceConfig.getNodePort(),
							getStackTraceAsString(e)
					);
				}
				return false;
			}
		}

		return true;
	}

	private void scheduleToRetryConnectionToDevice(final String port, final DeviceConfig deviceConfig) {

		synchronized (detectedButNotConnectedDevices) {

			detectedButNotConnectedDevices.put(port, deviceConfig);

			synchronized (tryToConnectToDetectedButUnconnectedDevicesScheduleLock) {
				if (tryToConnectToDetectedButUnconnectedDevicesSchedule == null) {
					tryToConnectToDetectedButUnconnectedDevicesSchedule = scheduler
							.scheduleAtFixedRate(tryToConnectToDetectedButUnconnectedDevicesRunnable, 30, 30,
									TimeUnit.SECONDS
							);
				}
			}
		}
	}

}
