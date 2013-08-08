package de.uniluebeck.itm.tr.common.plugins;

import com.google.inject.assistedinject.Assisted;

public interface PluginContainerFactory {

	PluginContainer create(@Assisted(PluginContainer.PLUGIN_DIR) String pluginDir,
						   @Assisted(PluginContainer.EXTENDER_BUNDLES) String... extenderBundles);

}
