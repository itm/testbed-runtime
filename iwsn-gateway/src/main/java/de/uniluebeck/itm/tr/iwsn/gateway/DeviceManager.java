package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.common.NodeUrn;

import javax.annotation.Nullable;
import java.util.Set;

public interface DeviceManager extends Service {

	/**
	 * Returns the device adapter with the node URN {@code nodeUrn} or {@code null} if there is no such device currently
	 * connected.
	 *
	 * @param nodeUrn
	 * 		the node URN of the device to get
	 *
	 * @return the device or {@code null} if not connected
	 */
	@Nullable
	DeviceAdapter getDeviceAdapter(NodeUrn nodeUrn);

	/**
	 * Returns all currently connected devices.
	 *
	 * @return all currently connected devices
	 */
	Iterable<DeviceAdapter> getDeviceAdapters();

	/**
	 * Returns the node URNs of all currently connected devices.
	 *
	 * @return the node URNs of all currently connected devices
	 */
	Set<NodeUrn> getConnectedNodeUrns();

	/**
	 * Returns a mapping between node URNs and the device driver interfaces of the nodes that are currently connected.
	 *
	 * @param nodeUrns
	 * 		the node URNs for which to get the device interfaces
	 *
	 * @return a mapping between node URNs and the device driver interfaces
	 */
	Multimap<DeviceAdapter, NodeUrn> getConnectedSubset(Iterable<NodeUrn> nodeUrns);

	/**
	 * Returns the subset of nodes from {@code nodeUrns} that are currently not connected.
	 *
	 * @param nodeUrns
	 * 		the node URNs to check
	 *
	 * @return a subset of currently not connected nodes
	 */
	Iterable<NodeUrn> getUnconnectedSubset(Iterable<NodeUrn> nodeUrns);

}
