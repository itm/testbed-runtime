package de.uniluebeck.itm.tr.rs;

import eu.wisebed.api.v3.common.*;
import eu.wisebed.api.v3.rs.AuthenticationFault;
import eu.wisebed.api.v3.rs.AuthorizationFault;
import eu.wisebed.api.v3.rs.*;
import eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault;
import eu.wisebed.api.v3.snaa.*;
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

	private static final ValidationResult VALIDATION_RESULT_VALID = new ValidationResult();

	private static final ValidationResult VALIDATION_RESULT_INVALID = new ValidationResult();

	static {
		VALIDATION_RESULT_VALID.setValid(true);
		VALIDATION_RESULT_INVALID.setValid(false);
	}

	private static final List<ValidationResult> VALIDATION_RESULTS_VALID = newArrayList(VALIDATION_RESULT_VALID);

	private static final List<ValidationResult> VALIDATION_RESULTS_INVALID = newArrayList(VALIDATION_RESULT_INVALID);

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
			throws SNAAFault_Exception, RSFault_Exception, UnknownSecretReservationKeyFault, AuthorizationFault,
			AuthenticationFault {

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
			throws SNAAFault_Exception, RSFault_Exception, UnknownSecretReservationKeyFault, AuthorizationFault,
			AuthenticationFault {

		when(snaa.isValid(anyListOf(SecretAuthenticationKey.class))).thenReturn(VALIDATION_RESULTS_VALID);
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), eq(Action.RS_DELETE_RESERVATION)))
				.thenReturn(DENIED_RESPONSE);

		try {
			rs.deleteReservation(EMPTY_SAK_LIST, EMPTY_SRK_LIST);
			fail("Exception should have been thrown");
		} catch (AuthorizationFault expected) {
			verify(rsImpl, never()).deleteReservation(
					Matchers.<List<SecretAuthenticationKey>>any(),
					Matchers.<List<SecretReservationKey>>any()
			);
		}
	}

	@Test
	public void testGetReservationsGranted() throws SNAAFault_Exception, RSFault_Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), eq(Action.RS_GET_RESERVATIONS)))
				.thenReturn(GRANTED_RESPONSE);
		rs.getReservations(NOW, THEN, null, null);
		verify(rsImpl).getReservations(NOW, THEN, null, null);
	}

	@Test
	public void testGetConfidentialReservationsGranted()
			throws RSFault_Exception, SNAAFault_Exception, AuthorizationFault, AuthenticationFault {

		when(snaa.isValid(anyListOf(SecretAuthenticationKey.class))).thenReturn(VALIDATION_RESULTS_VALID);
		when(snaa.isValid(EMPTY_SAK_LIST)).thenReturn(VALIDATION_RESULTS_VALID);

		rs.getConfidentialReservations(EMPTY_SAK_LIST, NOW, THEN, null, null);

		verify(rsImpl).getConfidentialReservations(
				Matchers.<List<SecretAuthenticationKey>>any(),
				eq(NOW),
				eq(THEN),
				isNull(Integer.class),
				isNull(Integer.class)
		);
	}

	@Test
	public void testGetConfidentialReservationsDenied()
			throws RSFault_Exception, SNAAFault_Exception, AuthenticationFault {

		when(snaa.isValid(anyListOf(SecretAuthenticationKey.class))).thenReturn(VALIDATION_RESULTS_VALID);
		when(snaa.isValid(EMPTY_SAK_LIST)).thenReturn(VALIDATION_RESULTS_INVALID);
		try {
			rs.getConfidentialReservations(EMPTY_SAK_LIST, NOW, THEN, null, null);
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
			UnknownSecretReservationKeyFault, SNAAFault_Exception, AuthenticationFault {

		when(snaa.isValid(anyListOf(SecretAuthenticationKey.class))).thenReturn(VALIDATION_RESULTS_VALID);
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), eq(Action.RS_MAKE_RESERVATION)))
				.thenReturn(GRANTED_RESPONSE);

		rs.makeReservation(EMPTY_SAK_LIST, NODE_URNS, NOW, THEN, DESCRIPTION, OPTIONS);

		verify(rsImpl).makeReservation(EMPTY_SAK_LIST, NODE_URNS, NOW, THEN, DESCRIPTION, OPTIONS);
	}

	@Test
	public void testMakeReservationDenied()
			throws RSFault_Exception, ReservationConflictFault_Exception,
			UnknownSecretReservationKeyFault, SNAAFault_Exception, AuthenticationFault {

		when(snaa.isValid(anyListOf(SecretAuthenticationKey.class))).thenReturn(VALIDATION_RESULTS_VALID);
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
