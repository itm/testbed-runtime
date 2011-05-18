
package eu.wisebed.api.snaa;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for authenticateResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="authenticateResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="secretAuthenticationKey" type="{http://testbed.wisebed.eu/api/snaa/v1/}secretAuthenticationKey" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "authenticateResponse", propOrder = {
    "secretAuthenticationKey"
})
public class AuthenticateResponse {

    @XmlElement(required = true)
    protected List<SecretAuthenticationKey> secretAuthenticationKey;

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

}
