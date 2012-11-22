package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceDB;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceData;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceEvent;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceInfo;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GatewayDeviceManagerImpl extends AbstractService implements GatewayDeviceManager {

	private static final Logger log = LoggerFactory.getLogger(GatewayDeviceManager.class);

	private final GatewayEventBus gatewayEventBus;

	private final DeviceDB deviceDB;

	@Inject
	public GatewayDeviceManagerImpl(final GatewayEventBus gatewayEventBus, final DeviceDB deviceDB) {
		this.gatewayEventBus = gatewayEventBus;
		this.deviceDB = deviceDB;
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

		DeviceData deviceData = null;
		if (deviceInfo.getMacAddress() != null) {
			deviceData = deviceDB.getByMacAddress(deviceInfo.getMacAddress());
		} else if (deviceInfo.getReference() != null) {
			deviceData = deviceDB.getByUsbChipId(deviceInfo.getReference());
		}

		if (deviceData == null) {
			log.warn("Ignoring unknown device: {}", deviceInfo);
			return;
		}

		final NodeUrn nodeUrn = deviceData.getNodeUrn();
	}

	private void onDeviceDetached(final DeviceInfo deviceInfo) {
		// TODO implement
	}
}
