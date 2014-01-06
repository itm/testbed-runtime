package de.uniluebeck.itm.tr.iwsn.portal.netty;

import org.jboss.netty.channel.ChannelPipelineFactory;

import java.net.SocketAddress;

public interface NettyServerFactory {

	NettyServer create(SocketAddress address, ChannelPipelineFactory pipelineFactory);

}
