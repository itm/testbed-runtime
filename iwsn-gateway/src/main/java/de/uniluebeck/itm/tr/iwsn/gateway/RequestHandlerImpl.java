package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiCallResult;
import de.uniluebeck.itm.tr.util.ListenableFutureMap;
import de.uniluebeck.itm.tr.util.ProgressListenableFutureMap;
import de.uniluebeck.itm.tr.util.Tuple;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;

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

	private void onEnableVirtualLinksRequest(final Request request) {

		log.trace("RequestHandlerImpl.onEnableVirtualLinksRequest({})", request);

		final long requestId = request.getRequestId();
		final Set<NodeUrn> nodeUrns = newHashSet();
		final List<Link> links = request.getEnableVirtualLinksRequest().getLinksList();

		for (Link link : links) {
			nodeUrns.add(new NodeUrn(link.getSourceNodeUrn()));
		}

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedSubset = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(request.getRequestId(), unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedSubset.keySet()) {
			final Map<NodeUrn, NodeUrn> linksMap = createLinkMap(links, connectedSubset.get(deviceAdapter));
			addNodeApiOperationListeners(requestId, deviceAdapter.enableVirtualLinks(linksMap));
		}
	}

	private void onSetChannelPipelinesRequest(final Request request) {

		log.trace("RequestHandlerImpl.onSetChannelPipelinesRequest({})", request);

		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getSetChannelPipelinesRequest().getNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(request.getRequestId(), unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {

			final Collection<NodeUrn> nodeUrnsToSetChannelPipelineOn = connectedMap.get(deviceAdapter);
			final List<SetChannelPipelinesRequest.ChannelHandlerConfiguration> channelHandlerConfigurationsList =
					request.getSetChannelPipelinesRequest().getChannelHandlerConfigurationsList();

			final List<Tuple<String, Multimap<String, String>>> cp = newArrayList();

			for (SetChannelPipelinesRequest.ChannelHandlerConfiguration config : channelHandlerConfigurationsList) {

				final HashMultimap<String, String> options = HashMultimap.create();
				for (SetChannelPipelinesRequest.ChannelHandlerConfiguration.KeyValuePair keyValuePair : config
						.getConfigurationList()) {
					options.put(keyValuePair.getKey(), keyValuePair.getValue());
				}

				cp.add(new Tuple<String, Multimap<String, String>>(config.getName(), options));
			}

			addVoidOperationListeners(requestId, deviceAdapter.setChannelPipelines(
					nodeUrnsToSetChannelPipelineOn,
					cp
			)
			);
		}
	}

	private void onSendDownstreamMessagesRequest(final Request request) {

		log.trace("RequestHandlerImpl.onSendDownstreamMessagesRequest({})", request);

		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getSendDownstreamMessagesRequest().getTargetNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(request.getRequestId(), unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {

			final Collection<NodeUrn> nodeUrnsToSendTo = connectedMap.get(deviceAdapter);
			final byte[] messageBytes = request.getSendDownstreamMessagesRequest().getMessageBytes().toByteArray();

			addVoidOperationListeners(requestId, deviceAdapter.sendMessage(nodeUrnsToSendTo, messageBytes));
		}
	}

	private void onResetNodesRequest(final Request request) {

		log.trace("RequestHandlerImpl.onResetNodesRequest({})", request);

		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getResetNodesRequest().getNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(request.getRequestId(), unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			addVoidOperationListeners(requestId, deviceAdapter.resetNodes(connectedMap.get(deviceAdapter)));
		}
	}

	private void onFlashImagesRequest(final Request request) {

		log.trace("RequestHandlerImpl.onFlashImagesRequest({})", request);

		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getFlashImagesRequest().getNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(request.getRequestId(), unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {

			final byte[] binaryImage = request.getFlashImagesRequest().getImage().toByteArray();
			final Collection<NodeUrn> nodeUrnsToFlash = connectedMap.get(deviceAdapter);

			addVoidOperationListeners(requestId, deviceAdapter.flashProgram(nodeUrnsToFlash, binaryImage));
		}
	}

	private void onEnablePhysicalLinksRequest(final Request request) {

		log.trace("RequestHandlerImpl.onEnablePhysicalLinksRequest({)", request);

		final long requestId = request.getRequestId();
		final Set<NodeUrn> nodeUrns = newHashSet();
		final List<Link> links = request.getEnablePhysicalLinksRequest().getLinksList();

		for (Link link : links) {
			nodeUrns.add(new NodeUrn(link.getSourceNodeUrn()));
		}

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedSubset = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(request.getRequestId(), unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedSubset.keySet()) {
			final Map<NodeUrn, NodeUrn> linksMap = createLinkMap(links, connectedSubset.get(deviceAdapter));
			addNodeApiOperationListeners(requestId, deviceAdapter.enablePhysicalLinks(linksMap));
		}
	}

	private void onEnablesNodeRequest(final Request request) {

		log.trace("RequestHandlerImpl.onEnablesNodeRequest({})", request);

		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getEnableNodesRequest().getNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(requestId, unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			addNodeApiOperationListeners(requestId, deviceAdapter.enableNodes(connectedMap.get(deviceAdapter)));
		}
	}

	private void onDisablePhysicalLinksRequest(final Request request) {

		log.trace("RequestHandlerImpl.onDisablePhysicalLinksRequest({})", request);

		final long requestId = request.getRequestId();
		final Set<NodeUrn> nodeUrns = newHashSet();
		final List<Link> links = request.getDisablePhysicalLinksRequest().getLinksList();

		for (Link link : links) {
			nodeUrns.add(new NodeUrn(link.getSourceNodeUrn()));
		}

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedSubset = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(request.getRequestId(), unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedSubset.keySet()) {
			final Map<NodeUrn, NodeUrn> linksMap = createLinkMap(links, connectedSubset.get(deviceAdapter));
			addNodeApiOperationListeners(requestId, deviceAdapter.disablePhysicalLinks(linksMap));
		}
	}

	private void onDisableNodesRequest(final Request request) {

		log.trace("RequestHandlerImpl.onDisableNodesRequest({})", request);

		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getDisableNodesRequest().getNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(requestId, unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			addNodeApiOperationListeners(requestId, deviceAdapter.disableNodes(connectedMap.get(deviceAdapter)));
		}
	}

	private void onDisableVirtualLinksRequest(final Request request) {

		log.trace("RequestHandlerImpl.onDisableVirtualLinksRequest({})", request);

		final long requestId = request.getRequestId();
		final Set<NodeUrn> nodeUrns = newHashSet();
		final List<Link> links = request.getDisableVirtualLinksRequest().getLinksList();

		for (Link link : links) {
			nodeUrns.add(new NodeUrn(link.getSourceNodeUrn()));
		}

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedSubset = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(request.getRequestId(), unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedSubset.keySet()) {
			final Map<NodeUrn, NodeUrn> linksMap = createLinkMap(links, connectedSubset.get(deviceAdapter));
			addNodeApiOperationListeners(requestId, deviceAdapter.disableVirtualLinks(linksMap));
		}
	}

	private void onAreNodesConnectedRequest(final Request request) {

		log.trace("RequestHandlerImpl.onAreNodesConnectedRequest({})", request);

		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getAreNodesAliveRequest().getNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(request.getRequestId(), unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			addBoolOperationListeners(requestId, deviceAdapter.areNodesConnected(connectedMap.get(deviceAdapter)));
		}
	}

	private void onAreNodesAliveRequest(final Request request) {

		log.trace("RequestHandlerImpl.onAreNodesAliveRequest({})", request);

		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getAreNodesAliveRequest().getNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		final Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = deviceManager.getConnectedSubset(nodeUrns);

		postNodeNotConnectedResponse(requestId, unconnectedSubset);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			addBoolOperationListeners(requestId, deviceAdapter.areNodesAlive(connectedMap.get(deviceAdapter)));
		}
	}

	private void addVoidOperationListeners(final long requestId,
										   final ListenableFutureMap<NodeUrn, Void> futureMap) {

		for (NodeUrn nodeUrn : futureMap.keySet()) {
			futureMap.get(nodeUrn).addListener(
					createVoidOperationListener(requestId, nodeUrn, futureMap.get(nodeUrn)),
					SAME_THREAD_EXECUTOR
			);
		}
	}

	private void addVoidOperationListeners(final long requestId,
										   final ProgressListenableFutureMap<NodeUrn, Void> futureMap) {

		for (NodeUrn nodeUrn : futureMap.keySet()) {
			futureMap.get(nodeUrn).addListener(
					createVoidOperationListener(requestId, nodeUrn, futureMap.get(nodeUrn)),
					SAME_THREAD_EXECUTOR
			);
		}
	}

	private void addBoolOperationListeners(final long requestId,
										   final ListenableFutureMap<NodeUrn, Boolean> futureMap) {

		for (NodeUrn nodeUrn : futureMap.keySet()) {
			futureMap.get(nodeUrn).addListener(
					createBoolOperationListener(requestId, nodeUrn, futureMap.get(nodeUrn)),
					SAME_THREAD_EXECUTOR
			);
		}
	}

	private void addNodeApiOperationListeners(final long requestId,
											  final ListenableFutureMap<NodeUrn, NodeApiCallResult> futureMap) {

		for (NodeUrn nodeUrn : futureMap.keySet()) {
			futureMap.get(nodeUrn).addListener(
					createNodeApiOperationListener(requestId, nodeUrn, futureMap.get(nodeUrn)),
					SAME_THREAD_EXECUTOR
			);
		}
	}

	private Runnable createVoidOperationListener(final long requestId, final NodeUrn nodeUrn,
												 final ListenableFuture<Void> future) {
		return new Runnable() {
			@Override
			public void run() {

				if (future instanceof ProgressListenableFutureMap && !future.isDone()) {

					postProgress(requestId, nodeUrn, ((ProgressListenableFutureMap) future).getProgress());

				} else {

					try {

						future.get();
						postResponse(requestId, nodeUrn, 1, null);

					} catch (Exception e) {
						postRequestFailureResponse(requestId, nodeUrn, e);
					}
				}
			}
		};
	}

	private Runnable createNodeApiOperationListener(final long requestId, final NodeUrn nodeUrn,
													final ListenableFuture<NodeApiCallResult> future) {
		return new Runnable() {

			@Override
			public void run() {
				try {

					final NodeApiCallResult result = future.get();
					final byte statusCode = result.isSuccessful() ? 1 : result.getResponseType();
					postResponse(requestId, nodeUrn, statusCode, result.getResponse());

				} catch (Exception e) {
					postRequestFailureResponse(requestId, nodeUrn, e);
				}
			}
		};
	}

	private Runnable createBoolOperationListener(final long requestId, final NodeUrn nodeUrn,
												 final ListenableFuture<Boolean> future) {
		return new Runnable() {
			@Override
			public void run() {
				try {
					postResponse(requestId, nodeUrn, future.get() ? 1 : 0, null);
				} catch (Exception e) {
					postRequestFailureResponse(requestId, nodeUrn, e);
				}
			}
		};
	}

	private void postProgress(final long requestId, final NodeUrn nodeUrn, final float progress) {
		gatewayEventBus.post(SingleNodeProgress.newBuilder()
				.setRequestId(requestId)
				.setNodeUrn(nodeUrn.toString())
				.setProgressInPercent((int) progress * 100)
		);
	}

	private void postResponse(final long requestId, final NodeUrn nodeUrn, final int statusCode,
							  @Nullable final byte[] responseBytes) {

		final SingleNodeResponse.Builder responseBuilder = SingleNodeResponse.newBuilder()
				.setRequestId(requestId)
				.setNodeUrn(nodeUrn.toString())
				.setStatusCode(statusCode);

		if (responseBytes != null) {
			responseBuilder.setResponse(ByteString.copyFrom(responseBytes));
		}

		gatewayEventBus.post(responseBuilder.build());
	}

	private void postRequestFailureResponse(final long requestId, final NodeUrn nodeUrn, final Exception e) {
		gatewayEventBus.post(SingleNodeResponse.newBuilder()
				.setRequestId(requestId)
				.setNodeUrn(nodeUrn.toString())
				.setStatusCode(-2)
				.setErrorMessage(Throwables.getStackTraceAsString(e))
				.build()
		);
	}

	private void postNodeNotConnectedResponse(final long requestId, final Iterable<NodeUrn> unconnectedNodeUrns) {
		for (NodeUrn nodeUrn : unconnectedNodeUrns) {
			gatewayEventBus.post(SingleNodeResponse.newBuilder()
					.setRequestId(requestId)
					.setNodeUrn(nodeUrn.toString())
					.setStatusCode(-1)
					.setErrorMessage("Node \"" + nodeUrn + "\" is not connected.")
					.build()
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
