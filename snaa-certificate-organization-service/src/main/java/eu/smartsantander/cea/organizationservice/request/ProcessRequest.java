/*******************************************************************************
* Copyright (c) 2013 CEA LIST.
* Contributor:
*   ROUX Pierre
*   Kim Thuat NGUYEN
*******************************************************************************/

package eu.smartsantander.cea.organizationservice.request;


import eu.smartsantander.cea.organizationservice.dao.ConnnectionBD;
import eu.smartsantander.cea.organizationservice.utilities.Config;
import eu.smartsantander.cea.utils.helper.HelperUtilities;
import eu.smartsantander.cea.utils.saml.SAMLGenerator;
import eu.smartsantander.cea.utils.saml.SAMLInput;
import eu.smartsantander.cea.utils.saml.SignAssertion;
import eu.smartsantander.cea.utils.signature.VerifyData;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;
import org.opensaml.xml.security.credential.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;



@WebServlet(name = "ProcessRequest", urlPatterns = {"/ProcessRequest"})
public class ProcessRequest extends HttpServlet {

	private static final Logger log = LoggerFactory.getLogger(ProcessRequest.class);

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String userDir = ProcessRequest.class.getClassLoader().getResource(".").getPath();
        
        String userId = request.getParameter("userId");
        String idpId = request.getParameter("idpId");
        String challenge =  request.getParameter("challenge");
        String challengeSigned = request.getParameter("challengeSigned");
        
        if (challenge!=null && challengeSigned!=null) {
            
            byte[] signedData = eu.smartsantander.cea.utils.helper.HelperUtilities.decodeToByte(challengeSigned);
            byte[] challengeBytes = eu.smartsantander.cea.utils.helper.HelperUtilities.decodeToByte(challenge);
            
            log.debug("Challenge received in client request: "+eu.smartsantander.cea.utils.helper.HelperUtilities.decode(challenge));
            
            
            log.debug("IDP SIDE: VERIFYING THE SIGNED CHALLENGE USING CLIENT PUBLIC KEY");
            boolean isValid = VerifyData.verifySignature(
		            challengeBytes,
		            HelperUtilities.getPublicKeyFromFile(
				            userDir +
				            File.separator +
				            Config.CLIENT_PUBKEY_DIRECTORY +
				            File.separator +
				            userId),
		            signedData);


            if (isValid) {
                log.debug("IDP SIDE: SINGED CHALLENGE IS VERIFIED SUCCESSFULLY. GENERATING SAMLRESPONSE TO CLIENT.......");
                try {
                    ConnnectionBD dao = new ConnnectionBD();
                    String role = dao.getUserRole(userId);
                    // Generate SAML Response to Client
                    Map attributs = new HashMap();
                    attributs.put("userId", userId);
                    attributs.put("idpId", idpId);
                    attributs.put("role", role);
                    
                    SAMLInput input = new SAMLInput(idpId, userId, "defautlNameQualifier", "defaultSessionId", Config.SAML_max_session_timeout, attributs);
                    SAMLGenerator generator = new SAMLGenerator();
                    Assertion assertion  = generator.createAssertion(input); 
                    SignAssertion signer = new SignAssertion(assertion, Config.pathToKeyStoreFile, Config.keystorePassword, Config.certificateAllias);
                    Credential credential = signer.getSignningCredentialFromKeyStore();
                    Response res = signer.singnAssertion(credential);
                    String responseToSend = eu.smartsantander.cea.utils.helper.HelperUtilities.samlResponseToString(res);
                    String responseEncoded = eu.smartsantander.cea.utils.helper.HelperUtilities.encode(responseToSend);
                    Map params = new HashMap();
                    params.put("SAMLResponse", responseEncoded);
                    log.debug("IDP SIDE: SENT SAMLRESPONSE TO CLIENT");
                    
                    out.println(responseEncoded);
                } catch (Exception e) {
                    log.error(e.getMessage(),e);
                }
            }
        } else {
            // Verify if the userId existe in the database
            ConnnectionBD conn = new ConnnectionBD();
            boolean isUserIdExist = conn.isUserIdExist(userId);
            if (!isUserIdExist) {
                String error = "USERID "+userId+ "  DOES NOT EXIST";
                out.println("["+error+"]");
            } else {
                                
                challenge = getSecureNumber();
                log.debug("IN IDP SIDE: Challenge generated: "+challenge);
                
                String challengeBytes = eu.smartsantander.cea.utils.helper.HelperUtilities.encode(challenge);
                
                out.println("challenge="+challengeBytes);
            }
       }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
    
     public String getSecureNumber() {
        SecureRandom secureRandom = new SecureRandom();
        int nb = secureRandom.nextInt();
        return String.valueOf(nb);
    }
    

      
}
