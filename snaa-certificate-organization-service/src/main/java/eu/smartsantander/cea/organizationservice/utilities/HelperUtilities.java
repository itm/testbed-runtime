/*******************************************************************************
* Copyright (c) 2013 CEA LIST.
* Contributor:
*   ROUX Pierre
*   Kim Thuat NGUYEN
*******************************************************************************/
package eu.smartsantander.cea.organizationservice.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Properties;




public class HelperUtilities {


	private static final Logger log = LoggerFactory.getLogger(HelperUtilities.class);

	public static Properties getProperties() {
		Properties props = new Properties();
		try {
			props.load(HelperUtilities.class.getClassLoader().getResourceAsStream("config.properties"));
		} catch (IOException e) {
			log.error(e.getMessage(),e);
		}
		return props;
	}

	public static String getProperty(String nameProperty) {
		return getProperties().getProperty(nameProperty);
	}

	public static boolean deleteFile(String pathToFile) {
		File file = new File(pathToFile);
		return file.delete();
	}
}
