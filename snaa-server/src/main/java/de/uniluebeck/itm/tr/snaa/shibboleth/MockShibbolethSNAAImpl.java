package de.uniluebeck.itm.tr.snaa.shibboleth;

import eu.wisebed.api.common.SecretAuthenticationKey;
import eu.wisebed.api.common.UsernameUrnPrefixPair;
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
    public AuthorizationResponse isAuthorized(
            @WebParam(name = "usernames", targetNamespace = "")
            List<UsernameUrnPrefixPair> usernames,
            @WebParam(name = "action", targetNamespace = "")
            Action action,
            @WebParam(name = "nodeUrns", targetNamespace = "")
            String nodeUrns)
            throws SNAAExceptionException {
    	AuthorizationResponse response = new AuthorizationResponse();
    	response.setMessage("MockShibbolethSNAAImpl will always return 'false'");
    	response.setAuthorized(false);
    	response.setNodeUrn(nodeUrns);
        return response; 
    }
}
