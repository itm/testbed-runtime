package de.uniluebeck.itm.tr.devicedb;

import com.google.common.collect.ImmutableMap;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.wiseml.Capability;
import eu.wisebed.wiseml.Dtypes;
import eu.wisebed.wiseml.Units;
import rd3.model.RDNode;
import rd3.model.RDNodeValueType;
import rd3.model.RDResource;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class DeviceDBRDManager {

    public static Long[] getTimeouts(RDResource rdResource) {
        Long[] timeouts = new Long[4];
        for (RDNode rdNode : rdResource.getChildren()) {
            if (rdNode.getName().equals("timeout_ms_reset")) {
                timeouts[0] = Long.parseLong(rdNode.getValue().toString());
            } else if (rdNode.getName().equals("timeout_ms_flash")) {
                timeouts[1] = Long.parseLong(rdNode.getValue().toString());
            } else if (rdNode.getName().equals("timeout_ms_nodeapi")) {
                timeouts[2] = Long.parseLong(rdNode.getValue().toString());
            } else if (rdNode.getName().equals("timeout_ms_checkalive")) {
                timeouts[3] = Long.parseLong(rdNode.getValue().toString());
            }
        }
        return timeouts;
    }

    public static Set<Capability> getCapabilities(RDResource rdResource) {
        Set<Capability> capabilities = new HashSet<Capability>();
        for (RDNode rdNode : rdResource.getChildren()) {
            if (rdNode.getName().equals("capability")) {
                String capabilityName = rdNode.getChild("phenomenon").getValue().toString();
                Dtypes dtype;
                if (rdNode.getChild("type").getValue().toString().equals("float"))
                    dtype = Dtypes.fromValue("decimal");
                else
                    dtype = Dtypes.fromValue("integer");
                Units unit;
                try {
                    unit = Units.fromValue(rdNode.getChild("type").getValue().toString());
                } catch (Exception e) {
                    unit = null;
                }
                System.out.println(capabilityName + "," + dtype + "," + unit);
                Capability cap = new Capability()
                        .withName(capabilityName)
                        .withDatatype(dtype)
                        .withUnit(unit);
                capabilities.add(cap);
            }
        }
        return capabilities;
    }

    DeviceConfig deviceConfigFromRDResource(RDResource rdResource) {

        Map<String, String> nodeConfig1 = new ImmutableMap.Builder<String, String>()
                .put("a", "b")
                .build();

        NodeUrn urn = new NodeUrn();
        urn.setNodeUrn(rdResource.getUid());

        RDNodeValueType type = rdResource.getType();
        Set<Capability> capabilities = getCapabilities(rdResource);
        Long[] timeouts = getTimeouts(rdResource);
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


}
