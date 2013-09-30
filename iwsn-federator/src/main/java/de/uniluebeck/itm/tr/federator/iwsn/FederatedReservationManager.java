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
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.sm.UnknownSecretReservationKeyFault;
import eu.wisebed.api.v3.wsn.WSN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.iwsn.portal.ReservationHelper.deserialize;
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

	public FederatedReservation getFederatedReservation(final List<SecretReservationKey> srkList)
			throws ReservationUnknownException {
		checkState(isRunning());
		final FederatedReservation reservation = getFromCache(srkList);
		return reservation == null ? putInCache(srkList, createReservation(srkList)) : reservation;
	}

	@Override
	public Reservation getReservation(final List<SecretReservationKey> srkList)
			throws ReservationUnknownException {
		return getFederatedReservation(srkList);
	}

	@Override
	public Reservation getReservation(final String jsonSerializedSecretReservationKeys)
			throws ReservationUnknownException {
		return getFromCache(deserialize(jsonSerializedSecretReservationKeys));
	}

	@Nullable
	private FederatedReservation getFromCache(final List<SecretReservationKey> srkList) {
		synchronized (reservationMap) {
			return reservationMap.get(newHashSet(srkList));
		}
	}

	private FederatedReservation putInCache(final List<SecretReservationKey> srkList,
											final FederatedReservation reservation) {
		synchronized (reservationMap) {
			reservationMap.put(newHashSet(srkList), reservation);
		}
		return reservation;
	}

	private synchronized FederatedReservation createReservation(final List<SecretReservationKey> srkList)
			throws ReservationUnknownException {

		try {

			final List<ConfidentialReservationData> reservationDataList = retrieveReservationData(srkList);
			final FederatedEndpoints<WSN> wsnFederatedEndpoints = retrieveFederatedEndpoints(srkList);
			final FederatedReservation reservation = federatedReservationFactory.create(
					reservationDataList,
					wsnFederatedEndpoints
			);

			reservation.startAndWait();
			return reservation;

		} catch (UnknownSecretReservationKeyFault unknownSecretReservationKeyFault) {
			throw new ReservationUnknownException(newHashSet(srkList));
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
		final Map<URI, Future<GetInstanceCallable.Result>> futures = newHashMap();
		for (URI smEndpointUrl : smToSrkMap.keys()) {

			final GetInstanceCallable callable = new GetInstanceCallable(
					smEndpointUrl,
					smFederatedEndpoints.getEndpointByEndpointUrl(smEndpointUrl),
					newArrayList(smToSrkMap.get(smEndpointUrl))
			);
			final Future<GetInstanceCallable.Result> future = schedulerService.submit(callable);
			futures.put(smEndpointUrl, future);
		}

		final Multimap<URI, NodeUrnPrefix> endpointUrlsToUrnPrefixesMap = HashMultimap.create();

		// join getInstance call results
		for (Map.Entry<URI, Future<GetInstanceCallable.Result>> entry : futures.entrySet()) {

			final URI uri = entry.getKey();
			final Future<GetInstanceCallable.Result> future = entry.getValue();
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

		// construct FederatedEndpoints for retrieved WSN API endpoints
		final Function<URI, WSN> endpointBuilderFunction = new Function<URI, WSN>() {
			@Override
			public WSN apply(final URI input) {
				return WisebedServiceHelper.getWSNService(input.toString());
			}
		};

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
