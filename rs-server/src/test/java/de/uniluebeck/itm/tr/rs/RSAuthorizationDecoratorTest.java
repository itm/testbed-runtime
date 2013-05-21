package de.uniluebeck.itm.tr.rs;

import eu.wisebed.api.v3.common.*;
import eu.wisebed.api.v3.rs.AuthorizationFault;
import eu.wisebed.api.v3.rs.*;
import eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault;
import eu.wisebed.api.v3.snaa.Action;
import eu.wisebed.api.v3.snaa.AuthorizationResponse;
import eu.wisebed.api.v3.snaa.SNAA;
import eu.wisebed.api.v3.snaa.SNAAFault_Exception;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RSAuthorizationDecoratorTest {

	private static final AuthorizationResponse GRANTED_RESPONSE = new AuthorizationResponse();

	private static final AuthorizationResponse DENIED_RESPONSE = new AuthorizationResponse();

	static {
		GRANTED_RESPONSE.setAuthorized(true);
		DENIED_RESPONSE.setAuthorized(false);
	}

	private static final ArrayList<SecretAuthenticationKey> EMPTY_SAK_LIST = newArrayList();

	private static final ArrayList<SecretReservationKey> EMPTY_SRK_LIST = newArrayList();

	private static final DateTime NOW = DateTime.now();

	private static final DateTime THEN = DateTime.now().plusHours(1);

	private static final List<NodeUrn> NODE_URNS = newArrayList(
			new NodeUrn("urn:wisebed:uzl1:0x1234"),
			new NodeUrn("urn:wisebed:uzl1:0x4321")
	);

	private static final String DESCRIPTION = "BlaBlub";

	private static final List<KeyValuePair> OPTIONS = newArrayList();

	@Mock
	private SNAA snaa;

	@Mock
	private Object service;

	@Mock
	private RS rsImpl;

	private RS rs;

	@Before
	public void setUp() throws Exception {
		rs = new RSAuthorizationDecorator(rsImpl, snaa);
	}

	@Test
	public void testDeleteReservationGranted()
			throws SNAAFault_Exception, RSFault_Exception, UnknownSecretReservationKeyFault, AuthorizationFault {

		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), eq(Action.RS_DELETE_RESERVATION)))
				.thenReturn(GRANTED_RESPONSE);

		rs.deleteReservation(EMPTY_SAK_LIST, EMPTY_SRK_LIST);

		verify(rsImpl).deleteReservation(
				Matchers.<List<SecretAuthenticationKey>>any(),
				Matchers.<List<SecretReservationKey>>any()
		);
	}

	@Test
	public void testDeleteReservationDenied()
			throws SNAAFault_Exception, RSFault_Exception, UnknownSecretReservationKeyFault {

		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), eq(Action.RS_DELETE_RESERVATION)))
				.thenReturn(DENIED_RESPONSE);

		try {
			rs.deleteReservation(EMPTY_SAK_LIST, EMPTY_SRK_LIST);
			fail("Exception should have been thrown");
		} catch (AuthorizationFault expected) {
			verifyZeroInteractions(rsImpl);
		}
	}

	@Test
	public void testGetReservationsGranted() throws SNAAFault_Exception, RSFault_Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), eq(Action.RS_GET_RESERVATIONS)))
				.thenReturn(GRANTED_RESPONSE);
		rs.getReservations(NOW, THEN);
		verify(rsImpl).getReservations(NOW, THEN);
	}

	@Test
	public void testGetConfidentialReservationsGranted()
			throws RSFault_Exception, SNAAFault_Exception, AuthorizationFault {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), eq(Action.RS_GET_CONFIDENTIAL_RESERVATIONS)))
				.thenReturn(GRANTED_RESPONSE);
		rs.getConfidentialReservations(EMPTY_SAK_LIST, NOW, THEN);
		verify(rsImpl).getConfidentialReservations(Matchers.<List<SecretAuthenticationKey>>any(), eq(NOW), eq(THEN));
	}

	@Test
	public void testGetConfidentialReservationsDenied()
			throws RSFault_Exception, SNAAFault_Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), eq(Action.RS_GET_CONFIDENTIAL_RESERVATIONS)))
				.thenReturn(DENIED_RESPONSE);
		try {
			rs.getConfidentialReservations(EMPTY_SAK_LIST, NOW, THEN);
			fail("Exception should have been thrown");
		} catch (AuthorizationFault authorizationFault) {
			verifyZeroInteractions(rsImpl);
		}
	}

	@Test
	public void testGetReservationGranted()
			throws SNAAFault_Exception, RSFault_Exception, UnknownSecretReservationKeyFault {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), eq(Action.RS_GET_RESERVATION)))
				.thenReturn(GRANTED_RESPONSE);
		rs.getReservation(EMPTY_SRK_LIST);
		verify(rsImpl).getReservation(EMPTY_SRK_LIST);
	}

	@Test
	public void testMakeReservationGranted()
			throws RSFault_Exception, AuthorizationFault, ReservationConflictFault_Exception,
			UnknownSecretReservationKeyFault, SNAAFault_Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), eq(Action.RS_MAKE_RESERVATION)))
				.thenReturn(GRANTED_RESPONSE);
		rs.makeReservation(EMPTY_SAK_LIST, NODE_URNS, NOW, THEN, DESCRIPTION, OPTIONS);
		verify(rsImpl).makeReservation(EMPTY_SAK_LIST, NODE_URNS, NOW, THEN, DESCRIPTION, OPTIONS);
	}

	@Test
	public void testMakeReservationDenied()
			throws RSFault_Exception, ReservationConflictFault_Exception,
			UnknownSecretReservationKeyFault, SNAAFault_Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), eq(Action.RS_MAKE_RESERVATION)))
				.thenReturn(DENIED_RESPONSE);
		try {
			rs.makeReservation(EMPTY_SAK_LIST, NODE_URNS, NOW, THEN, DESCRIPTION, OPTIONS);
			fail("Exception should have been thrown");
		} catch (AuthorizationFault authorizationFault) {
			verifyZeroInteractions(rsImpl);
		}
	}
}
