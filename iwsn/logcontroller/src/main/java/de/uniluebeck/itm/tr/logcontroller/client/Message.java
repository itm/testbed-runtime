
package de.uniluebeck.itm.tr.logcontroller.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for message complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="message">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="sourceNodeId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="timestamp" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="textMessage" type="{urn:CommonTypes}textMessage" minOccurs="0"/>
 *         &lt;element name="binaryMessage" type="{urn:CommonTypes}binaryMessage" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "message", namespace = "urn:CommonTypes", propOrder = {
    "sourceNodeId",
    "timestamp",
    "textMessage",
    "binaryMessage"
})
public class Message {

    @XmlElement(required = true)
    protected String sourceNodeId;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar timestamp;
    protected TextMessage textMessage;
    protected BinaryMessage binaryMessage;

    /**
     * Gets the value of the sourceNodeId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSourceNodeId() {
        return sourceNodeId;
    }

    /**
     * Sets the value of the sourceNodeId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSourceNodeId(String value) {
        this.sourceNodeId = value;
    }

    /**
     * Gets the value of the timestamp property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the value of the timestamp property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setTimestamp(XMLGregorianCalendar value) {
        this.timestamp = value;
    }

    /**
     * Gets the value of the textMessage property.
     * 
     * @return
     *     possible object is
     *     {@link TextMessage }
     *     
     */
    public TextMessage getTextMessage() {
        return textMessage;
    }

    /**
     * Sets the value of the textMessage property.
     * 
     * @param value
     *     allowed object is
     *     {@link TextMessage }
     *     
     */
    public void setTextMessage(TextMessage value) {
        this.textMessage = value;
    }

    /**
     * Gets the value of the binaryMessage property.
     * 
     * @return
     *     possible object is
     *     {@link BinaryMessage }
     *     
     */
    public BinaryMessage getBinaryMessage() {
        return binaryMessage;
    }

    /**
     * Sets the value of the binaryMessage property.
     * 
     * @param value
     *     allowed object is
     *     {@link BinaryMessage }
     *     
     */
    public void setBinaryMessage(BinaryMessage value) {
        this.binaryMessage = value;
    }

}
