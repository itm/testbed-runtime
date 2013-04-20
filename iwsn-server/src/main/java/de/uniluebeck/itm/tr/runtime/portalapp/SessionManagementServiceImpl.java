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

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.netty.handlerstack.HandlerFactoryRegistry;
import de.uniluebeck.itm.netty.handlerstack.protocolcollection.ProtocolCollection;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.SessionManagementPreconditions;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufControllerServer;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufDeliveryManager;
import de.uniluebeck.itm.tr.runtime.wsnapp.UnknownNodeUrnsException;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.rs.ReservationNotFoundFault_Exception;
import eu.wisebed.api.v3.sm.ChannelHandlerDescription;
import eu.wisebed.api.v3.sm.ExperimentNotRunningFault_Exception;
import eu.wisebed.api.v3.sm.UnknownReservationIdFault_Exception;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.iwsn.common.SessionManagementHelper.createExperimentNotRunningException;
import static eu.wisebed.api.v3.WisebedServiceHelper.createUnknownReservationIdException;

public class SessionManagementServiceImpl extends AbstractService implements SessionManagementService {

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
			} catch (ExperimentNotRunningFault_Exception expected) {
				// if user called free before this is expected
			} catch (UnknownReservationIdFault_Exception e) {
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
	@Nonnull
	private final SessionManagementServiceConfig config;

	/**
	 * An instance of a preconditions checker initiated with the URN prefix of this instance. Used for checking
	 * preconditions of the public Session Management API.
	 */
	@Nonnull
	private final SessionManagementPreconditions preconditions;

	/**
	 * Used to generate secure random IDs to append them to newly created WSN API instances.
	 */
	@Nonnull
	private final SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	/**
	 * The {@link TestbedRuntime} instance used to communicate with over the overlay
	 */
	@Nonnull
	private final TestbedRuntime testbedRuntime;

	/**
	 * Holds all currently instantiated WSN API instances that are not yet removed after reservation timeout.
	 */
	@Nonnull
	private final Map<String, WSNServiceHandle> wsnInstances = new HashMap<String, WSNServiceHandle>();

	/**
	 * {@link WSNApp} instance that is used to execute {@link eu.wisebed.api.v3.sm.SessionManagement#areNodesAlive(long,
	 * java.util.List, String)}.
	 */
	@Nonnull
	private final WSNApp wsnApp;

	@Nonnull
	private final Map<String, ScheduledFuture<?>> scheduledCleanUpWSNInstanceJobs =
			new HashMap<String, ScheduledFuture<?>>();

	/**
	 * Helper to deliver messages to controllers. Used for {@link eu.wisebed.api.v3.sm.SessionManagement#areNodesAlive(long,
	 * java.util.List, String)}.
	 * String)}.
	 */
	@Nonnull
	private final DeliveryManager deliveryManager;

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
	protected void doStart() {

		try {

			log.debug("Starting session management service...");

			if (config.getProtobufinterface() != null) {
				protobufControllerServer = new ProtobufControllerServer(this, config.getProtobufinterface());
				protobufControllerServer.startAndWait();
			}

			deliveryManager.startAndWait();

			log.debug("Started session management service!");
			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		try {

			log.debug("Stopping session management service...");

			synchronized (wsnInstances) {

				// copy key set to not cause ConcurrentModificationExceptions
				final Set<String> secretReservationKeys = new HashSet<String>(wsnInstances.keySet());

				for (String secretReservationKey : secretReservationKeys) {
					try {
						freeInternal(secretReservationKey);
					} catch (ExperimentNotRunningFault_Exception e) {
						log.error("ExperimentNotRunningFault while shutting down all WSN instances: " + e, e);
					}
				}
			}

			if (protobufControllerServer != null) {
				try {
					protobufControllerServer.stopAndWait();
				} catch (Exception e) {
					log.error("Exception while shutting down Session Management Protobuf service: {}", e);
				}
			}

			try {
				deliveryManager.stopAndWait();
			} catch (Exception e) {
				log.error("Exception while shutting down delivery manager: {}", e);
			}

			if (scheduler != null) {
				ExecutorUtils.shutdown(scheduler, 10, TimeUnit.SECONDS);
			}

			log.debug("Stopped session management service!");
			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Nullable
	public WSNServiceHandle getWsnServiceHandle(@Nonnull final String secretReservationKey) {
		checkNotNull(secretReservationKey);
		return wsnInstances.get(secretReservationKey);
	}

	@Override
	public List<ChannelHandlerDescription> getSupportedChannelHandlers() {

		HandlerFactoryRegistry handlerFactoryRegistry = new HandlerFactoryRegistry();
		try {
			ProtocolCollection.registerProtocols(handlerFactoryRegistry);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		final List<ChannelHandlerDescription> channelHandlerDescriptions = newArrayList();

		for (HandlerFactoryRegistry.ChannelHandlerDescription handlerDescription : handlerFactoryRegistry
				.getChannelHandlerDescriptions()) {
			channelHandlerDescriptions.add(convert(handlerDescription));
		}

		return channelHandlerDescriptions;
	}

	private ChannelHandlerDescription convert(
			final HandlerFactoryRegistry.ChannelHandlerDescription handlerDescription) {

		ChannelHandlerDescription target = new ChannelHandlerDescription();
		target.setDescription(handlerDescription.getDescription());
		target.setName(handlerDescription.getName());
		for (Map.Entry<String, String> entry : handlerDescription.getConfigurationOptions().entries()) {
			final KeyValuePair keyValuePair = new KeyValuePair();
			keyValuePair.setKey(entry.getKey());
			keyValuePair.setValue(entry.getValue());
			target.getConfigurationOptions().add(keyValuePair);
		}
		return target;
	}

	@Override
	public List<String> getSupportedVirtualLinkFilters() {
		log.debug("WSNServiceImpl.getFilters()");
		return newArrayList();
	}

	@Override
	public String getInstance(List<SecretReservationKey> secretReservationKeys)
			throws ExperimentNotRunningFault_Exception, UnknownReservationIdFault_Exception {

		preconditions.checkGetInstanceArguments(secretReservationKeys);

		// extract the one and only relevant secretReservationKey
		String secretReservationKey = secretReservationKeys.get(0).getSecretReservationKey();

		log.debug("SessionManagementServiceImpl.getInstance({})", secretReservationKey);

		// check if wsnInstance already exists and return it if that's the case
		WSNServiceHandle wsnServiceHandleInstance;
		synchronized (wsnInstances) {

			wsnServiceHandleInstance = wsnInstances.get(secretReservationKey);

			if (wsnServiceHandleInstance != null) {
				return wsnServiceHandleInstance.getWsnInstanceEndpointUrl().toString();
			}

			// no existing wsnInstance was found, so create new wsnInstance

			// query reservation system for reservation data if reservation system is to be used (i.e.
			// reservationEndpointUrl is not null)
			List<ConfidentialReservationData> confidentialReservationDataList;
			String requestingUser = null;
			Set<NodeUrn> reservedNodes = null;
			if (config.getReservationEndpointUrl() != null) {

				// integrate reservation system
				List<SecretReservationKey> keys = generateSecretReservationKeyList(secretReservationKey);
				confidentialReservationDataList = getReservationDataFromRS(keys);
				reservedNodes = newHashSet();

				// since only one secret reservation key is allowed only one piece of confidential reservation data is expected 
				checkArgument(
						confidentialReservationDataList.size() == 1,
						"There must be exactly one secret reservation key as this is a single URN-prefix implementation."
				);

				// assure that wsnInstance creation doesn't happen before reservation time slot
				assertReservationIntervalMet(confidentialReservationDataList);

				ConfidentialReservationData data = confidentialReservationDataList.get(0);

				// convert all node URNs to lower case so that we can do easy string-based comparisons
				for (NodeUrn nodeUrn : data.getNodeUrns()) {
					reservedNodes.add(nodeUrn);
				}


				// assure that nodes are in TestbedRuntime
				assertNodesInTestbed(reservedNodes);

				requestingUser = data.getKeys().get(0).getUsername();

				//Creating delay for CleanUpJob
				long delay = data.getTo().toGregorianCalendar().getTimeInMillis() - System.currentTimeMillis();

				//stop and remove invalid instances after their expiration time
				synchronized (scheduledCleanUpWSNInstanceJobs) {

					final ScheduledFuture<?> schedule = getScheduler().schedule(
							new CleanUpWSNInstanceJob(keys),
							delay,
							TimeUnit.MILLISECONDS
					);

					scheduledCleanUpWSNInstanceJobs.put(secretReservationKey, schedule);
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

			final ImmutableSet<NodeUrn> reservedNodesSet = reservedNodes != null ?
					ImmutableSet.copyOf(reservedNodes) :
					null;

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
					protobufControllerServer,
					config,
					requestingUser
			);

			// start the WSN instance
			try {

				wsnServiceHandleInstance.startAndWait();

			} catch (Exception e) {
				log.error("Exception while creating WSN API wsnInstance: " + e, e);
				throw new RuntimeException(e);
			}

			wsnInstances.put(secretReservationKey, wsnServiceHandleInstance);

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
	private void assertNodesInTestbed(Set<NodeUrn> reservedNodes) {

		for (NodeUrn node : reservedNodes) {

			boolean isLocal = testbedRuntime.getLocalNodeNameManager().getLocalNodeNames().contains(node.toString());
			boolean isRemote = testbedRuntime.getRoutingTableService().getEntries().keySet().contains(node.toString());

			if (!isLocal && !isRemote) {
				throw new RuntimeException("Node URN " + node + " unknown to testbed runtime environment.");
			}
		}
	}

	@Override
	public void areNodesAlive(final long requestId, final List<NodeUrn> nodes, final String controllerEndpointUrl) {

		preconditions.checkAreNodesAliveArguments(nodes, controllerEndpointUrl);

		log.debug("SessionManagementServiceImpl.checkAreNodesAlive({})", nodes);

		this.deliveryManager.addController(controllerEndpointUrl);

		try {
			wsnApp.areNodesAliveSm(new HashSet<NodeUrn>(nodes), new WSNApp.Callback() {
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
	}

	private void free(List<SecretReservationKey> secretReservationKeyList)
			throws ExperimentNotRunningFault_Exception, UnknownReservationIdFault_Exception {

		preconditions.checkFreeArguments(secretReservationKeyList);

		// extract the one and only relevant secret reservation key
		String secretReservationKey = secretReservationKeyList.get(0).getSecretReservationKey();

		log.debug("SessionManagementServiceImpl.free({})", secretReservationKey);

		freeInternal(secretReservationKey);

	}

	private void freeInternal(final String secretReservationKey) throws ExperimentNotRunningFault_Exception {

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
					wsnServiceHandleInstance.stopAndWait();
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


	/**
	 * Tries to fetch the reservation data from {@link de.uniluebeck.itm.tr.runtime.portalapp.SessionManagementServiceConfig#getReservationEndpointUrl()}
	 * and returns the list of reservations.
	 *
	 * @param secretReservationKeys
	 * 		the list of secret reservation keys
	 *
	 * @return the list of reservations
	 *
	 * @throws UnknownReservationIdFault_Exception
	 * 		if the reservation could not be found
	 */
	private List<ConfidentialReservationData> getReservationDataFromRS(
			List<SecretReservationKey> secretReservationKeys) throws UnknownReservationIdFault_Exception {

		try {

			RS rsService = WisebedServiceHelper.getRSService(config.getReservationEndpointUrl().toString());
			return rsService.getReservation((secretReservationKeys));

		} catch (RSFault_Exception e) {
			String msg = "Generic exception occurred in the federated reservation system.";
			log.warn(msg + ": " + e, e);
			throw createUnknownReservationIdException(msg, null, e);
		} catch (ReservationNotFoundFault_Exception e) {
			log.debug("Reservation was not found. Message from RS: {}", e.getMessage());
			throw createUnknownReservationIdException(e.getMessage(), null, e);
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
	 * @throws ExperimentNotRunningFault_Exception
	 * 		if now is not inside the reservations' time interval
	 */
	private void assertReservationIntervalMet(List<ConfidentialReservationData> reservations)
			throws ExperimentNotRunningFault_Exception {

		for (ConfidentialReservationData reservation : reservations) {

			DateTime from = new DateTime(reservation.getFrom().toGregorianCalendar());
			DateTime to = new DateTime(reservation.getTo().toGregorianCalendar());

			if (from.isAfterNow()) {
				throw WisebedServiceHelper.createExperimentNotRunningException(
						"Reservation time interval for node URNs "
								+ Arrays.toString(reservation.getNodeUrns().toArray())
								+ " lies in the future.",
						null
				);
			}

			if (to.isBeforeNow()) {
				throw WisebedServiceHelper.createExperimentNotRunningException(
						"Reservation time interval for node URNs "
								+ Arrays.toString(reservation.getNodeUrns().toArray())
								+ " lies in the past.",
						null
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
