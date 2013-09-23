package de.uniluebeck.itm.tr.iwsn.gateway;

import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfigList;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import eu.smartsantander.eventbroker.events.NodeOperationsEvents;
import eu.smartsantander.eventbroker.events.RegistrationEvents;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.wiseml.Capability;
import eu.wisebed.wiseml.Coordinate;
import eu.wisebed.wiseml.CoordinateType;
import eu.wisebed.wiseml.OutdoorCoordinatesType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public class SmartSantanderEventBrokerObserverHelper {

	private static final Logger log = LoggerFactory.getLogger(SmartSantanderEventBrokerObserverHelper.class);


	public static DeviceConfig convert(final NodeOperationsEvents.AddSensorNode addSensorNode) {

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

			final RegistrationEvents.Position position = addSensorNode.getPosition();
			final Coordinate coordinate = new Coordinate();
			coordinate.setType(CoordinateType.OUTDOOR);
			coordinate.setOutdoorCoordinates(new OutdoorCoordinatesType()
					.withLatitude(position.getLatitude())
					.withLongitude(position.getLongitude())
					.withX(position.getXcoor())
					.withY(position.getYcoor())
					.withZ(position.getZcoor())
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
			log.error(
					"The information from the provided AddSensorNode object could not be used to create a valid device configuration ",
					e
			);
			return null;
		}
	}
}
