/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.rs.federator;

import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import de.uniluebeck.itm.tr.rs.federator.FederatorRS;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import eu.wisebed.api.rs.*;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FederatorTest {

	private static final DatatypeFactory datatypeFactory;

	private static final String URN_PREFIX_TESTBED_1 = "urn:wisebed:testbed1";

	private static final String URN_PREFIX_TESTBED_2 = "urn:wisebed:testbed2";

	static {
		try {
			datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	@Mock
	private RS testbed1RS;

	@Mock
	private RS testbed2RS;

	@Mock
	private FederationManager<RS> federatorRSFederationManager;

	/**
	 * The RS under test
	 */
	private RS federatorRS;

	private ExecutorService federatorRSExecutorService;

	@Before
	public void setUp() throws Exception {
		federatorRSExecutorService = Executors.newSingleThreadExecutor();
		federatorRS = new FederatorRS(federatorRSFederationManager, federatorRSExecutorService);
	}

	@After
	public void tearDown() {
		ExecutorUtils.shutdown(federatorRSExecutorService, 0, TimeUnit.SECONDS);
	}

	@Test
	public void testGetReservationsWithNullParameters() {
		try {
			federatorRS.getReservations(null, null);
			fail("Should have raised RSException");
		} catch (RSExceptionException expected) {
		}
	}

	@Test
	public void testGetReservationWithNullParameters() throws Exception {
		try {
			federatorRS.getReservation(null);
			fail("Should have raised RSException");
		} catch (RSExceptionException expected) {
		}
	}

	@Test
	public void testGetReservationWithEmptySecretReservationKeyList() throws Exception {

		when(federatorRSFederationManager.getEndpointByUrnPrefix(URN_PREFIX_TESTBED_1)).thenReturn(testbed1RS);
		when(federatorRSFederationManager.getEndpointByUrnPrefix(URN_PREFIX_TESTBED_2)).thenReturn(testbed2RS);

		try {
			federatorRS.getReservation(newArrayList(new SecretReservationKey()));
			fail("Should have raised RSException");
		} catch (RSExceptionException expected) {
		}
	}

	@Test
	public void testGetReservationWithWrongSecretReservationKey() throws Exception {

		SecretReservationKey key = new SecretReservationKey();
		key.setSecretReservationKey("abcdefghijklmnopqrstuvwxyz");
		key.setUrnPrefix(URN_PREFIX_TESTBED_1);
		final List<SecretReservationKey> secretReservationKeys = newArrayList(key);

		when(federatorRSFederationManager.getEndpointByUrnPrefix(URN_PREFIX_TESTBED_1)).thenReturn(testbed1RS);
		when(testbed1RS.getReservation(secretReservationKeys)).thenThrow(
				new ReservervationNotFoundExceptionException("", new ReservervationNotFoundException())
		);

		try {
			federatorRS.getReservation(secretReservationKeys);
			fail("Should have raised ReservationNotFoundException");
		} catch (ReservervationNotFoundExceptionException expected) {
		}
	}

	@Test
	public void testMakeReservationWithNullParameters() throws Exception {
		try {
			federatorRS.makeReservation(null, null);
			fail("Should have raised an RSExceptionException");
		} catch (RSExceptionException expected) {
		}
	}

	@Test
	public void testMakeReservationWithInvalidParameters() throws Exception {
		try {
			List<SecretAuthenticationKey> data = new LinkedList<SecretAuthenticationKey>();
			federatorRS.makeReservation(data, null);
			fail("Should have raised an RSExceptionException");
		} catch (RSExceptionException expected) {
		}
	}

	@Test
	public void testMakeReservationWithNotServedUrn() throws Exception {
		try {
			List<SecretAuthenticationKey> authData = new LinkedList<SecretAuthenticationKey>();
			ConfidentialReservationData resData = new ConfidentialReservationData();
			resData.getNodeURNs().add("urn:not:served");
			federatorRS.makeReservation(authData, resData);
			fail("Should have raised an RSExceptionException");
		} catch (RSExceptionException expected) {
		}
	}

	@Test
	public void testMakeReservationWithInvalidAuthenticationData() throws Exception {
		try {
			List<SecretAuthenticationKey> authData = new LinkedList<SecretAuthenticationKey>();
			ConfidentialReservationData resData = new ConfidentialReservationData();
			resData.setFrom(createXMLGregorianCalendar(1 * 60 * 1000));
			resData.setTo(createXMLGregorianCalendar(5 * 60 * 1000));
			resData.getNodeURNs().add("urn:wisebed1:testbed1");
			federatorRS.makeReservation(authData, resData);
			fail("Should have raised an RSExceptionException");
		} catch (RSExceptionException expected) {
		}
	}

	@Test
	public void testMakeReservationWithEmptyReservationAndAuthenticationDataReturnsEmptyList() throws Exception {
		try {
			List<SecretAuthenticationKey> authData = new LinkedList<SecretAuthenticationKey>();
			ConfidentialReservationData resData = new ConfidentialReservationData();
			resData.getNodeURNs();
			federatorRS.makeReservation(authData, resData);
			fail();
		} catch (RSExceptionException expected) {
		}
	}

	/**
	 * Tests if the delete call is made on all federated RS instances.
	 *
	 * @throws Exception if anything goes wrong
	 */
	@Test
	public void testDeleteReservation() throws Exception {
		// TODO implement
	}

	/**
	 * Tests if the call of {@link RS#getReservations(javax.xml.datatype.XMLGregorianCalendar,
	 * javax.xml.datatype.XMLGregorianCalendar)} is made on all federated RS instances and the results are merged
	 * correctly.
	 *
	 * @throws Exception if anything goes wrong
	 */
	@Test
	public void testGetReservations() throws Exception {
		// TODO implement
	}

	/**
	 * Tests if the call of {@link RS#getConfidentialReservations(java.util.List, eu.wisebed.api.rs.GetReservations)} is
	 * made on all federated RS instances and the results are merged correctly.
	 *
	 * @throws Exception if anything goes wrong
	 */
	@Test
	public void testGetConfidentialReservations() throws Exception {
		// TODO implement
	}

	private XMLGregorianCalendar createXMLGregorianCalendar(int msInFuture) {
		DateTime dateTime = new DateTime(System.currentTimeMillis());
		return datatypeFactory.newXMLGregorianCalendar(dateTime.plusMillis(msInFuture).toGregorianCalendar());
	}
}
