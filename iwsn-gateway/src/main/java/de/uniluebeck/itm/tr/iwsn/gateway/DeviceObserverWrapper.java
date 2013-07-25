package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.wsn.deviceutils.DeviceUtilsModule;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceEvent;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceInfo;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceObserver;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceObserverListener;
import de.uniluebeck.itm.wsn.drivers.core.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.util.concurrent.ExecutorUtils.shutdown;

public class DeviceObserverWrapper extends AbstractService implements DeviceObserverListener {

	private static final Logger log = LoggerFactory.getLogger(DeviceObserverWrapper.class);

	private final Map<String, DeviceAdapter> connectedDevices = newHashMap();

	private final Set<DeviceInfo> detectedButNotConnectedDevices = newHashSet();

	private final DeviceDBService deviceDBService;

	private final Set<DeviceAdapterFactory> deviceAdapterFactories;

	private DeviceObserver deviceObserver;

	private ExecutorService deviceObserverExecutor;

	private ScheduledExecutorService scheduler;

	private ScheduledFuture<?> deviceObserverSchedule;

	private ExecutorService deviceDriverExecutorService;

	private Runnable tryToConnectToDetectedButUnconnectedDevicesRunnable = new Runnable() {
		@Override
		public void run() {
			synchronized (detectedButNotConnectedDevices) {

				log.trace("Before retrying to connect: detectedButNotConnectedDevices: {}",
						detectedButNotConnectedDevices
				);

				for (Iterator<DeviceInfo> iterator = detectedButNotConnectedDevices.iterator(); iterator.hasNext(); ) {
					final DeviceInfo deviceInfo = iterator.next();
					if (tryToConnectToDevice(deviceInfo)) {
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
	public DeviceObserverWrapper(final DeviceDBService deviceDBService,
								 final Set<DeviceAdapterFactory> deviceAdapterFactories) {
		checkArgument(deviceAdapterFactories != null && !deviceAdapterFactories.isEmpty());
		this.deviceAdapterFactories = deviceAdapterFactories;
		this.deviceDBService = checkNotNull(deviceDBService);
	}

	@Override
	protected void doStart() {

		log.trace("DeviceObserverWrapper.doStart()");

		try {

			final ThreadFactory deviceObserverThreadFactory = new ThreadFactoryBuilder()
					.setNameFormat(DeviceObserver.class.getSimpleName() + " %d")
					.build();
			deviceObserverExecutor = Executors.newCachedThreadPool(deviceObserverThreadFactory);

			final DeviceUtilsModule module = new DeviceUtilsModule(deviceObserverExecutor, null);
			deviceObserver = Guice.createInjector(module).getInstance(DeviceObserver.class);
			deviceObserver.addListener(this);

			final ThreadFactory schedulerThreadFactory = new ThreadFactoryBuilder()
					.setNameFormat(DeviceObserverWrapper.class.getSimpleName() + " %d")
					.build();
			scheduler = Executors.newScheduledThreadPool(1, schedulerThreadFactory);
			deviceObserverSchedule = scheduler.scheduleAtFixedRate(deviceObserver, 0, 1, TimeUnit.SECONDS);

			final ThreadFactory deviceDriverExecutorThreadFactory = new ThreadFactoryBuilder()
					.setNameFormat(Device.class.getSimpleName() + " %d")
					.build();
			deviceDriverExecutorService = Executors.newCachedThreadPool(deviceDriverExecutorThreadFactory);

		} catch (Exception e) {
			notifyFailed(e);
		}

		notifyStarted();
	}

	@Override
	protected void doStop() {

		log.trace("DeviceObserverWrapper.doStop()");

		try {

			deviceObserver.removeListener(this);
			deviceObserverSchedule.cancel(false);

			shutdown(scheduler, 1, TimeUnit.SECONDS);
			shutdown(deviceObserverExecutor, 1, TimeUnit.SECONDS);
			shutdown(deviceDriverExecutorService, 10, TimeUnit.SECONDS);

		} catch (Exception e) {
			notifyFailed(e);
		}

		notifyStopped();
	}

	@Override
	public void deviceEvent(final DeviceEvent event) {
		switch (event.getType()) {
			case ATTACHED:
				if (!tryToConnectToDevice(event.getDeviceInfo())) {
					scheduleToRetryConnectionToDevice(event.getDeviceInfo());
				}
				break;
			case REMOVED:
				onDeviceDetachedFromDeviceObserver(event.getDeviceInfo());
				break;
		}
	}

	private boolean tryToConnectToDevice(final DeviceInfo deviceInfo) {

		log.trace("DeviceObserverWrapper.tryToConnectToDevice({})", deviceInfo);

		final boolean deviceAlreadyConnected;
		synchronized (connectedDevices) {
			deviceAlreadyConnected = connectedDevices.containsKey(deviceInfo.getPort());
		}

		if (deviceAlreadyConnected) {
			return true;
		}

		final DeviceConfig deviceConfig;
		try {
			deviceConfig = getDeviceConfigFromDeviceDB(deviceInfo);
		} catch (Exception e) {
			log.error("Exception while fetching device configuration from DeviceDB: ", e);
			return false;
		}

		if (deviceConfig == null) {
			log.warn("Ignoring device unknown to DeviceDB: {}", deviceInfo);
			return false;
		}

		try {

			for (DeviceAdapterFactory deviceAdapterFactory : deviceAdapterFactories) {

				if (deviceAdapterFactory.canHandle(deviceConfig)) {

					final DeviceAdapter deviceAdapter = deviceAdapterFactory.create(
							deviceInfo.getPort(),
							deviceConfig
					);

					deviceAdapter.startAndWait();

					synchronized (connectedDevices) {
						connectedDevices.put(deviceInfo.getPort(), deviceAdapter);
					}

					return true;
				}
			}

			return false;

		} catch (Exception e) {
			if (log.isErrorEnabled()) {
				log.error("{} => Could not connect to {} device at {}: {}",
						deviceConfig.getNodeUrn(),
						deviceConfig.getNodeType(),
						deviceInfo.getPort(),
						getStackTraceAsString(e)
				);
			}
			return false;
		}
	}

	private void scheduleToRetryConnectionToDevice(final DeviceInfo deviceInfo) {

		synchronized (detectedButNotConnectedDevices) {

			detectedButNotConnectedDevices.add(deviceInfo);

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

	private void onDeviceDetachedFromDeviceObserver(final DeviceInfo deviceInfo) {

		log.trace("DeviceObserverWrapper.onDeviceDetachedFromDeviceObserver({})", deviceInfo);

		synchronized (connectedDevices) {
			final DeviceAdapter deviceAdapter = connectedDevices.get(deviceInfo.getPort());

			if (deviceAdapter != null) {
				deviceAdapter.stopAndWait();
				synchronized (connectedDevices) {
					connectedDevices.remove(deviceInfo.getPort());
				}
			}
		}

		synchronized (detectedButNotConnectedDevices) {
			if (detectedButNotConnectedDevices.contains(deviceInfo)) {
				detectedButNotConnectedDevices.remove(deviceInfo);
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
			return deviceDBService.getConfigByMacAddress(deviceInfo.getMacAddress().toLong());
		} else if (deviceInfo.getReference() != null) {
			return deviceDBService.getConfigByUsbChipId(deviceInfo.getReference());
		}
		return null;
	}
}
