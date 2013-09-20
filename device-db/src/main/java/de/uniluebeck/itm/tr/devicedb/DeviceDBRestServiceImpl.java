package de.uniluebeck.itm.tr.devicedb;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import org.apache.shiro.web.env.EnvironmentLoaderListener;
import org.apache.shiro.web.servlet.ShiroFilter;
import org.eclipse.jetty.servlet.FilterHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static de.uniluebeck.itm.util.files.FileUtils.assertFileExistsAndIsReadable;

public class DeviceDBRestServiceImpl extends AbstractService implements DeviceDBRestService {

	private static final Logger log = LoggerFactory.getLogger(DeviceDBRestServiceImpl.class);

	private final DeviceDBRestApplication restApplication;

	private final DeviceDBConfig config;

	private final ServicePublisher servicePublisher;

	private final CommonConfig commonConfig;

	private ServicePublisherService webApp;

	private ServicePublisherService jaxRsService;

	@Inject
	public DeviceDBRestServiceImpl(final CommonConfig commonConfig,
								   final DeviceDBConfig config,
								   final ServicePublisher servicePublisher,
								   final DeviceDBRestApplication restApplication) {
		this.commonConfig = commonConfig;
		this.config = checkNotNull(config);
		this.servicePublisher = checkNotNull(servicePublisher);
		this.restApplication = checkNotNull(restApplication);
	}

	@Override
	protected void doStart() {

		log.trace("DeviceDBRestServiceImpl.doStart()");

		try {

			jaxRsService = servicePublisher.createJaxRsService(config.getDeviceDBRestApiContextPath(), restApplication);
			jaxRsService.startAndWait();

			String webAppResourceBase = this.getClass().getResource("/de/uniluebeck/itm/tr/devicedb/webapp").toString();

			final Map<String, String> webAppInitParams = newHashMap();
			webAppInitParams.put(CommonConfig.URN_PREFIX, commonConfig.getUrnPrefix().toString());
			webAppInitParams.put(DeviceDBConfig.DEVICEDB_REST_API_CONTEXT_PATH, config.getDeviceDBRestApiContextPath());
			webAppInitParams.put(DeviceDBConfig.DEVICEDB_WEBAPP_CONTEXT_PATH, config.getDeviceDBWebappContextPath());

			webApp = servicePublisher.createServletService(
					config.getDeviceDBWebappContextPath(),
					webAppResourceBase,
					webAppInitParams
			);

			if (config.getDeviceDBAuthShiroConfig() != null && !"".equals(config.getDeviceDBAuthShiroConfig())) {

				assertFileExistsAndIsReadable(config.getDeviceDBAuthShiroConfig());

				final FilterHolder filterHolder = new FilterHolder();
				filterHolder.setDisplayName("ShiroFilter");
				filterHolder.setHeldClass(ShiroFilter.class);

				//noinspection ConstantConditions
				webApp.getServletContextHandler().addFilter(filterHolder, "/*", EnumSet.of(DispatcherType.REQUEST));

				//noinspection ConstantConditions
				webApp.getServletContextHandler().addEventListener(new EnvironmentLoaderListener());
			}

			webApp.startAndWait();

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}

	}

	@Override
	protected void doStop() {

		log.trace("DeviceDBRestServiceImpl.doStop()");

		try {

			if (webApp != null && webApp.isRunning()) {
				webApp.stopAndWait();
			}

			if (jaxRsService != null && jaxRsService.isRunning()) {
				jaxRsService.stopAndWait();
			}

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
