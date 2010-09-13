
package de.uniluebeck.itm.tr.logcontroller.client;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for fetchMessages complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="fetchMessages">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="secretReservationKey" type="{urn:WSNService}secretReservationKey" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="messageType" type="{urn:CommonTypes}messageType" minOccurs="0"/>
 *         &lt;element name="messageLimit" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fetchMessages", propOrder = {
    "secretReservationKey",
    "messageType",
    "messageLimit"
})
public class FetchMessages {

    protected List<SecretReservationKey> secretReservationKey;
    protected MessageType messageType;
    protected int messageLimit;

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

    /**
     * Gets the value of the messageType property.
     * 
     * @return
     *     possible object is
     *     {@link MessageType }
     *     
     */
    public MessageType getMessageType() {
        return messageType;
    }

    /**
     * Sets the value of the messageType property.
     * 
     * @param value
     *     allowed object is
     *     {@link MessageType }
     *     
     */
    public void setMessageType(MessageType value) {
        this.messageType = value;
    }

    /**
     * Gets the value of the messageLimit property.
     * 
     */
    public int getMessageLimit() {
        return messageLimit;
    }

    /**
     * Sets the value of the messageLimit property.
     * 
     */
    public void setMessageLimit(int value) {
        this.messageLimit = value;
    }

}
