package de.uniluebeck.itm.tr.rs;


import com.google.common.collect.Lists;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.EndpointManager;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import eu.wisebed.api.v3.common.*;
import eu.wisebed.api.v3.rs.AuthorizationFault;
import eu.wisebed.api.v3.rs.*;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.snaa.Action;
import eu.wisebed.api.v3.snaa.AuthorizationResponse;
import eu.wisebed.api.v3.snaa.SNAA;
import eu.wisebed.api.v3.snaa.ValidationResult;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static eu.wisebed.api.v3.util.WisebedConversionHelper.convertToUsernameNodeUrnsMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SingleUrnPrefixRSTest {

	private static final NodeUrnPrefix URN_PREFIX = new NodeUrnPrefix("urn:local:");

	private static final String USER1_SECRET_RESERVATION_KEY = "12345";

	private static final String USER2_SECRET_RESERVATION_KEY = "54321";

	private static final String USER1_SECRET_AUTHENTICATION_KEY = "abcdef";

	private static final String USER2_SECRET_AUTHENTICATION_KEY = "fedcba";

	private static final String USER1_USERNAME = "user1@domain.tld";

	private static final String USER2_USERNAME = "user2@otherdomain.tld";

	private static final String USER1_DESCRIPTION = "Hello, World";

	private static final SecretAuthenticationKey USER1_SAK;

	private static final List<SecretAuthenticationKey> USER1_SAKS;

	static {
		USER1_SAK = new SecretAuthenticationKey();
		USER1_SAK.setKey(USER1_SECRET_AUTHENTICATION_KEY);
		USER1_SAK.setUrnPrefix(URN_PREFIX);
		USER1_SAK.setUsername(USER1_USERNAME);
		USER1_SAKS = Lists.newArrayList(USER1_SAK);
	}

	private static final SecretAuthenticationKey USER2_SAK;

	private static final List<SecretAuthenticationKey> USER2_SAKS;

	static {
		USER2_SAK = new SecretAuthenticationKey();
		USER2_SAK.setKey(USER2_SECRET_AUTHENTICATION_KEY);
		USER2_SAK.setUrnPrefix(URN_PREFIX);
		USER2_SAK.setUsername(USER2_USERNAME);
		USER2_SAKS = Lists.newArrayList(USER2_SAK);
	}

	private static final SecretReservationKey USER1_SRK;

	private static final List<SecretReservationKey> USER1_SRKS;

	static {
		USER1_SRK = new SecretReservationKey();
		USER1_SRK.setKey(USER1_SECRET_RESERVATION_KEY);
		USER1_SRK.setUrnPrefix(URN_PREFIX);
		USER1_SRKS = Lists.newArrayList(USER1_SRK);
	}

	private static final SecretReservationKey USER2_SRK;

	@SuppressWarnings("UnusedDeclaration")
	private static final List<SecretReservationKey> USER2_SRKS;

	static {
		USER2_SRK = new SecretReservationKey();
		USER2_SRK.setKey(USER2_SECRET_RESERVATION_KEY);
		USER2_SRK.setUrnPrefix(URN_PREFIX);
		USER2_SRKS = Lists.newArrayList(USER2_SRK);
	}

	@Mock
	private RSPersistence persistence;

	@Mock
	private SNAA snaa;

	@Mock
	private Provider<SNAA> snaaProvider;

	@Mock
	private SessionManagement sessionManagement;

	@Mock
	private ServicePublisher servicePublisher;

	@Mock
	private ServedNodeUrnsProvider servedNodeUrnsProvider;

	@Mock
	private EndpointManager endpointManager;

	@Mock
	private TimeLimiter timeLimiter;

	private RS rs;

	@Before
	public void setUp() {

		when(snaaProvider.get()).thenReturn(snaa);

		final RSStandaloneConfigImpl config = new RSStandaloneConfigImpl();
		config.setUrnPrefix(URN_PREFIX);
		config.setRsPersistenceType(RSPersistenceType.IN_MEMORY);

		final Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			public void configure() {

				bind(ServicePublisher.class).toInstance(servicePublisher);
				bind(SNAA.class).toInstance(snaa);
				bind(SessionManagement.class).toInstance(sessionManagement);
				bind(EndpointManager.class).toInstance(endpointManager);
				bind(TimeLimiter.class).toInstance(timeLimiter);
				bind(ServedNodeUrnsProvider.class).toInstance(servedNodeUrnsProvider);

				install(new RSModule(config));
			}
		}
		);

		rs = injector.getInstance(RS.class);
	}

	@Test
	public void throwExceptionWhenTryingToDeleteReservationThatAlreadyStarted() throws Exception {

		final ConfidentialReservationData crd = new ConfidentialReservationData();
		crd.setFrom(DateTime.now().minusSeconds(1));
		crd.setTo(DateTime.now().minusSeconds(1).plusHours(1));

		final AuthorizationResponse authorizationResponse = new AuthorizationResponse();
		authorizationResponse.setAuthorized(true);

		List<UsernameNodeUrnsMap> usernameNodeUrnsMap = convertToUsernameNodeUrnsMap(
				USER1_SAKS,
				Lists.<NodeUrn>newArrayList()
		);

		when(snaa.isAuthorized(usernameNodeUrnsMap, Action.RS_DELETE_RESERVATION)).thenReturn(authorizationResponse);
		when(persistence.getReservation(USER1_SRK)).thenReturn(crd);

		try {
			rs.deleteReservation(USER1_SAKS, USER1_SRKS);
			fail();
		} catch (RSFault_Exception e) {
			// this should be thrown
		}

		// TODO https://github.com/itm/testbed-runtime/issues/47
		//verify(snaa).isAuthorized(user1SaksSnaa, Actions.DELETE_RESERVATION);
		verify(persistence).getReservation(USER1_SRK);

		verify(persistence, never()).deleteReservation(USER1_SRK);
	}

	@Test
	public void throwNoExceptionWhenTryingToDeleteReservationInTheFuture() throws Exception {

		final ConfidentialReservationData crd = new ConfidentialReservationData();
		crd.setFrom(DateTime.now().plusHours(1));
		crd.setTo(DateTime.now().plusHours(2));

		AuthorizationResponse successfulAuthorizationResponse = new AuthorizationResponse();
		successfulAuthorizationResponse.setAuthorized(true);

		List<UsernameNodeUrnsMap> usernameNodeUrnsMap = convertToUsernameNodeUrnsMap(
				USER1_SAKS,
				Lists.<NodeUrn>newArrayList()
		);

		when(snaa.isAuthorized(usernameNodeUrnsMap, Action.RS_GET_RESERVATIONS))
				.thenReturn(successfulAuthorizationResponse);
		when(persistence.getReservation(USER1_SRK)).thenReturn(crd);

		when(snaa.isAuthorized(usernameNodeUrnsMap, Action.RS_DELETE_RESERVATION))
				.thenReturn(successfulAuthorizationResponse);
		when(persistence.deleteReservation(USER1_SRK)).thenReturn(crd);

		rs.deleteReservation(USER1_SAKS, USER1_SRKS);

		// TODO https://github.com/itm/testbed-runtime/issues/47
		//verify(snaa).isAuthorized(user1SaksSnaa, Actions.DELETE_RESERVATION);

		verify(persistence).getReservation(USER1_SRK);
		verify(persistence).deleteReservation(USER1_SRK);
	}

	@Test
	public void reserveSameNodeTwiceInUpperCaseAndLowercase() throws Exception {

		final DateTime from = new DateTime().plusHours(1);
		final DateTime to = from.plusHours(1);

		final ConfidentialReservationData confidentialReservationData = new ConfidentialReservationData();
		confidentialReservationData.getNodeUrns().add(new NodeUrn("urn:local:0xcbe4"));
		confidentialReservationData.setFrom(from);
		confidentialReservationData.setTo(to);
		confidentialReservationData.setUsername(USER1_USERNAME);
		confidentialReservationData.setDescription(USER1_DESCRIPTION);
		confidentialReservationData.setSecretReservationKey(USER1_SRK);

		final List<ConfidentialReservationData> reservedNodes = newArrayList();
		reservedNodes.add(confidentialReservationData);

		AuthorizationResponse successfulAuthorizationResponse = new AuthorizationResponse();
		successfulAuthorizationResponse.setAuthorized(true);

		final ArrayList<NodeUrn> urnsLow = newArrayList(new NodeUrn("urn:local:0xCBE4"));
		final ArrayList<NodeUrn> urnsUp = newArrayList(new NodeUrn("urn:local:0xcbe4"));
		List<UsernameNodeUrnsMap> usernameNodeUrnsMapUpperCase = convertToUsernameNodeUrnsMap(USER1_SAKS, urnsLow);
		List<UsernameNodeUrnsMap> usernameNodeUrnsMapLowerCase = convertToUsernameNodeUrnsMap(USER1_SAKS, urnsUp);

		when(snaa.isAuthorized(usernameNodeUrnsMapUpperCase, Action.RS_MAKE_RESERVATION))
				.thenReturn(successfulAuthorizationResponse);
		when(snaa.isAuthorized(usernameNodeUrnsMapLowerCase, Action.RS_MAKE_RESERVATION))
				.thenReturn(successfulAuthorizationResponse);

		when(servedNodeUrnsProvider.get())
				.thenReturn(newHashSet(new NodeUrn("urn:local:0xcbe4")));

		when(persistence.addReservation(
				Matchers.<List<NodeUrn>>any(),
				eq(from),
				eq(to),
				eq(USER1_USERNAME),
				eq(URN_PREFIX),
				eq("Hello, World"),
				Matchers.<List<KeyValuePair>>any()
		)
		).thenReturn(confidentialReservationData);

		when(persistence.getReservations(Matchers.<Interval>any())).thenReturn(reservedNodes);

		// try to reserve in uppercase
		try {

			rs.makeReservation(USER1_SAKS, newArrayList(new NodeUrn("urn:local:0xCBE4")), from, to, null, null);

			fail();
		} catch (AuthorizationFault e) {
			fail();
		} catch (RSFault_Exception e) {
			fail();
		} catch (ReservationConflictFault_Exception expected) {
		}

		// try to reserve in lowercase
		try {
			rs.makeReservation(USER1_SAKS, newArrayList(new NodeUrn("urn:local:0xcbe4")), from, to, null, null);
			fail();
		} catch (AuthorizationFault e) {
			fail();
		} catch (RSFault_Exception e) {
			fail();
		} catch (ReservationConflictFault_Exception expected) {
		}

	}

	/**
	 * Given there are reservations by more than one user, the RS should only return reservations of the authenticated
	 * user when {@link eu.wisebed.api.v3.rs.RS#getConfidentialReservations(java.util.List, org.joda.time.DateTime,
	 * org.joda.time.DateTime)} is called.
	 *
	 * @throws Exception
	 * 		if anything goes wrong
	 */
	@Test
	public void testOnlyConfidentialReservationsOfAuthenticatedUserReturned() throws Exception {

		final DateTime from = new DateTime().plusHours(1);
		final DateTime to = from.plusHours(1);

		final NodeUrn user1Node = new NodeUrn(URN_PREFIX + "0x1234");
		final NodeUrn user2Node = new NodeUrn(URN_PREFIX + "0x4321");

		AuthorizationResponse successfulAuthorizationResponse = new AuthorizationResponse();
		successfulAuthorizationResponse.setAuthorized(true);

		List<UsernameNodeUrnsMap> usernameNodeUrnsMap = convertToUsernameNodeUrnsMap(
				USER1_SAKS,
				Lists.<NodeUrn>newArrayList()
		);

		ValidationResult validationResult = new ValidationResult();
		validationResult.setValid(true);

		when(snaa.isAuthorized(usernameNodeUrnsMap, Action.RS_GET_RESERVATIONS))
				.thenReturn(successfulAuthorizationResponse);
		when(snaa.isAuthorized(usernameNodeUrnsMap, Action.RS_GET_RESERVATIONS))
				.thenReturn(successfulAuthorizationResponse);
		when(snaa.isValid(USER1_SAKS)).thenReturn(newArrayList(validationResult));
		when(snaa.isValid(USER2_SAKS)).thenReturn(newArrayList(validationResult));
		when(servedNodeUrnsProvider.get()).thenReturn(newHashSet(user1Node, user2Node));

		final ConfidentialReservationData reservation1 =
				buildConfidentialReservationData(from, to, USER1_USERNAME, USER1_SECRET_RESERVATION_KEY, user1Node);
		final ConfidentialReservationData reservation2 =
				buildConfidentialReservationData(from, to, USER2_USERNAME, USER2_SECRET_RESERVATION_KEY, user2Node);
		when(persistence.getReservations(Matchers.<Interval>any())).thenReturn(
				newArrayList(reservation1, reservation2)
		);

		final List<ConfidentialReservationData> user1Reservations =
				rs.getConfidentialReservations(USER1_SAKS, from, to);

		assertEquals(1, user1Reservations.size());
		assertEquals(1, user1Reservations.get(0).getNodeUrns().size());
		assertEquals(user1Node, user1Reservations.get(0).getNodeUrns().get(0));

		final List<ConfidentialReservationData> user2Reservations =
				rs.getConfidentialReservations(USER2_SAKS, from, to);

		assertEquals(1, user2Reservations.size());
		assertEquals(1, user2Reservations.get(0).getNodeUrns().size());
		assertEquals(user2Node, user2Reservations.get(0).getNodeUrns().get(0));
	}

	@Test
	public void verifyNoReservationIsPerformedIfSNAARefusesAuthorization() throws Exception {

		final DateTime from = new DateTime().plusHours(1);
		final DateTime to = from.plusHours(1);

		final SecretReservationKey srk = new SecretReservationKey();
		srk.setKey("abc123");

		final List<ConfidentialReservationData> reservedNodes = newArrayList();
		ConfidentialReservationData persistenceCrd = new ConfidentialReservationData();
		persistenceCrd.getNodeUrns().add(new NodeUrn("urn:local:0xcbe4"));
		reservedNodes.add(persistenceCrd);

		AuthorizationResponse unsuccessfulAuthorizationResponse = new AuthorizationResponse();
		unsuccessfulAuthorizationResponse.setAuthorized(false);

		List<UsernameNodeUrnsMap> usernameNodeUrnsMap = convertToUsernameNodeUrnsMap(
				USER1_SAKS,
				newArrayList(new NodeUrn("urn:local:0xcbe4"))
		);

		when(snaa.isAuthorized(usernameNodeUrnsMap, Action.RS_MAKE_RESERVATION))
				.thenReturn(unsuccessfulAuthorizationResponse);

		// try to reserve
		try {

			rs.makeReservation(USER1_SAKS, newArrayList(new NodeUrn("urn:local:0xCBE4")), from, to, null, null);

			fail();
		} catch (AuthorizationFault e) {
			fail();
		} catch (RSFault_Exception expected) {
		} catch (ReservationConflictFault_Exception expected) {
			expected.printStackTrace();
		}

		verify(persistence, never()).addReservation(
				Matchers.<List<NodeUrn>>any(),
				Matchers.<DateTime>any(),
				Matchers.<DateTime>any(),
				Matchers.<String>any(),
				Matchers.<NodeUrnPrefix>any(),
				Matchers.<String>any(),
				Matchers.<List<KeyValuePair>>any()
		);
	}


	private ConfidentialReservationData buildConfidentialReservationData(final DateTime from, final DateTime to,
																		 final String username,
																		 final String secretReservationKey,
																		 final NodeUrn... nodeUrns) {

		final ConfidentialReservationData crd = new ConfidentialReservationData();
		crd.setFrom(from);
		crd.setTo(to);

		for (NodeUrn nodeUrn : nodeUrns) {
			crd.getNodeUrns().add(nodeUrn);
		}

		final SecretReservationKey srk = new SecretReservationKey();
		srk.setKey(secretReservationKey);
		srk.setUrnPrefix(URN_PREFIX);
		crd.setSecretReservationKey(srk);
		crd.setUsername(username);

		return crd;
	}
}
