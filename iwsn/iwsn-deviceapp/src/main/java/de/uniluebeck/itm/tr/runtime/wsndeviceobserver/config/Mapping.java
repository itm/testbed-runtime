//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.11.13 at 05:09:13 PM MEZ 
//


package de.uniluebeck.itm.tr.runtime.wsndeviceobserver.config;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Mapping complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Mapping">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="usbchipid" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="mac" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Mapping")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2011-11-13T05:09:13+01:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
public class Mapping {

    @XmlAttribute
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2011-11-13T05:09:13+01:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    protected String usbchipid;
    @XmlAttribute
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2011-11-13T05:09:13+01:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    protected String mac;

    /**
     * Gets the value of the usbchipid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2011-11-13T05:09:13+01:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    public String getUsbchipid() {
        return usbchipid;
    }

    /**
     * Sets the value of the usbchipid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2011-11-13T05:09:13+01:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    public void setUsbchipid(String value) {
        this.usbchipid = value;
    }

    /**
     * Gets the value of the mac property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2011-11-13T05:09:13+01:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    public String getMac() {
        return mac;
    }

    /**
     * Sets the value of the mac property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2011-11-13T05:09:13+01:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    public void setMac(String value) {
        this.mac = value;
    }

}
