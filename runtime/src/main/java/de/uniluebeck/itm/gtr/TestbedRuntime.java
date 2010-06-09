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
import com.google.inject.Guice;
import de.uniluebeck.itm.gtr.common.CommonModule;
import de.uniluebeck.itm.gtr.common.SchedulerService;
import de.uniluebeck.itm.gtr.connection.ConnectionModule;
import de.uniluebeck.itm.gtr.connection.ConnectionService;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventModule;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventService;
import de.uniluebeck.itm.gtr.messaging.reliable.ReliableMessagingModule;
import de.uniluebeck.itm.gtr.messaging.reliable.ReliableMessagingService;
import de.uniluebeck.itm.gtr.messaging.server.MessageServerModule;
import de.uniluebeck.itm.gtr.messaging.server.MessageServerService;
import de.uniluebeck.itm.gtr.messaging.srmr.SingleRequestMultiResponseModule;
import de.uniluebeck.itm.gtr.messaging.srmr.SingleRequestMultiResponseService;
import de.uniluebeck.itm.gtr.messaging.unreliable.UnreliableMessagingModule;
import de.uniluebeck.itm.gtr.messaging.unreliable.UnreliableMessagingService;
import de.uniluebeck.itm.gtr.naming.NamingModule;
import de.uniluebeck.itm.gtr.naming.NamingService;
import de.uniluebeck.itm.gtr.routing.RoutingModule;
import de.uniluebeck.itm.gtr.routing.RoutingTableService;


public interface TestbedRuntime {

	public static class Factory {

		public static TestbedRuntime create(String... localNodeNames) {

			Class[] services = new Class[]{
					SchedulerService.class,
					ConnectionService.class,
					MessageEventService.class,
					MessageServerService.class,
					NamingService.class,
					ReliableMessagingService.class,
					RoutingTableService.class,
					UnreliableMessagingService.class,
					SingleRequestMultiResponseService.class
			};

			return Guice.createInjector(
					new CommonModule(),
					new ConnectionModule(),
					new MessageEventModule(),
					new MessageServerModule(),
					new NamingModule(),
					new ReliableMessagingModule(),
					new RoutingModule(),
					new UnreliableMessagingModule(),
					new SingleRequestMultiResponseModule(),
					new TestbedRuntimeModule(localNodeNames, services)
			).getInstance(TestbedRuntime.class);

		}
	}

	void shutdown();

	void startServices() throws Exception;

	void stopServices();

	ReliableMessagingService getReliableMessagingService();

	UnreliableMessagingService getUnreliableMessagingService();

	ConnectionService getConnectionService();

	RoutingTableService getRoutingTableService();

	NamingService getNamingService();

	MessageEventService getMessageEventService();

	ImmutableSet<String> getLocalNodeNames();

	MessageServerService getMessageServerService();

	SingleRequestMultiResponseService getSingleRequestMultiResponseService();

	SchedulerService getSchedulerService();

}
