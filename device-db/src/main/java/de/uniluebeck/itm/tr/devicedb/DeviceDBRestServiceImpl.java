package de.uniluebeck.itm.tr.devicedb;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class DeviceDBRestServiceImpl extends AbstractService implements DeviceDBRestService {

	private static final Logger log = LoggerFactory.getLogger(DeviceDBRestServiceImpl.class);

	private final DeviceDBRestApplication restApplication;

	private final DeviceDBConfig config;

	private final ServicePublisher servicePublisher;

	private ServicePublisherService webApp;

	private ServicePublisherService jaxRsService;

	@Inject
	public DeviceDBRestServiceImpl(final DeviceDBConfig config,
								   final ServicePublisher servicePublisher,
								   final DeviceDBRestApplication restApplication) {
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
			webApp = servicePublisher.createServletService(config.getDeviceDBWebappContextPath(), webAppResourceBase);
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
