package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.base.Function;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfigList;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import eu.smartsantander.eventbroker.events.NodeOperationsEvents;
import eu.smartsantander.eventbroker.events.RegistrationEvents;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.wiseml.Capability;
import eu.wisebed.wiseml.Coordinate;
import eu.wisebed.wiseml.CoordinateType;
import eu.wisebed.wiseml.OutdoorCoordinatesType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public class SmartSantanderEventBrokerObserverConverter implements
		Function<NodeOperationsEvents.AddSensorNode, DeviceConfig> {

	@Override
	public DeviceConfig apply(final NodeOperationsEvents.AddSensorNode addSensorNode) {

		try {

			final RegistrationEvents.NodeTRConfig nodeTrConfig = addSensorNode.getNodeTrConfig();

			final NodeUrn nodeUrn = new NodeUrn(addSensorNode.getNodeId());
			final String nodeType = nodeTrConfig.getNodeType();
			final boolean gatewayNode =
					addSensorNode.getIotNodeType() == RegistrationEvents.RegRequestHeader.IoTNodeType.GATEWAY;
			final String nodePort = nodeTrConfig.getNodePort();

			final Long timeoutCheckAliveMillis = nodeTrConfig.hasTimeoutMsCheckalive() ?
					(long) nodeTrConfig.getTimeoutMsCheckalive() :
					null;
			final Long timeoutFlashMillis = nodeTrConfig.hasTimeoutMsFlash() ?
					(long) nodeTrConfig.getTimeoutMsFlash() :
					null;
			final Long timeoutNodeApiMillis = nodeTrConfig.hasTimeoutMsNodeapi() ?
					(long) nodeTrConfig.getTimeoutMsNodeapi() :
					null;
			final Long timeoutResetMillis = nodeTrConfig.hasTimeoutMsReset() ?
					(long) nodeTrConfig.getTimeoutMsReset() :
					null;

			final RegistrationEvents.Position pos = addSensorNode.getPosition();
			final Coordinate coordinate = new Coordinate();
			coordinate.setType(CoordinateType.OUTDOOR);
			coordinate.setOutdoorCoordinates(new OutdoorCoordinatesType()
					.withLatitude(pos.hasLatitude() ? (double) pos.getLatitude() : null)
					.withLongitude(pos.hasLongitude() ? (double) pos.getLongitude() : null)
					.withX(pos.hasXcoor() ? (double) pos.getXcoor() : null)
					.withY(pos.hasYcoor() ? (double) pos.getYcoor() : null)
					.withZ(pos.hasZcoor() ? (double) pos.getZcoor() : null)
			);
			final Set<Capability> capabilities = newHashSet();
			final List<RegistrationEvents.Capability> sensorCapabilityList = addSensorNode.getSensorCapabilityList();
			for (RegistrationEvents.Capability sensorCapability : sensorCapabilityList) {
				Capability capability = new Capability();
				capability.setName(sensorCapability.getName());
				capability.setDatatype(sensorCapability.getDatatype());
				capability.setUnit(sensorCapability.getUnit());
			}

			final Map<String, String> nodeConfiguration = new HashMap<String, String>();
			final List<RegistrationEvents.KeyValue> nodeConfigList = nodeTrConfig.getNodeConfigList();
			for (RegistrationEvents.KeyValue keyValue : nodeConfigList) {
				nodeConfiguration.put(keyValue.getKey(), keyValue.getValue());
			}

			String description = null;
			String nodeUSBChipID = null;
			ChannelHandlerConfigList defaultChannelPipeline = null;

			return new DeviceConfig(
					nodeUrn,
					nodeType,
					gatewayNode,
					nodePort,
					description,
					nodeUSBChipID,
					nodeConfiguration,
					defaultChannelPipeline,
					timeoutCheckAliveMillis,
					timeoutFlashMillis,
					timeoutNodeApiMillis,
					timeoutResetMillis,
					coordinate,
					capabilities
			);

		} catch (Exception e) {
			throw new IllegalArgumentException(
					"The information from the provided AddSensorNode object could not be used to create a valid device configuration",
					e
			);
		}
	}
}
