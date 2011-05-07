
package eu.wisebed.api.wsn;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for destroyVirtualLink complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="destroyVirtualLink">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="sourceNode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="targetNode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "destroyVirtualLink", propOrder = {
    "sourceNode",
    "targetNode"
})
public class DestroyVirtualLink {

    @XmlElement(required = true)
    protected String sourceNode;
    @XmlElement(required = true)
    protected String targetNode;

    /**
     * Gets the value of the sourceNode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSourceNode() {
        return sourceNode;
    }

    /**
     * Sets the value of the sourceNode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSourceNode(String value) {
        this.sourceNode = value;
    }

    /**
     * Gets the value of the targetNode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTargetNode() {
        return targetNode;
    }

    /**
     * Sets the value of the targetNode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTargetNode(String value) {
        this.targetNode = value;
    }

}
