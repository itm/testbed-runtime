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

package de.uniluebeck.itm.tr.rs.cmdline;

import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.gcal.GCalRSPersistence;
import de.uniluebeck.itm.tr.util.StringUtils;
import eu.wisebed.testbed.api.rs.v1.ConfidentialReservationData;
import eu.wisebed.testbed.api.rs.v1.Data;
import eu.wisebed.testbed.api.rs.v1.SecretReservationKey;
import org.joda.time.Interval;

import javax.xml.datatype.DatatypeFactory;
import java.util.ArrayList;
import java.util.List;

public class GoogleTest {
	public static void main(String[] args) throws Exception {
		RSPersistence p = new GCalRSPersistence("", "");

		org.joda.time.DateTime from = new org.joda.time.DateTime();
		org.joda.time.DateTime to = from.plusHours(3);

		ConfidentialReservationData res = new ConfidentialReservationData();
		res.setFrom(DatatypeFactory.newInstance().newXMLGregorianCalendar(from.toGregorianCalendar()));
		res.setTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(to.toGregorianCalendar()));
		List<String> nodeUrns = new ArrayList<String>();
		nodeUrns.add("urn:wisebed:uzl:node1");
		nodeUrns.add("urn:wisebed:uzl:node22");
		nodeUrns.add("urn:wisebed:uzl:node33");
		res.getNodeURNs().addAll(nodeUrns);
		List<Data> datas = new ArrayList<Data>();
		Data data = new Data();
		data.setUrnPrefix("urn:wisebed:uzl:");
		data.setUsername("testuser");
		datas.add(data);
		res.getData().addAll(datas);

		System.out.println("Creating reservation: " + StringUtils.jaxbMarshal(res));
		SecretReservationKey addedReservation = p.addReservation(res, "urn:wisebed:uzl:");

		System.out.println("Done, key is: " + StringUtils.jaxbMarshal(addedReservation));
		Thread.sleep(10000);

		System.out.println("Searching reservations from (" + from + ") to (" + to + ")");
		List<ConfidentialReservationData> reservations = p.getReservations(new Interval(from.minusHours(1), to
				.plusHours(1)));
		for (ConfidentialReservationData r : reservations) {
			System.out.println("Found reservation: " + StringUtils.jaxbMarshal(r));
		}

		Thread.sleep(20000);

		System.out.println("Deleting reservation with key: " + StringUtils.jaxbMarshal(addedReservation));
		ConfidentialReservationData deletedReservation = p.deleteReservation(addedReservation);

		System.out.println("Deleted reservation: " + StringUtils.jaxbMarshal(deletedReservation));


	}
}
