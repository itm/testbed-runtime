package de.uniluebeck.itm.tr.rs.persistence.jpa; /**********************************************************************************************************************
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
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

import com.google.inject.Guice;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistenceTest;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.ConfidentialReservationDataKey;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class RSPersistenceJPATest extends RSPersistenceTest {

	private final static TimeZone localTimeZone = TimeZone.getTimeZone("GMT");

	private static final Map<String, String> properties = new HashMap<String, String>() {{

		// configure derby as embedded db for unit test
		put("hibernate.connection.driver_class", "org.apache.derby.jdbc.EmbeddedDriver");
		put("hibernate.connection.url", "jdbc:derby:memory:default;create=true");
		put("hibernate.dialect", "org.hibernate.dialect.DerbyDialect");

		// configure hibernate ORM
		put("hibernate.ddl-generation.output-mode", "database");
		put("hibernate.hbm2ddl.auto", "create");
		put("hibernate.archive.autodetection", "class, hbm");

	}};

	@Before
	public void setUp() throws RSFault_Exception {
		super.setUp();
		final RSPersistenceJPAModule module = new RSPersistenceJPAModule(localTimeZone, properties);
		final RSPersistence rsPersistence = Guice.createInjector(module).getInstance(RSPersistence.class);
		super.setPersistence(rsPersistence);
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testGetReservations() throws Exception {
		final ConfidentialReservationData confidentialReservationData
				= createConfidentialReservationData();

		final SecretReservationKey secretReservationKey = persistence.addReservation(
				confidentialReservationData,
				new NodeUrnPrefix("urn:smartsantander:testbed:")
		);

		final ConfidentialReservationData result = persistence.getReservation(secretReservationKey);

		assertNotNull(result);
		assertNotNull(result.getKeys().get(0));

		final String resultReservationKey = result.getKeys().get(0).getSecretReservationKey();

		assertNotNull(resultReservationKey);
		assertFalse(resultReservationKey.isEmpty());
		assertEquals(resultReservationKey, confidentialReservationData.getKeys().get(0).getSecretReservationKey());

		System.out.println(resultReservationKey);
	}

	private ConfidentialReservationData createConfidentialReservationData() throws DatatypeConfigurationException {

		final ConfidentialReservationData confidentialReservationData = new ConfidentialReservationData();
		confidentialReservationData.setFrom(DateTime.now());
		confidentialReservationData.setTo(DateTime.now().plusMinutes(30));

		ConfidentialReservationDataKey data = new ConfidentialReservationDataKey();
		data.setSecretReservationKey("SECRET12345");
		data.setUrnPrefix(new NodeUrnPrefix("urn:smartsantander:testbed:"));
		data.setUsername("test-user");
		confidentialReservationData.getKeys().add(data);

		return confidentialReservationData;
	}
}
