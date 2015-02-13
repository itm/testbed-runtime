package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.common.Constants;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import org.apache.shiro.config.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class ShiroSNAARestServiceImpl extends AbstractService implements ShiroSNAARestService {

	private static final Logger log = LoggerFactory.getLogger(ShiroSNAARestServiceImpl.class);

	private final ServicePublisher servicePublisher;

	private final ShiroSNAARestApplication application;

	private final CommonConfig commonConfig;

	private ServicePublisherService jaxRsService;

	private ServicePublisherService webApp;

	@Inject
	public ShiroSNAARestServiceImpl(final ServicePublisher servicePublisher,
									final ShiroSNAARestApplication application,
									final CommonConfig commonConfig) {
		this.servicePublisher = servicePublisher;
		this.application = application;
		this.commonConfig = commonConfig;
	}

	@Override
	protected void doStart() {
		log.debug("ShiroSNAARestServiceImpl.doStart()");
		try {

			final Ini shiroIni = new Ini();
			shiroIni.addSection("urls");
			shiroIni.getSection("urls").put("/**", "authcBasic");
			shiroIni.addSection("users");
			shiroIni.getSection("users").put(commonConfig.getAdminUsername(), commonConfig.getAdminPassword());

			jaxRsService = servicePublisher.createJaxRsService(
					Constants.SHIRO_SNAA.ADMIN_REST_API_CONTEXT_PATH_VALUE,
					application,
					shiroIni
			);
			jaxRsService.startAsync().awaitRunning();

			String webAppResourceBase =
					this.getClass().getResource("/de/uniluebeck/itm/tr/snaa/shiro/webapp").toString();

			final Map<String, String> webAppInitParams = newHashMap();

			webAppInitParams.put(
					Constants.SHIRO_SNAA.ADMIN_REST_API_CONTEXT_PATH_KEY,
					Constants.SHIRO_SNAA.ADMIN_REST_API_CONTEXT_PATH_VALUE
			);

			webAppInitParams.put(
					Constants.SHIRO_SNAA.ADMIN_WEB_APP_CONTEXT_PATH_KEY,
					Constants.SHIRO_SNAA.ADMIN_WEB_APP_CONTEXT_PATH_VALUE
			);

			webAppInitParams.put(
					Constants.DEVICE_DB.DEVICEDB_REST_API_CONTEXT_PATH_KEY,
					Constants.DEVICE_DB.DEVICEDB_REST_API_CONTEXT_PATH_VALUE
			);

			webApp = servicePublisher.createServletService(
					Constants.SHIRO_SNAA.ADMIN_WEB_APP_CONTEXT_PATH_VALUE,
					webAppResourceBase,
					webAppInitParams,
					shiroIni
			);
			webApp.startAsync().awaitRunning();

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
				webApp.stopAsync().awaitTerminated();
			}
			if (jaxRsService != null && jaxRsService.isRunning()) {
				jaxRsService.stopAsync().awaitTerminated();
			}
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
