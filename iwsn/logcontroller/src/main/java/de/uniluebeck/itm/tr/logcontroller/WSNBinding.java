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
import eu.wisebed.testbed.api.wsn.v211.Controller;
import eu.wisebed.testbed.api.wsn.v211.Message;
import eu.wisebed.testbed.api.wsn.v211.RequestStatus;
import eu.wisebed.testbed.api.wsn.v211.SecretReservationKey;
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
    private Logger _log = LoggerFactory.getLogger(WSNBinding.class);
    private Endpoint _wsnEndpoint;
    private Endpoint _controllerEndpoint;
    private Map<String, String> _secretReservationKey = Maps.newHashMap();
    private final Set<IMessageListener> _listener = Sets.newHashSet();
    private final ControllerHelper _controllerHelper = new ControllerHelper();
    private String _wsnProxyAddress;
    private final String _controllerUrnPrefix;
    private final String _wsnUrnPrefix;
    private SecureIdGenerator _secureIdGenerator = new SecureIdGenerator();

    /**
     * delegate for the controller-service, intercepts all receive()-calls
     */
    @WebService(name = "ProxyController", targetNamespace = "urn:ControllerService")
    public class proxyController implements Controller {
        @Override
        public void receive(@WebParam(name = "msg", targetNamespace = "") Message msg) {
            _log.debug("before intercept");
            interceptReceive(msg);
            _controllerHelper.receive(msg);
        }

        @Override
        public void receiveStatus(@WebParam(name = "status", targetNamespace = "") RequestStatus status) {
            _controllerHelper.receiveStatus(status);
        }
    }

    public WSNBinding(List<SecretReservationKey> secretReservationKey, String controllerUrnPrefix, String wsnUrnPrefix) {
        Preconditions.checkNotNull(secretReservationKey, "SecretReservationKey is null!");
        Preconditions.checkNotNull(controllerUrnPrefix, "ControllerUrnPrefix is null!");
        Preconditions.checkNotNull(wsnUrnPrefix, "wnsUrnPrefix is null!");
        _controllerUrnPrefix = controllerUrnPrefix;
        _wsnUrnPrefix = wsnUrnPrefix;
        for (SecretReservationKey key : secretReservationKey)
            _secretReservationKey.put(key.getUrnPrefix(), key.getSecretReservationKey());
    }

    /**
     * sets the urn of the client-controller
     *
     * @param controllerEndpoint
     */
    public void setController(String controllerEndpoint) {
        Preconditions.checkNotNull(controllerEndpoint, "ControllerEndpoint is null!");
        _controllerHelper.removeController(controllerEndpoint);
        _controllerHelper.addController(controllerEndpoint);
    }

    /**
     * starts the wsn-service-proxy
     *
     * @param wsnEndpoint source-address
     * @return proxy-address
     */
    public String startWSN(String wsnEndpoint) {
        WSNDelegate wsn = new WSNDelegate(WSNServiceHelper.getWSNService(wsnEndpoint));
        _wsnProxyAddress = _wsnUrnPrefix + _secureIdGenerator.getNextId();
        _wsnEndpoint = Endpoint.publish(_wsnProxyAddress, wsn);
        return _wsnProxyAddress;
    }

    /**
     * gets the urn of the wsn-service-proxy
     *
     * @return
     */
    public String getWSN() {
        return _wsnProxyAddress;
    }

    /**
     * starts the controller-service-proxy
     *
     * @param controllerEndpoint client-controller-address
     * @return proxy-controller-address
     */
    public String startController(String controllerEndpoint) {
        if (_controllerEndpoint != null && _controllerEndpoint.isPublished())
            _controllerEndpoint.stop();
        setController(controllerEndpoint);
        String controllerAddress = _controllerUrnPrefix + _secureIdGenerator.getNextId();
        _controllerEndpoint = Endpoint.publish(controllerAddress,
                new proxyController());
        _log.debug("Controller-Service on {} published", controllerAddress);
        return controllerAddress;
    }

    /**
     * stops all active endpoints
     */
    public void stop() {
        if (_wsnEndpoint.isPublished())
            _wsnEndpoint.stop();
        if (_controllerEndpoint.isPublished())
            _controllerEndpoint.stop();
        for (IMessageListener listener : _listener)
            listener.dispose();
    }

    public void addMessageListener(IMessageListener listener) {
        _listener.add(listener);
    }

    /**
     * calls all message-listener
     *
     * @param msg
     */
    private void interceptReceive(Message msg) {
        String key = null;
        for (String prefix : _secretReservationKey.keySet())
            if (msg.getSourceNodeId().toLowerCase().contains(prefix))
                key = _secretReservationKey.get(prefix);
        for (IMessageListener listener : _listener)
            listener.newMessage(msg, key);
    }

}
