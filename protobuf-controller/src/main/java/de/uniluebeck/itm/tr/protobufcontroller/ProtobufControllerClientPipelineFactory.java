package de.uniluebeck.itm.tr.protobufcontroller;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import static org.jboss.netty.channel.Channels.pipeline;

public class ProtobufControllerClientPipelineFactory implements ChannelPipelineFactory {

	@Override
	public ChannelPipeline getPipeline() throws Exception {

		ChannelPipeline p = pipeline();

		p.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
		p.addLast("protobufDecoder", new ProtobufDecoder(WisebedProtocol.Envelope.getDefaultInstance()));

		p.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
		p.addLast("protobufEncoder", new ProtobufEncoder());

		p.addLast("handler", new ProtobufControllerClientHandler());

		return p;

	}
}
