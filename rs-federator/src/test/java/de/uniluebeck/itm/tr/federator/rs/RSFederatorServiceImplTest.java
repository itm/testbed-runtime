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

package de.uniluebeck.itm.tr.federator.rs;

import com.google.common.collect.Lists;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.federator.utils.FederationManager;
import de.uniluebeck.itm.util.concurrent.ExecutorUtils;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RSFederatorServiceImplTest {

	private static final NodeUrnPrefix URN_PREFIX_TESTBED_1 = new NodeUrnPrefix("urn:wisebed:testbed1:");

	private static final NodeUrnPrefix URN_PREFIX_TESTBED_2 = new NodeUrnPrefix("urn:wisebed:testbed2:");

	@Mock
	private RS testbed1RS;

	@Mock
	private RS testbed2RS;

	@Mock
	private FederationManager<RS> federatorRSFederationManager;

	@Mock
	private ServicePublisher servicePublisher;

	@Mock
	private ServicePublisherService servicePublisherService;

	@Mock
	private RSFederatorServiceConfig config;

	/**
	 * The RS under test
	 */
	private RSFederatorService federatorRS;

	private ExecutorService federatorRSExecutorService;

	@Before
	public void setUp() throws Exception {
		when(servicePublisher.createJaxWsService(anyString(), anyObject())).thenReturn(servicePublisherService);
		federatorRSExecutorService = Executors.newSingleThreadExecutor();
		federatorRS = new RSFederatorServiceImpl(
				federatorRSExecutorService,
				servicePublisher,
				federatorRSFederationManager,
				config
		);
		federatorRS.startAndWait();
	}

	@After
	public void tearDown() {
		federatorRS.stopAndWait();
		ExecutorUtils.shutdown(federatorRSExecutorService, 0, TimeUnit.SECONDS);
	}

	@Test
	public void testGetReservationsWithNullParameters() {
		try {
			federatorRS.getReservations(null, null, null, null);
			fail("Should have raised RSFault");
		} catch (RSFault_Exception expected) {
		}
	}

	@Test
	public void testGetReservationWithNullParameters() throws Exception {
		try {
			federatorRS.getReservation(null);
			fail("Should have raised RSFault");
		} catch (RSFault_Exception expected) {
		}
	}

	@Test
	public void testGetReservationWithEmptySecretReservationKeyList() throws Exception {

		when(federatorRSFederationManager.getEndpointByUrnPrefix(URN_PREFIX_TESTBED_1)).thenReturn(testbed1RS);
		when(federatorRSFederationManager.getEndpointByUrnPrefix(URN_PREFIX_TESTBED_2)).thenReturn(testbed2RS);

		try {
			federatorRS.getReservation(newArrayList(new SecretReservationKey()));
			fail("Should have raised RSFault");
		} catch (RSFault_Exception expected) {
		}
	}

	@Test
	public void testGetReservationWithWrongSecretReservationKey() throws Exception {

		SecretReservationKey key = new SecretReservationKey();
		key.setKey("abcdefghijklmnopqrstuvwxyz");
		key.setUrnPrefix(URN_PREFIX_TESTBED_1);
		final List<SecretReservationKey> secretReservationKeys = newArrayList(key);

		when(federatorRSFederationManager.getEndpointByUrnPrefix(URN_PREFIX_TESTBED_1)).thenReturn(testbed1RS);
		when(testbed1RS.getReservation(secretReservationKeys)).thenThrow(
				new UnknownSecretReservationKeyFault("",
						new eu.wisebed.api.v3.common.UnknownSecretReservationKeyFault()
				)
		);

		try {

			federatorRS.getReservation(secretReservationKeys);
			fail("Should have raised ReservationNotFoundFault");

		} catch (UnknownSecretReservationKeyFault expected) {
		}
	}

	@Test
	public void testMakeReservationWithNullParameters() throws Exception {
		try {

			federatorRS.makeReservation(null, null, null, null, null, null);
			fail("Should have raised an RSFault_Exception");

		} catch (RSFault_Exception expected) {
		}
	}

	@Test
	public void testMakeReservationWithInvalidParameters() throws Exception {
		try {

			List<SecretAuthenticationKey> data = new LinkedList<SecretAuthenticationKey>();

			federatorRS.makeReservation(data, null, null, null, null, null);
			fail("Should have raised an RSFault_Exception");

		} catch (RSFault_Exception expected) {
		}
	}

	@Test
	public void testMakeReservationWithNotServedUrn() throws Exception {
		try {

			List<SecretAuthenticationKey> authData = new LinkedList<SecretAuthenticationKey>();

			final DateTime from = DateTime.now();
			final DateTime to = DateTime.now().plusHours(1);

			federatorRS.makeReservation(authData, newArrayList(new NodeUrn("urn:not:served:0x1234")), from, to, null,
					null
			);
			fail("Should have raised an RSFault_Exception");

		} catch (RSFault_Exception expected) {
		}
	}

	@Test
	public void testMakeReservationWithInvalidAuthenticationData() throws Exception {
		try {

			List<SecretAuthenticationKey> authData = new LinkedList<SecretAuthenticationKey>();

			final DateTime from = DateTime.now();
			final DateTime to = DateTime.now().plusHours(1);

			federatorRS.makeReservation(authData, newArrayList(new NodeUrn("urn:wisebed1:testbed1:0x1234")), from, to,
					null, null
			);
			fail("Should have raised an RSFault_Exception");

		} catch (RSFault_Exception expected) {
		}
	}

	@Test
	public void testMakeReservationWithEmptyReservationAndAuthenticationDataReturnsEmptyList() throws Exception {
		try {
			List<SecretAuthenticationKey> authData = new LinkedList<SecretAuthenticationKey>();

			final DateTime from = DateTime.now();
			final DateTime to = DateTime.now().plusHours(1);

			federatorRS.makeReservation(authData, Lists.<NodeUrn>newArrayList(), from, to, null, null);
			fail();

		} catch (RSFault_Exception expected) {
		}
	}

	/**
	 * Tests if the delete call is made on all federated RS instances.
	 *
	 * @throws Exception
	 * 		if anything goes wrong
	 */
	@Test
	public void testDeleteReservation() throws Exception {
		// TODO implement
	}

	/**
	 * Tests if the call of {@link RS#getReservations(org.joda.time.DateTime, org.joda.time.DateTime, Integer, Integer)}
	 * is made on all federated RS instances and the results are merged correctly.
	 *
	 * @throws Exception
	 * 		if anything goes wrong
	 */
	@Test
	public void testGetReservations() throws Exception {
		// TODO implement
	}

	/**
	 * Tests if the call of {@link RS#getConfidentialReservations(java.util.List, org.joda.time.DateTime,
	 * org.joda.time.DateTime, Integer, Integer)} is made on all federated RS instances and the results are merged
	 * correctly.
	 *
	 * @throws Exception
	 * 		if anything goes wrong
	 */
	@Test
	public void testGetConfidentialReservations() throws Exception {
		// TODO implement
	}
}
