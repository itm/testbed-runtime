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
import de.uniluebeck.itm.gtr.connection.ConnectionService;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventService;
import de.uniluebeck.itm.gtr.messaging.reliable.ReliableMessagingService;
import de.uniluebeck.itm.gtr.messaging.server.MessageServerService;
import de.uniluebeck.itm.gtr.messaging.srmr.SingleRequestMultiResponseService;
import de.uniluebeck.itm.gtr.messaging.unreliable.UnreliableMessagingService;
import de.uniluebeck.itm.gtr.naming.NamingService;
import de.uniluebeck.itm.gtr.routing.RoutingTableService;
import de.uniluebeck.itm.tr.util.Service;


public interface TestbedRuntime extends Service {

	String INJECT_RELIABLE_MESSAGING_SCHEDULER = "de.uniluebeck.itm.gtr.messaging.reliable.ReliableMessagingService/scheduler";

	String INJECT_MESSAGE_SERVER_SCHEDULER = "de.uniluebeck.itm.gtr.messaging.server.MessageServerService/scheduler";

	String INJECT_ASYNC_EVENTBUS_EXECUTOR = "de.uniluebeck.itm.gtr.TestbedRuntime/asyncEventBusExecutor";

	EventBus getEventBus();

	AsyncEventBus getAsyncEventBus();

	ReliableMessagingService getReliableMessagingService();

	UnreliableMessagingService getUnreliableMessagingService();

	ConnectionService getConnectionService();

	RoutingTableService getRoutingTableService();

	NamingService getNamingService();

	MessageEventService getMessageEventService();

	LocalNodeNameManager getLocalNodeNameManager();

	MessageServerService getMessageServerService();

	SingleRequestMultiResponseService getSingleRequestMultiResponseService();

}
