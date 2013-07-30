package de.uniluebeck.itm.tr.iwsn.portal.plugins;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import de.uniluebeck.itm.tr.common.plugins.PluginContainer;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerFactory;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.snaa.SNAA;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import static com.google.inject.util.Providers.of;

public abstract class PortalPluginBundleActivator implements BundleActivator {

	protected BundleContext bundleContext;

	protected ReservationManager reservationManager;

	protected ServiceReference<ReservationManager> reservationManagerServiceReference;

	protected PortalEventBus portalEventBus;

	protected ServiceReference<PortalEventBus> portalEventBusServiceReference;

	protected SessionManagement sessionManagement;

	protected ServiceReference<SessionManagement> sessionManagementServiceReference;

	protected SNAA snaa;

	protected ServiceReference<SNAA> snaaServiceReference;

	protected RS rs;

	protected ServiceReference<RS> rsServiceReference;

	protected PluginContainer pluginContainer;

	protected ServiceReference<PluginContainer> pluginContainerServiceReference;

	protected ServiceReference<DeviceDBService> deviceDBServiceReference;

	protected DeviceDBService deviceDBService;

	protected ServiceReference<ResponseTrackerFactory> responseTrackerFactoryServiceReference;

	protected ResponseTrackerFactory responseTrackerFactory;

	@Override
	public final void start(final BundleContext bundleContext) throws Exception {

		this.bundleContext = bundleContext;

		responseTrackerFactoryServiceReference = bundleContext.getServiceReference(ResponseTrackerFactory.class);
		responseTrackerFactory = bundleContext.getService(responseTrackerFactoryServiceReference);

		deviceDBServiceReference = bundleContext.getServiceReference(DeviceDBService.class);
		deviceDBService = bundleContext.getService(deviceDBServiceReference);

		reservationManagerServiceReference = bundleContext.getServiceReference(ReservationManager.class);
		reservationManager = bundleContext.getService(reservationManagerServiceReference);

		portalEventBusServiceReference = bundleContext.getServiceReference(PortalEventBus.class);
		portalEventBus = bundleContext.getService(portalEventBusServiceReference);

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

		reservationManager = null;
		bundleContext.ungetService(reservationManagerServiceReference);
		reservationManagerServiceReference = null;

		portalEventBus = null;
		bundleContext.ungetService(portalEventBusServiceReference);
		portalEventBusServiceReference = null;

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

		responseTrackerFactory = null;
		bundleContext.ungetService(responseTrackerFactoryServiceReference);
		responseTrackerFactoryServiceReference = null;

		this.bundleContext = null;
	}

	protected abstract void doStop() throws Exception;

	protected Injector createInjector(final Module... modules) {
		return Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(ReservationManager.class).toProvider(of(reservationManager));
				bind(PortalEventBus.class).toProvider(of(portalEventBus));
				bind(SessionManagement.class).toProvider(of(sessionManagement));
				bind(SNAA.class).toProvider(of(snaa));
				bind(RS.class).toProvider(of(rs));
				bind(PluginContainer.class).toProvider(of(pluginContainer));
				bind(DeviceDBService.class).toProvider(of(deviceDBService));
				bind(ResponseTrackerFactory.class).toProvider(of(responseTrackerFactory));
			}
		}
		).createChildInjector(modules);
	}
}
