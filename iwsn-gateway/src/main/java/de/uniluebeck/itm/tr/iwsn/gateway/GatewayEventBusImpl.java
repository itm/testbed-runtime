package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.gateway.netty.NettyClient;
import de.uniluebeck.itm.tr.iwsn.gateway.netty.NettyClientFactory;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.net.InetSocketAddress;

import static com.google.common.base.Preconditions.checkState;

class GatewayEventBusImpl extends AbstractService implements GatewayEventBus {

	private final GatewayConfig config;

	private final EventBus eventBus;

	private final NettyClientFactory nettyClientFactory;

	private final GatewayChannelHandler gatewayChannelHandler;

	private NettyClient nettyClient;

	@Inject
	GatewayEventBusImpl(final GatewayConfig config,
						final EventBus eventBus,
						final NettyClientFactory nettyClientFactory,
						final GatewayChannelHandler gatewayChannelHandler) {
		this.config = config;
		this.eventBus = eventBus;
		this.nettyClientFactory = nettyClientFactory;
		this.gatewayChannelHandler = gatewayChannelHandler;
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

			final InetSocketAddress portalAddress = new InetSocketAddress(
					config.portalOverlayAddress.getHostText(),
					config.portalOverlayAddress.getPort()
			);

			nettyClient = nettyClientFactory.create(
					portalAddress,
					new ProtobufVarint32FrameDecoder(),
					new ProtobufDecoder(Message.getDefaultInstance()),
					new ProtobufVarint32LengthFieldPrepender(),
					new ProtobufEncoder(),
					gatewayChannelHandler
			);
			nettyClient.startAndWait();

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	private void assertConnectedToPortal() {
		checkState(isRunning(), "Not connected to portal server!");
	}
}
