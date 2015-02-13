package de.uniluebeck.itm.tr.plugins.defaultimage;

import de.uniluebeck.itm.tr.common.plugins.PluginContainer;
import de.uniluebeck.itm.tr.iwsn.portal.plugins.PortalPluginBundleActivator;

public class DefaultImagePluginActivator extends PortalPluginBundleActivator {

	private DefaultImagePlugin defaultImagePlugin;

	@Override
	protected void doStart() throws Exception {

		defaultImagePlugin = createInjector(new DefaultImagePluginModule()).getInstance(DefaultImagePlugin.class);
		defaultImagePlugin.startAsync().awaitRunning();

		getService(PluginContainer.class).registerService(DefaultImagePlugin.class, defaultImagePlugin);
	}

	@Override
	protected void doStop() throws Exception {

		getService(PluginContainer.class).unregisterService(DefaultImagePlugin.class);

		defaultImagePlugin.stopAsync().awaitTerminated();
	}
}
