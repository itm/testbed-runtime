package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import de.uniluebeck.itm.tr.iwsn.common.SchedulerService;
import de.uniluebeck.itm.tr.iwsn.common.SchedulerServiceFactory;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfigDB;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfig;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.rs.ReservationNotFoundFault_Exception;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static org.joda.time.DateTime.now;

public class ReservationManagerImpl extends AbstractService implements ReservationManager {

	private static final Logger log = LoggerFactory.getLogger(ReservationManager.class);

	private static final ThreadFactory SCHEDULER_THREAD_FACTORY =
			new ThreadFactoryBuilder().setNameFormat("ReservationManager %d").build();

	private static final TimeUnit MS = TimeUnit.MILLISECONDS;

	private final PortalConfig portalConfig;

	private final Provider<RS> rs;

	private final DeviceConfigDB deviceConfigDB;

	private final ReservationFactory reservationFactory;

	private final Map<String, Reservation> reservationMap = newHashMap();

	private final SchedulerService schedulerService;

	@Inject
	public ReservationManagerImpl(final PortalConfig portalConfig,
								  final Provider<RS> rs,
								  final DeviceConfigDB deviceConfigDB,
								  final ReservationFactory reservationFactory,
								  final SchedulerServiceFactory schedulerServiceFactory) {
		this.portalConfig = checkNotNull(portalConfig);
		this.rs = checkNotNull(rs);
		this.deviceConfigDB = checkNotNull(deviceConfigDB);
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
	public Reservation getReservation(final String secretReservationKey) throws ReservationUnknownException {

		log.trace("ReservationManagerImpl.getReservation(secretReservationKey={})", secretReservationKey);

		synchronized (reservationMap) {

			if (reservationMap.containsKey(secretReservationKey)) {
				return reservationMap.get(secretReservationKey);
			}

			final List<ConfidentialReservationData> confidentialReservationDataList;
			try {
				final List<SecretReservationKey> keys = toSecretReservationKeyList(secretReservationKey);
				confidentialReservationDataList = rs.get().getReservation(keys);
			} catch (RSFault_Exception e) {
				throw propagate(e);
			} catch (ReservationNotFoundFault_Exception e) {
				throw new ReservationUnknownException(secretReservationKey, e);
			}

			if (confidentialReservationDataList.size() == 0) {
				throw new ReservationUnknownException(secretReservationKey);
			}

			checkArgument(
					confidentialReservationDataList.size() == 1,
					"There must be exactly one secret reservation key as this is a single URN-prefix implementation."
			);

			final ConfidentialReservationData data = confidentialReservationDataList.get(0);
			final Set<NodeUrn> reservedNodes = newHashSet(data.getNodeUrns());

			assertNodesInTestbed(reservedNodes);

			final Reservation reservation = reservationFactory.create(
					reservedNodes,
					new Interval(data.getFrom(), data.getTo())
			);

			reservationMap.put(secretReservationKey, reservation);

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

	private void assertNodesInTestbed(final Set<NodeUrn> reservedNodes) {
		for (Map.Entry<NodeUrn, DeviceConfig> entry : deviceConfigDB.getByNodeUrns(reservedNodes).entrySet()) {
			if (entry.getValue() == null) {
				throw new RuntimeException("Node URN \"" + entry.getKey() + "\" unknown.");
			}
		}
	}

	private List<SecretReservationKey> toSecretReservationKeyList(String secretReservationKey) {
		SecretReservationKey key = new SecretReservationKey();
		key.setUrnPrefix(portalConfig.urnPrefix);
		key.setSecretReservationKey(secretReservationKey);
		return newArrayList(key);
	}
}
