package de.uniluebeck.itm.tr.devicedb;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import org.apache.shiro.config.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static de.uniluebeck.itm.tr.devicedb.DeviceDBConstants.DEVICEDB_REST_API_CONTEXT_PATH;
import static de.uniluebeck.itm.tr.devicedb.DeviceDBConstants.DEVICEDB_WEBAPP_CONTEXT_PATH;

public class DeviceDBRestServiceImpl extends AbstractService implements DeviceDBRestService {

	private static final Logger log = LoggerFactory.getLogger(DeviceDBRestServiceImpl.class);

	private final DeviceDBRestApplication restApplication;

	private final ServicePublisher servicePublisher;

	private final CommonConfig commonConfig;

	private ServicePublisherService webApp;

	private ServicePublisherService restService;

	@Inject
	public DeviceDBRestServiceImpl(final CommonConfig commonConfig,
								   final ServicePublisher servicePublisher,
								   final DeviceDBRestApplication restApplication) {
		this.commonConfig = checkNotNull(commonConfig);
		this.servicePublisher = checkNotNull(servicePublisher);
		this.restApplication = checkNotNull(restApplication);
	}

	@Override
	protected void doStart() {

		log.trace("DeviceDBRestServiceImpl.doStart()");

		try {

			final Ini shiroRestIni = new Ini();
			shiroRestIni.addSection("urls");
			shiroRestIni.getSection("urls").put("/admin/**", "authcBasic");
			shiroRestIni.addSection("users");
			shiroRestIni.getSection("users").put(commonConfig.getAdminUsername(), commonConfig.getAdminPassword());

			restService = servicePublisher.createJaxRsService(DEVICEDB_REST_API_CONTEXT_PATH, restApplication, shiroRestIni);
			restService.startAndWait();

			String webAppResourceBase = this.getClass().getResource("/de/uniluebeck/itm/tr/devicedb/webapp").toString();

			final Map<String, String> webAppInitParams = newHashMap();
			webAppInitParams.put(CommonConfig.URN_PREFIX, commonConfig.getUrnPrefix().toString());

			final Ini shiroWebappIni = new Ini();
			shiroWebappIni.addSection("urls");
			shiroWebappIni.getSection("urls").put("/**", "authcBasic");
			shiroWebappIni.addSection("users");
			shiroWebappIni.getSection("users").put(commonConfig.getAdminUsername(), commonConfig.getAdminPassword());

			webApp = servicePublisher.createServletService(
					DEVICEDB_WEBAPP_CONTEXT_PATH,
					webAppResourceBase,
					webAppInitParams,
					shiroWebappIni
			);

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

			if (restService != null && restService.isRunning()) {
				restService.stopAndWait();
			}

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
