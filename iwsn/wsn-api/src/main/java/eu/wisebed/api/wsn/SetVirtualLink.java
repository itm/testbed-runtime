
package eu.wisebed.api.wsn;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for setVirtualLink complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="setVirtualLink">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="sourceNode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="targetNode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="remoteServiceInstance" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="parameters" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="filters" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "setVirtualLink", propOrder = {
    "sourceNode",
    "targetNode",
    "remoteServiceInstance",
    "parameters",
    "filters"
})
public class SetVirtualLink {

    @XmlElement(required = true)
    protected String sourceNode;
    @XmlElement(required = true)
    protected String targetNode;
    @XmlElement(required = true)
    protected String remoteServiceInstance;
    @XmlElement(nillable = true)
    protected List<String> parameters;
    @XmlElement(nillable = true)
    protected List<String> filters;

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

    /**
     * Gets the value of the remoteServiceInstance property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRemoteServiceInstance() {
        return remoteServiceInstance;
    }

    /**
     * Sets the value of the remoteServiceInstance property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRemoteServiceInstance(String value) {
        this.remoteServiceInstance = value;
    }

    /**
     * Gets the value of the parameters property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the parameters property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getParameters().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getParameters() {
        if (parameters == null) {
            parameters = new ArrayList<String>();
        }
        return this.parameters;
    }

    /**
     * Gets the value of the filters property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the filters property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFilters().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getFilters() {
        if (filters == null) {
            filters = new ArrayList<String>();
        }
        return this.filters;
    }

}
