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
import java.net.SocketAddress;
import java.util.concurrent.Executors;

/**
 * Created by IntelliJ IDEA.
 * User: maxpagel
 * Date: 08.02.2010
 * Time: 16:01:41
 */
@ChannelPipelineCoverage("all")
public class SocketServer extends SimpleChannelUpstreamHandler {

    private static final Logger logger = LoggerFactory.getLogger(SocketServer.class);
    static final ChannelGroup allChannels = new DefaultChannelGroup("sensorMessage-server");
    private ChannelFactory factory;
    private SocketCommunicator communicator;
    private int port;
    ChannelLocal<String> channelUser;

    public SocketServer(SocketCommunicator communicator,int port) {
        this.communicator = communicator;
        this.port = port;
        channelUser = new ChannelLocal<String>();
    }

    public void startUp() {
        factory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        ServerBootstrap bootstrap = new ServerBootstrap(factory);

        // Set up the event pipeline factory.
        bootstrap.setPipelineFactory(new SocketServerPipelineFactory(this));

        // Bind and startUp to accept incoming connections.
        org.jboss.netty.channel.Channel channel = bootstrap.bind(new InetSocketAddress(port));
        allChannels.add(channel);
    }

    public void shutdown() {
        ChannelGroupFuture future = allChannels.close();
        future.awaitUninterruptibly();
        factory.releaseExternalResources();
    }



    public void handle(Messages.Msg message) {
        SocketServer.allChannels.write(message);
    }



    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        if(e.getMessage() instanceof Messages.Msg){
            Messages.Msg msg = (Messages.Msg) e.getMessage();
            communicator.sendToRuntime(msg);
        }else{
            logger.debug("Socket server received unknown msg");
        }

    }


    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
        SocketServer.allChannels.add(e.getChannel());
    }

    public static String getHostString(Channel channel) {
        SocketAddress remoteAddress = channel.getRemoteAddress();
        if (remoteAddress == null) return null;
        String address = remoteAddress.toString();
        if ((remoteAddress instanceof InetSocketAddress)) address =
                ((InetSocketAddress) remoteAddress).getHostName() + ":" + ((InetSocketAddress) remoteAddress).getPort();
        if (address.charAt(0) == '/') address = address.substring(1);

        return address;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        logger.error("Caught Exception during socket communication ",e.getCause());
    }
}
