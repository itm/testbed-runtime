
package eu.wisebed.api.snaa;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for isAuthorizedResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="isAuthorizedResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="authorization" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "isAuthorizedResponse", propOrder = {
    "authorization"
})
public class IsAuthorizedResponse {

    protected boolean authorization;

    /**
     * Gets the value of the authorization property.
     * 
     */
    public boolean isAuthorization() {
        return authorization;
    }

    /**
     * Sets the value of the authorization property.
     * 
     */
    public void setAuthorization(boolean value) {
        this.authorization = value;
    }

}
