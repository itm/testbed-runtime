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
import de.uniluebeck.itm.tr.rs.persistence.jpa.PersistenceModule;
import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.ConfidentialReservationDataInternal;
import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.SecretReservationKeyInternal;
import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.UserInternal;
import de.uniluebeck.itm.tr.rs.persistence.jpa.impl.RSPersistenceImpl;
import de.uniluebeck.itm.tr.rs.persistence.jpa.impl.TypeConverter;
import de.uniluebeck.itm.tr.rs.persistence.test.RSPersistenceTest;
import eu.wisebed.testbed.api.rs.v1.ConfidentialReservationData;
import eu.wisebed.testbed.api.rs.v1.RSExceptionException;
import eu.wisebed.testbed.api.rs.v1.SecretReservationKey;
import org.joda.time.Interval;
import org.junit.Before;

import javax.xml.datatype.DatatypeConfigurationException;
import java.sql.Date;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: rohwedder
 * Date: 20.04.2010
 * Time: 10:59:48
 * To change this template use File | Settings | File Templates.
 */
public class RSPersistenceJPATest extends RSPersistenceTest {

    @Before
    public void setUp() throws RSExceptionException, DatatypeConfigurationException {
        super.setUp();
        RSPersistence persistence = PersistenceModule.createInstance("PersistenceDB");
        super.setPersistence(persistence);
    }


    //simple TEST

    public static void main(String[] args) throws Throwable {

        RSPersistence rsPersistence = PersistenceModule.createInstance("PersistenceDB");
        String urnPrefix = "de";

        Date dateFrom = new Date(System.currentTimeMillis());
        Date dateTo = new Date(System.currentTimeMillis() + 10000);

        UserInternal user = new UserInternal();
        user.setUsername("Nils");
        List users = new LinkedList<UserInternal>();
        users.add(user);
        ConfidentialReservationDataInternal addConfidentialReservationData = new ConfidentialReservationDataInternal();
        List urns = new ArrayList();
        urns.add("testURN");
        addConfidentialReservationData.setNodeURNs(urns);
        addConfidentialReservationData.setFromDate(dateFrom.getTime());
        addConfidentialReservationData.setToDate(dateTo.getTime());
        addConfidentialReservationData.setNodeURNs(urns);
        addConfidentialReservationData.setUsers(users);
        SecretReservationKey addKey = rsPersistence.addReservation(TypeConverter.convert(addConfidentialReservationData), urnPrefix);
        System.out.println(addKey.getSecretReservationKey() + " added!");

        ((RSPersistenceImpl) rsPersistence).printPersistentReservationData();

        ConfidentialReservationData foundConfidentialReservationData = rsPersistence.getReservation(addKey);
        System.out.println("found Object: " + foundConfidentialReservationData + "for key: " + addKey.getSecretReservationKey());

        SecretReservationKeyInternal deleteKey = new SecretReservationKeyInternal();
        deleteKey.setSecretReservationKey(addKey.getSecretReservationKey());
        deleteKey.setUrnPrefix(urnPrefix);
        ConfidentialReservationData deleteConfidentialReservationData = rsPersistence.deleteReservation(TypeConverter.convert(deleteKey));
        System.out.println("deleted Object: " + deleteConfidentialReservationData + "for key: " + deleteKey.getSecretReservationKey());

        ((RSPersistenceImpl) rsPersistence).printPersistentReservationData();

        GregorianCalendar from = new GregorianCalendar();
        from.setTime(dateFrom);
        from.setTimeZone(TimeZone.getTimeZone("GMT-8:00"));
        Interval interval = new Interval(from.getTimeInMillis(), dateTo.getTime());
        System.out.println(rsPersistence.getReservations(interval));

        ((RSPersistenceImpl) rsPersistence).printPersistentReservationData();
    }

}
