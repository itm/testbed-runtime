package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceEvent;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceInfo;

public class GatewayDeviceManagerImpl extends AbstractService implements GatewayDeviceManager {

	private final GatewayEventBus gatewayEventBus;

	@Inject
	public GatewayDeviceManagerImpl(final GatewayEventBus gatewayEventBus) {
		this.gatewayEventBus = gatewayEventBus;
	}

	@Override
	protected void doStart() {
		try {
			gatewayEventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			gatewayEventBus.unregister(this);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Subscribe
	public void onDeviceEvent(final DeviceEvent event) {
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
		// TODO implement
	}

	private void onDeviceDetached(final DeviceInfo deviceInfo) {
		// TODO implement
	}
}
