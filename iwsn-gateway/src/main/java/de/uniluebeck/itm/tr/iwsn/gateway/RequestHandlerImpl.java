package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeResponse;
import de.uniluebeck.itm.tr.util.ListenableFutureMap;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Set;

import static com.google.common.collect.Iterables.transform;
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
		// TODO implement
	}

	private void onSetChannelPipelinesRequest(final Request request) {
		log.trace("RequestHandlerImpl.onSetChannelPipelinesRequest({})", request);
		// TODO implement
	}

	private void onSendDownstreamMessagesRequest(final Request request) {
		log.trace("RequestHandlerImpl.onSendDownstreamMessagesRequest({})", request);
		// TODO implement
	}

	private void onResetNodesRequest(final Request request) {
		log.trace("RequestHandlerImpl.onResetNodesRequest({})", request);
		// TODO implement
	}

	private void onFlashImagesRequest(final Request request) {
		log.trace("RequestHandlerImpl.onFlashImagesRequest({})", request);
		// TODO implement
	}

	private void onEnablePhysicalLinksRequest(final Request request) {
		log.trace("RequestHandlerImpl.onEnablePhysicalLinksRequest({)", request);
		// TODO implement
	}

	private void onEnablesNodeRequest(final Request request) {
		log.trace("RequestHandlerImpl.onEnablesNodeRequest({})", request);
		// TODO implement
	}

	private void onDisablePhysicalLinksRequest(final Request request) {
		log.trace("RequestHandlerImpl.onDisablePhysicalLinksRequest({})", request);
		// TODO implement
	}

	private void onDisableNodesRequest(final Request request) {
		log.trace("RequestHandlerImpl.onDisableNodesRequest({})", request);
		// TODO implement
	}

	private void onDisableVirtualLinksRequest(final Request request) {
		log.trace("RequestHandlerImpl.onDisableVirtualLinksRequest({})", request);
		// TODO implement
	}

	private void onAreNodesConnectedRequest(final Request request) {

		log.trace("RequestHandlerImpl.onAreNodesConnectedRequest({})", request);

		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getAreNodesAliveRequest().getNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		postNodeNotConnectedResponse(request.getRequestId(), deviceManager.getUnconnectedSubset(nodeUrns));

		final Multimap<DeviceAdapter,NodeUrn> connectedMap = deviceManager.getConnectedSubset(nodeUrns);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			final Set<NodeUrn> adapterNodeUrns = newHashSet(connectedMap.get(deviceAdapter));
			final ListenableFutureMap<NodeUrn, Boolean> future = deviceAdapter.areNodesConnected(adapterNodeUrns);
			addBoolOperationListeners(requestId, future);
		}
	}

	private void onAreNodesAliveRequest(final Request request) {

		log.trace("RequestHandlerImpl.onAreNodesAliveRequest({})", request);

		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getAreNodesAliveRequest().getNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		postNodeNotConnectedResponse(requestId, deviceManager.getUnconnectedSubset(nodeUrns));

		final Multimap<DeviceAdapter,NodeUrn> connectedMap = deviceManager.getConnectedSubset(nodeUrns);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			final Set<NodeUrn> adapterNodeUrns = newHashSet(connectedMap.get(deviceAdapter));
			final ListenableFutureMap<NodeUrn, Boolean> future = deviceAdapter.areNodesAlive(adapterNodeUrns);
			addBoolOperationListeners(requestId, future);
		}
	}

	private void addBoolOperationListeners(final long requestId, final ListenableFutureMap<NodeUrn, Boolean> future) {
		for (NodeUrn nodeUrn : future.keySet()) {
			future.get(nodeUrn).addListener(
					createBoolOperationListener(requestId, nodeUrn, future.get(nodeUrn)),
					SAME_THREAD_EXECUTOR
			);
		}
	}

	private Runnable createBoolOperationListener(final long requestId, final NodeUrn nodeUrn,
												 final ListenableFuture<Boolean> future) {
		return new Runnable() {
			@Override
			public void run() {
				try {
					postResponse(requestId, nodeUrn, future.get() ? 1 : 0);
				} catch (Exception e) {
					postRequestFailureResponse(requestId, nodeUrn, e);
				}
			}
		};
	}

	private void postResponse(final long requestId, final NodeUrn nodeUrn, final int statusCode) {
		gatewayEventBus.post(SingleNodeResponse.newBuilder()
				.setRequestId(requestId)
				.setNodeUrn(nodeUrn.toString())
				.setStatusCode(statusCode)
				.build()
		);
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
}
