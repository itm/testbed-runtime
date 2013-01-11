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
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.ByteString;
import de.uniluebeck.itm.netty.handlerstack.HandlerFactoryRegistry;
import de.uniluebeck.itm.netty.handlerstack.protocolcollection.ProtocolCollection;
import de.uniluebeck.itm.netty.handlerstack.util.ChannelBufferTools;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApi;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiCallResult;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiDeviceAdapter;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiFactory;
import de.uniluebeck.itm.tr.iwsn.pipeline.AbovePipelineLogger;
import de.uniluebeck.itm.tr.iwsn.pipeline.BelowPipelineLogger;
import de.uniluebeck.itm.tr.util.*;
import de.uniluebeck.itm.tr.util.RateLimiter;
import de.uniluebeck.itm.wsn.drivers.core.Device;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static de.uniluebeck.itm.tr.iwsn.gateway.GatewayDeviceConstants.*;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newNotificationEvent;
import static de.uniluebeck.itm.tr.iwsn.pipeline.PipelineHelper.setPipeline;
import static de.uniluebeck.itm.tr.util.StringUtils.toPrintableString;
import static org.jboss.netty.channel.Channels.pipeline;


class GatewayDeviceImpl extends AbstractService implements GatewayDevice {

	private static final Logger log = LoggerFactory.getLogger(GatewayDevice.class);

	private final DeviceConfig deviceConfig;

	private final RateLimiter maximumMessageRateLimiter;

	private final TimeDiff packetsDroppedTimeDiff = new TimeDiff(PACKETS_DROPPED_NOTIFICATION_RATE);

	private final TimeDiff pipelineMisconfigurationTimeDiff = new TimeDiff(PIPELINE_MISCONFIGURATION_NOTIFICATION_RATE);

	private Channel deviceChannel;

	private int packetsDroppedSinceLastNotification = 0;

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

	private ExecutorService deviceExecutor;

	@Inject
	public GatewayDeviceImpl(@Assisted final DeviceConfig deviceConfig,
							 @Assisted final Device device,
							 final NodeApiFactory nodeApiFactory,
							 final GatewayEventBus gatewayEventBus) {

		this.deviceConfig = checkNotNull(deviceConfig);
		this.device = checkNotNull(device);
		this.gatewayEventBus = checkNotNull(gatewayEventBus);

		checkState(device.isConnected());

		this.nodeApi = nodeApiFactory.create(
				deviceConfig.getNodeUrn().toString(),
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

		abovePipelineLogger = new AbovePipelineLogger(this.deviceConfig.getNodeUrn().toString());
		belowPipelineLogger = new BelowPipelineLogger(this.deviceConfig.getNodeUrn().toString());
	}

	@Override
	protected void doStart() {

		try {

			deviceExecutor = Executors.newCachedThreadPool(
					new ThreadFactoryBuilder().setNameFormat("GatewayDevice-" + deviceConfig.getNodeUrn()).build()
			);

			log.debug("{} => Starting {} device connector", deviceConfig.getNodeUrn(), deviceConfig.getNodeType());

			final ClientBootstrap bootstrap = new ClientBootstrap(new IOStreamChannelFactory(deviceExecutor));
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
			device.close();
			nodeApi.stop();
			ExecutorUtils.shutdown(deviceExecutor, 1, TimeUnit.SECONDS);

		} catch (Exception e) {
			notifyFailed(e);
		}

		notifyStopped();
	}

	@Override
	public ListenableFuture<NodeApiCallResult> destroyVirtualLink(final MacAddress targetMacAddress) {
		log.debug("{} => GatewayDeviceImpl.destroyVirtualLink()", deviceConfig.getNodeUrn());
		return nodeApi.destroyVirtualLink(targetMacAddress.toLong());
	}

	@Override
	public ListenableFuture<NodeApiCallResult> disableNode() {
		log.debug("{} => GatewayDeviceImpl.disableNode()", deviceConfig.getNodeUrn());
		return nodeApi.disableNode();
	}

	@Override
	public ListenableFuture<NodeApiCallResult> disablePhysicalLink(final MacAddress targetMacAddress) {
		log.debug("{} => GatewayDeviceImpl.disablePhysicalLink()", deviceConfig.getNodeUrn());
		return nodeApi.disablePhysicalLink(targetMacAddress.toLong());
	}

	@Override
	public ListenableFuture<NodeApiCallResult> enableNode() {
		log.debug("{} => GatewayDeviceImpl.enableNode()", deviceConfig.getNodeUrn());
		return nodeApi.enableNode();
	}

	@Override
	public ListenableFuture<NodeApiCallResult> enablePhysicalLink(final MacAddress targetMacAddress) {
		log.debug("{} => GatewayDeviceImpl.enablePhysicalLink()", deviceConfig.getNodeUrn());
		return nodeApi.enablePhysicalLink(targetMacAddress.toLong());
	}

	@Override
	public ProgressListenableFuture<Void> flashProgram(final byte[] binaryImage) {

		log.debug("{} => GatewayDeviceImpl.executeFlashPrograms()", deviceConfig.getNodeUrn());

		final ProgressSettableFuture<Void> future = ProgressSettableFuture.create();

		if (isConnected()) {

			if (flashOperationRunningOrEnqueued) {

				String msg = "There's a flash operation running or enqueued currently. Please try again later.";
				log.warn("{} => flashProgram: {}", deviceConfig.getNodeUrn(), msg);
				future.setException(new RuntimeException(msg));
				return future;

			} else {

				flashOperationRunningOrEnqueued = true;

				deviceLock.lock();
				try {
					device.program(binaryImage, deviceConfig.getTimeoutFlashMillis(),
							new OperationAdapter<Void>() {

								private int lastProgress = -1;

								@Override
								public void onExecute() {
									lastProgress = 0;
									future.setProgress(0f);
								}

								@Override
								public void onCancel() {

									String msg = "Flash operation was canceled.";
									log.error("{} => flashProgram: {}", deviceConfig.getNodeUrn(), msg);
									flashOperationRunningOrEnqueued = false;
									future.setException(new RuntimeException(msg));
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
									future.setException(new RuntimeException(msg));
								}

								@Override
								public void onProgressChange(final float fraction) {

									int newProgress = (int) (fraction * 100);

									if (newProgress > lastProgress) {

										log.debug("{} => Flashing progress: {}%.",
												deviceConfig.getNodeUrn(),
												newProgress
										);
										future.setProgress(fraction);
										lastProgress = newProgress;
									}
								}

								@Override
								public void onSuccess(final Void result) {

									log.debug("{} => Done flashing node.", deviceConfig.getNodeUrn());
									flashOperationRunningOrEnqueued = false;
									future.set(null);
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
			future.setException(new RuntimeException(msg));
		}

		return future;
	}

	@Override
	public ListenableFuture<Boolean> isNodeAlive() {

		log.debug("{} => GatewayDeviceImpl.isNodeAlive()", deviceConfig.getNodeUrn());

		final SettableFuture<Boolean> future = SettableFuture.create();

		if (isConnected()) {

			deviceLock.lock();
			try {
				device.isNodeAlive(deviceConfig.getTimeoutCheckAliveMillis(), new OperationAdapter<Boolean>() {

					@Override
					public void onExecute() {
						// nothing to do
					}

					@Override
					public void onSuccess(final Boolean result) {
						log.debug("{} => Done checking node alive (result={}).", deviceConfig.getNodeUrn(), result);
						future.set(result);
					}

					@Override
					public void onCancel() {
						final String msg = "Operation was cancelled.";
						log.warn("{} => isNodeAlive(): {}", deviceConfig.getNodeUrn(), msg);
						future.setException(new RuntimeException(msg));
					}

					@Override
					public void onFailure(final Throwable throwable) {
						String msg = "Failed checking if node is alive. Reason: " + throwable;
						log.warn("{} => resetNode(): {}", deviceConfig.getNodeUrn(), msg);
						future.setException(new RuntimeException(msg));
					}
				}
				);
			} finally {
				deviceLock.unlock();
			}

		} else {

			String msg = "Failed checking if node is alive. Reason: Device is not connected.";
			log.warn("{} => {}", deviceConfig.getNodeUrn(), msg);
			future.setException(new RuntimeException(msg));
		}

		return future;
	}

	@Override
	public ListenableFuture<Boolean> isNodeConnected() {
		log.debug("{} => GatewayDeviceImpl.isNodeConnected()", deviceConfig.getNodeUrn());
		return immediateFuture(isConnected());
	}

	@Override
	public ListenableFuture<Void> resetNode() {

		log.debug("{} => GatewayDeviceImpl.resetNode()", deviceConfig.getNodeUrn());
		final SettableFuture<Void> future = SettableFuture.create();

		if (isConnected()) {

			deviceLock.lock();
			try {
				device.reset(deviceConfig.getTimeoutResetMillis(), new OperationAdapter<Void>() {

					@Override
					public void onExecute() {
						// nothing to do
					}

					@Override
					public void onSuccess(final Void result) {
						log.debug("{} => Done resetting node.", deviceConfig.getNodeUrn());
						future.set(null);
					}

					@Override
					public void onCancel() {
						final String msg = "Operation was cancelled.";
						log.warn("{} => resetNode(): {}", deviceConfig.getNodeUrn(), msg);
						future.setException(new RuntimeException(msg));
					}

					@Override
					public void onFailure(final Throwable throwable) {
						String msg = "Failed resetting node. Reason: " + throwable;
						log.warn("{} => resetNode(): {}", deviceConfig.getNodeUrn(), msg);
						future.setException(new RuntimeException(msg));
					}
				}
				);
			} finally {
				deviceLock.unlock();
			}

		} else {

			String msg = "Failed resetting node. Reason: Device is not connected.";
			log.warn("{} => {}", deviceConfig.getNodeUrn(), msg);
			future.setException(new RuntimeException(msg));
		}

		return future;
	}

	@Override
	public ListenableFuture<Void> sendMessage(final byte[] messageBytes) {

		log.debug("{} => GatewayDeviceImpl.sendMessage()", deviceConfig.getNodeUrn());
		final SettableFuture<Void> future = SettableFuture.create();

		if (!isConnected()) {

			final String msg = "Node is not connected.";
			log.warn("{} => sendMessage(): ", deviceConfig.getNodeUrn(), msg);
			future.setException(new RuntimeException(msg));
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
			public void operationComplete(final ChannelFuture channelFuture) throws Exception {

				if (channelFuture.isSuccess()) {

					future.set(null);

				} else if (channelFuture.isCancelled()) {

					final String msg = "Sending message was canceled.";
					log.warn("{} => sendMessage(): {}", deviceConfig.getNodeUrn(), msg);
					future.setException(new RuntimeException(msg));

				} else {

					final Throwable cause = channelFuture.getCause();
					final String msg = "Failed sending message. Reason: " + cause;
					log.warn("{} => sendMessage(): {}", deviceConfig.getNodeUrn(), msg);
					future.setException(new RuntimeException(cause));
				}
			}
		}
		);

		return future;
	}

	@Override
	public ListenableFuture<Void> setDefaultChannelPipeline() {
		try {
			List<Tuple<String, ChannelHandler>> innerPipelineHandlers = createDefaultInnerPipelineHandlers();
			setPipeline(deviceChannel.getPipeline(), createPipelineHandlers(innerPipelineHandlers));
			log.debug("{} => Channel pipeline now set to: {}", deviceConfig.getNodeUrn(), innerPipelineHandlers);
			return immediateFuture(null);
		} catch (Exception e) {
			log.warn("Exception while setting default channel pipeline: {}", e);
			return Futures.immediateFailedFuture(new RuntimeException(e));
		}
	}

	@Override
	public ListenableFuture<Void> setChannelPipeline(
			final List<Tuple<String, Multimap<String, String>>> channelHandlerConfigs) {

		final SettableFuture<Void> future = SettableFuture.create();

		if (isConnected()) {

			List<Tuple<String, ChannelHandler>> innerPipelineHandlers;

			try {

				log.debug("{} => Setting channel pipeline using configuration: {}", deviceConfig.getNodeUrn(),
						channelHandlerConfigs
				);

				innerPipelineHandlers = handlerFactoryRegistry.create(channelHandlerConfigs);
				setPipeline(deviceChannel.getPipeline(), createPipelineHandlers(innerPipelineHandlers));

				log.debug("{} => Channel pipeline now set to: {}", deviceConfig.getNodeUrn(), innerPipelineHandlers);
				future.set(null);

			} catch (Exception e) {

				log.warn("{} => {} while setting channel pipeline: {}",
						deviceConfig.getNodeUrn(), e.getClass().getSimpleName(), e.getMessage()
				);
				future.setException(new RuntimeException(e));

				log.warn("{} => Resetting channel pipeline to default pipeline.", deviceConfig.getNodeUrn());
				setDefaultChannelPipeline();
			}

		} else {
			final String msg = "Node is not connected.";
			log.warn("{} => setChannelPipeline(): {}", deviceConfig.getNodeUrn(), msg);
			future.setException(new RuntimeException(msg));
		}

		return future;
	}

	private SimpleChannelHandler forwardingHandler = new SimpleChannelHandler() {
		@Override
		public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

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

			//noinspection ThrowableResultOfMethodCallIgnored
			if (e.getCause() instanceof IOException && "Pipe broken".equals(e.getCause().getMessage())) {

				log.trace("{} => Expected exception in pipeline: {}", deviceConfig.getNodeUrn(), e);

			} else {

				log.warn("{} => Exception in pipeline: {}", deviceConfig.getNodeUrn(), e);

				@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
				String notification = "The pipeline seems to be wrongly configured. A(n) " +
						e.getCause().getClass().getSimpleName() +
						" was caught and contained the following message: " +
						e.getCause().getMessage();

				sendPipelineMisconfigurationIfNotificationRateAllows(notification);
			}
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
						.setSourceNodeUrn(deviceConfig.getNodeUrn().toString())
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
			gatewayEventBus.post(newNotificationEvent(deviceConfig.getNodeUrn(), notification));
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

			gatewayEventBus.post(newNotificationEvent(deviceConfig.getNodeUrn(), notification));

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
	public ListenableFuture<NodeApiCallResult> setVirtualLink(final MacAddress targetMacAddress) {
		log.debug("{} => GatewayDeviceImpl.setVirtualLink()", deviceConfig.getNodeUrn());
		return nodeApi.setVirtualLink(targetMacAddress.toLong());
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

	private boolean isConnected() {
		deviceLock.lock();
		try {
			return device.isConnected();
		} finally {
			deviceLock.unlock();
		}
	}
}
