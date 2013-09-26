package de.uniluebeck.itm.tr.iwsn.portal.externalplugins;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationStartedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServer;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServerFactory;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

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

				nettyServer = nettyServerFactory.create(
						new InetSocketAddress(config.getExternalPluginServicePort()),
						new ProtobufVarint32FrameDecoder(),
						new ProtobufDecoder(ExternalPluginMessage.getDefaultInstance()),
						new ProtobufVarint32LengthFieldPrepender(),
						new ProtobufEncoder(),
						channelHandler
				);
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
