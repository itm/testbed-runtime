package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeResponse;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServer;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServerFactory;
import eu.wisebed.api.v3.common.NodeUrn;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.*;

class PortalEventBusImpl extends AbstractService implements PortalEventBus {

	private final Logger log = LoggerFactory.getLogger(PortalEventBusImpl.class);

	private final PortalConfig config;

	private final EventBus eventBus;

	private final NettyServerFactory nettyServerFactory;

	private final PortalChannelHandler portalChannelHandler;

	private NettyServer nettyServer;

	@Inject
	public PortalEventBusImpl(final PortalConfig config,
							  final EventBusFactory eventBusFactory,
							  final NettyServerFactory nettyServerFactory,
							  final PortalChannelHandler portalChannelHandler) {
		this.config = config;
		this.eventBus = eventBusFactory.create("PortalEventBus");
		this.nettyServerFactory = nettyServerFactory;
		this.portalChannelHandler = portalChannelHandler;
	}

	@Override
	public void register(final Object object) {
		eventBus.register(object);
	}

	@Override
	public void unregister(final Object object) {
		eventBus.unregister(object);
	}

	@Override
	public void post(final Object event) {
		eventBus.post(event);
	}

	@Override
	protected void doStart() {
		try {

			nettyServer = nettyServerFactory.create(
					new InetSocketAddress(config.overlayPort),
					new ProtobufVarint32FrameDecoder(),
					new ProtobufDecoder(Message.getDefaultInstance()),
					new ProtobufVarint32LengthFieldPrepender(),
					new ProtobufEncoder(),
					portalChannelHandler
			);
			nettyServer.startAndWait();

			eventBus.register(this);

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			nettyServer.stopAndWait();
			eventBus.unregister(this);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	/**
	 * Is called (besides others) for requests if no gateways are connected to the portal yet but the user already sends
	 * requests. In this case there's no channel been created yet and therefore no PortalChannelHandler which normally
	 * consumes requests.
	 *
	 * @param event
	 * 		the dead event
	 */
	@Subscribe
	public void onDeadEvent(DeadEvent event) {
		if (event.getEvent() instanceof Request) {
			Request request = (Request) event.getEvent();
			for (NodeUrn nodeUrn : getNodeUrns(request)) {
				final int status = getUnconnectedStatusCode(request);
				final SingleNodeResponse response = newSingleNodeResponse(
						request.hasReservationId() ? request.getReservationId() : null,
						request.getRequestId(),
						nodeUrn,
						status,
						"Node \"" + nodeUrn + "\" is not connected"
				);
				eventBus.post(response);
			}
		}
	}
}
