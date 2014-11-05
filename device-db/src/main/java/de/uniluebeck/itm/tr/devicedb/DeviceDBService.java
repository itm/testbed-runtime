package de.uniluebeck.itm.tr.devicedb;

import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.common.NodeUrn;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * The DeviceDB is an interface to abstract from a concrete database that holds the configurations of all node in a
 * given testbed. Testbed Runtime internals only rely on this interface and the data contained in the {@link
 * DeviceConfig} instances in order to be fully operational. E.g., the self-description of the testbed (state) is
 * generated from data retrieved through {@link DeviceDBService#getAll()} and the gateway hosts
 * of TR connect to attached devices after retrieving the corresponding configuration objects by e.g., calling {@link
 * DeviceDBService#getConfigByMacAddress(long)}.
 */
public interface DeviceDBService extends Service {

	/**
	 * Retrieves the configuration objects for a set of node URNs. If a node URN is unknown or no configuration object
	 * is available the resulting map will not contain an entry for this URN.
	 *
	 * @param nodeUrns
	 * 		the set of nodes for which to retrieve configuration objects
	 *
	 * @return a map mapping from node URNs to corresponding configuration objects
	 */
	Map<NodeUrn, DeviceConfig> getConfigsByNodeUrns(Iterable<NodeUrn> nodeUrns);

	/**
	 * Retrieves a configuration object by looking it up using the chip ID of the serial-to-USB converter of the attached
	 * device.
	 *
	 * @param usbChipId
	 * 		the chip ID of the serial-to-USB converter of the node
	 *
	 * @return a configuration object or {@code null} if none can be found
	 */
	@Nullable
	DeviceConfig getConfigByUsbChipId(String usbChipId);

	/**
	 * Retrieves a configuration object by looking it up using the node URN of the node.
	 *
	 * @param nodeUrn
	 * 		the URN of the node
	 *
	 * @return a configuration object or {@code null} if none can be found
	 */
	@Nullable
	DeviceConfig getConfigByNodeUrn(NodeUrn nodeUrn);

	/**
	 * Retrieves a configuration object by looking it up using the MAC address of the node.
	 *
	 * @param macAddress
	 * 		the MAC address of the node
	 *
	 * @return a configuration object or {@code null} if none can be found
	 */
	@Nullable
	DeviceConfig getConfigByMacAddress(long macAddress);

	/**
	 * Retrieves all configuration objects for this testbed.
	 *
	 * @return all configuration objects for this testbed
	 */
	Iterable<DeviceConfig> getAll();

	/**
	 * <p>Adds a configuration object to the database.</p>
	 * 
	 * <p>Support for this operation is optional as it is only called if using a DeviceDB administration frontend with
	 * e.g., a JPA based database as storage layer. Other storage layer may be used that have different means of
	 * administration.</p>
	 *
	 * @param deviceConfig
	 * 		the configuration object to be added
	 */
	void add(DeviceConfig deviceConfig);

	/**
	 * <p>Updates a configuration object in the database. The "primary key" is the node URN. Updates result in
	 * "overwriting" the old configuration object with the given, not merging of individual configuration properties is
	 * done here.</p>
	 * 
	 * <p>Support for this operation is optional as it is only called if using a DeviceDB administration frontend with
	 * e.g., a JPA based database as storage layer. Other storage layer may be used that have different means of
	 * administration.</p>
	 *
	 * @param deviceConfig
	 * 		the configuration to be updated
	 */
	void update(DeviceConfig deviceConfig);

	/**
	 * <p>Removes a configuration object from the database.</p>
	 * 
	 * <p>Support for this operation is optional as it is only called if using a DeviceDB administration frontend with
	 * e.g., a JPA based database as storage layer. Other storage layer may be used that have different means of
	 * administration.</p>
	 *
	 * @param nodeUrn
	 * 		the node URN for which to remove the configuration
	 *
	 * @return {@code true} if the configuration could be removed, {@code false} otherwise
	 */
	boolean removeByNodeUrn(NodeUrn nodeUrn);

	/**
	 * <p>Removes all configuration objects from the database.</p>
	 * 
	 * <p>Support for this operation is optional as it is only called if using a DeviceDB administration frontend with
	 * e.g., a JPA based database as storage layer. Other storage layer may be used that have different means of
	 * administration.</p>
	 */
	void removeAll();
}
