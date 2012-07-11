package de.uniluebeck.itm.tr.runtime.wsnapp;

import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory;

import javax.annotation.Nonnull;

public interface WSNDeviceAppGuiceFactory {

	WSNDeviceApp create(@Nonnull TestbedRuntime testbedRuntime,
						@Nonnull DeviceFactory deviceFactory,
						@Nonnull WSNDeviceAppConfiguration configuration,
						@Nonnull WSNDeviceAppConnectorConfiguration connectorConfiguration);

}
