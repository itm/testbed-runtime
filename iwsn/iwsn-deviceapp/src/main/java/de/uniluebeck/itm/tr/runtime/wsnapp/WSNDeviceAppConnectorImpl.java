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
import com.google.inject.Guice;
import de.uniluebeck.itm.gtr.common.SchedulerService;
import de.uniluebeck.itm.netty.handlerstack.dlestxetx.DleStxEtxFramingDecoder;
import de.uniluebeck.itm.netty.handlerstack.dlestxetx.DleStxEtxFramingEncoder;
import de.uniluebeck.itm.tr.nodeapi.NodeApi;
import de.uniluebeck.itm.tr.nodeapi.NodeApiCallResult;
import de.uniluebeck.itm.tr.nodeapi.NodeApiDeviceAdapter;
import de.uniluebeck.itm.tr.util.*;
import de.uniluebeck.itm.wsn.deviceutils.DeviceUtilsModule;
import de.uniluebeck.itm.wsn.deviceutils.macreader.DeviceMacReferenceMap;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceEvent;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceInfo;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceObserver;
import de.uniluebeck.itm.wsn.drivers.core.Connection;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import de.uniluebeck.itm.wsn.drivers.core.async.AsyncAdapter;
import de.uniluebeck.itm.wsn.drivers.core.async.DeviceAsync;
import de.uniluebeck.itm.wsn.drivers.core.async.OperationQueue;
import de.uniluebeck.itm.wsn.drivers.core.async.thread.PausableExecutorOperationQueue;
import de.uniluebeck.itm.wsn.drivers.core.exception.ProgramChipMismatchException;
import de.uniluebeck.itm.wsn.drivers.core.operation.Operation;
import de.uniluebeck.itm.wsn.drivers.core.operation.ProgramOperation;
import de.uniluebeck.itm.wsn.drivers.factories.ConnectionFactory;
import de.uniluebeck.itm.wsn.drivers.factories.ConnectionFactoryImpl;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceAsyncFactoryImpl;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactoryImpl;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.iostream.IOStreamAddress;
import org.jboss.netty.channel.iostream.IOStreamChannelFactory;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.*;


public class WSNDeviceAppConnectorImpl extends AbstractListenable<WSNDeviceAppConnector.NodeOutputListener>
		implements WSNDeviceAppConnector {

	private static final Logger log = LoggerFactory.getLogger(WSNDeviceAppConnector.class);

	private static final int MESSAGE_TYPE_WISELIB_DOWNSTREAM = 10;

	private static final int MESSAGE_TYPE_WISELIB_UPSTREAM = 105;

	private static final byte NODE_OUTPUT_TEXT = 50;

	private static final byte NODE_OUTPUT_BYTE = 51;

	private static final byte NODE_OUTPUT_VIRTUAL_LINK = 52;

	private static final byte VIRTUAL_LINK_MESSAGE = 11;

	private static final int DEFAULT_NODE_API_TIMEOUT = 1000;

	private static final int DEFAULT_MAXIMUM_MESSAGE_RATE = Integer.MAX_VALUE;

	private static final int PACKETS_DROPPED_NOTIFICATION_RATE = 1000;

	private static final int PIPELINE_MISCONFIGURATION_NOTIFICATION_RATE = 5000;

	private final String nodeType;

	private final String nodeUSBChipID;

	private String nodeSerialInterface;

	private final SchedulerService schedulerService;

	private int resetCount = 0;

	private int flashCount = 0;

	private final int timeoutReset;

	private final int timeoutFlash;

	private final int maximumMessageRate;

	private final TimeUnit maximumMessageRateTimeUnit;

	private final RateLimiter maximumMessageRateLimiter;

	private Channel deviceChannel;

	private final TimeDiff packetsDroppedTimeDiff = new TimeDiff(PACKETS_DROPPED_NOTIFICATION_RATE);

	private int packetsDroppedSinceLastNotification = 0;

	private final TimeDiff pipelineMisconfigurationTimeDiff = new TimeDiff(PIPELINE_MISCONFIGURATION_NOTIFICATION_RATE);

	private final Runnable connectRunnable = new Runnable() {

		@Override
		public void run() {

			try {
				if (nodeSerialInterfaceUnknown()) {

					tryToDetectNodeSerialInterface();

					if (nodeSerialInterfaceUnknown()) {
						log.warn("{} => Could not find port for {} device.", nodeUrn, nodeType);
						return;
					}

				}

				connect();

			} catch (Exception e) {
				device = null;
				if (deviceChannel != null) {
					deviceChannel.close().awaitUninterruptibly();
				}
				deviceChannel = null;
				if (deviceConnection != null) {
					try {
						deviceConnection.close();
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}
				}
				deviceConnection = null;
				if (deviceOperationQueue != null) {
					deviceOperationQueue.shutdown(true);
				}
				deviceOperationQueue = null;
				log.error("" + e, e);
				throw new RuntimeException(e);
			}

		}
	};

	private ExecutorService executorService;

	private void connect() throws Exception {

		final ConnectionFactory connectionFactory = new ConnectionFactoryImpl();
		final Connection connection = connectionFactory.create(nodeType);

		try {
			connection.connect(nodeSerialInterface);
		} catch (Exception e) {
			log.warn("{} => Could not connect to {} device at {}.",
					new Object[]{nodeUrn, nodeType, nodeSerialInterface}
			);
			throw new Exception("Could not connect to " + nodeType + " at " + nodeSerialInterface);
		}

		if (!connection.isConnected()) {
			log.warn("{} => Could not connect to {} device at {}.",
					new Object[]{nodeUrn, nodeType, nodeSerialInterface}
			);
			throw new Exception("Could not connect to " + nodeType + " at " + nodeSerialInterface);
		}

		log.info("{} => Successfully connected to {} node on serial port {}",
				new Object[]{nodeUrn, nodeType, nodeSerialInterface}
		);

		final DeviceAsyncFactoryImpl deviceFactory = new DeviceAsyncFactoryImpl(new DeviceFactoryImpl());

		deviceConnection = connection;
		deviceOperationQueue = new PausableExecutorOperationQueue();
		device = deviceFactory.create(executorService, nodeType, connection, deviceOperationQueue);

		final InputStream inputStream = device.getInputStream();
		final OutputStream outputStream = device.getOutputStream();

		final ClientBootstrap bootstrap = new ClientBootstrap(new IOStreamChannelFactory(executorService));
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				DefaultChannelPipeline pipeline = new DefaultChannelPipeline();
				pipeline.addLast("frameDecoder", new DleStxEtxFramingDecoder());
				pipeline.addLast("frameEncoder", new DleStxEtxFramingEncoder());
				pipeline.addLast("dataReceiver", new SimpleChannelHandler() {
					@Override
					public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e)
							throws Exception {

						if (e.getMessage() instanceof ChannelBuffer) {
							onBytesReceivedFromDevice((ChannelBuffer) e.getMessage());
						} else {
							sendPipelineMisconfigurationIfNotificationRateAllows(e.getMessage().getClass());
						}
					}
				}
				);
				return pipeline;
			}
		}
		);

		// Make a new connection.
		ChannelFuture connectFuture = bootstrap.connect(new IOStreamAddress(inputStream, outputStream));

		// Wait until the connection is made successfully.
		deviceChannel = connectFuture.awaitUninterruptibly().getChannel();
	}

	private void tryToDetectNodeSerialInterface() {

		final DeviceMacReferenceMap deviceMacReferenceMap = new DeviceMacReferenceMap();
		final Long macAsLong = StringUtils.parseHexOrDecLongFromUrn(nodeUrn);
		final MacAddress nodeMacAddress = new MacAddress(macAsLong);

		if (nodeUSBChipID != null && !"".equals(nodeUSBChipID)) {
			deviceMacReferenceMap.put(nodeUSBChipID, nodeMacAddress);
		}

		log.info("{} => Searching for port of {} device with MAC address {}.",
				new Object[]{nodeUrn, nodeType, nodeMacAddress}
		);

		DeviceObserver deviceObserver = Guice
				.createInjector(new DeviceUtilsModule(deviceMacReferenceMap))
				.getInstance(DeviceObserver.class);

		final ImmutableList<DeviceEvent> events = deviceObserver.getEvents();

		for (DeviceEvent event : events) {
			final DeviceInfo deviceInfo = event.getDeviceInfo();
			if (nodeMacAddress.equals(deviceInfo.getMacAddress())) {
				log.info("{} => Found {} device with MAC address {} at port {}",
						new Object[]{nodeUrn, nodeType, deviceInfo.getMacAddress(), deviceInfo.getPort()}
				);
				nodeSerialInterface = deviceInfo.getPort();
				break;
			}
		}
	}

	private boolean nodeSerialInterfaceUnknown() {
		return nodeSerialInterface == null || "".equals(nodeSerialInterface);
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
					nodeUrn,
					StringUtils.replaceNonPrintableAsciiCharacters(buffer.toString(CharsetUtil.UTF_8))
			);
		}

		boolean isWiselibUpstream =
				(buffer.getByte(0) & 0xFF) == MESSAGE_TYPE_WISELIB_UPSTREAM;

		boolean isByteTextOrVLink = buffer.readableBytes() > 0 &&
				(buffer.getByte(1) & 0xFF) == NODE_OUTPUT_BYTE ||
				(buffer.getByte(1) & 0xFF) == NODE_OUTPUT_TEXT ||
				(buffer.getByte(1) & 0xFF) == NODE_OUTPUT_VIRTUAL_LINK;

		boolean isWiselibReply = isWiselibUpstream && !isByteTextOrVLink;

		if (isWiselibReply) {

			final ByteBuffer packetWithoutISenseMessageType = ByteBuffer.allocate(buffer.readableBytes() - 1);
			System.arraycopy(buffer.array(), 1, packetWithoutISenseMessageType.array(), 0, buffer.readableBytes() - 1);

			if (nodeApiDeviceAdapter.receiveFromNode(packetWithoutISenseMessageType)) {

				if (log.isDebugEnabled()) {
					log.debug("{} => Received WISELIB_UPSTREAM packet with content: {}",
							nodeUrn,
							StringUtils.replaceNonPrintableAsciiCharacters(buffer.toString(CharsetUtil.UTF_8))
					);
				}
			} else {

				if (log.isWarnEnabled()) {
					log.warn(
							"{} => Received WISELIB_UPSTREAM packet that was not expected by the Node API with content: {}",
							nodeUrn,
							StringUtils.replaceNonPrintableAsciiCharacters(buffer.toString(CharsetUtil.UTF_8))
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

	private void sendPipelineMisconfigurationIfNotificationRateAllows(final Class<?> receivedType) {

		if (pipelineMisconfigurationTimeDiff.isTimeout()) {

			String notification =
					"The pipeline seems to be misconfigured. A message of type " + receivedType.getCanonicalName() +
							" was received. Only " + ChannelBuffer.class.getCanonicalName() + " instances are allowed!";

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
					"Dropped " + packetsDroppedSinceLastNotification + " packets of " + nodeUrn + " in the last "
							+ packetsDroppedTimeDiff.ms() + " milliseconds, because the node writes more packets to "
							+ "the serial interface per second than allowed (" + maximumMessageRate + " per " +
							maximumMessageRateTimeUnit + ").";

			for (NodeOutputListener listener : listeners) {
				listener.receiveNotification(notification);
			}

			packetsDroppedSinceLastNotification = 0;
			packetsDroppedTimeDiff.touch();
		}
	}

	private NodeApi nodeApi;

	private NodeApiDeviceAdapter nodeApiDeviceAdapter = new NodeApiDeviceAdapter() {
		@Override
		public void sendToNode(final ByteBuffer packet) {
			try {
				if (log.isDebugEnabled()) {
					log.debug(
							"{} => Sending a WISELIB_DOWNSTREAM packet: {}",
							nodeUrn,
							StringUtils.toHexString(packet.array())
					);
				}

				final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(
						new byte[]{MESSAGE_TYPE_WISELIB_DOWNSTREAM},
						packet.array()
				);

				deviceChannel.write(buffer).awaitUninterruptibly();

			} catch (Exception e) {
				log.error("" + e, e);
			}
		}
	};

	private String nodeUrn;

	private OperationQueue deviceOperationQueue;

	private Connection deviceConnection;

	private DeviceAsync device;

	public WSNDeviceAppConnectorImpl(final String nodeUrn, final String nodeType, final String nodeUSBChipID,
									 final String nodeSerialInterface, final Integer timeoutNodeAPI,
									 final Integer maximumMessageRate, final Integer timeoutReset,
									 final Integer timeoutFlash, final SchedulerService schedulerService) {

		this.nodeUrn = nodeUrn;
		this.nodeType = nodeType;
		this.nodeUSBChipID = nodeUSBChipID;
		this.nodeSerialInterface = nodeSerialInterface;
		this.schedulerService = schedulerService;

		this.maximumMessageRate = (maximumMessageRate == null ? DEFAULT_MAXIMUM_MESSAGE_RATE : maximumMessageRate);
		this.maximumMessageRateTimeUnit = TimeUnit.SECONDS;
		this.maximumMessageRateLimiter = new RateLimiterImpl(this.maximumMessageRate, 1, maximumMessageRateTimeUnit);

		this.nodeApi = new NodeApi(
				nodeApiDeviceAdapter,
				(timeoutNodeAPI == null ? DEFAULT_NODE_API_TIMEOUT : timeoutNodeAPI),
				TimeUnit.MILLISECONDS
		);
		this.timeoutReset = timeoutReset == null ? 3000 : timeoutReset;
		this.timeoutFlash = timeoutFlash == null ? 90000 : timeoutFlash;

	}

	private boolean isConnected() {
		return deviceConnection != null && deviceConnection.isConnected();
	}

	@Override
	public void destroyVirtualLink(final long targetNode, final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorImpl.destroyVirtualLink()", nodeUrn);

		if (isConnected()) {
			executorService.execute(new Runnable() {
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

	private void callCallback(final Future<NodeApiCallResult> future, final Callback listener) {
		try {

			NodeApiCallResult result = future.get();

			if (result.isSuccessful()) {
				listener.success(result.getResponse());
			} else {
				listener.failure(result.getResponseType(), result.getResponse());
			}

		} catch (InterruptedException e) {
			log.error("InterruptedException while reading Node API call result.");
			listener.failure((byte) 127, "Unknown error in testbed back-end occurred!".getBytes());
		} catch (ExecutionException e) {
			if (e.getCause() instanceof TimeoutException) {
				log.debug("{} => Call to Node API timed out.", nodeUrn);
				listener.timeout();
			} else {
				log.error("" + e, e);
				listener.failure((byte) 127, "Unknown error in testbed back-end occurred!".getBytes());
			}
		}
	}

	@Override
	public void disableNode(final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorImpl.disableNode()", nodeUrn);

		if (isConnected()) {
			executorService.execute(new Runnable() {
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

		log.debug("{} => WSNDeviceAppConnectorImpl.disablePhysicalLink()", nodeUrn);

		if (isConnected()) {
			executorService.execute(new Runnable() {
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

		log.debug("{} => WSNDeviceAppConnectorImpl.enableNode()", nodeUrn);

		if (isConnected()) {
			executorService.execute(new Runnable() {
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

		log.debug("{} => WSNDeviceAppConnectorImpl.enablePhysicalLink()", nodeUrn);

		if (isConnected()) {
			executorService.execute(new Runnable() {
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

	private boolean isFlashOperationRunningOrEnqueued() {
		for (Operation<?> operation : deviceOperationQueue.getOperations()) {
			if (operation instanceof ProgramOperation) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void flashProgram(final WSNAppMessages.Program program,
							 final FlashProgramCallback listener) {

		log.debug("{} => WSNDeviceAppConnectorImpl.executeFlashPrograms()", nodeUrn);

		if (isConnected()) {

			if (isFlashOperationRunningOrEnqueued()) {

				String msg = "There's a flash operation running or enqueued currently. Please try again later.";
				log.warn("{} => flashProgram: {}", nodeUrn, msg);
				listener.failure((byte) -1, msg.getBytes());

			} else {

				device.program(program.getProgram().toByteArray(), timeoutFlash, new AsyncAdapter<Void>() {

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
						log.error("{} => flashProgram: {}", nodeUrn, msg);
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

						log.warn("{} => flashProgram: {}", nodeUrn, msg);
						listener.failure((byte) -3, msg.getBytes());
					}

					@Override
					public void onProgressChange(final float fraction) {

						int newProgress = (int) (fraction * 100);

						if (newProgress > lastProgress) {

							log.debug("{} => Flashing progress: {}%.", nodeUrn, newProgress);
							listener.progress(fraction);
							lastProgress = newProgress;
						}
					}

					@Override
					public void onSuccess(final Void result) {

						log.debug("{} => Done flashing node.", nodeUrn);
						listener.success(null);
					}
				}
				);

			}

		} else {
			String msg = "Failed flashing node. Reason: Node is not connected.";
			log.warn("{} => {}", nodeUrn, msg);
			listener.failure((byte) -2, msg.getBytes());
		}

	}

	@Override
	public void isNodeAlive(final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorImpl.isNodeAlive()", nodeUrn);

		// to the best of our knowledge, a node is alive if we're connected to it
		if (isConnected()) {
			listener.success(null);
		} else {
			listener.failure((byte) 0, "Device is not connected.".getBytes());
		}
	}

	@Override
	public void resetNode(final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorImpl.resetNode()", nodeUrn);

		if (isConnected()) {

			device.reset(timeoutReset, new AsyncAdapter<Void>() {

				@Override
				public void onExecute() {
					resetCount = (resetCount % Integer.MAX_VALUE) == 0 ? 0 : resetCount++;
				}

				@Override
				public void onSuccess(final Void result) {
					log.debug("{} => Done resetting node.", nodeUrn);
					listener.success(null);
				}

				@Override
				public void onCancel() {
					listener.failure((byte) -1, "Operation was cancelled.".getBytes());
				}

				@Override
				public void onFailure(final Throwable throwable) {
					String msg = "Failed resetting node. Reason: " + throwable;
					log.warn("{} => resetNode(): {}", nodeUrn, msg);
					listener.failure((byte) -1, msg.getBytes());
				}
			}
			);

		} else {

			String msg = "Failed resetting node. Reason: Device is not connected.";
			log.warn("{} => {}", nodeUrn, msg);
			listener.failure((byte) 0, msg.getBytes());
		}

	}

	@Override
	public void sendMessage(final byte[] messageBytes, final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorImpl.sendMessage()", nodeUrn);

		if (isConnected()) {

			if (isVirtualLinkMessage(messageBytes)) {

				log.debug("{} => Delivering virtual link message over node API", nodeUrn);

				ByteBuffer messageBuffer = ByteBuffer.wrap(messageBytes);

				final byte RSSI = messageBuffer.get(3);
				final byte LQI = messageBuffer.get(4);
				final byte payloadLength = messageBuffer.get(5);
				final long destination = messageBuffer.getLong(6);
				final long source = messageBuffer.getLong(14);
				final byte[] payload = new byte[payloadLength];

				System.arraycopy(messageBytes, 22, payload, 0, payloadLength);

				executorService.execute(new Runnable() {
					@Override
					public void run() {
						callCallback(
								nodeApi.getInteraction().sendVirtualLinkMessage(RSSI, LQI, destination, source,
										payload
								),
								listener
						);
					}
				}
				);

			} else {

				log.debug("{} => Delivering message directly over iSenseDevice.send(), i.e. not as a virtual link "
						+ "message.", nodeUrn
				);

				final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(messageBytes);

				deviceChannel.write(buffer).addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(final ChannelFuture future) throws Exception {
						if (future.isSuccess()) {

							listener.success(null);

						} else if (future.isCancelled()) {

							String msg = "Sending message was canceled.";
							log.warn("{} => sendMessage(): {}", nodeUrn, msg);
							listener.failure((byte) -3, msg.getBytes());

						} else {

							String msg = "Failed sending message. Reason: " + future.getCause();
							log.warn("{} => sendMessage(): {}", nodeUrn, msg);
							listener.failure((byte) -2, msg.getBytes());
						}
					}
				}
				);

			}

		} else {
			listener.failure((byte) -1, "Node is not connected.".getBytes());
		}

	}

	@Override
	public void setVirtualLink(final long targetNode, final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorImpl.setVirtualLink()", nodeUrn);

		if (isConnected()) {
			executorService.execute(new Runnable() {
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

	@Override
	public void start() throws Exception {
		executorService = Executors.newCachedThreadPool();
		schedulerService.execute(connectRunnable);
		nodeApi.start();
	}

	@Override
	public void stop() {

		nodeApi.stop();

		if (device != null) {

			log.debug("{} => Shutting down {} device", nodeUrn, nodeType);

			try {

				deviceChannel.close();
				deviceChannel.getCloseFuture().awaitUninterruptibly();

			} catch (Exception e) {
				log.warn("Exception while closing DeviceConnection: {}", e);
			}

			try {
				deviceConnection.close();
			} catch (Exception e) {
				log.warn("Exception while closing DeviceConnection: {}", e);
			}

			try {
				device.close();
			} catch (Exception e) {
				log.warn("IOException while closing Device: {}", e);
			}
		}

		if (deviceOperationQueue != null) {
			deviceOperationQueue.shutdown(true);
		}

		if (executorService != null) {
			ExecutorUtils.shutdown(executorService, 1, TimeUnit.SECONDS);
		}

	}

	private boolean isVirtualLinkMessage(final byte[] messageBytes) {
		return messageBytes.length > 1 &&
				messageBytes[0] == MESSAGE_TYPE_WISELIB_DOWNSTREAM &&
				messageBytes[1] == VIRTUAL_LINK_MESSAGE;
	}
}
