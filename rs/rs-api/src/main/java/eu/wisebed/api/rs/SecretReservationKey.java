
package eu.wisebed.api.rs;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for secretReservationKey complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="secretReservationKey">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="secretReservationKey" type="{http://www.w3.org/2001/XMLSchema}string"/>
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
@XmlType(name = "secretReservationKey", propOrder = {
    "secretReservationKey",
    "urnPrefix"
})
public class SecretReservationKey {

    @XmlElement(required = true)
    protected String secretReservationKey;
    @XmlElement(required = true)
    protected String urnPrefix;

    /**
     * Gets the value of the secretReservationKey property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSecretReservationKey() {
        return secretReservationKey;
    }

    /**
     * Sets the value of the secretReservationKey property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSecretReservationKey(String value) {
        this.secretReservationKey = value;
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

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final SecretReservationKey that = (SecretReservationKey) o;

		if (!secretReservationKey.equals(that.secretReservationKey)) {
			return false;
		}
		if (!urnPrefix.equals(that.urnPrefix)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = secretReservationKey.hashCode();
		result = 31 * result + urnPrefix.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "SecretReservationKey{" +
				"secretReservationKey='" + secretReservationKey + '\'' +
				", urnPrefix='" + urnPrefix + '\'' +
				'}';
	}
}
