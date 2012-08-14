package de.uniluebeck.itm.tr.rs;


import com.google.common.collect.Lists;
import com.google.inject.*;
import com.google.inject.name.Names;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.rs.singleurnprefix.SingleUrnPrefixRS;
import de.uniluebeck.itm.tr.rs.singleurnprefix.SingleUrnPrefixSOAPRS;
import eu.wisebed.api.common.SecretAuthenticationKey;
import eu.wisebed.api.common.SecretReservationKey;
import eu.wisebed.api.common.UsernameNodeUrnsMap;
import eu.wisebed.api.rs.*;
import eu.wisebed.api.sm.SessionManagement;
import eu.wisebed.api.snaa.Action;
import eu.wisebed.api.snaa.AuthorizationResponse;
import eu.wisebed.api.snaa.IsValidResponse.ValidationResult;
import eu.wisebed.api.snaa.SNAA;
import eu.wisebed.api.util.WisebedConversionHelper;
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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.inject.matcher.Matchers.annotatedWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
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

	private List<SecretAuthenticationKey> user1SaksSnaa;

	private List<SecretAuthenticationKey> user2SaksSnaa;

	@Before
	public void setUp() {

		final Injector injector = Guice.createInjector(new Module() {
			@Override
			public void configure(final Binder binder) {

				binder.bind(String.class).annotatedWith(Names.named("SingleUrnPrefixSOAPRS.urnPrefix"))
						.toInstance(URN_PREFIX);

				binder.bind(SNAA.class)
						.toInstance(snaa);

				binder.bind(SessionManagement.class)
						.toInstance(sessionManagement);

				binder.bind(RSPersistence.class)
						.toInstance(persistence);

				binder.bind(String[].class)
						.annotatedWith(Names.named("SingleUrnPrefixSOAPRS.servedNodeUrns"))
						.toProvider(servedNodeUrns);

				binder.bind(RS.class)
						.to(SingleUrnPrefixSOAPRS.class);

				binder.bind(RS.class)
						.annotatedWith(NonWS.class)
						.to(SingleUrnPrefixRS.class);

				binder.bindInterceptor(com.google.inject.matcher.Matchers.any(),
						annotatedWith(AuthorizationRequired.class), new RSAuthorizationInterceptor(snaa)
				);
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

		user1SaksSnaa = copyRsToSnaa(user1Saks);
		user2SaksSnaa = copyRsToSnaa(user2Saks);

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

		AuthorizationResponse successfulAuthorizationResponse = new AuthorizationResponse();
		successfulAuthorizationResponse.setAuthorized(true);

		List<UsernameNodeUrnsMap> usernameNodeUrnsMap =
				WisebedConversionHelper.convertToUsernameNodeUrnsMap(
						WisebedConversionHelper.convert(user1SaksSnaa),
						new LinkedList<String>()
				);

		when(snaa.isAuthorized(usernameNodeUrnsMap, Action.RS_DELETE_RESERVATION))
				.thenReturn(successfulAuthorizationResponse);
		when(persistence.getReservation(user1Srk)).thenReturn(crd);

		try {
			rs.deleteReservation(user1Srks);
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

		AuthorizationResponse successfulAuthorizationResponse = new AuthorizationResponse();
		successfulAuthorizationResponse.setAuthorized(true);

		List<UsernameNodeUrnsMap> usernameNodeUrnsMap =
				WisebedConversionHelper.convertToUsernameNodeUrnsMap(
						WisebedConversionHelper.convert(user1SaksSnaa),
						new LinkedList<String>()
				);

		when(snaa.isAuthorized(usernameNodeUrnsMap, Action.RS_GET_RESERVATIONS))
				.thenReturn(successfulAuthorizationResponse);
		when(persistence.getReservation(user1Srk)).thenReturn(crd);

		when(snaa.isAuthorized(usernameNodeUrnsMap, Action.RS_DELETE_RESERVATION))
				.thenReturn(successfulAuthorizationResponse);
		when(persistence.deleteReservation(user1Srk)).thenReturn(crd);

		rs.deleteReservation(user1Srks);

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
		persistenceCrd.getNodeUrns().add("urn:local:0xcbe4");
		reservedNodes.add(persistenceCrd);

		AuthorizationResponse successfulAuthorizationResponse = new AuthorizationResponse();
		successfulAuthorizationResponse.setAuthorized(true);

		List<UsernameNodeUrnsMap> usernameNodeUrnsMapUpperCase =
				WisebedConversionHelper.convertToUsernameNodeUrnsMap(
						WisebedConversionHelper.convert(user1SaksSnaa),
						Arrays.asList("urn:local:0xCBE4")
				);


		List<UsernameNodeUrnsMap> usernameNodeUrnsMapLowerCase =
				WisebedConversionHelper.convertToUsernameNodeUrnsMap(
						WisebedConversionHelper.convert(user1SaksSnaa),
						Arrays.asList("urn:local:0xcbe4")
				);

//		when(snaa.isAuthorized(Matchers.<List<UsernameNodeUrnsMap>>any(), eq(Action.RS_MAKE_RESERVATION))).thenReturn(successfulAuthorizationResponse);
		when(snaa.isAuthorized(usernameNodeUrnsMapUpperCase, Action.RS_MAKE_RESERVATION))
				.thenReturn(successfulAuthorizationResponse);
		when(snaa.isAuthorized(usernameNodeUrnsMapLowerCase, Action.RS_MAKE_RESERVATION))
				.thenReturn(successfulAuthorizationResponse);
		when(servedNodeUrns.get()).thenReturn(new String[]{"urn:local:0xcbe4"});
		when(persistence.addReservation(Matchers.<ConfidentialReservationData>any(), eq("urn:local:"))).thenReturn(srk);
		when(persistence.getReservations(Matchers.<Interval>any())).thenReturn(reservedNodes);

		// try to reserve in uppercase
		ConfidentialReservationData crd =
				buildConfidentialReservationData(from, to, USER1_USERNAME, null, "urn:local:0xCBE4");
		try {
			rs.makeReservation(user1Saks, crd);
			fail();
		} catch (AuthorizationExceptionException e) {
			fail();
		} catch (RSExceptionException e) {
			fail();
		} catch (ReservationConflictExceptionException expected) {
		}

		// try to reserve in lowercase
		crd = buildConfidentialReservationData(from, to, USER1_USERNAME, null, "urn:local:0xcbe4");
		try {
			rs.makeReservation(user1Saks, crd);
			fail();
		} catch (AuthorizationExceptionException e) {
			fail();
		} catch (RSExceptionException e) {
			fail();
		} catch (ReservationConflictExceptionException expected) {
		}

	}

	/**
	 * Given there are reservations by more than one user, the RS should only return reservations of the authenticated
	 * user
	 * when {@link RS#getConfidentialReservations(java.util.List, eu.wisebed.api.rs.GetReservations)} is called.
	 *
	 * @throws Exception
	 * 		if anything goes wrong
	 */
	@Test
	public void testOnlyConfidentialReservationsOfAuthenticatedUserReturned() throws Exception {

		final DateTime from = new DateTime().plusHours(1);
		final DateTime to = from.plusHours(1);

		final String user1Node = URN_PREFIX + "0x1234";
		final String user2Node = URN_PREFIX + "0x4321";


		AuthorizationResponse successfulAuthorizationResponse = new AuthorizationResponse();
		successfulAuthorizationResponse.setAuthorized(true);

		List<UsernameNodeUrnsMap> usernameNodeUrnsMap =
				WisebedConversionHelper.convertToUsernameNodeUrnsMap(
						WisebedConversionHelper.convert(user1SaksSnaa),
						new LinkedList<String>()
				);

		ValidationResult validationResult = new ValidationResult();
		validationResult.setValid(true);

		when(snaa.isAuthorized(usernameNodeUrnsMap, Action.RS_GET_RESERVATIONS))
				.thenReturn(successfulAuthorizationResponse);
		when(snaa.isAuthorized(usernameNodeUrnsMap, Action.RS_GET_RESERVATIONS))
				.thenReturn(successfulAuthorizationResponse);
		when(snaa.isValid(user1Saks.get(0))).thenReturn(validationResult);
		when(snaa.isValid(user2Saks.get(0))).thenReturn(validationResult);
		when(servedNodeUrns.get()).thenReturn(new String[]{user1Node, user2Node});

		final ConfidentialReservationData reservation1 =
				buildConfidentialReservationData(from, to, USER1_USERNAME, USER1_SECRET_RESERVATION_KEY, user1Node);
		final ConfidentialReservationData reservation2 =
				buildConfidentialReservationData(from, to, USER2_USERNAME, USER2_SECRET_RESERVATION_KEY, user2Node);
		when(persistence.getReservations(Matchers.<Interval>any())).thenReturn(
				newArrayList(reservation1, reservation2)
		);

		final List<ConfidentialReservationData> user1Reservations = rs.getConfidentialReservations(
				user1Saks,
				buildPeriod(from, to)
		);

		assertEquals(1, user1Reservations.size());
		assertEquals(1, user1Reservations.get(0).getNodeUrns().size());
		assertEquals(user1Node, user1Reservations.get(0).getNodeUrns().get(0));

		final List<ConfidentialReservationData> user2Reservations = rs.getConfidentialReservations(
				user2Saks,
				buildPeriod(from, to)
		);

		assertEquals(1, user2Reservations.size());
		assertEquals(1, user2Reservations.get(0).getNodeUrns().size());
		assertEquals(user2Node, user2Reservations.get(0).getNodeUrns().get(0));
	}

	private GetReservations buildPeriod(final DateTime from, final DateTime to) {
		GetReservations gr = new GetReservations();
		gr.setFrom(datatypeFactory.newXMLGregorianCalendar(from.toGregorianCalendar()));
		gr.setTo(datatypeFactory.newXMLGregorianCalendar(to.toGregorianCalendar()));
		return gr;
	}

	private ConfidentialReservationData buildConfidentialReservationData(final DateTime from, final DateTime to,
																		 final String username,
																		 final String secretReservationKey,
																		 final String... nodeUrns) {
		final ConfidentialReservationData crd = new ConfidentialReservationData();

		crd.setFrom(datatypeFactory.newXMLGregorianCalendar(from.toGregorianCalendar()));
		crd.setTo(datatypeFactory.newXMLGregorianCalendar(to.toGregorianCalendar()));
		crd.setUserData(new Random().nextInt(Integer.MAX_VALUE) + "");

		for (String nodeUrn : nodeUrns) {
			crd.getNodeUrns().add(nodeUrn);
		}

		Data data = new Data();

		data.setSecretReservationKey(secretReservationKey);
		data.setUrnPrefix(URN_PREFIX);
		data.setUsername(username);

		crd.getData().add(data);

		return crd;
	}

	// TODO
	private static List<SecretAuthenticationKey> copyRsToSnaa(
			List<SecretAuthenticationKey> snaaKeys) {

		List<SecretAuthenticationKey> secretAuthKeys =
				Lists.newArrayListWithCapacity(snaaKeys.size());

		for (SecretAuthenticationKey snaaKey : snaaKeys) {

			SecretAuthenticationKey key = new SecretAuthenticationKey();

			key.setSecretAuthenticationKey(snaaKey.getSecretAuthenticationKey());
			key.setUrnPrefix(snaaKey.getUrnPrefix());
			key.setUsername(snaaKey.getUsername());

			secretAuthKeys.add(key);
		}

		return secretAuthKeys;
	}

}
