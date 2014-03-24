package de.uniluebeck.itm.tr.iwsn.portal.plugins;

import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
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

import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;

public abstract class PortalPluginBundleActivator implements BundleActivator {

	public static final Set<Class<? extends Object>> SERVICES_AVAILABLE = Sets.newHashSet(
			ReservationManager.class,
			PortalEventBus.class,
			SessionManagement.class,
			SNAA.class,
			RS.class,
			PluginContainer.class,
			DeviceDBService.class,
			ResponseTrackerFactory.class,
			ServicePublisher.class,
			ServedNodeUrnsProvider.class,
			ServedNodeUrnPrefixesProvider.class
	);

	protected BundleContext bundleContext;

	protected final Map<Class<?>, ServiceReference> serviceReferences = newHashMap();

	protected final Map<Class<?>, Object> services = newHashMap();

	@Override
	public final void start(final BundleContext bundleContext) throws Exception {

		this.bundleContext = bundleContext;

		for (Class<?> clazz : SERVICES_AVAILABLE) {
			final ServiceReference<?> serviceReference = bundleContext.getServiceReference(clazz);
			serviceReferences.put(clazz, serviceReference);
			services.put(clazz, bundleContext.getService(serviceReference));
		}

		doStart();
	}

	protected abstract void doStart() throws Exception;

	@Override
	public final void stop(final BundleContext bundleContext) throws Exception {

		doStop();

		for (Class<?> clazz : SERVICES_AVAILABLE) {
			bundleContext.ungetService(serviceReferences.get(clazz));
		}

		serviceReferences.clear();
		services.clear();

		this.bundleContext = null;
	}

	protected abstract void doStop() throws Exception;

	protected <T> T getService(Class<T> clazz) {
		if (!services.containsKey(clazz)) {
			throw new IllegalArgumentException("Service of type " +
					clazz.getCanonicalName() + " not available to plugins!"
			);
		}
		return clazz.cast(services.get(clazz));
	}

	protected Injector createInjector(final Module... modules) {
		return Guice.createInjector(new AbstractModule() {
			@Override
			@SuppressWarnings("unchecked")
			protected void configure() {
				for (Class<?> clazz : SERVICES_AVAILABLE) {
					bind((Class<Object>) clazz).toInstance(services.get(clazz));
				}
			}
		}
		).createChildInjector(modules);
	}
}
