package de.uniluebeck.itm.tr.iwsn.gateway.netty;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public class NettyClient extends AbstractService {

	private static final Logger log = LoggerFactory.getLogger(NettyClient.class);

	private final ChannelFactory factory;

	private final ChannelGroup allChannels;

	private final SocketAddress remoteAddress;

	private final ChannelHandler[] handlers;

	private final ClientBootstrap bootstrap;

	@Inject
	public NettyClient(final ChannelFactory factory,
					   final ChannelGroup allChannels,
					   @Assisted final SocketAddress remoteAddress,
					   @Assisted final Provider<ChannelHandler[]> handlers) {

		this.factory = factory;
		this.allChannels = allChannels;
		this.remoteAddress = remoteAddress;
		this.handlers = handlers.get();

		this.bootstrap = new ClientBootstrap(factory);
	}

	@Override
	protected void doStart() {

		log.trace("NettyClient.startUp(remoteAddress={}, handlers={})", remoteAddress, handlers);

		try {

			bootstrap.setPipelineFactory(pipelineFactory(handlers));
			bootstrap.setOption("child.tcpNoDelay", true);
			bootstrap.setOption("child.keepAlive", true);

			final ChannelFuture future = bootstrap.connect(remoteAddress);
			future.await(30, TimeUnit.SECONDS);

			if (future.isSuccess()) {
				allChannels.add(future.getChannel());
				notifyStarted();
			} else {
				notifyFailed(future.getCause());
			}

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		log.trace("NettyClient.shutDown(remoteAddress={}, handlers={})", remoteAddress, handlers);

		try {

			allChannels.close().awaitUninterruptibly();
			factory.releaseExternalResources();
			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	private ChannelPipelineFactory pipelineFactory(final ChannelHandler[] handlers) {
		return new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(handlers);
			}
		};
	}
}
