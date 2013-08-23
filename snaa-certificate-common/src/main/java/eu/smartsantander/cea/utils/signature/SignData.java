/*******************************************************************************
* Copyright (c) 2013 CEA LIST.
* Contributor:
*   ROUX Pierre
*   Kim Thuat NGUYEN
*******************************************************************************/
package eu.smartsantander.cea.utils.signature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;


public class SignData {

	private static final Logger log = LoggerFactory.getLogger(SignData.class);
    
    private static final String ALGORITHM = "MD5withRSA"; 
    
    public static byte[] sign(byte[] data, PrivateKey pkey) {
         byte[] signedData = null;
         try {
             Signature signature = Signature.getInstance(ALGORITHM);
             signature.initSign(pkey);
             signature.update(data);
             signedData = signature.sign();
         } catch (NoSuchAlgorithmException e) {
	         log.error(e.getMessage(),e);
         } catch (InvalidKeyException e) {
	         log.error(e.getMessage(),e);
         } catch (SignatureException e) {
	         log.error(e.getMessage(),e);
         }
         
         return signedData;
     }
}
