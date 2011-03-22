/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.runtime.portalapp;

import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufControllerHelper;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufControllerServer;
import de.uniluebeck.itm.tr.runtime.portalapp.xml.Portalapp;
import de.uniluebeck.itm.tr.runtime.portalapp.xml.ProtobufInterface;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import de.uniluebeck.itm.tr.util.UrlUtils;
import eu.wisebed.testbed.api.rs.RSServiceHelper;
import eu.wisebed.testbed.api.rs.v1.ConfidentialReservationData;
import eu.wisebed.testbed.api.rs.v1.RS;
import eu.wisebed.testbed.api.rs.v1.RSExceptionException;
import eu.wisebed.testbed.api.rs.v1.ReservervationNotFoundExceptionException;
import eu.wisebed.testbed.api.wsn.*;
import eu.wisebed.testbed.api.wsn.v22.ExperimentNotRunningException_Exception;
import eu.wisebed.testbed.api.wsn.v22.KeyValuePair;
import eu.wisebed.testbed.api.wsn.v22.SecretReservationKey;
import eu.wisebed.testbed.api.wsn.v22.UnknownReservationIdException_Exception;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

@WebService(
		serviceName = "SessionManagementService",
		targetNamespace = Constants.NAMESPACE_SESSION_MANAGEMENT_SERVICE,
		portName = "SessionManagementPort",
		endpointInterface = Constants.ENDPOINT_INTERFACE_SESSION_MANGEMENT_SERVICE
)
public class SessionManagementServiceImpl implements SessionManagementService {

	/**
	 * Job that is scheduled to clean up resources after a reservations end in time has been reached.
	 */
	private class CleanUpWSNInstanceJob implements Runnable {

		private List<SecretReservationKey> secretReservationKeys;

		public CleanUpWSNInstanceJob(List<SecretReservationKey> secretReservationKeys) {
			this.secretReservationKeys = secretReservationKeys;
		}

		@Override
		public void run() {
			try {
				free(secretReservationKeys);
			} catch (ExperimentNotRunningException_Exception expected) {
				// if user called free before this is expected
			} catch (UnknownReservationIdException_Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * A helper class that holds configuration options.
	 */
	private static class Config {

		/**
		 * The configuration object for the optional protocol buffers interface a client can connect to.
		 */
		private final ProtobufInterface protobufinterface;

		/**
		 * The maximum size of the message delivery queue after which messages to the client are discarded.
		 */
		private final Integer maximumDeliveryQueueSize;

		/**
		 * The sessionManagementEndpoint URL of this Session Management service instance.
		 */
		private final URL sessionManagementEndpointUrl;

		/**
		 * The sessionManagementEndpoint URL of the reservation system that is used for fetching node URNs from the reservation
		 * data. If it is {@code null} then the reservation system is not used.
		 */
		private final URL reservationEndpointUrl;

		/**
		 * The endpoint URL of the authentication and authorization system. If it is {@code null} then the system is not
		 * used and users are assumed to be always authorized.
		 *
		 * TODO currently the SNAA is not used inside SessionManagement or WSN instances for authorization, i.e. there is no authorization currently
		 */
		private URL snaaEndpointUrl;

		/**
		 * The URN prefix that is served by this instance.
		 */
		private final String urnPrefix;

		/**
		 * The base URL (i.e. prefix) that is used as the prefix of a newly created WSN API instance.
		 */
		private final URL wsnInstanceBaseUrl;

		/**
		 * The filename of the file containing the WiseML document that is to delivered when
		 * {@link eu.wisebed.testbed.api.wsn.v22.SessionManagement#getNetwork()} is called.
		 */
		private final String wiseMLFilename;

		public Config(Portalapp config) throws MalformedURLException {
			this.protobufinterface = config.getWebservice().getProtobufinterface();
			this.maximumDeliveryQueueSize = config.getWebservice().getMaximumdeliveryqueuesize();
			this.sessionManagementEndpointUrl = new URL(config.getWebservice().getSessionmanagementendpointurl());
			this.reservationEndpointUrl = config.getWebservice().getReservationendpointurl() == null ? null : new URL(config.getWebservice().getReservationendpointurl());
			this.snaaEndpointUrl = config.getWebservice().getSnaaendpointurl() == null ? null : new URL(config.getWebservice().getSnaaendpointurl());
			this.urnPrefix = config.getWebservice().getUrnprefix();
			this.wsnInstanceBaseUrl = new URL(config.getWebservice().getWsninstancebaseurl().endsWith("/") ? config.getWebservice().getWsninstancebaseurl() : config.getWebservice().getWsninstancebaseurl() + "/");
			this.wiseMLFilename = config.getWebservice().getWisemlfilename();
		}
	}

	/**
	 * The logger for this service.
	 */
	private static final Logger log = LoggerFactory.getLogger(SessionManagementService.class);

	/**
	 * The configuration object of this session management instance.
	 */
	private final Config config;

	/**
	 * An instance of a preconditions checker initiated with the URN prefix of this instance. Used for checking
	 * preconditions of the public Session Management API.
	 */
	private final SessionManagementPreconditions preconditions;

	/**
	 * The server that allows controllers to connect themselves via a Google Protocol Buffers message format to
	 * experiments.
	 */
	private ProtobufControllerServer protobufControllerServer;

	/**
	 * Used to generate secure random IDs to append them to newly created WSN API instances.
	 */
	private final SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	/**
	 * The sessionManagementEndpoint of this Session Management service instance.
	 */
	private Endpoint sessionManagementEndpoint;

	/**
	 * The {@link TestbedRuntime} instance used to communicate with over the overlay
	 */
	private final TestbedRuntime testbedRuntime;

	/**
	 * Holds all currently instantiated WSN API instances that are not yet removed by {@link
	 * de.uniluebeck.itm.tr.runtime.portalapp.SessionManagementService#free(java.util.List)}.
	 */
	private final Map<String, WSNServiceHandle> wsnInstances = new HashMap<String, WSNServiceHandle>();

	public SessionManagementServiceImpl(TestbedRuntime testbedRuntime, Portalapp config) throws MalformedURLException {

		de.uniluebeck.itm.tr.runtime.portalapp.xml.WebService webservice = config.getWebservice();

		checkNotNull(webservice.getUrnprefix());
		checkNotNull(webservice.getSessionmanagementendpointurl());
		checkNotNull(webservice.getWsninstancebaseurl());
		checkNotNull(webservice.getWisemlfilename());
		checkNotNull(testbedRuntime);

		this.testbedRuntime = testbedRuntime;
		this.config = new Config(config);

		this.preconditions = new SessionManagementPreconditions();
		this.preconditions.addServedUrnPrefixes(this.config.urnPrefix);
	}

	@Override
	public void start() throws Exception {

		String bindAllInterfacesUrl = UrlUtils.convertHostToZeros(config.sessionManagementEndpointUrl.toString());

		log.debug("Starting Session Management service ...");
		log.debug("Endpoint URL: {}", config.sessionManagementEndpointUrl.toString());
		log.debug("Binding  URL: {}", bindAllInterfacesUrl);

		sessionManagementEndpoint = Endpoint.publish(bindAllInterfacesUrl, this);

		log.info("Started Session Management service  on {}", bindAllInterfacesUrl);

		if (config.protobufinterface != null) {
			protobufControllerServer = new ProtobufControllerServer(this, config.protobufinterface);
			protobufControllerServer.start();
		}
	}

	@Override
	public void stop() {

		if (protobufControllerServer != null) {
			protobufControllerServer.stop();
		}

		if (sessionManagementEndpoint != null) {
			sessionManagementEndpoint.stop();
			log.info("Stopped Session Management service on {}", config.sessionManagementEndpointUrl);
		}

	}

	public WSNServiceHandle getWsnServiceHandle(String secretReservationKey) {
		return wsnInstances.get(secretReservationKey);
	}

	@Override
	public String getInstance(
			@WebParam(name = "secretReservationKey", targetNamespace = "")
			List<SecretReservationKey> secretReservationKeys,
			@WebParam(name = "controller", targetNamespace = "")
			String controller)
			throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {

		// TODO catch precondition exceptions and throw cleanly defined exception to client
		preconditions.checkGetInstanceArguments(secretReservationKeys, controller);

		if (!"NONE".equals(controller)) {
			boolean canConnect = ControllerHelper.testConnectivity(controller);
			if (!canConnect) {
				throw new RuntimeException("Could not connect to host/port of the given controller endpoint URL. "
						+ "Make sure you're not behind a firewall/NAT and the controller endpoint is already started "
						+ "when calling this method.");
			}
		}

		// extract the one and only relevant secretReservationKey
		String secretReservationKey = secretReservationKeys.get(0).getSecretReservationKey();

		log.debug("SessionManagementServiceImpl.getInstance({})", secretReservationKey);

		// check if wsnInstance already exists and return it if that's the case
		WSNServiceHandle wsnServiceHandleInstance;
		synchronized (wsnInstances) {

			wsnServiceHandleInstance = wsnInstances.get(secretReservationKey);

			if (wsnServiceHandleInstance != null) {

				if (!"NONE".equals(controller)) {
					log.debug("Adding new controller to the list: {}", controller);
					wsnServiceHandleInstance.getWsnService().addController(controller);
				}

				return wsnServiceHandleInstance.getWsnInstanceEndpointUrl().toString();
			}

		}

		// no existing wsnInstance was found, so create new wsnInstance

		// query reservation system for reservation data if reservation system is to be used (i.e.
		// reservationEndpointUrl is not null)
		List<ConfidentialReservationData> confidentialReservationDataList;
		Set<String> reservedNodes = null;
		if (config.reservationEndpointUrl != null) {
			// integrate reservation system
			List<SecretReservationKey> keys = generateSecretReservationKeyList(secretReservationKey);
			confidentialReservationDataList = getReservationDataFromRS(keys);
			reservedNodes = new HashSet<String>();

			// assure that wsnInstance creation doesn't happen before reservation time slot
			assertReservationIntervalMet(confidentialReservationDataList);

			// get reserved nodes
			for (ConfidentialReservationData data : confidentialReservationDataList) {

				// convert all node URNs to lower case so that we can do easy string-based comparisons
				for (String nodeURN : data.getNodeURNs()) {
					reservedNodes.add(nodeURN.toLowerCase());
				}
			}

			// assure that nodes are in TestbedRuntime
			assertNodesInTestbed(reservedNodes, testbedRuntime);

			// assure that all wsn-instances will be removed after expiration time
			for (ConfidentialReservationData data : confidentialReservationDataList) {

				//Creating delay for CleanUpJob
				long delay = data.getTo().toGregorianCalendar().getTimeInMillis() - System.currentTimeMillis();

				//stop and remove invalid instances after their expiration time
				testbedRuntime.getSchedulerService().schedule(
						new CleanUpWSNInstanceJob(keys),
						delay,
						TimeUnit.MILLISECONDS
				);
			}
		} else {
			log.info("Information: No Reservation-System found! All existing nodes will be used.");
		}

		try {

			URL wsnInstanceEndpointUrl = new URL(config.wsnInstanceBaseUrl + secureIdGenerator.getNextId());
			URL controllerEndpointUrl = null;
			if (!"NONE".equals(controller)) {
				controllerEndpointUrl = new URL(controller);
			}

			wsnServiceHandleInstance = WSNServiceHandle.Factory.create(
					secretReservationKey,
					testbedRuntime,
					config.urnPrefix,
					wsnInstanceEndpointUrl,
					controllerEndpointUrl,
					config.wiseMLFilename,
					reservedNodes == null ? null : reservedNodes.toArray(new String[reservedNodes.size()]),
					new ProtobufControllerHelper(config.maximumDeliveryQueueSize),
					protobufControllerServer
			);

		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

		try {
			wsnServiceHandleInstance.start();

		} catch (Exception e) {
			log.error("Exception while creating WSN API wsnInstance!", e);
			// TODO throw declared generic type
			throw new RuntimeException(e);
		}

		synchronized (wsnInstances) {
			wsnInstances.put(secretReservationKey, wsnServiceHandleInstance);
		}

		return wsnServiceHandleInstance.getWsnInstanceEndpointUrl().toString();

	}

	/**
	 * Checks if all reserved nodes are known to {@code testbedRuntime}.
	 *
	 * @param reservedNodes  the set of reserved node URNs
	 * @param testbedRuntime the testbed runtime instance
	 */
	private void assertNodesInTestbed(Set<String> reservedNodes, TestbedRuntime testbedRuntime) {

		for (String node : reservedNodes) {

			boolean isLocal = testbedRuntime.getLocalNodeNames().contains(node);
			boolean isRemote = testbedRuntime.getRoutingTableService().getEntries().keySet().contains(node);

			if (!isLocal && !isRemote) {
				throw new RuntimeException("Node URN " + node + " unknown to testbed runtime environment.");
			}
		}
	}

	@Override
	public void free(
			@WebParam(name = "secretReservationKey", targetNamespace = "")
			List<SecretReservationKey> secretReservationKeyList)
			throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {

		// TODO catch precondition exceptions and throw cleanly defined exception to client
		preconditions.checkFreeArguments(secretReservationKeyList);

		// extract the one and only relevant secret reservation key
		String secretReservationKey = secretReservationKeyList.get(0).getSecretReservationKey();

		log.debug("SessionManagementServiceImpl.free({})", secretReservationKey);

		synchronized (wsnInstances) {

			// search for the existing instance
			WSNServiceHandle wsnServiceHandleInstance = wsnInstances.get(secretReservationKey);

			// stop it if it is existing (it may have been freed before or its lifetime may have been reached)
			if (wsnServiceHandleInstance != null) {
				try {
					wsnServiceHandleInstance.stop();
				} catch (Exception e) {
					log.error("Error while stopping WSN service instance: " + e, e);
				}

				wsnInstances.remove(secretReservationKey);
				log.debug(
						"Removing WSNServiceHandle for WSN service endpoint {}. {} WSN service endpoints running.",
						wsnServiceHandleInstance.getWsnInstanceEndpointUrl(),
						wsnInstances.size()
				);

			} else {
				throw SessionManagementHelper.createExperimentNotRunningException(secretReservationKey);
			}

		}

	}

	@Override
	public String getNetwork() {
		return WiseMLHelper.prettyPrintWiseML(WiseMLHelper.readWiseMLFromFile(config.wiseMLFilename));
	}

	@Override
	public void getConfiguration(
			@WebParam(name = "rsEndpointUrl", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<String> rsEndpointUrl,
			@WebParam(name = "snaaEndpointUrl", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<String> snaaEndpointUrl,
			@WebParam(name = "options", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<List<KeyValuePair>> options) {

		rsEndpointUrl.value = (config.reservationEndpointUrl == null ? "" : config.reservationEndpointUrl.toString());
		snaaEndpointUrl.value = (config.snaaEndpointUrl == null ? "" : config.snaaEndpointUrl.toString());
		// TODO integrate options

	}

	private List<eu.wisebed.testbed.api.rs.v1.SecretReservationKey> convert(
			List<SecretReservationKey> secretReservationKey) {

		List<eu.wisebed.testbed.api.rs.v1.SecretReservationKey> retList =
				new ArrayList<eu.wisebed.testbed.api.rs.v1.SecretReservationKey>(secretReservationKey.size());
		for (SecretReservationKey reservationKey : secretReservationKey) {
			retList.add(convert(reservationKey));
		}
		return retList;
	}

	private eu.wisebed.testbed.api.rs.v1.SecretReservationKey convert(SecretReservationKey reservationKey) {
		eu.wisebed.testbed.api.rs.v1.SecretReservationKey retSRK =
				new eu.wisebed.testbed.api.rs.v1.SecretReservationKey();
		retSRK.setSecretReservationKey(reservationKey.getSecretReservationKey());
		retSRK.setUrnPrefix(reservationKey.getUrnPrefix());
		return retSRK;
	}

	/**
	 * Tries to fetch the reservation data from {@link SessionManagementServiceImpl.Config#reservationEndpointUrl}
	 * and returns the list of reservations.
	 *
	 * @param secretReservationKeys the list of secret reservation keys
	 * @return the list of reservations
	 * @throws UnknownReservationIdException_Exception
	 *          if the reservation could not be found
	 */
	private List<ConfidentialReservationData> getReservationDataFromRS(
			List<SecretReservationKey> secretReservationKeys) throws UnknownReservationIdException_Exception {

		try {
			RS rsService = RSServiceHelper.getRSService(config.reservationEndpointUrl.toString());
			return rsService.getReservation(convert(secretReservationKeys));

		} catch (RSExceptionException e) {
			String msg = "Generic exception occurred in the federated reservation system";
			log.warn(msg + ": " + e, e);
			// TODO replace with more generic exception type
			throw WSNServiceHelper.createUnknownReservationIdException(msg, null, e);
		} catch (ReservervationNotFoundExceptionException e) {
			log.debug("Reservation was not found. Message from RS: {}", e.getMessage());
			throw WSNServiceHelper.createUnknownReservationIdException(e.getMessage(), null, e);
		}

	}

	/**
	 * Checks the reservations' time intervals if they have already started or have already stopped and throws an exception
	 * if that's the case.
	 *
	 * @param reservations the reservations to check
	 * @throws ExperimentNotRunningException_Exception
	 *          if now is not inside the reservations' time interval
	 */
	private void assertReservationIntervalMet(List<ConfidentialReservationData> reservations)
			throws ExperimentNotRunningException_Exception {

		for (ConfidentialReservationData reservation : reservations) {

			DateTime from = new DateTime(reservation.getFrom().toGregorianCalendar());
			DateTime to = new DateTime(reservation.getTo().toGregorianCalendar());

			if (from.isAfterNow()) {
				throw WSNServiceHelper.createExperimentNotRunningException("Reservation time interval for node URNs " +
						Arrays.toString(reservation.getNodeURNs().toArray())
						+ " lies in the future.", null
				);
			}

			if (to.isBeforeNow()) {
				throw WSNServiceHelper.createExperimentNotRunningException("Reservation time interval for node URNs " +
						Arrays.toString(reservation.getNodeURNs().toArray())
						+ " lies in the past.", null
				);
			}

		}

	}

	private List<SecretReservationKey> generateSecretReservationKeyList(String secretReservationKey) {
		List<SecretReservationKey> secretReservationKeyList = new LinkedList<SecretReservationKey>();

		SecretReservationKey key = new SecretReservationKey();
		key.setUrnPrefix(config.urnPrefix);
		key.setSecretReservationKey(secretReservationKey);

		secretReservationKeyList.add(key);

		return secretReservationKeyList;
	}
}
