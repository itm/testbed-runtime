package de.uniluebeck.itm.tr.plugins.mockdeviceadapter;

import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfigList;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.gateway.ListenableDeviceAdapter;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiCallResult;
import de.uniluebeck.itm.util.concurrent.*;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;

public class MockDeviceAdapter extends ListenableDeviceAdapter {

	private static final Logger log = LoggerFactory.getLogger(MockDeviceAdapter.class);

	private final DeviceConfig deviceConfig;

	private final SchedulerService schedulerService;

	private final String deviceType;

	private final String devicePort;

	private final Map<String, String> deviceConfiguration;

	private ChannelHandlerConfigList channelHandlerConfigs;

	public MockDeviceAdapter(final String deviceType, final String devicePort,
							 final Map<String, String> deviceConfiguration,
							 @Nullable final DeviceConfig deviceConfig,
							 final SchedulerService schedulerService) {
		this.deviceType = deviceType;
		this.devicePort = devicePort;
		this.deviceConfiguration = deviceConfiguration;
		this.deviceConfig = checkNotNull(deviceConfig);
		this.schedulerService = schedulerService;
	}

	@Override
	protected void doStart() {
		try {
			log.trace("MockDeviceAdapter.doStart()");
			fireDevicesConnected(deviceConfig.getNodeUrn());
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			log.trace("MockDeviceAdapter.doStop()");
			fireDevicesDisconnected(deviceConfig.getNodeUrn());
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public String getDeviceType() {
		return deviceType;
	}

	@Override
	public String getDevicePort() {
		return devicePort;
	}

	@Override
	@Nullable
	public Map<String, String> getDeviceConfiguration() {
		return deviceConfiguration;
	}

	@Override
	@Nullable
	public DeviceConfig getDeviceConfig() {
		return deviceConfig;
	}

	@Override
	public Set<NodeUrn> getNodeUrns() {
		return newHashSet(deviceConfig.getNodeUrn());
	}

	@Override
	public ProgressListenableFutureMap<NodeUrn, Void> flashProgram(final Iterable<NodeUrn> nodeUrns,
																   final byte[] binaryImage) {

		log.trace("MockDeviceAdapter.flashProgram({}, {})", nodeUrns, binaryImage);

		final NodeUrn nodeUrn = deviceConfig.getNodeUrn();
		final ProgressSettableFuture<Void> future = ProgressSettableFuture.create();
		final ProgressSettableFutureMap<NodeUrn, Void> futureMap = ProgressSettableFutureMap.of(nodeUrn, future);

		for (int i = 1; i <10; i++) {
			schedulerService.schedule(createSetProgressRunnable(future, (float) i / 10f), i, TimeUnit.SECONDS);
		}

		return futureMap;
	}

	@Override
	public ListenableFutureMap<NodeUrn, Boolean> areNodesAlive(final Iterable<NodeUrn> nodeUrns) {
		log.trace("MockDeviceAdapter.areNodesAlive({})", nodeUrns);
		return ImmediateListenableFutureMap.of(deviceConfig.getNodeUrn(), true);
	}

	@Override
	public ListenableFutureMap<NodeUrn, Boolean> areNodesConnected(final Iterable<NodeUrn> nodeUrns) {
		log.trace("MockDeviceAdapter.areNodesConnected({})", nodeUrns);
		return ImmediateListenableFutureMap.of(deviceConfig.getNodeUrn(), true);
	}

	@Override
	public ListenableFutureMap<NodeUrn, Void> resetNodes(final Iterable<NodeUrn> nodeUrns) {
		log.trace("MockDeviceAdapter.resetNodes({})", nodeUrns);
		return ImmediateListenableFutureMap.of(deviceConfig.getNodeUrn(), null);
	}

	@Override
	public ListenableFutureMap<NodeUrn, Void> sendMessage(final Iterable<NodeUrn> nodeUrns, final byte[] messageBytes) {
		log.trace("MockDeviceAdapter.sendMessage({}, {})", nodeUrns, messageBytes);
		return ImmediateListenableFutureMap.of(deviceConfig.getNodeUrn(), null);
	}

	@Override
	public ListenableFutureMap<NodeUrn, Void> setChannelPipelines(final Iterable<NodeUrn> nodeUrns,
																  final ChannelHandlerConfigList channelHandlerConfigs) {
		log.trace("MockDeviceAdapter.setChannelPipelines({}, {})", nodeUrns, channelHandlerConfigs);
		this.channelHandlerConfigs = channelHandlerConfigs;
		return ImmediateListenableFutureMap.of(deviceConfig.getNodeUrn(), null);
	}

	@Override
	public ListenableFutureMap<NodeUrn, ChannelHandlerConfigList> getChannelPipelines(
			final Iterable<NodeUrn> nodeUrns) {
		log.trace("MockDeviceAdapter.getChannelPipelines({})", nodeUrns);
		return ImmediateListenableFutureMap.of(deviceConfig.getNodeUrn(), channelHandlerConfigs);
	}

	@Override
	public ListenableFutureMap<NodeUrn, NodeApiCallResult> enableNodes(final Iterable<NodeUrn> nodeUrns) {
		log.trace("MockDeviceAdapter.enableNodes({})", nodeUrns);
		return throwVirtualizationUnsupportedException();
	}

	@Override
	public ListenableFutureMap<NodeUrn, NodeApiCallResult> enablePhysicalLinks(
			final Map<NodeUrn, NodeUrn> sourceTargetMap) {
		log.trace("MockDeviceAdapter.enablePhysicalLinks({})", sourceTargetMap);
		return throwVirtualizationUnsupportedException();
	}

	@Override
	public ListenableFutureMap<NodeUrn, NodeApiCallResult> enableVirtualLinks(
			final Map<NodeUrn, NodeUrn> sourceTargetMap) {
		log.trace("MockDeviceAdapter.enableVirtualLinks({})", sourceTargetMap);
		return throwVirtualizationUnsupportedException();
	}

	@Override
	public ListenableFutureMap<NodeUrn, NodeApiCallResult> disableNodes(final Iterable<NodeUrn> nodeUrns) {
		log.trace("MockDeviceAdapter.disableNodes({})", nodeUrns);
		return throwVirtualizationUnsupportedException();
	}

	@Override
	public ListenableFutureMap<NodeUrn, NodeApiCallResult> disablePhysicalLinks(
			final Map<NodeUrn, NodeUrn> sourceTargetMap) {
		log.trace("MockDeviceAdapter.disablePhysicalLinks({})", sourceTargetMap);
		return throwVirtualizationUnsupportedException();
	}

	@Override
	public ListenableFutureMap<NodeUrn, NodeApiCallResult> disableVirtualLinks(
			final Map<NodeUrn, NodeUrn> sourceTargetMap) {
		log.trace("MockDeviceAdapter.disableVirtualLinks({})", sourceTargetMap);
		return throwVirtualizationUnsupportedException();
	}

	private ListenableFutureMap<NodeUrn, NodeApiCallResult> throwVirtualizationUnsupportedException() {
		throw new UnsupportedOperationException("Virtualization functionality is not supported by mock devices");
	}

	private Runnable createSetProgressRunnable(final ProgressSettableFuture<?> progressSettableFuture,
											   final float progress) {
		return new Runnable() {
			@Override
			public void run() {

				progressSettableFuture.setProgress(progress);

				if (progress == 1f) {
					progressSettableFuture.set(null);
				}
			}
		};
	}
}
