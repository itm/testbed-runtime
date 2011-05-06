/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package eu.wisebed.motap.connector;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.GregorianCalendar;
import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.ws.Endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coalesenses.otap.core.connector.DeviceConnector;
import com.coalesenses.otap.core.seraerial.SerAerialPacket;
import com.google.common.collect.Lists;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;
import de.uniluebeck.itm.wsn.devicedrivers.generic.PacketTypes;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.Controller;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.sm.SecretReservationKey;
import eu.wisebed.api.sm.SessionManagement;
import eu.wisebed.api.wsn.WSN;
import eu.wisebed.testbed.api.wsn.Constants;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;


class WisebedMotapConnectorImpl extends DeviceConnector {

	@WebService(serviceName = "ControllerService", targetNamespace = Constants.NAMESPACE_CONTROLLER_SERVICE,
			portName = "ControllerPort", endpointInterface = Constants.ENDPOINT_INTERFACE_CONTROLLER_SERVICE)
	public class MyController implements Controller {

		@Override
		public void receive(@WebParam(name = "msg", targetNamespace = "") final List<Message> msgs) {
			for (Message msg : msgs) {

				if (log.isTraceEnabled()) {
					log.trace("{} | {} => {}", new Object[] {
							msg.getTimestamp().toString(),
							msg.getSourceNodeId(),
							StringUtils.toHexString(msg.getBinaryData())
					});
				}

				if (nodeURN.equals(msg.getSourceNodeId())) {
					receivePacket(new MessagePacket(msg.getBinaryData()));
				}
			}
		}

		@Override
		public void receiveStatus(@WebParam(name = "status", targetNamespace = "") final List<RequestStatus> status) {
			// nothing to do
		}

		@Override
		public void receiveNotification(@WebParam(name = "msg", targetNamespace = "") final List<String> msgs) {
			for (String msg : msgs) {
				log.info("{}", msg);
			}
		}

		@Override
		public void experimentEnded() {
			// nothing to do
		}
	}

	private static final Logger log = LoggerFactory.getLogger(WisebedMotapConnectorImpl.class);

	private static DatatypeFactory datatypeFactory;

	static {
		try {
			datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			log.error("" + e, e);
			System.exit(1);
		}
	}

	private MyController controller = new MyController();

	private WSN wsnEndpoint;

	private String controllerEndpointURL;

	private HttpServer server;

	private String nodeURN;

	private HttpContext controllerEndpointContext;

	private Endpoint controllerEndpoint;

	public WisebedMotapConnectorImpl(final SessionManagement sessionManagement, final HttpServer server,
									 final List<SecretReservationKey> secretReservationKeys, final String nodeURN)
			throws Exception {

		this.server = server;
		this.nodeURN = nodeURN;

		if (!WiseMLHelper.getNodeUrns(sessionManagement.getNetwork()).contains(nodeURN)) {
			throw new IllegalArgumentException("The node " + nodeURN + " was not found in the testbed!");
		}

		SecureIdGenerator secureIdGenerator = new SecureIdGenerator();
		String path = secureIdGenerator.getNextId();

		controllerEndpointContext = server.createContext("/" + path);
		controllerEndpoint = Endpoint.create(controller);
		controllerEndpoint.publish(controllerEndpointContext);

		controllerEndpointURL = "http://" + InetAddress.getLocalHost().getCanonicalHostName() +
				":" + server.getAddress().getPort() +
				"/" + path;
		log.info("Bound local controller endpoint to {}.", controllerEndpointURL);

		String wsnEndpointURL = sessionManagement.getInstance(secretReservationKeys, controllerEndpointURL);
		log.info("Connected to testbed node {}.", nodeURN);
		wsnEndpoint = WSNServiceHelper.getWSNService(wsnEndpointURL);

	}

	@Override
	public void handleConfirm(final SerAerialPacket p) {
		// nothing to do
	}

	@Override
	public void shutdown() {

		log.debug("Unregistering local controller endpoint at testbed.");
		try {
			wsnEndpoint.removeController(controllerEndpointURL);
		} catch (Exception e) {
			log.trace("" + e, e);
			// silently ignore exceptions as we're shutting down anyway
		}

		log.debug("Shutting down local controller endpoint.");
		server.removeContext(controllerEndpointContext);

		log.debug("Shutdown complete.");

	}

	@Override
	public boolean sendPacket(final SerAerialPacket p) {
		if (p instanceof com.coalesenses.otap.core.seraerial.SerialRoutingPacket) {
			return send(PacketTypes.ISENSE_ISHELL_INTERPRETER & 0xFF, p.toByteArray());

		} else {
			return send(PacketTypes.SERAERIAL & 0xFF, p.toByteArray());

		}
	}

	@Override
	public boolean send(final int type, final byte[] data) {
		try {

			Message message = new Message();
			ByteBuffer buffer = ByteBuffer.allocate(1 + data.length);
			buffer.put((byte) (type & 0xFF));
			buffer.put(data);
			message.setBinaryData(buffer.array());
			message.setSourceNodeId(nodeURN);
			message.setTimestamp(datatypeFactory.newXMLGregorianCalendar(new GregorianCalendar()));

			wsnEndpoint.send(Lists.newArrayList(nodeURN), message);

			log.trace("Sent packet {} to {} in testbed.", message, nodeURN);

		} catch (Exception e) {
			log.error("" + e, e);
			return false;
		}
		return true;
	}

}
