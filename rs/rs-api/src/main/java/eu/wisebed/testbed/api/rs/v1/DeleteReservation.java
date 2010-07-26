package eu.wisebed.testbed.api.rs.v1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for deleteReservation complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="deleteReservation">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="authenticationData" type="{urn:RSService}secretAuthenticationKey" maxOccurs="unbounded"/>
 *         &lt;element name="secretReservationKey" type="{urn:RSService}secretReservationKey" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "deleteReservation", propOrder = {
		"authenticationData",
		"secretReservationKey"
})
public class DeleteReservation {

	@XmlElement(required = true)
	protected List<SecretAuthenticationKey> authenticationData;
	@XmlElement(required = true)
	protected List<SecretReservationKey> secretReservationKey;

	/**
	 * Gets the value of the authenticationData property.
	 * <p/>
	 * <p/>
	 * This accessor method returns a reference to the live list,
	 * not a snapshot. Therefore any modification you make to the
	 * returned list will be present inside the JAXB object.
	 * This is why there is not a <CODE>set</CODE> method for the authenticationData property.
	 * <p/>
	 * <p/>
	 * For example, to add a new item, do as follows:
	 * <pre>
	 *    getAuthenticationData().add(newItem);
	 * </pre>
	 * <p/>
	 * <p/>
	 * <p/>
	 * Objects of the following type(s) are allowed in the list
	 * {@link SecretAuthenticationKey }
	 */
	public List<SecretAuthenticationKey> getAuthenticationData() {
		if (authenticationData == null) {
			authenticationData = new ArrayList<SecretAuthenticationKey>();
		}
		return this.authenticationData;
	}

	/**
	 * Gets the value of the secretReservationKey property.
	 * <p/>
	 * <p/>
	 * This accessor method returns a reference to the live list,
	 * not a snapshot. Therefore any modification you make to the
	 * returned list will be present inside the JAXB object.
	 * This is why there is not a <CODE>set</CODE> method for the secretReservationKey property.
	 * <p/>
	 * <p/>
	 * For example, to add a new item, do as follows:
	 * <pre>
	 *    getSecretReservationKey().add(newItem);
	 * </pre>
	 * <p/>
	 * <p/>
	 * <p/>
	 * Objects of the following type(s) are allowed in the list
	 * {@link SecretReservationKey }
	 */
	public List<SecretReservationKey> getSecretReservationKey() {
		if (secretReservationKey == null) {
			secretReservationKey = new ArrayList<SecretReservationKey>();
		}
		return this.secretReservationKey;
	}

}
