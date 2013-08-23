/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uniluebeck.itm.tr.snaa.certificate;


public class SNAACertificateConfig {

	public static String CERTIFICATE_DIRECTORY_NAME = "certs";

	public static String USER_ID_SAML_ATTRIBUTE_NAME = "userId";

	public static String ORGANIZATION_ID_ATTRIBUTE_NAME = "idpId";

	public static String ROLE_ATTRIBUTE_NAME = "role";

	public static String TRUST_STORE_PW = "changeit";

	public static String PATH_TO_TRUST_STORE_LINUX = System.getProperty("user.dir") + "/certs/truststore/cacerts.jks";

	public static String PATH_TO_TRUST_STORE_WINDOWS = System.getProperty("user.dir") + "\\certs\\truststore\\cacerts.jks";


}
