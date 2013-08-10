package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceFoundEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceLostEvent;
import de.uniluebeck.itm.wsn.deviceutils.DeviceUtilsModule;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceEvent;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceInfo;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceObserver;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceObserverListener;
import de.uniluebeck.itm.wsn.drivers.core.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniluebeck.itm.util.concurrent.ExecutorUtils.shutdown;

public class DeviceObserverWrapper extends AbstractService implements DeviceObserverListener {

	private static final Logger log = LoggerFactory.getLogger(DeviceObserverWrapper.class);

	private final GatewayEventBus gatewayEventBus;

	private DeviceObserver deviceObserver;

	private ExecutorService deviceObserverExecutor;

	private ScheduledExecutorService scheduler;

	private ScheduledFuture<?> deviceObserverSchedule;

	private ExecutorService deviceDriverExecutorService;

	@Inject
	public DeviceObserverWrapper(final GatewayEventBus gatewayEventBus) {
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
				final DeviceFoundEvent deviceFoundEvent = new DeviceFoundEvent(
						deviceInfo.getType(),
						deviceInfo.getPort(),
						deviceInfo.getReference(),
						deviceInfo.getMacAddress(),
						null
				);
				gatewayEventBus.post(deviceFoundEvent);
				break;
			case REMOVED:
				final DeviceLostEvent deviceLostEvent = new DeviceLostEvent(
						deviceInfo.getType(),
						deviceInfo.getPort(),
						deviceInfo.getReference(),
						deviceInfo.getMacAddress(),
						null
				);
				gatewayEventBus.post(deviceLostEvent);
				break;
		}
	}
}