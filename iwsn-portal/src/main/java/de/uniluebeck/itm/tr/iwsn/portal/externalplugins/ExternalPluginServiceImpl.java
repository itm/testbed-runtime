package de.uniluebeck.itm.tr.iwsn.portal.externalplugins;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.common.netty.ExceptionChannelHandler;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.events.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.events.ReservationStartedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServer;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServerFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;

import java.net.InetSocketAddress;

class ExternalPluginServiceImpl extends AbstractService implements ExternalPluginService {

	private final ExternalPluginServiceConfig config;

	private final NettyServerFactory nettyServerFactory;

	private final ExternalPluginServiceChannelHandler channelHandler;

	private final PortalEventBus portalEventBus;

	private NettyServer nettyServer;

	@Inject
	public ExternalPluginServiceImpl(final ExternalPluginServiceConfig config,
									 final NettyServerFactory nettyServerFactory,
									 final ExternalPluginServiceChannelHandler channelHandler,
									 final PortalEventBus portalEventBus) {
		this.config = config;
		this.nettyServerFactory = nettyServerFactory;
		this.channelHandler = channelHandler;
		this.portalEventBus = portalEventBus;
	}

	@Override
	protected void doStart() {
		try {

			if (config.getExternalPluginServicePort() != null) {

				final ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory() {
					@Override
					public ChannelPipeline getPipeline() throws Exception {
						return Channels.pipeline(
								new ExceptionChannelHandler(),
								new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
								new ProtobufDecoder(ExternalPluginMessage.getDefaultInstance()),
								new LengthFieldPrepender(4, false),
								new ProtobufEncoder(),
								channelHandler
						);
					}
				};

				final InetSocketAddress address = new InetSocketAddress(config.getExternalPluginServicePort());

				nettyServer = nettyServerFactory.create(address, pipelineFactory);
				nettyServer.startAndWait();
			}

			portalEventBus.register(this);

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {

			portalEventBus.unregister(this);

			if (nettyServer != null && nettyServer.isRunning()) {
				nettyServer.stopAndWait();
			}

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public void onRequest(final Request request) {
		channelHandler.onRequest(request);
	}

	@Override
	public void onSingleNodeProgress(final SingleNodeProgress progress) {
		channelHandler.onSingleNodeProgress(progress);
	}

	@Override
	public void onSingleNodeResponse(final SingleNodeResponse response) {
		channelHandler.onSingleNodeResponse(response);
	}

	@Override
	public void onGetChannelPipelinesResponse(final GetChannelPipelinesResponse getChannelPipelinesResponse) {
		channelHandler.onGetChannelPipelinesResponse(getChannelPipelinesResponse);
	}

	@Override
	public void onEvent(final Event event) {
		channelHandler.onEvent(event);
	}

	@Override
	public void onEventAck(final EventAck eventAck) {
		channelHandler.onEventAck(eventAck);
	}

	@Subscribe
	public void onReservationStartedEvent(final ReservationStartedEvent event) {
		channelHandler.onReservationStartedEvent(event);
	}

	@Subscribe
	public void onReservationEndedEvent(final ReservationEndedEvent event) {
		channelHandler.onReservationEndedEvent(event);
	}
}
