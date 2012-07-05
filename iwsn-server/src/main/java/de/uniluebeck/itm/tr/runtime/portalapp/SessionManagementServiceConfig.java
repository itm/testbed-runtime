package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.collect.ImmutableSet;
import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;
import de.uniluebeck.itm.tr.runtime.portalapp.xml.Portalapp;
import de.uniluebeck.itm.tr.runtime.portalapp.xml.ProtobufInterface;

import java.net.MalformedURLException;
import java.net.URL;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A helper class that holds configuration options.
 */
public class SessionManagementServiceConfig {

	/**
	 * The configuration object for the optional protocol buffers interface a client can connect to.
	 */
	private final ProtobufInterface protobufinterface;

	/**
	 * The maximum size of the message delivery queue after which messages to the client are discarded.
	 */
	private final Integer maximumDeliveryQueueSize;

	/**
	 * The sessionManagementEndpoint URL of this Session Management service instance.
	 */
	private final URL sessionManagementEndpointUrl;

	/**
	 * The sessionManagementEndpoint URL of the reservation system that is used for fetching node URNs from the
	 * reservation data. If it is {@code null} then the reservation system is not used.
	 */
	private final URL reservationEndpointUrl;

	/**
	 * The endpoint URL of the authentication and authorization system. If it is {@code null} then the system is not used
	 * and users are assumed to be always authorized.
	 * <p/>
	 * TODO currently the SNAA is not used inside SessionManagement or WSN instances for authorization, i.e. there is no
	 * authorization currently
	 */
	private URL snaaEndpointUrl;

	/**
	 * The URN prefix that is served by this instance.
	 */
	private final String urnPrefix;

	/**
	 * The base URL (i.e. prefix) that is used as the prefix of a newly created WSN API instance.
	 */
	private final URL wsnInstanceBaseUrl;

	/**
	 * The filename of the file containing the WiseML document that is to delivered when {@link
	 * eu.wisebed.api.sm.SessionManagement#getNetwork()} is called.
	 */
	private final String wiseMLFilename;

	private final ImmutableSet<String> nodeUrnsServed;

	public SessionManagementServiceConfig(Portalapp config) {

		try {

			de.uniluebeck.itm.tr.runtime.portalapp.xml.WebService webservice = config.getWebservice();

			checkNotNull(webservice.getUrnprefix());
			checkNotNull(webservice.getSessionmanagementendpointurl());
			checkNotNull(webservice.getWsninstancebaseurl());
			checkNotNull(webservice.getWisemlfilename());

			final String serializedWiseML = WiseMLHelper.readWiseMLFromFile(webservice.getWisemlfilename());
			if (serializedWiseML == null) {
				throw new RuntimeException("Could not read WiseML from file " + webservice.getWisemlfilename() + ". "
						+ "Please make sure the file exists and is readable."
				);
			}

			this.nodeUrnsServed = ImmutableSet.<String>builder().addAll(WiseMLHelper.getNodeUrns(serializedWiseML)).build();
			this.protobufinterface = config.getWebservice().getProtobufinterface();
			this.maximumDeliveryQueueSize = config.getWebservice().getMaximumdeliveryqueuesize();
			this.sessionManagementEndpointUrl = new URL(config.getWebservice().getSessionmanagementendpointurl());
			this.reservationEndpointUrl = new URL(config.getWebservice().getReservationendpointurl());
			this.snaaEndpointUrl = new URL(config.getWebservice().getSnaaendpointurl());
			this.urnPrefix = config.getWebservice().getUrnprefix();

			this.wsnInstanceBaseUrl = new URL(config.getWebservice().getWsninstancebaseurl().endsWith("/") ?
					config.getWebservice().getWsninstancebaseurl() :
					config.getWebservice().getWsninstancebaseurl() + "/"
			);

			this.wiseMLFilename = config.getWebservice().getWisemlfilename();

		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public Integer getMaximumDeliveryQueueSize() {
		return maximumDeliveryQueueSize;
	}

	public ProtobufInterface getProtobufinterface() {
		return protobufinterface;
	}

	public URL getReservationEndpointUrl() {
		return reservationEndpointUrl;
	}

	public URL getSessionManagementEndpointUrl() {
		return sessionManagementEndpointUrl;
	}

	public URL getSnaaEndpointUrl() {
		return snaaEndpointUrl;
	}

	public String getUrnPrefix() {
		return urnPrefix;
	}

	public String getWiseMLFilename() {
		return wiseMLFilename;
	}

	public URL getWsnInstanceBaseUrl() {
		return wsnInstanceBaseUrl;
	}

	public ImmutableSet<String> getNodeUrnsServed() {
		return nodeUrnsServed;
	}
}