
package eu.wisebed.api.rs;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for makeReservation complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="makeReservation">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="authenticationData" type="{urn:RSService}secretAuthenticationKey" maxOccurs="unbounded"/>
 *         &lt;element name="reservation" type="{urn:RSService}confidentialReservationData"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "makeReservation", propOrder = {
    "authenticationData",
    "reservation"
})
public class MakeReservation {

    @XmlElement(required = true)
    protected List<SecretAuthenticationKey> authenticationData;
    @XmlElement(required = true)
    protected ConfidentialReservationData reservation;

    /**
     * Gets the value of the authenticationData property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the authenticationData property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAuthenticationData().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SecretAuthenticationKey }
     * 
     * 
     */
    public List<SecretAuthenticationKey> getAuthenticationData() {
        if (authenticationData == null) {
            authenticationData = new ArrayList<SecretAuthenticationKey>();
        }
        return this.authenticationData;
    }

    /**
     * Gets the value of the reservation property.
     * 
     * @return
     *     possible object is
     *     {@link ConfidentialReservationData }
     *     
     */
    public ConfidentialReservationData getReservation() {
        return reservation;
    }

    /**
     * Sets the value of the reservation property.
     * 
     * @param value
     *     allowed object is
     *     {@link ConfidentialReservationData }
     *     
     */
    public void setReservation(ConfidentialReservationData value) {
        this.reservation = value;
    }

}
