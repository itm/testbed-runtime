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
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.logcontroller;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import eu.wisebed.testbed.api.wsn.ControllerHelper;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import eu.wisebed.testbed.api.wsn.v22.Controller;
import eu.wisebed.testbed.api.wsn.v22.Message;
import eu.wisebed.testbed.api.wsn.v22.RequestStatus;
import eu.wisebed.testbed.api.wsn.v22.SecretReservationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Connects Controller with WSN and adds listener-functionality
 */
public class WSNBinding {
    private Logger log = LoggerFactory.getLogger(WSNBinding.class);
    private Endpoint wsnEndpoint;
    private Endpoint controllerEndpoint;
    private Map<String, String> secretReservationKey = Maps.newHashMap();
    private final Set<IMessageListener> listeners = Sets.newHashSet();
    private final ControllerHelper controllerHelper = new ControllerHelper();
    private String wsnProxyAddress;
    private final String _controllerUrnPrefix;
    private final String wsnUrnPrefix;
    private SecureIdGenerator _secureIdGenerator = new SecureIdGenerator();

    /**
     * delegate for the controller-service, intercepts all receive()-calls
     */
    @WebService(name = "ProxyController", targetNamespace = "urn:ControllerService")
    public class ProxyController implements Controller {

		@Override
		public void receive(@WebParam(name = "msg", targetNamespace = "") final List<Message> msgs) {
			for (Message msg : msgs) {
				log.debug("before intercept");
				interceptReceive(msg);
				controllerHelper.receive(msg);
			}
		}

		@Override
		public void receiveStatus(@WebParam(name = "status", targetNamespace = "") final List<RequestStatus> requestStatuses) {
			controllerHelper.receiveStatus(requestStatuses);
		}

		@Override
		public void receiveNotification(@WebParam(name = "msg", targetNamespace = "") final List<String> msgs) {
			controllerHelper.receiveNotification(msgs);
		}

		@Override
		public void experimentEnded() {
			controllerHelper.experimentEnded();
		}
	}

    public WSNBinding(List<SecretReservationKey> secretReservationKey, String controllerUrnPrefix, String wsnUrnPrefix) {
        Preconditions.checkNotNull(secretReservationKey, "SecretReservationKey is null!");
        Preconditions.checkNotNull(controllerUrnPrefix, "ControllerUrnPrefix is null!");
        Preconditions.checkNotNull(wsnUrnPrefix, "wnsUrnPrefix is null!");
        _controllerUrnPrefix = controllerUrnPrefix;
        this.wsnUrnPrefix = wsnUrnPrefix;
        for (SecretReservationKey key : secretReservationKey)
            this.secretReservationKey.put(key.getUrnPrefix(), key.getSecretReservationKey());
    }

    /**
     * sets the urn of the client-controller
     *
     * @param controllerEndpoint
     */
    public void setController(String controllerEndpoint) {
        Preconditions.checkNotNull(controllerEndpoint, "ControllerEndpoint is null!");
        controllerHelper.removeController(controllerEndpoint);
        controllerHelper.addController(controllerEndpoint);
    }

    /**
     * starts the wsn-service-proxy
     *
     * @param wsnEndpoint source-address
     * @return proxy-address
     */
    public String startWSN(String wsnEndpoint) {
        WSNDelegate wsn = new WSNDelegate(WSNServiceHelper.getWSNService(wsnEndpoint));
        wsnProxyAddress = wsnUrnPrefix + _secureIdGenerator.getNextId();
        this.wsnEndpoint = Endpoint.publish(wsnProxyAddress, wsn);
        return wsnProxyAddress;
    }

    /**
     * gets the urn of the wsn-service-proxy
     *
     * @return
     */
    public String getWSN() {
        return wsnProxyAddress;
    }

    /**
     * starts the controller-service-proxy
     *
     * @param controllerEndpoint client-controller-address
     * @return proxy-controller-address
     */
    public String startController(String controllerEndpoint) {
        if (this.controllerEndpoint != null && this.controllerEndpoint.isPublished())
            this.controllerEndpoint.stop();
        setController(controllerEndpoint);
        String controllerAddress = _controllerUrnPrefix + _secureIdGenerator.getNextId();
        this.controllerEndpoint = Endpoint.publish(controllerAddress,
                new ProxyController());
		log.debug("Controller-Service on {} published", controllerAddress);
        return controllerAddress;
    }

    /**
     * stops all active endpoints
     */
    public void stop() {
        if (wsnEndpoint.isPublished())
            wsnEndpoint.stop();
        if (controllerEndpoint.isPublished())
            controllerEndpoint.stop();
        for (IMessageListener listener : this.listeners)
            listener.dispose();
    }

    public void addMessageListener(IMessageListener listener) {
        listeners.add(listener);
    }

    /**
     * Calls all message listeners
     *
     * @param msg
     */
    private void interceptReceive(Message msg) {
        String key = null;
        for (String prefix : secretReservationKey.keySet())
            if (msg.getSourceNodeId().toLowerCase().contains(prefix))
                key = secretReservationKey.get(prefix);
        Preconditions.checkNotNull(key, "No Reservation Key for Node {} found!",
                msg.getSourceNodeId());
        for (IMessageListener listener : this.listeners)
            listener.newMessage(msg, key);
    }

}
