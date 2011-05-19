
package eu.wisebed.api.rs;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getConfidentialReservations complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="getConfidentialReservations">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="secretAuthenticationKey" type="{urn:RSService}secretAuthenticationKey" maxOccurs="unbounded"/>
 *         &lt;element name="period" type="{urn:RSService}getReservations"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "getConfidentialReservations", propOrder = {
    "secretAuthenticationKey",
    "period"
})
public class GetConfidentialReservations {

    @XmlElement(required = true)
    protected List<SecretAuthenticationKey> secretAuthenticationKey;
    @XmlElement(required = true)
    protected GetReservations period;

    /**
     * Gets the value of the secretAuthenticationKey property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the secretAuthenticationKey property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSecretAuthenticationKey().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SecretAuthenticationKey }
     * 
     * 
     */
    public List<SecretAuthenticationKey> getSecretAuthenticationKey() {
        if (secretAuthenticationKey == null) {
            secretAuthenticationKey = new ArrayList<SecretAuthenticationKey>();
        }
        return this.secretAuthenticationKey;
    }

    /**
     * Gets the value of the period property.
     * 
     * @return
     *     possible object is
     *     {@link GetReservations }
     *     
     */
    public GetReservations getPeriod() {
        return period;
    }

    /**
     * Sets the value of the period property.
     * 
     * @param value
     *     allowed object is
     *     {@link GetReservations }
     *     
     */
    public void setPeriod(GetReservations value) {
        this.period = value;
    }

}
