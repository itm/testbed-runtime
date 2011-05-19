
package eu.wisebed.api.rs;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getReservationsResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="getReservationsResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="reservations" type="{urn:RSService}publicReservationData" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "getReservationsResponse", propOrder = {
    "reservations"
})
public class GetReservationsResponse {

    @XmlElement(required = true)
    protected List<PublicReservationData> reservations;

    /**
     * Gets the value of the reservations property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the reservations property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getReservations().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PublicReservationData }
     * 
     * 
     */
    public List<PublicReservationData> getReservations() {
        if (reservations == null) {
            reservations = new ArrayList<PublicReservationData>();
        }
        return this.reservations;
    }

}
