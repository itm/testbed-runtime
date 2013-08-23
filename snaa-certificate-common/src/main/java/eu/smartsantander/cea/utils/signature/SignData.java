/*******************************************************************************
* Copyright (c) 2013 CEA LIST.
* Contributor:
*   ROUX Pierre
*   Kim Thuat NGUYEN
*******************************************************************************/
package eu.smartsantander.cea.utils.signature;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;


public class SignData {
    
    private static final String ALGORITHM = "MD5withRSA"; 
    
    public static byte[] sign(byte[] data, PrivateKey pkey) {
         byte[] signedData = null;
         try {
             Signature signature = Signature.getInstance(ALGORITHM);
             signature.initSign(pkey);
             signature.update(data);
             signedData = signature.sign();
         } catch (NoSuchAlgorithmException ne) {
             ne.printStackTrace();
         } catch (InvalidKeyException ie) {
             ie.printStackTrace();
         } catch (SignatureException se) {
             se.printStackTrace();
         }
         
         return signedData;
     }
}
