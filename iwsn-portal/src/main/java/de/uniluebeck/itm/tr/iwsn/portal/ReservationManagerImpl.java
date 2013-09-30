package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.iwsn.portal.ReservationHelper.deserialize;
import static org.joda.time.DateTime.now;

public class ReservationManagerImpl extends AbstractService implements ReservationManager {

	private static final Logger log = LoggerFactory.getLogger(ReservationManager.class);

	private static final TimeUnit MS = TimeUnit.MILLISECONDS;

	private final Provider<RS> rs;

	private final DeviceDBService deviceDBService;

	private final ReservationFactory reservationFactory;

	private final Map<Set<SecretReservationKey>, Reservation> reservationMap = newHashMap();

	private final SchedulerService schedulerService;

	@Inject
	public ReservationManagerImpl(final Provider<RS> rs,
								  final DeviceDBService deviceDBService,
								  final ReservationFactory reservationFactory,
								  final SchedulerServiceFactory schedulerServiceFactory) {
		this.rs = checkNotNull(rs);
		this.deviceDBService = checkNotNull(deviceDBService);
		this.reservationFactory = checkNotNull(reservationFactory);
		this.schedulerService = schedulerServiceFactory.create(-1, "ReservationManager");
	}

	@Override
	protected void doStart() {
		log.trace("ReservationManagerImpl.doStart()");
		try {
			schedulerService.startAndWait();
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
			schedulerService.stopAndWait();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public Reservation getReservation(final List<SecretReservationKey> secretReservationKeys)
			throws ReservationUnknownException {

		log.trace("ReservationManagerImpl.getReservation(secretReservationKey={})", secretReservationKeys);

		final Set<SecretReservationKey> srkSet = newHashSet(secretReservationKeys);

		synchronized (reservationMap) {

			if (reservationMap.containsKey(srkSet)) {
				return reservationMap.get(srkSet);
			}

			final List<ConfidentialReservationData> confidentialReservationDataList;
			try {
				confidentialReservationDataList = rs.get().getReservation(secretReservationKeys);
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
					data.getSecretReservationKey().getKey(),
					data.getUsername(),
					reservedNodes,
					new Interval(data.getFrom(), data.getTo())
			);

			reservationMap.put(srkSet, reservation);

			if (!reservation.isRunning() && reservation.getInterval().containsNow()) {

				reservation.startAndWait();

				final Duration stopAfter = new Duration(now(), reservation.getInterval().getEnd());
				schedulerService.schedule(new ReservationStopCallable(reservation), stopAfter.getMillis(), MS);

			} else if (reservation.getInterval().isAfterNow()) {

				final Duration startAfter = new Duration(now(), reservation.getInterval().getStart());
				final Duration stopAfter = new Duration(now(), reservation.getInterval().getEnd());

				schedulerService.schedule(new ReservationStartCallable(reservation), startAfter.getMillis(), MS);
				schedulerService.schedule(new ReservationStopCallable(reservation), stopAfter.getMillis(), MS);
			}

			return reservation;
		}
	}

	@Override
	public Reservation getReservation(final String jsonSerializedSecretReservationKeys)
			throws ReservationUnknownException {
		return getReservation(deserialize(jsonSerializedSecretReservationKeys));
	}

	private void assertNodesInTestbed(final Set<NodeUrn> reservedNodes) {
		for (Map.Entry<NodeUrn, DeviceConfig> entry : deviceDBService.getConfigsByNodeUrns(reservedNodes).entrySet()) {
			if (entry.getValue() == null) {
				throw new RuntimeException("Node URN \"" + entry.getKey() + "\" unknown.");
			}
		}
	}
}
