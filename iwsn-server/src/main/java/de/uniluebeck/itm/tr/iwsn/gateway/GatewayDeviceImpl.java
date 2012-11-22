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

package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.ByteString;
import de.uniluebeck.itm.netty.handlerstack.HandlerFactoryRegistry;
import de.uniluebeck.itm.netty.handlerstack.protocolcollection.ProtocolCollection;
import de.uniluebeck.itm.netty.handlerstack.util.ChannelBufferTools;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApi;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiCallResult;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiDeviceAdapter;
import de.uniluebeck.itm.tr.iwsn.pipeline.AbovePipelineLogger;
import de.uniluebeck.itm.tr.iwsn.pipeline.BelowPipelineLogger;
import de.uniluebeck.itm.tr.util.*;
import de.uniluebeck.itm.wsn.drivers.core.Device;
import de.uniluebeck.itm.wsn.drivers.core.exception.ProgramChipMismatchException;
import de.uniluebeck.itm.wsn.drivers.core.operation.OperationAdapter;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.iostream.IOStreamAddress;
import org.jboss.netty.channel.iostream.IOStreamChannelFactory;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static de.uniluebeck.itm.tr.iwsn.gateway.GatewayDeviceConstants.*;
import static de.uniluebeck.itm.tr.iwsn.pipeline.PipelineHelper.setPipeline;
import static de.uniluebeck.itm.tr.util.StringUtils.toPrintableString;
import static org.jboss.netty.channel.Channels.pipeline;


class GatewayDeviceImpl extends AbstractService implements GatewayDevice {

	private static final Logger log = LoggerFactory.getLogger(GatewayDevice.class);

	private final DeviceConfig deviceConfig;

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

	private transient boolean flashOperationRunningOrEnqueued = false;

	private final HandlerFactoryRegistry handlerFactoryRegistry = new HandlerFactoryRegistry();

	private final NodeApi nodeApi;

	private final NodeApiDeviceAdapter nodeApiDeviceAdapter = new NodeApiDeviceAdapter() {
		@Override
		public void sendToNode(final ByteBuffer packet) {
			try {
				if (log.isDebugEnabled()) {
					log.debug(
							"{} => Sending a WISELIB_DOWNSTREAM packet: {}",
							deviceConfig.getNodeUrn(),
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

	private final Device device;

	private final Lock deviceLock = new ReentrantLock();

	private final AbovePipelineLogger abovePipelineLogger;

	private final BelowPipelineLogger belowPipelineLogger;

	private final GatewayEventBus gatewayEventBus;

	@Inject
	public GatewayDeviceImpl(@Assisted final DeviceConfig deviceConfig,
							 @Assisted final Device device,
							 final GatewayEventBus gatewayEventBus) {

		this.deviceConfig = checkNotNull(deviceConfig);
		this.device = checkNotNull(device);
		this.gatewayEventBus = checkNotNull(gatewayEventBus);

		checkState(device.isConnected());

		this.nodeApi = new NodeApi(
				deviceConfig.getNodeUrn(),
				nodeApiDeviceAdapter,
				deviceConfig.getTimeoutNodeApiMillis(),
				TimeUnit.MILLISECONDS
		);

		this.maximumMessageRateLimiter = new RateLimiterImpl(
				deviceConfig.getMaximumMessageRate(),
				1,
				TimeUnit.SECONDS
		);

		try {
			ProtocolCollection.registerProtocols(handlerFactoryRegistry);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		abovePipelineLogger = new AbovePipelineLogger(this.deviceConfig.getNodeUrn());
		belowPipelineLogger = new BelowPipelineLogger(this.deviceConfig.getNodeUrn());
	}

	private void sendNotification(final String nodeUrn, final String msg) {

		final NotificationEvent notificationEvent = NotificationEvent.newBuilder()
				.setNodeUrn(nodeUrn)
				.setMessage(msg)
				.setTimestamp(new DateTime().getMillis())
				.build();

		gatewayEventBus.post(notificationEvent);
	}

	@Override
	protected void doStart() {

		try {

			log.debug("{} => Starting {} device connector", deviceConfig.getNodeUrn(), deviceConfig.getNodeType());

			final ClientBootstrap bootstrap = new ClientBootstrap(new IOStreamChannelFactory(nodeApiExecutor));
			bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
				@Override
				public ChannelPipeline getPipeline() throws Exception {
					return setPipeline(pipeline(), createPipelineHandlers(createDefaultInnerPipelineHandlers()));
				}
			}
			);

			final IOStreamAddress address = new IOStreamAddress(device.getInputStream(), device.getOutputStream());
			final ChannelFuture connectFuture = bootstrap.connect(address);

			try {
				deviceChannel = connectFuture.await().getChannel();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			nodeApiExecutor = Executors.newCachedThreadPool();
			nodeApi.start();

		} catch (Exception e) {
			notifyFailed(e);
		}

		notifyStarted();
	}

	@Override
	protected void doStop() {

		try {

			log.debug("{} => Shutting down {} device connector",
					deviceConfig.getNodeUrn(),
					deviceConfig.getNodeType()
			);

			shutdownDeviceChannel();
			nodeApi.stop();
			ExecutorUtils.shutdown(nodeApiExecutor, 1, TimeUnit.SECONDS);

		} catch (Exception e) {
			notifyFailed(e);
		}

		notifyStopped();
	}

	@Override
	public void destroyVirtualLink(final long targetNode, final Callback listener) {

		log.debug("{} => GatewayDeviceImpl.destroyVirtualLink()", deviceConfig.getNodeUrn());

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

		log.debug("{} => GatewayDeviceImpl.disableNode()", deviceConfig.getNodeUrn());

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

		log.debug("{} => GatewayDeviceImpl.disablePhysicalLink()", deviceConfig.getNodeUrn());

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

		log.debug("{} => GatewayDeviceImpl.enableNode()", deviceConfig.getNodeUrn());

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

		log.debug("{} => GatewayDeviceImpl.enablePhysicalLink()", deviceConfig.getNodeUrn());

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
	public void flashProgram(final byte[] binaryImage,
							 final FlashProgramCallback listener) {

		log.debug("{} => GatewayDeviceImpl.executeFlashPrograms()", deviceConfig.getNodeUrn());

		if (isConnected()) {

			if (flashOperationRunningOrEnqueued) {

				String msg = "There's a flash operation running or enqueued currently. Please try again later.";
				log.warn("{} => flashProgram: {}", deviceConfig.getNodeUrn(), msg);
				listener.failure((byte) -1, msg.getBytes());

			} else {

				flashOperationRunningOrEnqueued = true;

				deviceLock.lock();
				try {
					device.program(binaryImage, deviceConfig.getTimeoutFlashMillis(),
							new OperationAdapter<Void>() {

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
									log.error("{} => flashProgram: {}", deviceConfig.getNodeUrn(), msg);
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

									log.warn("{} => flashProgram: {}", deviceConfig.getNodeUrn(), msg);
									flashOperationRunningOrEnqueued = false;
									listener.failure((byte) -3, msg.getBytes());
								}

								@Override
								public void onProgressChange(final float fraction) {

									int newProgress = (int) (fraction * 100);

									if (newProgress > lastProgress) {

										log.debug("{} => Flashing progress: {}%.",
												deviceConfig.getNodeUrn(),
												newProgress
										);
										listener.progress(fraction);
										lastProgress = newProgress;
									}
								}

								@Override
								public void onSuccess(final Void result) {

									log.debug("{} => Done flashing node.", deviceConfig.getNodeUrn());
									flashOperationRunningOrEnqueued = false;
									listener.success(null);
								}
							}
					);
				} finally {
					deviceLock.unlock();
				}

			}

		} else {
			String msg = "Failed flashing node. Reason: Node is not connected.";
			log.warn("{} => {}", deviceConfig.getNodeUrn(), msg);
			listener.failure((byte) -2, msg.getBytes());
		}

	}

	@Override
	public void isNodeAlive(final Callback listener) {

		log.debug("{} => GatewayDeviceImpl.isNodeAlive()", deviceConfig.getNodeUrn());

		if (isConnected()) {

			deviceLock.lock();
			try {
				device.isNodeAlive(deviceConfig.getTimeoutCheckAliveMillis(), new OperationAdapter<Boolean>() {

					@Override
					public void onExecute() {
						resetCount = (resetCount % Integer.MAX_VALUE) == 0 ? 0 : resetCount++;
					}

					@Override
					public void onSuccess(final Boolean result) {
						log.debug("{} => Done checking node alive (result={}).", deviceConfig.getNodeUrn(), result);
						listener.success(null);
					}

					@Override
					public void onCancel() {
						listener.failure((byte) -1, "Operation was cancelled.".getBytes());
					}

					@Override
					public void onFailure(final Throwable throwable) {
						String msg = "Failed checking if node is alive. Reason: " + throwable;
						log.warn("{} => resetNode(): {}", deviceConfig.getNodeUrn(), msg);
						listener.failure((byte) -1, msg.getBytes());
					}
				}
				);
			} finally {
				deviceLock.unlock();
			}

		} else {

			String msg = "Failed checking if node is alive. Reason: Device is not connected.";
			log.warn("{} => {}", deviceConfig.getNodeUrn(), msg);
			listener.failure((byte) 0, msg.getBytes());
		}
	}

	@Override
	public void isNodeAliveSm(final Callback listener) {

		log.debug("{} => GatewayDeviceImpl.isNodeAliveSm()", deviceConfig.getNodeUrn());

		if (isConnected()) {
			listener.success(null);
		} else {
			listener.failure((byte) 0, "Device is not connected.".getBytes());
		}
	}

	@Override
	public void resetNode(final Callback listener) {

		log.debug("{} => GatewayDeviceImpl.resetNode()", deviceConfig.getNodeUrn());

		if (isConnected()) {

			deviceLock.lock();
			try {
				device.reset(deviceConfig.getTimeoutResetMillis(), new OperationAdapter<Void>() {

					@Override
					public void onExecute() {
						resetCount = (resetCount % Integer.MAX_VALUE) == 0 ? 0 : resetCount++;
					}

					@Override
					public void onSuccess(final Void result) {
						log.debug("{} => Done resetting node.", deviceConfig.getNodeUrn());
						listener.success(null);
					}

					@Override
					public void onCancel() {
						listener.failure((byte) -1, "Operation was cancelled.".getBytes());
					}

					@Override
					public void onFailure(final Throwable throwable) {
						String msg = "Failed resetting node. Reason: " + throwable;
						log.warn("{} => resetNode(): {}", deviceConfig.getNodeUrn(), msg);
						listener.failure((byte) -1, msg.getBytes());
					}
				}
				);
			} finally {
				deviceLock.unlock();
			}

		} else {

			String msg = "Failed resetting node. Reason: Device is not connected.";
			log.warn("{} => {}", deviceConfig.getNodeUrn(), msg);
			listener.failure((byte) 0, msg.getBytes());
		}

	}

	@Override
	public void sendMessage(final byte[] messageBytes, final Callback callback) {

		log.debug("{} => GatewayDeviceImpl.sendMessage()", deviceConfig.getNodeUrn());

		if (!isConnected()) {
			callback.failure((byte) -1, "Node is not connected.".getBytes());
			return;
		}

		final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(messageBytes);

		if (log.isDebugEnabled()) {
			log.debug("{} => Delivering message: ",
					deviceConfig.getNodeUrn(),
					ChannelBufferTools.toPrintableString(buffer, 200)
			);
		}

		deviceChannel.write(buffer).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(final ChannelFuture future) throws Exception {

				if (future.isSuccess()) {

					callback.success(null);

				} else if (future.isCancelled()) {

					String msg = "Sending message was canceled.";
					log.warn("{} => sendMessage(): {}", deviceConfig.getNodeUrn(), msg);
					callback.failure((byte) -3, msg.getBytes());

				} else {

					@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
					String msg = "Failed sending message. Reason: " + future.getCause();
					log.warn("{} => sendMessage(): {}", deviceConfig.getNodeUrn(), msg);
					callback.failure((byte) -2, msg.getBytes());
				}
			}
		}
		);
	}

	@Override
	public void setDefaultChannelPipeline(@Nullable final Callback callback) {

		try {

			List<Tuple<String, ChannelHandler>> innerPipelineHandlers = createDefaultInnerPipelineHandlers();
			setPipeline(deviceChannel.getPipeline(), createPipelineHandlers(innerPipelineHandlers));

			log.debug("{} => Channel pipeline now set to: {}", deviceConfig.getNodeUrn(), innerPipelineHandlers);

			if (callback != null) {
				callback.success(null);
			}

		} catch (Exception e) {

			log.warn("Exception while setting default channel pipeline: {}", e);

			if (callback != null) {
				callback.failure(
						(byte) -1,
						("Exception while setting channel pipeline: " + e.getMessage()).getBytes()
				);
			}
		}
	}

	@Override
	public void setChannelPipeline(final List<Tuple<String, Multimap<String, String>>> channelHandlerConfigurations,
								   final Callback callback) {

		if (isConnected()) {

			List<Tuple<String, ChannelHandler>> innerPipelineHandlers;

			try {

				log.debug("{} => Setting channel pipeline using configuration: {}", deviceConfig.getNodeUrn(),
						channelHandlerConfigurations
				);

				innerPipelineHandlers = handlerFactoryRegistry.create(channelHandlerConfigurations);
				setPipeline(deviceChannel.getPipeline(), createPipelineHandlers(innerPipelineHandlers));

				log.debug("{} => Channel pipeline now set to: {}", deviceConfig.getNodeUrn(), innerPipelineHandlers);

				callback.success(null);

			} catch (Exception e) {

				log.warn("{} => {} while setting channel pipeline: {}",
						deviceConfig.getNodeUrn(), e.getClass().getSimpleName(), e.getMessage()
				);

				callback.failure(
						(byte) -1,
						("Exception while setting channel pipeline: " + e.getMessage()).getBytes()
				);

				log.warn("{} => Resetting channel pipeline to default pipeline.", deviceConfig.getNodeUrn());

				setDefaultChannelPipeline(null);
			}

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

			log.warn("{} => Exception in pipeline: {}", deviceConfig.getNodeUrn(), e);

			@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
			String notification = "The pipeline seems to be wrongly configured. A(n) " +
					e.getCause().getClass().getSimpleName() +
					" was caught and contained the following message: " +
					e.getCause().getMessage();

			sendPipelineMisconfigurationIfNotificationRateAllows(notification);
		}
	};

	@VisibleForTesting
	void onBytesReceivedFromDevice(final ChannelBuffer buffer) {

		//if reaching maximum-message-rate do not send more then 1 message
		if (!maximumMessageRateLimiter.checkIfInSlotAndCount()) {
			int dismissedCount = maximumMessageRateLimiter.dismissedCount();
			if (dismissedCount >= 1) {
				sendPacketsDroppedWarningIfNotificationRateAllows(dismissedCount);
			}
			return;
		}

		if (log.isTraceEnabled()) {
			log.trace("{} => GatewayDeviceImpl.onBytesReceivedFromDevice: {}",
					deviceConfig.getNodeUrn(),
					ChannelBufferTools.toPrintableString(buffer, 200)
			);
		}

		// pass output to all listeners
		byte[] bytes = new byte[buffer.readableBytes()];
		buffer.getBytes(0, bytes);

		final de.uniluebeck.itm.tr.iwsn.messages.UpstreamMessageEvent event =
				de.uniluebeck.itm.tr.iwsn.messages.UpstreamMessageEvent.newBuilder()
						.setMessageBytes(ByteString.copyFrom(bytes))
						.setSourceNodeUrn(deviceConfig.getNodeUrn())
						.setTimestamp(new DateTime().getMillis())
						.build();

		gatewayEventBus.post(event);

		// additionally pass to Node API in case it awaits a response
		boolean isWiselibUpstream =
				(buffer.getByte(0) & 0xFF) == MESSAGE_TYPE_WISELIB_UPSTREAM;

		boolean isByteTextOrVLink = buffer.readableBytes() > 1 &&
				((buffer.getByte(1) & 0xFF) == NODE_OUTPUT_BYTE ||
						(buffer.getByte(1) & 0xFF) == NODE_OUTPUT_TEXT ||
						(buffer.getByte(1) & 0xFF) == NODE_OUTPUT_VIRTUAL_LINK);

		boolean isWiselibReply = isWiselibUpstream && !isByteTextOrVLink;

		if (isWiselibReply) {

			final ByteBuffer packetWithoutISenseMessageType = ByteBuffer.allocate(buffer.readableBytes() - 1);
			buffer.getBytes(1, packetWithoutISenseMessageType.array(), 0, buffer.readableBytes() - 1);

			final boolean isConsumedByNodeApi = nodeApiDeviceAdapter.receiveFromNode(packetWithoutISenseMessageType);

			if (isConsumedByNodeApi && log.isDebugEnabled()) {
				log.debug("{} => Received WISELIB_UPSTREAM packet with content: {}",
						deviceConfig.getNodeUrn(),
						ChannelBufferTools.toPrintableString(buffer, 200)
				);
			}

		}
	}

	private void sendPipelineMisconfigurationIfNotificationRateAllows(String notification) {
		if (pipelineMisconfigurationTimeDiff.isTimeout()) {
			sendNotification(deviceConfig.getNodeUrn(), notification);
			pipelineMisconfigurationTimeDiff.touch();
		}
	}

	private void sendPacketsDroppedWarningIfNotificationRateAllows(int packetsDropped) {

		packetsDroppedSinceLastNotification += packetsDropped;

		if (packetsDroppedTimeDiff.isTimeout()) {

			String notification =
					"Dropped " + packetsDroppedSinceLastNotification + " packets of " + deviceConfig.getNodeUrn() +
							" in the last " + packetsDroppedTimeDiff.ms() + " milliseconds, because the node writes "
							+ "more packets to the serial interface per second than allowed (" +
							deviceConfig.getMaximumMessageRate() + " per second).";

			sendNotification(deviceConfig.getNodeUrn(), notification);

			packetsDroppedSinceLastNotification = 0;
			packetsDroppedTimeDiff.touch();
		}
	}

	@Nonnull
	private List<Tuple<String, ChannelHandler>> createPipelineHandlers(
			@Nonnull List<Tuple<String, ChannelHandler>> innerHandlers) {

		LinkedList<Tuple<String, ChannelHandler>> handlers = newLinkedList();

		handlers.addFirst(new Tuple<String, ChannelHandler>("forwardingHandler", forwardingHandler));

		boolean doLogging = log.isTraceEnabled() && !innerHandlers.isEmpty();

		if (doLogging) {
			handlers.addFirst(new Tuple<String, ChannelHandler>("aboveFilterPipelineLogger", abovePipelineLogger));
		}

		for (Tuple<String, ChannelHandler> innerHandler : innerHandlers) {
			handlers.addFirst(innerHandler);
		}

		if (doLogging) {
			handlers.addFirst(new Tuple<String, ChannelHandler>("belowFilterPipelineLogger", belowPipelineLogger));
		}

		return handlers;

	}

	@Nonnull
	private List<Tuple<String, ChannelHandler>> createDefaultInnerPipelineHandlers() {

		boolean defaultChannelPipelineConfigurationFileExists =
				deviceConfig.getDefaultChannelPipelineConfigurationFile() != null;

		return defaultChannelPipelineConfigurationFileExists ?
				createDefaultInnerPipelineHandlersFromConfigurationFile() :
				Lists.<Tuple<String, ChannelHandler>>newArrayList();
	}

	private List<Tuple<String, ChannelHandler>> createDefaultInnerPipelineHandlersFromConfigurationFile() {

		try {
			return handlerFactoryRegistry.create(deviceConfig.getDefaultChannelPipelineConfigurationFile());
		} catch (Exception e) {
			log.warn(
					"Exception while creating default channel pipeline from configuration file ({}). "
							+ "Using empty pipeline as default pipeline. "
							+ "Error message: {}. Stack trace: {}",
					deviceConfig.getDefaultChannelPipelineConfigurationFile(),
					e.getMessage(),
					Throwables.getStackTraceAsString(e)
			);
			return newArrayList();
		}
	}

	@Override
	public void setVirtualLink(final long targetNode, final Callback listener) {

		log.debug("{} => GatewayDeviceImpl.setVirtualLink()", deviceConfig.getNodeUrn());

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
				log.warn("{} => Exception while closing DeviceConnection!", deviceConfig.getNodeUrn(), e);
			}
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
			log.error("{} => InterruptedException while reading Node API call result.", deviceConfig.getNodeUrn());
			listener.failure((byte) 127, "Unknown error in testbed back-end occurred!".getBytes());
		} catch (ExecutionException e) {
			if (e.getCause() instanceof TimeoutException) {
				log.debug("{} => Call to Node API timed out.", deviceConfig.getNodeUrn());
				listener.timeout();
			} else {
				log.error("" + e, e);
				listener.failure((byte) 127, "Unknown error in testbed back-end occurred!".getBytes());
			}
		}
	}

	private boolean isConnected() {
		deviceLock.lock();
		try {
			return device != null && device.isConnected();
		} finally {
			deviceLock.unlock();
		}
	}
}
