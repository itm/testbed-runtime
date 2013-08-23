/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uniluebeck.itm.tr.snaa.certificate;

import com.google.inject.AbstractModule;
import de.uniluebeck.itm.tr.snaa.shiro.UrnResourceGroupDao;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.snaa.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static eu.wisebed.api.v3.snaa.Action.SM_ARE_NODES_ALIVE;
import static eu.wisebed.api.v3.snaa.Action.WSN_FLASH_PROGRAMS;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 *
 *
 */

@RunWith(MockitoJUnitRunner.class)
public class SNAACertificateTest extends SNAACertificateTestBase {
	@Mock
	private EntityManager em;

	@Mock
	private UserCertDao userCertDao;

	@Mock
	private UrnResourceGroupDao urnResourceGroupDao;

	@Before
	public void setUp() throws Exception {
		when(userCertDao.find(ADMINISTRATOR1)).thenReturn(getAdministrator1());
		when(userCertDao.find(SERVICE_PROVIDER1)).thenReturn(getServiceProvider1());
		when(userCertDao.find(EXPERIMENTER1)).thenReturn(getExperimenter1());
		when(urnResourceGroupDao.find()).thenReturn(getUrnResourceGroup());

		final AbstractModule jpaModule = new AbstractModule() {
			@Override
			protected void configure() {
				bind(EntityManager.class).toInstance(em);
				bind(UserCertDao.class).toInstance(userCertDao);
				bind(UrnResourceGroupDao.class).toInstance(urnResourceGroupDao);
			}
		};

		super.setUp(jpaModule);
	}

	@Test
	public void testIsAuthorizedForAdministrator1OnExperimentNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(ADMINISTRATOR1,
				NODE_URN_PREFIX_1, "urn:wisebed:uzl2:0x2211"
		);
		try {
			assertTrue(snaaCertificate.isAuthorized(usernameNodeUrnsMaps, SM_ARE_NODES_ALIVE).isAuthorized());
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
			assertFalse(snaaCertificate.isAuthorized(usernameNodeUrnsMaps, Action.WSN_DESTROY_VIRTUAL_LINKS).isAuthorized());
		} catch (SNAAFault_Exception ex) {
			Logger.getLogger(SNAACertificateTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		}
	}

	@Test
	public void testIsAuthorizedForExperimenter1OnExperimentNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(EXPERIMENTER1,
				NODE_URN_PREFIX_1, "urn:wisebed:uzl2:0x2211"
		);
		try {
			assertTrue(snaaCertificate.isAuthorized(usernameNodeUrnsMaps, WSN_FLASH_PROGRAMS).isAuthorized());
		} catch (SNAAFault_Exception ex) {
			Logger.getLogger(SNAACertificateTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		}
	}

	@Test
	public void testAuthenticationForExperimenter1() {
		List<AuthenticationSAML> authenticationSAMLs = getAuthenticationInformation(EXPERIMENTER1, ORGANISATIONID, ROLE_EXPERIMENTER, NODE_URN_PREFIX_1);

		Authenticate parameters = new Authenticate();
		List<AuthenticationSAML> authenticationSAMLs1 = parameters.getCertAuthenticationData();
		authenticationSAMLs1.add(authenticationSAMLs.get(0));
		try {
			AuthenticateResponse authenticateResponse = snaaCertificate.authenticate(parameters);
			SecretAuthenticationKey sak = authenticateResponse.getSecretAuthenticationKey().get(0);
			Assert.assertEquals(EXPERIMENTER1, sak.getUsername());
			Assert.assertEquals(NODE_URN_PREFIX_1, sak.getUrnPrefix());

		} catch (AuthenticationFault ex) {
			Logger.getLogger(SNAACertificateTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		} catch (SNAAFault_Exception ex) {
			Logger.getLogger(SNAACertificateTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		}
	}
}
