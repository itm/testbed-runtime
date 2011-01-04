package de.uniluebeck.itm.tr.rs.cmdline;

import de.uniluebeck.itm.tr.util.StringUtils;
import eu.wisebed.testbed.api.rs.v1.PublicReservationData;
import eu.wisebed.testbed.api.rs.v1.RS;
import org.joda.time.DateTime;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

/**
 * This is a test case for https://www.itm.uni-luebeck.de/projects/testbed-runtime/ticket/131.
 */
public class Bug131Test {

	public static void main(String[] args) throws Exception {

		RS rsService = eu.wisebed.testbed.api.rs.RSServiceHelper
				.getRSService("http://wisebed-staging.itm.uni-luebeck.de:8889/rs");

		DateTime from = new DateTime().minusYears(3);
		DateTime to = new DateTime().plusDays(1);

		XMLGregorianCalendar xmlFrom = DatatypeFactory.newInstance().newXMLGregorianCalendar(from.toGregorianCalendar());
		XMLGregorianCalendar xmlTo = DatatypeFactory.newInstance().newXMLGregorianCalendar(to.toGregorianCalendar());
		List<PublicReservationData> reservations = rsService.getReservations(xmlFrom, xmlTo);

		System.err.println("Got " + reservations.size() + " reservations from " + xmlFrom + " until " + xmlTo);

		for(PublicReservationData res : reservations )
		{
			System.out.print("\"");
			System.out.print(res.getFrom());
			System.out.print("\";\"");
			System.out.print(res.getTo());
			System.out.print("\";\"");
			System.out.print(StringUtils.toString(res.getNodeURNs()));
			System.out.print("\"\n");
		}
	}

}
