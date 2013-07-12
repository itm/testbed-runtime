package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.federator.utils.FederationManager;
import de.uniluebeck.itm.tr.federator.utils.FederationManagerFactory;
import de.uniluebeck.itm.util.TimedCache;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.sm.UnknownSecretReservationKeyFault;
import eu.wisebed.api.v3.wsn.WSN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.iwsn.common.SessionManagementHelper.calculateWSNInstanceHash;
import static eu.wisebed.api.v3.WisebedServiceHelper.createSMUnknownSecretReservationKeyFault;

public class WSNFederatorManagerImpl extends AbstractService implements WSNFederatorManager {

	private static final Logger log = LoggerFactory.getLogger(WSNFederatorManagerImpl.class);

	private final WSNFederatorServiceFactory wsnFederatorServiceFactory;

	private final RS rs;

	private final TimedCache<String, WSNFederatorService> instanceCache = new TimedCache<String, WSNFederatorService>();

	private final FederationManager<SessionManagement> smFederationManager;

	private final ExecutorService executorService;

	private final FederationManagerFactory federationManagerFactory;

	@Inject
	public WSNFederatorManagerImpl(final RS rs,
								   final WSNFederatorServiceFactory wsnFederatorServiceFactory,
								   final FederationManager<SessionManagement> smFederationManager,
								   final FederationManagerFactory federationManagerFactory,
								   final ExecutorService executorService) {
		this.rs = checkNotNull(rs);
		this.wsnFederatorServiceFactory = checkNotNull(wsnFederatorServiceFactory);
		this.smFederationManager = checkNotNull(smFederationManager);
		this.federationManagerFactory = checkNotNull(federationManagerFactory);
		this.executorService = checkNotNull(executorService);
	}

	@Override
	protected void doStart() {
		try {
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			for (WSNFederatorService wsnFederatorService : instanceCache.values()) {
				if (wsnFederatorService.isRunning()) {
					wsnFederatorService.stopAndWait();
				}
			}
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public WSNFederatorService getWsnFederatorService(final List<SecretReservationKey> secretReservationKeys)
			throws UnknownSecretReservationKeyFault {

		checkState(isRunning());

		final WSNFederatorService cachedInstance = getInstanceFromCache(secretReservationKeys);

		if (cachedInstance != null) {
			return cachedInstance;
		}

		final Multimap<URI, SecretReservationKey> smToSrkMap = HashMultimap.create();
		for (SecretReservationKey srk : secretReservationKeys) {
			final URI endpointUrl = smFederationManager.getEndpointUrlByUrnPrefix(srk.getUrnPrefix());
			smToSrkMap.put(endpointUrl, srk);
		}

		final Set<Future<GetInstanceCallable.Result>> futures = newHashSet();
		for (URI smEndpointUrl : smToSrkMap.keys()) {
			futures.add(executorService.submit(new GetInstanceCallable(
					smFederationManager.getEndpointByEndpointUrl(smEndpointUrl),
					newArrayList(smToSrkMap.get(smEndpointUrl))
			)
			));
		}

		final Multimap<URI, NodeUrnPrefix> endpointUrlsToUrnPrefixesMap = HashMultimap.create();
		for (final Future<GetInstanceCallable.Result> future : futures) {

			final GetInstanceCallable.Result result;

			try {
				result = future.get();
			} catch (Exception e) {
				throw propagate(e);
			}

			for (SecretReservationKey secretReservationKey : result.secretReservationKey) {
				endpointUrlsToUrnPrefixesMap.put(
						result.federatedWSNInstanceEndpointUrl,
						secretReservationKey.getUrnPrefix()
				);
			}
		}

		final Function<URI, WSN> endpointBuilderFunction = new Function<URI, WSN>() {
			@Override
			public WSN apply(final URI input) {
				return WisebedServiceHelper.getWSNService(input.toString());
			}
		};

		final FederationManager<WSN> wsnFederationManager =
				federationManagerFactory.create(endpointBuilderFunction, endpointUrlsToUrnPrefixesMap);

		final Set<NodeUrn> reservedNodeUrns = retrieveReservedNodeUrns(secretReservationKeys);
		final Set<NodeUrnPrefix> servedNodeUrnPrefixes = newHashSet(
				transform(reservedNodeUrns, new Function<NodeUrn, NodeUrnPrefix>() {
					@Override
					public NodeUrnPrefix apply(final NodeUrn input) {
						return input.getPrefix();
					}
				}
				)
		);

		final WSNFederatorService wsnFederatorService = wsnFederatorServiceFactory.create(
				wsnFederationManager,
				servedNodeUrnPrefixes,
				reservedNodeUrns
		);

		wsnFederatorService.startAndWait();

		return putInstanceInCache(secretReservationKeys, wsnFederatorService);
	}

	@Override
	public WSNFederatorController getWsnFederatorController(final List<SecretReservationKey> secretReservationKeys) {
		checkState(isRunning());
		return getInstanceFromCache(secretReservationKeys).getWsnFederatorController();
	}

	private WSNFederatorService getInstanceFromCache(final List<SecretReservationKey> secretReservationKeys) {
		return instanceCache.get(calculateWSNInstanceHash(secretReservationKeys));
	}

	private WSNFederatorService putInstanceInCache(final List<SecretReservationKey> secretReservationKeys,
												   final WSNFederatorService instance) {
		instanceCache.put(calculateWSNInstanceHash(secretReservationKeys), instance);
		return instance;
	}

	private Set<NodeUrn> retrieveReservedNodeUrns(final List<SecretReservationKey> secretReservationKeys)
			throws UnknownSecretReservationKeyFault {

		try {

			final Set<NodeUrn> reservedNodeUrns = newHashSet();
			for (ConfidentialReservationData reservation : rs.getReservation(secretReservationKeys)) {
				reservedNodeUrns.addAll(reservation.getNodeUrns());
			}
			return reservedNodeUrns;

		} catch (eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault e) {

			throw createSMUnknownSecretReservationKeyFault(
					e.getMessage(),
					e.getFaultInfo().getSecretReservationKey(),
					e
			);

		} catch (Exception e) {
			log.warn("An exception was thrown by the reservation system: " + e, e);
			throw new RuntimeException("An exception was thrown by the reservation system: " + e, e);
		}
	}
}
