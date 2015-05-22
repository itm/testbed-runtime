package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfig;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfigList;
import de.uniluebeck.itm.tr.common.NodeUrnHelper;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiCallResult;
import de.uniluebeck.itm.util.concurrent.ListenableFutureMap;
import de.uniluebeck.itm.util.concurrent.ProgressListenableFuture;
import de.uniluebeck.itm.util.concurrent.ProgressListenableFutureMap;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static de.uniluebeck.itm.tr.iwsn.messages.MessageFactoryImpl.newGetChannelPipelinesResponse;
import static de.uniluebeck.itm.tr.iwsn.messages.MessageFactoryImpl.newSingleNodeResponse;

public class RequestHandlerImpl extends AbstractService implements RequestHandler {

	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

	private static final Function<? super String, ? extends NodeUrn> STRING_TO_NODE_URN =
			new Function<String, NodeUrn>() {
				@Nullable
				@Override
				public NodeUrn apply(@Nullable final String nodeUrnString) {
					return new NodeUrn(nodeUrnString);
				}
			};

	private static final ListeningExecutorService SAME_THREAD_EXECUTOR = sameThreadExecutor();

	private final DeviceManager deviceManager;

	private final GatewayEventBus gatewayEventBus;

	@Inject
	public RequestHandlerImpl(final DeviceManager deviceManager,
							  final GatewayEventBus gatewayEventBus) {
		this.deviceManager = deviceManager;
		this.gatewayEventBus = gatewayEventBus;
	}

	@Override
	protected void doStart() {
		log.trace("RequestHandlerImpl.doStart()");
		try {
			gatewayEventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.trace("RequestHandlerImpl.doStop()");
		try {
			gatewayEventBus.unregister(this);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Subscribe
	public void onRequest(final Request request) throws Exception {

		switch (request.getType()) {
			case ARE_NODES_ALIVE:
				onAreNodesAliveRequest(request);
				break;
			case ARE_NODES_CONNECTED:
				onAreNodesConnectedRequest(request);
				break;
			case DISABLE_NODES:
				onDisableNodesRequest(request);
				break;
			case DISABLE_PHYSICAL_LINKS:
				onDisablePhysicalLinksRequest(request);
				break;
			case DISABLE_VIRTUAL_LINKS:
				onDisableVirtualLinksRequest(request);
				break;
			case ENABLE_NODES:
				onEnablesNodeRequest(request);
				break;
			case ENABLE_PHYSICAL_LINKS:
				onEnablePhysicalLinksRequest(request);
				break;
			case ENABLE_VIRTUAL_LINKS:
				onEnableVirtualLinksRequest(request);
				break;
			case FLASH_IMAGES:
				onFlashImagesRequest(request);
				break;
			case GET_CHANNEL_PIPELINES:
				onGetChannelPipelinesRequest(request);
				break;
			case RESET_NODES:
				onResetNodesRequest(request);
				break;
			case SEND_DOWNSTREAM_MESSAGES:
				onSendDownstreamMessagesRequest(request);
				break;
			case SET_CHANNEL_PIPELINES:
				onSetChannelPipelinesRequest(request);
				break;
			default:
				throw new RuntimeException("Unknown request type received!");
		}
	}

	private void onGetChannelPipelinesRequest(final Request request) {

		log.trace("RequestHandlerImpl.onGetChannelPipelinesRequest({})", request);

		final Set<NodeUrn> nodeUrns = newHashSet(transform(
				request.getGetChannelPipelinesRequest().getNodeUrnsList(),
				NodeUrnHelper.STRING_TO_NODE_URN
		)
		);

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedSubset = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(request.getReservationId(), request.getRequestId(), unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedSubset.keys()) {

			try {

				final ListenableFutureMap<NodeUrn, ChannelHandlerConfigList> mapFuture = deviceAdapter
						.getChannelPipelines(connectedSubset.get(deviceAdapter));
				final Map<NodeUrn, ChannelHandlerConfigList> map = mapFuture.get(30, TimeUnit.SECONDS);
				final Map<NodeUrn, List<ChannelHandlerConfiguration>> responseMap = Maps.transformEntries(map,
						new Maps.EntryTransformer<NodeUrn, ChannelHandlerConfigList, List<ChannelHandlerConfiguration>>() {
							@Override
							public List<ChannelHandlerConfiguration> transformEntry(final NodeUrn key,
																					@Nullable final ChannelHandlerConfigList pipeline) {
								final List<ChannelHandlerConfiguration> res = newArrayList();
								if (pipeline != null) {
									for (ChannelHandlerConfig chc : pipeline) {
										res.add(convert(chc));
									}
								}
								return res;
							}
						}
				);
				final GetChannelPipelinesResponse response = newGetChannelPipelinesResponse(
						request.getReservationId(),
						request.getRequestId(),
						responseMap
				);

				gatewayEventBus.post(response);

			} catch (Exception e) {
				throw propagate(e);
			}
		}
	}

	private ChannelHandlerConfiguration convert(final ChannelHandlerConfig chc) {

		final ChannelHandlerConfiguration.Builder builder =
				ChannelHandlerConfiguration.newBuilder().setName(chc.getHandlerName());

		for (String key : chc.getProperties().keys()) {
			for (String value : chc.getProperties().get(key)) {
				final ChannelHandlerConfiguration.KeyValuePair.Builder keyValuePairBuilder =
						ChannelHandlerConfiguration.KeyValuePair
								.newBuilder()
								.setKey(key)
								.setValue(value);
				builder.addConfiguration(keyValuePairBuilder);
			}
		}

		return builder.build();
	}

	private void onEnableVirtualLinksRequest(final Request request) {

		log.trace("RequestHandlerImpl.onEnableVirtualLinksRequest({})", request);

		final String reservationId = request.hasReservationId() ? request.getReservationId() : null;
		final long requestId = request.getRequestId();
		final Set<NodeUrn> nodeUrns = newHashSet();
		final List<Link> links = request.getEnableVirtualLinksRequest().getLinksList();

		for (Link link : links) {
			nodeUrns.add(new NodeUrn(link.getSourceNodeUrn()));
		}

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedSubset = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(reservationId, requestId, unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedSubset.keySet()) {
			final Map<NodeUrn, NodeUrn> linksMap = createLinkMap(links, connectedSubset.get(deviceAdapter));
			addNodeApiOperationListeners(reservationId, requestId, deviceAdapter.enableVirtualLinks(linksMap));
		}
	}

	private void onSetChannelPipelinesRequest(final Request request) {

		log.trace("RequestHandlerImpl.onSetChannelPipelinesRequest({})", request);

		final String reservationId = request.hasReservationId() ? request.getReservationId() : null;
		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getSetChannelPipelinesRequest().getNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(reservationId, requestId, unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {

			final Collection<NodeUrn> nodeUrnsToSetChannelPipelineOn = connectedMap.get(deviceAdapter);
			final List<ChannelHandlerConfiguration> channelHandlerConfigurationsList =
					request.getSetChannelPipelinesRequest().getChannelHandlerConfigurationsList();

			final ChannelHandlerConfigList cp = new ChannelHandlerConfigList();

			for (ChannelHandlerConfiguration config : channelHandlerConfigurationsList) {

				final HashMultimap<String, String> options = HashMultimap.create();
				for (ChannelHandlerConfiguration.KeyValuePair kv : config
						.getConfigurationList()) {
					options.put(kv.getKey(), kv.getValue());
				}

				cp.add(new ChannelHandlerConfig(config.getName(), config.getName(), options));
			}

			addVoidOperationListeners(
					reservationId,
					requestId,
					1,
					deviceAdapter.setChannelPipelines(nodeUrnsToSetChannelPipelineOn, cp)
			);
		}
	}

	private void onSendDownstreamMessagesRequest(final Request request) {

		log.trace("RequestHandlerImpl.onSendDownstreamMessagesRequest({})", request);

		final String reservationId = request.hasReservationId() ? request.getReservationId() : null;
		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getSendDownstreamMessagesRequest().getTargetNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(reservationId, requestId, unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {

			final Collection<NodeUrn> nodeUrnsToSendTo = connectedMap.get(deviceAdapter);
			final byte[] messageBytes = request.getSendDownstreamMessagesRequest().getMessageBytes().toByteArray();

			addVoidOperationListeners(
					reservationId,
					requestId,
					1,
					deviceAdapter.sendMessage(nodeUrnsToSendTo, messageBytes)
			);
		}
	}

	private void onResetNodesRequest(final Request request) {

		log.trace("RequestHandlerImpl.onResetNodesRequest({})", request);

		final String reservationId = request.hasReservationId() ? request.getReservationId() : null;
		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getResetNodesRequest().getNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(reservationId, requestId, unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			addVoidOperationListeners(
					reservationId,
					requestId,
					1,
					deviceAdapter.resetNodes(connectedMap.get(deviceAdapter))
			);
		}
	}

	private void onFlashImagesRequest(final Request request) {

		log.trace("RequestHandlerImpl.onFlashImagesRequest({})", request);

		final String reservationId = request.hasReservationId() ? request.getReservationId() : null;
		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getFlashImagesRequest().getNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(reservationId, requestId, unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {

			final byte[] binaryImage = request.getFlashImagesRequest().getImage().toByteArray();
			final Collection<NodeUrn> nodeUrnsToFlash = connectedMap.get(deviceAdapter);

			addVoidOperationListeners(
					reservationId,
					requestId,
					100,
					deviceAdapter.flashProgram(nodeUrnsToFlash, binaryImage)
			);
		}
	}

	private void onEnablePhysicalLinksRequest(final Request request) {

		log.trace("RequestHandlerImpl.onEnablePhysicalLinksRequest({)", request);

		final String reservationId = request.hasReservationId() ? request.getReservationId() : null;
		final long requestId = request.getRequestId();
		final Set<NodeUrn> nodeUrns = newHashSet();
		final List<Link> links = request.getEnablePhysicalLinksRequest().getLinksList();

		for (Link link : links) {
			nodeUrns.add(new NodeUrn(link.getSourceNodeUrn()));
		}

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedSubset = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(reservationId, requestId, unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedSubset.keySet()) {
			final Map<NodeUrn, NodeUrn> linksMap = createLinkMap(links, connectedSubset.get(deviceAdapter));
			addNodeApiOperationListeners(reservationId, requestId, deviceAdapter.enablePhysicalLinks(linksMap));
		}
	}

	private void onEnablesNodeRequest(final Request request) {

		log.trace("RequestHandlerImpl.onEnablesNodeRequest({})", request);

		final String reservationId = request.hasReservationId() ? request.getReservationId() : null;
		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getEnableNodesRequest().getNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(reservationId, requestId, unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			addNodeApiOperationListeners(
					reservationId,
					requestId,
					deviceAdapter.enableNodes(connectedMap.get(deviceAdapter))
			);
		}
	}

	private void onDisablePhysicalLinksRequest(final Request request) {

		log.trace("RequestHandlerImpl.onDisablePhysicalLinksRequest({})", request);

		final String reservationId = request.hasReservationId() ? request.getReservationId() : null;
		final long requestId = request.getRequestId();
		final Set<NodeUrn> nodeUrns = newHashSet();
		final List<Link> links = request.getDisablePhysicalLinksRequest().getLinksList();

		for (Link link : links) {
			nodeUrns.add(new NodeUrn(link.getSourceNodeUrn()));
		}

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedSubset = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(reservationId, requestId, unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedSubset.keySet()) {
			final Map<NodeUrn, NodeUrn> linksMap = createLinkMap(links, connectedSubset.get(deviceAdapter));
			addNodeApiOperationListeners(reservationId, requestId, deviceAdapter.disablePhysicalLinks(linksMap));
		}
	}

	private void onDisableNodesRequest(final Request request) {

		log.trace("RequestHandlerImpl.onDisableNodesRequest({})", request);

		final String reservationId = request.hasReservationId() ? request.getReservationId() : null;
		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getDisableNodesRequest().getNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(reservationId, requestId, unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			addNodeApiOperationListeners(reservationId, requestId,
					deviceAdapter.disableNodes(connectedMap.get(deviceAdapter))
			);
		}
	}

	private void onDisableVirtualLinksRequest(final Request request) {

		log.trace("RequestHandlerImpl.onDisableVirtualLinksRequest({})", request);

		final String reservationId = request.hasReservationId() ? request.getReservationId() : null;
		final long requestId = request.getRequestId();
		final Set<NodeUrn> nodeUrns = newHashSet();
		final List<Link> links = request.getDisableVirtualLinksRequest().getLinksList();

		for (Link link : links) {
			nodeUrns.add(new NodeUrn(link.getSourceNodeUrn()));
		}

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedSubset = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(reservationId, requestId, unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedSubset.keySet()) {
			final Map<NodeUrn, NodeUrn> linksMap = createLinkMap(links, connectedSubset.get(deviceAdapter));
			addNodeApiOperationListeners(reservationId, requestId, deviceAdapter.disableVirtualLinks(linksMap));
		}
	}

	private void onAreNodesConnectedRequest(final Request request) {

		log.trace("RequestHandlerImpl.onAreNodesConnectedRequest({})", request);

		final String reservationId = request.hasReservationId() ? request.getReservationId() : null;
		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getAreNodesConnectedRequest().getNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(reservationId, request.getRequestId(), unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			addBoolOperationListeners(reservationId, requestId,
					deviceAdapter.areNodesConnected(connectedMap.get(deviceAdapter))
			);
		}
	}

	private void onAreNodesAliveRequest(final Request request) {

		log.trace("RequestHandlerImpl.onAreNodesAliveRequest({})", request);

		final String reservationId = request.hasReservationId() ? request.getReservationId() : null;
		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getAreNodesAliveRequest().getNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(reservationId, requestId, unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			addBoolOperationListeners(reservationId, requestId,
					deviceAdapter.areNodesAlive(connectedMap.get(deviceAdapter))
			);
		}
	}

	private void addVoidOperationListeners(@Nullable final String reservationId,
										   final long requestId,
										   final int completionStatusCode,
										   final ListenableFutureMap<NodeUrn, Void> futureMap) {

		for (NodeUrn nodeUrn : futureMap.keySet()) {
			futureMap.get(nodeUrn).addListener(
					createVoidOperationListener(
							reservationId,
							requestId,
							nodeUrn,
							completionStatusCode,
							futureMap.get(nodeUrn)
					),
					SAME_THREAD_EXECUTOR
			);
		}
	}

	private void addVoidOperationListeners(@Nullable final String reservationId,
										   final long requestId,
										   final int completionStatusCode,
										   final ProgressListenableFutureMap<NodeUrn, Void> futureMap) {

		for (NodeUrn nodeUrn : futureMap.keySet()) {
			final Runnable listener = createVoidOperationListener(
					reservationId,
					requestId,
					nodeUrn,
					completionStatusCode,
					futureMap.get(nodeUrn)
			);
			futureMap.get(nodeUrn).addListener(listener, SAME_THREAD_EXECUTOR);
			futureMap.get(nodeUrn).addProgressListener(listener, SAME_THREAD_EXECUTOR);
		}
	}

	private void addBoolOperationListeners(@Nullable final String reservationId,
										   final long requestId,
										   final ListenableFutureMap<NodeUrn, Boolean> futureMap) {

		for (NodeUrn nodeUrn : futureMap.keySet()) {
			final Runnable listener = createBoolOperationListener(
					reservationId,
					requestId,
					nodeUrn,
					futureMap.get(nodeUrn)
			);
			futureMap.get(nodeUrn).addListener(listener, SAME_THREAD_EXECUTOR);
		}
	}

	private void addNodeApiOperationListeners(@Nullable final String reservationId,
											  final long requestId,
											  final ListenableFutureMap<NodeUrn, NodeApiCallResult> futureMap) {

		for (NodeUrn nodeUrn : futureMap.keySet()) {
			final Runnable listener = createNodeApiOperationListener(
					reservationId,
					requestId,
					nodeUrn,
					futureMap.get(nodeUrn)
			);
			futureMap.get(nodeUrn).addListener(listener, SAME_THREAD_EXECUTOR);
		}
	}

	private Runnable createVoidOperationListener(@Nullable final String reservationId,
												 final long requestId,
												 final NodeUrn nodeUrn,
												 final int completionStatusCode,
												 final ListenableFuture<Void> future) {
		return new Runnable() {
			@Override
			public void run() {

				if (future instanceof ProgressListenableFuture && !future.isDone()) {

					final float progress = ((ProgressListenableFuture) future).getProgress();
					if (progress < 1f) {
						postProgress(
								reservationId,
								requestId,
								nodeUrn,
								progress
						);
					}

				} else if (future.isDone()) {

					try {
						future.get(); // check if exception occurred
						postResponse(reservationId, requestId, nodeUrn, completionStatusCode, null);
					} catch (Exception e) {
						postRequestFailureResponse(reservationId, requestId, nodeUrn, e);
					}
				}
			}
		};
	}

	private Runnable createNodeApiOperationListener(@Nullable final String reservationId,
													final long requestId,
													final NodeUrn nodeUrn,
													final ListenableFuture<NodeApiCallResult> future) {
		return new Runnable() {

			@Override
			public void run() {
				try {

					final NodeApiCallResult result = future.get();
					final byte statusCode = result.isSuccessful() ? 1 : result.getResponseType();
					postResponse(reservationId, requestId, nodeUrn, statusCode, result.getResponse());

				} catch (Exception e) {
					postRequestFailureResponse(reservationId, requestId, nodeUrn, e);
				}
			}
		};
	}

	private Runnable createBoolOperationListener(@Nullable final String reservationId,
												 final long requestId,
												 final NodeUrn nodeUrn,
												 final ListenableFuture<Boolean> future) {
		return new Runnable() {
			@Override
			public void run() {
				try {
					postResponse(reservationId, requestId, nodeUrn, future.get() ? 1 : 0, null);
				} catch (Exception e) {
					postRequestFailureResponse(reservationId, requestId, nodeUrn, e);
				}
			}
		};
	}

	private void postProgress(@Nullable final String reservationId,
							  final long requestId,
							  final NodeUrn nodeUrn,
							  final float progress) {

		final SingleNodeProgress.Builder builder = SingleNodeProgress.newBuilder()
				.setRequestId(requestId)
				.setNodeUrn(nodeUrn.toString())
				.setProgressInPercent((int) (progress * 100));

		if (reservationId != null) {
			builder.setReservationId(reservationId);
		}

		gatewayEventBus.post(builder.build());
	}

	private void postResponse(@Nullable final String reservationId,
							  final long requestId,
							  final NodeUrn nodeUrn,
							  final int statusCode,
							  @Nullable final byte[] responseBytes) {

		final SingleNodeResponse.Builder responseBuilder = SingleNodeResponse.newBuilder()
				.setRequestId(requestId)
				.setNodeUrn(nodeUrn.toString())
				.setStatusCode(statusCode);

		if (reservationId != null) {
			responseBuilder.setReservationId(reservationId);
		}

		if (responseBytes != null) {
			responseBuilder.setResponse(ByteString.copyFrom(responseBytes));
		}

		gatewayEventBus.post(responseBuilder.build());
	}

	private void postRequestFailureResponse(@Nullable final String reservationId,
											final long requestId,
											final NodeUrn nodeUrn,
											final Exception e) {
		final SingleNodeResponse.Builder builder = SingleNodeResponse.newBuilder()
				.setRequestId(requestId)
				.setNodeUrn(nodeUrn.toString())
				.setStatusCode(-2)
				.setErrorMessage(Throwables.getStackTraceAsString(e));

		if (reservationId != null) {
			builder.setReservationId(reservationId);
		}

		gatewayEventBus.post(builder.build());
	}

	private void postNodeNotConnectedResponse(@Nullable final String reservationId,
											  final long requestId,
											  final Iterable<NodeUrn> unconnectedNodeUrns) {
		for (NodeUrn nodeUrn : unconnectedNodeUrns) {
			gatewayEventBus.post(newSingleNodeResponse(
					reservationId,
					requestId,
					nodeUrn,
					-1,
					"Node \"" + nodeUrn + "\" is not connected."
			)
			);
		}
	}

	private Map<NodeUrn, NodeUrn> createLinkMap(final List<Link> links, final Collection<NodeUrn> connectedNodes) {

		final Map<NodeUrn, NodeUrn> virtualLinksToSet = newHashMap();

		for (NodeUrn connectedNode : connectedNodes) {
			for (Link link : links) {

				final NodeUrn sourceNodeUrn = new NodeUrn(link.getSourceNodeUrn());
				final NodeUrn targetNodeUrn = new NodeUrn(link.getTargetNodeUrn());

				if (connectedNode.equals(targetNodeUrn)) {
					virtualLinksToSet.put(sourceNodeUrn, targetNodeUrn);
				}
			}
		}

		return virtualLinksToSet;
	}
}
