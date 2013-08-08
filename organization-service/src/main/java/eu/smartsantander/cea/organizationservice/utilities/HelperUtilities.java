/*******************************************************************************
* Copyright (c) 2013 CEA LIST.
* Contributor:
*   ROUX Pierre
*   Kim Thuat NGUYEN
*******************************************************************************/
package eu.smartsantander.cea.organizationservice.utilities;

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
