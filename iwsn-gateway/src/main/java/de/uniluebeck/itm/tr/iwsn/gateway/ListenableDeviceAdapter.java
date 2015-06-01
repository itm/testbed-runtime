package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.util.concurrent.AbstractService;
import de.uniluebeck.itm.util.ListenerManager;
import de.uniluebeck.itm.util.ListenerManagerImpl;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public abstract class ListenableDeviceAdapter extends AbstractService implements DeviceAdapter {

	private static final Logger log = LoggerFactory.getLogger(SingleDeviceAdapterBase.class);

	protected final ListenerManager<DeviceAdapterListener> listenerManager =
			new ListenerManagerImpl<>();

	@Override
	public void addListener(final DeviceAdapterListener listener) {
		if (!listenerManager.getListeners().contains(listener)) {
			listenerManager.addListener(listener);
		}
	}

	@Override
	public void removeListener(final DeviceAdapterListener listener) {
		listenerManager.removeListener(listener);
	}

	protected void fireDevicesConnected(final NodeUrn nodeUrn) {
		fireDevicesConnected(newHashSet(nodeUrn));
	}

	protected void fireDevicesConnected(final Set<NodeUrn> nodeUrns) {
		log.trace("SingleDeviceAdapterBase.fireDevicesConnected({})", nodeUrns);
		for (DeviceAdapterListener listener : listenerManager.getListeners()) {
			try {
				listener.onDevicesConnected(this, nodeUrns);
			} catch (Exception e) {
				log.error("Exception while calling onDevicesConnected() on listener: ", e);
			}
		}
	}

	protected void fireDevicesDisconnected(final NodeUrn nodeUrn) {
		fireDevicesDisconnected(newHashSet(nodeUrn));
	}

	protected void fireDevicesDisconnected(final Set<NodeUrn> nodeUrns) {
		log.trace("SingleDeviceAdapterBase.fireDevicesDisconnected({})", nodeUrns);
		for (DeviceAdapterListener listener : listenerManager.getListeners()) {
			try {
				listener.onDevicesDisconnected(this, nodeUrns);
			} catch (Exception e) {
				log.error("Exception while calling onDevicesDisconnected() on listener: ", e);
			}
		}
	}

	protected void fireBytesReceivedFromDevice(final NodeUrn nodeUrn, final byte[] bytes) {
		log.trace("SingleDeviceAdapterBase.fireBytesReceivedFromDevice({}, {})", nodeUrn, bytes);
		for (DeviceAdapterListener listener : listenerManager.getListeners()) {
			try {
				listener.onBytesReceivedFromDevice(this, nodeUrn, bytes);
			} catch (Exception e) {
				log.error("Exception while calling onBytesReceivedFromDevice() on listener: ", e);
			}
		}
	}

	protected void fireNotification(final NodeUrn nodeUrn, final String notificationMessage) {
		log.trace("SingleDeviceAdapterBase.fireNotification({}, {})", nodeUrn, notificationMessage);
		for (DeviceAdapterListener listener : listenerManager.getListeners()) {
			try {
				listener.onNotification(this, nodeUrn, notificationMessage);
			} catch (Exception e) {
				log.error("Exception while calling onNotification() on listener: ", e);
			}
		}
	}
}
