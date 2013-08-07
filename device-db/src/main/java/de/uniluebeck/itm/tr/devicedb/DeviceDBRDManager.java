package de.uniluebeck.itm.tr.devicedb;

import com.google.common.collect.ImmutableMap;
import eu.smartsantander.testbed.events.NodeOperationsEvents;
import eu.smartsantander.testbed.jaxb.resource.CapabilityType;
import eu.smartsantander.testbed.jaxb.resource.IoTNodeType;
import eu.smartsantander.testbed.jaxb.resource.ResourceDescription;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.wiseml.Capability;
import eu.wisebed.wiseml.Coordinate;
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


    public static Coordinate getCoordinates(ResourceDescription rdResource) {
        Coordinate coord = new Coordinate();
        if (rdResource.getPosition() != null && rdResource.getPosition().getOutdoorCoordinates()!=null){
            coord.setX(rdResource.getPosition().getOutdoorCoordinates().getLongitude());
            coord.setY(rdResource.getPosition().getOutdoorCoordinates().getLatitude());
         }
        return coord;
    }

    public static Long[] getTimeouts(NodeOperationsEvents.AddSensorNode eventResource) {
        Long[] timeouts = new Long[4];
        timeouts[0] = (long) eventResource.getNodeTrConfig().getTimeoutMsReset();
        timeouts[1] = (long) eventResource.getNodeTrConfig().getTimeoutMsFlash();
        timeouts[2] = (long) eventResource.getNodeTrConfig().getTimeoutMsNodeapi();
        timeouts[3] = (long) eventResource.getNodeTrConfig().getTimeoutMsCheckalive();
        return timeouts;
    }

    public static Coordinate getCoordinates(NodeOperationsEvents.AddSensorNode eventResource) {
        Coordinate coord = new Coordinate();
        if (eventResource.getPosition() != null){
            coord.setX(eventResource.getPosition().getLongitude());
            coord.setY(eventResource.getPosition().getLatitude());
        }
        return coord;
    }


    public static DeviceConfig deviceConfigFromRDResource(ResourceDescription rdResource) {

        IoTNodeType type = rdResource.getResourceType();
        if (!(type.equals(IoTNodeType.SENSOR_NODE) == true
                || type.equals(IoTNodeType.MOBILE_SENSOR_NODE) == true)) return null;


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
        Coordinate coord=getCoordinates(rdResource);

        DeviceConfig devConfig = new DeviceConfig(
                urn,
                type.toString(),
                false,
                rdResource.getNodePort(),
                rdResource.getNodeDesc(),
                null,
                null,
                null,
                timeouts[3],
                timeouts[1],
                timeouts[2],
                timeouts[0],
                coord,
                capabilities
        );

        return devConfig;
    }

    public static DeviceConfig deviceConfigFromRDResource(NodeOperationsEvents.AddSensorNode eventResource) {

        String type = eventResource.getIotNodeType().toString();
        if (!(type.equals(IoTNodeType.SENSOR_NODE) == true
                || type.equals(IoTNodeType.MOBILE_SENSOR_NODE) == true)) return null;

        NodeUrn urn = new NodeUrn();
        urn.setNodeUrn(eventResource.getNodeId());


        List<eu.smartsantander.testbed.events.RegistrationEvents.Capability> caps = eventResource.getSensorCapabilityList();
        Set<Capability> capabilities = new HashSet<Capability>();
        for (eu.smartsantander.testbed.events.RegistrationEvents.Capability c : caps) {
            Capability capability = new Capability();
            capability.setName(c.getName());
            if (c.getDatatype().equals("float")) {   //todo extend types
                capability.setDatatype(Dtypes.valueOf(Dtypes.DECIMAL.toString()));
            } else {
                capability.setDatatype(Dtypes.valueOf(c.getDatatype()));
            }
            if (c.getUnit().equals("lumen")) {                        //todo extend units
                // 1 lx = 1 lm/m2.
                capability.setUnit(Units.valueOf(Units.LUX.toString()));
            } else if (c.getUnit().equals("celsius")) {               //todo extend units
                capability.setUnit(Units.valueOf(Units.KELVIN.toString()));
            } else {
                capability.setUnit(Units.valueOf(c.getUnit()));
            }
            capabilities.add(capability);
        }
        Long[] timeouts = getTimeouts(eventResource);
        DeviceConfig devConfig = new DeviceConfig(
                urn,
                type.toString(),
                false,
                eventResource.getNodeTrConfig().getNodePort(),
                eventResource.getNodeDesc(),
                null,
                null,
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
