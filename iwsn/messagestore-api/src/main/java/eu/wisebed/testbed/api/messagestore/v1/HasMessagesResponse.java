
package eu.wisebed.testbed.api.messagestore.v1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for hasMessagesResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="hasMessagesResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="messages-found" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "hasMessagesResponse", propOrder = {
    "messagesFound"
})
public class HasMessagesResponse {

    @XmlElement(name = "messages-found")
    protected boolean messagesFound;

    /**
     * Gets the value of the messagesFound property.
     * 
     */
    public boolean isMessagesFound() {
        return messagesFound;
    }

    /**
     * Sets the value of the messagesFound property.
     * 
     */
    public void setMessagesFound(boolean value) {
        this.messagesFound = value;
    }

}
