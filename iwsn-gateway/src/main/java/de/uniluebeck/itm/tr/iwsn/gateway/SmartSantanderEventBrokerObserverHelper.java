package de.uniluebeck.itm.tr.iwsn.gateway;

import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfigList;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import eu.smartsantander.testbed.events.NodeOperationsEvents;
import eu.smartsantander.testbed.events.RegistrationEvents;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.wiseml.Capability;
import eu.wisebed.wiseml.Coordinate;
import eu.wisebed.wiseml.Dtypes;
import eu.wisebed.wiseml.Units;
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
			final Long timeoutCheckAliveMillis = (long) nodeTrConfig.getTimeoutMsCheckalive();
			final Long timeoutFlashMillis = (long) nodeTrConfig.getTimeoutMsFlash();
			final Long timeoutNodeApiMillis = (long) nodeTrConfig.getTimeoutMsNodeapi();
			final Long timeoutResetMillis = (long) nodeTrConfig.getTimeoutMsReset();

			final RegistrationEvents.Position position = addSensorNode.getPosition();
			final Coordinate coordinate = new Coordinate();
			coordinate.setX(position.getXcoor());
			coordinate.setY(position.getYcoor());
			coordinate.setZ((double) position.getZcoor());

			Set<Capability> capabilities = newHashSet();
			final List<RegistrationEvents.Capability> sensorCapabilityList = addSensorNode.getSensorCapabilityList();
			for (RegistrationEvents.Capability sensorCapability : sensorCapabilityList) {
				Capability capability = new Capability();
				try {
					capability.setDatatype(convertToDtypes(sensorCapability));
				} catch (RuntimeException e) {
					log.error("The data type of a  device configuration's capability could not be set.");
				}
				capability.setName(sensorCapability.getName());
				try {
					capability.setUnit(Units.fromValue(sensorCapability.getUnit()));
				} catch (Exception e) {
					log.error("The unit of a  device configuration's capability could not be set: " + e.getCause());
				}
				//			capability.setDefault(sensorCapability.get);
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

	/**
	 * Converts and returns the data Type of a {@link RegistrationEvents.Capability} data type to {@link
	 * eu.wisebed.wiseml.Dtypes}.
	 *
	 * @param sensorCapability
	 * 		a {@link RegistrationEvents.Capability} data type
	 *
	 * @return a {@link eu.wisebed.wiseml.Dtypes} which corresponds to the provided {@link RegistrationEvents.Capability}
	 *
	 * @throws RuntimeException
	 * 		thrown if the provided data type cannot be converted
	 */
	private static Dtypes convertToDtypes(final RegistrationEvents.Capability sensorCapability)
			throws RuntimeException {
		final String datatype = sensorCapability.getDatatype();
		if (datatype.equals("float") || datatype.equals("double")) {
			return Dtypes.DECIMAL;
		} else if (datatype.equals("integer") || datatype.equals("int") || datatype.equals("long")) {
			return Dtypes.INTEGER;
		} else {
			throw new RuntimeException("The type " + datatype + " could not be handled.");
		}
	}
}
