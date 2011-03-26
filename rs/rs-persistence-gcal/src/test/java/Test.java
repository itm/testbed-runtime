//import javax.xml.datatype.DatatypeFactory;
//import javax.xml.datatype.XMLGregorianCalendar;
//
//import de.uniluebeck.itm.tr.rs.persistence.gcal.GCalRSPersistence;
//import eu.wisebed.testbed.api.rs.v1.RSExceptionException;
//import org.joda.time.DateTime;
//import org.joda.time.Interval;
//
//import eu.wisebed.testbed.api.rs.v1.ConfidentialReservationData;
//import eu.wisebed.testbed.api.rs.v1.SecretReservationKey;
//import eu.wisebed.testbed.api.rs.v1.User;
//
//public class Test {
//	private GCalRSPersistence gcal;
//	private String urnPrefix = "urn:wisebed:dummy1";
//
//
//    public Test() throws RSExceptionException {
//        gcal = new GCalRSPersistence("testbed-runtime-unittests@itm.uni-luebeck.de", "testbed-runtime-unittests123");
//    }
//
//    public void testCreateSearchDelete() throws Exception {
//		ConfidentialReservationData res = new ConfidentialReservationData();
//		DateTime from = new DateTime();
//		DateTime to = from.plusHours(2);
//
//		XMLGregorianCalendar xmlFrom = DatatypeFactory.newInstance()
//				.newXMLGregorianCalendar(from.toGregorianCalendar());
//		XMLGregorianCalendar xmlTo = DatatypeFactory.newInstance().newXMLGregorianCalendar(to.toGregorianCalendar());
//
//		res.setFrom(xmlFrom);
//		res.setTo(xmlTo);
//
//		User user = new User();
//		user.setUrnPrefix(urnPrefix);
//		user.setUsername("dummy-user");
//		res.getUsers().add(user);
//
//		for (int i = 0; i < 10; ++i)
//			res.getNodeURNs().add("urn:wisebed:dummy:node" + i);
//
//		SecretReservationKey secretReservationKey = gcal.addReservation(res, urnPrefix);
//		System.out.println("Got reservation key: " + secretReservationKey);
//
//		Thread.sleep(3000);
//
//		for (ConfidentialReservationData tmpRes : gcal
//				.getReservations(new Interval(from.minusHours(1), to.plusHours(1)))) {
//
//			System.out.println("--------------------------------");
//			System.out.println("Got reservation: " + tmpRes);
//			System.out.println("--------------------------------");
//			System.out.println(" From  : " + tmpRes.getFrom());
//			System.out.println(" To    : " + tmpRes.getTo());
//			System.out.println(" Nodes : " + tmpRes.getNodeURNs());
//			System.out.println(" Users : " + tmpRes.getUsers());
//			System.out.println();
//		}
//
//		Thread.sleep(3000);
//
//		gcal.deleteReservationBeforeDeletion(secretReservationKey);
//
//		System.out.println("Deleted reservation " + secretReservationKey.getSecretReservationKey());
//
//	}
//}
