package de.uniluebeck.itm.tr.iwsn.portal;

import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfigDB;
import de.uniluebeck.itm.tr.util.Logging;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.rs.ReservationNotFoundFault;
import eu.wisebed.api.v3.rs.ReservationNotFoundFault_Exception;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReservationManagerImplTest {

	static {
		Logging.setLoggingDefaults();
	}

	private static final NodeUrnPrefix NODE_URN_PREFIX = new NodeUrnPrefix("urn:unit-test:");

	private static final String KNOWN_SECRET_RESERVATION_KEY = "YOU_KNOW_ME";

	private static final String UNKNOWN_SECRET_RESERVATION_KEY = "YOU_DONT_KNOW_ME";

	private static final List<SecretReservationKey> KNOWN_SECRET_RESERVATION_KEY_LIST = newArrayList();

	private static final List<SecretReservationKey> UNKNOWN_SECRET_RESERVATION_KEY_LIST = newArrayList();

	static {
		final SecretReservationKey srk = new SecretReservationKey();
		srk.setUrnPrefix(NODE_URN_PREFIX);
		srk.setSecretReservationKey(KNOWN_SECRET_RESERVATION_KEY);
		KNOWN_SECRET_RESERVATION_KEY_LIST.add(srk);

		final SecretReservationKey srk2 = new SecretReservationKey();
		srk2.setUrnPrefix(NODE_URN_PREFIX);
		srk2.setSecretReservationKey(UNKNOWN_SECRET_RESERVATION_KEY);
		UNKNOWN_SECRET_RESERVATION_KEY_LIST.add(srk2);
	}

	private static final Set<NodeUrn> RESERVATION_NODE_URNS = newHashSet(new NodeUrn(NODE_URN_PREFIX + "0x0001"));

	private static final Interval RESERVATION_INTERVAL = new Interval(DateTime.now(), DateTime.now().plusHours(1));

	private static final List<ConfidentialReservationData> RESERVATION_DATA = newArrayList();

	static {
		final ConfidentialReservationData crd = new ConfidentialReservationData();
		crd.setFrom(RESERVATION_INTERVAL.getStart());
		crd.setTo(RESERVATION_INTERVAL.getEnd());
		crd.getNodeUrns().addAll(RESERVATION_NODE_URNS);
		RESERVATION_DATA.add(crd);
	}

	private static final PortalConfig PORTAL_CONFIG = new PortalConfig();

	static {
		PORTAL_CONFIG.urnPrefix = NODE_URN_PREFIX;
	}

	@Mock
	private RS rs;

	@Mock
	private DeviceConfigDB deviceConfigDB;

	@Mock
	private ReservationFactory reservationFactory;

	@Mock
	private Reservation reservation;

	private ReservationManagerImpl reservationManager;

	@Before
	public void setUp() throws Exception {
		reservationManager = new ReservationManagerImpl(PORTAL_CONFIG, rs, deviceConfigDB, reservationFactory);
		reservationManager.startAndWait();
	}

	@After
	public void tearDown() throws Exception {
		reservationManager.stopAndWait();
	}

	@Test
	public void testThatReservationIsStartedAndReturnedIfKnown() throws Exception {

		when(rs.getReservation(KNOWN_SECRET_RESERVATION_KEY_LIST)).thenReturn(RESERVATION_DATA);
		when(reservationFactory.create(RESERVATION_NODE_URNS, RESERVATION_INTERVAL)).thenReturn(reservation);

		assertSame(reservation, reservationManager.getReservation(KNOWN_SECRET_RESERVATION_KEY));
		verify(reservationFactory).create(eq(RESERVATION_NODE_URNS), eq(RESERVATION_INTERVAL));
		verify(reservation).startAndWait();
	}

	@Test
	public void testThatNullIsReturnedAndNoReservationIsCreatedIfReservationIsUnknown() throws Exception {
		when(rs.getReservation(eq(UNKNOWN_SECRET_RESERVATION_KEY_LIST)))
				.thenThrow(new ReservationNotFoundFault_Exception("not found", new ReservationNotFoundFault()));
		try {
			reservationManager.getReservation(UNKNOWN_SECRET_RESERVATION_KEY);
		} catch (ReservationNotFoundFault_Exception e) {
			verifyZeroInteractions(deviceConfigDB);
			verifyZeroInteractions(reservationFactory);
		}
	}
}
