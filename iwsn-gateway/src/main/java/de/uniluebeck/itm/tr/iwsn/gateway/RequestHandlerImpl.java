package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfig;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfigList;
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
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static java.util.Optional.empty;
import static java.util.Optional.of;

public class RequestHandlerImpl extends AbstractService implements RequestHandler {

	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

	private static final Executor SAME_THREAD_EXECUTOR = directExecutor();

	private final DeviceManager deviceManager;

	private final GatewayEventBus gatewayEventBus;

	private final MessageFactory messageFactory;

	@Inject
	public RequestHandlerImpl(final DeviceManager deviceManager,
							  final GatewayEventBus gatewayEventBus,
							  final MessageFactory messageFactory) {
		this.deviceManager = deviceManager;
		this.gatewayEventBus = gatewayEventBus;
		this.messageFactory = messageFactory;
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
	public void onGetChannelPipelinesRequest(final GetChannelPipelinesRequest request) {

		log.trace("RequestHandlerImpl.onGetChannelPipelinesRequest({})", request);

		final Header requestHeader = request.getHeader();
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = handleUnconnectedAndReturnConnected(requestHeader);

		for (DeviceAdapter deviceAdapter : connectedMap.keys()) {

			try {

				final ListenableFutureMap<NodeUrn, ChannelHandlerConfigList> mapFuture = deviceAdapter
						.getChannelPipelines(connectedMap.get(deviceAdapter));
				final Map<NodeUrn, ChannelHandlerConfigList> map = mapFuture.get(30, TimeUnit.SECONDS);
				final Map<NodeUrn, List<ChannelHandlerConfiguration>> responseMap = Maps.transformEntries(map,
						(key, pipeline) -> {
							final List<ChannelHandlerConfiguration> res = newArrayList();
							if (pipeline != null) {
								for (ChannelHandlerConfig chc : pipeline) {
									res.add(convert(chc));
								}
							}
							return res;
						}
				);

				gatewayEventBus.post(messageFactory.getChannelPipelinesResponse(
						requestHeader.hasSerializedReservationKey() ?
								of(requestHeader.getSerializedReservationKey()) :
								empty(),
						empty(),
						responseMap
				));

			} catch (Exception e) {
				throw propagate(e);
			}
		}
	}

	@Subscribe
	public void onEnableVirtualLinksRequest(final EnableVirtualLinksRequest request) {

		log.trace("RequestHandlerImpl.onEnableVirtualLinksRequest({})", request);

		final Header requestHeader = request.getHeader();
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = handleUnconnectedAndReturnConnected(requestHeader);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			Map<NodeUrn, NodeUrn> linksMap = createLinkMap(request.getLinksList(), connectedMap.get(deviceAdapter));
			addNodeApiOperationListeners(requestHeader, deviceAdapter.enableVirtualLinks(linksMap));
		}
	}

	@Subscribe
	public void onSetChannelPipelinesRequest(final SetChannelPipelinesRequest request) {

		log.trace("RequestHandlerImpl.onSetChannelPipelinesRequest({})", request);

		final Header requestHeader = request.getHeader();
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = handleUnconnectedAndReturnConnected(requestHeader);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {

			final Collection<NodeUrn> nodeUrnsToSetChannelPipelineOn = connectedMap.get(deviceAdapter);
			final List<ChannelHandlerConfiguration> channelHandlerConfigurationsList =
					request.getChannelHandlerConfigurationsList();

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
					requestHeader,
					1,
					deviceAdapter.setChannelPipelines(nodeUrnsToSetChannelPipelineOn, cp)
			);
		}
	}

	@Subscribe
	public void onSendDownstreamMessagesRequest(final SendDownstreamMessagesRequest request) {

		log.trace("RequestHandlerImpl.onSendDownstreamMessagesRequest({})", request);

		final Header requestHeader = request.getHeader();
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = handleUnconnectedAndReturnConnected(requestHeader);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {

			final Collection<NodeUrn> nodeUrnsToSendTo = connectedMap.get(deviceAdapter);
			final byte[] messageBytes = request.getMessageBytes().toByteArray();

			addVoidOperationListeners(requestHeader, 1, deviceAdapter.sendMessage(nodeUrnsToSendTo, messageBytes));
		}
	}

	@Subscribe
	public void onResetNodesRequest(final ResetNodesRequest request) {

		log.trace("RequestHandlerImpl.onResetNodesRequest({})", request);

		final Header requestHeader = request.getHeader();
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = handleUnconnectedAndReturnConnected(requestHeader);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			addVoidOperationListeners(requestHeader, 1, deviceAdapter.resetNodes(connectedMap.get(deviceAdapter)));
		}
	}

	@Subscribe
	public void onFlashImagesRequest(final FlashImagesRequest request) {

		log.trace("RequestHandlerImpl.onFlashImagesRequest({})", request);

		final Header requestHeader = request.getHeader();
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = handleUnconnectedAndReturnConnected(requestHeader);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {

			final byte[] binaryImage = request.getImage().toByteArray();
			final Collection<NodeUrn> nodeUrnsToFlash = connectedMap.get(deviceAdapter);

			addVoidOperationListeners(requestHeader, 100, deviceAdapter.flashProgram(nodeUrnsToFlash, binaryImage));
		}
	}

	@Subscribe
	public void onEnablePhysicalLinksRequest(final EnablePhysicalLinksRequest request) {

		log.trace("RequestHandlerImpl.onEnablePhysicalLinksRequest({)", request);

		final Header requestHeader = request.getHeader();
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = handleUnconnectedAndReturnConnected(requestHeader);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			final Map<NodeUrn, NodeUrn> linksMap = createLinkMap(request.getLinksList(), connectedMap.get(deviceAdapter));
			addNodeApiOperationListeners(requestHeader, deviceAdapter.enablePhysicalLinks(linksMap));
		}
	}

	@Subscribe
	public void onEnablesNodeRequest(final EnableNodesRequest request) {

		log.trace("RequestHandlerImpl.onEnablesNodeRequest({})", request);

		final Header requestHeader = request.getHeader();
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = handleUnconnectedAndReturnConnected(requestHeader);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			addNodeApiOperationListeners(requestHeader, deviceAdapter.enableNodes(connectedMap.get(deviceAdapter)));
		}
	}

	@Subscribe
	public void onDisablePhysicalLinksRequest(final DisablePhysicalLinksRequest request) {

		log.trace("RequestHandlerImpl.onDisablePhysicalLinksRequest({})", request);

		final Header requestHeader = request.getHeader();
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = handleUnconnectedAndReturnConnected(requestHeader);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			final Map<NodeUrn, NodeUrn> linksMap = createLinkMap(request.getLinksList(), connectedMap.get(deviceAdapter));
			addNodeApiOperationListeners(requestHeader, deviceAdapter.disablePhysicalLinks(linksMap));
		}
	}

	@Subscribe
	public void onDisableNodesRequest(final DisableNodesRequest request) {

		log.trace("RequestHandlerImpl.onDisableNodesRequest({})", request);

		final Header requestHeader = request.getHeader();
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = handleUnconnectedAndReturnConnected(requestHeader);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			addNodeApiOperationListeners(requestHeader, deviceAdapter.disableNodes(connectedMap.get(deviceAdapter)));
		}
	}

	@Subscribe
	public void onDisableVirtualLinksRequest(final DisableVirtualLinksRequest request) {

		log.trace("RequestHandlerImpl.onDisableVirtualLinksRequest({})", request);

		final Header requestHeader = request.getHeader();
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = handleUnconnectedAndReturnConnected(requestHeader);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			final Map<NodeUrn, NodeUrn> linksMap = createLinkMap(request.getLinksList(), connectedMap.get(deviceAdapter));
			addNodeApiOperationListeners(requestHeader, deviceAdapter.disableVirtualLinks(linksMap));
		}
	}

	@Subscribe
	public void onAreNodesConnectedRequest(final AreNodesConnectedRequest request) {

		log.trace("RequestHandlerImpl.onAreNodesConnectedRequest({})", request);

		final Header requestHeader = request.getHeader();
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = handleUnconnectedAndReturnConnected(requestHeader);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			addBoolOperationListeners(requestHeader, deviceAdapter.areNodesConnected(connectedMap.get(deviceAdapter)));
		}
	}

	@Subscribe
	public void onAreNodesAliveRequest(final AreNodesAliveRequest request) {

		log.trace("RequestHandlerImpl.onAreNodesAliveRequest({})", request);

		final Header requestHeader = request.getHeader();
		final Multimap<DeviceAdapter, NodeUrn> connectedMap = handleUnconnectedAndReturnConnected(requestHeader);

		for (DeviceAdapter deviceAdapter : connectedMap.keySet()) {
			addBoolOperationListeners(requestHeader, deviceAdapter.areNodesAlive(connectedMap.get(deviceAdapter)));
		}
	}

	private void addVoidOperationListeners(final Header requestHeader,
										   final int completionStatusCode,
										   final ListenableFutureMap<NodeUrn, Void> futureMap) {

		for (NodeUrn nodeUrn : futureMap.keySet()) {
			futureMap.get(nodeUrn).addListener(
					createVoidOperationListener(
							requestHeader,
							nodeUrn,
							completionStatusCode,
							futureMap.get(nodeUrn)
					),
					SAME_THREAD_EXECUTOR
			);
		}
	}

	private void addVoidOperationListeners(final Header requestHeader,
										   final int completionStatusCode,
										   final ProgressListenableFutureMap<NodeUrn, Void> futureMap) {

		for (NodeUrn nodeUrn : futureMap.keySet()) {
			final Runnable listener = createVoidOperationListener(
					requestHeader,
					nodeUrn,
					completionStatusCode,
					futureMap.get(nodeUrn)
			);
			futureMap.get(nodeUrn).addListener(listener, SAME_THREAD_EXECUTOR);
			futureMap.get(nodeUrn).addProgressListener(listener, SAME_THREAD_EXECUTOR);
		}
	}

	private void addBoolOperationListeners(final Header requestHeader,
										   final ListenableFutureMap<NodeUrn, Boolean> futureMap) {

		for (NodeUrn nodeUrn : futureMap.keySet()) {
			final Runnable listener = createBoolOperationListener(requestHeader, nodeUrn, futureMap.get(nodeUrn));
			futureMap.get(nodeUrn).addListener(listener, SAME_THREAD_EXECUTOR);
		}
	}

	private void addNodeApiOperationListeners(final Header requestHeader,
											  final ListenableFutureMap<NodeUrn, NodeApiCallResult> futureMap) {

		for (NodeUrn nodeUrn : futureMap.keySet()) {
			final Runnable listener = createNodeApiOperationListener(requestHeader, nodeUrn, futureMap.get(nodeUrn));
			futureMap.get(nodeUrn).addListener(listener, SAME_THREAD_EXECUTOR);
		}
	}

	private Runnable createVoidOperationListener(final Header requestHeader,
												 final NodeUrn nodeUrn,
												 final int completionStatusCode,
												 final ListenableFuture<Void> future) {
		return () -> {

			if (future instanceof ProgressListenableFuture && !future.isDone()) {

				final float progress = ((ProgressListenableFuture) future).getProgress();
				if (progress < 1f) {
					postProgress(requestHeader, nodeUrn, progress);
				}

			} else if (future.isDone()) {

				try {
					future.get(); // check if exception occurred
					postResponse(requestHeader, nodeUrn, completionStatusCode, null);
				} catch (Exception e) {
					postRequestFailureResponse(requestHeader, nodeUrn, e);
				}
			}
		};
	}

	private Runnable createNodeApiOperationListener(final Header requestHeader,
													final NodeUrn nodeUrn,
													final ListenableFuture<NodeApiCallResult> future) {
		return () -> {

			try {

				final NodeApiCallResult result = future.get();
				final byte statusCode = result.isSuccessful() ? 1 : result.getResponseType();
				postResponse(requestHeader, nodeUrn, statusCode, result.getResponse());

			} catch (Exception e) {
				postRequestFailureResponse(requestHeader, nodeUrn, e);
			}
		};
	}

	private Runnable createBoolOperationListener(final Header requestHeader,
												 final NodeUrn nodeUrn,
												 final ListenableFuture<Boolean> future) {
		return () -> {
			try {
				postResponse(requestHeader, nodeUrn, future.get() ? 1 : 0, null);
			} catch (Exception e) {
				postRequestFailureResponse(requestHeader, nodeUrn, e);
			}
		};
	}

	private void postProgress(final Header requestHeader,
							  final NodeUrn nodeUrn,
							  final float progress) {
		gatewayEventBus.post(
				messageFactory.progress(
						requestHeader.hasSerializedReservationKey() ?
								of(requestHeader.getSerializedReservationKey()) :
								empty(),
						empty(),
						requestHeader.getCorrelationId(),
						newArrayList(nodeUrn),
						(int) (progress * 100)
				)
		);
	}

	private void postResponse(final Header requestHeader,
							  final NodeUrn nodeUrn,
							  final int statusCode,
							  @Nullable final byte[] responseBytes) {
		gatewayEventBus.post(messageFactory.response(
				requestHeader.hasSerializedReservationKey() ? of(requestHeader.getSerializedReservationKey()) : empty(),
				empty(),
				requestHeader.getCorrelationId(),
				newArrayList(nodeUrn),
				statusCode,
				empty(),
				responseBytes == null ? empty() : of(responseBytes)
		));
	}

	private void postRequestFailureResponse(final Header requestHeader,
											final NodeUrn nodeUrn,
											final Exception e) {
		gatewayEventBus.post(messageFactory.response(
				requestHeader.hasSerializedReservationKey() ? of(requestHeader.getSerializedReservationKey()) : empty(),
				empty(),
				requestHeader.getCorrelationId(),
				newArrayList(nodeUrn),
				-2,
				of(Throwables.getStackTraceAsString(e)),
				empty()
		));
	}

	private void postNodeNotConnectedResponse(final Header requestHeader,
											  final Iterable<NodeUrn> unconnectedNodeUrns) {
		gatewayEventBus.post(messageFactory.response(
				requestHeader.hasSerializedReservationKey() ?
						of(requestHeader.getSerializedReservationKey()) :
						empty(),
				empty(),
				requestHeader.getCorrelationId(),
				unconnectedNodeUrns,
				MessageUtils.getUnconnectedStatusCode(requestHeader.getType()),
				of("Node URNs [" + Joiner.on(",").join(unconnectedNodeUrns) + "] are not connected."),
				empty()
		));
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

	private Multimap<DeviceAdapter, NodeUrn> handleUnconnectedAndReturnConnected(Header requestHeader) {

		Iterable<NodeUrn> nodeUrns = transform(requestHeader.getNodeUrnsList(), NodeUrn::new);
		Iterable<NodeUrn> unconnectedSubset = deviceManager.getUnconnectedSubset(nodeUrns);
		postNodeNotConnectedResponse(requestHeader, unconnectedSubset);

		return deviceManager.getConnectedSubset(nodeUrns);
	}
}
