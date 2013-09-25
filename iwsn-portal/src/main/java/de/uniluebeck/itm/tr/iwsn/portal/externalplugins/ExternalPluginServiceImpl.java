package de.uniluebeck.itm.tr.iwsn.portal.externalplugins;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.messages.ExternalPluginMessage;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServer;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServerFactory;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.net.InetSocketAddress;

public class ExternalPluginServiceImpl extends AbstractService implements ExternalPluginService {

	private final ExternalPluginServiceConfig config;

	private final NettyServerFactory nettyServerFactory;

	private final ExternalPluginServiceChannelHandler channelHandler;

	private NettyServer nettyServer;

	@Inject
	public ExternalPluginServiceImpl(final ExternalPluginServiceConfig config,
									 final NettyServerFactory nettyServerFactory,
									 final ExternalPluginServiceChannelHandler channelHandler) {
		this.config = config;
		this.nettyServerFactory = nettyServerFactory;
		this.channelHandler = channelHandler;
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

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {

			if (nettyServer != null && nettyServer.isRunning()) {
				nettyServer.stopAndWait();
			}

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
