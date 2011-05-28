
package eu.wisebed.api.rs;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the eu.wisebed.api.rs package. 
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

    private final static QName _GetReservation_QNAME = new QName("urn:RSService", "getReservation");
    private final static QName _GetReservationsResponse_QNAME = new QName("urn:RSService", "getReservationsResponse");
    private final static QName _MakeReservation_QNAME = new QName("urn:RSService", "makeReservation");
    private final static QName _GetReservations_QNAME = new QName("urn:RSService", "getReservations");
    private final static QName _AuthorizationFault_QNAME = new QName("urn:RSService", "AuthorizationFault");
    private final static QName _DeleteReservationResponse_QNAME = new QName("urn:RSService", "deleteReservationResponse");
    private final static QName _RSFault_QNAME = new QName("urn:RSService", "RSFault");
    private final static QName _GetConfidentialReservationsResponse_QNAME = new QName("urn:RSService", "getConfidentialReservationsResponse");
    private final static QName _ReservationConflictFault_QNAME = new QName("urn:RSService", "ReservationConflictFault");
    private final static QName _GetReservationResponse_QNAME = new QName("urn:RSService", "getReservationResponse");
    private final static QName _ReservationNotFoundFault_QNAME = new QName("urn:RSService", "ReservationNotFoundFault");
    private final static QName _GetConfidentialReservations_QNAME = new QName("urn:RSService", "getConfidentialReservations");
    private final static QName _DeleteReservation_QNAME = new QName("urn:RSService", "deleteReservation");
    private final static QName _MakeReservationResponse_QNAME = new QName("urn:RSService", "makeReservationResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: eu.wisebed.api.rs
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GetReservationsResponse }
     * 
     */
    public GetReservationsResponse createGetReservationsResponse() {
        return new GetReservationsResponse();
    }

    /**
     * Create an instance of {@link MakeReservation }
     * 
     */
    public MakeReservation createMakeReservation() {
        return new MakeReservation();
    }

    /**
     * Create an instance of {@link Data }
     * 
     */
    public Data createData() {
        return new Data();
    }

    /**
     * Create an instance of {@link GetReservations }
     * 
     */
    public GetReservations createGetReservations() {
        return new GetReservations();
    }

    /**
     * Create an instance of {@link SecretAuthenticationKey }
     * 
     */
    public SecretAuthenticationKey createSecretAuthenticationKey() {
        return new SecretAuthenticationKey();
    }

    /**
     * Create an instance of {@link GetReservationResponse }
     * 
     */
    public GetReservationResponse createGetReservationResponse() {
        return new GetReservationResponse();
    }

    /**
     * Create an instance of {@link GetConfidentialReservationsResponse }
     * 
     */
    public GetConfidentialReservationsResponse createGetConfidentialReservationsResponse() {
        return new GetConfidentialReservationsResponse();
    }

    /**
     * Create an instance of {@link DeleteReservationResponse }
     * 
     */
    public DeleteReservationResponse createDeleteReservationResponse() {
        return new DeleteReservationResponse();
    }

    /**
     * Create an instance of {@link GetReservation }
     * 
     */
    public GetReservation createGetReservation() {
        return new GetReservation();
    }

    /**
     * Create an instance of {@link ReservervationConflictException }
     * 
     */
    public ReservervationConflictException createReservervationConflictException() {
        return new ReservervationConflictException();
    }

    /**
     * Create an instance of {@link RSException }
     * 
     */
    public RSException createRSException() {
        return new RSException();
    }

    /**
     * Create an instance of {@link AuthorizationException }
     * 
     */
    public AuthorizationException createAuthorizationException() {
        return new AuthorizationException();
    }

    /**
     * Create an instance of {@link MakeReservationResponse }
     * 
     */
    public MakeReservationResponse createMakeReservationResponse() {
        return new MakeReservationResponse();
    }

    /**
     * Create an instance of {@link DeleteReservation }
     * 
     */
    public DeleteReservation createDeleteReservation() {
        return new DeleteReservation();
    }

    /**
     * Create an instance of {@link PublicReservationData }
     * 
     */
    public PublicReservationData createPublicReservationData() {
        return new PublicReservationData();
    }

    /**
     * Create an instance of {@link SecretReservationKey }
     * 
     */
    public SecretReservationKey createSecretReservationKey() {
        return new SecretReservationKey();
    }

    /**
     * Create an instance of {@link GetConfidentialReservations }
     * 
     */
    public GetConfidentialReservations createGetConfidentialReservations() {
        return new GetConfidentialReservations();
    }

    /**
     * Create an instance of {@link ConfidentialReservationData }
     * 
     */
    public ConfidentialReservationData createConfidentialReservationData() {
        return new ConfidentialReservationData();
    }

    /**
     * Create an instance of {@link ReservervationNotFoundException }
     * 
     */
    public ReservervationNotFoundException createReservervationNotFoundException() {
        return new ReservervationNotFoundException();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetReservation }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:RSService", name = "getReservation")
    public JAXBElement<GetReservation> createGetReservation(GetReservation value) {
        return new JAXBElement<GetReservation>(_GetReservation_QNAME, GetReservation.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetReservationsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:RSService", name = "getReservationsResponse")
    public JAXBElement<GetReservationsResponse> createGetReservationsResponse(GetReservationsResponse value) {
        return new JAXBElement<GetReservationsResponse>(_GetReservationsResponse_QNAME, GetReservationsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MakeReservation }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:RSService", name = "makeReservation")
    public JAXBElement<MakeReservation> createMakeReservation(MakeReservation value) {
        return new JAXBElement<MakeReservation>(_MakeReservation_QNAME, MakeReservation.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetReservations }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:RSService", name = "getReservations")
    public JAXBElement<GetReservations> createGetReservations(GetReservations value) {
        return new JAXBElement<GetReservations>(_GetReservations_QNAME, GetReservations.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AuthorizationException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:RSService", name = "AuthorizationFault")
    public JAXBElement<AuthorizationException> createAuthorizationFault(AuthorizationException value) {
        return new JAXBElement<AuthorizationException>(_AuthorizationFault_QNAME, AuthorizationException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DeleteReservationResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:RSService", name = "deleteReservationResponse")
    public JAXBElement<DeleteReservationResponse> createDeleteReservationResponse(DeleteReservationResponse value) {
        return new JAXBElement<DeleteReservationResponse>(_DeleteReservationResponse_QNAME, DeleteReservationResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RSException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:RSService", name = "RSFault")
    public JAXBElement<RSException> createRSFault(RSException value) {
        return new JAXBElement<RSException>(_RSFault_QNAME, RSException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetConfidentialReservationsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:RSService", name = "getConfidentialReservationsResponse")
    public JAXBElement<GetConfidentialReservationsResponse> createGetConfidentialReservationsResponse(GetConfidentialReservationsResponse value) {
        return new JAXBElement<GetConfidentialReservationsResponse>(_GetConfidentialReservationsResponse_QNAME, GetConfidentialReservationsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReservervationConflictException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:RSService", name = "ReservationConflictFault")
    public JAXBElement<ReservervationConflictException> createReservationConflictFault(ReservervationConflictException value) {
        return new JAXBElement<ReservervationConflictException>(_ReservationConflictFault_QNAME, ReservervationConflictException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetReservationResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:RSService", name = "getReservationResponse")
    public JAXBElement<GetReservationResponse> createGetReservationResponse(GetReservationResponse value) {
        return new JAXBElement<GetReservationResponse>(_GetReservationResponse_QNAME, GetReservationResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReservervationNotFoundException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:RSService", name = "ReservationNotFoundFault")
    public JAXBElement<ReservervationNotFoundException> createReservationNotFoundFault(ReservervationNotFoundException value) {
        return new JAXBElement<ReservervationNotFoundException>(_ReservationNotFoundFault_QNAME, ReservervationNotFoundException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetConfidentialReservations }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:RSService", name = "getConfidentialReservations")
    public JAXBElement<GetConfidentialReservations> createGetConfidentialReservations(GetConfidentialReservations value) {
        return new JAXBElement<GetConfidentialReservations>(_GetConfidentialReservations_QNAME, GetConfidentialReservations.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DeleteReservation }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:RSService", name = "deleteReservation")
    public JAXBElement<DeleteReservation> createDeleteReservation(DeleteReservation value) {
        return new JAXBElement<DeleteReservation>(_DeleteReservation_QNAME, DeleteReservation.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MakeReservationResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:RSService", name = "makeReservationResponse")
    public JAXBElement<MakeReservationResponse> createMakeReservationResponse(MakeReservationResponse value) {
        return new JAXBElement<MakeReservationResponse>(_MakeReservationResponse_QNAME, MakeReservationResponse.class, null, value);
    }

}
