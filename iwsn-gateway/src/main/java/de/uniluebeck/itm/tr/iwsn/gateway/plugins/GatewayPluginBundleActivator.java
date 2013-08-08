package de.uniluebeck.itm.tr.iwsn.gateway.plugins;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import de.uniluebeck.itm.tr.common.plugins.PluginContainer;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.iwsn.gateway.DeviceAdapterRegistry;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayEventBus;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayScheduler;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.snaa.SNAA;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import static com.google.inject.util.Providers.of;

public abstract class GatewayPluginBundleActivator implements BundleActivator {

	protected BundleContext bundleContext;

	protected ServiceReference<GatewayEventBus> gatewayEventBusServiceReference;

	protected GatewayEventBus gatewayEventBus;

	protected ServiceReference<SessionManagement> sessionManagementServiceReference;

	protected SessionManagement sessionManagement;

	protected ServiceReference<SNAA> snaaServiceReference;

	protected SNAA snaa;

	protected ServiceReference<RS> rsServiceReference;

	protected RS rs;

	protected ServiceReference<PluginContainer> pluginContainerServiceReference;

	protected PluginContainer pluginContainer;

	protected ServiceReference<DeviceDBService> deviceDBServiceReference;

	protected DeviceDBService deviceDBService;

	protected ServiceReference<DeviceAdapterRegistry> deviceAdapterRegistryServiceReference;

	protected DeviceAdapterRegistry deviceAdapterRegistry;

	protected ServiceReference<GatewayScheduler> gatewaySchedulerServiceReference;

	protected GatewayScheduler gatewayScheduler;

	@Override
	public final void start(final BundleContext bundleContext) throws Exception {

		this.bundleContext = bundleContext;

		gatewaySchedulerServiceReference = bundleContext.getServiceReference(GatewayScheduler.class);
		gatewayScheduler = bundleContext.getService(gatewaySchedulerServiceReference);

		deviceAdapterRegistryServiceReference = bundleContext.getServiceReference(DeviceAdapterRegistry.class);
		deviceAdapterRegistry = bundleContext.getService(deviceAdapterRegistryServiceReference);

		deviceDBServiceReference = bundleContext.getServiceReference(DeviceDBService.class);
		deviceDBService = bundleContext.getService(deviceDBServiceReference);

		gatewayEventBusServiceReference = bundleContext.getServiceReference(GatewayEventBus.class);
		gatewayEventBus = bundleContext.getService(gatewayEventBusServiceReference);

		sessionManagementServiceReference = bundleContext.getServiceReference(SessionManagement.class);
		sessionManagement = bundleContext.getService(sessionManagementServiceReference);

		snaaServiceReference = bundleContext.getServiceReference(SNAA.class);
		snaa = bundleContext.getService(snaaServiceReference);

		rsServiceReference = bundleContext.getServiceReference(RS.class);
		rs = bundleContext.getService(rsServiceReference);

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

		sessionManagement = null;
		bundleContext.ungetService(sessionManagementServiceReference);
		sessionManagementServiceReference = null;

		snaa = null;
		bundleContext.ungetService(snaaServiceReference);
		snaaServiceReference = null;

		rs = null;
		bundleContext.ungetService(rsServiceReference);
		rsServiceReference = null;

		deviceDBService = null;
		bundleContext.ungetService(deviceDBServiceReference);
		deviceDBServiceReference = null;

		deviceAdapterRegistry = null;
		bundleContext.ungetService(deviceAdapterRegistryServiceReference);
		deviceAdapterRegistryServiceReference = null;

		gatewayScheduler = null;
		bundleContext.ungetService(gatewaySchedulerServiceReference);
		gatewaySchedulerServiceReference = null;

		this.bundleContext = null;
	}

	protected abstract void doStop() throws Exception;

	protected Injector createInjector(final Module... modules) {
		return Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {

				bind(PluginContainer.class).toProvider(of(pluginContainer));

				bind(GatewayEventBus.class).toProvider(of(gatewayEventBus));
				bind(SessionManagement.class).toProvider(of(sessionManagement));
				bind(SNAA.class).toProvider(of(snaa));
				bind(RS.class).toProvider(of(rs));
				bind(DeviceDBService.class).toProvider(of(deviceDBService));
				bind(DeviceAdapterRegistry.class).toProvider(of(deviceAdapterRegistry));
				bind(GatewayScheduler.class).toProvider(of(gatewayScheduler));
			}
		}
		).createChildInjector(modules);
	}
}
