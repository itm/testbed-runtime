package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.HttpServerCodec;

import java.net.SocketAddress;

class NettyServer extends AbstractIdleService {

	private final ChannelGroup allChannels;

	private final SocketAddress address;

	private final ChannelFactory factory;

	private final ServerBootstrap bootstrap;

	private final Provider<NettyServerHandler> handler;

	@Inject
	NettyServer(ChannelFactory factory, ChannelGroup allChannels, SocketAddress address,
				Provider<NettyServerHandler> handler) {
		this.factory = factory;
		this.bootstrap = new ServerBootstrap(factory);
		this.allChannels = allChannels;
		this.address = address;
		this.handler = handler;
	}

	@Override
	protected void startUp() throws Exception {
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(new HttpServerCodec(), handler.get());
			}
		}
		);
		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.keepAlive", true);
		Channel channel = bootstrap.bind(address);
		allChannels.add(channel);
	}

	@Override
	protected void shutDown() throws Exception {
		allChannels.close().awaitUninterruptibly();
		factory.releaseExternalResources();
	}
}
