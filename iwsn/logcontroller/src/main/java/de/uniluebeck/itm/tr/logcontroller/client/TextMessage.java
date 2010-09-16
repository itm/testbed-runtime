
package de.uniluebeck.itm.tr.logcontroller.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for textMessage complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="textMessage">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="msg" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="messageLevel" type="{urn:CommonTypes}messageLevel" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "textMessage", namespace = "urn:CommonTypes", propOrder = {
    "msg",
    "messageLevel"
})
public class TextMessage {

    @XmlElement(required = true)
    protected String msg;
    protected MessageLevel messageLevel;

    /**
     * Gets the value of the msg property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMsg() {
        return msg;
    }

    /**
     * Sets the value of the msg property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMsg(String value) {
        this.msg = value;
    }

    /**
     * Gets the value of the messageLevel property.
     * 
     * @return
     *     possible object is
     *     {@link MessageLevel }
     *     
     */
    public MessageLevel getMessageLevel() {
        return messageLevel;
    }

    /**
     * Sets the value of the messageLevel property.
     * 
     * @param value
     *     allowed object is
     *     {@link MessageLevel }
     *     
     */
    public void setMessageLevel(MessageLevel value) {
        this.messageLevel = value;
    }

}
