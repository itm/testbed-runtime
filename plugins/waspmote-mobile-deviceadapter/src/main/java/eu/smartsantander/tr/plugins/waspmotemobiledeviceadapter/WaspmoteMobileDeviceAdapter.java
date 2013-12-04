package eu.smartsantander.tr.plugins.waspmotemobiledeviceadapter;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfigList;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.gateway.ListenableDeviceAdapter;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiCallResult;
import de.uniluebeck.itm.util.concurrent.*;
import es.unican.tlmat.smartsantander.waspmote.driver.MobileWaspmoteManager;
import es.unican.tlmat.smartsantander.waspmote.driver.multiplexer.AsyncDataAvailableListener;
import es.unican.tlmat.smartsantander.waspmote.driver.operation.OperationListener;
import es.unican.tlmat.smartsantander.waspmote.driver.operation.mobile.composed.SmartSantanderOTAPWithLoadOperation;
import es.unican.tlmat.smartsantander.waspmote.driver.operation.mobile.runnable.SmartSantanderResetOperation;
import es.unican.tlmat.smartsantander.waspmote.driver.operation.mobile.runnable.SmartSantanderScanNodesOperation;
import es.unican.tlmat.smartsantander.waspmote.driver.operation.mobile.runnable.SmartSantanderSendExperimentationMessageOperation;
import es.unican.tlmat.smartsantander.waspmote.driver.util.SmartSantanderNodeUrn;
import es.unican.tlmat.smartsantander.waspmote.lib.frame.payload.api.gw2node.ScanNodeMode;
import es.unican.tlmat.smartsantander.waspmote.lib.frame.payload.api.gw2node.WaspmoteBinData;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Iterables.transform;

public class WaspmoteMobileDeviceAdapter extends ListenableDeviceAdapter implements AsyncDataAvailableListener {

    private static final Logger LOG = LoggerFactory.getLogger(WaspmoteMobileDeviceAdapter.class);

    private static final Function<? super NodeUrn, ? extends SmartSantanderNodeUrn> NODE_URN_TO_SMARTSANTANDER_NODE_URN =
            new Function<NodeUrn, SmartSantanderNodeUrn>() {
                @Nullable
                @Override
                public SmartSantanderNodeUrn apply(@Nullable final NodeUrn nodeUrn) {
                    return new SmartSantanderNodeUrn(nodeUrn);
                }
            };

    private static final Function<? super SmartSantanderNodeUrn, ? extends NodeUrn> SMARTSANTANDER_NODE_URN_TO_NODE_URN =
            new Function<SmartSantanderNodeUrn, NodeUrn>() {
                @Nullable
                @Override
                public NodeUrn apply(@Nullable final SmartSantanderNodeUrn smartSantanderNodeUrn) {
                    return new NodeUrn(smartSantanderNodeUrn.getNodeUrn());
                }
            };

    private final Map<NodeUrn, ChannelHandlerConfigList> channelHandlerConfigs = Maps.newHashMap();
    private final Map<NodeUrn, DeviceConfig> registeredDeviceConfigs = Maps.newHashMap();
    private final Map<String, String> clusterConfiguration;

    private final String port;
    private final String type;

    private final long timeoutCheckAliveMillis;
    private final long timeoutResetMillis;
    private final long timeoutFlashMillis;
    private final long timeoutNodeApiMillis;

    private final MobileWaspmoteManager waspmoteManager;

    public WaspmoteMobileDeviceAdapter(final DeviceConfig deviceConfig,
                                       final String identity) {
        this.port = deviceConfig.getNodePort();
        this.type = deviceConfig.getNodeType();
        this.timeoutCheckAliveMillis = deviceConfig.getTimeoutCheckAliveMillis();
        this.timeoutFlashMillis = deviceConfig.getTimeoutFlashMillis();
        this.timeoutResetMillis = deviceConfig.getTimeoutResetMillis();
        this.timeoutNodeApiMillis = deviceConfig.getTimeoutNodeApiMillis();

        clusterConfiguration = deviceConfig.getNodeConfiguration();
        String zmqPubPort = port + clusterConfiguration.get("node_port1");
        String zmqRouterPort = port + clusterConfiguration.get("node_port2");
        // @TODO
        // What happens if we have different URN prefixes belonging to the same GW??.
        // We will need to deal with it in the future, for the moment we suppose that it is unique
        this.waspmoteManager = new MobileWaspmoteManager(
                identity, deviceConfig.getNodeUrn().getPrefix(), zmqPubPort, zmqRouterPort);
    }

    @Override
    protected void doStart() {
        try {
            LOG.trace("WaspmoteMobileDeviceAdapter.doStart()");
            this.waspmoteManager.addListener(this);
            notifyStarted();
        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    @Override
    protected void doStop() {
        try {
            LOG.trace("WaspmoteMobileDeviceAdapter.doStop()");
            for (SmartSantanderNodeUrn nodeUrn : waspmoteManager.getRegisteredNodes()) {
                waspmoteManager.unregisterNode(nodeUrn);
                registeredDeviceConfigs.remove(nodeUrn);
                fireDevicesDisconnected(nodeUrn);
            }
            this.waspmoteManager.removeListener(this);
            notifyStopped();
        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    @Override
    public Set<NodeUrn> getNodeUrns() {
        return ImmutableSet.copyOf(transform(waspmoteManager.getRegisteredNodes(), SMARTSANTANDER_NODE_URN_TO_NODE_URN));
    }

    @Override
    public String getDeviceType() {
        return type;
    }

    @Override
    public String getDevicePort() {
        return port;
    }

    @Nullable
    @Override
    public Map<String, String> getDeviceConfiguration() {
        return clusterConfiguration;
    }

    @Nullable
    @Override
    public Map<NodeUrn, DeviceConfig> getDeviceConfigs() {
        return ImmutableMap.copyOf(registeredDeviceConfigs);
    }

    public void registerDevice(final DeviceConfig deviceConfig) {
        SmartSantanderNodeUrn nodeUrn = new SmartSantanderNodeUrn(deviceConfig.getNodeUrn());
        waspmoteManager.registerNode(nodeUrn);
        SmartSantanderScanNodesOperation pingOperation = new SmartSantanderScanNodesOperation(
                ScanNodeMode.PING_MODE, null, Sets.newHashSet(nodeUrn), this.timeoutCheckAliveMillis);
        pingOperation.addListener(new OperationListener() {
            @Override
            public void onOperationSucceeded(Set<SmartSantanderNodeUrn> nodeUrnSet) {
                LOG.trace("WaspmoteMobileDeviceAdapter.onRegisterSucceeded({})", nodeUrnSet);
                registeredDeviceConfigs.put(nodeUrnSet.iterator().next(), deviceConfig);
                fireDevicesConnected(ImmutableSet.copyOf(transform(nodeUrnSet, SMARTSANTANDER_NODE_URN_TO_NODE_URN)));
            }

            @Override
            public void onOperationFailed(Set<SmartSantanderNodeUrn> nodeUrnSet, Exception E) {
                LOG.trace("WaspmoteMobileDeviceAdapter.onRegisterFailed({})", nodeUrnSet);
                for (SmartSantanderNodeUrn failedNodeUrn : nodeUrnSet) {
                    waspmoteManager.unregisterNode(failedNodeUrn);
                }
            }
        });
        //        waspmoteManager.submitOperation(pingOperation);
        registeredDeviceConfigs.put(nodeUrn, deviceConfig);
        fireDevicesConnected(deviceConfig.getNodeUrn());
    }

    @Override
    public void onAsynchronousDataAvailable(SmartSantanderNodeUrn nodeUrn, byte[] bytes) {
        this.fireBytesReceivedFromDevice(nodeUrn, bytes);
    }

    @Override
    public ListenableFutureMap<NodeUrn, Boolean> areNodesConnected(final Iterable<NodeUrn> nodeUrns) {
        LOG.trace("WaspmoteMobileDeviceAdapter.areNodesConnected({})", nodeUrns);
        Map<NodeUrn, Boolean> areNodesConnectedMap = Maps.newHashMap();
        Set<SmartSantanderNodeUrn> registeredNodes = this.waspmoteManager.getRegisteredNodes();
        for (NodeUrn nodeUrn : nodeUrns) {
            if (registeredNodes.contains(nodeUrn)) {
                areNodesConnectedMap.put(nodeUrn, true);
            } else {
                areNodesConnectedMap.put(nodeUrn, false);
            }
        }
        return ImmediateListenableFutureMap.of(areNodesConnectedMap);
    }

    @Override
    public ProgressListenableFutureMap<NodeUrn, Void> flashProgram(final Iterable<NodeUrn> nodeUrns, final byte[] binaryImage) {
        LOG.trace("WaspmoteMobileDeviceAdapter.flashProgram({}, {})", nodeUrns, binaryImage);
        final Map<NodeUrn, ProgressSettableFuture<Void>> settableFutureMap = Maps.newHashMap();
        for (NodeUrn nodeUrn : nodeUrns) {
            ProgressSettableFuture<Void> future = ProgressSettableFuture.create();
            settableFutureMap.put(nodeUrn, future);
        }
        final ProgressSettableFutureMap<NodeUrn, Void> futureMap = new ProgressSettableFutureMap(settableFutureMap);

        WaspmoteBinData program = new WaspmoteBinData(binaryImage);
        SmartSantanderOTAPWithLoadOperation otapOperation = new SmartSantanderOTAPWithLoadOperation(
                program, null,
                Sets.newTreeSet(transform(nodeUrns, NODE_URN_TO_SMARTSANTANDER_NODE_URN)), this.timeoutFlashMillis);
        otapOperation.addListener(new OperationListener() {
            @Override
            public void onOperationSucceeded(Set<SmartSantanderNodeUrn> nodeUrnSet) {
                for (NodeUrn nodeUrn : nodeUrnSet) {
                    ProgressSettableFuture<Void> future = settableFutureMap.get(nodeUrn);
                    future.set(null);
                }
            }

            @Override
            public void onOperationFailed(Set<SmartSantanderNodeUrn> nodeUrnSet, Exception E) {
                for (NodeUrn nodeUrn : nodeUrnSet) {
                    ProgressSettableFuture<Void> future = settableFutureMap.get(nodeUrn);
                    future.set(null);
                    future.setException(E);
                }
            }
        });
        waspmoteManager.submitOperation(otapOperation);
        return futureMap;
    }

    @Override
    public ListenableFutureMap<NodeUrn, Boolean> areNodesAlive(final Iterable<NodeUrn> nodeUrns) {
        LOG.trace("WaspmoteMobileDeviceAdapter.areNodesAlive({})", nodeUrns);
        final Map<NodeUrn, SettableFuture<Boolean>> settableFutureMap = Maps.newHashMap();
        for (NodeUrn nodeUrn : nodeUrns) {
            SettableFuture<Boolean> future = SettableFuture.create();
            settableFutureMap.put(nodeUrn, future);
        }
        final SettableFutureMap<NodeUrn, Boolean> futureMap = new SettableFutureMap(settableFutureMap);

        SmartSantanderScanNodesOperation pingOperation = new SmartSantanderScanNodesOperation(
                ScanNodeMode.PING_MODE, null, Sets.newTreeSet(transform(nodeUrns, NODE_URN_TO_SMARTSANTANDER_NODE_URN)), this.timeoutCheckAliveMillis);
        pingOperation.addListener(new OperationListener() {
            @Override
            public void onOperationSucceeded(Set<SmartSantanderNodeUrn> nodeUrnSet) {
                for (NodeUrn nodeUrn : nodeUrnSet) {
                    SettableFuture<Boolean> future = settableFutureMap.get(nodeUrn);
                    future.set(true);
                }
            }

            @Override
            public void onOperationFailed(Set<SmartSantanderNodeUrn> nodeUrnSet, Exception E) {
                for (NodeUrn nodeUrn : nodeUrnSet) {
                    SettableFuture<Boolean> future = settableFutureMap.get(nodeUrn);
                    future.set(false);
                    future.setException(E);
                }
            }
        });
        waspmoteManager.submitOperation(pingOperation);

        return futureMap;
    }

    @Override
    public ListenableFutureMap<NodeUrn, Void> resetNodes(final Iterable<NodeUrn> nodeUrns) {
        LOG.trace("WaspmoteMobileDeviceAdapter.resetNodes({})", nodeUrns);
        final Map<NodeUrn, SettableFuture<Void>> settableFutureMap = Maps.newHashMap();
        for (NodeUrn nodeUrn : nodeUrns) {
            SettableFuture<Void> future = SettableFuture.create();
            settableFutureMap.put(nodeUrn, future);
        }
        final SettableFutureMap<NodeUrn, Void> futureMap = new SettableFutureMap(settableFutureMap);

        SmartSantanderResetOperation resetOperation = new SmartSantanderResetOperation(
                null, Sets.newTreeSet(transform(nodeUrns, NODE_URN_TO_SMARTSANTANDER_NODE_URN)), this.timeoutResetMillis);
        resetOperation.addListener(new OperationListener() {
            @Override
            public void onOperationSucceeded(Set<SmartSantanderNodeUrn> nodeUrnSet) {
                for (NodeUrn nodeUrn : nodeUrnSet) {
                    SettableFuture<Void> future = settableFutureMap.get(nodeUrn);
                    future.set(null);
                }
            }

            @Override
            public void onOperationFailed(Set<SmartSantanderNodeUrn> nodeUrnSet, Exception E) {
                for (NodeUrn nodeUrn : nodeUrnSet) {
                    SettableFuture<Void> future = settableFutureMap.get(nodeUrn);
                    future.setException(E);
                }
            }
        });
        waspmoteManager.submitOperation(resetOperation);

        return futureMap;
    }

    @Override
    public ListenableFutureMap<NodeUrn, Void> sendMessage(final Iterable<NodeUrn> nodeUrns, final byte[] messageBytes) {
        LOG.trace("WaspmoteMobileDeviceAdapter.sendMessage({}, {})", nodeUrns, messageBytes);
        final Map<NodeUrn, SettableFuture<Void>> settableFutureMap = Maps.newHashMap();
        for (NodeUrn nodeUrn : nodeUrns) {
            SettableFuture<Void> future = SettableFuture.create();
            settableFutureMap.put(nodeUrn, future);
        }
        final SettableFutureMap<NodeUrn, Void> futureMap = new SettableFutureMap(settableFutureMap);

        SmartSantanderSendExperimentationMessageOperation sendExperimentationMessageOperation = new SmartSantanderSendExperimentationMessageOperation(
                messageBytes, null, Sets.newTreeSet(transform(nodeUrns, NODE_URN_TO_SMARTSANTANDER_NODE_URN)), this.timeoutCheckAliveMillis);
        sendExperimentationMessageOperation.addListener(new OperationListener() {
            @Override
            public void onOperationSucceeded(Set<SmartSantanderNodeUrn> nodeUrnSet) {
                for (NodeUrn nodeUrn : nodeUrnSet) {
                    SettableFuture<Void> future = settableFutureMap.get(nodeUrn);
                    future.set(null);
                }
            }

            @Override
            public void onOperationFailed(Set<SmartSantanderNodeUrn> nodeUrnSet, Exception E) {
                for (NodeUrn nodeUrn : nodeUrnSet) {
                    SettableFuture<Void> future = settableFutureMap.get(nodeUrn);
                    future.setException(E);
                }
            }
        });
        waspmoteManager.submitOperation(sendExperimentationMessageOperation);

        return futureMap;
    }

    @Override
    public ListenableFutureMap<NodeUrn, Void> setChannelPipelines(final Iterable<NodeUrn> nodeUrns,
                                                                  final ChannelHandlerConfigList channelHandlerConfigs) {
        LOG.trace("WaspmoteMobileDeviceAdapter.setChannelPipelines({}, {})", nodeUrns, channelHandlerConfigs);
        for (NodeUrn nodeUrn : nodeUrns) {
            this.channelHandlerConfigs.put(nodeUrn, channelHandlerConfigs);
        }
        return ImmediateListenableFutureMap.of(this.channelHandlerConfigs.keySet(), (Void) null);
    }

    @Override
    public ListenableFutureMap<NodeUrn, ChannelHandlerConfigList> getChannelPipelines(final Iterable<NodeUrn> nodeUrns) {
        LOG.trace("WaspmoteMobileDeviceAdapter.getChannelPipelines({})", nodeUrns);
        return ImmediateListenableFutureMap.of(this.channelHandlerConfigs);
    }

    @Override
    public ListenableFutureMap<NodeUrn, NodeApiCallResult> enableNodes(final Iterable<NodeUrn> nodeUrns) {
        LOG.trace("WaspmoteMobileDeviceAdapter.enableNodes({})", nodeUrns);
        return throwVirtualizationUnsupportedException();
    }

    @Override
    public ListenableFutureMap<NodeUrn, NodeApiCallResult> enablePhysicalLinks(final Map<NodeUrn, NodeUrn> sourceTargetMap) {
        LOG.trace("WaspmoteMobileDeviceAdapter.enablePhysicalLinks({})", sourceTargetMap);
        return throwVirtualizationUnsupportedException();
    }

    @Override
    public ListenableFutureMap<NodeUrn, NodeApiCallResult> enableVirtualLinks(final Map<NodeUrn, NodeUrn> sourceTargetMap) {
        LOG.trace("WaspmoteMobileDeviceAdapter.enableVirtualLinks({})", sourceTargetMap);
        return throwVirtualizationUnsupportedException();
    }

    @Override
    public ListenableFutureMap<NodeUrn, NodeApiCallResult> disableNodes(final Iterable<NodeUrn> nodeUrns) {
        LOG.trace("WaspmoteMobileDeviceAdapter.disableNodes({})", nodeUrns);
        return throwVirtualizationUnsupportedException();
    }

    @Override
    public ListenableFutureMap<NodeUrn, NodeApiCallResult> disablePhysicalLinks(
            final Map<NodeUrn, NodeUrn> sourceTargetMap) {
        LOG.trace("WaspmoteMobileDeviceAdapter.disablePhysicalLinks({})", sourceTargetMap);
        return throwVirtualizationUnsupportedException();
    }

    @Override
    public ListenableFutureMap<NodeUrn, NodeApiCallResult> disableVirtualLinks(
            final Map<NodeUrn, NodeUrn> sourceTargetMap) {
        LOG.trace("WaspmoteMobileDeviceAdapter.disableVirtualLinks({})", sourceTargetMap);
        return throwVirtualizationUnsupportedException();
    }

    private ListenableFutureMap<NodeUrn, NodeApiCallResult> throwVirtualizationUnsupportedException() {
        throw new UnsupportedOperationException("Virtualization functionality is not supported by waspmote devices");
    }

}
