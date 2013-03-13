package de.uniluebeck.itm.tr.devicedb;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceDBServiceImpl extends AbstractService implements DeviceDBService {

	private static final Logger log = LoggerFactory.getLogger(DeviceDBServiceImpl.class);

	private final DeviceDBRestApplication restApplication;

	private final String path;

	private final ServicePublisher servicePublisher;

	@Inject
	public DeviceDBServiceImpl(@Assisted String path,
							   final ServicePublisher servicePublisher,
							   final DeviceDBRestApplication restApplication) {
		this.path = path;
		this.servicePublisher = servicePublisher;
		this.restApplication = restApplication;
	}

	@Override
	protected void doStart() {

		log.trace("DeviceDBServiceImpl.doStart()");

		try {

			servicePublisher.createJaxRsService(path + "/*", restApplication);
			servicePublisher.startAndWait();

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
