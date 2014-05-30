package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import de.uniluebeck.itm.tr.common.ReservationHelper;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.common.events.ReservationDeletedEvent;
import de.uniluebeck.itm.tr.common.events.ReservationMadeEvent;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceFactory;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.common.ReservationHelper.deserialize;
import static org.joda.time.DateTime.now;

public class ReservationManagerImpl extends AbstractService implements ReservationManager {

	private static final Logger log = LoggerFactory.getLogger(ReservationManager.class);

	private static final TimeUnit MS = TimeUnit.MILLISECONDS;

	private final Provider<RS> rs;

	private final DeviceDBService deviceDBService;

	private final ReservationFactory reservationFactory;

	private final Map<Set<SecretReservationKey>, Reservation> reservationMap = newHashMap();

	private final SchedulerService schedulerService;

	private final CommonConfig commonConfig;

	private final Provider<RSPersistence> rsPersistence;

	private final PortalEventBus portalEventBus;

	@Inject
	public ReservationManagerImpl(final CommonConfig commonConfig,
								  final Provider<RS> rs,
								  final Provider<RSPersistence> rsPersistence,
								  final DeviceDBService deviceDBService,
								  final ReservationFactory reservationFactory,
								  final SchedulerServiceFactory schedulerServiceFactory,
								  final PortalEventBus portalEventBus) {
		this.commonConfig = checkNotNull(commonConfig);
		this.rs = checkNotNull(rs);
		this.rsPersistence = checkNotNull(rsPersistence);
		this.deviceDBService = checkNotNull(deviceDBService);
		this.reservationFactory = checkNotNull(reservationFactory);
		this.schedulerService = schedulerServiceFactory.create(-1, "ReservationManager");
		this.portalEventBus = checkNotNull(portalEventBus);
	}

	@Override
	protected void doStart() {
		log.trace("ReservationManagerImpl.doStart()");
		try {
			portalEventBus.register(this);
			schedulerService.startAndWait();
			replayStartedEvents();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.trace("ReservationManagerImpl.doStop()");
		try {
			for (Reservation reservation : reservationMap.values()) {
				reservation.stopAndWait();
			}
			portalEventBus.unregister(this);
			schedulerService.stopAndWait();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	/**
	 * Replay all {@link de.uniluebeck.itm.tr.iwsn.messages.ReservationStartedEvent}s of reservations that are
	 * currently active to drive all subscribers to the current state (cf. event-sourced architecture).
	 */
	private void replayStartedEvents() {
		log.trace("ReservationManagerImpl.replayStartedEvents()");
		try {
			for (ConfidentialReservationData crd : rsPersistence.get().getActiveReservations()) {
				final String serializedKey = ReservationHelper.serialize(crd.getSecretReservationKey());
				log.trace("Replaying ReservationStartedEvent for {}", serializedKey);
				getReservation(serializedKey);
			}
		} catch (RSFault_Exception e) {
			throw propagate(e);
		}
	}

	@Subscribe
	public void onReservationMadeEvent(final ReservationMadeEvent event) {
		log.trace("ReservationManagerImpl.onReservationMadeEvent({})", event.getSerializedKey());
		// getReservation will set up any internal state necessary
		getReservation(ReservationHelper.deserialize(event.getSerializedKey()));
	}

	@Subscribe
	public void onReservationDeletedEvent(final ReservationDeletedEvent event) {
		log.trace("ReservationManagerImpl.onReservationDeletedEvent({})", event.getSerializedKey());
		// TODO implement me
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

		synchronized (reservationMap) {

			final Set<SecretReservationKey> srkSet = newHashSet(srks);
			if (reservationMap.containsKey(srkSet)) {
				return reservationMap.get(srkSet);
			}

			final List<ConfidentialReservationData> confidentialReservationDataList;
			try {
				confidentialReservationDataList = rs.get().getReservation(newArrayList(srkSet));
			} catch (RSFault_Exception e) {
				throw propagate(e);
			} catch (UnknownSecretReservationKeyFault e) {
				throw new ReservationUnknownException(srkSet, e);
			}

			if (confidentialReservationDataList.size() == 0) {
				throw new ReservationUnknownException(srkSet);
			}

			checkArgument(
					confidentialReservationDataList.size() == 1,
					"There must be exactly one secret reservation key as this is a single URN-prefix implementation."
			);

			final ConfidentialReservationData data = confidentialReservationDataList.get(0);
			final Set<NodeUrn> reservedNodes = newHashSet(data.getNodeUrns());

			assertNodesInTestbed(reservedNodes);

			final Reservation reservation = reservationFactory.create(
					confidentialReservationDataList,
					data.getSecretReservationKey().getKey(),
					data.getUsername(),
					reservedNodes,
					new Interval(data.getFrom(), data.getTo())
			);

			reservationMap.put(srkSet, reservation);

			scheduleStartStop(reservation);

			return reservation;
		}
	}

	/**
	 * Schedules starting and stopping the @link{Reservation} instance depending on its interval. If the reservation is
	 * already running {@link com.google.common.util.concurrent.Service#startAndWait()} will be called immediately. The
	 * callables scheduled will also trigger a post of a {@link de.uniluebeck.itm.tr.iwsn.messages.ReservationStartedEvent}.
	 *
	 * @param reservation
	 * 		the reservation
	 */
	private void scheduleStartStop(final Reservation reservation) {

		if (!reservation.isRunning() && reservation.getInterval().containsNow()) {

			log.trace("ReservationManagerImpl.scheduleStartStop() for currently running reservation");

			final Duration startAfter = Duration.ZERO;
			final Duration stopAfter = new Duration(now(), reservation.getInterval().getEnd());

			final ReservationStartCallable startCallable = new ReservationStartCallable(portalEventBus, reservation);
			final ReservationStopCallable stopCallable = new ReservationStopCallable(portalEventBus, reservation);

			schedulerService.schedule(startCallable, startAfter.getMillis(), MS);
			schedulerService.schedule(stopCallable, stopAfter.getMillis(), MS);

		} else if (reservation.getInterval().isAfterNow()) {

			log.trace("ReservationManagerImpl.scheduleStartStop() for future reservation");

			final Duration startAfter = new Duration(now(), reservation.getInterval().getStart());
			final Duration stopAfter = new Duration(now(), reservation.getInterval().getEnd());

			final ReservationStartCallable startCallable = new ReservationStartCallable(portalEventBus, reservation);
			final ReservationStopCallable stopCallable = new ReservationStopCallable(portalEventBus, reservation);

			schedulerService.schedule(startCallable, startAfter.getMillis(), MS);
			schedulerService.schedule(stopCallable, stopAfter.getMillis(), MS);
		}
	}

	@Override
	public Reservation getReservation(final String secretReservationKeysBase64)
			throws ReservationUnknownException {
		return getReservation(deserialize(secretReservationKeysBase64));
	}

	private void assertNodesInTestbed(final Set<NodeUrn> reservedNodes) {
		for (Map.Entry<NodeUrn, DeviceConfig> entry : deviceDBService.getConfigsByNodeUrns(reservedNodes).entrySet()) {
			if (entry.getValue() == null) {
				throw new RuntimeException("Node URN \"" + entry.getKey() + "\" unknown.");
			}
		}
	}
}
