package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ListenableFuture;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeResponse;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;

public class GatewayDeviceRequestHandlerImpl extends AbstractService implements GatewayDeviceRequestHandler {

	private static final Logger log = LoggerFactory.getLogger(GatewayDeviceRequestHandler.class);

	private static final Function<? super String, ? extends NodeUrn> STRING_TO_NODE_URN =
			new Function<String, NodeUrn>() {
				@Nullable
				@Override
				public NodeUrn apply(@Nullable final String nodeUrnString) {
					return new NodeUrn(nodeUrnString);
				}
			};

	private final GatewayDeviceManager gatewayDeviceManager;

	private final GatewayEventBus gatewayEventBus;

	public GatewayDeviceRequestHandlerImpl(final GatewayDeviceManager gatewayDeviceManager,
										   final GatewayEventBus gatewayEventBus) {
		this.gatewayDeviceManager = gatewayDeviceManager;
		this.gatewayEventBus = gatewayEventBus;
	}

	@Override
	protected void doStart() {
		try {
			gatewayEventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
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
			case DESTROY_VIRTUAL_LINK:
				onDestroyVirtualLinksRequest(request);
				break;
			case DISABLE_NODE:
				onDisableNodesRequest(request);
				break;
			case DISABLE_PHYSICAL_LINK:
				onDisablePhysicalLinksRequest(request);
				break;
			case ENABLE_NODE:
				onEnablesNodeRequest(request);
				break;
			case ENABLE_PHYSICAL_LINK:
				onEnablePhysicalLinksRequest(request);
				break;
			case FLASH_PROGRAMS:
				onFlashImagesRequest(request);
				break;
			case RESET_NODES:
				onResetNodesRequest(request);
				break;
			case SEND_DOWNSTREAM_MESSAGE:
				onSendDownstreamMessagesRequest(request);
				break;
			case SET_CHANNEL_PIPELINE:
				onSetChannelPipelinesRequest(request);
				break;
			case SET_VIRTUAL_LINK:
				onSetVirtualLinksRequest(request);
				break;
			default:
				throw new RuntimeException("Unknown request type received!");
		}
	}

	private void onSetVirtualLinksRequest(final Request request) {
		// TODO implement
	}

	private void onSetChannelPipelinesRequest(final Request request) {
		// TODO implement
	}

	private void onSendDownstreamMessagesRequest(final Request request) {
		// TODO implement
	}

	private void onResetNodesRequest(final Request request) {
		// TODO implement
	}

	private void onFlashImagesRequest(final Request request) {
		// TODO implement
	}

	private void onEnablePhysicalLinksRequest(final Request request) {
		// TODO implement
	}

	private void onEnablesNodeRequest(final Request request) {
		// TODO implement
	}

	private void onDisablePhysicalLinksRequest(final Request request) {
		// TODO implement
	}

	private void onDisableNodesRequest(final Request request) {
		// TODO implement
	}

	private void onDestroyVirtualLinksRequest(final Request request) {
		// TODO implement
	}

	private void onAreNodesConnectedRequest(final Request request) {
		final Iterable<NodeUrn> nodeUrns =
				transform(request.getAreNodesAliveRequest().getNodeUrnsList(), STRING_TO_NODE_URN);

		postNodeNotConnectedResponse(request.getRequestId(), gatewayDeviceManager.getUnconnectedSubset(nodeUrns));

		for (Map.Entry<NodeUrn, GatewayDevice> entry : gatewayDeviceManager.getConnectedSubset(nodeUrns)
				.entrySet()) {

			final GatewayDevice device = entry.getValue();
			final NodeUrn nodeUrn = entry.getKey();

			final ListenableFuture<Boolean> future = device.isNodeConnected();
			future.addListener(createBoolOperationListener(request.getRequestId(), nodeUrn, future),
					sameThreadExecutor()
			);
		}
	}

	private void onAreNodesAliveRequest(final Request request) {

		final long requestId = request.getRequestId();
		final Iterable<NodeUrn> nodeUrns = transform(
				request.getAreNodesAliveRequest().getNodeUrnsList(),
				STRING_TO_NODE_URN
		);

		postNodeNotConnectedResponse(requestId, gatewayDeviceManager.getUnconnectedSubset(nodeUrns));

		for (Map.Entry<NodeUrn, GatewayDevice> entry : gatewayDeviceManager.getConnectedSubset(nodeUrns).entrySet()) {

			final GatewayDevice device = entry.getValue();
			final NodeUrn nodeUrn = entry.getKey();

			final ListenableFuture<Boolean> future = device.isNodeAlive();
			future.addListener(createBoolOperationListener(requestId, nodeUrn, future), sameThreadExecutor());
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
				.setErrorCode(statusCode)
				.build()
		);
	}

	private void postRequestFailureResponse(final long requestId, final NodeUrn nodeUrn, final Exception e) {
		gatewayEventBus.post(SingleNodeResponse.newBuilder()
				.setRequestId(requestId)
				.setNodeUrn(nodeUrn.toString())
				.setErrorCode(-2)
				.setErrorMessage(Throwables.getStackTraceAsString(e))
				.build()
		);
	}

	private void postNodeNotConnectedResponse(final long requestId, final Iterable<NodeUrn> unconnectedNodeUrns) {
		for (NodeUrn nodeUrn : unconnectedNodeUrns) {
			gatewayEventBus.post(SingleNodeResponse.newBuilder()
					.setRequestId(requestId)
					.setNodeUrn(nodeUrn.toString())
					.setErrorCode(-1)
					.setErrorMessage("Node \"" + nodeUrn + "\" is not connected.")
					.build()
			);
		}
	}
}
