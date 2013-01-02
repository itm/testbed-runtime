package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.wsn.deviceutils.DeviceUtilsModule;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceEvent;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceInfo;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceObserver;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceObserverListener;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class GatewayDeviceObserver extends AbstractService implements DeviceObserverListener {

	private static final Logger log = LoggerFactory.getLogger(GatewayDeviceObserver.class);

	private final GatewayEventBus gatewayEventBus;

	private DeviceObserver deviceObserver;

	private ExecutorService deviceUtilsExecutor;

	private ScheduledExecutorService scheduler;

	private ScheduledFuture<?> deviceObserverSchedule;

	@Inject
	public GatewayDeviceObserver(final GatewayEventBus gatewayEventBus) {
		this.gatewayEventBus = gatewayEventBus;
	}

	@Override
	protected void doStart() {

		log.trace("GatewayDeviceObserver.doStart()");

		try {

			final ThreadFactory threadFactory = new ThreadFactoryBuilder()
					.setNameFormat(GatewayDeviceObserver.class.getSimpleName() + " %d").build();

			deviceUtilsExecutor = Executors.newCachedThreadPool(threadFactory);
			deviceObserver = Guice
					.createInjector(new DeviceUtilsModule(deviceUtilsExecutor, null))
					.getInstance(DeviceObserver.class);
			deviceObserver.addListener(this);
			scheduler = Executors.newScheduledThreadPool(1, threadFactory);
			deviceObserverSchedule = scheduler.scheduleAtFixedRate(deviceObserver, 0, 5, TimeUnit.SECONDS);

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
			ExecutorUtils.shutdown(deviceUtilsExecutor, 1, TimeUnit.SECONDS);

		} catch (Exception e) {
			notifyFailed(e);
		}

		notifyStopped();
	}

	@Override
	public void deviceEvent(final DeviceEvent event) {
		log.debug("GatewayDeviceObserver.deviceEvent({})", event);
		gatewayEventBus.post(event);
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
}
