package de.uniluebeck.itm.tr.iwsn.gateway.netty;

import org.jboss.netty.channel.ChannelPipelineFactory;

import java.net.SocketAddress;

public interface NettyClientFactory {

	NettyClient create(SocketAddress remoteAddress, ChannelPipelineFactory pipelineFactory);

}
