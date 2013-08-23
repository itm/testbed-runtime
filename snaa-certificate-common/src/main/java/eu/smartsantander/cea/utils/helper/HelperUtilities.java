/*******************************************************************************
* Copyright (c) 2013 CEA LIST.
* Contributor:
*   ROUX Pierre
*   Kim Thuat NGUYEN
*******************************************************************************/
package eu.smartsantander.cea.utils.helper;

import eu.smartsantander.cea.utils.certificate.CertificateUtilies;
import org.apache.commons.codec.binary.Base64;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.impl.ResponseMarshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Properties;

public class HelperUtilities {

	/**
	 * Logs messages
	 */
	private static final Logger log = LoggerFactory.getLogger(CertificateUtilies.class);
        
    
    public static String stringArrayToString(String[] arrays) {
          String result = "";
          for (String s: arrays) {
              result+=s;
          }
          return result;
    }
    
    
    public static String getParameterValue(String nameParameter, String response) {
        String value = null;
        if (response.contains(nameParameter)) {
            
        }
        return value;
    }
    
    /********************************************
     * Encode and Decode using org.apache.commons
     *******************************************/
    
    public static String encode(String data) {
        return new String(Base64.encodeBase64(data.getBytes()));
    }
    
    public static String decode(String dataEncoded) {
        return new String(Base64.decodeBase64(dataEncoded));
    }
    
    public static byte[] decodeToByte(String dataEncoded) {
        return Base64.decodeBase64(dataEncoded);
    }
    
    public static String encodeToByte(byte[] data) {
        return Base64.encodeBase64String(data);
    }
    
    public static PrivateKey getPrivateKeyFromFile(String pathToPrivateKey) {
        PrivateKey pkey = null;
        try {
            File filePrivateKey = new File(pathToPrivateKey);
            byte[] encodePrivateKey;
            try (FileInputStream fis = new FileInputStream(pathToPrivateKey)) {
                encodePrivateKey = new byte[(int) filePrivateKey.length()];
                fis.read(encodePrivateKey);
            }

            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec privatekeySpec = new PKCS8EncodedKeySpec(encodePrivateKey);
            pkey = (PrivateKey) keyFactory.generatePrivate(privatekeySpec);
    
            return pkey;
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(),e);
        } catch (InvalidKeySpecException e) {
            log.error(e.getMessage(),e);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }
        return pkey;
    }
    
    public static PublicKey getPublicKeyFromFile(String pathToFileFolder) {
        PublicKey publicKey = null;
        File userFolder = new File(pathToFileFolder);
        log.debug("Path to user public key folder: "+ pathToFileFolder);
        File[] listFiles = userFolder.listFiles();
        if (listFiles.length == 0) {
           log.error("Folder empty. Public key for user does not exist");
        } else if (listFiles.length>1) {
           log.error("ERROR. Should have only one public key for user ");
        } else {
            String pubKeyFileName = listFiles[0].getName();
            try {
                File filePublicKey = new File(pathToFileFolder+"/"+pubKeyFileName);
                FileInputStream fis = new FileInputStream(pathToFileFolder+"/"+pubKeyFileName);
                byte[] encodePublicKey = new byte[(int) filePublicKey.length()];
                fis.read(encodePublicKey);
                fis.close();
              java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
                X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(encodePublicKey);
                publicKey = keyFactory.generatePublic(pubKeySpec);
            } catch (IOException e) {
	            log.error(e.getMessage(),e);
            } catch (NoSuchAlgorithmException e) {
	            log.error(e.getMessage(),e);
            } catch (InvalidKeySpecException e) {
	            log.error(e.getMessage(),e);
            } catch (Exception e) {
	            log.error(e.getMessage(),e);
            }
            
        }
        return publicKey;
    }
    
    
    // Get the configuration file saved in resources directory 
    public static Properties getProperties(String fileName) {
        Properties props = new Properties();
        try {
            props.load(HelperUtilities.class.getClassLoader().getResourceAsStream(fileName));
        } catch (IOException e) {
	        log.error(e.getMessage(),e);
        }
        return props;
      }
    
    public static void writeToFile(String infoToWrite, String fileName) {
        try { 
            try (BufferedWriter writer1 = new BufferedWriter(new FileWriter(fileName))) {
                writer1.write(infoToWrite);
            }
        } catch (IOException e) {
	        log.error(e.getMessage(),e);
        }
    }
    
    public static String samlResponseToString(Response res) {
          String result = null;
          try {
              ResponseMarshaller marshaller = new ResponseMarshaller();
              Element plainText = marshaller.marshall(res);
              result = XMLHelper.nodeToString(plainText);
          } catch (MarshallingException e) {
	          log.error(e.getMessage(),e);
          }
          return result;
      }

}
