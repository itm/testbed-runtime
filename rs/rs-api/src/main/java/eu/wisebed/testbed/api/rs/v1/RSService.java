package eu.wisebed.testbed.api.rs.v1;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.6 in JDK 6
 * Generated source version: 2.1
 */
@WebServiceClient(name = "RSService", targetNamespace = "urn:RSService", wsdlLocation = "REPLACE_WITH_ACTUAL_URL")
public class RSService extends Service {

	public RSService(URL wsdlLocation) {
		super(wsdlLocation, new QName("urn:RSService", "RSService"));
	}

	/**
	 * @return returns RS
	 */
	@WebEndpoint(name = "RSPort")
	public RS getRSPort() {
		return super.getPort(new QName("urn:RSService", "RSPort"), RS.class);
	}

	/**
	 * @param features A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
	 * @return returns RS
	 */
	@WebEndpoint(name = "RSPort")
	public RS getRSPort(WebServiceFeature... features) {
		return super.getPort(new QName("urn:RSService", "RSPort"), RS.class, features);
	}

}
