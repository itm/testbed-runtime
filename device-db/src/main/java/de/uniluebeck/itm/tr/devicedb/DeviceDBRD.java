package de.uniluebeck.itm.tr.devicedb;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.AbstractService;
import eu.smartsantander.rd.rd3api.IRD3API;
import eu.smartsantander.rd.rd3api.RD3APIImpl;
import eu.smartsantander.rd.rd3api.RDAPIException;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.wiseml.Capability;
import rd3.model.RDNodeValueType;
import rd3.model.RDResource;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static de.uniluebeck.itm.util.StringUtils.parseHexOrDecLong;


public class DeviceDBRD extends AbstractService implements DeviceDB {

    private final List<DeviceConfig> configs = newArrayList();


    public DeviceDBRD() {
        this.bootstrapResourcesFromRD();
    }

    @Override
    public Map<NodeUrn, DeviceConfig> getConfigsByNodeUrns(final Iterable<NodeUrn> nodeUrns) {

        final Map<NodeUrn, DeviceConfig> map = newHashMap();
        synchronized (configs) {
            for (NodeUrn nodeUrn : nodeUrns) {
                for (DeviceConfig config : configs) {
                    if (config.getNodeUrn().equals(nodeUrn)) {
                        map.put(nodeUrn, config);
                    }
                }
            }
        }
        return map;
    }

    @Nullable
    @Override
    public DeviceConfig getConfigByUsbChipId(final String usbChipId) {
        if (usbChipId == null) {
            throw new IllegalArgumentException("usbChipId is null");
        }
        synchronized (configs) {
            for (DeviceConfig config : configs) {
                if (usbChipId.equals(config.getNodeUSBChipID())) {
                    return config;
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    public DeviceConfig getConfigByNodeUrn(final NodeUrn nodeUrn) {

        synchronized (configs) {
            for (DeviceConfig config : configs) {
                if (config.getNodeUrn().equals(nodeUrn)) {
                    return config;
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    public DeviceConfig getConfigByMacAddress(final long macAddress) {

        synchronized (configs) {
            for (DeviceConfig config : configs) {
                if (parseHexOrDecLong(config.getNodeUrn().getSuffix()) == macAddress) {
                    return config;
                }
            }
        }

        return null;
    }

    @Override
    public Iterable<DeviceConfig> getAll() {

        synchronized (configs) {
            return Iterables.unmodifiableIterable(configs);
        }
    }

    @Override
    public void add(final DeviceConfig deviceConfig) {

        synchronized (configs) {
            if (getConfigByNodeUrn(deviceConfig.getNodeUrn()) != null) {
                throw new IllegalArgumentException(deviceConfig.getNodeUrn() + " already exists!");
            }
            configs.add(deviceConfig);
        }
    }

    @Override
    public void update(final DeviceConfig deviceConfig) {

        synchronized (configs) {
            if (removeByNodeUrn(deviceConfig.getNodeUrn())) {
                configs.add(deviceConfig);
            } else {
                throw new IllegalArgumentException(deviceConfig.getNodeType() + " does not exist!");
            }
        }
    }

    @Override
    public boolean removeByNodeUrn(final NodeUrn nodeUrn) {

        synchronized (configs) {
            for (Iterator<DeviceConfig> iterator = configs.iterator(); iterator.hasNext(); ) {
                DeviceConfig next = iterator.next();
                if (next.getNodeUrn().equals(nodeUrn)) {
                    iterator.remove();
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void removeAll() {

        synchronized (configs) {
            configs.clear();
        }
    }

    @Override
    protected void doStart() {
        notifyStarted();
    }

    @Override
    protected void doStop() {
        notifyStopped();
    }




    DeviceConfig deviceConfigFromRDResource(RDResource rdResource) {

        Map<String, String> nodeConfig1 = new ImmutableMap.Builder<String, String>()
                .put("a", "b")
                .build();

        NodeUrn urn = new NodeUrn();
        urn.setNodeUrn(rdResource.getUid());

        RDNodeValueType type = rdResource.getType();
        Set<Capability> capabilities = DeviceDBRDManager.getCapabilities(rdResource);
        Long[] timeouts = DeviceDBRDManager.getTimeouts(rdResource);
        DeviceConfig devConfig = new DeviceConfig(
                urn,
                type.toString(),
                false,
                null,
                rdResource.toString(),
                null,
                nodeConfig1,
                null,
                timeouts[3],
                timeouts[1],
                timeouts[2],
                timeouts[0],
                null,
                capabilities
        );

        return devConfig;
    }


    public void bootstrapResourcesFromRD() {
        try {
            IRD3API rdv3API = RD3APIImpl.getInstance();
            Set<RDResource> rdResources = rdv3API.ResourceQuery("resource_type=SENSOR_NODE", null);
            System.out.println("Resources: " + rdResources.size());
            for (RDResource RDResource : rdResources) {
                System.out.println(RDResource.getUid());
                DeviceConfig deviceConfig = this.deviceConfigFromRDResource(RDResource);
                this.add(deviceConfig);
            }

        } catch (RDAPIException e) {
            e.printStackTrace();
        }
    }
}
