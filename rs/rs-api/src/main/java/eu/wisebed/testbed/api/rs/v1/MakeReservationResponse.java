
package eu.wisebed.testbed.api.rs.v1;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for makeReservationResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="makeReservationResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="secretReservationKey" type="{urn:RSService}secretReservationKey" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "makeReservationResponse", propOrder = {
    "secretReservationKey"
})
public class MakeReservationResponse {

    @XmlElement(required = true)
    protected List<SecretReservationKey> secretReservationKey;

    /**
     * Gets the value of the secretReservationKey property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the secretReservationKey property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSecretReservationKey().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SecretReservationKey }
     * 
     * 
     */
    public List<SecretReservationKey> getSecretReservationKey() {
        if (secretReservationKey == null) {
            secretReservationKey = new ArrayList<SecretReservationKey>();
        }
        return this.secretReservationKey;
    }

}
