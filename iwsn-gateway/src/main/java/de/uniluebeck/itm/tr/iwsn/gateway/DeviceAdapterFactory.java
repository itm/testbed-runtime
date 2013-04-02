package de.uniluebeck.itm.tr.iwsn.gateway;

import de.uniluebeck.itm.tr.devicedb.DeviceConfig;

/**
 * A factory to create instances of {@link DeviceAdapter}. For every implementation of DeviceAdapter there must be one
 * DeviceAdapterFactory implementation that is registered with the Testbed Runtime gateway implementation. If a device
 * gets attached to the gateway host (either by getting serially attached or e.g., by coming into communication range)
 * the gateway implementation will retrieve the corresponding {@link DeviceConfig} instance from the {@link
 * de.uniluebeck.itm.tr.devicedb.DeviceDB} and ask each registered DeviceAdapterFactory if it can handle this type of
 * node (configuration). If true, a new DeviceAdapter instance will then be retrieved or created by calling {@link
 * DeviceAdapterFactory#create(String, de.uniluebeck.itm.tr.devicedb.DeviceConfig)}.
 */
public interface DeviceAdapterFactory {

	/**
	 * Returns true if the {@link DeviceAdapter} instances created by this factory match with the node (configuration)
	 * given in the {@code deviceConfig} parameter.
	 *
	 * @param deviceConfig
	 * 		the configuration object for the device
	 *
	 * @return {@code true} if this type of device (config) can be handled, {@code false} otherwise
	 */
	boolean canHandle(final DeviceConfig deviceConfig);

	/**
	 * Returns a newly created/the {@link DeviceAdapter} instance responsible for the given {@code port} and {@code
	 * deviceConfig}.
	 *
	 * @param port
	 * 		the port on which the device is attached. For "wirelessly attached" devices this is typically the port of
	 * 		the gateway node over which the communication with the wireless node is handled
	 * @param deviceConfig
	 * 		the configuration object for the device
	 *
	 * @return a newly created/the {@link DeviceAdapter} instance responsible for the given {@code port} and {@code
	 *         deviceConfig}
	 */
	DeviceAdapter create(final String port, final DeviceConfig deviceConfig);

}
