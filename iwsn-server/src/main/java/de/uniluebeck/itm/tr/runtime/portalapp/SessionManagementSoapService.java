package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.tr.util.UrlUtils;
import eu.wisebed.api.common.KeyValuePair;
import eu.wisebed.api.common.SecretReservationKey;
import eu.wisebed.api.sm.ExperimentNotRunningException_Exception;
import eu.wisebed.api.sm.SessionManagement;
import eu.wisebed.api.sm.UnknownReservationIdException_Exception;
import eu.wisebed.wiseml.WiseMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import java.net.MalformedURLException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@WebService(
		serviceName = "SessionManagementService",
		targetNamespace = "urn:SessionManagementService",
		portName = "SessionManagementPort",
		endpointInterface = "eu.wisebed.api.sm.SessionManagement"
)
public class SessionManagementSoapService extends AbstractService implements Service, SessionManagement {

	private static final Logger log = LoggerFactory.getLogger(SessionManagementSoapService.class);

	private final SessionManagementService sm;

	private final SessionManagementServiceConfig config;

	private Endpoint endpoint;

	public SessionManagementSoapService(final SessionManagementService sm,
										final SessionManagementServiceConfig config) {

		checkNotNull(sm);
		checkNotNull(config);

		this.sm = sm;
		this.config = config;
	}

	@Override
	protected void doStart() {

		try {

			String bindAllInterfacesUrl = System.getProperty("disableBindAllInterfacesUrl") != null ?
					config.getSessionManagementEndpointUrl().toString() :
					UrlUtils.convertHostToZeros(config.getSessionManagementEndpointUrl().toString());

			log.info("Starting session management SOAP service on binding URL {} for endpoint URL {}",
					bindAllInterfacesUrl,
					config.getSessionManagementEndpointUrl().toString()
			);

			endpoint = Endpoint.publish(bindAllInterfacesUrl, this);

			notifyStarted();

		} catch (MalformedURLException e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		try {

			if (endpoint != null) {
				try {
					endpoint.stop();
				} catch (NullPointerException expectedWellKnownBug) {
					// do nothing
				}
				log.info("Stopped session management SOAP service on {}", config.getSessionManagementEndpointUrl());
			}

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}

	}

	@Override
	public String areNodesAlive(final List<String> nodeUrns, final String controllerEndpointUrl) {

		return sm.areNodesAlive(nodeUrns, controllerEndpointUrl);
	}

	@Override
	public void getConfiguration(final Holder<String> rsEndpointUrl,
								 final Holder<String> snaaEndpointUrl,
								 final Holder<List<KeyValuePair>> options) {

		if (config.getReservationEndpointUrl() != null) {
			rsEndpointUrl.value = config.getReservationEndpointUrl().toString();
		} else {
			rsEndpointUrl.value = "";
		}

		if (config.getSnaaEndpointUrl() != null) {
			snaaEndpointUrl.value = config.getSnaaEndpointUrl().toString();
		} else {
			snaaEndpointUrl.value = "";
		}
	}

	@Override
	public String getInstance(final List<SecretReservationKey> secretReservationKey)
			throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {

		return sm.getInstance(secretReservationKey);
	}

	@Override
	public String getNetwork() {
		return WiseMLHelper.prettyPrintWiseML(WiseMLHelper.readWiseMLFromFile(config.getWiseMLFilename()));
	}
}
