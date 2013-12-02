package eu.smartsantander.tr.plugins.waspmotedeviceadapter;

import de.uniluebeck.itm.tr.iwsn.gateway.plugins.GatewayPluginBundleActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaspmoteDeviceAdapterPluginActivator extends GatewayPluginBundleActivator {

	private static final Logger log = LoggerFactory.getLogger(WaspmoteDeviceAdapterPluginActivator.class);

	private WaspmoteDeviceAdapterFactory deviceAdapterFactory;

	@Override
	protected void doStart() throws Exception {

		log.trace("WaspmoteDeviceAdapterPluginActivator.doStart()");

		deviceAdapterFactory = new WaspmoteDeviceAdapterFactory();
		deviceAdapterRegistry.addDeviceAdapterFactory(deviceAdapterFactory, WaspmoteDeviceAdapter.class);
	}

	@Override
	protected void doStop() throws Exception {

		log.trace("WaspmoteDeviceAdapterPluginActivator.doStop()");

		deviceAdapterRegistry.removeDeviceAdapterFactory(deviceAdapterFactory, WaspmoteDeviceAdapter.class);
		deviceAdapterFactory = null;
	}
}
