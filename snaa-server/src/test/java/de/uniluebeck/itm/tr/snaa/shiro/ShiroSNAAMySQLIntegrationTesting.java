package de.uniluebeck.itm.tr.snaa.shiro;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.jpa.JpaPersistModule;
import de.uniluebeck.itm.tr.snaa.SNAAServer;
import de.uniluebeck.itm.tr.snaa.shiro.entity.*;
import de.uniluebeck.itm.tr.util.Logging;
import eu.wisebed.api.v3.common.*;
import eu.wisebed.api.v3.snaa.Action;
import eu.wisebed.api.v3.snaa.AuthenticationFault_Exception;
import eu.wisebed.api.v3.snaa.AuthenticationTriple;
import eu.wisebed.api.v3.snaa.SNAAFault_Exception;
import org.apache.log4j.Level;
import org.apache.shiro.SecurityUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.Table;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.Assert.*;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public class ShiroSNAAMySQLIntegrationTesting {

	static {
		Logging.setLoggingDefaults(Level.WARN);
	}
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ShiroSNAATest.class);

	private static final String EXPERIMENTER1_PASS = "Exp1Pass";
	private static final String EXPERIMENTER1 = "Experimenter1";

	private static final String EXPERIMENTER2_PASS = "Exp2Pass";

	private static final String SERVICE_PROVIDER1_PASS = "SP1Pass";
	private static final String SERVICE_PROVIDER1 = "ServiceProvider1";

	private static final String ADMINISTRATOR1 = "Administrator1";

	private List<NodeUrnPrefix> nodeUrnPrefix = Lists.newArrayList(new NodeUrnPrefix("urn:wisebed:uzl2:"));

	private ShiroSNAA shiroSNAA;

    @Mock
    private EntityManager em;

    @Mock
    private UrnResourceGroupDao UrnResourceGroupDao;


	@BeforeClass
	public static void setDBForEntities(){

		Class[] entities = {de.uniluebeck.itm.tr.snaa.shiro.entity.Action.class,
				Permission.class,
				ResourceGroup.class,
				Role.class,
				UrnResourceGroup.class,
				User.class};

		for (Class entity : entities) {
			final Annotation[] declaredAnnotations = entity.getDeclaredAnnotations();
			for (Annotation declaredAnnotation : declaredAnnotations) {
				if (declaredAnnotation instanceof Table){
					final Table tableAnnotation = (Table) declaredAnnotation;
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("catalog","trauthsampledb");
					map.put("name", tableAnnotation.name());
					map.put("uniqueConstraints", tableAnnotation.uniqueConstraints());
					map.put("schema",tableAnnotation.schema());
					replaceAnnotations(tableAnnotation, map);
					break;
				}
			}

		}

	}

	private static void replaceAnnotations(Object someAnnotation, Map<String, Object> newValues) {
		if (Proxy.isProxyClass(someAnnotation.getClass())) {
			Object invocationHandler = Proxy.getInvocationHandler(someAnnotation);
			try {
				Field field = invocationHandler.getClass().getDeclaredField("memberValues");
				field.setAccessible(true);
				field.set(invocationHandler, newValues);
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		}
	}

	@Before
	public void setUp() {

		Properties properties = new Properties();
		try {
			properties.load(SNAAServer.class.getClassLoader().getResourceAsStream("META-INF/hibernate.properties"));
		} catch (IOException e) {
			log.error(e.getMessage(), e);

		}
		Injector jpaInjector = Guice.createInjector(new JpaPersistModule("ShiroSNAATest").properties(properties));
		jpaInjector.getInstance(PersistService.class).start();

		MyShiroModule myShiroModule = new MyShiroModule();
		Injector shiroInjector = jpaInjector.createChildInjector(myShiroModule);

		SecurityUtils.setSecurityManager(shiroInjector.getInstance(org.apache.shiro.mgt.SecurityManager.class));
		ShiroSNAAFactory factory = shiroInjector.getInstance(ShiroSNAAFactory.class);
		shiroSNAA = factory.create(Sets.newHashSet(nodeUrnPrefix));
	}

	@Test
	public void testAuthenticationForExperimenter1() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		try {
			List<SecretAuthenticationKey> sakList = shiroSNAA.authenticate(authenticationData);
			assertNotNull(sakList);
			assertEquals(EXPERIMENTER1, sakList.get(0).getUsername());
			assertEquals(nodeUrnPrefix.get(0), sakList.get(0).getUrnPrefix());
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
	public void testAuthenticationFailForExperimenter1DueToWrongPassword() {

		AuthenticationTriple authTriple = new AuthenticationTriple();
		authTriple.setUsername(EXPERIMENTER1);
		authTriple.setPassword(EXPERIMENTER2_PASS);
		authTriple.setUrnPrefix(nodeUrnPrefix.get(0));
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
			assertEquals(nodeUrnPrefix.get(0), sakList.get(0).getUrnPrefix());
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
		} catch (AuthenticationFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
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
		} catch (AuthenticationFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
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
		} catch (AuthenticationFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
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
			nodeGroupsForNodeURNs = shiroSNAA.getNodeGroupsForNodeURNs(Lists.newArrayList(new NodeUrn("urn:wisebed:uzl2:0x2211"), new NodeUrn("urn:wisebed:uzl2:0x2311")));
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
		assertTrue(nodeGroupsForNodeURNs.size() == 2);
		SortedSet<String> actual = new TreeSet<String>(nodeGroupsForNodeURNs);
		SortedSet<String> expected = new TreeSet<String>(Lists.newArrayList("EXPERIMENT_ONLY_NODES","SERVICE_ONLY_NODES"));
		assertEquals(expected,actual);
	}

	@Test
	public void testIsAuthorizedForAdministrator1OnExperimentNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(ADMINISTRATOR1, nodeUrnPrefix.get(0), "urn:wisebed:uzl2:0x2211");
		try {
			assertTrue(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.SM_ARE_NODES_ALIVE).isAuthorized());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
	}

	@Test
	public void testIsAuthorizedForAdministrator1OnExperimentNodeFromWrongNetwork() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(ADMINISTRATOR1, nodeUrnPrefix.get(0), "urn:wisebed:ulanc:0x1211");
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
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(EXPERIMENTER1, nodeUrnPrefix.get(0), "urn:wisebed:uzl2:0x2211");
		try {
			assertTrue(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.WSN_FLASH_PROGRAMS).isAuthorized());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
	}

	@Test
	public void testIsAuthorizedForServiceProvider1OnExperimentNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(SERVICE_PROVIDER1, nodeUrnPrefix.get(0), "urn:wisebed:uzl2:0x2211");
		try {
			assertFalse(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.WSN_FLASH_PROGRAMS).isAuthorized());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
	}

	@Test
	public void testIsFlashingAuthorizedForServiceProvider1OnServiceNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(SERVICE_PROVIDER1, nodeUrnPrefix.get(0), "urn:wisebed:uzl2:0x2311");
		try {
			assertFalse(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.WSN_FLASH_PROGRAMS).isAuthorized());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
	}

	@Test
	public void testIsAuthorizedForServiceProvider1OnServiceNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(SERVICE_PROVIDER1, nodeUrnPrefix.get(0), "urn:wisebed:uzl2:0x2311");
		try {
			assertTrue(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.WSN_ARE_NODES_ALIVE).isAuthorized());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(),e);
			fail();
		}
	}




	/* ------------------------------ Helpers ----------------------------------- */

	private List<UsernameNodeUrnsMap> createUsernameNodeUrnsMapList(String username, NodeUrnPrefix nodeUrnPrefix, String... nodeUrnStrings){
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


	private List<AuthenticationTriple> getAuthenticationTripleListForExperimenter1() {
		AuthenticationTriple authTriple = new AuthenticationTriple();
		authTriple.setUsername(EXPERIMENTER1);
		authTriple.setPassword(EXPERIMENTER1_PASS);
		authTriple.setUrnPrefix(nodeUrnPrefix.get(0));
		List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
		authenticationData.add(authTriple);
		return authenticationData;
	}

	private List<AuthenticationTriple> getAuthenticationTripleListForServiceProvider1() {
		AuthenticationTriple authTriple = new AuthenticationTriple();
		authTriple.setUsername(SERVICE_PROVIDER1);
		authTriple.setPassword(SERVICE_PROVIDER1_PASS);
		authTriple.setUrnPrefix(nodeUrnPrefix.get(0));
		List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
		authenticationData.add(authTriple);
		return authenticationData;
	}

}
