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


import de.uniluebeck.itm.gtr.messaging.Messages;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;


@ChannelPipelineCoverage("all")
public class SocketServer extends SimpleChannelUpstreamHandler {

	private static final Logger log = LoggerFactory.getLogger(SocketServer.class);

	private final ChannelGroup allChannels = new DefaultChannelGroup("SensorMessage-Server");

	private ChannelFactory channelFactory;

	private SocketConnectorApplication socketConnectorApplication;

	private int port;

	public SocketServer(final SocketConnectorApplication socketConnectorApplication, final int port) {
		this.socketConnectorApplication = socketConnectorApplication;
		this.port = port;
	}

	public void startUp() {

		// set up server socket
		channelFactory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(), Executors.newCachedThreadPool()
		);
		ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);

		// Set up the event pipeline channelFactory.
		bootstrap.setPipelineFactory(new SocketServerPipelineFactory(this));

		// Bind and startUp to accept incoming connections.
		allChannels.add(bootstrap.bind(new InetSocketAddress(port)));
	}

	public void shutdown() {
		ChannelGroupFuture future = allChannels.close();
		future.awaitUninterruptibly();
		channelFactory.releaseExternalResources();
	}

	public void sendToClients(Messages.Msg message) {
		allChannels.write(message);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		log.debug("SocketServer.messageReceived({}, {})", ctx, e);
		if (e.getMessage() instanceof Messages.Msg) {
			Messages.Msg msg = (Messages.Msg) e.getMessage();
			socketConnectorApplication.sendToNode(msg);
		} else {
			log.debug("Socket server received unknown msg");
		}
		ctx.sendUpstream(e);
	}

	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
		log.debug("SocketServer.channelOpen({}, {})", ctx, e);
		allChannels.add(e.getChannel());
		socketConnectorApplication.registerAsNodeOutputListener();
		ctx.sendUpstream(e);
	}

	@Override
	public void channelClosed(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		log.debug("SocketServer.channelClosed({}, {})", ctx, e);
		if (allChannels.size() == 1) {
			socketConnectorApplication.unregisterAsNodeOutputListener();
		}
		ctx.sendUpstream(e);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		log.error("Caught Exception during socket communication!", e);
		ctx.sendUpstream(e);
	}

}
