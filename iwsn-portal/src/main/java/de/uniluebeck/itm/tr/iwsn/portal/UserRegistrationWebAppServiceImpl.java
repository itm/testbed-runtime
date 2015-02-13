package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.AbstractService;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class UserRegistrationWebAppServiceImpl extends AbstractService implements UserRegistrationWebAppService {

	private static final Logger log = LoggerFactory.getLogger(UserRegistrationWebAppServiceImpl.class);

	private final ServicePublisher servicePublisher;

	private ServicePublisherService webapp;

	@Inject
	public UserRegistrationWebAppServiceImpl(final ServicePublisher servicePublisher) {
		this.servicePublisher = servicePublisher;
	}

	@Override
	protected void doStart() {
		log.trace("UserRegistrationWebAppServiceImpl.doStart()");
		try {

			final String resourceBase = this.getClass().getResource("/de/uniluebeck/itm/tr/iwsn/portal/userregistration").toString();
			final Map<String, String> params = newHashMap();

			params.put(Constants.REST_API_V1.REST_API_CONTEXT_PATH_KEY, Constants.REST_API_V1.REST_API_CONTEXT_PATH_VALUE);

			webapp = servicePublisher.createServletService(Constants.USER_REG.WEB_APP_CONTEXT_PATH, resourceBase, params, null);
			webapp.startAsync().awaitRunning();

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.trace("UserRegistrationWebAppServiceImpl.doStop()");
		try {

			webapp.stopAsync().awaitTerminated();
			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
