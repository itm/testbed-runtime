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

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import de.uniluebeck.itm.gtr.connection.ConnectionService;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventService;
import de.uniluebeck.itm.gtr.messaging.reliable.ReliableMessagingService;
import de.uniluebeck.itm.gtr.messaging.server.MessageServerService;
import de.uniluebeck.itm.gtr.messaging.srmr.SingleRequestMultiResponseService;
import de.uniluebeck.itm.gtr.messaging.unreliable.UnreliableMessagingService;
import de.uniluebeck.itm.gtr.naming.NamingService;
import de.uniluebeck.itm.gtr.routing.RoutingTableService;

import java.util.concurrent.ExecutorService;


@Singleton
class TestbedRuntimeImpl implements TestbedRuntime {

	@Inject
	private LocalNodeNameManager localNodeNameManager;

	@Inject
	private ConnectionService connectionService;

	@Inject
	private MessageEventService messageEventService;

	@Inject
	private MessageServerService messageServerService;

	@Inject
	private NamingService namingService;

	@Inject
	private ReliableMessagingService reliableMessagingService;

	@Inject
	private RoutingTableService routingTableService;

	@Inject
	private UnreliableMessagingService unreliableMessagingService;

	@Inject
	private SingleRequestMultiResponseService singleRequestMultiResponseService;

	@Inject
	@Named(TestbedRuntime.INJECT_ASYNC_EVENTBUS_EXECUTOR)
	private ExecutorService asyncEventBusExecutor;

	private EventBus eventBus;

	private AsyncEventBus asyncEventBus;

	@Override
	public void stop() {
		singleRequestMultiResponseService.stop();
		unreliableMessagingService.stop();
		routingTableService.stop();
		reliableMessagingService.stop();
		namingService.stop();
		messageServerService.stop();
		messageEventService.stop();
		connectionService.stop();
	}

	@Override
	public void start() throws Exception {
		connectionService.start();
		messageEventService.start();
		messageServerService.start();
		namingService.start();
		reliableMessagingService.start();
		routingTableService.start();
		unreliableMessagingService.start();
		singleRequestMultiResponseService.start();
	}

	@Override
	public EventBus getEventBus() {
		if (eventBus == null) {
			eventBus = new EventBus();
		}
		return eventBus;
	}

	@Override
	public AsyncEventBus getAsyncEventBus() {
		if (asyncEventBus == null) {
			asyncEventBus = new AsyncEventBus(asyncEventBusExecutor);
		}
		return asyncEventBus;
	}

	@Override
	public ReliableMessagingService getReliableMessagingService() {
		return reliableMessagingService;
	}

	@Override
	public UnreliableMessagingService getUnreliableMessagingService() {
		return unreliableMessagingService;
	}

	@Override
	public ConnectionService getConnectionService() {
		return connectionService;
	}

	@Override
	public RoutingTableService getRoutingTableService() {
		return routingTableService;
	}

	@Override
	public NamingService getNamingService() {
		return namingService;
	}

	@Override
	public MessageEventService getMessageEventService() {
		return messageEventService;
	}

	@Override
	public LocalNodeNameManager getLocalNodeNameManager() {
		return localNodeNameManager;
	}

	@Override
	public MessageServerService getMessageServerService() {
		return messageServerService;
	}

	@Override
	public SingleRequestMultiResponseService getSingleRequestMultiResponseService() {
		return singleRequestMultiResponseService;
	}
}
