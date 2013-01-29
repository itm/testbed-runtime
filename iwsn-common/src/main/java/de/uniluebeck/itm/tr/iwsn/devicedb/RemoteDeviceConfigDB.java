package de.uniluebeck.itm.tr.iwsn.devicedb;

import com.google.common.util.concurrent.AbstractService;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.Map;

public class RemoteDeviceConfigDB extends AbstractService implements DeviceConfigDB {


	@Override
	protected void doStart() {
		try {
			// TODO implement
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			// TODO implement
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public Map<NodeUrn, DeviceConfig> getByNodeUrns(final Iterable<NodeUrn> nodeUrns) {
		return null;  // TODO implement
	}

	@Override
	public DeviceConfig getByUsbChipId(final String usbChipId) {
		return null;  // TODO implement
	}

	@Override
	public DeviceConfig getByNodeUrn(final NodeUrn nodeUrn) {
		return null;  // TODO implement
	}

	@Override
	public DeviceConfig getByMacAddress(final long macAddress) {
		return null;  // TODO implement
	}

	@Override
	public Iterable<DeviceConfig> getAll() {
		return null;  // TODO implement
	}
}
