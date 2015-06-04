package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.federator.iwsn.async.GetInstanceCallable;
import de.uniluebeck.itm.tr.federator.utils.FederatedEndpoints;
import de.uniluebeck.itm.tr.federator.utils.FederatedEndpointsFactory;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationUnknownException;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.sm.UnknownSecretReservationKeyFault;
import eu.wisebed.api.v3.wsn.WSN;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.common.ReservationHelper.deserialize;
import static eu.wisebed.api.v3.WisebedServiceHelper.createSMUnknownSecretReservationKeyFault;

public class FederatedReservationManager extends AbstractService implements ReservationManager {

	private static final Logger log = LoggerFactory.getLogger(FederatedReservationManager.class);

	private final FederatedEndpoints<SessionManagement> smFederatedEndpoints;

	private final FederatedEndpointsFactory federatedEndpointsFactory;

	private final FederatedReservationFactory federatedReservationFactory;

	private final SchedulerService schedulerService;

	private final RS rs;

	private final Map<Set<SecretReservationKey>, FederatedReservation> reservationMap = newHashMap();

	@Inject
	public FederatedReservationManager(final FederatedEndpoints<SessionManagement> smFederatedEndpoints,
									   final FederatedReservationFactory federatedReservationFactory,
									   final FederatedEndpointsFactory federatedEndpointsFactory,
									   final SchedulerService schedulerService,
									   final RS rs) {
		this.smFederatedEndpoints = smFederatedEndpoints;
		this.federatedReservationFactory = federatedReservationFactory;
		this.federatedEndpointsFactory = federatedEndpointsFactory;
		this.schedulerService = schedulerService;
		this.rs = rs;
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
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	public synchronized FederatedReservation getFederatedReservation(final Set<SecretReservationKey> srkSet)
			throws ReservationUnknownException {
		log.trace("FederatedReservationManager.getFederatedReservation({})", srkSet);
		checkState(isRunning());
		FederatedReservation reservation = getFromCache(srkSet);

		if (reservation == null) {
			reservation = createReservation(srkSet);
			reservation.startAsync().awaitRunning();
			cache(srkSet, reservation);
		}

		return reservation;
	}

	@Override
	public synchronized Reservation getReservation(final Set<SecretReservationKey> srkSet)
			throws ReservationUnknownException {
		log.trace("FederatedReservationManager.getReservation({})", srkSet);
		return getFederatedReservation(srkSet);
	}

	@Override
	public synchronized Reservation getReservation(final String jsonSerializedSecretReservationKeys)
			throws ReservationUnknownException {
		log.trace("FederatedReservationManager.getReservation({})", jsonSerializedSecretReservationKeys);
        if (jsonSerializedSecretReservationKeys == null || jsonSerializedSecretReservationKeys.length() == 0) {
            return null;
        }
		return getFederatedReservation(deserialize(jsonSerializedSecretReservationKeys));
	}

	@Override
	public Optional<Reservation> getReservation(final NodeUrn nodeUrn, final DateTime timestamp) {
		throw new RuntimeException("Not yet implemented!");
	}

	@Override
	public Multimap<Reservation, NodeUrn> getReservationMapping(Set<NodeUrn> nodeUrns, DateTime timestamp) {
		throw new RuntimeException("Not yet implemented!");
	}

	@Override
    public List<Reservation> getReservations(DateTime timestamp) {
        throw new RuntimeException("Not yet implemented!");

    }

    @Override
    public Collection<Reservation> getNonFinalizedReservations() {
        throw new RuntimeException("Not yet implemented!");
    }

    @Nullable
	private FederatedReservation getFromCache(final Set<SecretReservationKey> srkSet) {
		log.trace("FederatedReservationManager.getFromCache({})", srkSet);
		synchronized (reservationMap) {
			return reservationMap.get(srkSet);
		}
	}

	private void cache(final Set<SecretReservationKey> srkSet,
					   final FederatedReservation reservation) {
		log.trace("FederatedReservationManager.cache({}, {})", srkSet, reservation);
		synchronized (reservationMap) {
			reservationMap.put(srkSet, reservation);
		}
	}

	private synchronized FederatedReservation createReservation(final Set<SecretReservationKey> srkSet)
			throws ReservationUnknownException {

		log.trace("FederatedReservationManager.createReservation({})", srkSet);

		try {

			final ArrayList<SecretReservationKey> srkList = newArrayList(srkSet);
			final List<ConfidentialReservationData> reservationDataList = retrieveReservationData(srkList);
			final FederatedEndpoints<WSN> wsnFederatedEndpoints = retrieveFederatedEndpoints(srkList);
			return federatedReservationFactory.create(
					reservationDataList,
					wsnFederatedEndpoints
			);

		} catch (UnknownSecretReservationKeyFault unknownSecretReservationKeyFault) {
			throw new ReservationUnknownException(newHashSet(srkSet));
		}
	}

	private FederatedEndpoints<WSN> retrieveFederatedEndpoints(final List<SecretReservationKey> srkList)
			throws UnknownSecretReservationKeyFault {

		final Multimap<URI, SecretReservationKey> smToSrkMap = HashMultimap.create();
		for (SecretReservationKey srk : srkList) {
			final URI endpointUrl = smFederatedEndpoints.getEndpointUrlByUrnPrefix(srk.getUrnPrefix());
			smToSrkMap.put(endpointUrl, srk);
		}

		// fork calls to getInstance on federated testbeds
		final Multimap<URI, Future<GetInstanceCallable.Result>> futures = HashMultimap.create();
		for (URI smEndpointUrl : smToSrkMap.keys()) {
			for (SecretReservationKey secretReservationKey : smToSrkMap.get(smEndpointUrl)) {
				final GetInstanceCallable callable = new GetInstanceCallable(
						smEndpointUrl,
						smFederatedEndpoints.getEndpointByEndpointUrl(smEndpointUrl),
						newArrayList(secretReservationKey)
				);
				final Future<GetInstanceCallable.Result> future = schedulerService.submit(callable);
				futures.put(smEndpointUrl, future);
			}
		}

		final Multimap<URI, NodeUrnPrefix> endpointUrlsToUrnPrefixesMap = HashMultimap.create();

		// join getInstance call results
		for (URI uri : futures.keys()) {
			for (Future<GetInstanceCallable.Result> future : futures.get(uri)) {

				final GetInstanceCallable.Result result;

				try {
					result = future.get();
				} catch (Exception e) {
					if (e instanceof ExecutionException && e.getCause() instanceof UnknownSecretReservationKeyFault) {
						throw (UnknownSecretReservationKeyFault) e.getCause();
					}
					log.error("Exception while calling getInstance on federated testbed " + uri + ":", e);
					throw propagate(e);
				}

				for (SecretReservationKey secretReservationKey : result.secretReservationKey) {
					endpointUrlsToUrnPrefixesMap.put(
							result.federatedWSNInstanceEndpointUrl,
							secretReservationKey.getUrnPrefix()
					);
				}
			}
		}

		// construct FederatedEndpoints for retrieved WSN API endpoints
		final Function<URI, WSN> endpointBuilderFunction = input -> WisebedServiceHelper.getWSNService(input.toString());

		return federatedEndpointsFactory.create(
				endpointBuilderFunction,
				endpointUrlsToUrnPrefixesMap
		);
	}

	private List<ConfidentialReservationData> retrieveReservationData(
			final List<SecretReservationKey> secretReservationKeys) throws UnknownSecretReservationKeyFault {

		try {
			return rs.getReservation(secretReservationKeys);
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
