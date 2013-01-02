package de.uniluebeck.itm.tr.iwsn.portal.netty;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;

import java.net.SocketAddress;

public class NettyServer extends AbstractIdleService {

	private final ChannelGroup allChannels;

	private final SocketAddress address;

	private final ChannelFactory factory;

	private final ServerBootstrap bootstrap;

	private final Provider<ChannelHandler[]> handlers;

	@Inject
	NettyServer(final ChannelFactory factory,
				final ChannelGroup allChannels,
				@Assisted final SocketAddress address,
				@Assisted final Provider<ChannelHandler[]> handlers) {

		this.factory = factory;
		this.allChannels = allChannels;
		this.address = address;
		this.handlers = handlers;

		this.bootstrap = new ServerBootstrap(factory);
	}

	@Override
	protected void startUp() throws Exception {

		bootstrap.setPipelineFactory(pipelineFactory(handlers.get()));
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

	private ChannelPipelineFactory pipelineFactory(final ChannelHandler[] handlers) {
		return new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {

				return Channels.pipeline(handlers);
			}
		};
	}
}