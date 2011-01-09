package de.uniluebeck.itm.tr.protobufcontroller;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.util.concurrent.ScheduledExecutorService;

import static org.jboss.netty.channel.Channels.pipeline;


public class ProtobufControllerServerPipelineFactory implements ChannelPipelineFactory {

	private final ScheduledExecutorService executor;

	public ProtobufControllerServerPipelineFactory(final ScheduledExecutorService executor) {
		this.executor = executor;
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception {

		ChannelPipeline p = pipeline();

		p.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
		p.addLast("protobufDecoder", new ProtobufDecoder(WisebedProtocol.Envelope.getDefaultInstance()));

		p.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
		p.addLast("protobufEncoder", new ProtobufEncoder());

		p.addLast("handler", new ProtobufControllerServerHandler(executor));

		return p;
	}
}
