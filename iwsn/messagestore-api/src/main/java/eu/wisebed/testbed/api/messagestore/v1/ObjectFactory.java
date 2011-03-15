
package eu.wisebed.testbed.api.messagestore.v1;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the eu.wisebed.testbed.api.messagestore.v1 package. 
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

    private final static QName _HasMessages_QNAME = new QName("urn:MessageStore", "hasMessages");
    private final static QName _HasMessagesResponse_QNAME = new QName("urn:MessageStore", "hasMessagesResponse");
    private final static QName _FetchMessagesResponse_QNAME = new QName("urn:MessageStore", "fetchMessagesResponse");
    private final static QName _FetchMessages_QNAME = new QName("urn:MessageStore", "fetchMessages");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: eu.wisebed.testbed.api.messagestore.v1
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link SecretReservationKey }
     * 
     */
    public SecretReservationKey createSecretReservationKey() {
        return new SecretReservationKey();
    }

    /**
     * Create an instance of {@link HasMessages }
     * 
     */
    public HasMessages createHasMessages() {
        return new HasMessages();
    }

    /**
     * Create an instance of {@link FetchMessages }
     * 
     */
    public FetchMessages createFetchMessages() {
        return new FetchMessages();
    }

    /**
     * Create an instance of {@link FetchMessagesResponse }
     * 
     */
    public FetchMessagesResponse createFetchMessagesResponse() {
        return new FetchMessagesResponse();
    }

    /**
     * Create an instance of {@link HasMessagesResponse }
     * 
     */
    public HasMessagesResponse createHasMessagesResponse() {
        return new HasMessagesResponse();
    }

    /**
     * Create an instance of {@link Message }
     * 
     */
    public Message createMessage() {
        return new Message();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HasMessages }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:MessageStore", name = "hasMessages")
    public JAXBElement<HasMessages> createHasMessages(HasMessages value) {
        return new JAXBElement<HasMessages>(_HasMessages_QNAME, HasMessages.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HasMessagesResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:MessageStore", name = "hasMessagesResponse")
    public JAXBElement<HasMessagesResponse> createHasMessagesResponse(HasMessagesResponse value) {
        return new JAXBElement<HasMessagesResponse>(_HasMessagesResponse_QNAME, HasMessagesResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FetchMessagesResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:MessageStore", name = "fetchMessagesResponse")
    public JAXBElement<FetchMessagesResponse> createFetchMessagesResponse(FetchMessagesResponse value) {
        return new JAXBElement<FetchMessagesResponse>(_FetchMessagesResponse_QNAME, FetchMessagesResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FetchMessages }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:MessageStore", name = "fetchMessages")
    public JAXBElement<FetchMessages> createFetchMessages(FetchMessages value) {
        return new JAXBElement<FetchMessages>(_FetchMessages_QNAME, FetchMessages.class, null, value);
    }

}
