package de.uniluebeck.itm.tr.runtime.socketconnector.client;

import com.google.protobuf.ByteString;
import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.StringUtils;
import org.apache.commons.cli.*;
import org.apache.log4j.Level;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import static de.uniluebeck.itm.tr.util.StringUtils.toPrintableString;
import static org.jboss.netty.channel.Channels.pipeline;


public class SocketConnectorClient {

	private static Logger log;

	private String host;

	private int port;

	private Channel channel;

	private ClientBootstrap bootstrap;

	public SocketConnectorClient(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public static void main(String[] args) throws IOException {

		// set up logging
		Logging.setLoggingDefaults();
		log = LoggerFactory.getLogger(SocketConnectorClient.class);

		// create the command line parser
		CommandLineParser parser = new PosixParser();
		Options options = new Options();

		options.addOption("i", "ip", true, "The IP address of the host to connect to");
		options.addOption("p", "port", true, "The port number of the host to connect to");

		options.addOption("v", "verbose", false, "Verbose logging output (equal to -l DEBUG)");
		options.addOption("l", "logging", true,
				"Set logging level (one of [" + Level.TRACE + "," + Level.DEBUG + "," + Level.INFO + "," +
						Level.WARN + "," + Level.ERROR + "])"
		);

		options.addOption("h", "help", false, "Help output");

		String ipAddress = null;
		int port = Integer.MIN_VALUE;

		try {

			CommandLine line = parser.parse(options, args);

			if (line.hasOption('v')) {
				org.apache.log4j.Logger.getRootLogger().setLevel(Level.DEBUG);
			}

			if (line.hasOption('l')) {
				Level level = Level.toLevel(line.getOptionValue('l'));
				System.out.println("Setting log level to " + level);
				org.apache.log4j.Logger.getRootLogger().setLevel(level);
				org.apache.log4j.Logger.getLogger("de.uniluebeck.itm").setLevel(level);
			}

			if (line.hasOption('h')) {
				usage(options);
			}

			if (line.hasOption('i')) {
				ipAddress = line.getOptionValue('i');
			} else {
				throw new Exception("Please supply -i");
			}

			if (line.hasOption('p')) {
				try {
					port = Integer.parseInt(line.getOptionValue('p'));
				} catch (NumberFormatException e) {
					throw new Exception("Port number must be a valid integer between 0 and 65536");
				}
			} else {
				throw new Exception("Please supply -p");
			}

		} catch (Exception e) {
			log.error("Invalid command line: " + e, e);
			usage(options);
		}

		SocketConnectorClient client = new SocketConnectorClient(ipAddress, port);
		client.start();
		client.loopMenu();

	}

	private void loopMenu() throws IOException {

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String currentLine;

		printMenu();

		while ((currentLine = reader.readLine()) != null) {
			if ("1".equals(currentLine)) {
				pingNode();
			} else if ("2".equals(currentLine)) {
				stop();
				break;
			} else {
				System.out.println("Invalid input.");
			}
			printMenu();
		}

	}

	private void pingNode() {

		WSNAppMessages.Message.Builder message = WSNAppMessages.Message.newBuilder()
				.setBinaryData(ByteString.copyFrom("Hello World".getBytes()))
				.setSourceNodeId("urn:wisebed:nodeconnector:client:1")
				.setTimestamp("Nobody cares for this demo purpose");

		WSNAppMessages.OperationInvocation.Builder oiBuilder = WSNAppMessages.OperationInvocation.newBuilder()
				.setOperation(WSNAppMessages.OperationInvocation.Operation.SEND)
				.setArguments(message.build().toByteString());

		Messages.Msg msg = Messages.Msg.newBuilder()
				.setMsgType(WSNApp.MSG_TYPE_OPERATION_INVOCATION_REQUEST)
				.setFrom("urn:wisebed:nodeconnector:client:1")
				.setTo("urn:wisebed:testbeduzl1:1")
				.setPayload(oiBuilder.build().toByteString())
				.setPriority(1)
				.setValidUntil(System.currentTimeMillis() + 5000)
				.build();

		channel.write(msg);

	}

	private void printMenu() {
		System.out.println("********************************");
		System.out.println("1 => ping all nodes");
		System.out.println("2 => quit program");
		System.out.println("********************************");
	}

	private SimpleChannelUpstreamHandler upstreamHandler = new SimpleChannelUpstreamHandler() {
		@Override
		public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

			Messages.Msg message = (Messages.Msg) e.getMessage();

			if (WSNApp.MSG_TYPE_LISTENER_MESSAGE.equals(message.getMsgType())) {

				WSNAppMessages.Message wsnAppMessage = WSNAppMessages.Message.parseFrom(message.getPayload());
				if (log.isInfoEnabled()) {
					log.info(
							"Received device output: \"{}\"",
							toPrintableString(wsnAppMessage.getBinaryData().toByteArray())
					);
				}

			} else {
				log.info("Received message: {}", StringUtils.jaxbMarshal(message));
			}
		}
	};

	private ChannelPipelineFactory channelPipelineFactory = new ChannelPipelineFactory() {
		@Override
		public ChannelPipeline getPipeline() throws Exception {

			ChannelPipeline p = pipeline();

			p.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
			p.addLast("protobufEnvelopeMessageDecoder", new ProtobufDecoder(Messages.Msg.getDefaultInstance()));

			p.addLast("frameEncoder", new LengthFieldPrepender(4));
			p.addLast("protobufEncoder", new ProtobufEncoder());

			p.addLast("handler", upstreamHandler);

			return p;

		}
	};

	private void start() {

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
			log.error("client connect failed!", connectFuture.getCause());
		}

	}

	private void stop() {

		channel.close().awaitUninterruptibly();
		channel = null;

		bootstrap.releaseExternalResources();
		bootstrap = null;
	}

	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(120, SocketConnectorClient.class.getCanonicalName(), null, options, null);
		System.exit(1);
	}

}
