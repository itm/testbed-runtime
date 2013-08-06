/*******************************************************************************
* Copyright (c) 2013 CEA LIST.
* Contributor:
*   ROUX Pierre
*   Kim Thuat NGUYEN
*******************************************************************************/
package com.smartsantander.cea.organizationservice.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;



public class HelperUtilities {
    
        
      public static Properties getProperties() {
        Properties props = new Properties();
        try {
            props.load(HelperUtilities.class.getClassLoader().getResourceAsStream("config.properties"));
        } catch (IOException ie) {
            ie.printStackTrace();
        }
        return props;
      }
     
      public static Properties getTmpProperties() {
         Properties props = new Properties();
        try {
           props.load(HelperUtilities.class.getClassLoader().getResourceAsStream("tmp.properties"));
         } catch (IOException ie) {
            ie.printStackTrace();
        }
        return props; 
      }
      // TODO: Need to change when there is concurrent access.
      public static void saveCurrentClient(String userId, String idpId, String userDir) {
        try {
            Properties props = new Properties();
            props.setProperty("userId", userId);
            props.setProperty("idpId", idpId);
            
            if (!eu.smartsantander.cea.utils.Helper.HelperUtilities.isWindows()) {
                String pathToFile = userDir+"/src/main/resources/tmp.properties";
                if (deleteFile(pathToFile)) {
                    props.store(new FileOutputStream(pathToFile), "Identity of current user");
                }
                System.out.println("Can NOT delete file tmp.properties");
            } else {
                String pathToFile = userDir+"\\src\\main\\resources\\tmp.properties";
                if (deleteFile(pathToFile)) {
                    props.store(new FileOutputStream(pathToFile), "Identity of current user");
                }
                System.out.println("Can NOT delete file tmp.properties");
            }
   
        } catch (IOException ie) {
            ie.printStackTrace();
        } 
      }
      
      
      public static String getUserDir(HttpServletRequest request) {
        String userDir = "";
        String absuluPath = request.getSession().getServletContext().getRealPath("/");
        int indexOfTarget = absuluPath.indexOf("target");
        userDir = absuluPath.substring(0, indexOfTarget-1);
        return userDir;
      }
      
     
      public static String getProperty(String nameProperty) {
          return getProperties().getProperty(nameProperty);
      }
      
      public static boolean deleteFile(String pathToFile) {
          File file = new File(pathToFile);
          return file.delete();
      }
}
