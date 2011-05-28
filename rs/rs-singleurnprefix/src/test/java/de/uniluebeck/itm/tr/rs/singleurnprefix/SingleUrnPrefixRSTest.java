package de.uniluebeck.itm.tr.rs.singleurnprefix;


import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.wisebed.cmdlineclient.BeanShellHelper;
import eu.wisebed.api.rs.*;
import eu.wisebed.api.rs.SecretAuthenticationKey;
import eu.wisebed.api.sm.SessionManagement;
import eu.wisebed.api.snaa.*;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import sun.security.provider.certpath.Vertex;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.List;

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

	private static final String SECRET_RESERVATION_KEY = "12345";

	private static final String SECRET_AUTHENTICATION_KEY = "abcdef";

	private static final String USERNAME = "user@domain.tld";

	@Mock
	private RSPersistence persistence;

	@Mock
	private SNAA snaa;

	@Mock
	private SessionManagement sessionManagement;

	private RS rs;

	private SecretAuthenticationKey sak;

	private List<SecretAuthenticationKey> saks;

	private SecretReservationKey srk;

	private List<SecretReservationKey> srks;

	private List<eu.wisebed.api.snaa.SecretAuthenticationKey> saksSnaa;

	@Before
	public void setUp() {

		final Injector injector = Guice.createInjector(new Module() {
			@Override
			public void configure(final Binder binder) {

				binder.bind(String.class).annotatedWith(Names.named("SingleUrnPrefixRS.urnPrefix")).toInstance(URN_PREFIX);
				binder.bind(SNAA.class).toInstance(snaa);
				binder.bind(SessionManagement.class).toInstance(sessionManagement);
				binder.bind(RSPersistence.class).toInstance(persistence);

				binder.bind(RS.class).to(SingleUrnPrefixRS.class);
			}
		}
		);

		rs = injector.getInstance(RS.class);

		sak = new SecretAuthenticationKey();
		sak.setSecretAuthenticationKey(SECRET_AUTHENTICATION_KEY);
		sak.setUrnPrefix(URN_PREFIX);
		sak.setUsername(USERNAME);

		saks = Lists.newArrayList(sak);

		saksSnaa = BeanShellHelper.copyRsToSnaa(saks);

		srk = new SecretReservationKey();
		srk.setSecretReservationKey(SECRET_RESERVATION_KEY);
		srk.setUrnPrefix(URN_PREFIX);

		srks = Lists.newArrayList(srk);
	}

	@Test
	public void throwExceptionWhenTryingToDeleteReservationThatAlreadyStarted() throws Exception {

		final DateTime from = new DateTime().minusSeconds(1);
		final DateTime to = from.plusHours(1);

		final ConfidentialReservationData crd = new ConfidentialReservationData();
		crd.setFrom(datatypeFactory.newXMLGregorianCalendar(from.toGregorianCalendar()));
		crd.setTo(datatypeFactory.newXMLGregorianCalendar(to.toGregorianCalendar()));
		crd.setUserData(USERNAME);

		when(snaa.isAuthorized(saksSnaa, Actions.DELETE_RESERVATION)).thenReturn(true);
		when(persistence.getReservation(srk)).thenReturn(crd);

		try {
			rs.deleteReservation(saks, srks);
			fail();
		} catch (RSExceptionException e) {
			// this should be thrown
		}

		verify(snaa).isAuthorized(saksSnaa, Actions.DELETE_RESERVATION);
		verify(persistence).getReservation(srk);

		verify(persistence, never()).deleteReservation(srk);
	}

	@Test
	public void throwNoExceptionWhenTryingToDeleteReservationInTheFuture() throws Exception {

		final DateTime from = new DateTime().plusHours(1);
		final DateTime to = from.plusHours(1);

		final ConfidentialReservationData crd = new ConfidentialReservationData();
		crd.setFrom(datatypeFactory.newXMLGregorianCalendar(from.toGregorianCalendar()));
		crd.setTo(datatypeFactory.newXMLGregorianCalendar(to.toGregorianCalendar()));
		crd.setUserData(USERNAME);

		when(snaa.isAuthorized(saksSnaa, Actions.GET_CONFIDENTIAL_RESERVATION)).thenReturn(true);
		when(persistence.getReservation(srk)).thenReturn(crd);

		when(snaa.isAuthorized(saksSnaa, Actions.DELETE_RESERVATION)).thenReturn(true);
		when(persistence.deleteReservation(srk)).thenReturn(crd);

		rs.deleteReservation(saks, srks);

		verify(snaa).isAuthorized(saksSnaa, Actions.DELETE_RESERVATION);

		verify(persistence).getReservation(srk);
		verify(persistence).deleteReservation(srk);
	}

}
