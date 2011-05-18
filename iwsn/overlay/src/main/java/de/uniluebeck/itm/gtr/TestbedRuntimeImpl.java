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

package de.uniluebeck.itm.gtr;

import com.google.common.collect.ImmutableSet;
import com.google.inject.ConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.uniluebeck.itm.gtr.common.SchedulerService;
import de.uniluebeck.itm.tr.util.Service;
import de.uniluebeck.itm.gtr.connection.ConnectionService;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventService;
import de.uniluebeck.itm.gtr.messaging.reliable.ReliableMessagingService;
import de.uniluebeck.itm.gtr.messaging.server.MessageServerService;
import de.uniluebeck.itm.gtr.messaging.srmr.SingleRequestMultiResponseService;
import de.uniluebeck.itm.gtr.messaging.unreliable.UnreliableMessagingService;
import de.uniluebeck.itm.gtr.naming.NamingService;
import de.uniluebeck.itm.gtr.routing.RoutingTableService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@Singleton
class TestbedRuntimeImpl implements TestbedRuntime {

	private Class[] services;

	private String[] localNodeNames;

	private Injector injector;

	@Inject
	public TestbedRuntimeImpl(
			@TestbedRuntimeServices Class[] services,
			@LocalNodeNames String[] localNodeNames,
			Injector injector) {

		this.services = services;
		this.localNodeNames = localNodeNames;
		this.injector = injector;
	}

	@Override
	public void shutdown() {
		stopServices();
	}

	@Override
	public void startServices() throws Exception {
		for (Class<?> clazz : services) {
			((Service) injector.getInstance(clazz)).start();
		}
	}

	@Override
	public void stopServices() {
		List<Class> list = Arrays.asList(services);
		Collections.reverse(list);
		for (Class<?> clazz : list) {
			((Service) injector.getInstance(clazz)).stop();
		}
	}

	@Override
	public ReliableMessagingService getReliableMessagingService() {
		try {
			return injector.getInstance(ReliableMessagingService.class);
		} catch (ConfigurationException e) {
			throw new IllegalArgumentException("ReliableMessagingService was not found. " +
					"Please assure that TestbedRuntime was initialized correctly!"
			);
		}
	}

	@Override
	public UnreliableMessagingService getUnreliableMessagingService() {
		try {
			return injector.getInstance(UnreliableMessagingService.class);
		} catch (ConfigurationException e) {
			throw new IllegalArgumentException("UnreliableMessagingService was not found. " +
					"Please assure that TestbedRuntime was initialized correctly!"
			);
		}
	}

	@Override
	public ConnectionService getConnectionService() {
		try {
			return injector.getInstance(ConnectionService.class);
		} catch (ConfigurationException e) {
			throw new IllegalArgumentException("ConnectionService was not found. " +
					"Please assure that TestbedRuntime was initialized correctly!"
			);
		}
	}

	@Override
	public RoutingTableService getRoutingTableService() {
		try {
			return injector.getInstance(RoutingTableService.class);
		} catch (ConfigurationException e) {
			throw new IllegalArgumentException("ConnectionService was not found. " +
					"Please assure that TestbedRuntime was initialized correctly!"
			);
		}
	}

	@Override
	public NamingService getNamingService() {
		try {
			return injector.getInstance(NamingService.class);
		} catch (ConfigurationException e) {
			throw new IllegalArgumentException("ConnectionService was not found. " +
					"Please assure that TestbedRuntime was initialized correctly!"
			);
		}
	}

	@Override
	public MessageEventService getMessageEventService() {
		try {
			return injector.getInstance(MessageEventService.class);
		} catch (ConfigurationException e) {
			throw new IllegalArgumentException("MessageEventService was not found. " +
					"Please assure that TestbedRuntime was initialized correctly!"
			);
		}
	}

	@Override
	public ImmutableSet<String> getLocalNodeNames() {
		return ImmutableSet.of(localNodeNames);
	}

	@Override
	public MessageServerService getMessageServerService() {
		try {
			return injector.getInstance(MessageServerService.class);
		} catch (ConfigurationException e) {
			throw new IllegalArgumentException("MessageServerService was not found. " +
					"Please assure that TestbedRuntime was initialized correctly!"
			);
		}
	}

	@Override
	public SingleRequestMultiResponseService getSingleRequestMultiResponseService() {
		try {
			return injector.getInstance(SingleRequestMultiResponseService.class);
		} catch (ConfigurationException e) {
			throw new IllegalArgumentException("SingleRequestMultiResponseService was not found. " +
					"Please assure that TestbedRuntime was initialized correctly!"
			);
		}
	}

	@Override
	public SchedulerService getSchedulerService() {
		try {
			return injector.getInstance(SchedulerService.class);
		} catch (ConfigurationException e) {
			throw new IllegalArgumentException("SchedulerService was not found. " +
					"Please assure that TestbedRuntime was initialized correctly!"
			);
		}
	}

}
