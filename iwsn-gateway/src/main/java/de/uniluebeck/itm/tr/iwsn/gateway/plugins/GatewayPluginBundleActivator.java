package de.uniluebeck.itm.tr.iwsn.gateway.plugins;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import de.uniluebeck.itm.tr.common.plugins.PluginContainer;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.iwsn.gateway.DeviceAdapterRegistry;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayEventBus;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import static com.google.inject.util.Providers.of;

public abstract class GatewayPluginBundleActivator implements BundleActivator {

	protected BundleContext bundleContext;

	protected ServiceReference<GatewayEventBus> gatewayEventBusServiceReference;

	protected GatewayEventBus gatewayEventBus;

	protected ServiceReference<PluginContainer> pluginContainerServiceReference;

	protected PluginContainer pluginContainer;

	protected ServiceReference<DeviceDBService> deviceDBServiceReference;

	protected DeviceDBService deviceDBService;

	protected ServiceReference<DeviceAdapterRegistry> deviceAdapterRegistryServiceReference;

	protected DeviceAdapterRegistry deviceAdapterRegistry;

	protected ServiceReference<SchedulerService> schedulerServiceServiceReference;

	protected SchedulerService schedulerService;

	@Override
	public final void start(final BundleContext bundleContext) throws Exception {

		this.bundleContext = bundleContext;

		schedulerServiceServiceReference = bundleContext.getServiceReference(SchedulerService.class);
		schedulerService = bundleContext.getService(schedulerServiceServiceReference);

		deviceAdapterRegistryServiceReference = bundleContext.getServiceReference(DeviceAdapterRegistry.class);
		deviceAdapterRegistry = bundleContext.getService(deviceAdapterRegistryServiceReference);

		deviceDBServiceReference = bundleContext.getServiceReference(DeviceDBService.class);
		deviceDBService = bundleContext.getService(deviceDBServiceReference);

		gatewayEventBusServiceReference = bundleContext.getServiceReference(GatewayEventBus.class);
		gatewayEventBus = bundleContext.getService(gatewayEventBusServiceReference);

		pluginContainerServiceReference = bundleContext.getServiceReference(PluginContainer.class);
		pluginContainer = bundleContext.getService(pluginContainerServiceReference);

		doStart();
	}

	protected abstract void doStart() throws Exception;

	@Override
	public final void stop(final BundleContext bundleContext) throws Exception {

		doStop();

		pluginContainer = null;
		bundleContext.ungetService(pluginContainerServiceReference);
		pluginContainerServiceReference = null;

		gatewayEventBus = null;
		bundleContext.ungetService(gatewayEventBusServiceReference);
		gatewayEventBusServiceReference = null;

		deviceDBService = null;
		bundleContext.ungetService(deviceDBServiceReference);
		deviceDBServiceReference = null;

		deviceAdapterRegistry = null;
		bundleContext.ungetService(deviceAdapterRegistryServiceReference);
		deviceAdapterRegistryServiceReference = null;

		schedulerService = null;
		bundleContext.ungetService(schedulerServiceServiceReference);
		schedulerServiceServiceReference = null;

		this.bundleContext = null;
	}

	protected abstract void doStop() throws Exception;
}
