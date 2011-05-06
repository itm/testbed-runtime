
package eu.wisebed.api.sm;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the eu.wisebed.api.sm package. 
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

    private final static QName _GetConfiguration_QNAME = new QName("urn:SessionManagementService", "getConfiguration");
    private final static QName _AreNodesAliveResponse_QNAME = new QName("urn:SessionManagementService", "areNodesAliveResponse");
    private final static QName _GetConfigurationResponse_QNAME = new QName("urn:SessionManagementService", "getConfigurationResponse");
    private final static QName _Free_QNAME = new QName("urn:SessionManagementService", "free");
    private final static QName _GetInstanceResponse_QNAME = new QName("urn:SessionManagementService", "getInstanceResponse");
    private final static QName _UnknownReservationIdException_QNAME = new QName("urn:SessionManagementService", "UnknownReservationIdException");
    private final static QName _AreNodesAlive_QNAME = new QName("urn:SessionManagementService", "areNodesAlive");
    private final static QName _ExperimentNotRunningException_QNAME = new QName("urn:SessionManagementService", "ExperimentNotRunningException");
    private final static QName _GetInstance_QNAME = new QName("urn:SessionManagementService", "getInstance");
    private final static QName _FreeResponse_QNAME = new QName("urn:SessionManagementService", "freeResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: eu.wisebed.api.sm
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GetConfigurationResponse }
     * 
     */
    public GetConfigurationResponse createGetConfigurationResponse() {
        return new GetConfigurationResponse();
    }

    /**
     * Create an instance of {@link UnknownReservationIdException }
     * 
     */
    public UnknownReservationIdException createUnknownReservationIdException() {
        return new UnknownReservationIdException();
    }

    /**
     * Create an instance of {@link SecretReservationKey }
     * 
     */
    public SecretReservationKey createSecretReservationKey() {
        return new SecretReservationKey();
    }

    /**
     * Create an instance of {@link Free }
     * 
     */
    public Free createFree() {
        return new Free();
    }

    /**
     * Create an instance of {@link GetInstance }
     * 
     */
    public GetInstance createGetInstance() {
        return new GetInstance();
    }

    /**
     * Create an instance of {@link GetInstanceResponse }
     * 
     */
    public GetInstanceResponse createGetInstanceResponse() {
        return new GetInstanceResponse();
    }

    /**
     * Create an instance of {@link AreNodesAlive }
     * 
     */
    public AreNodesAlive createAreNodesAlive() {
        return new AreNodesAlive();
    }

    /**
     * Create an instance of {@link GetConfiguration }
     * 
     */
    public GetConfiguration createGetConfiguration() {
        return new GetConfiguration();
    }

    /**
     * Create an instance of {@link AreNodesAliveResponse }
     * 
     */
    public AreNodesAliveResponse createAreNodesAliveResponse() {
        return new AreNodesAliveResponse();
    }

    /**
     * Create an instance of {@link FreeResponse }
     * 
     */
    public FreeResponse createFreeResponse() {
        return new FreeResponse();
    }

    /**
     * Create an instance of {@link ExperimentNotRunningException }
     * 
     */
    public ExperimentNotRunningException createExperimentNotRunningException() {
        return new ExperimentNotRunningException();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetConfiguration }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:SessionManagementService", name = "getConfiguration")
    public JAXBElement<GetConfiguration> createGetConfiguration(GetConfiguration value) {
        return new JAXBElement<GetConfiguration>(_GetConfiguration_QNAME, GetConfiguration.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AreNodesAliveResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:SessionManagementService", name = "areNodesAliveResponse")
    public JAXBElement<AreNodesAliveResponse> createAreNodesAliveResponse(AreNodesAliveResponse value) {
        return new JAXBElement<AreNodesAliveResponse>(_AreNodesAliveResponse_QNAME, AreNodesAliveResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetConfigurationResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:SessionManagementService", name = "getConfigurationResponse")
    public JAXBElement<GetConfigurationResponse> createGetConfigurationResponse(GetConfigurationResponse value) {
        return new JAXBElement<GetConfigurationResponse>(_GetConfigurationResponse_QNAME, GetConfigurationResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Free }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:SessionManagementService", name = "free")
    public JAXBElement<Free> createFree(Free value) {
        return new JAXBElement<Free>(_Free_QNAME, Free.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetInstanceResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:SessionManagementService", name = "getInstanceResponse")
    public JAXBElement<GetInstanceResponse> createGetInstanceResponse(GetInstanceResponse value) {
        return new JAXBElement<GetInstanceResponse>(_GetInstanceResponse_QNAME, GetInstanceResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UnknownReservationIdException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:SessionManagementService", name = "UnknownReservationIdException")
    public JAXBElement<UnknownReservationIdException> createUnknownReservationIdException(UnknownReservationIdException value) {
        return new JAXBElement<UnknownReservationIdException>(_UnknownReservationIdException_QNAME, UnknownReservationIdException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AreNodesAlive }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:SessionManagementService", name = "areNodesAlive")
    public JAXBElement<AreNodesAlive> createAreNodesAlive(AreNodesAlive value) {
        return new JAXBElement<AreNodesAlive>(_AreNodesAlive_QNAME, AreNodesAlive.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExperimentNotRunningException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:SessionManagementService", name = "ExperimentNotRunningException")
    public JAXBElement<ExperimentNotRunningException> createExperimentNotRunningException(ExperimentNotRunningException value) {
        return new JAXBElement<ExperimentNotRunningException>(_ExperimentNotRunningException_QNAME, ExperimentNotRunningException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetInstance }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:SessionManagementService", name = "getInstance")
    public JAXBElement<GetInstance> createGetInstance(GetInstance value) {
        return new JAXBElement<GetInstance>(_GetInstance_QNAME, GetInstance.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FreeResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:SessionManagementService", name = "freeResponse")
    public JAXBElement<FreeResponse> createFreeResponse(FreeResponse value) {
        return new JAXBElement<FreeResponse>(_FreeResponse_QNAME, FreeResponse.class, null, value);
    }

}
