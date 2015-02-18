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
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfig;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfigList;
import de.uniluebeck.itm.nettyprotocols.HandlerFactory;
import de.uniluebeck.itm.nettyprotocols.NamedChannelHandlerList;
import de.uniluebeck.itm.nettyprotocols.util.ChannelBufferTools;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceFoundEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceLostEvent;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApi;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiCallResult;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiDeviceAdapter;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiFactory;
import de.uniluebeck.itm.tr.iwsn.pipeline.AbovePipelineLogger;
import de.uniluebeck.itm.tr.iwsn.pipeline.BelowPipelineLogger;
import de.uniluebeck.itm.util.TimeDiff;
import de.uniluebeck.itm.util.concurrent.ProgressListenableFuture;
import de.uniluebeck.itm.util.concurrent.ProgressSettableFuture;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.wsn.drivers.core.Device;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import de.uniluebeck.itm.wsn.drivers.core.operation.OperationAdapter;
import de.uniluebeck.itm.wsn.drivers.core.operation.OperationFuture;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceType;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.iostream.IOStreamAddress;
import org.jboss.netty.channel.iostream.IOStreamChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static de.uniluebeck.itm.tr.iwsn.pipeline.PipelineHelper.setPipeline;
import static de.uniluebeck.itm.util.StringUtils.toPrintableString;
import static de.uniluebeck.itm.util.concurrent.ExecutorUtils.shutdown;
import static org.jboss.netty.channel.Channels.pipeline;


class SingleDeviceAdapter extends SingleDeviceAdapterBase {

	public static final int MESSAGE_TYPE_WISELIB_DOWNSTREAM = 10;

	public static final int MESSAGE_TYPE_WISELIB_UPSTREAM = 105;

	public static final byte NODE_OUTPUT_TEXT = 50;

	public static final byte NODE_OUTPUT_BYTE = 51;

	public static final byte NODE_OUTPUT_VIRTUAL_LINK = 52;

	public static final int PIPELINE_MISCONFIGURATION_NOTIFICATION_RATE = 5000;

	private static final Logger log = LoggerFactory.getLogger(DeviceAdapter.class);

	private static final int TIMEOUT_READMAC_MILLIS = 60 * 1000;

	private static final int TIMEOUT_WRITEMAC_MILLIS = 60 * 1000;

	private final TimeDiff pipelineMisconfigurationTimeDiff = new TimeDiff(PIPELINE_MISCONFIGURATION_NOTIFICATION_RATE);

	private final DeviceFactory deviceFactory;

	private final NodeApiFactory nodeApiFactory;

	private final ImmutableMap<String, HandlerFactory> handlerFactories;

	private final GatewayEventBus gatewayEventBus;

	private final SchedulerService schedulerService;

	private Channel deviceChannel;

	private transient boolean flashOperationRunningOrEnqueued = false;

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

	private final Lock deviceLock = new ReentrantLock();

	private final AbovePipelineLogger abovePipelineLogger;

	private final BelowPipelineLogger belowPipelineLogger;

	private NodeApi nodeApi;

	private Device device;

	private ExecutorService deviceExecutor;

	private ChannelHandlerConfigList currentPipeline;

	@Inject
	public SingleDeviceAdapter(final String deviceType,
							   final String devicePort,
							   final Map<String, String> deviceConfiguration,
							   final GatewayEventBus gatewayEventBus,
							   @Nullable final DeviceConfig deviceConfig,
							   final DeviceFactory deviceFactory,
							   final NodeApiFactory nodeApiFactory,
							   final Set<HandlerFactory> handlerFactories,
							   final SchedulerService schedulerService) {

		super(deviceType, devicePort, deviceConfiguration, deviceConfig);
		this.gatewayEventBus = gatewayEventBus;
		this.schedulerService = schedulerService;

		this.deviceFactory = checkNotNull(deviceFactory);
		this.nodeApiFactory = checkNotNull(nodeApiFactory);

		final ImmutableMap.Builder<String, HandlerFactory> handlerFactoriesBuilder = ImmutableMap.builder();
		for (HandlerFactory handlerFactory : checkNotNull(handlerFactories)) {
			handlerFactoriesBuilder.put(handlerFactory.getName(), handlerFactory);
		}
		this.handlerFactories = handlerFactoriesBuilder.build();

		this.abovePipelineLogger = new AbovePipelineLogger(this.deviceConfig.getNodeUrn().toString());
		this.belowPipelineLogger = new BelowPipelineLogger(this.deviceConfig.getNodeUrn().toString());
		this.currentPipeline = this.deviceConfig.getDefaultChannelPipeline();
	}

	@Override
	protected void doStart() {

		try {

			log.debug("{} => Starting {} device connector", deviceConfig.getNodeUrn(), deviceConfig.getNodeType());

			deviceExecutor = Executors.newCachedThreadPool(
					new ThreadFactoryBuilder().setNameFormat("DeviceAdapter-" + deviceConfig.getNodeUrn())
							.build()
			);

			device = deviceFactory.create(
					deviceExecutor,
					deviceConfig.getNodeType(),
					deviceConfig.getNodeConfiguration()
			);

			device.connect(devicePort);

			checkForBrokenMacAndRepair();

			log.info("{} => Successfully connected to {} device on serial port {}",
					deviceConfig.getNodeUrn(), deviceConfig.getNodeType(), devicePort
			);

			this.nodeApi = nodeApiFactory.create(
					deviceConfig.getNodeUrn().toString(),
					nodeApiDeviceAdapter,
					deviceConfig.getTimeoutNodeApiMillis(),
					TimeUnit.MILLISECONDS
			);

			final ClientBootstrap bootstrap = new ClientBootstrap(new IOStreamChannelFactory(deviceExecutor));
			bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
											 @Override
											 public ChannelPipeline getPipeline() throws Exception {
												 currentPipeline = deviceConfig.getDefaultChannelPipeline();
												 return setPipeline(
														 pipeline(),
														 createPipelineHandlers(createHandlers(currentPipeline))
												 );
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

			setDefaultChannelPipeline();
			nodeApi.startAsync().awaitRunning();

			fireDevicesConnected(deviceConfig.getNodeUrn());

			schedulerService.execute(new Runnable() {
										 @Override
										 public void run() {
											 resetNode();
										 }
									 }
			);

		} catch (Exception e) {
			notifyFailed(e);
		}

		notifyStarted();
	}

	private void checkForBrokenMacAndRepair() throws ExecutionException, InterruptedException {
		deviceLock.lock();
		try {

			final MacAddress actualMacAddress;

			try {
				actualMacAddress =
						device.readMac(TIMEOUT_READMAC_MILLIS, null).get(TIMEOUT_READMAC_MILLIS, TimeUnit.MILLISECONDS);
			} catch (ExecutionException e) {
				if (e.getCause() instanceof UnsupportedOperationException) {
					log.trace(
							"Reading MAC addresses is not supported on device type \"{}\". Omitting MAC address check.",
							deviceType
					);
					return;
				}
				throw propagate(e);
			} catch (TimeoutException e) {
				log.warn("TimeoutException while reading MAC address");
				return;
			}

			final MacAddress configuredMacAddress = new MacAddress(deviceConfig.getNodeUrn().getSuffix());

			if (!actualMacAddress.equals(configuredMacAddress)) {
				log.warn(
						"Found device with broken MAC address ({}), trying to reset it to ({}) as configured in DeviceDB",
						actualMacAddress, configuredMacAddress
				);
				try {
					device.writeMac(configuredMacAddress, TIMEOUT_WRITEMAC_MILLIS, null).get();
				} catch (Exception e) {
					throw new RuntimeException(
							"Failed resetting broken device MAC address to configured MAC address " + configuredMacAddress
					);
				}
			} else if (log.isTraceEnabled()) {
				log.trace("Checked for broken MAC address but everything is fine :-)");
			}
		} finally {
			deviceLock.unlock();
		}
	}

	@Override
	protected void doStop() {

		try {

			fireDevicesDisconnected(deviceConfig.getNodeUrn());

			log.debug("{} => Shutting down {} device connector",
					deviceConfig.getNodeUrn(),
					deviceConfig.getNodeType()
			);

			shutdownDeviceChannel();
			device.close();
            nodeApi.stopAsync().awaitTerminated();

			shutdown(deviceExecutor, 1, TimeUnit.SECONDS);

		} catch (Exception e) {
			notifyFailed(e);
		}

		notifyStopped();
	}

	@Override
	public ListenableFuture<NodeApiCallResult> disableVirtualLink(final MacAddress targetMacAddress) {
		log.debug("{} => SingleDeviceAdapter.disableVirtualLink()", deviceConfig.getNodeUrn());
		return nodeApi.destroyVirtualLink(targetMacAddress.toLong());
	}

	@Override
	public ListenableFuture<NodeApiCallResult> disableNode() {
		log.debug("{} => SingleDeviceAdapter.disableNode()", deviceConfig.getNodeUrn());
		return nodeApi.disableNode();
	}

	@Override
	public ListenableFuture<NodeApiCallResult> disablePhysicalLink(final MacAddress targetMacAddress) {
		log.debug("{} => SingleDeviceAdapter.disablePhysicalLink()", deviceConfig.getNodeUrn());
		return nodeApi.disablePhysicalLink(targetMacAddress.toLong());
	}

	@Override
	public ListenableFuture<NodeApiCallResult> enableNode() {
		log.debug("{} => SingleDeviceAdapter.enableNode()", deviceConfig.getNodeUrn());
		return nodeApi.enableNode();
	}

	@Override
	public ListenableFuture<NodeApiCallResult> enablePhysicalLink(final MacAddress targetMacAddress) {
		log.debug("{} => SingleDeviceAdapter.enablePhysicalLink()", deviceConfig.getNodeUrn());
		return nodeApi.enablePhysicalLink(targetMacAddress.toLong());
	}

	@Override
	public ProgressListenableFuture<Void> flashProgram(final byte[] binaryImage) {

		log.debug("{} => SingleDeviceAdapter.executeFlashPrograms()", deviceConfig.getNodeUrn());

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

					final OperationAdapter<Void> progressListener = new OperationAdapter<Void>() {

						private RateLimiter rateLimiter = RateLimiter.create(4);

						@Override
						public void onExecute() {
							future.setProgress(0f);
						}

						@Override
						public void onCancel() {
							// handled by other listener
						}

						@Override
						public void onFailure(final Throwable throwable) {
							// handled by other listener
						}

						@Override
						public void onProgressChange(final float fraction) {

							if (rateLimiter.tryAcquire(0, TimeUnit.SECONDS)) {

								log.debug("{} => Flashing progress: {}%.",
										deviceConfig.getNodeUrn(),
										(int) (fraction * 100)
								);

								future.setProgress(fraction);
							}
						}

						@Override
						public void onSuccess(final Void result) {
							// handled by other listener
						}
					};

					final OperationFuture<Void> operationFuture = device.program(
							binaryImage,
							deviceConfig.getTimeoutFlashMillis(),
							progressListener
					);

					final Runnable completionListener = new Runnable() {
						@Override
						public void run() {

							if (operationFuture.isCancelled()) {

								String msg = "Flash operation was canceled.";
								log.error("{} => flashProgram: {}", deviceConfig.getNodeUrn(), msg);

								try {
									checkForBrokenMacAndRepair();
								} catch (Exception e) {
									log.error("", e);
								}

								future.setException(new RuntimeException(msg));
								flashOperationRunningOrEnqueued = false;

							} else {

								try {

									operationFuture.get();
									log.debug("{} => Done flashing node.", deviceConfig.getNodeUrn());

									try {

										checkForBrokenMacAndRepair();

										// reconnect if TelosB (workaround for https://github.com/itm/wsn-device-drivers/issues/143)
										if (DeviceType.fromString(deviceConfig.getNodeType()) == DeviceType.TELOSB) {

											final DeviceLostEvent deviceLostEvent = new DeviceLostEvent(
													deviceType,
													devicePort,
													deviceConfiguration,
													deviceConfig.getNodeUSBChipID(),
													new MacAddress(deviceConfig.getNodeUrn().getSuffix()),
													deviceConfig
											);

											final DeviceFoundEvent deviceFoundEvent = new DeviceFoundEvent(
													deviceType,
													devicePort,
													deviceConfiguration,
													deviceConfig.getNodeUSBChipID(),
													new MacAddress(deviceConfig.getNodeUrn().getSuffix()),
													deviceConfig
											);


											schedulerService.schedule(new Runnable() {
																		  @Override
																		  public void run() {
																			  gatewayEventBus.post(deviceLostEvent);
																		  }
																	  }, 0, TimeUnit.SECONDS
											);
											schedulerService.schedule(new Runnable() {
																		  @Override
																		  public void run() {
																			  gatewayEventBus.post(deviceFoundEvent);
																		  }
																	  }, 1, TimeUnit.SECONDS
											);
										} else {
											resetNode();
										}

										future.set(null);

									} catch (Exception ex) {
										future.setException(ex);
									}

									flashOperationRunningOrEnqueued = false;

								} catch (Exception e) {

									String msg = "Failed flashing node. Reason: " + e;
									log.warn("{} => flashProgram: {}", deviceConfig.getNodeUrn(), msg);

									try {
										checkForBrokenMacAndRepair();
									} catch (Exception ex) {
										log.error("", ex);
									}

									flashOperationRunningOrEnqueued = false;
									future.setException(new RuntimeException(msg));
								}
							}
						}
					};

					operationFuture.addListener(completionListener, deviceExecutor);

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

		log.debug("{} => SingleDeviceAdapter.isNodeAlive()", deviceConfig.getNodeUrn());

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
								log.debug("{} => Done checking node alive (result={}).", deviceConfig.getNodeUrn(),
										result
								);
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
		log.debug("{} => SingleDeviceAdapter.isNodeConnected()", deviceConfig.getNodeUrn());
		return immediateFuture(isConnected());
	}

	@Override
	public ListenableFuture<Void> resetNode() {

		log.debug("{} => SingleDeviceAdapter.resetNode()", deviceConfig.getNodeUrn());
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

		log.debug("{} => SingleDeviceAdapter.sendMessage()", deviceConfig.getNodeUrn());
		final SettableFuture<Void> future = SettableFuture.create();

		if (!isConnected()) {

			final String msg = "Node is not connected.";
			log.warn("{} => sendMessage(): ", deviceConfig.getNodeUrn(), msg);
			future.setException(new RuntimeException(msg));
		}

		final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(messageBytes);

		if (log.isDebugEnabled()) {
			log.debug("{} => Delivering message: {}",
					deviceConfig.getNodeUrn(),
					ChannelBufferTools.toPrintableString(buffer, 200)
			);
		}

		deviceChannel.write(buffer).addListener(new ChannelFutureListener() {

													@Override
													public void operationComplete(final ChannelFuture channelFuture)
															throws Exception {

														if (channelFuture.isSuccess()) {

															future.set(null);

														} else if (channelFuture.isCancelled()) {

															final String msg = "Sending message was canceled.";
															log.warn("{} => sendMessage(): {}",
																	deviceConfig.getNodeUrn(), msg
															);
															future.setException(new RuntimeException(msg));

														} else {

															final Throwable cause = channelFuture.getCause();
															final String msg =
																	"Failed sending message. Reason: " + cause;
															log.warn("{} => sendMessage(): {}",
																	deviceConfig.getNodeUrn(), msg
															);
															future.setException(new RuntimeException(cause));
														}
													}
												}
		);

		return future;
	}

	public ListenableFuture<Void> setDefaultChannelPipeline() {
		try {
			currentPipeline = deviceConfig.getDefaultChannelPipeline();
			setPipeline(deviceChannel.getPipeline(), createPipelineHandlers(createHandlers(currentPipeline)));
			log.debug("{} => Channel pipeline now set to: {}", deviceConfig.getNodeUrn(), currentPipeline);
			return immediateFuture(null);
		} catch (Exception e) {
			log.warn("Exception while setting default channel pipeline: {}", e);
			return Futures.immediateFailedFuture(new RuntimeException(e));
		}
	}

	@Override
	public ListenableFuture<Void> setChannelPipeline(final ChannelHandlerConfigList channelHandlerConfigs) {

		final SettableFuture<Void> future = SettableFuture.create();

		if (isConnected()) {

			NamedChannelHandlerList innerPipelineHandlers;

			try {

				log.debug("{} => Setting channel pipeline using configuration: {}", deviceConfig.getNodeUrn(),
						channelHandlerConfigs
				);

				currentPipeline = channelHandlerConfigs;
				innerPipelineHandlers = createHandlers(currentPipeline);
				setPipeline(deviceChannel.getPipeline(), createPipelineHandlers(innerPipelineHandlers));

				log.debug("{} => Channel pipeline now set to: {}", deviceConfig.getNodeUrn(), innerPipelineHandlers);
				future.set(null);

			} catch (Exception e) {

				log.warn("{} => {} while setting channel pipeline: {}",
						deviceConfig.getNodeUrn(), e.getClass().getSimpleName(), e.getMessage()
				);
				future.setException(e);

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

	@Override
	public ListenableFuture<ChannelHandlerConfigList> getChannelPipeline() {
		return immediateFuture(currentPipeline);
	}

	private NamedChannelHandlerList createHandlers(@Nullable final ChannelHandlerConfigList configs) throws Exception {
		final NamedChannelHandlerList handlers = new NamedChannelHandlerList();
		if (configs != null) {
			for (ChannelHandlerConfig config : configs) {
				handlers.add(handlerFactories.get(config.getHandlerName()).create(config));
			}
		}
		return handlers;
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

                stopAsync();
                log.trace("{} => Pipe broken, disconnecting: {}", deviceConfig.getNodeUrn(), e);

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

		if (log.isTraceEnabled()) {
			log.trace("{} => SingleDeviceAdapter.onBytesReceivedFromDevice: {}",
					deviceConfig.getNodeUrn(),
					ChannelBufferTools.toPrintableString(buffer, 200)
			);
		}

		// pass output to all listeners
		byte[] bytes = new byte[buffer.readableBytes()];
		buffer.getBytes(0, bytes);

		fireBytesReceivedFromDevice(deviceConfig.getNodeUrn(), bytes);

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
			fireNotification(deviceConfig.getNodeUrn(), notification);
			pipelineMisconfigurationTimeDiff.touch();
		}
	}

	@Nonnull
	private NamedChannelHandlerList createPipelineHandlers(NamedChannelHandlerList innerChannelHandlers)
			throws Exception {

		final NamedChannelHandlerList handlers = new NamedChannelHandlerList();

		final boolean innerHandlersExist = !checkNotNull(innerChannelHandlers).isEmpty();

		if (log.isTraceEnabled() && innerHandlersExist) {
			handlers.add("belowChannelPipelineLogger", belowPipelineLogger);
		}

		if (innerHandlersExist) {
			handlers.add(innerChannelHandlers);
		}

		if (log.isTraceEnabled() && innerHandlersExist) {
			handlers.add("aboveChannelPipelineLogger", abovePipelineLogger);
		}

		handlers.add("forwardingHandler", forwardingHandler);

		return handlers;
	}

	@Override
	public ListenableFuture<NodeApiCallResult> enableVirtualLink(final MacAddress targetMacAddress) {
		log.debug("{} => SingleDeviceAdapter.enableVirtualLink()", deviceConfig.getNodeUrn());
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
