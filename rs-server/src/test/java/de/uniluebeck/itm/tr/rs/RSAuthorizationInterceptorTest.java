package de.uniluebeck.itm.tr.rs;

import com.google.common.collect.Lists;
import com.google.inject.Provider;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.rs.*;
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

import java.util.List;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RSAuthorizationInterceptorTest {

	private static final AuthorizationResponse GRANTED_RESPONSE = new AuthorizationResponse();

	private static final AuthorizationResponse DENIED_RESPONSE = new AuthorizationResponse();

	static {
		GRANTED_RESPONSE.setAuthorized(true);
		DENIED_RESPONSE.setAuthorized(false);
	}

	@Mock
	private Provider<SNAA> snaaProvider;

	@Mock
	private SNAA snaa;

	@Mock
	private Object service;

	@Mock
	private RS rsImpl;

	private RS rs;

	@Before
	public void setUp() throws Exception {
		when(snaaProvider.get()).thenReturn(snaa);
		rs = new RSAuthorizationDecorator(rsImpl);
	}

	@Test
	public void testDeleteReservationGranted() throws SNAAFault_Exception, RSFault_Exception, UnknownSecretReservationKeyFault {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), Action.RS_DELETE_RESERVATION))
				.thenReturn(GRANTED_RESPONSE);
		rs.deleteReservation(Lists.<SecretReservationKey>newArrayList());
		verify(rsImpl).deleteReservation(Matchers.<List<SecretReservationKey>>any());
	}

	@Test
	public void testDeleteReservationDenied() throws SNAAFault_Exception, RSFault_Exception, UnknownSecretReservationKeyFault {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), Action.RS_DELETE_RESERVATION))
				.thenReturn(DENIED_RESPONSE);
		rs.deleteReservation(Lists.<SecretReservationKey>newArrayList());
		// TODO
		verifyZeroInteractions(rsImpl);
	}

	@Test
	public void testGetReservationsGranted() throws SNAAFault_Exception, RSFault_Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), Action.RS_GET_RESERVATIONS))
				.thenReturn(GRANTED_RESPONSE);
		final DateTime from = DateTime.now();
		final DateTime until = from.plusHours(1);
		rs.getReservations(from, until);
		verify(rsImpl).getReservations(from, until);
	}

	@Test
	public void testGetReservationsDenied() throws SNAAFault_Exception, RSFault_Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), Action.RS_GET_RESERVATIONS))
				.thenReturn(DENIED_RESPONSE);
		final DateTime from = DateTime.now();
		final DateTime until = from.plusHours(1);
		rs.getReservations(from, until);
		// TODO
		fail("Exception should have been thrown");
	}

	@Test
	public List<ConfidentialReservationData> getConfidentialReservations() {
		return null;
	}

	@Test
	public List<ConfidentialReservationData> getReservation() {
		return null;
	}

	@Test
	public List<SecretReservationKey> makeReservation() {
		return null;
	}
}
