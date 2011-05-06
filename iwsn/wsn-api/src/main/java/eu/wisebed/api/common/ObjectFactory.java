
package eu.wisebed.api.common;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the eu.wisebed.api.common package. 
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

    private final static QName _GetNetworkResponse_QNAME = new QName("urn:CommonTypes", "getNetworkResponse");
    private final static QName _Message_QNAME = new QName("urn:CommonTypes", "message");
    private final static QName _GetNetwork_QNAME = new QName("urn:CommonTypes", "getNetwork");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: eu.wisebed.api.common
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link KeyValuePair }
     * 
     */
    public KeyValuePair createKeyValuePair() {
        return new KeyValuePair();
    }

    /**
     * Create an instance of {@link GetNetwork }
     * 
     */
    public GetNetwork createGetNetwork() {
        return new GetNetwork();
    }

    /**
     * Create an instance of {@link Message }
     * 
     */
    public Message createMessage() {
        return new Message();
    }

    /**
     * Create an instance of {@link GetNetworkResponse }
     * 
     */
    public GetNetworkResponse createGetNetworkResponse() {
        return new GetNetworkResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetNetworkResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:CommonTypes", name = "getNetworkResponse")
    public JAXBElement<GetNetworkResponse> createGetNetworkResponse(GetNetworkResponse value) {
        return new JAXBElement<GetNetworkResponse>(_GetNetworkResponse_QNAME, GetNetworkResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Message }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:CommonTypes", name = "message")
    public JAXBElement<Message> createMessage(Message value) {
        return new JAXBElement<Message>(_Message_QNAME, Message.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetNetwork }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:CommonTypes", name = "getNetwork")
    public JAXBElement<GetNetwork> createGetNetwork(GetNetwork value) {
        return new JAXBElement<GetNetwork>(_GetNetwork_QNAME, GetNetwork.class, null, value);
    }

}
