package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.netty.NettyServer;
import de.uniluebeck.itm.tr.iwsn.netty.NettyServerFactory;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.net.InetSocketAddress;

import static com.google.common.base.Preconditions.checkState;

class PortalEventBusImpl extends AbstractService implements PortalEventBus {

	private final PortalConfig config;

	private final EventBus eventBus;

	private final NettyServerFactory nettyServerFactory;

	private final PortalChannelHandler portalChannelHandler;

	private NettyServer nettyServer;

	@Inject
	public PortalEventBusImpl(final PortalConfig config,
							  final EventBus eventBus,
							  final NettyServerFactory nettyServerFactory,
							  final PortalChannelHandler portalChannelHandler) {
		this.config = config;
		this.eventBus = eventBus;
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
		assertConnectedToPortal();
		throw new RuntimeException("IMPLEMENT ME!");
	}

	@Override
	protected void doStart() {
		try {
			final InetSocketAddress portalAddress = new InetSocketAddress(
					config.portalAddress.getHostText(),
					config.portalAddress.getPort()
			);
			nettyServer = nettyServerFactory.create(
					portalAddress,
					new ProtobufVarint32FrameDecoder(),
					new ProtobufDecoder(Message.getDefaultInstance()),
					new ProtobufVarint32LengthFieldPrepender(),
					new ProtobufEncoder(),
					portalChannelHandler
			);
			nettyServer.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			nettyServer.stopAndWait();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	private void assertConnectedToPortal() {
		checkState(isRunning(), "Not connected to portal server!");
	}
}
