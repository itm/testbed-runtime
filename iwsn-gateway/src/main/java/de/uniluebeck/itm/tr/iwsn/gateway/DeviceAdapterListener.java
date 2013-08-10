package de.uniluebeck.itm.tr.iwsn.gateway;

import eu.wisebed.api.v3.common.NodeUrn;

import java.util.Set;

public interface DeviceAdapterListener {

	void onDevicesConnected(DeviceAdapter deviceAdapter, Set<NodeUrn> nodeUrnsConnected);

	void onDevicesDisconnected(DeviceAdapter deviceAdapter, Set<NodeUrn> nodeUrnsDisconnected);

	void onBytesReceivedFromDevice(DeviceAdapter deviceAdapter, NodeUrn nodeUrn, byte[] bytes);

	void onNotification(DeviceAdapter deviceAdapter, NodeUrn nodeUrn, String notification);

}
