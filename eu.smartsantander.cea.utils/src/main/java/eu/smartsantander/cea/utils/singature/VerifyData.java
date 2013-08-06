/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.smartsantander.cea.utils.singature;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;


public class VerifyData {
    
    private static String algorithm="MD5withRSA";
    
    public VerifyData(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
    
    
    public static boolean verifySignature(byte[] data, PublicKey pubkey, byte[] signedData) {
        try {
            Signature signer;
            signer = Signature.getInstance(algorithm);
            signer.initVerify(pubkey);
            signer.update(data);
            return signer.verify(signedData);
        } catch (NoSuchAlgorithmException na) {
            na.printStackTrace();
        } catch (InvalidKeyException ie) {
            ie.printStackTrace();
        } catch (SignatureException se) {
            se.printStackTrace();
        }
        
        return false;
    }
}
