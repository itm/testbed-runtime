
package eu.wisebed.api.controller;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the eu.wisebed.api.controller package. 
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

    private final static QName _ReceiveNotification_QNAME = new QName("urn:ControllerService", "receiveNotification");
    private final static QName _RequestStatus_QNAME = new QName("urn:ControllerService", "requestStatus");
    private final static QName _Receive_QNAME = new QName("urn:ControllerService", "receive");
    private final static QName _ReceiveStatus_QNAME = new QName("urn:ControllerService", "receiveStatus");
    private final static QName _ExperimentEnded_QNAME = new QName("urn:ControllerService", "experimentEnded");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: eu.wisebed.api.controller
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ReceiveNotification }
     * 
     */
    public ReceiveNotification createReceiveNotification() {
        return new ReceiveNotification();
    }

    /**
     * Create an instance of {@link ExperimentEnded }
     * 
     */
    public ExperimentEnded createExperimentEnded() {
        return new ExperimentEnded();
    }

    /**
     * Create an instance of {@link Receive }
     * 
     */
    public Receive createReceive() {
        return new Receive();
    }

    /**
     * Create an instance of {@link RequestStatus }
     * 
     */
    public RequestStatus createRequestStatus() {
        return new RequestStatus();
    }

    /**
     * Create an instance of {@link Status }
     * 
     */
    public Status createStatus() {
        return new Status();
    }

    /**
     * Create an instance of {@link ReceiveStatus }
     * 
     */
    public ReceiveStatus createReceiveStatus() {
        return new ReceiveStatus();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReceiveNotification }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:ControllerService", name = "receiveNotification")
    public JAXBElement<ReceiveNotification> createReceiveNotification(ReceiveNotification value) {
        return new JAXBElement<ReceiveNotification>(_ReceiveNotification_QNAME, ReceiveNotification.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RequestStatus }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:ControllerService", name = "requestStatus")
    public JAXBElement<RequestStatus> createRequestStatus(RequestStatus value) {
        return new JAXBElement<RequestStatus>(_RequestStatus_QNAME, RequestStatus.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Receive }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:ControllerService", name = "receive")
    public JAXBElement<Receive> createReceive(Receive value) {
        return new JAXBElement<Receive>(_Receive_QNAME, Receive.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReceiveStatus }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:ControllerService", name = "receiveStatus")
    public JAXBElement<ReceiveStatus> createReceiveStatus(ReceiveStatus value) {
        return new JAXBElement<ReceiveStatus>(_ReceiveStatus_QNAME, ReceiveStatus.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExperimentEnded }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:ControllerService", name = "experimentEnded")
    public JAXBElement<ExperimentEnded> createExperimentEnded(ExperimentEnded value) {
        return new JAXBElement<ExperimentEnded>(_ExperimentEnded_QNAME, ExperimentEnded.class, null, value);
    }

}
