package de.uniluebeck.itm.tr.snaa.shibboleth;

import eu.wisebed.api.snaa.*;

import javax.jws.WebParam;
import javax.jws.WebService;
import java.util.List;

@WebService(endpointInterface = "eu.wisebed.api.snaa.SNAA", portName = "SNAAPort", serviceName = "SNAAService", targetNamespace = "http://testbed.wisebed.eu/api/snaa/v1/")
public class MockShibbolethSNAAImpl implements SNAA {

    @Override
    public List<SecretAuthenticationKey> authenticate(@WebParam(name = "authenticationData", targetNamespace = "") List<AuthenticationTriple> authenticationData) throws AuthenticationExceptionException, SNAAExceptionException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isAuthorized(@WebParam(name = "authenticationData", targetNamespace = "") List<SecretAuthenticationKey> authenticationData, @WebParam(name = "action", targetNamespace = "") Action action) throws SNAAExceptionException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
