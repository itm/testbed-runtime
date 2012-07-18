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

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniluebeck.itm.tr.iwsn.common.SessionManagementHelper.createExperimentNotRunningException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.SessionManagementPreconditions;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufControllerServer;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufDeliveryManager;
import de.uniluebeck.itm.tr.runtime.wsnapp.UnknownNodeUrnsException;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.NetworkUtils;
import de.uniluebeck.itm.tr.util.Preconditions;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import eu.wisebed.api.WisebedServiceHelper;
import eu.wisebed.api.rs.ConfidentialReservationData;
import eu.wisebed.api.rs.RS;
import eu.wisebed.api.rs.RSExceptionException;
import eu.wisebed.api.rs.ReservervationNotFoundExceptionException;
import eu.wisebed.api.sm.ExperimentNotRunningException_Exception;
import eu.wisebed.api.sm.SecretReservationKey;
import eu.wisebed.api.sm.UnknownReservationIdException_Exception;

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
	 * The logger for this service.
	 */
	private static final Logger log = LoggerFactory.getLogger(SessionManagementService.class);

	/**
	 * The configuration object of this session management instance.
	 */
	private final SessionManagementServiceConfig config;

	/**
	 * An instance of a preconditions checker initiated with the URN prefix of this instance. Used for checking
	 * preconditions of the public Session Management API.
	 */
	private final SessionManagementPreconditions preconditions;

	/**
	 * Used to generate secure random IDs to append them to newly created WSN API instances.
	 */
	private final SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	/**
	 * The {@link TestbedRuntime} instance used to communicate with over the overlay
	 */
	private final TestbedRuntime testbedRuntime;

	/**
	 * Holds all currently instantiated WSN API instances that are not yet removed by {@link
	 * de.uniluebeck.itm.tr.runtime.portalapp.SessionManagementService#free(java.util.List)}.
	 */
	private final Map<String, WSNServiceHandle> wsnInstances = new HashMap<String, WSNServiceHandle>();

	/**
	 * {@link WSNApp} instance that is used to execute {@link eu.wisebed.api.sm.SessionManagement#areNodesAlive(java.util.List,
	 * String)}.
	 */
	private final WSNApp wsnApp;

	private final Map<String, ScheduledFuture<?>> scheduledCleanUpWSNInstanceJobs =
			new HashMap<String, ScheduledFuture<?>>();

	/**
	 * Helper to deliver messages to controllers. Used for {@link eu.wisebed.api.sm.SessionManagement#areNodesAlive(java.util.List,
	 * String)}.
	 */
	private DeliveryManager deliveryManager;

	private ScheduledExecutorService scheduler;

	/**
	 * Google Protocol Buffers API
	 */
	private ProtobufControllerServer protobufControllerServer;

	public SessionManagementServiceImpl(final TestbedRuntime testbedRuntime,
										final SessionManagementServiceConfig config,
										final SessionManagementPreconditions preconditions,
										final WSNApp wsnApp,
										final DeliveryManager deliveryManager) throws MalformedURLException {

		checkNotNull(testbedRuntime);
		checkNotNull(config);
		checkNotNull(preconditions);
		checkNotNull(wsnApp);
		checkNotNull(deliveryManager);

		this.testbedRuntime = testbedRuntime;
		this.config = config;
		this.preconditions = preconditions;
		this.wsnApp = wsnApp;
		this.deliveryManager = deliveryManager;
	}

	@Override
	public void start() throws Exception {

		if (config.getProtobufinterface() != null) {
			protobufControllerServer = new ProtobufControllerServer(this, config.getProtobufinterface());
			protobufControllerServer.start();
		}

		deliveryManager.start();
	}

	@Override
	public void stop() {

		synchronized (wsnInstances) {

			// copy key set to not cause ConcurrentModificationExceptions
			final Set<String> secretReservationKeys = new HashSet<String>(wsnInstances.keySet());

			for (String secretReservationKey : secretReservationKeys) {
				try {
					freeInternal(secretReservationKey);
				} catch (ExperimentNotRunningException_Exception e) {
					log.error("ExperimentNotRunningException while shutting down all WSN instances: " + e, e);
				}
			}
		}

		if (protobufControllerServer != null) {
			try {
				protobufControllerServer.stop();
			} catch (Exception e) {
				log.error("Exception while shutting down Session Management Protobuf service: {}", e);
			}
		}

		if (deliveryManager != null) {
			try {
				deliveryManager.stop();
			} catch (Exception e) {
				log.error("Exception while shutting down delivery manager: {}", e);
			}
		}

		if (scheduler != null) {
			ExecutorUtils.shutdown(scheduler, 10, TimeUnit.SECONDS);
		}
	}

	@Nullable
	public WSNServiceHandle getWsnServiceHandle(@Nonnull final String secretReservationKey) {
		checkNotNull(secretReservationKey);
		return wsnInstances.get(secretReservationKey);
	}

	@Override
	public String getInstance(List<SecretReservationKey> secretReservationKeys, String controller)
			throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {

		preconditions.checkGetInstanceArguments(secretReservationKeys, controller);

		// check if controller endpoint URL is a valid URL and connectivity is given
		// (i.e. endpoint is not behind a NAT or firewalled)
		try {

			// the user may pass NONE to indicate the wish to not add a controller endpoint URL for now
			if (!"NONE".equals(controller)) {
				new URL(controller);
				try {
					NetworkUtils.checkConnectivity(controller);
				} catch (Exception e) {
					throw new RuntimeException("The testbed backend system could not connect to host/port of the given "
							+ "controller endpoint URL: \"" + controller + "\". Please make sure: \n"
							+ " 1) your host is not behind a firewall or the firewall is configured to allow incoming connections\n"
							+ " 2) your host is not behind a Network Address Translation (NAT) system or the NAT system is configured to forward incoming connections\n"
							+ " 3) the domain in the endpoint URL can be resolved to an IP address and\n"
							+ " 4) the Controller endpoint Web service is already started.\n"
							+ "\n"
							+ "The testbed backend system needs an implementation of the Wisebed APIs Controller "
							+ "Web service to run on the client side. It uses this as a feedback channel to deliver "
							+ "sensor node outputs to the client application.\n"
							+ "\n"
							+ "Please note: If this testbed runs the unofficial Protocol buffers based API you might "
							+ "try to use this method to connect to the testbed as it doesn't require a feedback "
							+ "channel but delivers the node output using the TCP connection initiated by the client."
					);
				}
			}

		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
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

			// no existing wsnInstance was found, so create new wsnInstance

			// query reservation system for reservation data if reservation system is to be used (i.e.
			// reservationEndpointUrl is not null)
			List<ConfidentialReservationData> confidentialReservationDataList;
			Set<String> reservedNodes = null;
			if (config.getReservationEndpointUrl() != null) {

				// integrate reservation system
				List<SecretReservationKey> keys = generateSecretReservationKeyList(secretReservationKey);
				confidentialReservationDataList = getReservationDataFromRS(keys);
				reservedNodes = new HashSet<String>();
				
				// since only one secret reservation key is allowed only one piece of confidential reservation data is expected 
				com.google.common.base.Preconditions.checkArgument(confidentialReservationDataList.size() == 1,
						"There must be exactly one secret reservation key as this is a single URN-prefix implementation."
				);

				// assure that wsnInstance creation doesn't happen before reservation time slot
				assertReservationIntervalMet(confidentialReservationDataList);

				ConfidentialReservationData data = confidentialReservationDataList.get(0);

				// convert all node URNs to lower case so that we can do easy string-based comparisons
				for (String nodeURN : data.getNodeURNs()) {
					reservedNodes.add(nodeURN.toLowerCase());
				}
				

				// assure that nodes are in TestbedRuntime
				assertNodesInTestbed(reservedNodes);


				//Creating delay for CleanUpJob
				long delay = data.getTo().toGregorianCalendar().getTimeInMillis() - System.currentTimeMillis();

				//stop and remove invalid instances after their expiration time
				synchronized (scheduledCleanUpWSNInstanceJobs) {

					final ScheduledFuture<?> schedule = getScheduler().schedule(
							new CleanUpWSNInstanceJob(keys),
							delay,
							TimeUnit.MILLISECONDS
					);

					scheduledCleanUpWSNInstanceJobs.put(secretReservationKey,schedule);
				}
				

			} else {
				log.info("Information: No reservation system found! All existing nodes will be used.");
			}

			URL wsnInstanceEndpointUrl;
			try {
				wsnInstanceEndpointUrl = new URL(config.getWsnInstanceBaseUrl() + secureIdGenerator.getNextId());
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}

			final ImmutableSet<String> reservedNodesSet = reservedNodes == null ?
					null :
					ImmutableSet.<String>builder().add(reservedNodes.toArray(new String[reservedNodes.size()])).build();

			final ProtobufDeliveryManager protobufDeliveryManager =
					new ProtobufDeliveryManager(config.getMaximumDeliveryQueueSize());

			wsnServiceHandleInstance = WSNServiceHandleFactory.create(
					secretReservationKey,
					testbedRuntime,
					config.getUrnPrefix(),
					wsnInstanceEndpointUrl,
					config.getWiseMLFilename(),
					reservedNodesSet,
					protobufDeliveryManager,
					protobufControllerServer
			);

			// start the WSN instance
			try {

				wsnServiceHandleInstance.start();

			} catch (Exception e) {
				log.error("Exception while creating WSN API wsnInstance: " + e, e);
				throw new RuntimeException(e);
			}

			wsnInstances.put(secretReservationKey, wsnServiceHandleInstance);

			if (!"NONE".equals(controller)) {
				wsnServiceHandleInstance.getWsnService().addController(controller);
			}

			return wsnServiceHandleInstance.getWsnInstanceEndpointUrl().toString();

		}

	}

	private ScheduledExecutorService getScheduler() {
		if (scheduler == null) {
			scheduler = Executors.newScheduledThreadPool(
					1,
					new ThreadFactoryBuilder().setNameFormat("SessionManagement-Thread %d").build()
			);
		}
		return scheduler;
	}

	/**
	 * Checks if all reserved nodes are known to {@code testbedRuntime}.
	 *
	 * @param reservedNodes
	 * 		the set of reserved node URNs
	 */
	private void assertNodesInTestbed(Set<String> reservedNodes) {

		for (String node : reservedNodes) {

			boolean isLocal = testbedRuntime.getLocalNodeNameManager().getLocalNodeNames().contains(node);
			boolean isRemote = testbedRuntime.getRoutingTableService().getEntries().keySet().contains(node);

			if (!isLocal && !isRemote) {
				throw new RuntimeException("Node URN " + node + " unknown to testbed runtime environment.");
			}
		}
	}

	@Override
	public String areNodesAlive(final List<String> nodes, final String controllerEndpointUrl) {

		preconditions.checkAreNodesAliveArguments(nodes, controllerEndpointUrl);

		log.debug("SessionManagementServiceImpl.checkAreNodesAlive({})", nodes);

		this.deliveryManager.addController(controllerEndpointUrl);
		final String requestId = secureIdGenerator.getNextId();

		try {
			wsnApp.areNodesAliveSm(new HashSet<String>(nodes), new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {
					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
				}

				@Override
				public void failure(Exception e) {
					deliveryManager.receiveFailureStatusMessages(nodes, requestId, e, -1);
				}
			}
			);
		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
			deliveryManager.removeController(controllerEndpointUrl);
		}

		getScheduler().schedule(
				new Runnable() {
					@Override
					public void run() {
						deliveryManager.removeController(controllerEndpointUrl);
					}
				}, 10, TimeUnit.SECONDS
		);

		return requestId;

	}

	@Override
	public void free(List<SecretReservationKey> secretReservationKeyList)
			throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {

		preconditions.checkFreeArguments(secretReservationKeyList);

		// extract the one and only relevant secret reservation key
		String secretReservationKey = secretReservationKeyList.get(0).getSecretReservationKey();

		log.debug("SessionManagementServiceImpl.free({})", secretReservationKey);

		freeInternal(secretReservationKey);

	}

	private void freeInternal(final String secretReservationKey) throws ExperimentNotRunningException_Exception {

		synchronized (scheduledCleanUpWSNInstanceJobs) {

			final ScheduledFuture<?> schedule = scheduledCleanUpWSNInstanceJobs.get(secretReservationKey);

			if (schedule != null) {
				schedule.cancel(true);
			}
		}

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
				throw createExperimentNotRunningException(secretReservationKey);
			}

		}
	}

	private List<eu.wisebed.api.rs.SecretReservationKey> convert(
			List<SecretReservationKey> secretReservationKey) {

		List<eu.wisebed.api.rs.SecretReservationKey> retList =
				new ArrayList<eu.wisebed.api.rs.SecretReservationKey>(secretReservationKey.size());
		for (SecretReservationKey reservationKey : secretReservationKey) {
			retList.add(convert(reservationKey));
		}
		return retList;
	}

	private eu.wisebed.api.rs.SecretReservationKey convert(SecretReservationKey reservationKey) {
		eu.wisebed.api.rs.SecretReservationKey retSRK =
				new eu.wisebed.api.rs.SecretReservationKey();
		retSRK.setSecretReservationKey(reservationKey.getSecretReservationKey());
		retSRK.setUrnPrefix(reservationKey.getUrnPrefix());
		return retSRK;
	}

	/**
	 * Tries to fetch the reservation data from {@link de.uniluebeck.itm.tr.runtime.portalapp.SessionManagementServiceConfig#getReservationEndpointUrl()}
	 * and returns the list of reservations.
	 *
	 * @param secretReservationKeys
	 * 		the list of secret reservation keys
	 *
	 * @return the list of reservations
	 *
	 * @throws UnknownReservationIdException_Exception
	 * 		if the reservation could not be found
	 */
	private List<ConfidentialReservationData> getReservationDataFromRS(
			List<SecretReservationKey> secretReservationKeys) throws UnknownReservationIdException_Exception {

		try {

			RS rsService = WisebedServiceHelper.getRSService(config.getReservationEndpointUrl().toString());
			return rsService.getReservation(convert(secretReservationKeys));

		} catch (RSExceptionException e) {
			String msg = "Generic exception occurred in the federated reservation system.";
			log.warn(msg + ": " + e, e);
			throw WisebedServiceHelper.createUnknownReservationIdException(msg, null, e);
		} catch (ReservervationNotFoundExceptionException e) {
			log.debug("Reservation was not found. Message from RS: {}", e.getMessage());
			throw WisebedServiceHelper.createUnknownReservationIdException(e.getMessage(), null, e);
		}

	}

	/**
	 * Checks the reservations' time intervals if they have already started or have already stopped and throws an
	 * exception
	 * if that's the case.
	 *
	 * @param reservations
	 * 		the reservations to check
	 *
	 * @throws ExperimentNotRunningException_Exception
	 * 		if now is not inside the reservations' time interval
	 */
	private void assertReservationIntervalMet(List<ConfidentialReservationData> reservations)
			throws ExperimentNotRunningException_Exception {

		for (ConfidentialReservationData reservation : reservations) {

			DateTime from = new DateTime(reservation.getFrom().toGregorianCalendar());
			DateTime to = new DateTime(reservation.getTo().toGregorianCalendar());

			if (from.isAfterNow()) {
				throw WisebedServiceHelper
						.createExperimentNotRunningException("Reservation time interval for node URNs " +
								Arrays.toString(reservation.getNodeURNs().toArray())
								+ " lies in the future.", null
						);
			}

			if (to.isBeforeNow()) {
				throw WisebedServiceHelper
						.createExperimentNotRunningException("Reservation time interval for node URNs " +
								Arrays.toString(reservation.getNodeURNs().toArray())
								+ " lies in the past.", null
						);
			}

		}

	}

	private List<SecretReservationKey> generateSecretReservationKeyList(String secretReservationKey) {

		List<SecretReservationKey> secretReservationKeyList = new LinkedList<SecretReservationKey>();

		SecretReservationKey key = new SecretReservationKey();
		key.setUrnPrefix(config.getUrnPrefix());
		key.setSecretReservationKey(secretReservationKey);

		secretReservationKeyList.add(key);

		return secretReservationKeyList;
	}
}
