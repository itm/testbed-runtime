package de.uniluebeck.itm.tr.runtime.portalapp;

import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;
import de.uniluebeck.itm.tr.util.Service;
import de.uniluebeck.itm.tr.util.UrlUtils;
import eu.wisebed.api.common.KeyValuePair;
import eu.wisebed.api.sm.ExperimentNotRunningException_Exception;
import eu.wisebed.api.sm.SecretReservationKey;
import eu.wisebed.api.sm.SessionManagement;
import eu.wisebed.api.sm.UnknownReservationIdException_Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@WebService(
		serviceName = "SessionManagementService",
		targetNamespace = "urn:SessionManagementService",
		portName = "SessionManagementPort",
		endpointInterface = "eu.wisebed.api.sm.SessionManagement"
)
public class SessionManagementSoapService implements Service, SessionManagement {

	private static final Logger log = LoggerFactory.getLogger(SessionManagementSoapService.class);

	private final SessionManagementService sm;

	private final SessionManagementServiceConfig config;

	private Endpoint endpoint;

	public SessionManagementSoapService(final SessionManagementService sm, final SessionManagementServiceConfig config) {

		checkNotNull(sm);
		checkNotNull(config);

		this.sm = sm;
		this.config = config;
	}

	@Override
	public void start() throws Exception {

		String bindAllInterfacesUrl = System.getProperty("disableBindAllInterfacesUrl") != null ?
				config.getSessionManagementEndpointUrl().toString() :
				UrlUtils.convertHostToZeros(config.getSessionManagementEndpointUrl().toString());

		log.info("Starting Session Management service on binding URL {} for endpoint URL {}",
				bindAllInterfacesUrl,
				config.getSessionManagementEndpointUrl().toString()
		);

		endpoint = Endpoint.publish(bindAllInterfacesUrl, this);
	}

	@Override
	public void stop() {

		if (endpoint != null) {
			try {
				endpoint.stop();
			} catch (NullPointerException expectedWellKnownBug) {
				// do nothing
			}
			log.info("Stopped Session Management service on {}", config.getSessionManagementEndpointUrl());
		}
	}

	@Override
	public String areNodesAlive(@WebParam(name = "nodes", targetNamespace = "") final List<String> nodes,
								@WebParam(name = "controllerEndpointUrl", targetNamespace = "") final
								String controllerEndpointUrl) {

		return sm.areNodesAlive(nodes, controllerEndpointUrl);
	}

	@Override
	public void free(
			@WebParam(name = "secretReservationKey", targetNamespace = "") final
			List<SecretReservationKey> secretReservationKey)
			throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {

		sm.free(secretReservationKey);
	}

	@Override
	public void getConfiguration(
			@WebParam(name = "rsEndpointUrl", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<String> rsEndpointUrl,
			@WebParam(name = "snaaEndpointUrl", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<String> snaaEndpointUrl,
			@WebParam(name = "options", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<List<KeyValuePair>> options) {

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
	public String getInstance(
			@WebParam(name = "secretReservationKey", targetNamespace = "") final
			List<SecretReservationKey> secretReservationKey,
			@WebParam(name = "controller", targetNamespace = "") final String controller)
			throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {

		return sm.getInstance(secretReservationKey, controller);
	}

	@Override
	public String getNetwork() {
		return WiseMLHelper.prettyPrintWiseML(WiseMLHelper.readWiseMLFromFile(config.getWiseMLFilename()));
	}
}
