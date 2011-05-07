
package eu.wisebed.api.wsn;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for disablePhysicalLink complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="disablePhysicalLink">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="nodeA" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="nodeB" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "disablePhysicalLink", propOrder = {
    "nodeA",
    "nodeB"
})
public class DisablePhysicalLink {

    @XmlElement(required = true)
    protected String nodeA;
    @XmlElement(required = true)
    protected String nodeB;

    /**
     * Gets the value of the nodeA property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNodeA() {
        return nodeA;
    }

    /**
     * Sets the value of the nodeA property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNodeA(String value) {
        this.nodeA = value;
    }

    /**
     * Gets the value of the nodeB property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNodeB() {
        return nodeB;
    }

    /**
     * Sets the value of the nodeB property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNodeB(String value) {
        this.nodeB = value;
    }

}
