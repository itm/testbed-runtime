/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.common.PreconditionsFactory;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.common.SessionManagementPreconditions;
import de.uniluebeck.itm.tr.federator.iwsn.async.GetSupportedChannelHandlersCallable;
import de.uniluebeck.itm.tr.federator.iwsn.async.SMAreNodesAliveCallable;
import de.uniluebeck.itm.tr.federator.utils.FederatedEndpoints;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationUnknownException;
import de.uniluebeck.itm.util.Tuple;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.sm.ChannelHandlerDescription;
import eu.wisebed.api.v3.sm.NodeConnectionStatus;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.sm.UnknownSecretReservationKeyFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebService;
import javax.xml.ws.Holder;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static eu.wisebed.wiseml.WiseMLHelper.serialize;

@WebService(
		name = "SessionManagement",
		endpointInterface = "eu.wisebed.api.v3.sm.SessionManagement",
		portName = "SessionManagementPort",
		serviceName = "SessionManagementService",
		targetNamespace = "http://wisebed.eu/api/v3/sm"
)
public class SessionManagementFederatorServiceImpl extends AbstractService
		implements SessionManagementFederatorService {

	/**
	 * The logger instance for this Session Management federator instance.
	 */
	private static final Logger log = LoggerFactory.getLogger(SessionManagementFederatorServiceImpl.class);

	/**
	 * The {@link java.util.concurrent.ExecutorService} instance that is used to run jobs in parallel.
	 */
	private final ExecutorService executorService;

	private final ServicePublisher servicePublisher;

	private final IWSNFederatorServiceConfig config;

	private final FederatedEndpoints<SessionManagement> federatedEndpoints;

	private final FederatedReservationManager reservationManager;

	private final PreconditionsFactory preconditionsFactory;

	private final ServedNodeUrnPrefixesProvider servedNodeUrnPrefixesProvider;

	private final ServedNodeUrnsProvider servedNodeUrnsProvider;

	private final SessionManagementWisemlProvider wisemlProvider;

	/**
	 * Preconditions instance to check method arguments sent by user.
	 */
	private SessionManagementPreconditions preconditions;

	private ServicePublisherService jaxWsService;

	@Inject
	public SessionManagementFederatorServiceImpl(
			final FederatedEndpoints<SessionManagement> federatedEndpoints,
			final PreconditionsFactory preconditionsFactory,
			final IWSNFederatorServiceConfig config,
			final ServicePublisher servicePublisher,
			final ExecutorService executorService,
			final FederatedReservationManager reservationManager,
			final ServedNodeUrnPrefixesProvider servedNodeUrnPrefixesProvider,
			final ServedNodeUrnsProvider servedNodeUrnsProvider,
			final SessionManagementWisemlProvider wisemlProvider) {
		this.preconditionsFactory = checkNotNull(preconditionsFactory);
		this.servedNodeUrnPrefixesProvider = checkNotNull(servedNodeUrnPrefixesProvider);
		this.servedNodeUrnsProvider = checkNotNull(servedNodeUrnsProvider);
		this.wisemlProvider = checkNotNull(wisemlProvider);
		this.federatedEndpoints = checkNotNull(federatedEndpoints);
		this.config = checkNotNull(config);
		this.servicePublisher = checkNotNull(servicePublisher);
		this.executorService = checkNotNull(executorService);
		this.reservationManager = checkNotNull(reservationManager);
	}

	@Override
	protected void doStart() {
		log.trace("SessionManagementFederatorServiceImpl.doStart()");
		try {

			jaxWsService = servicePublisher.createJaxWsService(config.getFederatorSmEndpointUri().getPath(), this);
			jaxWsService.startAndWait();

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.trace("SessionManagementFederatorServiceImpl.doStop()");
		try {

			if (jaxWsService != null && jaxWsService.isRunning()) {
				jaxWsService.stopAndWait();
			}

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	private synchronized SessionManagementPreconditions getPreconditions() {
		if (preconditions == null) {
			preconditions = preconditionsFactory.createSessionManagementPreconditions(
					servedNodeUrnPrefixesProvider.get(),
					servedNodeUrnsProvider.get()
			);
		}
		return preconditions;
	}

	@Override
	public List<ChannelHandlerDescription> getSupportedChannelHandlers() {

		checkState(isRunning());

		log.debug("getSupportedChannelHandlers() called...");

		final ImmutableSet<FederatedEndpoints.Entry<SessionManagement>> entries = federatedEndpoints.getEntries();
		final Map<FederatedEndpoints.Entry<SessionManagement>, Future<List<ChannelHandlerDescription>>>
				entryToResultMapping = Maps.newHashMap();

		// fork calls to endpoints
		for (final FederatedEndpoints.Entry<SessionManagement> entry : entries) {
			final Future<List<ChannelHandlerDescription>> future = executorService.submit(
					new GetSupportedChannelHandlersCallable(entry.endpoint)
			);
			entryToResultMapping.put(entry, future);
		}

		final Set<ChannelHandlerDescription> commonHandlers = newTreeSet(CHANNEL_HANDLER_DESCRIPTION_COMPARATOR);

		for (Map.Entry<FederatedEndpoints.Entry<SessionManagement>, Future<List<ChannelHandlerDescription>>> outerEntry : entryToResultMapping
				.entrySet()) {

			try {

				List<ChannelHandlerDescription> outerChannelHandlers = outerEntry.getValue().get();

				for (ChannelHandlerDescription outerChannelHandler : outerChannelHandlers) {

					boolean containedInAllOthers = true;

					for (Map.Entry<FederatedEndpoints.Entry<SessionManagement>, Future<List<ChannelHandlerDescription>>> innerEntry : entryToResultMapping
							.entrySet()) {

						if (innerEntry != outerEntry) {

							boolean outerContainedInInnerEntry = false;

							final List<ChannelHandlerDescription> innerChannelHandlers = innerEntry.getValue().get();
							for (ChannelHandlerDescription innerChannelHandler : innerChannelHandlers) {
								if (equals(outerChannelHandler, innerChannelHandler)) {
									outerContainedInInnerEntry = true;
									break;
								}
							}

							if (!outerContainedInInnerEntry) {
								containedInAllOthers = false;
								break;
							}
						}
					}

					if (containedInAllOthers) {
						commonHandlers.add(outerChannelHandler);
					}
				}

			} catch (Exception e) {
				log.error("Error while calling getFilters() on federated WSN endpoint \"{}\". Ignoring this endpoint.",
						outerEntry.getKey()
				);
			}
		}

		return newArrayList(commonHandlers);
	}

	@Override
	public List<String> getSupportedVirtualLinkFilters() {

		checkState(isRunning());

		ImmutableSet<URI> endpointUrls = federatedEndpoints.getEndpointUrls();
		Map<URI, Future<ImmutableSet<String>>> endpointUrlToResultsMapping = Maps.newHashMap();

		// fork calls to endpoints
		log.debug("Invoking getFilters() on {}", endpointUrls);
		for (final URI endpointUrl : endpointUrls) {
			Future<ImmutableSet<String>> future = executorService.submit(new Callable<ImmutableSet<String>>() {
				@Override
				public ImmutableSet<String> call() throws Exception {
					SessionManagement endpoint = federatedEndpoints.getEndpointByEndpointUrl(endpointUrl);
					return ImmutableSet.copyOf(endpoint.getSupportedVirtualLinkFilters());
				}
			}
			);
			endpointUrlToResultsMapping.put(endpointUrl, future);
		}

		// join results from endpoints
		ImmutableSet<String> intersectedFilters = null;
		for (Map.Entry<URI, Future<ImmutableSet<String>>> entry : endpointUrlToResultsMapping.entrySet()) {

			try {

				ImmutableSet<String> endpointFilters = entry.getValue().get();

				if (intersectedFilters == null) {
					intersectedFilters = endpointFilters;
				} else {
					intersectedFilters = ImmutableSet.copyOf(Sets.intersection(intersectedFilters, endpointFilters));
				}

			} catch (Exception e) {
				log.error("Error while calling getFilters() on federated WSN endpoint \"{}\". Ignoring this endpoint.",
						entry.getKey()
				);
			}
		}

		return Lists.newArrayList(intersectedFilters);
	}

	@Override
	public String getVersion() {
		checkState(isRunning());
		return "3.0";
	}

	private static final Comparator<ChannelHandlerDescription> CHANNEL_HANDLER_DESCRIPTION_COMPARATOR =
			new Comparator<ChannelHandlerDescription>() {
				@Override
				public int compare(final ChannelHandlerDescription o1, final ChannelHandlerDescription o2) {
					return SessionManagementFederatorServiceImpl.equals(o1, o2) ? 0 : -1;
				}
			};

	private static boolean equals(final ChannelHandlerDescription outerChannelHandler,
								  final ChannelHandlerDescription innerChannelHandler) {

		if (!outerChannelHandler.getName().equals(innerChannelHandler.getName())) {
			return false;
		}

		Set<String> outerConfigurationKeys = newHashSet();
		Set<String> innerConfigurationKeys = newHashSet();

		for (KeyValuePair keyValuePair : outerChannelHandler.getConfigurationOptions()) {
			outerConfigurationKeys.add(keyValuePair.getKey());
		}

		for (KeyValuePair keyValuePair : innerChannelHandler.getConfigurationOptions()) {
			innerConfigurationKeys.add(keyValuePair.getKey());
		}

		return Sets.symmetricDifference(outerConfigurationKeys, innerConfigurationKeys).size() == 0;
	}

	@Override
	public String getInstance(final List<SecretReservationKey> srks)
			throws UnknownSecretReservationKeyFault {

		log.trace("SessionManagementFederatorServiceImpl.getInstance({})", srks);
		checkState(isRunning());

		getPreconditions().checkGetInstanceArguments(srks);
		final FederatedReservation reservation;
		try {
			reservation = reservationManager.getFederatedReservation(newHashSet(srks));
		} catch (ReservationUnknownException e) {
			final String msg = "Reservation " + Joiner.on(",").join(srks) + " unknown!";
			final eu.wisebed.api.v3.common.UnknownSecretReservationKeyFault faultInfo =
					new eu.wisebed.api.v3.common.UnknownSecretReservationKeyFault();
			faultInfo.setMessage(e.getMessage());
			faultInfo.setSecretReservationKey(e.getSecretReservationKeys().iterator().next());
			throw new UnknownSecretReservationKeyFault(msg, faultInfo);
		}
		return reservation.getWsnFederatorService().getEndpointUri().toString();
	}

	@Override
	public List<NodeConnectionStatus> areNodesConnected(final List<NodeUrn> nodeUrns) {

		log.trace("SessionManagementFederatorServiceImpl.areNodesConnected({})", nodeUrns);
		checkState(isRunning());

		// fork areNodesAlive() calls to federated testbeds
		final Map<URI, Set<NodeUrn>> sessionManagementEndpointUrlToNodeUrnMapping =
				createSessionManagementEndpointUrlToNodeUrnMapping(nodeUrns);

		final List<Tuple<Set<NodeUrn>, Future<List<NodeConnectionStatus>>>> futures = newArrayList();

		for (Map.Entry<URI, Set<NodeUrn>> entry : sessionManagementEndpointUrlToNodeUrnMapping.entrySet()) {

			final URI nodeUrnSubsetSessionManagementEndpointUrl = entry.getKey();
			final Set<NodeUrn> nodeUrnSubset = entry.getValue();

			final SMAreNodesAliveCallable callable = new SMAreNodesAliveCallable(
					federatedEndpoints.getEndpointByEndpointUrl(nodeUrnSubsetSessionManagementEndpointUrl),
					newArrayList(nodeUrnSubset)
			);

			futures.add(new Tuple<Set<NodeUrn>, Future<List<NodeConnectionStatus>>>(
					nodeUrnSubset,
					executorService.submit(callable)
			)
			);
		}

		final List<NodeConnectionStatus> statusList = newArrayList();

		for (Tuple<Set<NodeUrn>, Future<List<NodeConnectionStatus>>> tuple : futures) {
			try {
				statusList.addAll(tuple.getSecond().get(30, TimeUnit.SECONDS));
			} catch (Exception e) {
				for (NodeUrn nodeUrn : tuple.getFirst()) {
					final NodeConnectionStatus status = new NodeConnectionStatus();
					status.setNodeUrn(nodeUrn);
					status.setConnected(false);
					statusList.add(status);
				}
			}
		}

		return statusList;
	}

	@Override
	public void getConfiguration(final Holder<String> rsEndpointUrl,
								 final Holder<String> snaaEndpointUrl,
								 final Holder<List<NodeUrnPrefix>> servedUrnPrefixes,
								 final Holder<List<KeyValuePair>> options) {

		log.trace("SessionManagementFederatorServiceImpl.getConfiguration()");
		checkState(isRunning());

		rsEndpointUrl.value = config.getFederatorRsEndpointUri().toString();
		snaaEndpointUrl.value = config.getFederatorSnaaEndpointUri().toString();
		servedUrnPrefixes.value = newArrayList();
		for (Set<NodeUrnPrefix> nodeUrnPrefixes : config.getFederates().values()) {
			servedUrnPrefixes.value.addAll(nodeUrnPrefixes);
		}
	}

	/**
	 * Returns a {@link Map} that maps Session Management Endpoint URLs to the subset of node URNs of {@code nodeUrns} for
	 * which each map entry is responsible.
	 *
	 * @param nodeUrns
	 * 		the node URNs for which to create the mapping
	 *
	 * @return see above
	 */
	private Map<URI, Set<NodeUrn>> createSessionManagementEndpointUrlToNodeUrnMapping(final List<NodeUrn> nodeUrns) {

		final Map<URI, Set<NodeUrn>> map = Maps.newHashMap();

		final ImmutableSet<FederatedEndpoints.Entry<SessionManagement>> entries = federatedEndpoints.getEntries();
		for (FederatedEndpoints.Entry<SessionManagement> entry : entries) {
			for (NodeUrn nodeUrn : nodeUrns) {
				for (NodeUrnPrefix urnPrefix : entry.urnPrefixes) {
					if (nodeUrn.belongsTo(urnPrefix)) {
						Set<NodeUrn> remoteNodeUrnsToInclude = map.get(entry.endpointUrl);
						if (remoteNodeUrnsToInclude == null) {
							remoteNodeUrnsToInclude = Sets.newHashSet();
							map.put(entry.endpointUrl, remoteNodeUrnsToInclude);
						}
						remoteNodeUrnsToInclude.add(nodeUrn);
					}
				}
			}
		}
		return map;
	}

	@Override
	public String getNetwork() {
		log.trace("SessionManagementFederatorServiceImpl.getNetwork()");
		checkState(isRunning());
		return serialize(wisemlProvider.get());

	}
}
