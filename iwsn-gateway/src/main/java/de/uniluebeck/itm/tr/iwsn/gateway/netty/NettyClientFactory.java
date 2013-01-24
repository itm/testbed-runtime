package de.uniluebeck.itm.tr.iwsn.gateway.netty;

import org.jboss.netty.channel.ChannelHandler;

import java.net.SocketAddress;

public interface NettyClientFactory {

	NettyClient create(SocketAddress remoteAddress, ChannelHandler... handlers);

}
