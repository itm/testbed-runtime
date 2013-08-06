/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.smartsantander.cea.certificate;

import de.uniluebeck.itm.tr.snaa.shiro.JpaModule;
import static eu.smartsantander.cea.certificate.SNAACertificateTestBase.ADMINISTRATOR1;
import static eu.smartsantander.cea.certificate.SNAACertificateTestBase.NODE_URN_PREFIX_1;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.snaa.Action;
import eu.wisebed.api.v3.snaa.Authenticate;
import eu.wisebed.api.v3.snaa.AuthenticateResponse;
import eu.wisebed.api.v3.snaa.AuthenticationFault;
import eu.wisebed.api.v3.snaa.AuthenticationSAML;
import eu.wisebed.api.v3.snaa.SNAAFault_Exception;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import static org.junit.Assert.*;



/**
 *
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class SNAACertificateMySQLIntegrationTesting extends SNAACertificateTestBase {
    
  
    @Before
    public void setUp() throws Exception {
        Properties jpaProperties = new Properties();

	//jpaProperties.put("hibernate.bytecode.use_reflection_optimizer", "false");
	jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
	jpaProperties.put("hibernate.connection.driver_class", "org.gjt.mm.mysql.Driver");
	jpaProperties.put("hibernate.connection.url", "jdbc:mysql://localhost:3306/trauth");
	jpaProperties.put("hibernate.connection.username", "trauth");
	jpaProperties.put("hibernate.connection.password", "trauth");
       
        JpaModule jpaModule = new JpaModule("ShiroSNAA", jpaProperties);
        super.setUp(jpaModule);
     }
     
    
  
    @Test
    public void testIsAuthorizedForAdministratorInExperimentionNodeOnly() {
        List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(ADMINISTRATOR1,
				NODE_URN_PREFIX_1, "urn:wisebed:uzl2:0x2211"
		);
		try {
			assertTrue(snaaCertificate.isAuthorized(usernameNodeUrnsMaps, Action.RS_GET_CONFIDENTIAL_RESERVATIONS).isAuthorized());
		} catch (SNAAFault_Exception e) {
			log.error(e.getMessage(), e);
			fail();
		}
    }
    
    @Test
    public void testIsAuthorizedForExperimenter1OnExperimentNode() {
        List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(EXPERIMENTER1,
				NODE_URN_PREFIX_1, "urn:wisebed:uzl2:0x2211"
		);
        try {
            assertTrue(snaaCertificate.isAuthorized(usernameNodeUrnsMaps, Action.WSN_ARE_NODES_ALIVE).isAuthorized());
        } catch (SNAAFault_Exception ex) {
            Logger.getLogger(SNAACertificateMySQLIntegrationTesting.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Test
    public void testIsNOTAuthorizedForServiceProviderInExperimentationNode() {
        List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(SERVICE_PROVIDER1,
				NODE_URN_PREFIX_2, "urn:wisebed:uzl1:0x1234");
        try {
            assertFalse(snaaCertificate.isAuthorized(usernameNodeUrnsMaps, Action.RS_DELETE_RESERVATION).isAuthorized());
        } catch (SNAAFault_Exception ex) {
            Logger.getLogger(SNAACertificateMySQLIntegrationTesting.class.getName()).log(Level.SEVERE, null, ex);
        }
	
    }
    
      @Test
      public void testAuthenticationForAdministrator() {
            List<AuthenticationSAML> authenticationSAMLs = getAuthenticationInformation(ADMINISTRATOR1, ORGANISATIONID, ROLE_ADMIN, NODE_URN_PREFIX_1);
            
            Authenticate parameters = new Authenticate();
            List<AuthenticationSAML> authenticationSAMLs1 = parameters.getCertAuthenticationData();
            authenticationSAMLs1.add(authenticationSAMLs.get(0));
            try {
                AuthenticateResponse authenticateResponse = snaaCertificate.authenticate(parameters);
                SecretAuthenticationKey sak = authenticateResponse.getSecretAuthenticationKey().get(0);
                Assert.assertEquals(ADMINISTRATOR1, sak.getUsername());
                Assert.assertEquals(NODE_URN_PREFIX_1, sak.getUrnPrefix());
                
            } catch (AuthenticationFault ex) {
                Logger.getLogger(SNAACertificateTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SNAAFault_Exception ex) {
                Logger.getLogger(SNAACertificateTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
}
