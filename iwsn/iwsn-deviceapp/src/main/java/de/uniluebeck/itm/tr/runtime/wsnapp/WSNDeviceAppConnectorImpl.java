/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import de.uniluebeck.itm.netty.handlerstack.FilterPipeline;
import de.uniluebeck.itm.netty.handlerstack.FilterPipelineImpl;
import de.uniluebeck.itm.netty.handlerstack.HandlerFactoryRegistry;
import de.uniluebeck.itm.netty.handlerstack.dlestxetx.DleStxEtxFramingDecoder;
import de.uniluebeck.itm.netty.handlerstack.dlestxetx.DleStxEtxFramingEncoder;
import de.uniluebeck.itm.netty.handlerstack.protocolcollection.ProtocolCollection;
import de.uniluebeck.itm.netty.handlerstack.util.ChannelBufferTools;
import de.uniluebeck.itm.tr.nodeapi.NodeApi;
import de.uniluebeck.itm.tr.nodeapi.NodeApiCallResult;
import de.uniluebeck.itm.tr.nodeapi.NodeApiDeviceAdapter;
import de.uniluebeck.itm.tr.util.*;
import de.uniluebeck.itm.wsn.deviceutils.DeviceUtilsModule;
import de.uniluebeck.itm.wsn.deviceutils.macreader.DeviceMacReferenceMap;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceEvent;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceInfo;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceObserver;
import de.uniluebeck.itm.wsn.drivers.core.Device;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import de.uniluebeck.itm.wsn.drivers.core.exception.ProgramChipMismatchException;
import de.uniluebeck.itm.wsn.drivers.core.operation.OperationCallback;
import de.uniluebeck.itm.wsn.drivers.core.operation.OperationCallbackAdapter;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactoryImpl;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.iostream.IOStreamAddress;
import org.jboss.netty.channel.iostream.IOStreamChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Closeables.closeQuietly;
import static de.uniluebeck.itm.tr.util.StringUtils.toPrintableString;
import static org.jboss.netty.channel.Channels.pipeline;


public class WSNDeviceAppConnectorImpl extends ListenerManagerImpl<WSNDeviceAppConnector.NodeOutputListener>
		implements WSNDeviceAppConnector {

	private static final Logger log = LoggerFactory.getLogger(WSNDeviceAppConnector.class);

	private static final int MESSAGE_TYPE_WISELIB_DOWNSTREAM = 10;

	private static final int MESSAGE_TYPE_WISELIB_UPSTREAM = 105;

	private static final byte NODE_OUTPUT_TEXT = 50;

	private static final byte NODE_OUTPUT_BYTE = 51;

	private static final byte NODE_OUTPUT_VIRTUAL_LINK = 52;

	private static final byte VIRTUAL_LINK_MESSAGE = 11;

	private static final int PACKETS_DROPPED_NOTIFICATION_RATE = 1000;

	private static final int PIPELINE_MISCONFIGURATION_NOTIFICATION_RATE = 5000;

	private final WSNDeviceAppConfiguration configuration;

	private final RateLimiter maximumMessageRateLimiter;

	private final TimeDiff packetsDroppedTimeDiff = new TimeDiff(PACKETS_DROPPED_NOTIFICATION_RATE);

	private final TimeDiff pipelineMisconfigurationTimeDiff = new TimeDiff(PIPELINE_MISCONFIGURATION_NOTIFICATION_RATE);

	private int resetCount = 0;

	private int flashCount = 0;

	private Channel deviceChannel;

	private int packetsDroppedSinceLastNotification = 0;

	/**
	 * Used to execute calls on the Node API as calls to it will currently block the thread executing the call.
	 */
	private ExecutorService nodeApiExecutor;

	/**
	 * A scheduler that is used by the device drivers to schedule operations and communications with the device.
	 */
	private ScheduledExecutorService deviceDriverScheduler;

	private transient boolean flashOperationRunningOrEnqueued = false;

	private final Runnable connectRunnable = new Runnable() {

		@Override
		public void run() {

			try {

				if (nodeSerialInterfaceUnknown()) {

					tryToDetectNodeSerialInterface();

					if (nodeSerialInterfaceUnknown()) {
						log.warn("{} => Could not find port for {} device.",
								configuration.getNodeUrn(), configuration.getNodeType()
						);
						return;
					}

				}

				closeQuietly(device);
				tryToConnect();

			} catch (Exception e) {
				log.error("" + e, e);
				throw new RuntimeException(e);
			}

		}
	};

	private HandlerFactoryRegistry handlerFactoryRegistry = new HandlerFactoryRegistry();

	private NodeApi nodeApi;

	private NodeApiDeviceAdapter nodeApiDeviceAdapter = new NodeApiDeviceAdapter() {
		@Override
		public void sendToNode(final ByteBuffer packet) {
			try {
				if (log.isDebugEnabled()) {
					log.debug(
							"{} => Sending a WISELIB_DOWNSTREAM packet: {}",
							configuration.getNodeUrn(),
							toPrintableString(packet.array(), 200)
					);
				}

				final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(
						new byte[]{MESSAGE_TYPE_WISELIB_DOWNSTREAM},
						packet.array()
				);

				deviceChannel.write(buffer);

			} catch (Exception e) {
				log.error("" + e, e);
			}
		}
	};

	private Device device;

	private FilterPipeline filterPipeline = new FilterPipelineImpl();

	private String detectedNodeSerialInterface;

	public WSNDeviceAppConnectorImpl(final WSNDeviceAppConfiguration configuration) {

		this.configuration = configuration;

		this.nodeApi = new NodeApi(
				configuration.getNodeUrn(),
				nodeApiDeviceAdapter,
				configuration.getTimeoutNodeAPI(),
				TimeUnit.MILLISECONDS
		);

		this.maximumMessageRateLimiter = new RateLimiterImpl(
				configuration.getMaximumMessageRate(),
				1,
				TimeUnit.SECONDS
		);

		try {
			ProtocolCollection.registerProtocols(handlerFactoryRegistry);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void start() throws Exception {
		nodeApiExecutor = Executors.newCachedThreadPool();
		deviceDriverScheduler = Executors.newScheduledThreadPool(2,
				new ThreadFactoryBuilder()
						.setNameFormat("[" + configuration.getNodeUrn() + "]-Thread %d")
						.build()
		);
		deviceDriverScheduler.execute(connectRunnable);
		nodeApi.start();
	}

	@Override
	public void stop() {

		nodeApi.stop();

		log.debug("{} => Shutting down {} device", configuration.getNodeUrn(), configuration.getNodeType());

		shutdownDeviceChannel();
		closeQuietly(device);

		if (nodeApiExecutor != null) {
			ExecutorUtils.shutdown(nodeApiExecutor, 1, TimeUnit.SECONDS);
		}

		if (deviceDriverScheduler != null) {
			ExecutorUtils.shutdown(deviceDriverScheduler, 1, TimeUnit.SECONDS);
		}
	}

	@Override
	public void destroyVirtualLink(final long targetNode, final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorImpl.destroyVirtualLink()", configuration.getNodeUrn());

		if (isConnected()) {
			nodeApiExecutor.execute(new Runnable() {
				@Override
				public void run() {
					callCallback(nodeApi.getLinkControl().destroyVirtualLink(targetNode), listener);
				}
			}
			);
		} else {
			listener.failure((byte) -1, "Node is not connected.".getBytes());
		}

	}

	@Override
	public void disableNode(final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorImpl.disableNode()", configuration.getNodeUrn());

		if (isConnected()) {
			nodeApiExecutor.execute(new Runnable() {
				@Override
				public void run() {
					callCallback(nodeApi.getNodeControl().disableNode(), listener);
				}
			}
			);
		} else {
			listener.failure((byte) -1, "Node is not connected.".getBytes());
		}

	}

	@Override
	public void disablePhysicalLink(final long nodeB, final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorImpl.disablePhysicalLink()", configuration.getNodeUrn());

		if (isConnected()) {
			nodeApiExecutor.execute(new Runnable() {
				@Override
				public void run() {
					callCallback(nodeApi.getLinkControl().disablePhysicalLink(nodeB), listener);
				}
			}
			);
		} else {
			listener.failure((byte) -1, "Node is not connected.".getBytes());
		}

	}

	@Override
	public void enableNode(final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorImpl.enableNode()", configuration.getNodeUrn());

		if (isConnected()) {
			nodeApiExecutor.execute(new Runnable() {
				@Override
				public void run() {
					callCallback(nodeApi.getNodeControl().enableNode(), listener);
				}
			}
			);
		} else {
			listener.failure((byte) -1, "Node is not connected.".getBytes());
		}
	}

	@Override
	public void enablePhysicalLink(final long nodeB, final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorImpl.enablePhysicalLink()", configuration.getNodeUrn());

		if (isConnected()) {
			nodeApiExecutor.execute(new Runnable() {
				@Override
				public void run() {
					callCallback(nodeApi.getLinkControl().enablePhysicalLink(nodeB), listener);
				}
			}
			);
		} else {
			listener.failure((byte) -1, "Node is not connected.".getBytes());
		}
	}

	@Override
	public void flashProgram(final WSNAppMessages.Program program,
							 final FlashProgramCallback listener) {

		log.debug("{} => WSNDeviceAppConnectorImpl.executeFlashPrograms()", configuration.getNodeUrn());

		if (isConnected()) {

			if (flashOperationRunningOrEnqueued) {

				String msg = "There's a flash operation running or enqueued currently. Please try again later.";
				log.warn("{} => flashProgram: {}", configuration.getNodeUrn(), msg);
				listener.failure((byte) -1, msg.getBytes());

			} else {

				flashOperationRunningOrEnqueued = true;
				device.program(program.getProgram().toByteArray(), configuration.getTimeoutFlash(),
						new OperationCallback<Void>() {

							private int lastProgress = -1;

							@Override
							public void onExecute() {
								flashCount = (flashCount % Integer.MAX_VALUE) == 0 ? 0 : flashCount++;
								lastProgress = 0;
								listener.progress(0f);
							}

							@Override
							public void onCancel() {

								String msg = "Flash operation was canceled.";
								log.error("{} => flashProgram: {}", configuration.getNodeUrn(), msg);
								flashOperationRunningOrEnqueued = false;
								listener.failure((byte) -1, msg.getBytes());
							}

							@Override
							public void onFailure(final Throwable throwable) {

								String msg;

								if (throwable instanceof ProgramChipMismatchException) {
									msg = "Error reading binary image: " + throwable;
								} else {
									msg = "Failed flashing node. Reason: " + throwable;
								}

								log.warn("{} => flashProgram: {}", configuration.getNodeUrn(), msg);
								flashOperationRunningOrEnqueued = false;
								listener.failure((byte) -3, msg.getBytes());
							}

							@Override
							public void onProgressChange(final float fraction) {

								int newProgress = (int) (fraction * 100);

								if (newProgress > lastProgress) {

									log.debug("{} => Flashing progress: {}%.", configuration.getNodeUrn(), newProgress);
									listener.progress(fraction);
									lastProgress = newProgress;
								}
							}

							@Override
							public void onSuccess(final Void result) {

								log.debug("{} => Done flashing node.", configuration.getNodeUrn());
								flashOperationRunningOrEnqueued = false;
								listener.success(null);
							}
						}
				);

			}

		} else {
			String msg = "Failed flashing node. Reason: Node is not connected.";
			log.warn("{} => {}", configuration.getNodeUrn(), msg);
			listener.failure((byte) -2, msg.getBytes());
		}

	}

	@Override
	public void isNodeAlive(final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorImpl.isNodeAlive()", configuration.getNodeUrn());

		// to the best of our knowledge, a node is alive if we're connected to it
		if (isConnected()) {
			listener.success(null);
		} else {
			listener.failure((byte) 0, "Device is not connected.".getBytes());
		}
	}

	@Override
	public void resetNode(final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorImpl.resetNode()", configuration.getNodeUrn());

		if (isConnected()) {

			device.reset(configuration.getTimeoutReset(), new OperationCallbackAdapter<Void>() {

				@Override
				public void onExecute() {
					resetCount = (resetCount % Integer.MAX_VALUE) == 0 ? 0 : resetCount++;
				}

				@Override
				public void onSuccess(final Void result) {
					log.debug("{} => Done resetting node.", configuration.getNodeUrn());
					listener.success(null);
				}

				@Override
				public void onCancel() {
					listener.failure((byte) -1, "Operation was cancelled.".getBytes());
				}

				@Override
				public void onFailure(final Throwable throwable) {
					String msg = "Failed resetting node. Reason: " + throwable;
					log.warn("{} => resetNode(): {}", configuration.getNodeUrn(), msg);
					listener.failure((byte) -1, msg.getBytes());
				}
			}
			);

		} else {

			String msg = "Failed resetting node. Reason: Device is not connected.";
			log.warn("{} => {}", configuration.getNodeUrn(), msg);
			listener.failure((byte) 0, msg.getBytes());
		}

	}

	@Override
	public void sendMessage(final byte[] messageBytes, final Callback callback) {

		log.debug("{} => WSNDeviceAppConnectorImpl.sendMessage()", configuration.getNodeUrn());

		if (isConnected()) {

			if (isVirtualLinkMessage(messageBytes)) {

				log.debug("{} => Delivering virtual link message over node API", configuration.getNodeUrn());

				ByteBuffer messageBuffer = ByteBuffer.wrap(messageBytes);

				final byte RSSI = messageBuffer.get(3);
				final byte LQI = messageBuffer.get(4);
				final byte payloadLength = messageBuffer.get(5);
				final long destination = messageBuffer.getLong(6);
				final long source = messageBuffer.getLong(14);
				final byte[] payload = new byte[payloadLength];

				System.arraycopy(messageBytes, 22, payload, 0, payloadLength);

				nodeApiExecutor.execute(new Runnable() {
					@Override
					public void run() {
						callCallback(
								nodeApi.getInteraction().sendVirtualLinkMessage(RSSI, LQI, destination, source,
										payload
								),
								callback
						);
					}
				}
				);

			} else {

				log.debug("{} => Delivering message directly over iSenseDevice.send(), i.e. not as a virtual link "
						+ "message.", configuration.getNodeUrn()
				);

				final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(messageBytes);

				deviceChannel.write(buffer).addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(final ChannelFuture future) throws Exception {
						if (future.isSuccess()) {

							callback.success(null);

						} else if (future.isCancelled()) {

							String msg = "Sending message was canceled.";
							log.warn("{} => sendMessage(): {}", configuration.getNodeUrn(), msg);
							callback.failure((byte) -3, msg.getBytes());

						} else {

							String msg = "Failed sending message. Reason: " + future.getCause();
							log.warn("{} => sendMessage(): {}", configuration.getNodeUrn(), msg);
							callback.failure((byte) -2, msg.getBytes());
						}
					}
				}
				);

			}

		} else {
			callback.failure((byte) -1, "Node is not connected.".getBytes());
		}

	}

	@Override
	public void setChannelPipeline(final List<Tuple<String, Multimap<String, String>>> channelHandlerConfigurations,
								   final Callback callback) {

		if (isConnected()) {

			try {

				log.debug("{} => Setting channel pipeline using configuration: {}", configuration.getNodeUrn(),
						channelHandlerConfigurations
				);

				final List<Tuple<String, ChannelHandler>> pipeline =
						handlerFactoryRegistry.create(channelHandlerConfigurations);

				if (log.isDebugEnabled() && pipeline.size() > 0) {
					pipeline.add(0, new Tuple<String, ChannelHandler>(
							"aboveFilterPipelineLogger",
							new AboveFilterPipelineLogger(configuration.getNodeUrn())
					)
					);
					pipeline.add(new Tuple<String, ChannelHandler>(
							"belowFilterPipelineLogger",
							new BelowFilterPipelineLogger(configuration.getNodeUrn())
					)
					);
				}

				filterPipeline.setChannelPipeline(pipeline);
				callback.success(null);

			} catch (Exception e) {

				log.warn("{} => {} while setting channel pipeline: {}",
						new Object[]{configuration.getNodeUrn(), e.getClass().getSimpleName(), e.getMessage()}
				);

				callback.failure(
						(byte) -1,
						("Exception while setting channel pipeline: " + e.getMessage()).getBytes()
				);

				log.warn("{} => Resetting channel pipeline to default pipeline.", configuration.getNodeUrn());
				filterPipeline.setChannelPipeline(createDefaultChannelHandlers());
			}

			log.debug("{} => Channel pipeline now set to: {}",
					configuration.getNodeUrn(), filterPipeline.getChannelPipeline()
			);

		} else {
			callback.failure((byte) -1, "Node is not connected.".getBytes());
		}
	}

	private SimpleChannelHandler forwardingHandler = new SimpleChannelHandler() {
		@Override
		public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e)
				throws Exception {

			if (e.getMessage() instanceof ChannelBuffer) {

				final ChannelBuffer message = (ChannelBuffer) e.getMessage();
				onBytesReceivedFromDevice(message);

			} else {

				String notification = "The pipeline seems to be wrongly configured. A message of type " +
						e.getMessage().getClass().getCanonicalName() +
						" was received. Only " +
						ChannelBuffer.class.getCanonicalName() +
						" instances are allowed!";

				sendPipelineMisconfigurationIfNotificationRateAllows(notification);
			}
		}

		@Override
		public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e)
				throws Exception {

			log.warn(
					"{} => {} in pipeline: {}",
					new Object[]{
							configuration.getNodeUrn(),
							e.getCause().getClass().getSimpleName(),
							e.getCause().getMessage()
					}
			);

			String notification = "The pipeline seems to be wrongly configured. A(n) " +
					e.getCause().getClass().getSimpleName() +
					" was caught and contained the following message: " +
					e.getCause().getMessage();

			sendPipelineMisconfigurationIfNotificationRateAllows(notification);
		}
	};

	private void tryToConnect() throws Exception {

		DeviceFactory deviceFactory = new DeviceFactoryImpl();
		device = deviceFactory.create(deviceDriverScheduler, configuration.getNodeType());

		String interfaceToConnectTo = detectedNodeSerialInterface != null ?
				detectedNodeSerialInterface :
				configuration.getNodeSerialInterface();

		try {

			device.connect(interfaceToConnectTo);

		} catch (Exception e) {
			log.warn("{} => Could not connect to {} device at {}.",
					new Object[]{
							configuration.getNodeUrn(),
							configuration.getNodeType(),
							interfaceToConnectTo
					}
			);
			throw new Exception("Could not connect to " + configuration.getNodeType() + " at " +
					interfaceToConnectTo
			);
		}

		if (!device.isConnected()) {
			log.warn("{} => Could not connect to {} device at {}.",
					new Object[]{
							configuration.getNodeUrn(),
							configuration.getNodeType(),
							interfaceToConnectTo
					}
			);
			throw new Exception("Could not connect to " + configuration.getNodeType() + " at " +
					interfaceToConnectTo
			);
		}

		log.info("{} => Successfully connected to {} node on serial port {}",
				new Object[]{
						configuration.getNodeUrn(), configuration.getNodeType(), interfaceToConnectTo
				}
		);

		final ClientBootstrap bootstrap = new ClientBootstrap(new IOStreamChannelFactory(nodeApiExecutor));
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				final ChannelPipeline pipeline = pipeline();
				pipeline.addFirst("forwardingHandler", forwardingHandler);
				pipeline.addFirst("filterHandler", filterPipeline);
				filterPipeline.setChannelPipeline(createDefaultChannelHandlers());
				return pipeline;
			}
		}
		);
		final ChannelFuture connectFuture = bootstrap.connect(
				new IOStreamAddress(
						device.getInputStream(),
						device.getOutputStream()
				)
		);
		deviceChannel = connectFuture.await().getChannel();
	}

	private void tryToDetectNodeSerialInterface() {

		final DeviceMacReferenceMap deviceMacReferenceMap = new DeviceMacReferenceMap();
		final Long macAsLong = StringUtils.parseHexOrDecLongFromUrn(configuration.getNodeUrn());
		final MacAddress nodeMacAddress = new MacAddress(macAsLong);

		if (configuration.getNodeUSBChipID() != null && !"".equals(configuration.getNodeUSBChipID())) {
			deviceMacReferenceMap.put(configuration.getNodeUSBChipID(), nodeMacAddress);
		}

		log.info("{} => Searching for port of {} device with MAC address {}.",
				new Object[]{configuration.getNodeUrn(), configuration.getNodeType(), nodeMacAddress}
		);

		DeviceObserver deviceObserver = Guice
				.createInjector(new DeviceUtilsModule(deviceMacReferenceMap))
				.getInstance(DeviceObserver.class);

		final ImmutableList<DeviceEvent> events = deviceObserver.getEvents();

		for (DeviceEvent event : events) {
			final DeviceInfo deviceInfo = event.getDeviceInfo();
			if (nodeMacAddress.equals(deviceInfo.getMacAddress())) {
				log.info("{} => Found {} device with MAC address {} at port {}",
						new Object[]{
								configuration.getNodeUrn(),
								configuration.getNodeType(),
								deviceInfo.getMacAddress(),
								deviceInfo.getPort()
						}
				);
				detectedNodeSerialInterface = deviceInfo.getPort();
				break;
			}
		}
	}

	private boolean nodeSerialInterfaceUnknown() {
		return (configuration.getNodeSerialInterface() == null || "".equals(configuration.getNodeSerialInterface())) &&
				(detectedNodeSerialInterface == null || "".equals(detectedNodeSerialInterface));
	}

	private void onBytesReceivedFromDevice(final ChannelBuffer buffer) {

		//if reaching maximum-message-rate do not send more then 1 message
		if (!maximumMessageRateLimiter.checkIfInSlotAndCount()) {
			int dismissedCount = maximumMessageRateLimiter.dismissedCount();
			if (dismissedCount >= 1) {
				sendPacketsDroppedWarningIfNotificationRateAllows(dismissedCount);
			}
			return;
		}

		if (log.isTraceEnabled()) {
			log.trace("{} => WSNDeviceAppConnectorImpl.receivePacket: {}",
					configuration.getNodeUrn(),
					ChannelBufferTools.toPrintableString(buffer, 200)
			);
		}

		boolean isWiselibUpstream =
				(buffer.getByte(0) & 0xFF) == MESSAGE_TYPE_WISELIB_UPSTREAM;

		boolean isByteTextOrVLink = buffer.readableBytes() > 1 &&
				((buffer.getByte(1) & 0xFF) == NODE_OUTPUT_BYTE ||
						(buffer.getByte(1) & 0xFF) == NODE_OUTPUT_TEXT ||
						(buffer.getByte(1) & 0xFF) == NODE_OUTPUT_VIRTUAL_LINK);

		boolean isWiselibReply = isWiselibUpstream && !isByteTextOrVLink;

		if (isWiselibReply) {

			final ByteBuffer packetWithoutISenseMessageType = ByteBuffer.allocate(buffer.readableBytes() - 1);
			System.arraycopy(buffer.array(), 1, packetWithoutISenseMessageType.array(), 0, buffer.readableBytes() - 1);

			if (nodeApiDeviceAdapter.receiveFromNode(packetWithoutISenseMessageType)) {

				if (log.isDebugEnabled()) {
					log.debug("{} => Received WISELIB_UPSTREAM packet with content: {}",
							configuration.getNodeUrn(),
							ChannelBufferTools.toPrintableString(buffer, 200)
					);
				}
			} else {

				if (log.isWarnEnabled()) {
					log.warn(
							"{} => Received WISELIB_UPSTREAM packet that was not expected by the Node API with content: {}",
							configuration.getNodeUrn(),
							ChannelBufferTools.toPrintableString(buffer, 200)
					);
				}
			}


		} else {

			// convert buffer to plain byte-array
			byte[] bytes = new byte[buffer.readableBytes()];
			buffer.getBytes(0, bytes);

			for (NodeOutputListener listener : listeners) {
				listener.receivedPacket(bytes);
			}
		}
	}

	private void sendPipelineMisconfigurationIfNotificationRateAllows(String notification) {

		if (pipelineMisconfigurationTimeDiff.isTimeout()) {

			for (NodeOutputListener listener : listeners) {
				listener.receiveNotification(notification);
			}

			pipelineMisconfigurationTimeDiff.touch();
		}
	}

	private void sendPacketsDroppedWarningIfNotificationRateAllows(int packetsDropped) {

		packetsDroppedSinceLastNotification += packetsDropped;

		if (packetsDroppedTimeDiff.isTimeout()) {

			String notification =
					"Dropped " + packetsDroppedSinceLastNotification + " packets of " + configuration.getNodeUrn() +
							" in the last " + packetsDroppedTimeDiff.ms() + " milliseconds, because the node writes "
							+ "more packets to the serial interface per second than allowed (" +
							configuration.getMaximumMessageRate() + " per second).";

			for (NodeOutputListener listener : listeners) {
				listener.receiveNotification(notification);
			}

			packetsDroppedSinceLastNotification = 0;
			packetsDroppedTimeDiff.touch();
		}
	}

	private List<Tuple<String, ChannelHandler>> createDefaultChannelHandlers() {
		return newArrayList(
				new Tuple<String, ChannelHandler>("frameDecoder", new DleStxEtxFramingDecoder()),
				new Tuple<String, ChannelHandler>("frameEncoder", new DleStxEtxFramingEncoder())
		);
	}

	@Override
	public void setVirtualLink(final long targetNode, final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorImpl.setVirtualLink()", configuration.getNodeUrn());

		if (isConnected()) {
			nodeApiExecutor.execute(new Runnable() {
				@Override
				public void run() {
					callCallback(nodeApi.getLinkControl().setVirtualLink(targetNode), listener);
				}
			}
			);
		} else {
			listener.failure((byte) -1, "Node is not connected.".getBytes());
		}
	}

	private void shutdownDeviceChannel() {

		if (deviceChannel != null && deviceChannel.isConnected()) {

			try {

				deviceChannel.close().await();

			} catch (Exception e) {
				log.warn("{} => Exception while closing DeviceConnection!", configuration.getNodeUrn(), e);
			}
		}
	}

	private boolean isVirtualLinkMessage(final byte[] messageBytes) {
		return messageBytes.length > 1 &&
				messageBytes[0] == MESSAGE_TYPE_WISELIB_DOWNSTREAM &&
				messageBytes[1] == VIRTUAL_LINK_MESSAGE;
	}

	private void callCallback(final Future<NodeApiCallResult> future, final Callback listener) {
		try {

			NodeApiCallResult result = future.get();

			if (result.isSuccessful()) {
				listener.success(result.getResponse());
			} else {
				listener.failure(result.getResponseType(), result.getResponse());
			}

		} catch (InterruptedException e) {
			log.error("{} => InterruptedException while reading Node API call result.", configuration.getNodeUrn());
			listener.failure((byte) 127, "Unknown error in testbed back-end occurred!".getBytes());
		} catch (ExecutionException e) {
			if (e.getCause() instanceof TimeoutException) {
				log.debug("{} => Call to Node API timed out.", configuration.getNodeUrn());
				listener.timeout();
			} else {
				log.error("" + e, e);
				listener.failure((byte) 127, "Unknown error in testbed back-end occurred!".getBytes());
			}
		}
	}

	private boolean isConnected() {
		return device != null && device.isConnected();
	}
}
