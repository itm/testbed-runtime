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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.internal.Nullable;
import com.google.inject.name.Named;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import de.uniluebeck.itm.tr.util.UrlUtils;
import eu.wisebed.testbed.api.rs.RSServiceHelper;
import eu.wisebed.testbed.api.rs.v1.ConfidentialReservationData;
import eu.wisebed.testbed.api.rs.v1.RS;
import eu.wisebed.testbed.api.rs.v1.RSExceptionException;
import eu.wisebed.testbed.api.rs.v1.ReservationNotFoundExceptionException;
import eu.wisebed.testbed.api.wsn.Constants;
import eu.wisebed.testbed.api.wsn.SessionManagementHelper;
import eu.wisebed.testbed.api.wsn.SessionManagementPreconditions;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import eu.wisebed.testbed.api.wsn.v211.ExperimentNotRunningException_Exception;
import eu.wisebed.testbed.api.wsn.v211.SecretReservationKey;
import eu.wisebed.testbed.api.wsn.v211.UnknownReservationIdException_Exception;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;


@Singleton
@WebService(
		serviceName = "SessionManagementService",
		targetNamespace = Constants.NAMESPACE_SESSION_MANAGEMENT_SERVICE,
		portName = "SessionManagementPort",
		endpointInterface = Constants.ENDPOINT_INTERFACE_SESSION_MANGEMENT_SERVICE
)
public class SessionManagementServiceImpl implements SessionManagementService {

	private TestbedRuntime testbedRuntime;

	private Integer maximumDeliveryQueueSize;

	private class CleanUpWSNInstanceJob implements Runnable {

		private List<SecretReservationKey> secretReservationKeys;

		public CleanUpWSNInstanceJob(List<SecretReservationKey> secretReservationKeys) {
			this.secretReservationKeys = secretReservationKeys;
		}

		@Override
		public void run() {
			try {
				free(secretReservationKeys);
			} catch (ExperimentNotRunningException_Exception enre) {
				log.error(enre.getMessage(), enre);
			} catch (UnknownReservationIdException_Exception urie) {
				log.error(urie.getMessage(), urie);
			}
			//To change body of implemented methods use File | Settings | File Templates.
		}
	}

	/**
	 * The logger for this service.
	 */
	private static final Logger log = LoggerFactory.getLogger(SessionManagementService.class);

	/**
	 * The sessionManagementEndpoint of this Session Management service instance.
	 */
	private Endpoint sessionManagementEndpoint;

	/**
	 * The sessionManagementEndpoint URL of this Session Management service instance.
	 */
	private URL sessionManagementEndpointUrl;

	/**
	 * The sessionManagementEndpoint URL of the reservation system that is used for fetching node URNs from the reservation
	 * data. If it is {@code null} then the reservation system is not used.
	 */
	private URL reservationEndpointUrl;

	/**
	 * The URN prefix that is served by this instance.
	 */
	private String urnPrefix;

	/**
	 * Used to generate secure random IDs to append them to newly created WSN API instances.
	 */
	private SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	/**
	 * Holds all currently instantiated WSN API instances that are not yet removed by {@link
	 * de.uniluebeck.itm.tr.runtime.portalapp.SessionManagementService#free(java.util.List)}.
	 */
	private final Map<String, WSNServiceHandle> wsnInstances = new HashMap<String, WSNServiceHandle>();

	/**
	 * The base URL (i.e. prefix) that is prepended to a newly created WSN API instance.
	 */
	private URL wsnInstanceBaseUrl;

	/**
	 * An instance of a preconditions checker initiated with the URN prefix of this instance. Used for checking
	 * preconditions of the public Session Management API.
	 */
	private SessionManagementPreconditions preconditions;

	private String wiseML;

	@Inject
	public SessionManagementServiceImpl(@Named(PortalModule.NAME_URN_PREFIX) String urnPrefix,
										@Named(PortalModule.NAME_SESSION_MANAGEMENT_ENDPOINT_URL)
										String sessionManagementEndpointUrl,
										@Named(PortalModule.NAME_WSN_INSTANCE_BASE_URL) String wsnInstanceBaseUrl,
										@Nullable @Named(PortalModule.NAME_RESERVATION_ENDPOINT_URL)
										String reservationEndpointUrl,
										@Nullable @Named(PortalModule.NAME_MAXIMUM_DELIVERY_QUEUE_SIZE)
										Integer maximumDeliveryQueueSize,
										@Named(PortalModule.NAME_WISEML) String wiseML,
										TestbedRuntime testbedRuntime) throws MalformedURLException {

		checkNotNull(urnPrefix);
		checkNotNull(sessionManagementEndpointUrl);
		checkNotNull(wsnInstanceBaseUrl);
		checkNotNull(wiseML);
		checkNotNull(testbedRuntime);

		this.urnPrefix = urnPrefix;
		this.sessionManagementEndpointUrl = new URL(sessionManagementEndpointUrl);
		this.reservationEndpointUrl = reservationEndpointUrl == null ? null : new URL(reservationEndpointUrl);
		this.wsnInstanceBaseUrl = new URL(
				wsnInstanceBaseUrl.endsWith("/") ? wsnInstanceBaseUrl : wsnInstanceBaseUrl + "/"
		);
		this.maximumDeliveryQueueSize = maximumDeliveryQueueSize;
		this.wiseML = wiseML;
		this.testbedRuntime = testbedRuntime;

		this.preconditions = new SessionManagementPreconditions();
		this.preconditions.addServedUrnPrefixes(urnPrefix);
	}

	@Override
	public void start() throws Exception {

		String bindAllInterfacesUrl = UrlUtils.convertHostToZeros(sessionManagementEndpointUrl.toString());

		log.debug("Starting Session Management service ...");
		log.debug("Endpoint URL: {}", sessionManagementEndpointUrl.toString());
		log.debug("Binding  URL: {}", bindAllInterfacesUrl);

		sessionManagementEndpoint = Endpoint.publish(bindAllInterfacesUrl, this);

		log.info("Started Session Management service  on {}", bindAllInterfacesUrl);
	}

	@Override
	public void stop() {

		if (sessionManagementEndpoint != null) {
			sessionManagementEndpoint.stop();
			log.info("Stopped Session Management service on {}", sessionManagementEndpointUrl);
		}

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

		// extract the one and only relevant secretReservationKey
		String secretReservationKey = secretReservationKeys.get(0).getSecretReservationKey();

		// check if wsnInstance already exists and return it if that's the case
		WSNServiceHandle wsnServiceHandleInstance;
		synchronized (wsnInstances) {

			wsnServiceHandleInstance = wsnInstances.get(secretReservationKey);

			if (wsnServiceHandleInstance != null) {
				log.debug("Adding new controller to the list: {}", controller);
				wsnServiceHandleInstance.getWSNService().addController(controller);

				return wsnServiceHandleInstance.getWsnInstanceEndpointUrl().toString();
			}

		}

		// no existing wsnInstance was found, so create new wsnInstance

		// query reservation system for reservation data if reservation system is to be used (i.e.
		// reservationEndpointUrl is not null)
		List<ConfidentialReservationData> confidentialReservationDataList;
		Set<String> reservedNodes = null;
		if (reservationEndpointUrl != null) {
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
				testbedRuntime.getSchedulerService()
						.schedule(new CleanUpWSNInstanceJob(keys), delay, TimeUnit.MILLISECONDS);
			}
		} else {
			log.info("Information: No Reservation-System found! All existing nodes will be used.");
		}

		try {
			URL wsnInstanceEndpointUrl = new URL(wsnInstanceBaseUrl + secureIdGenerator.getNextId());
			URL controllerEndpointUrl = new URL(controller);

			wsnServiceHandleInstance = WSNServiceModule.Factory.create(
					testbedRuntime,
					urnPrefix,
					wsnInstanceEndpointUrl,
					controllerEndpointUrl,
					wiseML,
					reservedNodes == null ? null : reservedNodes.toArray(new String[reservedNodes.size()]),
					maximumDeliveryQueueSize
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

	//check if all reserved nodes are in testbedruntime

	private void assertNodesInTestbed(Set<String> reservedNodes, TestbedRuntime testbedRuntime)
			throws ExperimentNotRunningException_Exception {
		for (String node : reservedNodes) {
			if (!testbedRuntime.getLocalNodeNames().contains(node) && !testbedRuntime.getRoutingTableService()
					.getEntries().keySet().contains(node)) {
				throw new RuntimeException(
						"Node URN " + node + " in RS-ConfidentialReservationData not in testbed-runtime environment."
				);
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
		// TODO implement run-time generation of WiseML file
		return wiseML;
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
	 * Tries to fetch the reservation data from {@link de.uniluebeck.itm.tr.runtime.portalapp.SessionManagementServiceImpl#reservationEndpointUrl}
	 * and returns the list of reservations.
	 *
	 * @param secretReservationKeys the list of secret reservation keys
	 *
	 * @return the list of reservations
	 *
	 * @throws UnknownReservationIdException_Exception
	 *          if the reservation could not be found
	 */
	private List<ConfidentialReservationData> getReservationDataFromRS(
			List<SecretReservationKey> secretReservationKeys) throws UnknownReservationIdException_Exception {

		try {
			RS rsService = RSServiceHelper.getRSService(reservationEndpointUrl.toString());
			return rsService.getReservation(convert(secretReservationKeys));

		} catch (RSExceptionException e) {
			String msg = "Generic exception occured in the federated reservation system";
			log.warn(msg + ": " + e, e);
			// TODO replace with more generic exception type
			throw WSNServiceHelper.createUnknownReservationIdException(msg, null, e);
		} catch (ReservationNotFoundExceptionException e) {
			log.debug("Reservation was not found. Message from RS: {}", e.getMessage());
			throw WSNServiceHelper.createUnknownReservationIdException(e.getMessage(), null, e);
		}

	}

	/**
	 * Checks the reservations' time intervals if they have already started or have already stopped and throws an exception
	 * if that's the case.
	 *
	 * @param reservations the reservations to check
	 *
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
		key.setUrnPrefix(urnPrefix);
		key.setSecretReservationKey(secretReservationKey);

		secretReservationKeyList.add(key);

		return secretReservationKeyList;
	}
}
