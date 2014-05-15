package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.AbstractService;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class UserRegistrationWebAppServiceImpl extends AbstractService implements UserRegistrationWebAppService {

	private static final Logger log = LoggerFactory.getLogger(UserRegistrationWebAppServiceImpl.class);

	private final ServicePublisher servicePublisher;

	private final PortalServerConfig portalServerConfig;

	private ServicePublisherService webapp;

	@Inject
	public UserRegistrationWebAppServiceImpl(final ServicePublisher servicePublisher,
											 final PortalServerConfig portalServerConfig) {
		this.servicePublisher = servicePublisher;
		this.portalServerConfig = portalServerConfig;
	}

	@Override
	protected void doStart() {
		log.trace("UserRegistrationWebAppServiceImpl.doStart()");
		try {

			final String resourceBase = this.getClass().getResource("/de/uniluebeck/itm/tr/iwsn/portal/userregistration").toString();
			final Map<String, String> params = newHashMap();

			params.put(PortalServerConfig.REST_API_CONTEXT_PATH, portalServerConfig.getRestApiContextPath());

			webapp = servicePublisher.createServletService("/user_registration", resourceBase, params, null);
			webapp.startAndWait();

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.trace("UserRegistrationWebAppServiceImpl.doStop()");
		try {

			webapp.stopAndWait();
			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
