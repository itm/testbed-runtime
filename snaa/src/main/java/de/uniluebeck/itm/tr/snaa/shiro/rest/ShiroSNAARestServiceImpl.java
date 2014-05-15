package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import org.apache.shiro.config.Ini;
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

	private final CommonConfig commonConfig;

	private ServicePublisherService jaxRsService;

	private ServicePublisherService webApp;

	@Inject
	public ShiroSNAARestServiceImpl(final ServicePublisher servicePublisher,
									final ShiroSNAARestApplication application,
									final SNAAServiceConfig config,
									final CommonConfig commonConfig) {
		this.servicePublisher = servicePublisher;
		this.application = application;
		this.config = config;
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
					"/admin/shiro-snaa/rest",
					application,
					shiroIni
			);
			jaxRsService.startAndWait();

			String webAppResourceBase =
					this.getClass().getResource("/de/uniluebeck/itm/tr/snaa/shiro/webapp").toString();

			final Map<String, String> webAppInitParams = newHashMap();

			webAppInitParams.put(
					SHIRO_ADMIN_DEVICE_DB_REST_API_CONTEXTPATH,
					config.getShiroAdminDeviceDBRestApiContextPath()
			);

			webApp = servicePublisher.createServletService(
					"/admin/shiro-snaa",
					webAppResourceBase,
					webAppInitParams,
					shiroIni
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
