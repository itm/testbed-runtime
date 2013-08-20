/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.smartsantander.cea.certificate;

import eu.smartsantander.cea.utils.Helper.HelperUtilities;
import eu.smartsantander.cea.utils.saml.SAMLGenerator;
import eu.smartsantander.cea.utils.saml.SAMLInput;
import eu.smartsantander.cea.utils.saml.SignAssertion;
import static com.google.common.collect.Sets.newHashSet;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import static com.google.inject.util.Providers.of;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import de.uniluebeck.itm.tr.snaa.shiro.ShiroSNAATestBase;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Organization;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Permission;
import de.uniluebeck.itm.tr.snaa.shiro.entity.ResourceGroup;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Role;
import de.uniluebeck.itm.tr.snaa.shiro.entity.UrnResourceGroup;
import de.uniluebeck.itm.tr.snaa.shiro.entity.UrnResourceGroupId;
import de.uniluebeck.itm.tr.snaa.shiro.entity.UsersCert;
import de.uniluebeck.itm.util.logging.LogLevel;
import de.uniluebeck.itm.util.logging.Logging;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import static eu.wisebed.api.v3.snaa.Action.SM_ARE_NODES_ALIVE;
import static eu.wisebed.api.v3.snaa.Action.WSN_ARE_NODES_ALIVE;
import static eu.wisebed.api.v3.snaa.Action.WSN_FLASH_PROGRAMS;
import eu.wisebed.api.v3.snaa.AuthenticationSAML;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;
import org.slf4j.LoggerFactory;

/**
 *
 *
 */
public abstract class SNAACertificateTestBase {
    static {
		Logging.setLoggingDefaults(LogLevel.WARN);
	}

	protected static final org.slf4j.Logger log = LoggerFactory.getLogger(ShiroSNAATestBase.class);

	protected static final String SERVICE_PROVIDER1 = "ServiceProvider1";

        protected static final String EXPERIMENTER1 = "Experimenter1";
        
	protected static final String ADMINISTRATOR1 = "Administrator1";

        protected static final String ORGANISATIONID = "Organization1";
        
	protected static final NodeUrnPrefix NODE_URN_PREFIX_1 = new NodeUrnPrefix("urn:wisebed:uzl2:");
        
        protected static final NodeUrnPrefix NODE_URN_PREFIX_2 = new NodeUrnPrefix("urn:wisebed:uzl1:");

        protected static final String currentUserDir = System.getProperty("user.dir");
        
        // Additional information to create a signed saml assertion
        
        protected static final String pathToKeystoreFile = currentUserDir+"/certs/keystore/keystore.jks";
        
        protected static final String keystorePassword = "changeit";
        
        protected static final String certificateAliasName = "organization-alias";
        
        protected static final String ROLE_ADMIN = "ADMINISTRATOR";
        
        protected static final String ROLE_EXPERIMENTER = "EXPERIMENTER";
        
	@Mock
	protected CommonConfig commonConfig;

	@Mock
	protected SNAAServiceConfig snaaServiceConfig;

	@Mock
	protected ServicePublisher servicePublisher;

	@Mock
	protected ServicePublisherService servicePublisherService;

	@Mock
	protected ServedNodeUrnPrefixesProvider servedNodeUrnPrefixesProvider;

	protected SNAACertificate snaaCertificate;

	public void setUp(final Module jpaModule) throws Exception {

		when(servicePublisher.createJaxWsService(anyString(), anyObject())).thenReturn(servicePublisherService);
		when(commonConfig.getUrnPrefix()).thenReturn(NODE_URN_PREFIX_1);
		when(servedNodeUrnPrefixesProvider.get()).thenReturn(newHashSet(NODE_URN_PREFIX_1, NODE_URN_PREFIX_2));
		when(snaaServiceConfig.getShiroJpaProperties()).thenReturn(new Properties());
		
		final AbstractModule mocksModule = new AbstractModule() {
			@Override
			protected void configure() {
				bind(CommonConfig.class).toProvider(of(commonConfig));
				bind(SNAAServiceConfig.class).toProvider(of(snaaServiceConfig));
				bind(ServicePublisher.class).toInstance(servicePublisher);
				bind(ServedNodeUrnPrefixesProvider.class).toInstance(servedNodeUrnPrefixesProvider);
			}
		};

		
                SNAACertificateModule snaaCertificateModule = new SNAACertificateModule();
		Injector injector = Guice.createInjector(jpaModule, mocksModule, snaaCertificateModule);

		snaaCertificate = injector.getInstance(SNAACertificate.class);
		snaaCertificate.startAndWait();
	}

        
        /* ------------------------------ Helpers ----------------------------------- */

	protected List<UsernameNodeUrnsMap> createUsernameNodeUrnsMapList(String username, NodeUrnPrefix nodeUrnPrefix,
																	  String... nodeUrnStrings) {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = new LinkedList<UsernameNodeUrnsMap>();
		UsernameNodeUrnsMap map = new UsernameNodeUrnsMap();
		map.setUrnPrefix(nodeUrnPrefix);
		map.setUsername(username);

		List<NodeUrn> nodeUrns = map.getNodeUrns();

		for (String nodeUrnString : nodeUrnStrings) {
			nodeUrns.add(new NodeUrn(nodeUrnString));
		}

		usernameNodeUrnsMaps.add(map);
		return usernameNodeUrnsMaps;
	}

        protected Permission getPermissionsObject(eu.wisebed.api.v3.snaa.Action action, String resourceGroupsName) {
		Permission permission = new Permission();
		permission.setAction(new de.uniluebeck.itm.tr.snaa.shiro.entity.Action(action.name()));
		permission.setResourceGroup(new ResourceGroup(resourceGroupsName));
		return permission;
	}
        
        /**
	 * Mocks returning a user which has the role "EXPERIMENTER" from the data base.
	 *
	 * @return a user which has the role "Experimenter" from the data base.
	 */
	protected UsersCert getExperimenter1() {
		Role role = new Role("EXPERIMENTER");
		role.setPermissions(newHashSet(getPermissionsObject(WSN_FLASH_PROGRAMS, "EXPERIMENT_ONLY_NODES")));
		return new UsersCert(EXPERIMENTER1, new Organization("cea", "url"), true, newHashSet(role));
	}
        
        /**
	 * Mocks returning a user which has the role "ADMINISTRATOR" from the data base.
	 *
	 * @return a user which has the role "Experimenter" from the data base.
	 */
	protected UsersCert getAdministrator1() {
		Role role = new Role("ADMINISTRATOR");
		Set<Permission> permissionsSet = new HashSet<Permission>();
		role.setPermissions(permissionsSet);
		permissionsSet.add(getPermissionsObject(WSN_ARE_NODES_ALIVE, "SERVICE_ONLY_NODES"));
		permissionsSet.add(getPermissionsObject(SM_ARE_NODES_ALIVE, "EXPERIMENT_ONLY_NODES"));
		return new UsersCert(ADMINISTRATOR1,new Organization("cea", "url"), true, newHashSet(role));
	}
        
        /**
	 * Mocks returning a user which has the role "SERVICE_PROVIDER" from the data base.
	 *
	 * @return a user which has the role "Experimenter" from the data base.
	 */
	protected UsersCert getServiceProvider1() {
		Role role = new Role("SERVICE_PROVIDER");
		role.setPermissions(newHashSet(getPermissionsObject(WSN_ARE_NODES_ALIVE, "SERVICE_ONLY_NODES")));
		return new UsersCert(SERVICE_PROVIDER1, new Organization("cea", "url"), true, newHashSet(role));
	}

        protected List<UrnResourceGroup> getUrnResourceGroup() {
		List<UrnResourceGroup> UrnResourceGroupList = new LinkedList<UrnResourceGroup>();
		UrnResourceGroupList
				.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:ulanc1:0x1211", "EXPERIMENT_ONLY_NODES"),
						null
				)
				);

		UrnResourceGroupList
				.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:ulanc1:0x1211", "EXPERIMENT_ONLY_NODES"),
						null
				)
				);
		UrnResourceGroupList
				.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:ulanc1:0x1212", "EXPERIMENT_ONLY_NODES"),
						null
				)
				);
		UrnResourceGroupList
				.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:ulanc1:0x1213", "EXPERIMENT_ONLY_NODES"),
						null
				)
				);
		UrnResourceGroupList
				.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:ulanc1:0x1311", "SERVICE_ONLY_NODES"),
						null
				)
				);
		UrnResourceGroupList
				.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:ulanc1:0x1312", "SERVICE_ONLY_NODES"),
						null
				)
				);
		UrnResourceGroupList
				.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:ulanc1:0x1313", "SERVICE_ONLY_NODES"),
						null
				)
				);

		UrnResourceGroupList
				.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:uzl2:0x2211", "EXPERIMENT_ONLY_NODES"),
						null
				)
				);
		UrnResourceGroupList
				.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:uzl2:0x2212", "EXPERIMENT_ONLY_NODES"),
						null
				)
				);
		UrnResourceGroupList
				.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:uzl2:0x2213", "EXPERIMENT_ONLY_NODES"),
						null
				)
				);
		UrnResourceGroupList
				.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:uzl2:0x2311", "SERVICE_ONLY_NODES"), null
				)
				);
		UrnResourceGroupList
				.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:uzl2:0x2312", "SERVICE_ONLY_NODES"), null
				)
				);
		UrnResourceGroupList
				.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:uzl2:0x2313", "SERVICE_ONLY_NODES"), null
				)
				);

		return UrnResourceGroupList;
	}
        
        public String createSAMLResponse(String userId, String idpId, String role) {
            Map attributs = new HashMap();
            attributs.put(SNAACertificateConfig.USER_ID_SAML_ATTRIBUTE_NAME, userId);
            attributs.put(SNAACertificateConfig.ORGANIZATION_ID_ATTRIBUTE_NAME, idpId);
            attributs.put(SNAACertificateConfig.ROLE_ATTRIBUTE_NAME, role);
            
            // Create a SAML Reponse composing an assertion and its signature
            SAMLInput input = new SAMLInput(idpId, userId, "defautlNameQualifier", "defaultsessionId", 10, attributs);
            SAMLGenerator generator = new SAMLGenerator();
            Assertion assertion  = generator.createAssertion(input);
            SignAssertion signer = new SignAssertion(assertion, pathToKeystoreFile, keystorePassword, certificateAliasName);
            org.opensaml.xml.security.credential.Credential credential = signer.getSignningCredentialFromKeyStore();
            Response res = signer.singnAssertion(credential);
            String samlResponseInString = HelperUtilities.samlResponseToString(res);

            // Encode SAML Response in Base64
            String samlRespEncoded = HelperUtilities.encode(samlResponseInString);
            return samlRespEncoded;
        }
        
        public List<AuthenticationSAML> getAuthenticationInformation(String userId, String idpId, String role, NodeUrnPrefix nodeUrnPrefix) {
            List<AuthenticationSAML> authenticationSAMLs = new ArrayList<AuthenticationSAML>();
            AuthenticationSAML authenticationSAML = new AuthenticationSAML();
            String samlResp = createSAMLResponse(userId, idpId, role);
            authenticationSAML.setSAMLAssertion(samlResp);
            authenticationSAML.setUrnPrefix(nodeUrnPrefix);
            authenticationSAMLs.add(authenticationSAML);
            return authenticationSAMLs;
        }
}
