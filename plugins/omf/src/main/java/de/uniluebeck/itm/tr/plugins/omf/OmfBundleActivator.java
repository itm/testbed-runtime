package de.uniluebeck.itm.tr.plugins.omf;

import de.uniluebeck.itm.tr.iwsn.portal.plugins.PortalPluginBundleActivator;

public class OmfBundleActivator extends PortalPluginBundleActivator {

	private OmfBundlePlugin omfBundlePlugin;

	@Override
	protected void doStart() throws Exception {
		omfBundlePlugin = new OmfBundlePlugin(portalEventBus);
		omfBundlePlugin.startAndWait();
	}

	@Override
	protected void doStop() throws Exception {
		omfBundlePlugin.stopAndWait();
		omfBundlePlugin = null;
	}
}
