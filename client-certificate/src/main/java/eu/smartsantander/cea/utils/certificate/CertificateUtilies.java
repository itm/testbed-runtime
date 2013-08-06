/*******************************************************************************
* Copyright (c) 2013 CEA LIST.
* Contributor:
*   ROUX Pierre
*   Kim Thuat NGUYEN
*******************************************************************************/
package eu.smartsantander.cea.utils.certificate;

import eu.smartsantander.cea.utils.Helper.HelperUtilities;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Level;

/**
 *
 * 
 */
public class CertificateUtilies {
    
    public static X509Certificate getCertificate(String pathToFolder) {
        
        System.out.println("DEBUG: "+pathToFolder);
        String fileName=null;
        File folder = new File(pathToFolder);
        File[] listFiles = folder.listFiles();
        if (listFiles.length>0) {
            fileName = listFiles[0].getName();
        }
        
        InputStream in = null;
        try {
            String path = "";
            if (HelperUtilities.isWindows()) {
                path = pathToFolder+"\\"+fileName;
            } else {
                path = pathToFolder+"/"+fileName;
            }
            in = new FileInputStream(path);
            CertificateFactory cf = CertificateFactory.getInstance("X509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
            return cert;
        } catch (FileNotFoundException ex) {
            java.util.logging.Logger.getLogger(CertificateUtilies.class.getName()).log(Level.SEVERE, "File "+ pathToFolder+ " not found", ex);
        } catch (CertificateException ex) {
            java.util.logging.Logger.getLogger(CertificateUtilies.class.getName()).log(Level.SEVERE, "Certificate exception", ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(CertificateUtilies.class.getName()).log(Level.SEVERE, "IOE exception", ex);
            }
        }
        return null;
    }
    
     
}
