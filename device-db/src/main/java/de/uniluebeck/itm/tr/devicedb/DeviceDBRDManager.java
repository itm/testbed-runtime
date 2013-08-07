package de.uniluebeck.itm.tr.devicedb;

import com.google.common.collect.ImmutableMap;
import eu.smartsantander.testbed.jaxb.resource.CapabilityType;
import eu.smartsantander.testbed.jaxb.resource.IoTNodeType;
import eu.smartsantander.testbed.jaxb.resource.ResourceDescription;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.wiseml.Capability;
import eu.wisebed.wiseml.Dtypes;
import eu.wisebed.wiseml.Units;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class DeviceDBRDManager {

    public static Long[] getTimeouts(ResourceDescription rdResource) {
        Long[] timeouts = new Long[4];
        if (rdResource.getTimeoutMsReset() != null)
            timeouts[0] = rdResource.getTimeoutMsReset().longValue();
        if (rdResource.getTimeoutMsFlash() != null)
            timeouts[1] = rdResource.getTimeoutMsFlash().longValue();
        if (rdResource.getTimeoutMsNodeapi() != null)
            timeouts[2] = rdResource.getTimeoutMsNodeapi().longValue();
        if (rdResource.getTimeoutMsCheckalive() != null)
            timeouts[3] = rdResource.getTimeoutMsCheckalive().longValue();
        return timeouts;
    }

    public static DeviceConfig deviceConfigFromRDResource(ResourceDescription rdResource) {

        IoTNodeType type = rdResource.getResourceType();
        if (!(type.equals(IoTNodeType.SENSOR_NODE) == true
                || type.equals(IoTNodeType.MOBILE_SENSOR_NODE) == true) ) return null;


        Map<String, String> nodeConfig1 = new ImmutableMap.Builder<String, String>()
                .put("a", "b")
                .build();


        NodeUrn urn = new NodeUrn();
        urn.setNodeUrn(rdResource.getUid());


        List<CapabilityType> caps = rdResource.getCapabilities().getCapability();
        Set<Capability> capabilities = new HashSet<Capability>();
        for (CapabilityType c : caps) {
            Capability capability = new Capability();
            capability.setName(c.getPhenomenon());
            if (c.getType().equals("float")) {   //todo extend types
                c.setType(Dtypes.DECIMAL.toString());
            }

            capability.setDatatype(Dtypes.valueOf(c.getType()));
            if (c.getUom().equals("lumen")) {                        //todo extend units
                c.setUom(Units.LUX.toString());// 1 lx = 1 lm/m2.
            } else if (c.getUom().equals("celsius")) {
                c.setUom(Units.KELVIN.toString()); //todo extend units
            }
            capability.setUnit(Units.valueOf(c.getUom()));
            capabilities.add(capability);
        }
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
