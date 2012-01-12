package de.uniluebeck.itm.tr.runtime.wsndeviceobserver;

import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.application.TestbedApplication;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.wsn.deviceutils.DeviceUtilsModule;
import de.uniluebeck.itm.wsn.deviceutils.ScheduledExecutorServiceModule;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceEvent;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceInfo;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceObserver;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceObserverListener;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceType;

import java.util.concurrent.*;

public class WSNDeviceObserver implements TestbedApplication, DeviceObserverListener {

    private final TestbedRuntime testbedRuntime;

    private final String applicationName;

    private final DeviceObserver deviceObserver;

    private ScheduledExecutorService scheduler;

    private ScheduledFuture<?> deviceObserverSchedule;

    public WSNDeviceObserver(final TestbedRuntime testbedRuntime, final String applicationName,
                             final WSNDeviceObserverConfiguration configuration) {

        this.testbedRuntime = testbedRuntime;
        this.applicationName = applicationName;

        Injector injector = Guice.createInjector(
                new DeviceUtilsModule(configuration.getDeviceMacReferenceMap()),
				new AbstractModule() {
					@Override
					protected void configure() {
						bind(ExecutorService.class).toInstance(Executors.newCachedThreadPool(
								new ThreadFactoryBuilder().setNameFormat(DeviceObserver.class.getSimpleName() + " %d").build()
						));
					}
				}
        );
        this.deviceObserver = injector.getInstance(DeviceObserver.class);
    }

    @Override
    public String getName() {
        return applicationName;
    }

    @Override
    public void start() throws Exception {
        deviceObserver.addListener(this);
        scheduler = Executors.newScheduledThreadPool(
                1,
                new ThreadFactoryBuilder().setNameFormat("DeviceObserver-Thread %d").build()
        );
        deviceObserverSchedule = scheduler.scheduleAtFixedRate(deviceObserver, 0, 5, TimeUnit.SECONDS);
        testbedRuntime.getEventBus().register(this);
    }

    @Override
    public void stop() throws Exception {
        testbedRuntime.getEventBus().unregister(this);
        deviceObserver.removeListener(this);
        deviceObserverSchedule.cancel(false);
        ExecutorUtils.shutdown(scheduler, 1, TimeUnit.SECONDS);
    }

    @Override
    public void deviceEvent(final DeviceEvent event) {
        testbedRuntime.getAsyncEventBus().post(event);
    }

    @Subscribe
    public void onDeviceRequest(DeviceRequest deviceRequest) {

        ImmutableMap<String, DeviceInfo> currentState = deviceObserver.getCurrentState();

        for (DeviceInfo deviceInfo : currentState.values()) {

            boolean sameType = DeviceType.fromString(deviceInfo.getType()) == deviceRequest.getDeviceType();
            boolean sameMAC = deviceInfo.getMacAddress() != null && deviceInfo.getMacAddress().equals(deviceRequest.getMacAddress());
            boolean sameReference = deviceInfo.getReference() != null && deviceInfo.getReference().equals(deviceRequest.getReference());

            if (sameType && (sameMAC || sameReference)) {
                deviceRequest.setResponse(deviceInfo);
            }
        }
    }
}
