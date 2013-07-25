package de.uniluebeck.itm.tr.plugins.defaultimage;

import de.uniluebeck.itm.tr.iwsn.portal.plugins.PortalPluginBundleActivator;

public class DefaultImagePluginActivator extends PortalPluginBundleActivator {

	private DefaultImagePlugin defaultImagePlugin;

	@Override
	protected void doStart() throws Exception {
		defaultImagePlugin = new DefaultImagePluginImpl(rs, portalEventBus, deviceDBService, responseTrackerFactory);
		defaultImagePlugin.startAndWait();
		pluginContainer.registerService(DefaultImagePlugin.class, defaultImagePlugin);
	}

	@Override
	protected void doStop() throws Exception {
		pluginContainer.unregisterService(DefaultImagePlugin.class);
		defaultImagePlugin.stopAndWait();
	}
}
