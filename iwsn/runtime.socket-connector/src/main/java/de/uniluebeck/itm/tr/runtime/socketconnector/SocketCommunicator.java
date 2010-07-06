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
package de.uniluebeck.itm.tr.runtime.socketconnector;

import com.google.common.collect.ImmutableMap;

import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventAdapter;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventListener;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import org.apache.log4j.Logger;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class SocketCommunicator {

    private static final Logger logger = Logger.getLogger(SocketCommunicator.class);

    private TestbedRuntime testbedRuntime;

    private SocketServer socketServer;

    public SocketCommunicator(TestbedRuntime testbedRuntime) {
        this.testbedRuntime = testbedRuntime;
        MessageEventListener messageEventListener = new MessageEventAdapter() {
            @Override
            public void messageReceived(Messages.Msg msg) {

                if (WSNApp.MSG_TYPE_LISTENER_MESSAGE.equals(msg.getMsgType())) {
                    logger.debug("received message from node with id " + msg.getFrom());
                    socketServer.handle(msg);
                }
            }
        };
        this.testbedRuntime.getMessageEventService().addListener(messageEventListener);
    }

    public void sendToRuntime(Messages.Msg msg) {
        testbedRuntime.getUnreliableMessagingService().sendAsync(msg);
    }

    private ScheduledFuture<?> hackyRegisterNodeMessageReceiverFuture;

    private Runnable hackyRegisterNodeMessageReceiverRunnable = new Runnable() {
        public void run() {

            ImmutableMap<String, String> map = testbedRuntime.getRoutingTableService().getEntries();

            WSNAppMessages.ListenerManagement management = WSNAppMessages.ListenerManagement.newBuilder()
                    .setNodeName(testbedRuntime.getLocalNodeNames().iterator().next())
                    .setOperation(WSNAppMessages.ListenerManagement.Operation.REGISTER).build();

            for (String destinationNodeName : map.keySet()) {

                testbedRuntime.getUnreliableMessagingService().sendAsync(
                        testbedRuntime.getLocalNodeNames().iterator().next(), destinationNodeName,
                        WSNApp.MSG_TYPE_LISTENER_MANAGEMENT, management.toByteArray(), 1,
                        System.currentTimeMillis() + 5000);
            }

            // also register ourselves for all local node names
            for (String currentLocalNodeName : testbedRuntime.getLocalNodeNames()) {
                testbedRuntime.getUnreliableMessagingService().sendAsync(
                        testbedRuntime.getLocalNodeNames().iterator().next(), currentLocalNodeName,
                        WSNApp.MSG_TYPE_LISTENER_MANAGEMENT, management.toByteArray(), 1,
                        System.currentTimeMillis() + 5000);
            }
        }
    };

    public void start() {
        // periodically register at the node counterpart as listener to receive output from the nodes
        hackyRegisterNodeMessageReceiverFuture = testbedRuntime.getSchedulerService()
                .scheduleWithFixedDelay(hackyRegisterNodeMessageReceiverRunnable, 0, 60, TimeUnit.SECONDS);
    }

    public void stop() {
        hackyRegisterNodeMessageReceiverFuture.cancel(true);
    }

    public void setServer(SocketServer socketServer) {
        this.socketServer = socketServer;
    }
}