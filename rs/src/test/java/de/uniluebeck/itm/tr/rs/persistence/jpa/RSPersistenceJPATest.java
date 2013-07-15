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
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class RSPersistenceJPATest extends RSPersistenceTest {

	private static final Map<String, String> properties = new HashMap<String, String>() {{

		// configure derby as embedded db for unit test
		put("hibernate.connection.url", "jdbc:derby:memory:DeviceConfigDB;create=true");
		put("hibernate.connection.driver_class", "org.apache.derby.jdbc.EmbeddedDriver");
		put("hibernate.dialect", "org.hibernate.dialect.DerbyTenSevenDialect");

		// configure hibernate ORM
		put("hibernate.ddl-generation.output-mode", "database");
		put("hibernate.hbm2ddl.auto", "create");
		put("hibernate.archive.autodetection", "class, hbm");

		// configure time zone
		put("timezone", "GMT");
	}};

	private static final DateTime CRD_FROM = DateTime.now();

	private static final DateTime CRD_TO = CRD_FROM.plusMinutes(30);

	private static final String CRD_SRK_KEY = "SECRET12345";

	private static final NodeUrnPrefix CRD_SRK_URN_PREFIX = new NodeUrnPrefix("urn:smartsantander:testbed:");

	private static final NodeUrn CRD_NODE_URN_1 = new NodeUrn(CRD_SRK_URN_PREFIX.toString() + "0x1234");

	private static final NodeUrn CRD_NODE_URN_2 = new NodeUrn(CRD_SRK_URN_PREFIX.toString() + "0x2345");

	private static final String CRD_USERNAME = "test-user";

	private static final String CRD_DESCRIPTION = "hello, world!";

	private static final SecretReservationKey CRD_SRK;

	private static final ConfidentialReservationData CRD;

	static {

		CRD_SRK = new SecretReservationKey();
		CRD_SRK.setKey(CRD_SRK_KEY);
		CRD_SRK.setUrnPrefix(CRD_SRK_URN_PREFIX);

		CRD = new ConfidentialReservationData();
		CRD.setFrom(CRD_FROM);
		CRD.setTo(CRD_TO);
		CRD.setSecretReservationKey(CRD_SRK);
		CRD.setUsername(CRD_USERNAME);
		CRD.setDescription(CRD_DESCRIPTION);
		CRD.getNodeUrns().add(CRD_NODE_URN_1);
		CRD.getNodeUrns().add(CRD_NODE_URN_2);
	}

	@Before
	public void setUp() throws RSFault_Exception {
		super.setUp();
		final RSPersistenceJPAModule module = new RSPersistenceJPAModule(TimeZone.getDefault(), mapToProperties());
		final RSPersistence rsPersistence = Guice.createInjector(module).getInstance(RSPersistence.class);
		super.setPersistence(rsPersistence);
	}

	private Properties mapToProperties() {
		final Properties props = new Properties();
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			props.put(entry.getKey(), entry.getValue());
		}
		return props;
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testGetReservations() throws Exception {

		final ConfidentialReservationData inserted = persistence.addReservation(
				CRD.getNodeUrns(),
				CRD.getFrom(),
				CRD.getTo(),
				CRD.getUsername(),
				CRD.getSecretReservationKey().getUrnPrefix(),
				CRD.getDescription(),
				CRD.getOptions()
		);

		final ConfidentialReservationData retrieved = persistence.getReservation(inserted.getSecretReservationKey());
		assertEquals(inserted, retrieved);
	}
}
