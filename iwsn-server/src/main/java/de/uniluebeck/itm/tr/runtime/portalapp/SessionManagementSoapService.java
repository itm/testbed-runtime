package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.tr.util.UrlUtils;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.sm.ChannelHandlerDescription;
import eu.wisebed.api.v3.sm.ExperimentNotRunningFault_Exception;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.sm.UnknownReservationIdFault_Exception;
import eu.wisebed.wiseml.WiseMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import java.net.MalformedURLException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

@WebService(
		name = "SessionManagement",
		endpointInterface = "eu.wisebed.api.v3.sm.SessionManagement",
		portName = "SessionManagementPort",
		serviceName = "SessionManagementService",
		targetNamespace = "http://wisebed.eu/api/v3/sm"
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
	public void areNodesAlive(final long requestId, final List<NodeUrn> nodeUrns, final String controllerEndpointUrl) {
		sm.areNodesAlive(requestId, nodeUrns, controllerEndpointUrl);
	}

	@Override
	public void getConfiguration(
			@WebParam(name = "rsEndpointUrl", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<String> rsEndpointUrl,
			@WebParam(name = "snaaEndpointUrl", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<String> snaaEndpointUrl,
			@WebParam(name = "servedUrnPrefixes", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<List<NodeUrnPrefix>> servedUrnPrefixes,
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

		servedUrnPrefixes.value = newArrayList();
		servedUrnPrefixes.value.add(config.getUrnPrefix());
	}

	@Override
	public String getInstance(final List<SecretReservationKey> secretReservationKey)
			throws ExperimentNotRunningFault_Exception, UnknownReservationIdFault_Exception {

		return sm.getInstance(secretReservationKey);
	}

	@Override
	public String getNetwork() {
		return WiseMLHelper.prettyPrintWiseML(WiseMLHelper.readWiseMLFromFile(config.getWiseMLFilename()));
	}

	@Override
	public List<ChannelHandlerDescription> getSupportedChannelHandlers() {
		return sm.getSupportedChannelHandlers();
	}

	@Override
	public List<String> getSupportedVirtualLinkFilters() {
		return sm.getSupportedVirtualLinkFilters();
	}
}
