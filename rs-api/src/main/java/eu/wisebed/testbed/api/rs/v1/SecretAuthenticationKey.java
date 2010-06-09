
package eu.wisebed.testbed.api.rs.v1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for secretAuthenticationKey complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="secretAuthenticationKey">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="username" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="secretAuthenticationKey" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="urnPrefix" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "secretAuthenticationKey", propOrder = {
    "username",
    "secretAuthenticationKey",
    "urnPrefix"
})
public class SecretAuthenticationKey {

    @XmlElement(required = true)
    protected String username;
    @XmlElement(required = true)
    protected String secretAuthenticationKey;
    @XmlElement(required = true)
    protected String urnPrefix;

    /**
     * Gets the value of the username property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the value of the username property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUsername(String value) {
        this.username = value;
    }

    /**
     * Gets the value of the secretAuthenticationKey property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSecretAuthenticationKey() {
        return secretAuthenticationKey;
    }

    /**
     * Sets the value of the secretAuthenticationKey property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSecretAuthenticationKey(String value) {
        this.secretAuthenticationKey = value;
    }

    /**
     * Gets the value of the urnPrefix property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUrnPrefix() {
        return urnPrefix;
    }

    /**
     * Sets the value of the urnPrefix property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUrnPrefix(String value) {
        this.urnPrefix = value;
    }

}
