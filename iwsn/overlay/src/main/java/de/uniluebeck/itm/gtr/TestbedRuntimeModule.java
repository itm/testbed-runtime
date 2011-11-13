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

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import de.uniluebeck.itm.gtr.connection.ConnectionModule;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventModule;
import de.uniluebeck.itm.gtr.messaging.reliable.ReliableMessagingModule;
import de.uniluebeck.itm.gtr.messaging.server.MessageServerServiceModule;
import de.uniluebeck.itm.gtr.messaging.srmr.SingleRequestMultiResponseModule;
import de.uniluebeck.itm.gtr.messaging.unreliable.UnreliableMessagingModule;
import de.uniluebeck.itm.gtr.naming.NamingModule;
import de.uniluebeck.itm.gtr.routing.RoutingModule;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;


public class TestbedRuntimeModule extends AbstractModule {

	private final ExecutorService asyncEventBusExecutor;

	private final ScheduledExecutorService messageServerServiceScheduler;

	private final ScheduledExecutorService reliableMessagingServiceScheduler;

	public TestbedRuntimeModule(final ExecutorService asyncEventBusExecutor,
								final ScheduledExecutorService messageServerServiceScheduler,
								final ScheduledExecutorService reliableMessagingServiceScheduler) {

		this.asyncEventBusExecutor = asyncEventBusExecutor;
		this.messageServerServiceScheduler = messageServerServiceScheduler;
		this.reliableMessagingServiceScheduler = reliableMessagingServiceScheduler;
	}

	@Override
	protected void configure() {

		bind(ExecutorService.class)
				.annotatedWith(Names.named(TestbedRuntime.INJECT_ASYNC_EVENTBUS_EXECUTOR))
				.toInstance(asyncEventBusExecutor);

		bind(ScheduledExecutorService.class)
				.annotatedWith(Names.named(TestbedRuntime.INJECT_MESSAGE_SERVER_SCHEDULER))
				.toInstance(messageServerServiceScheduler);

		bind(ScheduledExecutorService.class)
				.annotatedWith(Names.named(TestbedRuntime.INJECT_RELIABLE_MESSAGING_SCHEDULER))
				.toInstance(reliableMessagingServiceScheduler);

		install(new ConnectionModule());
		install(new MessageEventModule());
		install(new MessageServerServiceModule());
		install(new NamingModule());
		install(new ReliableMessagingModule());
		install(new RoutingModule());
		install(new UnreliableMessagingModule());
		install(new SingleRequestMultiResponseModule());
		install(new LocalNodeNameManagerModule());

		bind(TestbedRuntime.class).to(TestbedRuntimeImpl.class);
	}

}
