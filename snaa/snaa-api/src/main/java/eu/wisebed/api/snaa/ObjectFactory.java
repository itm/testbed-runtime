
package eu.wisebed.api.snaa;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the eu.wisebed.api.snaa package. 
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

    private final static QName _AuthenticateResponse_QNAME = new QName("http://testbed.wisebed.eu/api/snaa/v1/", "authenticateResponse");
    private final static QName _IsAuthorized_QNAME = new QName("http://testbed.wisebed.eu/api/snaa/v1/", "isAuthorized");
    private final static QName _SNAAFault_QNAME = new QName("http://testbed.wisebed.eu/api/snaa/v1/", "SNAAFault");
    private final static QName _AuthenticationFault_QNAME = new QName("http://testbed.wisebed.eu/api/snaa/v1/", "AuthenticationFault");
    private final static QName _Authenticate_QNAME = new QName("http://testbed.wisebed.eu/api/snaa/v1/", "authenticate");
    private final static QName _IsAuthorizedResponse_QNAME = new QName("http://testbed.wisebed.eu/api/snaa/v1/", "isAuthorizedResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: eu.wisebed.api.snaa
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Action }
     * 
     */
    public Action createAction() {
        return new Action();
    }

    /**
     * Create an instance of {@link SNAAException }
     * 
     */
    public SNAAException createSNAAException() {
        return new SNAAException();
    }

    /**
     * Create an instance of {@link AuthenticationException }
     * 
     */
    public AuthenticationException createAuthenticationException() {
        return new AuthenticationException();
    }

    /**
     * Create an instance of {@link Authenticate }
     * 
     */
    public Authenticate createAuthenticate() {
        return new Authenticate();
    }

    /**
     * Create an instance of {@link SecretAuthenticationKey }
     * 
     */
    public SecretAuthenticationKey createSecretAuthenticationKey() {
        return new SecretAuthenticationKey();
    }

    /**
     * Create an instance of {@link IsAuthorized }
     * 
     */
    public IsAuthorized createIsAuthorized() {
        return new IsAuthorized();
    }

    /**
     * Create an instance of {@link AuthenticateResponse }
     * 
     */
    public AuthenticateResponse createAuthenticateResponse() {
        return new AuthenticateResponse();
    }

    /**
     * Create an instance of {@link IsAuthorizedResponse }
     * 
     */
    public IsAuthorizedResponse createIsAuthorizedResponse() {
        return new IsAuthorizedResponse();
    }

    /**
     * Create an instance of {@link AuthenticationTriple }
     * 
     */
    public AuthenticationTriple createAuthenticationTriple() {
        return new AuthenticationTriple();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AuthenticateResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://testbed.wisebed.eu/api/snaa/v1/", name = "authenticateResponse")
    public JAXBElement<AuthenticateResponse> createAuthenticateResponse(AuthenticateResponse value) {
        return new JAXBElement<AuthenticateResponse>(_AuthenticateResponse_QNAME, AuthenticateResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IsAuthorized }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://testbed.wisebed.eu/api/snaa/v1/", name = "isAuthorized")
    public JAXBElement<IsAuthorized> createIsAuthorized(IsAuthorized value) {
        return new JAXBElement<IsAuthorized>(_IsAuthorized_QNAME, IsAuthorized.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SNAAException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://testbed.wisebed.eu/api/snaa/v1/", name = "SNAAFault")
    public JAXBElement<SNAAException> createSNAAFault(SNAAException value) {
        return new JAXBElement<SNAAException>(_SNAAFault_QNAME, SNAAException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AuthenticationException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://testbed.wisebed.eu/api/snaa/v1/", name = "AuthenticationFault")
    public JAXBElement<AuthenticationException> createAuthenticationFault(AuthenticationException value) {
        return new JAXBElement<AuthenticationException>(_AuthenticationFault_QNAME, AuthenticationException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Authenticate }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://testbed.wisebed.eu/api/snaa/v1/", name = "authenticate")
    public JAXBElement<Authenticate> createAuthenticate(Authenticate value) {
        return new JAXBElement<Authenticate>(_Authenticate_QNAME, Authenticate.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IsAuthorizedResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://testbed.wisebed.eu/api/snaa/v1/", name = "isAuthorizedResponse")
    public JAXBElement<IsAuthorizedResponse> createIsAuthorizedResponse(IsAuthorizedResponse value) {
        return new JAXBElement<IsAuthorizedResponse>(_IsAuthorizedResponse_QNAME, IsAuthorizedResponse.class, null, value);
    }

}
