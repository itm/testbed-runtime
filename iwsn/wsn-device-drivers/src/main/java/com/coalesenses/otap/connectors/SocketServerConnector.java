package com.coalesenses.otap.connectors;

import com.coalesenses.seraerial.SerAerialPacket;
import com.coalesenses.seraerial.SerialRoutingPacket;
import com.google.protobuf.ByteString;
import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;
import de.uniluebeck.itm.wsn.devicedrivers.generic.PacketTypes;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import static org.jboss.netty.channel.Channels.pipeline;

/**
 * Created by IntelliJ IDEA.
 * User: maxpagel
 * Date: 19.11.2010
 * Time: 13:36:25
 * To change this template use File | Settings | File Templates.
 */
public class SocketServerConnector extends DeviceConnector {

    private static Logger log = LoggerFactory.getLogger(SocketServerConnector.class);

    private String host;

    private int port;

    private SimpleChannelUpstreamHandler upstreamHandler = new SimpleChannelUpstreamHandler() {
        @Override
        public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e)
                throws Exception {

            Messages.Msg message = (Messages.Msg) e.getMessage();

            if (WSNApp.MSG_TYPE_LISTENER_MESSAGE.equals(message.getMsgType())) {

                WSNAppMessages.Message wsnAppMessage = WSNAppMessages.Message.parseFrom(message.getPayload());
                if (wsnAppMessage.hasTextMessage()) {

                    WSNAppMessages.Message.TextMessage textMessage = wsnAppMessage.getTextMessage();
                    WSNAppMessages.Message.MessageLevel messageLevel = textMessage.getMessageLevel();
                    String msg = textMessage.getMsg();
                    log.info(
                            "Received sensor node text output with messageLevel=\"{}\" and msg=\"{}\"", messageLevel,
                            msg);

                } else if (wsnAppMessage.hasBinaryMessage()) {

                    
                    WSNAppMessages.Message.BinaryMessage binaryMessage = wsnAppMessage.getBinaryMessage();
                    
                    byte binaryType = (byte) binaryMessage.getBinaryType();
                    byte[] binaryData = binaryMessage.getBinaryData().toByteArray();

                    log.info(
                            "Received sensor node binary output with binaryType=\"{}\" and binaryData=\"{}\"",
                            StringUtils.toHexString(binaryType), StringUtils.toHexString(binaryData));
                }

            } else {
                log.info("Received message: {}", StringUtils.jaxbMarshal(message));
            }
            notifyListeners(p);
        }
    };

    private ChannelPipelineFactory channelPipelineFactory = new ChannelPipelineFactory() {
        @Override
        public ChannelPipeline getPipeline()
                throws Exception {

            ChannelPipeline p = pipeline();

            p.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
            p.addLast("protobufEnvelopeMessageDecoder", new ProtobufDecoder(Messages.Msg.getDefaultInstance()));

            p.addLast("frameEncoder", new LengthFieldPrepender(4));
            p.addLast("protobufEncoder", new ProtobufEncoder());

            p.addLast("handler", upstreamHandler);

            return p;

        }
    };
    private ClientBootstrap bootstrap;
    private Channel channel;

    @Override
    public void seraerialInit() {
        NioClientSocketChannelFactory factory =
                new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        bootstrap = new ClientBootstrap(factory);

        // Configure the event pipeline factory.
        bootstrap.setPipelineFactory(channelPipelineFactory);

        // Make a new connection.
        ChannelFuture connectFuture = bootstrap.connect(new InetSocketAddress(host, port));

        // Wait until the connection is made successfully.
        channel = connectFuture.awaitUninterruptibly().getChannel();
        if (!connectFuture.isSuccess()) {
            log.error("Client connect failed", connectFuture.getCause());
        }
    }

    @Override
    public void seraerialShutdown() {
        channel.close().awaitUninterruptibly();
        channel = null;

        bootstrap.releaseExternalResources();
        bootstrap = null;
    }

    @Override
    public boolean sendPacket(SerAerialPacket p) {
        if (p instanceof SerialRoutingPacket) {
            return send(PacketTypes.ISENSE_ISHELL_INTERPRETER & 0xFF, p.toByteArray());

        } else {
            return send(PacketTypes.SERAERIAL & 0xFF, p.toByteArray());

        }
    }

    public boolean send(int type, byte[] b){
        MessagePacket packet = new MessagePacket(type,b);
        WSNAppMessages.Message.BinaryMessage.Builder binaryMessageBuilder =
                WSNAppMessages.Message.BinaryMessage.newBuilder().setBinaryType(0xFF)
                        .setBinaryData(ByteString.copyFrom(packet.getContent()));

        WSNAppMessages.Message.Builder message =
                WSNAppMessages.Message.newBuilder().setBinaryMessage(binaryMessageBuilder)
                        .setSourceNodeId("urn:wisebed:nodeconnector:client:1")
                        .setTimestamp("Nobody cares for this demo purpose");

        WSNAppMessages.OperationInvocation.Builder oiBuilder = WSNAppMessages.OperationInvocation.newBuilder()
                .setOperation(WSNAppMessages.OperationInvocation.Operation.SEND)
                .setArguments(message.build().toByteString());

        Messages.Msg msg = Messages.Msg.newBuilder().setMsgType(WSNApp.MSG_TYPE_OPERATION_INVOCATION_REQUEST)
                .setFrom("urn:wisebed:nodeconnector:client:1").setTo("urn:wisebed:testbeduzl1:1")
                .setPayload(oiBuilder.build().toByteString()).setPriority(1)
                .setValidUntil(System.currentTimeMillis() + 5000).build();

        ChannelFuture channelFuture = channel.write(msg);
        channelFuture.awaitUninterruptibly(MAX_LOCK_WAIT_MS);
        return channelFuture.isSuccess();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
