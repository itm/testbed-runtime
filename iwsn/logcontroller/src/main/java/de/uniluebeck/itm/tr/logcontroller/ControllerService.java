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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import eu.wisebed.testbed.api.wsn.v22.SessionManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.Endpoint;
import java.util.Iterator;
import java.util.Map;

/**
 * Controller for Proxy-Service
 */
public class ControllerService {
    private Logger _log = LoggerFactory.getLogger(ControllerService.class);
    private ImmutableMap<String, String> _properties;
    private Endpoint _sessionManagementEndpoint;
    private ImmutableSet<IMessageListener> _listenerSet;
    private Endpoint _messagestoreEndpoint;

    public ControllerService() {
    }

    /**
     * Preconfiguration of the Controller
     *
     * @param properties
     */
    public void init(Map properties)
            throws Exception {
        Preconditions.checkArgument(properties != null, "Properties-Map is null!");
        _properties = ImmutableMap.copyOf(properties);
        _log.debug("Configuration:");
        for (Map.Entry entry : _properties.entrySet())
            _log.debug("{} : {}", entry.getKey(), entry.getValue());

        Preconditions.checkNotNull(_properties.get(Server.MESSAGELISTENER), "No Listenerclass specified!");
        IMessageListener listener =
                (IMessageListener) Class.forName(_properties.get(Server.MESSAGELISTENER))
                        .newInstance();
        listener.init(_properties);
        _log.debug("Messagelistener {} initialized", listener.getClass().getSimpleName());
        _listenerSet = ImmutableSet.of(listener);
    }

    /**
     * Starts all Services
     */
    public void startup()
            throws Exception {
        Preconditions.checkNotNull(_properties.get(Server.SESSIONMANAGEMENT_ENDPOINT),
                "Property '" + Server.SESSIONMANAGEMENT_ENDPOINT + "' not specified!");
        SessionManagement sessionManagment =
                WSNServiceHelper.getSessionManagementService(
                        _properties.get(Server.SESSIONMANAGEMENT_ENDPOINT));
        Preconditions.checkNotNull(_properties.get(Server.SESSIONMANAGEMENT_PROXY),
                "Property '" + Server.SESSIONMANAGEMENT_PROXY + "' not specified!");
        _sessionManagementEndpoint = Endpoint.publish(_properties.get(Server.SESSIONMANAGEMENT_PROXY),
                new SessionManagementDelegate(sessionManagment, this));
        _log.info("SessionManagement-Service on {} published", _properties.get(Server.SESSIONMANAGEMENT_PROXY));
        Preconditions.checkNotNull(_properties.get(Server.MESSAGESTORE_ENDPOINT),
                "Property '"+Server.MESSAGESTORE_ENDPOINT+"' not specified!");
        _messagestoreEndpoint = Endpoint.publish(_properties.get(Server.MESSAGESTORE_ENDPOINT),
                new DBMessageStore(_properties));
        _log.info("Messagestore-Service on {} published", _properties.get(Server.MESSAGESTORE_ENDPOINT));
    }

    /**
     * Shuts down all Services
     *
     * @throws Throwable
     */
    public void shutdown()
            throws Exception {
        ((SessionManagementDelegate) _sessionManagementEndpoint.getImplementor()).dispose();
        _sessionManagementEndpoint.stop();
        ((DBMessageStore)_messagestoreEndpoint.getImplementor()).stop();
        _messagestoreEndpoint.stop();
        _log.info("{} successfully shut down", getClass().getSimpleName());
    }

    /**
     * Get Messagelisteners
     *
     * @return Iterator over all Messagelistener
     */
    public Iterator<IMessageListener> getListenerIterator() {
        return _listenerSet.iterator();
    }

    public String getControllerUrnPrefix() {
        Preconditions.checkNotNull(_properties.get(Server.CONTROLLER_PROXY_PREFIX),
                "Property '" + Server.CONTROLLER_PROXY_PREFIX + "' not specified!");
        return _properties.get(Server.CONTROLLER_PROXY_PREFIX);
    }


    public String getWsnUrnPrefix() {
        Preconditions.checkNotNull(_properties.get(Server.WSN_PROXY_PREFIX),
                "Property '" + Server.WSN_PROXY_PREFIX + "' not specified!");
        return _properties.get(Server.WSN_PROXY_PREFIX);
    }

    public String getReservationEndpoint() {
        Preconditions.checkNotNull(_properties.get(Server.RESERVATION_ENDPOINT),
                "Property '" + Server.RESERVATION_ENDPOINT + "' not specified!");
        return _properties.get(Server.RESERVATION_ENDPOINT);
    }
}
