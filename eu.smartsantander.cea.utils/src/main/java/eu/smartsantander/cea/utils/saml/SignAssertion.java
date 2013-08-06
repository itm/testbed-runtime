/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.smartsantander.cea.utils.saml;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.impl.ResponseMarshaller;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.security.SecurityConfiguration;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.keyinfo.KeyInfoGenerator;
import org.opensaml.xml.security.keyinfo.KeyInfoGeneratorFactory;
import org.opensaml.xml.security.keyinfo.KeyInfoGeneratorManager;
import org.opensaml.xml.security.keyinfo.NamedKeyInfoGeneratorManager;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureConstants;
import org.opensaml.xml.signature.SignatureException;
import org.opensaml.xml.signature.Signer;
import org.w3c.dom.Element;

/**
 *
 * 
 */
public class SignAssertion {
    
    private Assertion assertionToSign;
    private String pathTokeystorefile;
    private String keystorePassword;
    private String certificateAliasName;

    
    public SignAssertion(Assertion assertionToSign, String pathTokeystorefile, String keystorePassword, String certificateAliasName) {
        this.assertionToSign = assertionToSign;
        this.pathTokeystorefile = pathTokeystorefile;
        this.keystorePassword = keystorePassword;
        this.certificateAliasName = certificateAliasName;
    }
    
        
    public String getKeystorefile() {
        return pathTokeystorefile;
    }

    public void setKeystorefile(String keystorefile) {
        this.pathTokeystorefile = keystorefile;
    }

    public String getPassword() {
        return keystorePassword;
    }

    public void setPassword(String password) {
        this.keystorePassword = password;
    }

    public String getCertificateAliasName() {
        return certificateAliasName;
    }

    public void setCertificateAliasName(String certificateAliasName) {
        this.certificateAliasName = certificateAliasName;
    }
   
    
    public Credential getSignningCredentialFromKeyStore() {
        // Load Keystore
        KeyStore ks = null;
        FileInputStream file = null;
        char[] pwd = this.keystorePassword.toCharArray();
        
        try {
            // Get the default instance of Keystore
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            
            // Read keystore file as file input stream
            file = new FileInputStream(this.pathTokeystorefile);
            
            // Load the Keystore
            ks.load(file, pwd);
            
            // Close the file
            file.close();
        
            // Get private key entry from Certificate
     
            KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(this.certificateAliasName, new KeyStore.PasswordProtection(this.keystorePassword.toCharArray()));
            
            PrivateKey pk = pkEntry.getPrivateKey();
            
            X509Certificate certificate = (X509Certificate) pkEntry.getCertificate();
            BasicX509Credential credential = new BasicX509Credential();
            credential.setEntityCertificate(certificate);
            credential.setPrivateKey(pk);
            return credential;
        } catch (KeyStoreException ke) {
            ke.printStackTrace();
        } catch (FileNotFoundException fe) {
            fe.printStackTrace();
        } catch (NoSuchAlgorithmException ne) {
            ne.printStackTrace();
        } catch (CertificateException ce) {
            ce.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
       return null;
    }
    
    public Response singnAssertion(Credential signingCredential) {
        try {
                       
            DefaultBootstrap.bootstrap();
     
            Signature signature = (Signature) Configuration.getBuilderFactory().getBuilder(Signature.DEFAULT_ELEMENT_NAME).buildObject(Signature.DEFAULT_ELEMENT_NAME);
            signature.setSigningCredential(signingCredential);
            signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            
               
            SecurityConfiguration secConfiguration = Configuration.getGlobalSecurityConfiguration();
            NamedKeyInfoGeneratorManager namedKeyInfoGeneratorManager = secConfiguration.getKeyInfoGeneratorManager(); 
            KeyInfoGeneratorManager keyInfoGeneratorManager = namedKeyInfoGeneratorManager.getDefaultManager();            KeyInfoGeneratorFactory keyInfoGeneratorFactory = keyInfoGeneratorManager.getFactory(signingCredential);
            KeyInfoGenerator keyInfoGenerator = keyInfoGeneratorFactory.newInstance();            KeyInfo keyInfo = null;
         
            try {
                keyInfo = keyInfoGenerator.generate(signingCredential);
            } catch  (Exception e) {
             
            } 
        
            signature.setKeyInfo(keyInfo);
             
            Assertion assertion = getAssertion();
          
            Response res = (Response) Configuration.getBuilderFactory().getBuilder(Response.DEFAULT_ELEMENT_NAME).buildObject(Response.DEFAULT_ELEMENT_NAME);
            res.getAssertions().add(assertion);
             
            res.setSignature(signature);
         
            Configuration.getMarshallerFactory().getMarshaller(res).marshall(res);
             
            Signer.signObject(signature);
             
             
         
            ResponseMarshaller marshaller1 = new ResponseMarshaller();
            Element plaintext = marshaller1.marshall(res);
 
            return res;
        } catch (SecurityException e) {
         e.printStackTrace();
        } catch (SignatureException e) {
         e.printStackTrace();
      } catch (ConfigurationException ce) {
          ce.printStackTrace();
      }
        catch (Exception e) {
            e.printStackTrace();
      }
        
        return null;
    }
    
    
    public Assertion getAssertion() {
        return this.assertionToSign;
    }
        
    
}
