package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.inject.util.Providers;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.util.logging.Logging;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceFactory;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.common.ReservationHelper.serialize;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReservationCacheImplIntervalTest {

	private static final String USERNAME = "My Awesome Username";
	private static final NodeUrnPrefix NODE_URN_PREFIX = new NodeUrnPrefix("urn:unit-test:");
	private static final SecretReservationKey SRK_1;
	private static final SecretReservationKey SRK_2;
	private static final SecretReservationKey SRK_3;
	private static final SecretReservationKey SRK_4;
	private static final NodeUrn NODE_1 = new NodeUrn(NODE_URN_PREFIX + "0x0001");
	private static final NodeUrn NODE_2 = new NodeUrn(NODE_URN_PREFIX + "0x0002");
	private static final NodeUrn NODE_3 = new NodeUrn(NODE_URN_PREFIX + "0x0003");
	private static final Set<NodeUrn> NODE_SET_1 = newHashSet(NODE_1);
	private static final Set<NodeUrn> NODE_SET_2 = newHashSet(NODE_2);
	private static final Set<NodeUrn> NODE_SET_3 = newHashSet(NODE_1);
	private static final Set<NodeUrn> NODE_SET_4 = newHashSet(NODE_1);
	private static final Interval RESERVATION_INTERVAL_1 = new Interval(
			DateTime.now().minusMinutes(90),
			DateTime.now().minusMinutes(70)
	);
	private static final Interval RESERVATION_INTERVAL_2 = new Interval(
			DateTime.now().minusMinutes(80),
			DateTime.now().minusMinutes(60)
	);
	private static final Interval RESERVATION_INTERVAL_3 = new Interval(
			DateTime.now().minusMinutes(50),
			DateTime.now().minusMinutes(30)
	);
	private static final Interval RESERVATION_INTERVAL_4 = new Interval(
			DateTime.now().minusMinutes(20),
			DateTime.now().plusMinutes(10)
	);
	private static final List<ConfidentialReservationData> RESERVATION_DATA_1 = newArrayList();
	private static final List<ConfidentialReservationData> RESERVATION_DATA_2 = newArrayList();
	private static final List<ConfidentialReservationData> RESERVATION_DATA_3 = newArrayList();
	private static final List<ConfidentialReservationData> RESERVATION_DATA_4 = newArrayList();
	private static final DateTime QUERY_TIMESTAMP_75 = DateTime.now().minusMinutes(75);
	private static final DateTime QUERY_TIMESTAMP_40 = DateTime.now().minusMinutes(40);
	private static final DateTime QUERY_TIMESTAMP_25 = DateTime.now().minusMinutes(25);
	private static final DateTime QUERY_TIMESTAMP_5 = DateTime.now().plusMinutes(5);

	static {
		Logging.setLoggingDefaults();

		SRK_1 = new SecretReservationKey().withKey("SRK_1").withUrnPrefix(NODE_URN_PREFIX);
		SRK_2 = new SecretReservationKey().withKey("SRK_2").withUrnPrefix(NODE_URN_PREFIX);
		SRK_3 = new SecretReservationKey().withKey("SRK_3").withUrnPrefix(NODE_URN_PREFIX);
		SRK_4 = new SecretReservationKey().withKey("SRK_4").withUrnPrefix(NODE_URN_PREFIX);
	}

	static {

		final ConfidentialReservationData crd1 = new ConfidentialReservationData();
		crd1.setFrom(RESERVATION_INTERVAL_1.getStart());
		crd1.setTo(RESERVATION_INTERVAL_1.getEnd());
		crd1.getNodeUrns().addAll(NODE_SET_1);
		crd1.setUsername(USERNAME);
		crd1.setSecretReservationKey(SRK_1);
		RESERVATION_DATA_1.add(crd1);

		final ConfidentialReservationData crd2 = new ConfidentialReservationData();
		crd2.setFrom(RESERVATION_INTERVAL_2.getStart());
		crd2.setTo(RESERVATION_INTERVAL_2.getEnd());
		crd2.getNodeUrns().addAll(NODE_SET_2);
		crd2.setUsername(USERNAME);
		crd2.setSecretReservationKey(SRK_2);
		RESERVATION_DATA_2.add(crd2);

		final ConfidentialReservationData crd3 = new ConfidentialReservationData();
		crd3.setFrom(RESERVATION_INTERVAL_3.getStart());
		crd3.setTo(RESERVATION_INTERVAL_3.getEnd());
		crd3.getNodeUrns().addAll(NODE_SET_3);
		crd3.setUsername(USERNAME);
		crd3.setSecretReservationKey(SRK_3);
		RESERVATION_DATA_3.add(crd3);

		final ConfidentialReservationData crd4 = new ConfidentialReservationData();
		crd4.setFrom(RESERVATION_INTERVAL_4.getStart());
		crd4.setTo(RESERVATION_INTERVAL_4.getEnd());
		crd4.getNodeUrns().addAll(NODE_SET_4);
		crd4.setUsername(USERNAME);
		crd4.setSecretReservationKey(SRK_4);
		RESERVATION_DATA_4.add(crd4);
	}

	private static final String SSRK_1 = serialize(SRK_1);
	private static final String SSRK_2 = serialize(SRK_1);
	private static final String SSRK_3 = serialize(SRK_1);
	private static final String SSRK_4 = serialize(SRK_1);

	@Mock
	private Reservation reservation1;

	@Mock
	private Reservation reservation2;

	@Mock
	private Reservation reservation3;

	@Mock
	private Reservation reservation4;

	@Mock
	private SchedulerServiceFactory schedulerServiceFactory;

	@Mock
	private SchedulerService schedulerService;

	@Mock
	private CommonConfig commonConfig;

	@Mock
	private ReservationCache reservationCache;

	@Mock
	private Ticker ticker;

	@Mock
	private ScheduledFuture scheduledFuture;

	private ReservationCacheImpl cache;

	@Before
	public void setUp() throws Exception {

		when(commonConfig.getUrnPrefix()).thenReturn(NODE_URN_PREFIX);
		when(schedulerServiceFactory.create(anyInt(), anyString())).thenReturn(schedulerService);
		//noinspection unchecked
		when(schedulerService.scheduleAtFixedRate(
				any(Runnable.class),
				anyInt(),
				anyInt(),
				any(TimeUnit.class)
		)).thenReturn(scheduledFuture);
		when(schedulerService.startAsync()).thenReturn(schedulerService);
		when(schedulerService.stopAsync()).thenReturn(schedulerService);

		when(reservation1.getNodeUrnPrefixes()).thenReturn(newHashSet(NODE_URN_PREFIX));
		when(reservation2.getNodeUrnPrefixes()).thenReturn(newHashSet(NODE_URN_PREFIX));
		when(reservation3.getNodeUrnPrefixes()).thenReturn(newHashSet(NODE_URN_PREFIX));
		when(reservation4.getNodeUrnPrefixes()).thenReturn(newHashSet(NODE_URN_PREFIX));

		when(reservation1.getSecretReservationKeys()).thenReturn(newHashSet(SRK_1));
		when(reservation2.getSecretReservationKeys()).thenReturn(newHashSet(SRK_2));
		when(reservation3.getSecretReservationKeys()).thenReturn(newHashSet(SRK_3));
		when(reservation4.getSecretReservationKeys()).thenReturn(newHashSet(SRK_4));

		when(reservation1.getInterval()).thenReturn(RESERVATION_INTERVAL_1);
		when(reservation2.getInterval()).thenReturn(RESERVATION_INTERVAL_2);
		when(reservation3.getInterval()).thenReturn(RESERVATION_INTERVAL_3);
		when(reservation4.getInterval()).thenReturn(RESERVATION_INTERVAL_4);

		when(reservation1.getNodeUrns()).thenReturn(NODE_SET_1);
		when(reservation2.getNodeUrns()).thenReturn(NODE_SET_2);
		when(reservation3.getNodeUrns()).thenReturn(NODE_SET_3);
		when(reservation4.getNodeUrns()).thenReturn(NODE_SET_4);

		when(reservation1.getSerializedKey()).thenReturn(SSRK_1);
		when(reservation2.getSerializedKey()).thenReturn(SSRK_2);
		when(reservation3.getSerializedKey()).thenReturn(SSRK_3);
		when(reservation4.getSerializedKey()).thenReturn(SSRK_4);

		cache = new ReservationCacheImpl(schedulerServiceFactory, Providers.of(Stopwatch.createUnstarted(ticker)));

		cache.startAsync().awaitRunning();

		cache.put(reservation1);
		cache.put(reservation2);
		cache.put(reservation3);
		cache.put(reservation4);
	}

	@After
	public void tearDown() throws Exception {
		cache.stopAsync().awaitTerminated();
	}

	@Test
	public void testOverlappingReservationsOneAndTwo() throws Exception {

		Optional<Reservation> queryNode1 = cache.lookup(NODE_1, QUERY_TIMESTAMP_75);
		assertTrue(queryNode1.isPresent());
		assertTrue(queryNode1.get().getNodeUrns().contains(NODE_1));
		assertEquals(SSRK_1, queryNode1.get().getSerializedKey());

		Optional<Reservation> queryNode2 = cache.lookup(NODE_2, QUERY_TIMESTAMP_75);
		assertTrue(queryNode2.isPresent());
		assertTrue(queryNode2.get().getNodeUrns().contains(NODE_2));
		assertEquals(SSRK_2, queryNode1.get().getSerializedKey());

		Optional<Reservation> queryNode3 = cache.lookup(NODE_3, QUERY_TIMESTAMP_75);
		assertFalse(queryNode3.isPresent());
	}

	@Test
	public void testReservationThree() throws Exception {

		Optional<Reservation> queryNode1 = cache.lookup(NODE_1, QUERY_TIMESTAMP_40);
		assertTrue(queryNode1.isPresent());
		assertTrue(queryNode1.get().getNodeUrns().contains(NODE_1));
		assertEquals(SSRK_1, queryNode1.get().getSerializedKey());

		Optional<Reservation> queryNode3 = cache.lookup(NODE_3, QUERY_TIMESTAMP_40);
		assertFalse(queryNode3.isPresent());
	}

	@Test
	public void testDuringNoReservationsTimeSpan() throws Exception {

		assertFalse(cache.lookup(NODE_1, QUERY_TIMESTAMP_25).isPresent());
		assertFalse(cache.lookup(NODE_2, QUERY_TIMESTAMP_25).isPresent());
		assertFalse(cache.lookup(NODE_3, QUERY_TIMESTAMP_25).isPresent());
	}

	@Test
	public void testPointOfTimeInFuture() throws Exception {

		Optional<Reservation> queryNode1 = cache.lookup(NODE_1, QUERY_TIMESTAMP_5);
		Optional<Reservation> queryNode2 = cache.lookup(NODE_2, QUERY_TIMESTAMP_5);
		Optional<Reservation> queryNode3 = cache.lookup(NODE_3, QUERY_TIMESTAMP_5);

		assertTrue(queryNode1.isPresent());
		assertTrue(queryNode1.get().getNodeUrns().contains(NODE_1));

		assertFalse(queryNode2.isPresent());
		assertFalse(queryNode3.isPresent());
	}
}
