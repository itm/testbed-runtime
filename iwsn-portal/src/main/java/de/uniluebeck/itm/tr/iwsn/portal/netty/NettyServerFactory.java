package de.uniluebeck.itm.tr.iwsn.portal.netty;

import org.jboss.netty.channel.ChannelHandler;

import java.net.SocketAddress;

public interface NettyServerFactory {

	NettyServer create(SocketAddress address, ChannelHandler... handlers);

}
