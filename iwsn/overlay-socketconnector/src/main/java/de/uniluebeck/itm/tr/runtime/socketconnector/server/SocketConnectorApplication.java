/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck,                                                *
 * Institute of Operating Systems and Computer Networks Algorithms Group  University of Braunschweig                  *                          *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
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
package de.uniluebeck.itm.tr.runtime.socketconnector.server;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.application.TestbedApplication;
import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventAdapter;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventListener;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class SocketConnectorApplication implements TestbedApplication {

	private static final Logger log = LoggerFactory.getLogger(SocketConnectorApplication.class);

	private TestbedRuntime testbedRuntime;

	private SocketServer socketServer;

	private ScheduledExecutorService scheduler;

	public SocketConnectorApplication(TestbedRuntime testbedRuntime, int port) {
		this.testbedRuntime = testbedRuntime;
		this.socketServer = new SocketServer(this, port);
	}

	public String getName() {
		return "Socket-Connector";
	}

	private MessageEventListener messageEventListener = new MessageEventAdapter() {
		@Override
		public void messageReceived(Messages.Msg msg) {
			if (WSNApp.MSG_TYPE_LISTENER_MESSAGE.equals(msg.getMsgType())) {
				log.debug("Forwarding message from node {} to connected clients", msg.getFrom());
				socketServer.sendToClients(msg);
			}
		}
	};

	public void registerAsNodeOutputListener() {
		log.debug("SocketConnectorApplication.registerAsNodeOutputListener()");
		if (registerNodeMessageReceiverFuture == null || registerNodeMessageReceiverFuture.isCancelled()) {

			// periodically register at the node counterpart as listener to receive output from the nodes
			registerNodeMessageReceiverFuture = scheduler.scheduleWithFixedDelay(
					registerNodeMessageReceiverRunnable,
					0,
					30,
					TimeUnit.SECONDS
			);
		}
	}

	public void start() throws Exception {

		log.debug("SocketConnectorApplication.start()");

		scheduler = Executors.newScheduledThreadPool(
				1,
				new ThreadFactoryBuilder().setNameFormat("SocketConnector-Thread %d").build()
		);

		// start the server socket application
		socketServer.startUp();

		// register for incoming messages
		testbedRuntime.getMessageEventService().addListener(messageEventListener);

	}

	public void stop() throws Exception {

		log.debug("SocketConnectorApplication.stop()");

		// unregister for incoming messages
		testbedRuntime.getMessageEventService().removeListener(messageEventListener);

		// close server socket
		socketServer.shutdown();

	}

	public void unregisterAsNodeOutputListener() {
		log.debug("SocketConnectorApplication.unregisterAsNodeOutputListener()");
		registerNodeMessageReceiverFuture.cancel(true);
		scheduler.execute(unregisterNodeMessageReceiverRunnable);
	}

	private ScheduledFuture<?> registerNodeMessageReceiverFuture;

	private Runnable registerNodeMessageReceiverRunnable = new Runnable() {
		public void run() {
			registerNodeMessageReceiver(true);
		}
	};

	private Runnable unregisterNodeMessageReceiverRunnable = new Runnable() {
		@Override
		public void run() {
			registerNodeMessageReceiver(false);
		}
	};

	private void registerNodeMessageReceiver(boolean register) {

		ImmutableMap<String, String> map = testbedRuntime.getRoutingTableService().getEntries();

		WSNAppMessages.ListenerManagement management = WSNAppMessages.ListenerManagement.newBuilder()
				.setNodeName(testbedRuntime.getLocalNodeNameManager().getLocalNodeNames().iterator().next())
				.setOperation(
						register ? WSNAppMessages.ListenerManagement.Operation.REGISTER :
								WSNAppMessages.ListenerManagement.Operation.UNREGISTER
				).build();

		for (String destinationNodeName : map.keySet()) {

			testbedRuntime.getUnreliableMessagingService().sendAsync(
					testbedRuntime.getLocalNodeNameManager().getLocalNodeNames().iterator().next(), destinationNodeName,
					WSNApp.MSG_TYPE_LISTENER_MANAGEMENT, management.toByteArray(), 1,
					System.currentTimeMillis() + 5000
			);
		}

		// also register ourselves for all local node names
		for (String currentLocalNodeName : testbedRuntime.getLocalNodeNameManager().getLocalNodeNames()) {
			testbedRuntime.getUnreliableMessagingService().sendAsync(
					testbedRuntime.getLocalNodeNameManager().getLocalNodeNames().iterator().next(),
					currentLocalNodeName,
					WSNApp.MSG_TYPE_LISTENER_MANAGEMENT, management.toByteArray(), 1,
					System.currentTimeMillis() + 5000
			);
		}
	}

	public void sendToNode(Messages.Msg msg) {
		// rebuild message if source node name is different to this host so that nodes can reply to it
		ImmutableSet<String> localNodeNames = testbedRuntime.getLocalNodeNameManager().getLocalNodeNames();
		if (!localNodeNames.contains(msg.getFrom())) {
			String localNodeName = localNodeNames.iterator().next();
			log.debug("SocketConnectorApplication.sendToNode(): Rewriting source address \"{}\" to \"{}\"",
					msg.getFrom(), localNodeName
			);
			msg = Messages.Msg.newBuilder(msg).setFrom(localNodeName).build();
		}
		testbedRuntime.getUnreliableMessagingService().sendAsync(msg);
	}

}
