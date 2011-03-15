
package eu.wisebed.testbed.api.messagestore.v1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for hasMessages complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="hasMessages">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="secretReservationKey" type="{urn:WSNService}secretReservationKey" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "hasMessages", propOrder = {
    "secretReservationKey"
})
public class HasMessages {

    protected SecretReservationKey secretReservationKey;

    /**
     * Gets the value of the secretReservationKey property.
     * 
     * @return
     *     possible object is
     *     {@link SecretReservationKey }
     *     
     */
    public SecretReservationKey getSecretReservationKey() {
        return secretReservationKey;
    }

    /**
     * Sets the value of the secretReservationKey property.
     * 
     * @param value
     *     allowed object is
     *     {@link SecretReservationKey }
     *     
     */
    public void setSecretReservationKey(SecretReservationKey value) {
        this.secretReservationKey = value;
    }

}
