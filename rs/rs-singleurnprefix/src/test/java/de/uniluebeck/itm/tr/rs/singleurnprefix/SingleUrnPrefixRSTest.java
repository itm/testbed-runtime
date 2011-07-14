package de.uniluebeck.itm.tr.rs.singleurnprefix;


import com.google.common.collect.Lists;
import com.google.inject.*;
import com.google.inject.name.Names;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.wisebed.cmdlineclient.BeanShellHelper;
import eu.wisebed.api.rs.*;
import eu.wisebed.api.sm.SessionManagement;
import eu.wisebed.api.snaa.Actions;
import eu.wisebed.api.snaa.SNAA;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SingleUrnPrefixRSTest {

	static {
		try {
			datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			fail();
		}
	}

	private static DatatypeFactory datatypeFactory;

	private static final String URN_PREFIX = "urn:local:";

	private static final String USER1_SECRET_RESERVATION_KEY = "12345";

	private static final String USER2_SECRET_RESERVATION_KEY = "54321";

	private static final String USER1_SECRET_AUTHENTICATION_KEY = "abcdef";

	private static final String USER2_SECRET_AUTHENTICATION_KEY = "fedcba";

	private static final String USER1_USERNAME = "user1@domain.tld";

	private static final String USER2_USERNAME = "user2@otherdomain.tld";

	@Mock
	private RSPersistence persistence;

	@Mock
	private SNAA snaa;

	@Mock
	private SessionManagement sessionManagement;

	@Mock
	private Provider<String[]> servedNodeUrns;

	private RS rs;

	private SecretAuthenticationKey user1Sak;

	private SecretAuthenticationKey user2Sak;

	private List<SecretAuthenticationKey> user1Saks;

	private List<SecretAuthenticationKey> user2Saks;

	private SecretReservationKey user1Srk;

	private SecretReservationKey user2Srk;

	private List<SecretReservationKey> user1Srks;

	private List<SecretReservationKey> user2Srks;

	private List<eu.wisebed.api.snaa.SecretAuthenticationKey> user1SaksSnaa;

	private List<eu.wisebed.api.snaa.SecretAuthenticationKey> user2SaksSnaa;

	@Before
	public void setUp() {

		final Injector injector = Guice.createInjector(new Module() {
			@Override
			public void configure(final Binder binder) {

				binder.bind(String.class).annotatedWith(Names.named("SingleUrnPrefixRS.urnPrefix"))
						.toInstance(URN_PREFIX);

				binder.bind(SNAA.class)
						.toInstance(snaa);

				binder.bind(SessionManagement.class)
						.toInstance(sessionManagement);

				binder.bind(RSPersistence.class)
						.toInstance(persistence);

				binder.bind(String[].class)
						.annotatedWith(Names.named("SingleUrnPrefixRS.servedNodeUrns"))
						.toProvider(servedNodeUrns);

				binder.bind(RS.class)
						.to(SingleUrnPrefixRS.class);
			}
		}
		);

		rs = injector.getInstance(RS.class);

		user1Sak = new SecretAuthenticationKey();
		user1Sak.setSecretAuthenticationKey(USER1_SECRET_AUTHENTICATION_KEY);
		user1Sak.setUrnPrefix(URN_PREFIX);
		user1Sak.setUsername(USER1_USERNAME);
		user1Saks = Lists.newArrayList(user1Sak);

		user2Sak = new SecretAuthenticationKey();
		user2Sak.setSecretAuthenticationKey(USER2_SECRET_AUTHENTICATION_KEY);
		user2Sak.setUrnPrefix(URN_PREFIX);
		user2Sak.setUsername(USER2_USERNAME);
		user2Saks = Lists.newArrayList(user2Sak);

		user1SaksSnaa = BeanShellHelper.copyRsToSnaa(user1Saks);
		user2SaksSnaa = BeanShellHelper.copyRsToSnaa(user2Saks);

		user1Srk = new SecretReservationKey();
		user1Srk.setSecretReservationKey(USER1_SECRET_RESERVATION_KEY);
		user1Srk.setUrnPrefix(URN_PREFIX);
		user1Srks = Lists.newArrayList(user1Srk);

		user2Srk = new SecretReservationKey();
		user2Srk.setSecretReservationKey(USER2_SECRET_RESERVATION_KEY);
		user2Srk.setUrnPrefix(URN_PREFIX);
		user2Srks = Lists.newArrayList(user2Srk);
	}

	@Test
	public void throwExceptionWhenTryingToDeleteReservationThatAlreadyStarted() throws Exception {

		final DateTime from = new DateTime().minusSeconds(1);
		final DateTime to = from.plusHours(1);

		final ConfidentialReservationData crd = new ConfidentialReservationData();
		crd.setFrom(datatypeFactory.newXMLGregorianCalendar(from.toGregorianCalendar()));
		crd.setTo(datatypeFactory.newXMLGregorianCalendar(to.toGregorianCalendar()));
		crd.setUserData(USER1_USERNAME);

		when(snaa.isAuthorized(user1SaksSnaa, Actions.DELETE_RESERVATION)).thenReturn(true);
		when(persistence.getReservation(user1Srk)).thenReturn(crd);

		try {
			rs.deleteReservation(user1Saks, user1Srks);
			fail();
		} catch (RSExceptionException e) {
			// this should be thrown
		}

		// TODO https://github.com/itm/testbed-runtime/issues/47
		//verify(snaa).isAuthorized(user1SaksSnaa, Actions.DELETE_RESERVATION);
		verify(persistence).getReservation(user1Srk);

		verify(persistence, never()).deleteReservation(user1Srk);
	}

	@Test
	public void throwNoExceptionWhenTryingToDeleteReservationInTheFuture() throws Exception {

		final DateTime from = new DateTime().plusHours(1);
		final DateTime to = from.plusHours(1);

		final ConfidentialReservationData crd = new ConfidentialReservationData();
		crd.setFrom(datatypeFactory.newXMLGregorianCalendar(from.toGregorianCalendar()));
		crd.setTo(datatypeFactory.newXMLGregorianCalendar(to.toGregorianCalendar()));
		crd.setUserData(USER1_USERNAME);

		when(snaa.isAuthorized(user1SaksSnaa, Actions.GET_CONFIDENTIAL_RESERVATION)).thenReturn(true);
		when(persistence.getReservation(user1Srk)).thenReturn(crd);

		when(snaa.isAuthorized(user1SaksSnaa, Actions.DELETE_RESERVATION)).thenReturn(true);
		when(persistence.deleteReservation(user1Srk)).thenReturn(crd);

		rs.deleteReservation(user1Saks, user1Srks);

		// TODO https://github.com/itm/testbed-runtime/issues/47
		//verify(snaa).isAuthorized(user1SaksSnaa, Actions.DELETE_RESERVATION);

		verify(persistence).getReservation(user1Srk);
		verify(persistence).deleteReservation(user1Srk);
	}

	@Test
	public void reserveSameNodeTwiceInUpperCaseAndLowercase() throws Exception {

		final DateTime from = new DateTime().plusHours(1);
		final DateTime to = from.plusHours(1);

		final SecretReservationKey srk = new SecretReservationKey();
		srk.setSecretReservationKey("abc123");

		final List<ConfidentialReservationData> reservedNodes = newArrayList();
		ConfidentialReservationData persistenceCrd = new ConfidentialReservationData();
		persistenceCrd.getNodeURNs().add("urn:local:0xcbe4");
		reservedNodes.add(persistenceCrd);

		// make a valid reservation
		when(snaa.isAuthorized(user1SaksSnaa, Actions.MAKE_RESERVATION)).thenReturn(true);
		when(servedNodeUrns.get()).thenReturn(new String[]{"urn:local:0xcbe4"});
		when(persistence.addReservation(Matchers.<ConfidentialReservationData>any(), eq("urn:local:"))).thenReturn(srk);
		when(persistence.getReservations(Matchers.<Interval>any())).thenReturn(reservedNodes);

		// try to reserve in uppercase
		ConfidentialReservationData crd = buildConfidentialReservationData(from, to, USER1_USERNAME, "urn:local:0xCBE4");
		try {
			rs.makeReservation(user1Saks, crd);
			fail();
		} catch (AuthorizationExceptionException e) {
			fail();
		} catch (RSExceptionException e) {
			fail();
		} catch (ReservervationConflictExceptionException expected) {
		}

		// try to reserve in lowercase
		crd = buildConfidentialReservationData(from, to, USER1_USERNAME, "urn:local:0xcbe4");
		try {
			rs.makeReservation(user1Saks, crd);
			fail();
		} catch (AuthorizationExceptionException e) {
			fail();
		} catch (RSExceptionException e) {
			fail();
		} catch (ReservervationConflictExceptionException expected) {
		}

	}

	/**
	 * Given there are reservations by more than one user, the RS should only return reservations of the authenticated user
	 * when {@link RS#getConfidentialReservations(java.util.List, eu.wisebed.api.rs.GetReservations)} is called.
	 *
	 * @throws Exception if anything goes wrong
	 */
	@Test
	public void testOnlyConfidentialReservationsOfAuthenticatedUserReturned() throws Exception {

		final DateTime from = new DateTime().plusHours(1);
		final DateTime to = from.plusHours(1);

		final String user1Node = URN_PREFIX + "0x1234";
		final String user2Node = URN_PREFIX + "0x4321";

		when(snaa.isAuthorized(user1SaksSnaa, Actions.GET_CONFIDENTIAL_RESERVATION)).thenReturn(true);
		when(snaa.isAuthorized(user2SaksSnaa, Actions.GET_CONFIDENTIAL_RESERVATION)).thenReturn(true);
		when(servedNodeUrns.get()).thenReturn(new String[]{user1Node, user2Node});

		final ConfidentialReservationData reservation1 = buildConfidentialReservationData(from, to, USER1_USERNAME, user1Node);
		final ConfidentialReservationData reservation2 = buildConfidentialReservationData(from, to, USER2_USERNAME, user2Node);
		when(persistence.getReservations(Matchers.<Interval>any())).thenReturn(
				newArrayList(reservation1, reservation2)
		);

		final List<ConfidentialReservationData> user1Reservations = rs.getConfidentialReservations(
				user1Saks,
				buildPeriod(from, to)
		);

		assertEquals(1, user1Reservations.size());
		assertEquals(1, user1Reservations.get(0).getNodeURNs().size());
		assertEquals(user1Node, user1Reservations.get(0).getNodeURNs().get(0));

		final List<ConfidentialReservationData> user2Reservations = rs.getConfidentialReservations(
				user2Saks,
				buildPeriod(from, to)
		);

		assertEquals(1, user2Reservations.size());
		assertEquals(1, user2Reservations.get(0).getNodeURNs().size());
		assertEquals(user2Node, user2Reservations.get(0).getNodeURNs().get(0));
	}

	private GetReservations buildPeriod(final DateTime from, final DateTime to) {
		GetReservations gr = new GetReservations();
		gr.setFrom(datatypeFactory.newXMLGregorianCalendar(from.toGregorianCalendar()));
		gr.setTo(datatypeFactory.newXMLGregorianCalendar(to.toGregorianCalendar()));
		return gr;
	}

	private ConfidentialReservationData buildConfidentialReservationData(final DateTime from, final DateTime to,
																		 final String username,
																		 final String... nodeUrns) {
		final ConfidentialReservationData crd = new ConfidentialReservationData();
		crd.setFrom(datatypeFactory.newXMLGregorianCalendar(from.toGregorianCalendar()));
		crd.setTo(datatypeFactory.newXMLGregorianCalendar(to.toGregorianCalendar()));
		crd.setUserData(username);
		for (String nodeUrn : nodeUrns) {
			crd.getNodeURNs().add(nodeUrn);
		}
		return crd;
	}

}
