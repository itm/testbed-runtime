//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.12.30 at 02:12:08 PM MEZ 
//


package de.uniluebeck.itm.tr.runtime.wsnapp.xml;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the de.uniluebeck.itm.tr.runtime.wsnapp.xml package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _WsnDevice_QNAME = new QName("http://itm.uniluebeck.de/tr/runtime/wsnapp/xml", "wsnDevice");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: de.uniluebeck.itm.tr.runtime.wsnapp.xml
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link WsnDevice.DefaultChannelPipeline }
     * 
     */
    public WsnDevice.DefaultChannelPipeline createWsnDeviceDefaultChannelPipeline() {
        return new WsnDevice.DefaultChannelPipeline();
    }

    /**
     * Create an instance of {@link Configuration }
     * 
     */
    public Configuration createConfiguration() {
        return new Configuration();
    }

    /**
     * Create an instance of {@link Timeouts }
     * 
     */
    public Timeouts createTimeouts() {
        return new Timeouts();
    }

    /**
     * Create an instance of {@link WsnDevice }
     * 
     */
    public WsnDevice createWsnDevice() {
        return new WsnDevice();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link WsnDevice }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://itm.uniluebeck.de/tr/runtime/wsnapp/xml", name = "wsnDevice")
    public JAXBElement<WsnDevice> createWsnDevice(WsnDevice value) {
        return new JAXBElement<WsnDevice>(_WsnDevice_QNAME, WsnDevice.class, null, value);
    }

}
