/*******************************************************************************
* Copyright (c) 2013 CEA LIST.
* Contributor:
*   ROUX Pierre
*   Kim Thuat NGUYEN
*******************************************************************************/
package eu.smartsantander.cea.utils.client;

import eu.smartsantander.cea.utils.Helper.HelperUtilities;
import eu.smartsantander.cea.utils.httprequest.HttpRequestUtil;
import eu.smartsantander.cea.utils.singature.SignData;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.snaa.Action;
import eu.wisebed.api.v3.snaa.Authenticate;
import eu.wisebed.api.v3.snaa.AuthenticateResponse;
import eu.wisebed.api.v3.snaa.AuthenticationFault;
import eu.wisebed.api.v3.snaa.AuthenticationSAML;
import eu.wisebed.api.v3.snaa.AuthorizationResponse;
import eu.wisebed.api.v3.snaa.SNAA;
import eu.wisebed.api.v3.snaa.SNAAFault_Exception;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SNAAClient {
    private static final String ENCODING = "UTF-8";
    private static final String METHOD = "POST";
    
    private static final String defaultSnaaUrl="http://localhost:8890/soap/v3/snaa";
    
    private static List<UsernameNodeUrnsMap> createUsernameNodeUrnsMapList(String username, NodeUrnPrefix nodeUrnPrefix,
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
    
    public static void main(String args[]) {
         
        
        if (args.length>0) {
            try {
                
                String propertiesFile = args[1];
                Properties props = new Properties();
                props.load(new FileInputStream(propertiesFile));
                String userId = props.getProperty("userId");
                String organizationId = props.getProperty("organizationId");
                String pathPrivateKey = props.getProperty("pathPrivateKey");
                String nodeUrnPrefix = props.getProperty("nodeUrnPrefix");
                String nodeUrns = props.getProperty("nodeUrns");
                String action = props.getProperty("action");
                 // Create the map of parameters
                Map params = new HashMap();
                params.put("userId", userId);
                params.put("idpId", organizationId);
                
                String idpUrl = props.getProperty("idpUrl");
                
                // Client sends request to organization portail to do authentication and get the SAML assertion
                String[] res = HttpRequestUtil.sendHttpRequest(idpUrl, METHOD, params, ENCODING);
                
                String resultInString = HelperUtilities.stringArrayToString(res);
                
                // Check if the response contains the challenge   
                // Form: challenge=qqdsvvedvsv
                if (resultInString.contains("challenge")) {
                    String[] splits = resultInString.split("challenge=");
                    String challengeInBytes = splits[splits.length-1];
                    String challenge = HelperUtilities.decode(challengeInBytes);
                    System.out.println("Challenge received from the organization portail: \n "+ challenge);
                    
                            
                    PrivateKey privateKey = HelperUtilities.getPrivateKeyFromFile(pathPrivateKey);
                    byte[] challengeSignedInBytes = SignData.sign(challenge.getBytes(), privateKey);
                    String signedChallenge = HelperUtilities.encodeToByte(challengeSignedInBytes);
                    
                    params.clear();
                    params.put("challenge", HelperUtilities.encode(challenge));
                    params.put("challengeSigned", signedChallenge);
                    
                    System.out.println("Signed Challenge to send to the organization portail:\n"+signedChallenge);
                    
                    String[] samlResponse = HttpRequestUtil.sendHttpRequest(idpUrl, METHOD, params, ENCODING);
                    resultInString = HelperUtilities.stringArrayToString(samlResponse);
                    
                    String samlResonseDecoded = HelperUtilities.decode(resultInString);
                    System.out.println("SAML RESPONSE RECEIVED: \n" + samlResonseDecoded);
                    
                    SNAA port = WisebedServiceHelper.getSNAAService(defaultSnaaUrl);
                    
                    if (args[0].equalsIgnoreCase("-authenticate")) {
                        // Send samlResponse to testbed
                        AuthenticationSAML authenticationSAML = new AuthenticationSAML();
                        authenticationSAML.setSAMLAssertion(resultInString);

                        NodeUrnPrefix urnPrefix = new NodeUrnPrefix();
                        urnPrefix.setNodeUrnPrefix(nodeUrnPrefix);
                        authenticationSAML.setUrnPrefix(urnPrefix);                    



                        Authenticate authenticationData = new Authenticate();
                        List<AuthenticationSAML> authenticationSAMLs = authenticationData.getCertAuthenticationData();
                        authenticationSAMLs.add(authenticationSAML);


                        try {
                            AuthenticateResponse authenticateResponse = port.authenticate(authenticationData);
                            System.out.println("Authentication suceeded, secret authentication key(s): ");
                            List<SecretAuthenticationKey> secretAuthenticationKeys = authenticateResponse.getSecretAuthenticationKey();
                            for (SecretAuthenticationKey sak : secretAuthenticationKeys) {
                                    System.out.println("\tuser[" + sak.getUsername() + "], urnprefix[" + sak.getUrnPrefix() + "], key["
                                                            + sak.getKey()+ "]");
                            }
                        
                        } catch (AuthenticationFault ex) {
                            Logger.getLogger(SNAAClient.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (SNAAFault_Exception ex) {
                            Logger.getLogger(SNAAClient.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else if (args[0].equalsIgnoreCase("-authorize")) {
                        Action actionObj = Action.fromValue(action);
                        NodeUrnPrefix urnPrefix = new NodeUrnPrefix();
                        urnPrefix.setNodeUrnPrefix(nodeUrnPrefix);
                        List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(userId, urnPrefix, nodeUrns.split(","));
                        try {
                            AuthorizationResponse authorizationResponse =  port.isAuthorized(usernameNodeUrnsMaps, actionObj);
                            System.out.println("Authorization " + (authorizationResponse.isAuthorized() ? "suceeded": "failed"));
                            
                        } catch (SNAAFault_Exception ex) {
                            Logger.getLogger(SNAAClient.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        System.out.println("Unknow option: "+args[0]);
                    }
                }
                
            } catch (FileNotFoundException ex) {
                Logger.getLogger(SNAAClient.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(SNAAClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        } else {
            System.out.println("Please provide your configuration file for the experiment");
            System.out.println("Usage: [program] pathToExperimentFile");
        }
       
    }
}
