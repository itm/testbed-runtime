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

import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistenceTest;
import de.uniluebeck.itm.tr.rs.persistence.jpa.RSPersistenceJPAFactory;
import eu.wisebed.api.rs.ConfidentialReservationData;
import eu.wisebed.api.rs.Data;
import eu.wisebed.api.rs.RSExceptionException;
import eu.wisebed.api.rs.SecretReservationKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class RSPersistenceJPATest extends RSPersistenceTest {
    private final static TimeZone localTimeZone = TimeZone.getTimeZone("GMT");
    private static final Map<String, String> properties = new HashMap<String, String>() {{
        //Configure Apache
        put("hibernate.connection.driver_class", "org.apache.derby.jdbc.EmbeddedDriver");
        put("hibernate.connection.url", "jdbc:derby:target/default;create=true");
        put("hibernate.dialect", "org.hibernate.dialect.DerbyDialect");
        //Configure Hibernate
        put("hibernate.ddl-generation.output-mode", "database");
        put("hibernate.hbm2ddl.auto", "create");
        put("hibernate.archive.autodetection", "class, hbm");
    }};

    private RSPersistence reservationRepository;

    @Before
    public void setUp() throws RSExceptionException {
        super.setUp();

        reservationRepository = RSPersistenceJPAFactory.createInstance(properties, localTimeZone);
        super.setPersistence(reservationRepository);
    }

    @After
    public void tearDown() {
        reservationRepository = null;
    }

    @Test
    public void testGetReservations() throws Exception {
        final ConfidentialReservationData confidentialReservationData
                = createConfidentialReservationData();

        final SecretReservationKey secretReservationKey
                = reservationRepository.addReservation(confidentialReservationData, "urn:smartsantander:testbed:");

        final ConfidentialReservationData result
                = reservationRepository.getReservation(secretReservationKey);

        assertNotNull(result);
        assertNotNull(result.getUserData());
        assertEquals(result.getUserData(), confidentialReservationData.getUserData());
        assertNotNull(result.getData().get(0));

        final String resultReservationKey = result.getData().get(0).getSecretReservationKey();

        assertNotNull(resultReservationKey);
        assertFalse(resultReservationKey.isEmpty());
        assertEquals(resultReservationKey, confidentialReservationData.getData().get(0).getSecretReservationKey());

        System.out.println(resultReservationKey);
    }

    private ConfidentialReservationData createConfidentialReservationData() throws DatatypeConfigurationException {
        final ConfidentialReservationData confidentialReservationData = new ConfidentialReservationData();
        confidentialReservationData.setUserData("test-user");

        final XMLGregorianCalendar xmlGregorianCalendar
                = DatatypeFactory.newInstance().newXMLGregorianCalendarDate(2011, 6, 25, 0);
        confidentialReservationData.setFrom(xmlGregorianCalendar);
        xmlGregorianCalendar.add(
                DatatypeFactory.newInstance().newDuration(1000 * 60 * 30)); // 30 minutes
        confidentialReservationData.setTo(xmlGregorianCalendar);

        Data data = new Data();
        data.setSecretReservationKey("SECRET12345");
        data.setUrnPrefix("urn:smartsantander:testbed:");
        data.setUsername("test-user");
        confidentialReservationData.getData().add(data);

        return confidentialReservationData;
    }
}
