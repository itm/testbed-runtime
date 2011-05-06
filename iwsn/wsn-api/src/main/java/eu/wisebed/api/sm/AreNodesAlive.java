
package eu.wisebed.api.sm;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for areNodesAlive complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="areNodesAlive">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="nodes" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded"/>
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
@XmlType(name = "areNodesAlive", propOrder = {
    "nodes",
    "controllerEndpointUrl"
})
public class AreNodesAlive {

    @XmlElement(required = true, nillable = true)
    protected List<String> nodes;
    @XmlElement(required = true)
    protected String controllerEndpointUrl;

    /**
     * Gets the value of the nodes property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the nodes property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNodes().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getNodes() {
        if (nodes == null) {
            nodes = new ArrayList<String>();
        }
        return this.nodes;
    }

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
