
package eu.wisebed.api.wsn;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for addController complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="addController">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="controllerEndpointUrl" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "addController", propOrder = {
    "controllerEndpointUrl"
})
public class AddController {

    @XmlElement(required = true)
    protected String controllerEndpointUrl;

    /**
     * Gets the value of the controllerEndpointUrl property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getControllerEndpointUrl() {
        return controllerEndpointUrl;
    }

    /**
     * Sets the value of the controllerEndpointUrl property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setControllerEndpointUrl(String value) {
        this.controllerEndpointUrl = value;
    }

}
