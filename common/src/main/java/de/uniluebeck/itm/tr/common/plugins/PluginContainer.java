package de.uniluebeck.itm.tr.common.plugins;

import com.google.common.util.concurrent.Service;
import org.osgi.framework.ServiceReference;

public interface PluginContainer extends Service {

	String PLUGIN_DIR = "de.uniluebeck.itm.tr.common.plugins.PluginContainer.pluginDir";
	String SYSTEM_PACKAGES = "de.uniluebeck.itm.tr.common.plugins.PluginContainer.systemPackages";

	<T> ServiceReference<T> registerService(Class<T> serviceClass, T service);

	<T> T getService(Class<T> serviceClass);

	<T> void unregisterService(Class<T> serviceClass);
}
