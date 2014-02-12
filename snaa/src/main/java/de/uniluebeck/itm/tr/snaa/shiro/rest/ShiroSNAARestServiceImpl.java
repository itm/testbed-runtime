package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static de.uniluebeck.itm.tr.snaa.SNAAServiceConfig.*;

public class ShiroSNAARestServiceImpl extends AbstractService implements ShiroSNAARestService {

	private static final Logger log = LoggerFactory.getLogger(ShiroSNAARestServiceImpl.class);

	private final ServicePublisher servicePublisher;

	private final ShiroSNAARestApplication application;

	private final SNAAServiceConfig config;

	private ServicePublisherService jaxRsService;

	private ServicePublisherService webApp;

	@Inject
	public ShiroSNAARestServiceImpl(final ServicePublisher servicePublisher,
									final ShiroSNAARestApplication application,
									final SNAAServiceConfig config) {
		this.servicePublisher = servicePublisher;
		this.application = application;
		this.config = config;
	}

	@Override
	protected void doStart() {
		log.debug("ShiroSNAARestServiceImpl.doStart()");
		try {

			jaxRsService = servicePublisher.createJaxRsService(config.getShiroAdminRestApiContextPath(), application);
			jaxRsService.startAndWait();

			String webAppResourceBase =
					this.getClass().getResource("/de/uniluebeck/itm/tr/snaa/shiro/webapp").toString();

			final Map<String, String> webAppInitParams = newHashMap();

			webAppInitParams.put(
					SHIRO_ADMIN_REST_API_CONTEXTPATH,
					config.getShiroAdminRestApiContextPath()
			);

			webAppInitParams.put(
					SHIRO_ADMIN_WEBAPP_CONTEXTPATH,
					config.getShiroAdminWebappContextPath()
			);

			webAppInitParams.put(
					SHIRO_ADMIN_DEVICE_DB_REST_API_CONTEXTPATH,
					config.getShiroAdminDeviceDBRestApiContextPath()
			);

			webApp = servicePublisher.createServletService(
					config.getShiroAdminWebappContextPath(),
					webAppResourceBase,
					webAppInitParams
			);
			webApp.startAndWait();

			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.debug("ShiroSNAARestServiceImpl.doStop()");
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
