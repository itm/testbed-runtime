package de.uniluebeck.itm.tr.logcontroller;

import com.google.common.collect.Lists;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import eu.wisebed.testbed.api.rs.v1.*;
import org.joda.time.DateTime;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jenskluttig
 * Date: 19.08.2010
 * Time: 16:45:53
 * To change this template use File | Settings | File Templates.
 */
@WebService(endpointInterface = "eu.wisebed.testbed.api.rs.v1.RS", portName = "RSPort", serviceName = "RSService", targetNamespace = "urn:RSService")
public class ReservationDummy implements RS {
    @Override
    public List<PublicReservationData> getReservations(@WebParam(name = "from", targetNamespace = "") XMLGregorianCalendar from, @WebParam(name = "to", targetNamespace = "") XMLGregorianCalendar to) throws RSExceptionException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<ConfidentialReservationData> getConfidentialReservations(@WebParam(name = "secretAuthenticationKey", targetNamespace = "") List<SecretAuthenticationKey> secretAuthenticationKey, @WebParam(name = "period", targetNamespace = "") GetReservations period) throws RSExceptionException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<ConfidentialReservationData> getReservation(@WebParam(name = "secretReservationKey", targetNamespace = "") List<SecretReservationKey> secretReservationKey) throws RSExceptionException, ReservervationNotFoundExceptionException {
        List<ConfidentialReservationData> data = Lists.newArrayList();
        ConfidentialReservationData dummy = new ConfidentialReservationData();
        XMLGregorianCalendar calendar = new XMLGregorianCalendarImpl();
        DateTime date = new DateTime();
        calendar.setYear(date.getYear());
        calendar.setMonth(date.getMonthOfYear());
        calendar.setDay(date.getDayOfMonth());
        calendar.setTime(date.getHourOfDay(), date.getMinuteOfHour() + 1, 0);
        dummy.setTo(calendar);
        data.add(dummy);
        return data;
    }

    @Override
    public void deleteReservation(@WebParam(name = "authenticationData", targetNamespace = "") List<SecretAuthenticationKey> authenticationData, @WebParam(name = "secretReservationKey", targetNamespace = "") List<SecretReservationKey> secretReservationKey) throws RSExceptionException, ReservervationNotFoundExceptionException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<SecretReservationKey> makeReservation(@WebParam(name = "authenticationData", targetNamespace = "") List<SecretAuthenticationKey> authenticationData, @WebParam(name = "reservation", targetNamespace = "") ConfidentialReservationData reservation) throws AuthorizationExceptionException, RSExceptionException, ReservervationConflictExceptionException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
