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
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;

public class GatewayDeviceObserver extends AbstractService implements DeviceObserverListener {

	private static final Logger log = LoggerFactory.getLogger(GatewayDeviceObserver.class);

	private final Map<String, GatewayDeviceAdapter> currentlyConnectedDevices = newHashMap();

	private final GatewayEventBus gatewayEventBus;

	private final DeviceFactory deviceFactory;

	private final DeviceConfigDB deviceConfigDB;

	private final GatewayDeviceAdapterFactory gatewayDeviceAdapterFactory;

	private DeviceObserver deviceObserver;

	private ExecutorService deviceObserverExecutor;

	private ScheduledExecutorService scheduler;

	private ScheduledFuture<?> deviceObserverSchedule;

	private ExecutorService deviceDriverExecutorService;

	@Inject
	public GatewayDeviceObserver(final GatewayEventBus gatewayEventBus,
								 final DeviceFactory deviceFactory,
								 final DeviceConfigDB deviceConfigDB,
								 final GatewayDeviceAdapterFactory gatewayDeviceAdapterFactory) {
		this.gatewayDeviceAdapterFactory = gatewayDeviceAdapterFactory;
		this.gatewayEventBus = checkNotNull(gatewayEventBus);
		this.deviceFactory = checkNotNull(deviceFactory);
		this.deviceConfigDB = checkNotNull(deviceConfigDB);
	}

	@Override
	protected void doStart() {

		log.trace("GatewayDeviceObserver.doStart()");

		try {

			deviceObserverExecutor = Executors.newCachedThreadPool(
					new ThreadFactoryBuilder().setNameFormat(DeviceObserver.class.getSimpleName() + " %d").build()
			);

			deviceObserver = Guice
					.createInjector(new DeviceUtilsModule(deviceObserverExecutor, null))
					.getInstance(DeviceObserver.class);
			deviceObserver.addListener(this);
			scheduler = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder()
					.setNameFormat(GatewayDeviceObserver.class.getSimpleName() + " %d")
					.build()
			);
			deviceObserverSchedule = scheduler.scheduleAtFixedRate(deviceObserver, 0, 5, TimeUnit.SECONDS);

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

		log.trace("GatewayDeviceObserver.doStop()");

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
		log.debug("GatewayDeviceObserver.deviceEvent({})", event);
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
	public void onDeviceRequest(final GatewayDeviceObserverRequest request) {

		log.debug("GatewayDeviceObserver.onDeviceRequest({})", request);

		ImmutableMap<String, DeviceInfo> currentState = deviceObserver.getCurrentState();

		for (DeviceInfo deviceInfo : currentState.values()) {

			boolean sameType = DeviceType.fromString(deviceInfo.getType()) == request.getDeviceType();
			boolean sameMAC = deviceInfo.getMacAddress() != null &&
					deviceInfo.getMacAddress().equals(request.getMacAddress());
			boolean sameReference = deviceInfo.getReference() != null &&
					deviceInfo.getReference().equals(request.getReference());

			if (sameType && (sameMAC || sameReference)) {
				log.debug("GatewayDeviceObserver.onDeviceRequest({}) -> {}", request, deviceInfo);
				request.setResponse(deviceInfo);
			}
		}
	}

	private void onDeviceAttached(final DeviceInfo deviceInfo) {

		log.trace("GatewayDeviceManagerImpl.onDeviceAttached({})", deviceInfo);

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

				final Device device = deviceFactory.create(
						deviceDriverExecutorService,
						deviceConfig.getNodeType(),
						deviceConfig.getNodeConfiguration()
				);

				device.connect(deviceInfo.getPort());

				log.info("{} => Successfully connected to {} device on serial port {}",
						deviceConfig.getNodeUrn(), deviceConfig.getNodeType(), deviceInfo.getPort()
				);

				final GatewayDeviceAdapter gatewayDeviceAdapter = gatewayDeviceAdapterFactory.create(
						deviceConfig,
						device
				);

				gatewayDeviceAdapter.startAndWait();

				synchronized (currentlyConnectedDevices) {
					currentlyConnectedDevices.put(deviceInfo.getPort(), gatewayDeviceAdapter);
				}

			} catch (Exception e) {
				log.error("{} => Could not connect to {} device at {}.", e);
				throw new RuntimeException(e);
			}
		}
	}

	private void onDeviceDetached(final DeviceInfo deviceInfo) {

		log.trace("GatewayDeviceManagerImpl.onDeviceDetached({})", deviceInfo);

		final GatewayDeviceAdapter gatewayDeviceAdapter = currentlyConnectedDevices.get(deviceInfo.getPort());

		if (gatewayDeviceAdapter != null) {
			gatewayDeviceAdapter.stopAndWait();
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
