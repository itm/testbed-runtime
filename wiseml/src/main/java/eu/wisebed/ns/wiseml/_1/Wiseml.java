//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.06.03 at 09:15:54 AM MESZ 
//


package eu.wisebed.ns.wiseml._1;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://wisebed.eu/ns/wiseml/1.0}setup"/>
 *         &lt;element ref="{http://wisebed.eu/ns/wiseml/1.0}scenario" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://wisebed.eu/ns/wiseml/1.0}trace" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="version" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token">
 *             &lt;enumeration value="1.0"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "setup",
    "scenario",
    "trace"
})
@XmlRootElement(name = "wiseml")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2010-06-03T09:15:54+02:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
public class Wiseml {

    @XmlElement(required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2010-06-03T09:15:54+02:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    protected Setup setup;
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2010-06-03T09:15:54+02:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    protected List<Scenario> scenario;
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2010-06-03T09:15:54+02:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    protected List<Trace> trace;
    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2010-06-03T09:15:54+02:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    protected String version;

    /**
     * Gets the value of the setup property.
     * 
     * @return
     *     possible object is
     *     {@link Setup }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2010-06-03T09:15:54+02:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    public Setup getSetup() {
        return setup;
    }

    /**
     * Sets the value of the setup property.
     * 
     * @param value
     *     allowed object is
     *     {@link Setup }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2010-06-03T09:15:54+02:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    public void setSetup(Setup value) {
        this.setup = value;
    }

    /**
     * Gets the value of the scenario property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the scenario property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getScenario().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Scenario }
     * 
     * 
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2010-06-03T09:15:54+02:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    public List<Scenario> getScenario() {
        if (scenario == null) {
            scenario = new ArrayList<Scenario>();
        }
        return this.scenario;
    }

    /**
     * Gets the value of the trace property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the trace property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTrace().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Trace }
     * 
     * 
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2010-06-03T09:15:54+02:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    public List<Trace> getTrace() {
        if (trace == null) {
            trace = new ArrayList<Trace>();
        }
        return this.trace;
    }

    /**
     * Gets the value of the version property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2010-06-03T09:15:54+02:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    public String getVersion() {
        return version;
    }

    /**
     * Sets the value of the version property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2010-06-03T09:15:54+02:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    public void setVersion(String value) {
        this.version = value;
    }

}