/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.smartsantander.cea.certificate;

import eu.smartsantander.cea.utils.Helper.HelperUtilities;
import eu.smartsantander.cea.utils.certificate.CertificateUtilies;
import eu.smartsantander.cea.utils.saml.SAMLUtilities;
import com.google.common.base.Joiner;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Organization;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Role;
import de.uniluebeck.itm.tr.snaa.shiro.entity.UsersCert;
import de.uniluebeck.itm.util.TimedCache;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.snaa.Action;
import eu.wisebed.api.v3.snaa.Authenticate;
import eu.wisebed.api.v3.snaa.AuthenticateResponse;
import eu.wisebed.api.v3.snaa.AuthenticationFault;
import eu.wisebed.api.v3.snaa.AuthenticationSAML;
import eu.wisebed.api.v3.snaa.AuthorizationResponse;
import eu.wisebed.api.v3.snaa.SNAAFault_Exception;
import eu.wisebed.api.v3.snaa.ValidationResult;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.jws.WebService;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.joda.time.DateTime;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.Lists;
import static com.google.common.collect.Lists.newArrayList;
import com.google.inject.Provider;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import static de.uniluebeck.itm.tr.snaa.common.SNAAHelper.*;
import de.uniluebeck.itm.tr.snaa.shiro.UrnResourceGroupDao;
import de.uniluebeck.itm.tr.snaa.shiro.entity.UrnResourceGroup;
import eu.wisebed.api.v3.common.NodeUrn;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.jws.WebParam;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;


@WebService(endpointInterface = "eu.wisebed.api.v3.snaa.SNAA", portName = "SNAAPort", serviceName = "SNAAService")
public class SNAACertificate extends AbstractService implements SNAAService {

    /*
     * Logs messages
     */
    private static final Logger log = LoggerFactory.getLogger(SNAACertificate.class);
    
    /**
     * Access authorization for users is performed for nodes which uniform resource locator starts
     * with these prefixes.
     */
    protected final Provider<Set<NodeUrnPrefix>> servedNodeUrnPrefixesProvider;
    
    /**
     * A security component that can access application-specific security entities such as users,
     * roles, and permissions to determine authentication and authorization operations.
     */
    private final Realm realm;
    
    /**
     * Used to generate {@link SecretAuthenticationKey}s
     */
    private final Random r = new SecureRandom();
    
    /**
     * This cache keeps tack of all authenticated sessions for a certain amount of time.<br/>
     * It maps a secret String which is only known to one authenticated user to the user's
     * authentication details.<br/>
     * This cache may be used to check whether the user was authenticated by this server recently
     * whenever this secret String is provided.
     */
    
    private final TimedCache<String, AuthenticationTripleCertificate> authenticatedSessions =
			new TimedCache<String, AuthenticationTripleCertificate>(30, TimeUnit.MINUTES);

    private final ServicePublisher servicePublisher;

    private final org.apache.shiro.mgt.SecurityManager securityManager;

    private final Provider<Subject> currentUserProvider;
    
    private final SNAAServiceConfig snaaServiceConfig;

    private ServicePublisherService jaxWsService;

    
    /**
     * An object which provides access to the persisted groups of resources
     */
    @Inject
    private final UrnResourceGroupDao urnResourceGroupsDAO;
       
    @Inject
    private final UserCertDao userCertDao;
    
    @Inject
    public SNAACertificate( final ServicePublisher servicePublisher,
                            final SecurityManager securityManager,
                            final SNAAServiceConfig snaaServiceConfig, 
                            final ServedNodeUrnPrefixesProvider servedNodeUrnPrefixesProvider, 
                            final UserCertDao userCertDao,
                            final UrnResourceGroupDao urnResourceGroupDao, 
                            final Provider<Subject> currentUserProvider) {
        
        Collection<Realm> realms = ((RealmSecurityManager) securityManager).getRealms();
	checkArgument(realms.size() == 1, "Exactly one realm must be configured");
        
        this.currentUserProvider = currentUserProvider;
        this.realm = realms.iterator().next();
        this.servicePublisher = servicePublisher;
        this.securityManager = securityManager;
        this.snaaServiceConfig = snaaServiceConfig;
        this.servedNodeUrnPrefixesProvider = servedNodeUrnPrefixesProvider;
        this.userCertDao = userCertDao;
        this.urnResourceGroupsDAO = urnResourceGroupDao;
    }
    
    
    @Override
    protected void doStart() {
	try {
		SecurityUtils.setSecurityManager(securityManager);
		jaxWsService = servicePublisher.createJaxWsService(snaaServiceConfig.getSnaaContextPath(), this);
		jaxWsService.startAndWait();
		notifyStarted();
	} catch (Exception e) {
		notifyFailed(e);
	}
    }

    @Override
    protected void doStop() {
	try {
		if (jaxWsService != null) {
			jaxWsService.stopAndWait();
		}
		notifyStopped();
	} catch (Exception e) {
		notifyFailed(e);
	}
    }

    @Override
    public AuthenticateResponse authenticate(Authenticate parameters) throws AuthenticationFault, SNAAFault_Exception {
       List<AuthenticationSAML> authenticationSAMLs = parameters.getCertAuthenticationData();
       List<AuthenticationTripleCertificate> authenticationTripleCertificates = getTripleAuthenticationCertficate(authenticationSAMLs);
       assertAuthenticationCountCert(authenticationTripleCertificates, 1, 1);
       assertAllUrnPrefixesServedCert(servedNodeUrnPrefixesProvider.get(), authenticationTripleCertificates);
       
       AuthenticationTripleCertificate authenticationTripleCertificate = authenticationTripleCertificates.get(0);
       checkNotNull(authenticationTripleCertificate.getUsername(), "The user could not be authenticated: username is null.");
       checkNotNull(authenticationTripleCertificate.getCertificate(), "The user could not be authenticated: certificate is null");
       
        // Authentication
        Subject currentUser = SecurityUtils.getSubject();
        try {
            X509CertificateOnlyAuthenticationToken token = new X509CertificateOnlyAuthenticationToken(
            authenticationTripleCertificate.getUsername(), authenticationTripleCertificate.getCertificate());
            currentUser.login(token);
            currentUser.logout();
        } catch (AuthenticationException e) {
            throw new AuthenticationFault(
                "The user could not be authenticated: Wrong username and/or certificate.", null, e);
        }
        
        String randomLongAsString = Long.toString(r.nextLong());
        authenticatedSessions.put(randomLongAsString, authenticationTripleCertificate);
        /* Create a secret authentication key for the authenticated user */
	SecretAuthenticationKey secretAuthenticationKey = new SecretAuthenticationKey();
	secretAuthenticationKey.setUrnPrefix(authenticationTripleCertificate.getUrnPrefix());
	secretAuthenticationKey.setKey(randomLongAsString);
	secretAuthenticationKey.setUsername(authenticationTripleCertificate.getUsername());

	/* Return the single secret authentication key in a list (due to the federator) */
	final AuthenticateResponse authenticateResponse = new AuthenticateResponse();
	authenticateResponse.getSecretAuthenticationKey().add(secretAuthenticationKey);
	
        return authenticateResponse;
    }

    @Override
    public AuthorizationResponse isAuthorized(
			@WebParam(name = "usernameNodeUrnsMapList", targetNamespace = "")
			List<UsernameNodeUrnsMap> usernameNodeUrnsMaps,
			@WebParam(name = "action", targetNamespace = "")
			Action action)
			throws SNAAFault_Exception {
        
                checkNotNull(action, "action parameter must be non-null (one of " + Joiner.on(", ").join(Action.values()) + ")");
		checkArgument(usernameNodeUrnsMaps.size() == 1,
				"The number of username and node urn mappings must be 1 but is " + usernameNodeUrnsMaps.size()
		);

		UsernameNodeUrnsMap usernameNodeUrnsMapping = usernameNodeUrnsMaps.get(0);
		String userName = usernameNodeUrnsMapping.getUsername();

		checkArgument(
				servedNodeUrnPrefixesProvider.get().contains(usernameNodeUrnsMapping.getUrnPrefix()),
				"The prefix provided along with the user is not served!"
		);

		assertAllNodeUrnPrefixesServed(servedNodeUrnPrefixesProvider.get(), usernameNodeUrnsMapping.getNodeUrns());

                if (!((X509CertificateRealm) realm).doesUserExist(userName)) {
			AuthorizationResponse authorizationResponse = new AuthorizationResponse();
			authorizationResponse.setAuthorized(false);
			authorizationResponse.setMessage("User \"" + userName + "\" is unknown!");
			return authorizationResponse;
		}
		PrincipalCollection principals = new SimplePrincipalCollection(userName, realm.getName());
		Subject subject = new Subject.Builder().principals(principals).buildSubject();

		Set<String> nodeGroups = getNodeGroupsForNodeURNs(usernameNodeUrnsMapping.getNodeUrns());

		AuthorizationResponse authorizationResponse = new AuthorizationResponse();
		authorizationResponse.setAuthorized(true);
		StringBuilder reason = new StringBuilder();
		for (String nodeGroup : nodeGroups) {
			if (!subject.isPermittedAll(action.name() + ":" + nodeGroup)) {
				authorizationResponse.setAuthorized(false);
				reason.append("The action '")
						.append(action.name())
						.append("' is not allowed for node group '")
						.append(nodeGroup)
						.append("' and user '")
						.append(userName)
						.append("'. ");
			}
		}
		subject.logout();

		if (!authorizationResponse.isAuthorized()) {
			authorizationResponse.setMessage(reason.toString());
			log.debug("User requested unauthorized action(s): " + reason.toString());
		} else {
			log.debug("The requested actions were authorized successfully.");
		}

		return authorizationResponse;
    }


    @Override
    public List<ValidationResult> isValid(List<SecretAuthenticationKey> secretAuthenticationKeys) throws SNAAFault_Exception {
        // check whether the urn prefix associated to the key is served at all
	assertAllUrnPrefixesInSAKsAreServed(servedNodeUrnPrefixesProvider.get(), secretAuthenticationKeys);
        
        final SecretAuthenticationKey secretAuthenticationKey = secretAuthenticationKeys.get(0);
        // Get the session from the cache of authenticated sessions
        AuthenticationTripleCertificate authenticationTripleCertificate = authenticatedSessions.get(secretAuthenticationKey.getKey());
        
        ValidationResult result = new ValidationResult();
        
        if (authenticationTripleCertificate == null) {
            result.setValid(false);
            result.setMessage("The provides secret authentication key is not found. It is either invalid or expired.");
        } else if (secretAuthenticationKey.getUsername() == null) {
			result.setValid(false);
			result.setMessage("The user name comprised in the secret authentication key must not be 'null'.");
	} else if (!secretAuthenticationKey.getUsername().equals(authenticationTripleCertificate.getUsername())) {
		result.setValid(false);
		result.setMessage(
				"The user name which was provided by the original authentication does not match the one in the secret authentication key."
        	);
	} else if (!secretAuthenticationKey.getUrnPrefix().equals(authenticationTripleCertificate.getUrnPrefix())) {
		result.setValid(false);
		result.setMessage(
			"The urn prefix which was provided by the original authentication does not match the one in the secret authentication key."
		);
	} else {
		result.setValid(true);
	}

	return newArrayList(result);
    }
    
    /**
	 * Iterates over a collection or node urns and returns the groups of these nodes.
	 *
	 * @param nodeUrns
	 * 		A collection of node urns
	 *
	 * @return A set of those groups at least one of the provided node belongs to
	 *
	 * @throws SNAAFault_Exception
	 * 		Thrown if the provided collection of node urns contains node urns with prefixes which
	 * 		are not served by this SNAA server
	 */
	protected Set<String> getNodeGroupsForNodeURNs(final Collection<NodeUrn> nodeUrns) throws SNAAFault_Exception {

		assertAllNodeUrnPrefixesServed(servedNodeUrnPrefixesProvider.get(), Lists.newLinkedList(nodeUrns));

		Set<String> nodeGroups = new HashSet<String>();
		List<String> nodeUrnStringList = new ArrayList<String>();
		for (NodeUrn nodeUrn : nodeUrns) {
			nodeUrnStringList.add(nodeUrn.getPrefix().toString() + nodeUrn.getSuffix());
		}

		List<UrnResourceGroup> nodeUrnResourceGroup = urnResourceGroupsDAO.find();
		for (UrnResourceGroup grp : nodeUrnResourceGroup) {
			if (nodeUrnStringList.contains(grp.getId().getUrn())) {
				nodeGroups.add(grp.getId().getResourcegroup());
			}
		}

		return nodeGroups;
	}
    
    public List<AuthenticationTripleCertificate> getTripleAuthenticationCertficate(List<AuthenticationSAML> authenticationSAMLs) {
        
        AuthenticationSAML authenticationSAML = authenticationSAMLs.get(0);
        String samlResponseDecoded = HelperUtilities.decode(authenticationSAML.getSAMLAssertion());
        Response samlResponse = SAMLUtilities.getResponseObjectFromRequest(samlResponseDecoded);
        // Verify if the saml assertion is not timed out
        boolean isSamlTimedOut = false;
        Assertion assertion = samlResponse.getAssertions().get(0);
        AuthnStatement authnStatement = assertion.getAuthnStatements().get(0);
        DateTime notAfter = authnStatement.getSessionNotOnOrAfter();
        DateTime now = new DateTime();
        if ((now.compareTo(notAfter)>0)) {
            isSamlTimedOut = true;
        }
        
        if (!isSamlTimedOut) {
            String userId = SAMLUtilities.getAttribute(samlResponse, SNAACertificateConfig.USER_ID_SAML_ATTRIBUTE_NAME);
            String idpId = SAMLUtilities.getAttribute(samlResponse, SNAACertificateConfig.ORGANIZATION_ID_ATTRIBUTE_NAME);
            String role = SAMLUtilities.getAttribute(samlResponse, SNAACertificateConfig.ROLE_ATTRIBUTE_NAME);
            
            // Adding the user's information to the database
            UsersCert user = new UsersCert(userId, new Organization(idpId, null), true);
            user.getRoles().add(new Role(role));
            if (userCertDao.find(userId)==null) {
                userCertDao.save(user);
            } 

            return getUserCredentials(userId, authenticationSAML.getUrnPrefix().getNodeUrnPrefix(), idpId);
        }
        return null;
    }
    
    public List<AuthenticationTripleCertificate> getUserCredentials(String userId, String urnPrefix, String organizationId) {
        List<AuthenticationTripleCertificate> lists = new ArrayList<AuthenticationTripleCertificate>();
        AuthenticationTripleCertificate authenticationTripleCertificate = new AuthenticationTripleCertificate();
        authenticationTripleCertificate.setUsername(userId);
        authenticationTripleCertificate.setUrnPrefix(new NodeUrnPrefix(urnPrefix));
        String userDir = System.getProperty("user.dir");
        X509Certificate cert = null;
        if (!HelperUtilities.isWindows()) {
            cert = CertificateUtilies.getCertificate(userDir+"/"+ SNAACertificateConfig.CERTIFICATE_DIRECTORY_NAME+"/"+organizationId);
        } else {
            cert = CertificateUtilies.getCertificate(userDir+"\\"+SNAACertificateConfig.CERTIFICATE_DIRECTORY_NAME+"\\"+organizationId);
        }
        authenticationTripleCertificate.setCertificate(cert);
        lists.add(authenticationTripleCertificate);
        
        return lists;
    }
    
    public static void assertAuthenticationCountCert(List<AuthenticationTripleCertificate> authenticationData, int minCountInclusive,
												 int maxCountInclusive) throws SNAAFault_Exception {
        try {
            assertCollectionMinMaxCount(authenticationData, minCountInclusive, maxCountInclusive);
	} catch (Exception e) {
		throw createSNAAFault(e.getMessage());
	}
    }
    
    public static void assertAllUrnPrefixesServedCert(Set<NodeUrnPrefix> servedURNPrefixes, List<AuthenticationTripleCertificate> authenticationData)
                                                                                            throws SNAAFault_Exception {
	for (AuthenticationTripleCertificate triple : authenticationData) {
		if (!servedURNPrefixes.contains(triple.getUrnPrefix())) {
			throw createSNAAFault("Not serving node URN prefix " + triple.getUrnPrefix());
		}
	}
    }
}
