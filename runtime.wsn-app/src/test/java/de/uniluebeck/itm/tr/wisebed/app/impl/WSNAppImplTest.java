///**********************************************************************************************************************
// * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
// * All rights reserved.                                                                                               *
// *                                                                                                                    *
// * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
// * following conditions are met:                                                                                      *
// *                                                                                                                    *
// * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
// *   disclaimer.                                                                                                      *
// * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
// *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
// * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
// *   products derived from this software without specific prior written permission.                                   *
// *                                                                                                                    *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
// * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
// * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
// * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
// * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
// * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
// **********************************************************************************************************************/
//
//package de.uniluebeck.itm.tr.wisebed.app.de.uniluebeck.itm.tr.rs.persistence.jpa.impl;
//
//import de.uniluebeck.itm.gtr.TestbedRuntime;
//import de.uniluebeck.itm.gtr.TestbedRuntimeStarter;
//import de.uniluebeck.itm.gtr.config.*;
//import de.uniluebeck.itm.gtr.connection.ConnectionOption;
//import de.uniluebeck.itm.gtr.messaging.MessageTools;
//import de.uniluebeck.itm.gtr.messaging.Messages;
//import de.uniluebeck.itm.gtr.messaging.event.MessageEventListener;
//import de.uniluebeck.itm.gtr.messaging.reliable.ReliableMessagingService;
//import de.uniluebeck.itm.gtr.messaging.unreliable.UnreliableMessagingService;
//import de.uniluebeck.itm.tr.wisebed.app.WSNApp;
//import de.uniluebeck.itm.tr.wisebed.app.WSNAppStarter;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//
//public class WSNAppImplTest {
//
//    private static final Logger log = LoggerFactory.getLogger(WSNAppImplTest.class);
//
//    private static final String NODE_NAME_1 = "n1";
//
//    private static final String NODE_NAME_2 = "n2";
//
//    private TestbedRuntime gw1;
//
//    private TestbedRuntime gw2;
//
//    private ReliableMessagingService rms1;
//
//    private ReliableMessagingService rms2;
//
//    private WSNApp gw1WsnApp;
//
//    private WSNApp gw2WsnApp;
//
//    @Before
//    public void setUp() throws Exception {
//
//        gw1 = TestbedRuntimeStarter.createTestbedRuntime(NODE_NAME_1);
//        gw2 = TestbedRuntimeStarter.createTestbedRuntime(NODE_NAME_2);
//
//        configureNetwork(gw1);
//        configureNetwork(gw2);
//
//        gw1.startServices();
//        gw2.startServices();
//
//        rms1 = gw1.getReliableMessagingService();
//        rms2 = gw2.getReliableMessagingService();
//
//        MessageEventListener messageEventListener = new MessageEventListener() {
//            @Override
//            public void messageSent(Messages.Msg msg) {
//                log.info("SENT: {} -> {}: {}", new Object[]{msg.getFrom(), msg.getTo(), MessageTools.getPayloadSerializable(msg)});
//            }
//
//            @Override
//            public void messageDropped(Messages.Msg msg) {
//                log.info("DROP: {} -> {}: {}", new Object[]{msg.getFrom(), msg.getTo(), MessageTools.getPayloadSerializable(msg)});
//            }
//
//            @Override
//            public void messageReceived(Messages.Msg msg) {
//                log.info("RECV: {} -> {}: {}", new Object[]{msg.getFrom(), msg.getTo(), MessageTools.getPayloadSerializable(msg)});
//            }
//        };
//
//        gw1.getMessageEventService().addListener(messageEventListener);
//        gw1.getMessageEventService().addListener(messageEventListener);
//
//        gw1WsnApp = WSNAppStarter.createWSNApp(NODE_NAME_1, null, gw1);
//        gw2WsnApp = WSNAppStarter.createWSNApp(NODE_NAME_2, null, gw2);
//
//    }
//
//    private void configureNetwork(TestbedRuntime testbedRuntime) {
//
//        XmlNode gw1 = new XmlNode(NODE_NAME_1);
//        XmlNode gw2 = new XmlNode(NODE_NAME_2);
//
//        XmlAddress gw1a0 = new IPAddress("localhost", 1110, gw1);
//        XmlAddress gw2a0 = new IPAddress("localhost", 2220, gw2);
//
//        testbedRuntime.getConfig().getNetwork()
//                .addNode(gw1, gw2)
//                .addLink(gw1a0, gw2a0, XmlLinkDirection.BIDIRECTIONAL, ConnectionOption.priority(1));
//
//        testbedRuntime.getConfig().add(
//                Option.create(UnreliableMessagingService.KEY_MAX_VALIDITY, "10000")
//        );
//
//    }
//
//    @After
//    public void tearDown() throws Exception {
//
//        gw1.stopServices();
//        gw2.stopServices();
//
//    }
//
//    @Test
//    public void testSend() throws Exception {
//    }
//
//    @Test
//    public void testAreNodesAlive() throws Exception {
//    }
//
//    @Test
//    public void testFlashPrograms() throws Exception {
//    }
//
//    @Test
//    public void testResetNodes() throws Exception {
//    }
//
//}
