package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfigDB;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.rs.ReservationNotFoundFault_Exception;
import eu.wisebed.api.v3.sm.ExperimentNotRunningFault_Exception;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

public class ReservationManagerImpl extends AbstractService implements ReservationManager {

	private static final Logger log = LoggerFactory.getLogger(ReservationManager.class);

	private final PortalConfig portalConfig;

	private final RS rs;

	private final DeviceConfigDB deviceConfigDB;

	private final ReservationFactory reservationFactory;

	private final Map<String, Reservation> reservationMap = newHashMap();

	private ScheduledExecutorService scheduler;

	@Inject
	public ReservationManagerImpl(final PortalConfig portalConfig, final RS rs, final DeviceConfigDB deviceConfigDB,
								  final ReservationFactory reservationFactory) {
		this.portalConfig = portalConfig;
		this.rs = rs;
		this.deviceConfigDB = deviceConfigDB;
		this.reservationFactory = reservationFactory;
	}

	@Override
	protected void doStart() {
		try {
			scheduler = Executors.newScheduledThreadPool(1,
					new ThreadFactoryBuilder().setNameFormat("ReservationManager %d").build()
			);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			ExecutorUtils.shutdown(scheduler, 1, TimeUnit.SECONDS);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public Reservation getReservation(final String secretReservationKey)
			throws RSFault_Exception, ReservationNotFoundFault_Exception, ExperimentNotRunningFault_Exception {

		synchronized (reservationMap) {

			if (reservationMap.containsKey(secretReservationKey)) {
				return reservationMap.get(secretReservationKey);
			}

			final List<SecretReservationKey> keys = toSecretReservationKeyList(secretReservationKey);
			final List<ConfidentialReservationData> confidentialReservationDataList = rs.getReservation(keys);

			checkArgument(
					confidentialReservationDataList.size() == 1,
					"There must be exactly one secret reservation key as this is a single URN-prefix implementation."
			);

			assertReservationIntervalMet(confidentialReservationDataList);

			final ConfidentialReservationData data = confidentialReservationDataList.get(0);
			final Set<NodeUrn> reservedNodes = newHashSet(data.getNodeUrns());

			assertNodesInTestbed(reservedNodes);

			final Reservation reservation = reservationFactory.create(
					reservedNodes,
					new Interval(data.getFrom(), data.getTo())
			);

			reservation.startAndWait();

			final Runnable stopReservationRunnable = new Runnable() {
				@Override
				public void run() {
					reservation.stopAndWait();
				}
			};

			final long stopAfterMillis = new Duration(DateTime.now(), data.getTo()).getMillis();
			scheduler.schedule(stopReservationRunnable, stopAfterMillis, TimeUnit.MILLISECONDS);

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

	/**
	 * Checks the reservations' time intervals if they have already started or have already stopped and throws an
	 * exception
	 * if that's the case.
	 *
	 * @param reservations
	 * 		the reservations to check
	 *
	 * @throws eu.wisebed.api.v3.sm.ExperimentNotRunningFault_Exception
	 * 		if now is not inside the reservations' time interval
	 */
	private void assertReservationIntervalMet(List<ConfidentialReservationData> reservations)
			throws ExperimentNotRunningFault_Exception {

		for (ConfidentialReservationData reservation : reservations) {

			DateTime from = reservation.getFrom();
			DateTime to = reservation.getTo();

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

	private List<SecretReservationKey> toSecretReservationKeyList(String secretReservationKey) {
		SecretReservationKey key = new SecretReservationKey();
		key.setUrnPrefix(portalConfig.urnPrefix);
		key.setSecretReservationKey(secretReservationKey);
		return newArrayList(key);
	}
}
