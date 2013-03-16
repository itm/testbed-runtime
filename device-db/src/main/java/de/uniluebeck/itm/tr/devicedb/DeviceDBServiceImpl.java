package de.uniluebeck.itm.tr.devicedb;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceDBServiceImpl extends AbstractService implements DeviceDBService {

	private static final Logger log = LoggerFactory.getLogger(DeviceDBServiceImpl.class);

	private final DeviceDBRestApplication restApplication;

	private final String restPath;

	private final String webAppPath;

	private final ServicePublisher servicePublisher;

	@Inject
	public DeviceDBServiceImpl(@Assisted("path_rest") String restPath,
							   @Assisted("path_webapp") String webAppPath,
							   final ServicePublisher servicePublisher,
							   final DeviceDBRestApplication restApplication) {
		this.restPath = restPath;
		this.webAppPath = webAppPath;
		this.servicePublisher = servicePublisher;
		this.restApplication = restApplication;
	}

	@Override
	protected void doStart() {

		log.trace("DeviceDBServiceImpl.doStart()");

		try {

			servicePublisher.createJaxRsService(restPath, restApplication);
			String webAppResourceBase = this.getClass().getResource("/de/uniluebeck/itm/tr/devicedb/webapp").toString();
			ServicePublisherService ss = servicePublisher.createServletService(webAppPath, webAppResourceBase);
			servicePublisher.startAndWait();
			ss.startAndWait();

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}

	}

	@Override
	protected void doStop() {

		log.trace("DeviceDBServiceImpl.doStop()");

		try {

			if (servicePublisher != null && servicePublisher.isRunning()) {
				servicePublisher.stopAndWait();
			}

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
