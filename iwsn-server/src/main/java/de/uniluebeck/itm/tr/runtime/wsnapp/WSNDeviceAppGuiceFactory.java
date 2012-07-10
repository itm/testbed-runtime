package de.uniluebeck.itm.tr.runtime.wsnapp;

import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;

import javax.annotation.Nonnull;

public interface WSNDeviceAppGuiceFactory {

	WSNDeviceApp create(@Nonnull TestbedRuntime testbedRuntime,
						@Nonnull WSNDeviceAppConfiguration configuration,
						@Nonnull WSNDeviceAppConnectorConfiguration connectorConfiguration);

}
