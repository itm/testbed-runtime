package de.uniluebeck.itm.tr.runtime.portalapp.protobuf;

import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import de.uniluebeck.itm.tr.runtime.portalapp.SessionManagementServiceImpl;


public class ProtobufControllerServerPipelineFactory implements ChannelPipelineFactory {

	private ProtobufControllerServer protobufControllerServer;

	private SessionManagementServiceImpl sessionManagement;

	public ProtobufControllerServerPipelineFactory(final ProtobufControllerServer protobufControllerServer,
												   final SessionManagementServiceImpl sessionManagement) {
		this.protobufControllerServer = protobufControllerServer;
		this.sessionManagement = sessionManagement;
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception {

		ChannelPipeline p = pipeline();

		p.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
		p.addLast("protobufDecoder", new ProtobufDecoder(WisebedProtocol.Envelope.getDefaultInstance()));

		p.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
		p.addLast("protobufEncoder", new ProtobufEncoder());

		ProtobufControllerServerHandler handler = new ProtobufControllerServerHandler(
				protobufControllerServer,
				sessionManagement
		);
		p.addLast("handler", handler);

		protobufControllerServer.addHandler(handler);

		return p;
	}
}
