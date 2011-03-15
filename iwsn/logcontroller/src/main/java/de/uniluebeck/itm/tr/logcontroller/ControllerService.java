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
 * Controller proxy
 */
public class ControllerService {

	private static final Logger log = LoggerFactory.getLogger(ControllerService.class);

	private ImmutableMap<String, String> properties;

	private Endpoint sessionManagementEndpoint;

	private ImmutableSet<IMessageListener> listenerSet;

	private Endpoint messageStoreEndpoint;

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
		this.properties = ImmutableMap.copyOf(properties);
		log.debug("Configuration:");
		for (Map.Entry entry : this.properties.entrySet()) {
			log.debug("{} : {}", entry.getKey(), entry.getValue());
		}

		Preconditions.checkNotNull(this.properties.get(Server.MESSAGELISTENER), "No Listenerclass specified!");
		IMessageListener listener =
				(IMessageListener) Class.forName(this.properties.get(Server.MESSAGELISTENER))
						.newInstance();
		listener.init(this.properties);
		log.debug("Messagelistener {} initialized", listener.getClass().getSimpleName());
		listenerSet = ImmutableSet.of(listener);
	}

	/**
	 * Starts all Services
	 */
	public void startup()
			throws Exception {
		Preconditions.checkNotNull(properties.get(Server.SESSIONMANAGEMENT_ENDPOINT),
				"Property '" + Server.SESSIONMANAGEMENT_ENDPOINT + "' not specified!"
		);
		SessionManagement sessionManagement =
				WSNServiceHelper.getSessionManagementService(
						properties.get(Server.SESSIONMANAGEMENT_ENDPOINT)
				);
		Preconditions.checkNotNull(properties.get(Server.SESSIONMANAGEMENT_PROXY),
				"Property '" + Server.SESSIONMANAGEMENT_PROXY + "' not specified!"
		);
		sessionManagementEndpoint = Endpoint.publish(properties.get(Server.SESSIONMANAGEMENT_PROXY),
				new SessionManagementDelegate(sessionManagement, this)
		);
		log.info("SessionManagement-Service on {} published", properties.get(Server.SESSIONMANAGEMENT_PROXY));
		Preconditions.checkNotNull(properties.get(Server.MESSAGESTORE_ENDPOINT),
				"Property '" + Server.MESSAGESTORE_ENDPOINT + "' not specified!"
		);
		messageStoreEndpoint = Endpoint.publish(properties.get(Server.MESSAGESTORE_ENDPOINT),
				new DBMessageStore(properties)
		);
		log.info("Messagestore-Service on {} published", properties.get(Server.MESSAGESTORE_ENDPOINT));
	}

	/**
	 * Shuts down all Services
	 *
	 * @throws Throwable
	 */
	public void shutdown()
			throws Exception {
		((SessionManagementDelegate) sessionManagementEndpoint.getImplementor()).dispose();
		sessionManagementEndpoint.stop();
		((DBMessageStore) messageStoreEndpoint.getImplementor()).stop();
		messageStoreEndpoint.stop();
		log.info("{} successfully shut down", getClass().getSimpleName());
	}

	/**
	 * Get Messagelisteners
	 *
	 * @return Iterator over all Messagelistener
	 */
	public Iterator<IMessageListener> getListenerIterator() {
		return listenerSet.iterator();
	}

	public String getControllerUrnPrefix() {
		Preconditions.checkNotNull(properties.get(Server.CONTROLLER_PROXY_PREFIX),
				"Property '" + Server.CONTROLLER_PROXY_PREFIX + "' not specified!"
		);
		return properties.get(Server.CONTROLLER_PROXY_PREFIX);
	}


	public String getWsnUrnPrefix() {
		Preconditions.checkNotNull(properties.get(Server.WSN_PROXY_PREFIX),
				"Property '" + Server.WSN_PROXY_PREFIX + "' not specified!"
		);
		return properties.get(Server.WSN_PROXY_PREFIX);
	}

	public String getReservationEndpoint() {
		Preconditions.checkNotNull(properties.get(Server.RESERVATION_ENDPOINT),
				"Property '" + Server.RESERVATION_ENDPOINT + "' not specified!"
		);
		return properties.get(Server.RESERVATION_ENDPOINT);
	}
}
