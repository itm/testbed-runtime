package de.uniluebeck.itm.tr.snaa.shiro;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.snaa.shiro.entity.*;
import de.uniluebeck.itm.tr.util.Logging;
import eu.wisebed.api.v3.common.*;
import eu.wisebed.api.v3.snaa.Action;
import eu.wisebed.api.v3.snaa.AuthenticationFault_Exception;
import eu.wisebed.api.v3.snaa.AuthenticationTriple;
import eu.wisebed.api.v3.snaa.SNAAFault_Exception;
import org.apache.log4j.Level;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.crypto.hash.Sha512Hash;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ShiroSNAATest {

	static {
		Logging.setLoggingDefaults(Level.WARN);
	}
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ShiroSNAATest.class);

	private static final String EXPERIMENTER1_PASS = "Exp1Pass";
	private static final String EXPERIMENTER1_SALT = "Exp1Salt";
	private static final String EXPERIMENTER1_PASS_HASHED = new Sha512Hash(EXPERIMENTER1_PASS,EXPERIMENTER1_SALT,1000).toHex();
	private static final String EXPERIMENTER1 = "Experimenter1";

	private static final String EXPERIMENTER2_PASS = "Exp2Pass";

	private static final String SERVICE_PROVIDER1_PASS = "SP1Pass";
	private static final String SERVICE_PROVIDER1_SALT = "SP1PSalt";
	private static final String SERVICE_PROVIDER1_PASS_HASHED = new Sha512Hash(SERVICE_PROVIDER1_PASS,SERVICE_PROVIDER1_SALT,1000).toHex();
	private static final String SERVICE_PROVIDER1 = "ServiceProvider1";

	private static final String ADMINISTRATOR1_PASS = "Adm1Pass";
	private static final String ADMINISTRATOR1_SALT = "Adm1Salt";
	private static final String ADMINISTRATOR1_PASS_HASHED = new Sha512Hash(ADMINISTRATOR1_PASS,ADMINISTRATOR1_SALT,1000).toHex();
	private static final String ADMINISTRATOR1 = "Administrator1";

	private List<NodeUrnPrefix> nodeUrnPrefixes =  Lists.newArrayList(new NodeUrnPrefix("urn:wisebed:uzl2:"),new NodeUrnPrefix("urn:wisebed:uzl3:"));

	private ShiroSNAA shiroSNAA;

    @Mock
    private EntityManager em;

    @Mock
    private UserDao usersDao;

    @Mock
    private UrnResourceGroupDao UrnResourceGroupDao;

	@Before
	public void setUp() {
		when(usersDao.find(EXPERIMENTER1)).thenReturn(getExperimenter1());
		when(usersDao.find(SERVICE_PROVIDER1)).thenReturn(getServiceProvider1());
		when(usersDao.find(ADMINISTRATOR1)).thenReturn(getAdministrator1());
		when(UrnResourceGroupDao.find()).thenReturn(getUrnResourceGroup());

        Injector mockedEntityManagerInjector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
               bind(EntityManager.class).toInstance(em);
               bind(UserDao.class).toInstance(usersDao);
               bind(UrnResourceGroupDao.class).toInstance(UrnResourceGroupDao);
            }
        });

		MyShiroModule myShiroModule = new MyShiroModule();
		Injector shiroInjector = mockedEntityManagerInjector.createChildInjector(myShiroModule);
		SecurityUtils.setSecurityManager(shiroInjector.getInstance(org.apache.shiro.mgt.SecurityManager.class));

		ShiroSNAAFactory factory = shiroInjector.getInstance(ShiroSNAAFactory.class);
		shiroSNAA = factory.create(Sets.newHashSet(nodeUrnPrefixes));
	}

	@Test
	public void testAuthenticationForExperimenter1() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		try {
			List<SecretAuthenticationKey> sakList = shiroSNAA.authenticate(authenticationData);
			assertNotNull(sakList);
			assertEquals(EXPERIMENTER1, sakList.get(0).getUsername());
			assertEquals(nodeUrnPrefixes.get(0), sakList.get(0).getUrnPrefix());
			assertNotNull(sakList.get(0).getKey());
		} catch (AuthenticationFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}

	}

	@Test
	public void testAuthenticationFailForExperimenter1DueToWrongPasswd() {

		AuthenticationTriple authTriple = new AuthenticationTriple();
		authTriple.setUsername(EXPERIMENTER1);
		authTriple.setPassword(EXPERIMENTER2_PASS);
		authTriple.setUrnPrefix(nodeUrnPrefixes.get(0));
		List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
		authenticationData.add(authTriple);
		try {
			shiroSNAA.authenticate(authenticationData);
			fail();
		} catch (AuthenticationFault_Exception e) {
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
			assertEquals(nodeUrnPrefixes.get(0), sakList.get(0).getUrnPrefix());
			assertNotNull(sakList.get(0).getKey());
		} catch (AuthenticationFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}

	}

	@Test
	public void testIsValidWhenUsernameIsNull() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListWithEmptyUserName();
		try {
			shiroSNAA.authenticate(authenticationData);
		} catch (AuthenticationFault_Exception e) {
			assertEquals("The user could not be authenticated: Wrong username and/or password.", e.getMessage());
			return;
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
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
		} catch (AuthenticationFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}

		try {
			assertTrue(shiroSNAA.isValid(sakList.get(0)).isValid());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
	}

	@Test
	public void testIsValidWhenUsernameWasChanged() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		List<SecretAuthenticationKey> sakList = null;
		try {
			sakList = shiroSNAA.authenticate(authenticationData);
		} catch (AuthenticationFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
		sakList.get(0).setUsername(ADMINISTRATOR1);
		try {
			assertFalse(shiroSNAA.isValid(sakList.get(0)).isValid());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
	}

	@Test
	public void testIsValidWhenUsernameWasChangedAndIsUnknown() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		List<SecretAuthenticationKey> sakList = null;
		try {
			sakList = shiroSNAA.authenticate(authenticationData);
		} catch (AuthenticationFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
		sakList.get(0).setUsername("Trudy");
		try {
			assertFalse(shiroSNAA.isValid(sakList.get(0)).isValid());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
	}

	@Test
	public void testIsValidWhenNodeUrnPrefixWasChanged() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		List<SecretAuthenticationKey> sakList = null;
		try {
			sakList = shiroSNAA.authenticate(authenticationData);
		} catch (AuthenticationFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
		sakList.get(0).setUrnPrefix(new NodeUrnPrefix("urn:wisebed:uzl88:"));
		try {
			assertFalse(shiroSNAA.isValid(sakList.get(0)).isValid());
			return;
		} catch (SNAAFault_Exception e) {
			// expected exception if one is thrown:
			assertEquals("Not serving node URN prefix urn:wisebed:uzl88:", e.getMessage());
			return;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
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
		} catch (AuthenticationFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
		sakList.get(0).setUsername(null);
		try {
			assertFalse(shiroSNAA.isValid(sakList.get(0)).isValid());
			return;
		} catch (SNAAFault_Exception e) {
			// expected exception if one is thrown:
			assertEquals("The user name comprised in the secret authentication key must not be 'null'.", e.getMessage());
			return;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
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
		} catch (AuthenticationFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
		sakList.get(0).setKey("123456");
		try {
			assertFalse(shiroSNAA.isValid(sakList.get(0)).isValid());
			return;
		} catch (SNAAFault_Exception e) {
			// expected exception if one is thrown:
			assertEquals("The provides secret authentication key is not found. It is either invalid or expired.", e.getMessage());
			return;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
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
		} catch (AuthenticationFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
		sakList.get(0).setUrnPrefix(null);
		try {
			assertFalse(shiroSNAA.isValid(sakList.get(0)).isValid());
			return;
		} catch (SNAAFault_Exception e) {
			// expected exception if one is thrown:
			assertEquals("Not serving node URN prefix null", e.getMessage());
			return;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
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
		} catch (AuthenticationFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
		sakList.get(0).setUrnPrefix(new NodeUrnPrefix("urn:wisebed:uzl3:"));
		try {
			assertFalse(shiroSNAA.isValid(sakList.get(0)).isValid());
			return;
		} catch (SNAAFault_Exception e) {
			// expected exception if one is thrown:
			assertEquals("The urn prefix which was provided by the original authentication does not match the one in the secret authentication key.", e.getMessage());
			return;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
		fail();
	}

	@Test
	public void testGetNodeGroupsForNodeURNsForSingleURN() {
		Set<String> nodeGroupsForNodeURNs = null;
		try {
			nodeGroupsForNodeURNs = shiroSNAA.getNodeGroupsForNodeURNs(Lists.newArrayList(new NodeUrn("urn:wisebed:uzl2:0x2211")));
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
		assertTrue(nodeGroupsForNodeURNs.size() == 1);
		assertEquals("EXPERIMENT_ONLY_NODES", nodeGroupsForNodeURNs.iterator().next());
	}
	
	@Test
	public void testGetNodeGroupsForNodeURNsForMultipleURNs() {
		Set<String> nodeGroupsForNodeURNs = null;
		try {
			nodeGroupsForNodeURNs = shiroSNAA.getNodeGroupsForNodeURNs(Lists.newArrayList(new NodeUrn("urn:wisebed:uzl2:0x2211"), new NodeUrn("urn:wisebed:uzl2:0x2311")));
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
		assertTrue(nodeGroupsForNodeURNs.size() == 2);
		SortedSet<String> actual = new TreeSet<String>(nodeGroupsForNodeURNs);
		SortedSet<String> expected = new TreeSet<String>(Lists.newArrayList("EXPERIMENT_ONLY_NODES","SERVICE_ONLY_NODES"));
		assertEquals(expected,actual);
	}

	@Test
	public void testIsAuthorizedForAdministrator1OnExperimentNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(ADMINISTRATOR1, nodeUrnPrefixes.get(0), "urn:wisebed:uzl2:0x2211");
		try {
			assertTrue(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.SM_ARE_NODES_ALIVE).isAuthorized());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
	}
	
	@Test
	public void testIsAuthorizedForAdministrator1OnExperimentNodeFromWrongNetwork() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(ADMINISTRATOR1, nodeUrnPrefixes.get(0), "urn:wisebed:ulanc:0x1211");
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
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(EXPERIMENTER1, nodeUrnPrefixes.get(0), "urn:wisebed:uzl2:0x2211");
		try {
			assertTrue(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.WSN_FLASH_PROGRAMS).isAuthorized());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
	}

	@Test
	public void testIsAuthorizedForServiceProvider1OnExperimentNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(SERVICE_PROVIDER1, nodeUrnPrefixes.get(0), "urn:wisebed:uzl2:0x2211");
		try {
			assertFalse(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.WSN_FLASH_PROGRAMS).isAuthorized());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
	}
	
	@Test
	public void testIsFlashingAuthorizedForServiceProvider1OnServiceNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(SERVICE_PROVIDER1, nodeUrnPrefixes.get(0), "urn:wisebed:uzl2:0x2311");
		try {
			assertFalse(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.WSN_FLASH_PROGRAMS).isAuthorized());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
	}
	
	@Test
	public void testIsAuthorizedForServiceProvider1OnServiceNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(SERVICE_PROVIDER1, nodeUrnPrefixes.get(0), "urn:wisebed:uzl2:0x2311");
		try {
			assertTrue(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.WSN_ARE_NODES_ALIVE).isAuthorized());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
	}
	
	
	
	
	/* ------------------------------ Helpers ----------------------------------- */

	private List<UsernameNodeUrnsMap> createUsernameNodeUrnsMapList(String username, NodeUrnPrefix nodeUrnPrefix, String... nodeURNStrings){
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = new LinkedList<UsernameNodeUrnsMap>();
		UsernameNodeUrnsMap map = new UsernameNodeUrnsMap();
		UsernameUrnPrefixPair usernameUrnPrefixPair = new UsernameUrnPrefixPair();
		usernameUrnPrefixPair.setUrnPrefix(nodeUrnPrefix);
		usernameUrnPrefixPair.setUsername(username);
		map.setUsername(usernameUrnPrefixPair);
		List<NodeUrn> nodeUrns = map.getNodeUrns();
		
		for (String nodeUrnString : nodeURNStrings) {
			nodeUrns.add(new NodeUrn(nodeUrnString));
		}

		usernameNodeUrnsMaps.add(map);
		return usernameNodeUrnsMaps;
	}
	
	
	private List<AuthenticationTriple> getAuthenticationTripleListForExperimenter1() {
		AuthenticationTriple authTriple = new AuthenticationTriple();
		authTriple.setUsername(EXPERIMENTER1);
		authTriple.setPassword(EXPERIMENTER1_PASS);
		authTriple.setUrnPrefix(nodeUrnPrefixes.get(0));
		List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
		authenticationData.add(authTriple);
		return authenticationData;
	}

	private List<AuthenticationTriple> getAuthenticationTripleListForServiceProvider1() {
		AuthenticationTriple authTriple = new AuthenticationTriple();
		authTriple.setUsername(SERVICE_PROVIDER1);
		authTriple.setPassword(SERVICE_PROVIDER1_PASS);
		authTriple.setUrnPrefix(nodeUrnPrefixes.get(0));
		List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
		authenticationData.add(authTriple);
		return authenticationData;
	}

	private List<AuthenticationTriple> getAuthenticationTripleListWithEmptyUserName() {
		AuthenticationTriple authTriple = new AuthenticationTriple();
		authTriple.setUsername(null);
		authTriple.setPassword(EXPERIMENTER1_PASS);
		authTriple.setUrnPrefix(nodeUrnPrefixes.get(0));
		List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
		authenticationData.add(authTriple);
		return authenticationData;
	}


	/**
	 * Mocks returning a user which has the role "EXPERIMENTER" from the data base.
	 * @return a user which has the role "Experimenter" from the data base.
	 */
	private User getExperimenter1(){
		Role role = new Role("EXPERIMENTER");
		role.setPermissions(Sets.newHashSet(getPermissionsObject(Action.WSN_FLASH_PROGRAMS, "EXPERIMENT_ONLY_NODES")));
		return new User(EXPERIMENTER1, EXPERIMENTER1_PASS_HASHED, EXPERIMENTER1_SALT, Sets.newHashSet(role));
	}
	
	/**
	 * Mocks returning a user which has the role "SERVICE_PROVIDER" from the data base.
	 * @return a user which has the role "Experimenter" from the data base.
	 */
	private User getServiceProvider1(){
		Role role = new Role("SERVICE_PROVIDER");
		role.setPermissions(Sets.newHashSet(getPermissionsObject(Action.WSN_ARE_NODES_ALIVE, "SERVICE_ONLY_NODES")));
		return new User(SERVICE_PROVIDER1, SERVICE_PROVIDER1_PASS_HASHED, SERVICE_PROVIDER1_SALT, Sets.newHashSet(role));
	}
	
	/**
	 * Mocks returning a user which has the role "ADMINISTRATOR" from the data base.
	 * @return a user which has the role "Experimenter" from the data base.
	 */
	private User getAdministrator1(){
		Role role = new Role("ADMINISTRATOR");
		Set<Permission> permissionsSet = new HashSet<Permission>();
		role.setPermissions(permissionsSet);
		permissionsSet.add(getPermissionsObject(Action.WSN_ARE_NODES_ALIVE, "SERVICE_ONLY_NODES"));
		permissionsSet.add(getPermissionsObject(Action.SM_ARE_NODES_ALIVE, "EXPERIMENT_ONLY_NODES"));
		return new User(ADMINISTRATOR1, ADMINISTRATOR1_PASS_HASHED, ADMINISTRATOR1_SALT, Sets.newHashSet(role));
	}


	private Permission getPermissionsObject(Action action, String resourceGroupsName) {
		Permission permission = new Permission();
		permission.setAction(new de.uniluebeck.itm.tr.snaa.shiro.entity.Action(action.name()));
		permission.setResourceGroup(new ResourceGroup(resourceGroupsName));
		return permission;
	}
	
	private List<UrnResourceGroup> getUrnResourceGroup(){
		List<UrnResourceGroup> UrnResourceGroupList = new LinkedList<UrnResourceGroup>();
		UrnResourceGroupList.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:ulanc1:0x1211", "EXPERIMENT_ONLY_NODES"), null));

		UrnResourceGroupList.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:ulanc1:0x1211", "EXPERIMENT_ONLY_NODES"), null));
		UrnResourceGroupList.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:ulanc1:0x1212", "EXPERIMENT_ONLY_NODES"), null));
		UrnResourceGroupList.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:ulanc1:0x1213", "EXPERIMENT_ONLY_NODES"), null));
		UrnResourceGroupList.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:ulanc1:0x1311", "SERVICE_ONLY_NODES"), null));
		UrnResourceGroupList.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:ulanc1:0x1312", "SERVICE_ONLY_NODES"), null));
		UrnResourceGroupList.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:ulanc1:0x1313", "SERVICE_ONLY_NODES"), null));
		
		UrnResourceGroupList.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:uzl2:0x2211", "EXPERIMENT_ONLY_NODES"), null));
		UrnResourceGroupList.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:uzl2:0x2212", "EXPERIMENT_ONLY_NODES"), null));
		UrnResourceGroupList.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:uzl2:0x2213", "EXPERIMENT_ONLY_NODES"), null));
		UrnResourceGroupList.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:uzl2:0x2311", "SERVICE_ONLY_NODES"), null));
		UrnResourceGroupList.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:uzl2:0x2312", "SERVICE_ONLY_NODES"), null));
		UrnResourceGroupList.add(new UrnResourceGroup(new UrnResourceGroupId("urn:wisebed:uzl2:0x2313", "SERVICE_ONLY_NODES"), null));
		
		return UrnResourceGroupList;
	}
}
