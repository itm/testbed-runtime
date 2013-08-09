package de.uniluebeck.itm.tr.common.plugins;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.io.Files.copy;
import static com.google.common.io.Resources.newInputStreamSupplier;

public class PluginContainerImpl extends AbstractService implements PluginContainer {

	private static final Logger log = LoggerFactory.getLogger(PluginContainerImpl.class);

	private static final String[] SYSTEM_BUNDLES = {
			"tr.plugins.framework-extender-0.9-SNAPSHOT.jar",
			"osgi-over-slf4j-1.7.5.jar",
			"org.apache.felix.fileinstall-3.2.0.jar",
			"org.apache.felix.configadmin-1.6.0.jar"
	};

	private final Framework framework;

	private final Map<Class<?>, ServiceRegistration<?>> serviceRegistrationMap = newHashMap();

	@Inject
	public PluginContainerImpl(@Assisted(PluginContainer.PLUGIN_DIR) final String pluginDir,
							   @Assisted(PluginContainer.SYSTEM_PACKAGES) final String... systemPackages) {

		final File storageDir = Files.createTempDir();
		final FrameworkFactory frameworkFactory = ServiceLoader.load(FrameworkFactory.class).iterator().next();

		final Map<String, String> config = new HashMap<String, String>();
		config.put("felix.fileinstall.dir", pluginDir);
		config.put("felix.fileinstall.log.level", "4");
		config.put(Constants.FRAMEWORK_STORAGE, storageDir.getAbsolutePath());
		config.put(Constants.FRAMEWORK_STORAGE_CLEAN, "true");

		final List<String> systemPackageList = newArrayList(systemPackages);
		systemPackageList.add("org.slf4j");
		systemPackageList.add("org.slf4j.helpers");
		systemPackageList.add("org.slf4j.spi");
		systemPackageList.add("org.slf4j.impl");

		config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, Joiner.on(",").join(systemPackageList));

		framework = frameworkFactory.newFramework(config);
	}

	@Override
	protected synchronized void doStart() {
		try {

			log.trace("PluginContainerImpl.doStart()");

			framework.start();
			installSystemBundles();
			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected synchronized void doStop() {
		try {

			log.trace("PluginContainerImpl.doStop()");

			for (Bundle bundle : framework.getBundleContext().getBundles()) {
				if (!"org.apache.felix.framework".equals(bundle.getSymbolicName())) {
					bundle.uninstall();
				}
			}

			framework.stop();
			framework.waitForStop(0);

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	private void installSystemBundles() throws Exception {

		final List<Bundle> installedBundles = new LinkedList<Bundle>();

		for (String systemBundle : SYSTEM_BUNDLES) {
			final String bundlePath = copySystemBundleToTmpFile(systemBundle);
			final Bundle bundle = framework.getBundleContext().installBundle("file:" + bundlePath);
			installedBundles.add(bundle);
		}

		for (Bundle bundle : installedBundles) {
			if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
				bundle.start();
			}
		}
	}

	private String copySystemBundleToTmpFile(final String fileName) throws IOException {
		final URL resource = PluginContainerImpl.class.getResource(fileName);
		final File tempDir = Files.createTempDir();
		final File tempFile = new File(tempDir, fileName);
		copy(newInputStreamSupplier(resource), tempFile);
		return tempFile.getAbsolutePath();
	}

	@Override
	public synchronized <T> ServiceReference<T> registerService(final Class<T> serviceClass, final T service) {

		if (serviceRegistrationMap.containsKey(serviceClass)) {
			throw new IllegalArgumentException(
					"There already a service register under class " + serviceClass.getCanonicalName()
			);
		}

		final ServiceRegistration<T> serviceRegistration =
				framework.getBundleContext().registerService(serviceClass, service, null);

		serviceRegistrationMap.put(serviceClass, serviceRegistration);

		return serviceRegistration.getReference();
	}

	@Override
	public synchronized <T> T getService(final Class<T> serviceClass) {
		return framework.getBundleContext().getService(framework.getBundleContext().getServiceReference(serviceClass));
	}

	@Override
	public synchronized <T> void unregisterService(final Class<T> serviceClass) {

		final ServiceRegistration<?> serviceRegistration = serviceRegistrationMap.get(serviceClass);

		if (serviceRegistration == null) {
			throw new IllegalArgumentException(
					"No service of class " + serviceClass.getCanonicalName() + " was registered!"
			);
		}

		serviceRegistration.unregister();
		serviceRegistrationMap.remove(serviceClass);
	}
}
