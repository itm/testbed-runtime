
package eu.wisebed.testbed.api.rs.v1;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getReservationResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="getReservationResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="reservationData" type="{urn:RSService}confidentialReservationData" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "getReservationResponse", propOrder = {
    "reservationData"
})
public class GetReservationResponse {

    @XmlElement(required = true)
    protected List<ConfidentialReservationData> reservationData;

    /**
     * Gets the value of the reservationData property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the reservationData property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getReservationData().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ConfidentialReservationData }
     * 
     * 
     */
    public List<ConfidentialReservationData> getReservationData() {
        if (reservationData == null) {
            reservationData = new ArrayList<ConfidentialReservationData>();
        }
        return this.reservationData;
    }

}
