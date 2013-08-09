package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceFoundEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceLostEvent;
import de.uniluebeck.itm.wsn.deviceutils.DeviceUtilsModule;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceEvent;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceInfo;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceObserver;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceObserverListener;
import de.uniluebeck.itm.wsn.drivers.core.Device;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.inject.internal.util.$Sets.newHashSet;
import static de.uniluebeck.itm.util.concurrent.ExecutorUtils.shutdown;

public class DeviceObserverWrapper extends AbstractService implements DeviceObserverListener {

	private static final Logger log = LoggerFactory.getLogger(DeviceObserverWrapper.class);

	private final DeviceDBService deviceDBService;

	private final GatewayEventBus gatewayEventBus;

	private final Set<DeviceInfo> detectedButUnknownDevices = newHashSet();

	private final Map<DeviceInfo, NodeUrn> deviceInfoNodeUrnMap = newHashMap();

	private DeviceObserver deviceObserver;

	private ExecutorService deviceObserverExecutor;

	private ScheduledExecutorService scheduler;

	private ScheduledFuture<?> deviceObserverSchedule;

	private ExecutorService deviceDriverExecutorService;


	private Runnable tryToConnectToDetectedButUnconnectedDevicesRunnable = new Runnable() {
		@Override
		public void run() {
			synchronized (detectedButUnknownDevices) {

				log.trace("Before retrying to connect: detectedButUnknownDevices: {}",
						detectedButUnknownDevices
				);

				for (Iterator<DeviceInfo> iterator = detectedButUnknownDevices.iterator(); iterator.hasNext(); ) {
					final DeviceInfo deviceInfo = iterator.next();
					if (fetchAndPostDeviceConfig(deviceInfo)) {
						iterator.remove();
					}
				}

				log.trace("After retrying to connect: detectedButUnknownDevices: {}",
						detectedButUnknownDevices
				);

				synchronized (tryToConnectToDetectedButUnconnectedDevicesScheduleLock) {

					if (detectedButUnknownDevices.isEmpty() &&
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
	public DeviceObserverWrapper(final DeviceDBService deviceDBService, final GatewayEventBus gatewayEventBus) {
		this.deviceDBService = checkNotNull(deviceDBService);
		this.gatewayEventBus = checkNotNull(gatewayEventBus);
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
		final DeviceInfo deviceInfo = event.getDeviceInfo();
		switch (event.getType()) {
			case ATTACHED:
				if (!fetchAndPostDeviceConfig(deviceInfo)){
					scheduleRetryFetchingAndPostingDeviceConfig(deviceInfo);
				}
				break;
			case REMOVED:
				onDeviceDetached(deviceInfo);
				break;
		}
	}

	/**
	 * Fetches a device configuration for a device from the device data base and posts a
	 * {@link DeviceFoundEvent} on the {@link GatewayEventBus} if the device was not attached previously.<br/>
	 * The device configuration will only be posted once, even if this method is called multiple times.
	 *
	 * @param deviceInfo
	 * 		Information identifying a device
	 * @return <code>true</code> if the device information were found and posted on the event bus,
	 *              <code>false</code> otherwise.
	 */
	private boolean fetchAndPostDeviceConfig(final DeviceInfo deviceInfo) {

		log.trace("DeviceObserverWrapper.fetchAndPostDeviceConfig({})", deviceInfo);

		final boolean deviceAlreadyConnected;

		synchronized (deviceInfoNodeUrnMap) {
			deviceAlreadyConnected = deviceInfoNodeUrnMap.containsKey(deviceInfo);
		}

		if (!deviceAlreadyConnected) {

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

			synchronized (deviceInfoNodeUrnMap) {
				deviceInfoNodeUrnMap.put(deviceInfo, deviceConfig.getNodeUrn());
				final String port = deviceInfo.getPort() != null ? deviceInfo.getPort() : deviceConfig.getNodePort();
				gatewayEventBus.post(new DeviceFoundEvent(port, deviceConfig));
			}
		}

		return true;
	}


	private void scheduleRetryFetchingAndPostingDeviceConfig(final DeviceInfo deviceInfo) {

		synchronized (detectedButUnknownDevices) {

			detectedButUnknownDevices.add(deviceInfo);

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


	/**
	 * Posts a {@link de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceLostEvent} on the {@link GatewayEventBus} if a device was connected previously.
	 *
	 * @param deviceInfo
	 * 		Information about the device to be detached.
	 */
	private void onDeviceDetached(final DeviceInfo deviceInfo) {

		log.trace("DeviceObserverWrapper.onDeviceDetached({})", deviceInfo);

		synchronized (deviceInfoNodeUrnMap) {
			final NodeUrn nodeUrn = deviceInfoNodeUrnMap.remove(deviceInfo);
			if (nodeUrn != null) {
				gatewayEventBus.post(new DeviceLostEvent(deviceInfo.getPort(), nodeUrn));
			} else {
				log.warn("The device ({}) to be disconnected was not connected previously.", deviceInfo);
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