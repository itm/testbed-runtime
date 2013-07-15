package de.uniluebeck.itm.tr.snaa.shiro;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import de.uniluebeck.itm.tr.snaa.shiro.entity.*;
import de.uniluebeck.itm.util.logging.LogLevel;
import de.uniluebeck.itm.util.logging.Logging;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.snaa.Action;
import eu.wisebed.api.v3.snaa.AuthenticationFault;
import eu.wisebed.api.v3.snaa.AuthenticationTriple;
import eu.wisebed.api.v3.snaa.SNAAFault_Exception;
import org.apache.shiro.crypto.hash.Sha512Hash;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static com.google.inject.util.Providers.of;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public abstract class ShiroSNAATestBase {

	static {
		Logging.setLoggingDefaults(LogLevel.WARN);
	}

	protected static final org.slf4j.Logger log = LoggerFactory.getLogger(ShiroSNAATestBase.class);

	protected static final String EXPERIMENTER1_PASS = "Exp1Pass";

	protected static final String EXPERIMENTER1_SALT = "Exp1Salt";

	protected static final String EXPERIMENTER1_PASS_HASHED = new Sha512Hash(
			EXPERIMENTER1_PASS,
			EXPERIMENTER1_SALT,
			1000
	).toHex();

	protected static final String EXPERIMENTER1 = "Experimenter1";

	protected static final String EXPERIMENTER2_PASS = "Exp2Pass";

	protected static final String SERVICE_PROVIDER1_PASS = "SP1Pass";

	protected static final String SERVICE_PROVIDER1_SALT = "SP1PSalt";

	protected static final String SERVICE_PROVIDER1_PASS_HASHED = new Sha512Hash(
			SERVICE_PROVIDER1_PASS,
			SERVICE_PROVIDER1_SALT,
			1000
	).toHex();

	protected static final String SERVICE_PROVIDER1 = "ServiceProvider1";

	protected static final String ADMINISTRATOR1_PASS = "Adm1Pass";

	protected static final String ADMINISTRATOR1_SALT = "Adm1Salt";

	protected static final String ADMINISTRATOR1_PASS_HASHED = new Sha512Hash(
			ADMINISTRATOR1_PASS,
			ADMINISTRATOR1_SALT,
			1000
	).toHex();

	protected static final String ADMINISTRATOR1 = "Administrator1";

	protected static final NodeUrnPrefix NODE_URN_PREFIX_1 = new NodeUrnPrefix("urn:wisebed:uzl2:");

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

	protected ShiroSNAA shiroSNAA;

	public void setUp(final Module jpaModule) throws Exception {

		when(servicePublisher.createJaxWsService(anyString(), anyObject())).thenReturn(servicePublisherService);
		when(commonConfig.getUrnPrefix()).thenReturn(NODE_URN_PREFIX_1);
		when(servedNodeUrnPrefixesProvider.get()).thenReturn(newHashSet(NODE_URN_PREFIX_1));
		when(snaaServiceConfig.getShiroJpaProperties()).thenReturn(new Properties());
		when(snaaServiceConfig.getShiroHashAlgorithmName()).thenReturn(Sha512Hash.ALGORITHM_NAME);
		when(snaaServiceConfig.getShiroHashAlgorithmIterations()).thenReturn(1000);

		final AbstractModule mocksModule = new AbstractModule() {
			@Override
			protected void configure() {
				bind(CommonConfig.class).toProvider(of(commonConfig));
				bind(SNAAServiceConfig.class).toProvider(of(snaaServiceConfig));
				bind(ServicePublisher.class).toInstance(servicePublisher);
				bind(ServedNodeUrnPrefixesProvider.class).toInstance(servedNodeUrnPrefixesProvider);
			}
		};

		ShiroSNAAModule shiroSNAAModule = new ShiroSNAAModule(snaaServiceConfig);
		Injector injector = Guice.createInjector(jpaModule, mocksModule, shiroSNAAModule);

		shiroSNAA = injector.getInstance(ShiroSNAA.class);
		shiroSNAA.startAndWait();
	}

	@Test
	public void testAuthenticationForExperimenter1() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		try {
			List<SecretAuthenticationKey> sakList = shiroSNAA.authenticate(authenticationData);
			assertNotNull(sakList);
			assertEquals(EXPERIMENTER1, sakList.get(0).getUsername());
			assertEquals(NODE_URN_PREFIX_1, sakList.get(0).getUrnPrefix());
			assertNotNull(sakList.get(0).getKey());
		} catch (AuthenticationFault e) {
			log.error(e.getMessage(), e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}

	}

	@Test
	public void testAuthenticationFailForExperimenter1DueToWrongPassword() {

		AuthenticationTriple authTriple = new AuthenticationTriple();
		authTriple.setUsername(EXPERIMENTER1);
		authTriple.setPassword(EXPERIMENTER2_PASS);
		authTriple.setUrnPrefix(NODE_URN_PREFIX_1);
		List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
		authenticationData.add(authTriple);
		try {
			shiroSNAA.authenticate(authenticationData);
			fail();
		} catch (AuthenticationFault e) {
			// an exception has to be thrown
		} catch (SNAAFault_Exception e) {
			// an exception has to be thrown
		}
	}

	@Test
	public void testAuthenticationForServiceProvider1() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForServiceProvider1();
		try {
			List<SecretAuthenticationKey> sakList = shiroSNAA.authenticate(authenticationData);
			assertNotNull(sakList);
			assertEquals(SERVICE_PROVIDER1, sakList.get(0).getUsername());
			assertEquals(NODE_URN_PREFIX_1, sakList.get(0).getUrnPrefix());
			assertNotNull(sakList.get(0).getKey());
		} catch (AuthenticationFault e) {
			log.error(e.getMessage(), e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}

	}

	@Test
	public void testIsValidWhenUsernameIsNull() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListWithEmptyUserName();
		try {
			shiroSNAA.authenticate(authenticationData);
		} catch (NullPointerException e) {
			assertEquals("The user could not be authenticated: username is null.", e.getMessage());
			return;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
		fail();
	}

	@Test
	public void testIsValidWhenValid() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		List<SecretAuthenticationKey> sakList = null;
		try {
			sakList = shiroSNAA.authenticate(authenticationData);
		} catch (AuthenticationFault e) {
			log.error(e.getMessage(), e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}

		try {
			assertTrue(shiroSNAA.isValid(sakList).get(0).isValid());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
	}

	@Test
	public void testIsValidWhenUsernameWasChanged() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		List<SecretAuthenticationKey> sakList = null;
		try {
			sakList = shiroSNAA.authenticate(authenticationData);
		} catch (AuthenticationFault e) {
			log.error(e.getMessage(), e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
		sakList.get(0).setUsername(ADMINISTRATOR1);
		try {
			assertFalse(shiroSNAA.isValid(sakList).get(0).isValid());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
	}

	@Test
	public void testIsValidWhenUsernameWasChangedAndIsUnknown() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		List<SecretAuthenticationKey> sakList = null;
		try {
			sakList = shiroSNAA.authenticate(authenticationData);
		} catch (AuthenticationFault e) {
			log.error(e.getMessage(), e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
		sakList.get(0).setUsername("Trudy");
		try {
			assertFalse(shiroSNAA.isValid(sakList).get(0).isValid());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
	}

	@Test
	public void testIsValidWhenNodeUrnPrefixWasChanged() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		List<SecretAuthenticationKey> sakList = null;
		try {
			sakList = shiroSNAA.authenticate(authenticationData);
		} catch (AuthenticationFault e) {
			log.error(e.getMessage(), e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
		sakList.get(0).setUrnPrefix(new NodeUrnPrefix("urn:wisebed:uzl88:"));
		try {
			assertFalse(shiroSNAA.isValid(sakList).get(0).isValid());
			return;
		} catch (SNAAFault_Exception e) {
			// expected exception if one is thrown:
			assertEquals("Not serving node URN prefix urn:wisebed:uzl88:", e.getMessage());
			return;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
		fail();
	}

	@Test
	public void testIsValidWhenSakUsernameIsNull() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		List<SecretAuthenticationKey> sakList = null;
		try {
			sakList = shiroSNAA.authenticate(authenticationData);
		} catch (AuthenticationFault e) {
			log.error(e.getMessage(), e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
		sakList.get(0).setUsername(null);
		try {
			assertFalse(shiroSNAA.isValid(sakList).get(0).isValid());
			return;
		} catch (SNAAFault_Exception e) {
			// expected exception if one is thrown:
			assertEquals("The user name comprised in the secret authentication key must not be 'null'.", e.getMessage()
			);
			return;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
		fail();
	}


	@Test
	public void testIsValidWhenSakIsNotFound() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		List<SecretAuthenticationKey> sakList = null;
		try {
			sakList = shiroSNAA.authenticate(authenticationData);
		} catch (AuthenticationFault e) {
			log.error(e.getMessage(), e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
		sakList.get(0).setKey("123456");
		try {
			assertFalse(shiroSNAA.isValid(sakList).get(0).isValid());
			return;
		} catch (SNAAFault_Exception e) {
			// expected exception if one is thrown:
			assertEquals("The provides secret authentication key is not found. It is either invalid or expired.",
					e.getMessage()
			);
			return;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
		fail();
	}

	@Test
	public void testIsValidWhenSakUrnPrefixIsNull() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		List<SecretAuthenticationKey> sakList = null;
		try {
			sakList = shiroSNAA.authenticate(authenticationData);
		} catch (AuthenticationFault e) {
			log.error(e.getMessage(), e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
		sakList.get(0).setUrnPrefix(null);
		try {
			assertFalse(shiroSNAA.isValid(sakList).get(0).isValid());
			return;
		} catch (SNAAFault_Exception e) {
			// expected exception if one is thrown:
			assertEquals("Not serving node URN prefix null", e.getMessage());
			return;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
		fail();
	}

	@Test
	public void testIsValidWhenSakUrnPrefixDoesNotMatch() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		List<SecretAuthenticationKey> sakList = null;
		try {
			sakList = shiroSNAA.authenticate(authenticationData);
		} catch (AuthenticationFault e) {
			log.error(e.getMessage(), e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
		sakList.get(0).setUrnPrefix(new NodeUrnPrefix("urn:wisebed:uzl3:"));
		try {
			assertFalse(shiroSNAA.isValid(sakList).get(0).isValid());
			return;
		} catch (SNAAFault_Exception e) {
			// expected exception if one is thrown:
			assertEquals(
					"Not serving node URN prefix urn:wisebed:uzl3:",
					e.getMessage()
			);
			return;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
		fail();
	}

	@Test
	public void testGetNodeGroupsForNodeURNsForSingleURN() {
		Set<String> nodeGroupsForNodeURNs = null;
		try {
			nodeGroupsForNodeURNs =
					shiroSNAA.getNodeGroupsForNodeURNs(Lists.newArrayList(new NodeUrn("urn:wisebed:uzl2:0x2211")));
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
		assertTrue(nodeGroupsForNodeURNs.size() == 1);
		assertEquals("EXPERIMENT_ONLY_NODES", nodeGroupsForNodeURNs.iterator().next());
	}

	@Test
	public void testGetNodeGroupsForNodeURNsForMultipleURNs() {
		Set<String> nodeGroupsForNodeURNs = null;
		try {
			nodeGroupsForNodeURNs = shiroSNAA.getNodeGroupsForNodeURNs(
					Lists.newArrayList(new NodeUrn("urn:wisebed:uzl2:0x2211"), new NodeUrn("urn:wisebed:uzl2:0x2311"))
			);
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
		assertTrue(nodeGroupsForNodeURNs.size() == 2);
		SortedSet<String> actual = new TreeSet<String>(nodeGroupsForNodeURNs);
		SortedSet<String> expected =
				new TreeSet<String>(Lists.newArrayList("EXPERIMENT_ONLY_NODES", "SERVICE_ONLY_NODES"));
		assertEquals(expected, actual);
	}

	@Test
	public void testIsAuthorizedForAdministrator1OnExperimentNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(ADMINISTRATOR1,
				NODE_URN_PREFIX_1, "urn:wisebed:uzl2:0x2211"
		);
		try {
			assertTrue(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.SM_ARE_NODES_ALIVE).isAuthorized());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
	}

	@Test
	public void testIsAuthorizedForAdministrator1OnExperimentNodeFromWrongNetwork() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(ADMINISTRATOR1,
				NODE_URN_PREFIX_1, "urn:wisebed:ulanc:0x1211"
		);
		try {
			shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.SM_ARE_NODES_ALIVE).isAuthorized();
			fail();
		} catch (SNAAFault_Exception e) {
			// expected exception 
			assertEquals("Not serving node URN prefix 'urn:wisebed:ulanc:'", e.getMessage());
		}
	}

	@Test
	public void testIsAuthorizedForExperimenter1OnExperimentNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(EXPERIMENTER1,
				NODE_URN_PREFIX_1, "urn:wisebed:uzl2:0x2211"
		);
		try {
			assertTrue(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.WSN_FLASH_PROGRAMS).isAuthorized());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
	}

	@Test
	public void testIsAuthorizedForServiceProvider1OnExperimentNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(SERVICE_PROVIDER1,
				NODE_URN_PREFIX_1, "urn:wisebed:uzl2:0x2211"
		);
		try {
			assertFalse(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.WSN_FLASH_PROGRAMS).isAuthorized());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
	}

	@Test
	public void testIsFlashingAuthorizedForServiceProvider1OnServiceNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(SERVICE_PROVIDER1,
				NODE_URN_PREFIX_1, "urn:wisebed:uzl2:0x2311"
		);
		try {
			assertFalse(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.WSN_FLASH_PROGRAMS).isAuthorized());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
	}

	@Test
	public void testIsAuthorizedForServiceProvider1OnServiceNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(SERVICE_PROVIDER1,
				NODE_URN_PREFIX_1, "urn:wisebed:uzl2:0x2311"
		);
		try {
			assertTrue(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.WSN_ARE_NODES_ALIVE).isAuthorized());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
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


	protected List<AuthenticationTriple> getAuthenticationTripleListForExperimenter1() {
		AuthenticationTriple authTriple = new AuthenticationTriple();
		authTriple.setUsername(EXPERIMENTER1);
		authTriple.setPassword(EXPERIMENTER1_PASS);
		authTriple.setUrnPrefix(NODE_URN_PREFIX_1);
		List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
		authenticationData.add(authTriple);
		return authenticationData;
	}

	protected List<AuthenticationTriple> getAuthenticationTripleListForServiceProvider1() {
		AuthenticationTriple authTriple = new AuthenticationTriple();
		authTriple.setUsername(SERVICE_PROVIDER1);
		authTriple.setPassword(SERVICE_PROVIDER1_PASS);
		authTriple.setUrnPrefix(NODE_URN_PREFIX_1);
		List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
		authenticationData.add(authTriple);
		return authenticationData;
	}

	protected List<AuthenticationTriple> getAuthenticationTripleListWithEmptyUserName() {
		AuthenticationTriple authTriple = new AuthenticationTriple();
		authTriple.setUsername(null);
		authTriple.setPassword(EXPERIMENTER1_PASS);
		authTriple.setUrnPrefix(NODE_URN_PREFIX_1);
		List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
		authenticationData.add(authTriple);
		return authenticationData;
	}


	/**
	 * Mocks returning a user which has the role "EXPERIMENTER" from the data base.
	 *
	 * @return a user which has the role "Experimenter" from the data base.
	 */
	protected User getExperimenter1() {
		Role role = new Role("EXPERIMENTER");
		role.setPermissions(newHashSet(getPermissionsObject(Action.WSN_FLASH_PROGRAMS, "EXPERIMENT_ONLY_NODES")));
		return new User(EXPERIMENTER1, EXPERIMENTER1_PASS_HASHED, EXPERIMENTER1_SALT, newHashSet(role));
	}

	/**
	 * Mocks returning a user which has the role "SERVICE_PROVIDER" from the data base.
	 *
	 * @return a user which has the role "Experimenter" from the data base.
	 */
	protected User getServiceProvider1() {
		Role role = new Role("SERVICE_PROVIDER");
		role.setPermissions(newHashSet(getPermissionsObject(Action.WSN_ARE_NODES_ALIVE, "SERVICE_ONLY_NODES")));
		return new User(SERVICE_PROVIDER1, SERVICE_PROVIDER1_PASS_HASHED, SERVICE_PROVIDER1_SALT, newHashSet(role));
	}

	/**
	 * Mocks returning a user which has the role "ADMINISTRATOR" from the data base.
	 *
	 * @return a user which has the role "Experimenter" from the data base.
	 */
	protected User getAdministrator1() {
		Role role = new Role("ADMINISTRATOR");
		Set<Permission> permissionsSet = new HashSet<Permission>();
		role.setPermissions(permissionsSet);
		permissionsSet.add(getPermissionsObject(Action.WSN_ARE_NODES_ALIVE, "SERVICE_ONLY_NODES"));
		permissionsSet.add(getPermissionsObject(Action.SM_ARE_NODES_ALIVE, "EXPERIMENT_ONLY_NODES"));
		return new User(ADMINISTRATOR1, ADMINISTRATOR1_PASS_HASHED, ADMINISTRATOR1_SALT, newHashSet(role));
	}


	protected Permission getPermissionsObject(Action action, String resourceGroupsName) {
		Permission permission = new Permission();
		permission.setAction(new de.uniluebeck.itm.tr.snaa.shiro.entity.Action(action.name()));
		permission.setResourceGroup(new ResourceGroup(resourceGroupsName));
		return permission;
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
}
