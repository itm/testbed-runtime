package de.uniluebeck.itm.wisebed.cmdlineclient.protobuf;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

public class ProtobufControllerClientPipelineFactory implements ChannelPipelineFactory {

	private final ProtobufControllerClient protobufControllerClient;

	public ProtobufControllerClientPipelineFactory(ProtobufControllerClient protobufControllerClient) {
		this.protobufControllerClient = protobufControllerClient;
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception {

		ChannelPipeline p = Channels.pipeline();

		p.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
		p.addLast("protobufDecoder", new ProtobufDecoder(WisebedProtocol.Envelope.getDefaultInstance()));

		p.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
		p.addLast("protobufEncoder", new ProtobufEncoder());

		p.addLast("handler", new ProtobufControllerClientHandler(protobufControllerClient));

		return p;

	}
}
