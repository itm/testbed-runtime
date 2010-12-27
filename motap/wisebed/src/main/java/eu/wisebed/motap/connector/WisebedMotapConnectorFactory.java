package eu.wisebed.motap.connector;

import com.coalesenses.otap.core.connector.DeviceConnector;
import com.sun.net.httpserver.HttpServer;
import eu.wisebed.testbed.api.wsn.v211.SecretReservationKey;
import eu.wisebed.testbed.api.wsn.v211.SessionManagement;

import java.util.List;


public class WisebedMotapConnectorFactory {

	/**
	 * Creates a {@link DeviceConnector} instance that is connected to a remote WISEBED testbed.
	 *
	 * @param sessionManagement	 the testbeds session management service instance
	 * @param server				the HTTP server that is used to run the local controller endpoint on
	 * @param secretReservationKeys the secret reservation key list that identifies the experiment to connect to
	 * @param nodeURN			   the URN of the node that we connect to
	 */
	public static DeviceConnector create(final SessionManagement sessionManagement, final HttpServer server,
										 final List<SecretReservationKey> secretReservationKeys, final String nodeURN)
			throws Exception {

		return new WisebedMotapConnectorImpl(sessionManagement, server, secretReservationKeys, nodeURN);
	}

}
