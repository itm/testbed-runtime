package de.uniluebeck.itm.tr.plugins.defaultimage;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeStatusTrackerResourceServiceImpl extends AbstractService implements NodeStatusTrackerResourceService {

	private static final Logger log = LoggerFactory.getLogger(NodeStatusTrackerResourceServiceImpl.class);

	private static final String REST_API_CONTEXT_PATH = "/plugins/default_image";

	private final ServicePublisher servicePublisher;

	private final NodeStatusTrackerApplication nodeStatusTrackerApplication;

	private ServicePublisherService jaxRsService;

	@Inject
	public NodeStatusTrackerResourceServiceImpl(final ServicePublisher servicePublisher,
												final NodeStatusTrackerApplication nodeStatusTrackerApplication) {
		this.servicePublisher = servicePublisher;
		this.nodeStatusTrackerApplication = nodeStatusTrackerApplication;
	}

	@Override
	protected void doStart() {
		try {
			jaxRsService = servicePublisher.createJaxRsService(
					REST_API_CONTEXT_PATH,
					nodeStatusTrackerApplication,
					null
			);
			jaxRsService.startAndWait();
			log.info("Started Default Image Plugin HTTP API at {}", REST_API_CONTEXT_PATH);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			if (jaxRsService != null && jaxRsService.isRunning()) {
				jaxRsService.stopAndWait();
				log.info("Stopped Default Image Plugin HTTP API at {}", REST_API_CONTEXT_PATH);
			}
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
