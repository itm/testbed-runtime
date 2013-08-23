/*******************************************************************************
* Copyright (c) 2013 CEA LIST.
* Contributor:
*   ROUX Pierre
*   Kim Thuat NGUYEN
*******************************************************************************/
package eu.smartsantander.cea.utils.certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;


public class CertificateUtilies {


	/**
	 * Logs messages
	 */
	private static final Logger log = LoggerFactory.getLogger(CertificateUtilies.class);

    public static X509Certificate getCertificate(String pathToFolder) {

        
        log.debug(pathToFolder);
        String fileName=null;
        File folder = new File(pathToFolder);
        File[] listFiles = folder.listFiles();
        if (listFiles.length>0) {
            fileName = listFiles[0].getName();
        }
        
        InputStream in = null;
        try {
            String path = pathToFolder+File.separator+fileName;
            in = new FileInputStream(path);
            CertificateFactory cf = CertificateFactory.getInstance("X509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
            return cert;
        } catch (FileNotFoundException ex) {
            log.error("File "+ pathToFolder+ " not found", ex);
        } catch (CertificateException ex) {
	        log.error("Certificate exception", ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
	            log.error("IOE exception", ex);
            }
        }
        return null;
    }
    
     
}
