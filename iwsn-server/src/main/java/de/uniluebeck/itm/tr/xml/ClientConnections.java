//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.11.14 at 01:47:45 PM MEZ 
//


package de.uniluebeck.itm.tr.xml;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ClientConnections complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ClientConnections">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="clientconnection" type="{http://itm.uniluebeck.de/tr/xml}ClientConnection" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ClientConnections", propOrder = {
    "clientconnection"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2011-11-14T01:47:45+01:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
public class ClientConnections {

    @XmlElement(required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2011-11-14T01:47:45+01:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    protected List<ClientConnection> clientconnection;

    /**
     * Gets the value of the clientconnection property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the clientconnection property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getClientconnection().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ClientConnection }
     * 
     * 
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2011-11-14T01:47:45+01:00", comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    public List<ClientConnection> getClientconnection() {
        if (clientconnection == null) {
            clientconnection = new ArrayList<ClientConnection>();
        }
        return this.clientconnection;
    }

}