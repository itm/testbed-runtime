package de.uniluebeck.itm.tr.runtime.socketconnector;

import de.uniluebeck.itm.gtr.messaging.Messages;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;

import static org.jboss.netty.channel.Channels.pipeline;

/**
 * Created by IntelliJ IDEA.
 * User: maxpagel
 * Date: 08.02.2010
 * Time: 16:04:51
 */
public class SocketServerPipelineFactory implements ChannelPipelineFactory {

    SocketServer server;

    public SocketServerPipelineFactory(SocketServer server) {
        this.server = server;
    }

    public ChannelPipeline getPipeline()
            throws Exception {
        ChannelPipeline p = pipeline();
        p.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
        p.addLast("protobufEnvelopeMessageDecoder", new ProtobufDecoder(Messages.Msg.getDefaultInstance()));


        p.addLast("frameEncoder", new LengthFieldPrepender(4));
        p.addLast("protobufEncoder", new ProtobufEncoder());

        p.addLast("handler", server);
        return p;
    }
}
