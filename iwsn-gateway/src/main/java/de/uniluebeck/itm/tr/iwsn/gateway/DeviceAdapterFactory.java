package de.uniluebeck.itm.tr.iwsn.gateway;

import de.uniluebeck.itm.tr.devicedb.DeviceConfig;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * A factory to create instances of {@link DeviceAdapter}. For every implementation of DeviceAdapter there must be one
 * DeviceAdapterFactory implementation that is registered with the Testbed Runtime gateway implementation. If a device
 * gets attached to the gateway host (either by getting serially attached or e.g., by coming into communication range)
 * the gateway implementation will retrieve the corresponding {@link DeviceConfig} instance from the {@link
 * de.uniluebeck.itm.tr.devicedb.DeviceDBService} and ask each registered DeviceAdapterFactory if it can handle this
 * type of
 * node (configuration). If true, a new DeviceAdapter instance will then be retrieved or created by calling {@link
 * DeviceAdapterFactory#create(String, String, java.util.Map, de.uniluebeck.itm.tr.devicedb.DeviceConfig)} .
 */
public interface DeviceAdapterFactory {

	/**
	 * Returns true if the {@link DeviceAdapter} instances created by this factory match with the node (configuration)
	 * given in the {@code deviceConfig} parameter.
	 *
	 * @param deviceType
	 * 		the type of the device
	 * @param devicePort
	 * 		the port of the device
	 * @param deviceConfiguration
	 * 		configuration for the device, containing arbitrary key/value pairs
	 * @param deviceConfig
	 * 		the configuration object for the device or null if not applicable
	 *
	 * @return {@code true} if this type of device (config) can be handled, {@code false} otherwise
	 */
	boolean canHandle(String deviceType,
					  String devicePort,
					  @Nullable Map<String, String> deviceConfiguration,
					  @Nullable DeviceConfig deviceConfig);

	/**
	 * Returns a newly created/the {@link DeviceAdapter} instance responsible for the given {@code port} and {@code
	 * deviceConfig}.
	 *
	 * @param deviceType
	 * 		the type of the device
	 * @param devicePort
	 * 		the port of the device
	 * @param deviceConfiguration
	 * 		configuration for the device, containing arbitrary key/value pairs
	 * @param deviceConfig
	 * 		the configuration object for the device or null if not applicable
	 *
	 * @return a newly created/the {@link DeviceAdapter} instance responsible for the given {@code port} and {@code
	 *         deviceConfig}
	 */
	DeviceAdapter create(String deviceType,
						 String devicePort,
						 @Nullable Map<String, String> deviceConfiguration,
						 @Nullable DeviceConfig deviceConfig);

}
