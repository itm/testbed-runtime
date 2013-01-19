package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfigDB;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.wsn.deviceutils.DeviceUtilsModule;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceEvent;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceInfo;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceObserver;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceObserverListener;
import de.uniluebeck.itm.wsn.drivers.core.Device;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;

public class DeviceObserverWrapper extends AbstractService implements DeviceObserverListener {

	private static final Logger log = LoggerFactory.getLogger(DeviceObserverWrapper.class);

	private final Map<String, DeviceAdapter> currentlyConnectedDevices = newHashMap();

	private final DeviceConfigDB deviceConfigDB;

	private final Set<DeviceAdapterFactory> gatewayDeviceAdapterFactories;

	private DeviceObserver deviceObserver;

	private ExecutorService deviceObserverExecutor;

	private ScheduledExecutorService scheduler;

	private ScheduledFuture<?> deviceObserverSchedule;

	private ExecutorService deviceDriverExecutorService;

	@Inject
	public DeviceObserverWrapper(final DeviceConfigDB deviceConfigDB,
								 final Set<DeviceAdapterFactory> gatewayDeviceAdapterFactories) {
		this.gatewayDeviceAdapterFactories = gatewayDeviceAdapterFactories;
		this.deviceConfigDB = checkNotNull(deviceConfigDB);
	}

	@Override
	protected void doStart() {

		log.trace("DeviceObserverWrapper.doStart()");

		try {

			deviceObserverExecutor = Executors.newCachedThreadPool(
					new ThreadFactoryBuilder().setNameFormat(DeviceObserver.class.getSimpleName() + " %d").build()
			);

			deviceObserver = Guice
					.createInjector(new DeviceUtilsModule(deviceObserverExecutor, null))
					.getInstance(DeviceObserver.class);
			deviceObserver.addListener(this);
			scheduler = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder()
					.setNameFormat(DeviceObserverWrapper.class.getSimpleName() + " %d")
					.build()
			);
			deviceObserverSchedule = scheduler.scheduleAtFixedRate(deviceObserver, 0, 1, TimeUnit.SECONDS);

			deviceDriverExecutorService = Executors.newCachedThreadPool(
					new ThreadFactoryBuilder().setNameFormat(Device.class.getSimpleName() + " %d").build()
			);

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
			ExecutorUtils.shutdown(scheduler, 1, TimeUnit.SECONDS);
			ExecutorUtils.shutdown(deviceObserverExecutor, 1, TimeUnit.SECONDS);
			ExecutorUtils.shutdown(deviceDriverExecutorService, 10, TimeUnit.SECONDS);

		} catch (Exception e) {
			notifyFailed(e);
		}

		notifyStopped();
	}

	@Override
	public void deviceEvent(final DeviceEvent event) {
		log.debug("DeviceObserverWrapper.deviceEvent({})", event);
		switch (event.getType()) {
			case ATTACHED:
				onDeviceAttached(event.getDeviceInfo());
				break;
			case REMOVED:
				onDeviceDetached(event.getDeviceInfo());
				break;
		}
	}

	@Subscribe
	public void onDeviceRequest(final DeviceObserverWrapperRequest request) {

		log.debug("DeviceObserverWrapper.onDeviceRequest({})", request);

		ImmutableMap<String, DeviceInfo> currentState = deviceObserver.getCurrentState();

		for (DeviceInfo deviceInfo : currentState.values()) {

			boolean sameType = DeviceType.fromString(deviceInfo.getType()) == request.getDeviceType();
			boolean sameMAC = deviceInfo.getMacAddress() != null &&
					deviceInfo.getMacAddress().equals(request.getMacAddress());
			boolean sameReference = deviceInfo.getReference() != null &&
					deviceInfo.getReference().equals(request.getReference());

			if (sameType && (sameMAC || sameReference)) {
				log.debug("DeviceObserverWrapper.onDeviceRequest({}) -> {}", request, deviceInfo);
				request.setResponse(deviceInfo);
			}
		}
	}

	private void onDeviceAttached(final DeviceInfo deviceInfo) {

		log.trace("DeviceManagerImpl.onDeviceAttached({})", deviceInfo);

		final boolean deviceAlreadyConnected;
		synchronized (currentlyConnectedDevices) {
			deviceAlreadyConnected = currentlyConnectedDevices.containsKey(deviceInfo.getPort());
		}

		if (!deviceAlreadyConnected) {

			final DeviceConfig deviceConfig = getDeviceConfigFromDeviceDB(deviceInfo);

			if (deviceConfig == null) {
				log.warn("Ignoring attached event of unknown device: {}", deviceInfo);
				return;
			}

			try {

				for (DeviceAdapterFactory deviceAdapterFactory : gatewayDeviceAdapterFactories) {

					if (deviceAdapterFactory.canHandle(deviceConfig)) {

						final DeviceAdapter deviceAdapter = deviceAdapterFactory.create(
								deviceInfo.getPort(),
								deviceConfig
						);

						deviceAdapter.startAndWait();

						synchronized (currentlyConnectedDevices) {
							currentlyConnectedDevices.put(deviceInfo.getPort(), deviceAdapter);
						}
					}
				}

			} catch (Exception e) {
				log.error("{} => Could not connect to {} device at {}.", e);
				throw new RuntimeException(e);
			}
		}
	}

	private void onDeviceDetached(final DeviceInfo deviceInfo) {

		log.trace("DeviceManagerImpl.onDeviceDetached({})", deviceInfo);

		final DeviceAdapter deviceAdapter = currentlyConnectedDevices.get(deviceInfo.getPort());

		if (deviceAdapter != null) {
			deviceAdapter.stopAndWait();
			synchronized (currentlyConnectedDevices) {
				currentlyConnectedDevices.remove(deviceInfo.getPort());
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
}
