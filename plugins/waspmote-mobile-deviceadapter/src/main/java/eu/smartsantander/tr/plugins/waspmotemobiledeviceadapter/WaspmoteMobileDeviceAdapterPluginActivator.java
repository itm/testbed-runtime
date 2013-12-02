package eu.smartsantander.tr.plugins.waspmotemobiledeviceadapter;

import de.uniluebeck.itm.tr.iwsn.gateway.plugins.GatewayPluginBundleActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaspmoteMobileDeviceAdapterPluginActivator extends GatewayPluginBundleActivator {

	private static final Logger log = LoggerFactory.getLogger(WaspmoteMobileDeviceAdapterPluginActivator.class);

	private WaspmoteMobileDeviceAdapterFactory deviceAdapterFactory;

	@Override
	protected void doStart() throws Exception {

		log.trace("WaspmoteDeviceAdapterPluginActivator.doStart()");

		deviceAdapterFactory = new WaspmoteMobileDeviceAdapterFactory();
		deviceAdapterRegistry.addDeviceAdapterFactory(deviceAdapterFactory, WaspmoteMobileDeviceAdapter.class);
	}

	@Override
	protected void doStop() throws Exception {

		log.trace("WaspmoteDeviceAdapterPluginActivator.doStop()");

		deviceAdapterRegistry.removeDeviceAdapterFactory(deviceAdapterFactory, WaspmoteMobileDeviceAdapter.class);
		deviceAdapterFactory = null;
	}
}
