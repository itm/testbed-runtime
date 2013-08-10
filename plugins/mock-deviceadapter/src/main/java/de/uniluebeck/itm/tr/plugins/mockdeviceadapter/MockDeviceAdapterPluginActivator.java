package de.uniluebeck.itm.tr.plugins.mockdeviceadapter;

import de.uniluebeck.itm.tr.iwsn.gateway.plugins.GatewayPluginBundleActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockDeviceAdapterPluginActivator extends GatewayPluginBundleActivator {

	private static final Logger log = LoggerFactory.getLogger(MockDeviceAdapterPluginActivator.class);

	private MockDeviceAdapterFactory deviceAdapterFactory;

	@Override
	protected void doStart() throws Exception {

		log.trace("MockDeviceAdapterPluginActivator.doStart()");

		deviceAdapterFactory = new MockDeviceAdapterFactory(gatewayScheduler);
		deviceAdapterRegistry.addDeviceAdapterFactory(deviceAdapterFactory, MockDeviceAdapter.class);
	}

	@Override
	protected void doStop() throws Exception {

		log.trace("MockDeviceAdapterPluginActivator.doStop()");

		deviceAdapterRegistry.removeDeviceAdapterFactory(deviceAdapterFactory, MockDeviceAdapter.class);
		deviceAdapterFactory = null;
	}
}
