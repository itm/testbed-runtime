
package eu.wisebed.testbed.api.wsn.v22;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for defineNetwork complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="defineNetwork">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="newNetwork" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "defineNetwork", propOrder = {
    "newNetwork"
})
public class DefineNetwork {

    @XmlElement(required = true)
    protected String newNetwork;

    /**
     * Gets the value of the newNetwork property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNewNetwork() {
        return newNetwork;
    }

    /**
     * Sets the value of the newNetwork property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNewNetwork(String value) {
        this.newNetwork = value;
    }

}
