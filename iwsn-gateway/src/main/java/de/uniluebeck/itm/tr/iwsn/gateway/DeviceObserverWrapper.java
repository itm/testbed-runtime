package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDB;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.wsn.deviceutils.DeviceUtilsModule;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceEvent;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceInfo;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceObserver;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceObserverListener;
import de.uniluebeck.itm.wsn.drivers.core.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;

public class DeviceObserverWrapper extends AbstractService implements DeviceObserverListener {

	private static final Logger log = LoggerFactory.getLogger(DeviceObserverWrapper.class);

	private final Map<String, DeviceAdapter> connectedDevices = newHashMap();

	private final DeviceDB deviceDB;

	private final Set<DeviceAdapterFactory> deviceAdapterFactories;

	private DeviceObserver deviceObserver;

	private ExecutorService deviceObserverExecutor;

	private ScheduledExecutorService scheduler;

	private ScheduledFuture<?> deviceObserverSchedule;

	private ExecutorService deviceDriverExecutorService;

	@Inject
	public DeviceObserverWrapper(final DeviceDB deviceDB,
								 final Set<DeviceAdapterFactory> deviceAdapterFactories) {
		checkArgument(deviceAdapterFactories != null && !deviceAdapterFactories.isEmpty());
		this.deviceAdapterFactories = deviceAdapterFactories;
		this.deviceDB = checkNotNull(deviceDB);
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

		log.trace("DeviceObserverWrapper.onDeviceAttached({})", deviceInfo);

		final boolean deviceAlreadyConnected;
		synchronized (connectedDevices) {
			deviceAlreadyConnected = connectedDevices.containsKey(deviceInfo.getPort());
		}

		if (!deviceAlreadyConnected) {

			final DeviceConfig deviceConfig = getDeviceConfigFromDeviceDB(deviceInfo);

			if (deviceConfig == null) {
				log.warn("Ignoring attached event of unknown device: {}", deviceInfo);
				return;
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
					}
				}

			} catch (Exception e) {
				log.error("{} => Could not connect to {} device at {}.", e);
				throw new RuntimeException(e);
			}
		}
	}

	private void onDeviceDetached(final DeviceInfo deviceInfo) {

		log.trace("DeviceObserverWrapper.onDeviceDetached({})", deviceInfo);

		final DeviceAdapter deviceAdapter = connectedDevices.get(deviceInfo.getPort());

		if (deviceAdapter != null) {
			deviceAdapter.stopAndWait();
			synchronized (connectedDevices) {
				connectedDevices.remove(deviceInfo.getPort());
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
			return deviceDB.getConfigByMacAddress(deviceInfo.getMacAddress().toLong());
		} else if (deviceInfo.getReference() != null) {
			return deviceDB.getConfigByUsbChipId(deviceInfo.getReference());
		}
		return null;
	}
}
