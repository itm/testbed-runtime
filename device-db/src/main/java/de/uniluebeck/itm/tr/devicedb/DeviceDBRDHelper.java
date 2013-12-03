package de.uniluebeck.itm.tr.devicedb;

import eu.smartsantander.eventbroker.events.NodeOperationsEvents;
import eu.smartsantander.eventbroker.events.RegistrationEvents;
import eu.smartsantander.rd.jaxb.CapabilityType;
import eu.smartsantander.rd.jaxb.IoTNodeType;
import eu.smartsantander.rd.jaxb.KeyValuePair;
import eu.smartsantander.rd.jaxb.ResourceDescription;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.wiseml.*;

import javax.annotation.Nullable;
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

	@Nullable
	private static Double floatToDouble(@Nullable Float floatValue) {
		return floatValue == null ? null : Double.parseDouble(Float.toString(floatValue));
	}

	private static Double floatToDoubleNotNull(Float floatValue) {
		if (floatValue == null) {
			throw new NullPointerException("float value must not be null!");
		}
		return Double.parseDouble(Float.toString(floatValue));
	}

	public static Coordinate getCoordinates(ResourceDescription rdResource) {

		if (rdResource.getPosition() == null) {
			return null;
		}

		Coordinate coordinate = null;
		final eu.smartsantander.rd.jaxb.OutdoorCoordinatesType rdOC = rdResource
				.getPosition()
				.getOutdoorCoordinates();
		final eu.smartsantander.rd.jaxb.IndoorCoordinatesType rdIC = rdResource
				.getPosition()
				.getIndoorCoordinates();

		if (rdOC != null) {

			final OutdoorCoordinatesType wiseMLOC = new OutdoorCoordinatesType();
			wiseMLOC.setLatitude(rdOC.getLatitude() == null ? null : Double.parseDouble(
					Float.toString(rdOC.getLatitude())
			));
			wiseMLOC.setLongitude(floatToDouble(rdOC.getLongitude()));
			wiseMLOC.setX(floatToDouble(rdOC.getXcoor()));
			wiseMLOC.setY(floatToDouble(rdOC.getYcoor()));
			wiseMLOC.setZ(floatToDouble(rdOC.getZcoor()));

			coordinate = new Coordinate();
			coordinate.setType(CoordinateType.OUTDOOR);
			coordinate.setOutdoorCoordinates(wiseMLOC);
		}

		if (rdIC != null) {

			final IndoorCoordinatesType wiseMLIC = new IndoorCoordinatesType();
			wiseMLIC.setBackgroundimage(rdIC.getBackgroundimage());
			wiseMLIC.setBuilding(rdIC.getBuilding());
			wiseMLIC.setFloor(rdIC.getFloor());
			wiseMLIC.setRoom(rdIC.getRoom());
			wiseMLIC.setX(floatToDoubleNotNull(rdIC.getXcoor()));
			wiseMLIC.setY(floatToDoubleNotNull(rdIC.getYcoor()));
			wiseMLIC.setZ(floatToDoubleNotNull(rdIC.getZcoor()));

			coordinate = new Coordinate();
			coordinate.setType(CoordinateType.INDOOR);
			coordinate.setIndoorCoordinates(wiseMLIC);
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

		return answer;
	}


	public static Long[] getTimeouts(NodeOperationsEvents.AddSensorNode eventResource) {
		final RegistrationEvents.NodeTRConfig config = eventResource.getNodeTrConfig();
		Long[] timeouts = new Long[4];
		timeouts[0] = config.hasTimeoutMsReset() ? (long) config.getTimeoutMsReset() : null;
		timeouts[1] = config.hasTimeoutMsFlash() ? (long) config.getTimeoutMsFlash() : null;
		timeouts[2] = config.hasTimeoutMsNodeapi() ? (long) config.getTimeoutMsNodeapi() : null;
		timeouts[3] = config.hasTimeoutMsCheckalive() ? (long) config.getTimeoutMsCheckalive() : null;
		return timeouts;
	}

	public static Coordinate getCoordinates(NodeOperationsEvents.AddSensorNode eventResource) {

		Coordinate coordinate = null;
		final RegistrationEvents.Position pos = eventResource.getPosition();
		if (pos != null) {

			final OutdoorCoordinatesType outdoorCoordinates = new OutdoorCoordinatesType();
			outdoorCoordinates.setLatitude(pos.hasLatitude() ? floatToDouble(pos.getLatitude()) : null);
			outdoorCoordinates.setLongitude(pos.hasLongitude() ? floatToDouble(pos.getLongitude()) : null);
			outdoorCoordinates.setX(pos.hasXcoor() ? floatToDouble(pos.getXcoor()) : null);
			outdoorCoordinates.setY(pos.hasYcoor() ? floatToDouble(pos.getYcoor()) : null);
			outdoorCoordinates.setZ(pos.hasZcoor() ? floatToDouble(pos.getZcoor()) : null);

			coordinate = new Coordinate();
			coordinate.setType(CoordinateType.OUTDOOR);
			coordinate.setOutdoorCoordinates(outdoorCoordinates);
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

	public static DeviceConfig deviceConfigFromRDResource(ResourceDescription rdResource)
			throws IllegalArgumentException {

		IoTNodeType type = rdResource.getResourceType();

		if (!(type.equals(SENSOR_NODE) || type.equals(MOBILE_SENSOR_NODE))) {
			return null;
		}

		NodeUrn urn = new NodeUrn();
		urn.setNodeUrn(rdResource.getUid());

		Set<Capability> capabilities = convertCapabilitiesFromRDToWiseML(rdResource.getCapabilities().getCapability());
		Long[] timeouts = getTimeouts(rdResource);
		Coordinate coordinate = getCoordinates(rdResource);
		Map<String, String> keyValues = getKeyValueConfig(rdResource);

		keyValues.put("gateway_id", rdResource.getParentId());

		return new DeviceConfig(
				urn,
				rdResource.getNodeType(),
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

	private static Set<Capability> convertCapabilitiesFromRDToWiseML(final List<CapabilityType> caps) {
		Set<Capability> capabilities = new HashSet<Capability>();
		for (CapabilityType c : caps) {
			Capability capability = new Capability();
			capability.setName(c.getPhenomenon());
			capability.setDatatype(c.getType());
			capability.setUnit(c.getUom());
			capabilities.add(capability);
		}
		return capabilities;
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
			capability.setDatatype(c.getDatatype());
			capability.setUnit(c.getUnit());
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
				getCoordinates(eventResource),
				capabilities
		);
	}


}
