
package eu.wisebed.api.sm;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import eu.wisebed.api.common.KeyValuePair;


/**
 * <p>Java class for getConfigurationResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="getConfigurationResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="rsEndpointUrl" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="snaaEndpointUrl" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="options" type="{urn:CommonTypes}KeyValuePair" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "getConfigurationResponse", propOrder = {
    "rsEndpointUrl",
    "snaaEndpointUrl",
    "options"
})
public class GetConfigurationResponse {

    @XmlElement(required = true)
    protected String rsEndpointUrl;
    @XmlElement(required = true)
    protected String snaaEndpointUrl;
    protected List<KeyValuePair> options;

    /**
     * Gets the value of the rsEndpointUrl property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRsEndpointUrl() {
        return rsEndpointUrl;
    }

    /**
     * Sets the value of the rsEndpointUrl property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRsEndpointUrl(String value) {
        this.rsEndpointUrl = value;
    }

    /**
     * Gets the value of the snaaEndpointUrl property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSnaaEndpointUrl() {
        return snaaEndpointUrl;
    }

    /**
     * Sets the value of the snaaEndpointUrl property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSnaaEndpointUrl(String value) {
        this.snaaEndpointUrl = value;
    }

    /**
     * Gets the value of the options property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the options property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOptions().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link KeyValuePair }
     * 
     * 
     */
    public List<KeyValuePair> getOptions() {
        if (options == null) {
            options = new ArrayList<KeyValuePair>();
        }
        return this.options;
    }

}
