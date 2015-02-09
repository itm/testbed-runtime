package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistenceListener;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceFactory;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.common.ReservationHelper.deserialize;
import static de.uniluebeck.itm.tr.common.ReservationHelper.serialize;

public class ReservationManagerImpl extends AbstractService implements ReservationManager {

    private static final Logger log = LoggerFactory.getLogger(ReservationManager.class);

    private final ReservationFactory reservationFactory;

    private final CommonConfig commonConfig;

    private final Provider<RSPersistence> rsPersistence;

    @VisibleForTesting
    final RSPersistenceListener rsPersistenceListener = new RSPersistenceListener() {
        @Override
        public void onReservationMade(final List<ConfidentialReservationData> crd) {
            log.trace("ReservationManagerImpl.onReservationMade({})", crd);
            cache.put(initReservation(crd));
        }

        @Override
        public void onReservationCancelled(final List<ConfidentialReservationData> crd) {
            // nothing to do, this is handled by the reservation itself
        }

        @Override
        public void onReservationFinalized(final List<ConfidentialReservationData> crd) {
            // nothing should be done here
        }
    };

    private final SchedulerService schedulerService;

    private ReservationCache cache;

    @Inject
    public ReservationManagerImpl(final CommonConfig commonConfig,
                                  final Provider<RSPersistence> rsPersistence,
                                  final ReservationFactory reservationFactory,
                                  final SchedulerServiceFactory schedulerServiceFactory,
                                  final ReservationCache cache) {
        this.commonConfig = checkNotNull(commonConfig);
        this.rsPersistence = checkNotNull(rsPersistence);
        this.reservationFactory = checkNotNull(reservationFactory);
        this.cache = cache;
        this.schedulerService = schedulerServiceFactory.create(-1, "ReservationScheduler");
    }

    @Override
    protected void doStart() {
        log.trace("ReservationManagerImpl.doStart()");
        try {
            schedulerService.startAndWait();
            rsPersistence.get().addListener(rsPersistenceListener);
            initializeNonFinalizedReservations();
            notifyStarted();
        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    @Override
    protected void doStop() {
        log.trace("ReservationManagerImpl.doStop()");
        try {

            for (Reservation reservation : cache.getAll()) {
                reservation.stopAndWait();
            }

            cache.clear();

            rsPersistence.get().removeListener(rsPersistenceListener);
            schedulerService.stopAndWait();
            notifyStopped();

        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    /**
     * Creates instances for all reservations which are not finalized at the moment and need to be reopened
     */
    private void initializeNonFinalizedReservations() {

        log.trace("ReservationManagerImpl.initializeNonFinalizedReservations()");

        try {
            for (ConfidentialReservationData crd : rsPersistence.get().getNonFinalizedReservations()) {
                final String serializedKey = serialize(crd.getSecretReservationKey());
                log.trace("Rebuilding state for {}", serializedKey);

                // initReservation triggers scheduleLifecycleEvents (rebuilding events)
                cache.put(initReservation(newArrayList(crd)));
            }
        } catch (RSFault_Exception e) {
            throw propagate(e);
        }
    }

    @Override
    public Reservation getReservation(final Set<SecretReservationKey> srks)
            throws ReservationUnknownException {

        log.trace("ReservationManagerImpl.getReservation(secretReservationKey={})", srks);

        // filter out additional keys that are not for this testbed (= urn prefix) and make a set so we can match in map
        for (Iterator<SecretReservationKey> it = srks.iterator(); it.hasNext(); ) {
            SecretReservationKey current = it.next();
            if (!commonConfig.getUrnPrefix().equals(current.getUrnPrefix())) {
                it.remove();
            }
        }

        final Set<SecretReservationKey> srkSet = newHashSet(srks);

        Optional<Reservation> res = cache.lookup(srkSet);
        if (res.isPresent()) {
            return res.get();
        }

        Reservation reservation = createAndInitReservation(srkSet);
        cache.put(reservation);
        return reservation;
    }

    /**
     * Only call when synchronized on reservationsBySrk. Creates a new reservation and puts a reference into the map.
     */
    private Reservation createAndInitReservation(final Set<SecretReservationKey> srkSet) {

        checkArgument(srkSet.size() == 1,
                "There must be exactly one secret reservation key as this is a single URN-prefix implementation."
        );

        final SecretReservationKey srk = srkSet.iterator().next();
        final ConfidentialReservationData crd;

        try {
            crd = rsPersistence.get().getReservation(srk);
        } catch (UnknownSecretReservationKeyFault f) {
            throw new ReservationUnknownException(srkSet, f);
        } catch (RSFault_Exception e) {
            throw propagate(e);
        }

        if (crd == null) {
            throw new ReservationUnknownException(srkSet);
        }

        return initReservation(newArrayList(crd));
    }

    /**
     * Only call when synchronized on reservationsBySrk. Creates a new reservation and puts a reference into the map.
     */
    private Reservation initReservation(final List<ConfidentialReservationData> confidentialReservationDataList) {

        final ConfidentialReservationData data = confidentialReservationDataList.get(0);
        log.trace("ReservationManagerImpl.initReservation({})", data);
        final Set<NodeUrn> reservedNodes = newHashSet(data.getNodeUrns());

        final Reservation reservation = reservationFactory.create(
                confidentialReservationDataList,
                data.getSecretReservationKey().getKey(),
                data.getUsername(),
                data.getCancelled(),
                data.getFinalized(),
                schedulerService,
                reservedNodes,
                new Interval(data.getFrom(), data.getTo())
        );

        reservation.startAndWait();

        return reservation;
    }

    @Override
    public Reservation getReservation(final String secretReservationKeysBase64)
            throws ReservationUnknownException {
        if (secretReservationKeysBase64 == null || secretReservationKeysBase64.length() == 0) {
            return null;
        }
        return getReservation(deserialize(secretReservationKeysBase64));
    }

    @Override
    public Optional<Reservation> getReservation(final NodeUrn nodeUrn, final DateTime timestamp) {

        try {

            Optional<Reservation> reservation = cache.lookup(nodeUrn, timestamp);

            if (reservation.isPresent()) {
                return reservation;
            }

            Optional<ConfidentialReservationData> reservationData =
                    rsPersistence.get().getReservation(nodeUrn, timestamp);

            if (!reservationData.isPresent()) {
                return Optional.absent();
            }

            reservation = Optional.of(initReservation(newArrayList(reservationData.get())));
            cache.put(reservation.get());
            return reservation;

        } catch (RSFault_Exception e) {
            throw propagate(e);
        }
    }

    @Override
    public List<Reservation> getReservations(DateTime timestamp) {
        try {

            final List<ConfidentialReservationData> reservationDataList =
                    rsPersistence.get().getReservations(timestamp, timestamp, null, null, null);

            final List<Reservation> reservations = new ArrayList<Reservation>(reservationDataList.size());
            for (ConfidentialReservationData crd : reservationDataList) {
                reservations.add(getReservation(newHashSet(crd.getSecretReservationKey())));
            }
            return reservations;

        } catch (RSFault_Exception e) {
            throw propagate(e);
        }
    }

    @Override
    public Collection<Reservation> getNonFinalizedReservations() {
        try {
            Set<Reservation> reservations = newHashSet();
            List<ConfidentialReservationData> nonFinalizedReservations = rsPersistence.get().getNonFinalizedReservations();
            for (ConfidentialReservationData reservationData : nonFinalizedReservations) {
                // calling getReservation makes sure they're initialized and cached
                Reservation reservation = getReservation(newHashSet(reservationData.getSecretReservationKey()));
                if (!reservation.isFinalized()) {
                    reservations.add(reservation);
                }
            }
            return reservations;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
