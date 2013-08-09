package de.uniluebeck.itm.tr.devicedb;

import eu.smartsantander.eventbroker.events.NodeOperationsEvents;
import eu.smartsantander.eventbroker.events.RegistrationEvents;
import eu.smartsantander.rd.jaxb.CapabilityType;
import eu.smartsantander.rd.jaxb.IoTNodeType;
import eu.smartsantander.rd.jaxb.KeyValuePair;
import eu.smartsantander.rd.jaxb.ResourceDescription;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.wiseml.Capability;
import eu.wisebed.wiseml.Coordinate;
import eu.wisebed.wiseml.Dtypes;
import eu.wisebed.wiseml.Units;

import java.util.*;

import static eu.smartsantander.rd.jaxb.IoTNodeType.MOBILE_SENSOR_NODE;
import static eu.smartsantander.rd.jaxb.IoTNodeType.SENSOR_NODE;


public abstract class DeviceDBRDHelper {

	public static Long[] getTimeouts(ResourceDescription rdResource) {
		Long[] timeouts = new Long[4];
		if (rdResource.getTimeoutMsReset() != null) {
			timeouts[0] = rdResource.getTimeoutMsReset().longValue();
		}
		if (rdResource.getTimeoutMsFlash() != null) {
			timeouts[1] = rdResource.getTimeoutMsFlash().longValue();
		}
		if (rdResource.getTimeoutMsNodeapi() != null) {
			timeouts[2] = rdResource.getTimeoutMsNodeapi().longValue();
		}
		if (rdResource.getTimeoutMsCheckalive() != null) {
			timeouts[3] = rdResource.getTimeoutMsCheckalive().longValue();
		}
		return timeouts;
	}


	public static Coordinate getCoordinates(ResourceDescription rdResource) {
		Coordinate coordinate = new Coordinate();
		if (rdResource.getPosition() != null && rdResource.getPosition().getOutdoorCoordinates() != null) {
			coordinate.setX(rdResource.getPosition().getOutdoorCoordinates().getLongitude());
			coordinate.setY(rdResource.getPosition().getOutdoorCoordinates().getLatitude());
		}
		return coordinate;
	}

	public static Map<String, String> getKeyValueConfig(ResourceDescription rdResource) {
		Map<String, String> answer = new HashMap<String, String>();
		if (rdResource.getTrConfiguration() != null && rdResource.getTrConfiguration().getKeyvalue() != null) {
			for (KeyValuePair pair : rdResource.getTrConfiguration().getKeyvalue()) {
				answer.put(pair.getKey(), pair.getValue());
			}
		}
		if (answer.size() == 0) {
			return null;
		}
		return answer;
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
		Coordinate coordinate = new Coordinate();
		if (eventResource.getPosition() != null) {
			coordinate.setX(eventResource.getPosition().getLongitude());
			coordinate.setY(eventResource.getPosition().getLatitude());
		}
		return coordinate;
	}


	public static Map<String, String> getKeyValueConfig(NodeOperationsEvents.AddSensorNode eventResource) {
		Map<String, String> answer = new HashMap<String, String>();
		if (eventResource.getNodeTrConfig() != null && eventResource.getNodeTrConfig().getNodeConfigList() != null) {
			for (RegistrationEvents.KeyValue pair : eventResource.getNodeTrConfig().getNodeConfigList()) {
				answer.put(pair.getKey(), pair.getValue());
			}
		}
		if (answer.size() == 0) {
			return null;
		}
		return answer;
	}

	public static DeviceConfig deviceConfigFromRDResource(ResourceDescription rdResource) {

		IoTNodeType type = rdResource.getResourceType();

		if (!(type.equals(SENSOR_NODE) || type.equals(MOBILE_SENSOR_NODE))) {
			return null;
		}

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
		Coordinate coordinate = getCoordinates(rdResource);
		Map<String, String> keyValues = getKeyValueConfig(rdResource);

		return new DeviceConfig(
				urn,
				type.toString(),
				false,
				rdResource.getNodePort(),
				rdResource.getNodeDesc(),
				null,
				keyValues,
				null,
				timeouts[3],
				timeouts[1],
				timeouts[2],
				timeouts[0],
				coordinate,
				capabilities
		);
	}

	public static DeviceConfig deviceConfigFromRDResource(NodeOperationsEvents.AddSensorNode eventResource) {

		final String type = eventResource.getIotNodeType().toString();

		if (!(SENSOR_NODE.toString().equals(type) || MOBILE_SENSOR_NODE.toString().equals(type))) {
			return null;
		}

		NodeUrn urn = new NodeUrn();
		urn.setNodeUrn(eventResource.getNodeId());

		List<RegistrationEvents.Capability> caps = eventResource.getSensorCapabilityList();
		Set<Capability> capabilities = new HashSet<Capability>();
		for (RegistrationEvents.Capability c : caps) {
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
		Map<String, String> keyValues = getKeyValueConfig(eventResource);

		return new DeviceConfig(
				urn,
				type,
				false,
				eventResource.getNodeTrConfig().getNodePort(),
				eventResource.getNodeDesc(),
				null,
				keyValues,
				null,
				timeouts[3],
				timeouts[1],
				timeouts[2],
				timeouts[0],
				null,
				capabilities
		);
	}


}
