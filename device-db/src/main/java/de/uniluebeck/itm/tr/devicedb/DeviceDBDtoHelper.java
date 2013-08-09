package de.uniluebeck.itm.tr.devicedb;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfig;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfigList;
import de.uniluebeck.itm.tr.common.dto.*;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.wiseml.Capability;
import eu.wisebed.wiseml.Coordinate;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

public abstract class DeviceDBDtoHelper {

	public static DeviceConfig toDeviceConfig(final DeviceConfigDto dto) {

		Map<String, String> nodeConfigurationMap = null;
		if (dto.getNodeConfiguration() != null) {
			nodeConfigurationMap = newHashMap();
			for (KeyValueDto keyValueDto : dto.getNodeConfiguration()) {
				nodeConfigurationMap.put(keyValueDto.getKey(), keyValueDto.getValue());
			}
		}

		ChannelHandlerConfigList channelHandlerConfigs = null;
		if (dto.getDefaultChannelPipeline() != null) {
			channelHandlerConfigs = new ChannelHandlerConfigList();
			for (ChannelHandlerConfigDto channelHandlerConfigDto : dto.getDefaultChannelPipeline()) {
				channelHandlerConfigs.add(toChannelHandlerConfig(channelHandlerConfigDto));
			}
		}

		Set<Capability> capabilitiesSet = null;
		if (dto.getCapabilities() != null) {
			capabilitiesSet = new HashSet<Capability>();
			for (CapabilityDto cap : dto.getCapabilities()) {
				capabilitiesSet.add(toCapability(cap));
			}
		}

		return new DeviceConfig(
				new NodeUrn(dto.getNodeUrn()),
				dto.getNodeType(),
				dto.isGatewayNode(),
				dto.getNodePort(),
				dto.getDescription(),
				dto.getNodeUSBChipID(),
				nodeConfigurationMap,
				channelHandlerConfigs,
				dto.getTimeoutCheckAliveMillis(),
				dto.getTimeoutFlashMillis(),
				dto.getTimeoutNodeApiMillis(),
				dto.getTimeoutResetMillis(),
				dto.getPosition() == null ? null : toCoordinate(dto.getPosition()),
				capabilitiesSet
		);
	}

	public static DeviceConfigDto fromDeviceConfig(DeviceConfig deviceConfig) {

		final DeviceConfigDto dto = new DeviceConfigDto();

		final ChannelHandlerConfigList defaultChannelPipeline = deviceConfig.getDefaultChannelPipeline();
		if (defaultChannelPipeline != null) {
			List<ChannelHandlerConfigDto> dtoList = newArrayList();
			for (ChannelHandlerConfig config : defaultChannelPipeline) {
				dtoList.add(fromChannelHandlerConfig(config));
			}
			dto.setDefaultChannelPipeline(dtoList);
		}

		dto.setDescription(deviceConfig.getDescription());
		dto.setGatewayNode(deviceConfig.isGatewayNode());
		dto.setNodePort(deviceConfig.getNodePort());

		final Map<String, String> nodeConfiguration = deviceConfig.getNodeConfiguration();
		if (nodeConfiguration != null) {
			final List<KeyValueDto> nodeConfigs = newArrayList();
			for (Map.Entry<String, String> entry : nodeConfiguration.entrySet()) {
				nodeConfigs.add(new KeyValueDto(entry.getKey(), entry.getValue()));
			}
			dto.setNodeConfiguration(nodeConfigs);
		}

		final Set<Capability> capabilities = deviceConfig.getCapabilities();
		if (capabilities != null) {
			final Set<CapabilityDto> caps = new HashSet<CapabilityDto>();
			for (Capability cap : capabilities) {
				caps.add(fromCapability(cap));
			}
			dto.setCapabilities(caps);
		}

		dto.setNodeType(deviceConfig.getNodeType());
		dto.setNodeUrn(deviceConfig.getNodeUrn().toString());
		dto.setNodeUSBChipID(deviceConfig.getNodeUSBChipID());
		dto.setPosition(deviceConfig.getPosition() == null ?
				null :
				fromCoordinate(deviceConfig.getPosition()));
		dto.setTimeoutCheckAliveMillis(deviceConfig.getTimeoutCheckAliveMillis());
		dto.setTimeoutFlashMillis(deviceConfig.getTimeoutFlashMillis());
		dto.setTimeoutNodeApiMillis(deviceConfig.getTimeoutNodeApiMillis());
		dto.setTimeoutResetMillis(deviceConfig.getTimeoutResetMillis());

		return dto;
	}

	public static Coordinate toCoordinate(final CoordinateDto dto) {
		final Coordinate coordinate = new Coordinate();
		coordinate.setX(dto.getX());
		coordinate.setY(dto.getY());
		coordinate.setZ(dto.getZ());
		coordinate.setPhi(dto.getPhi());
		coordinate.setTheta(dto.getTheta());
		return coordinate;
	}

	public static CoordinateDto fromCoordinate(final Coordinate position) {
		final CoordinateDto dto = new CoordinateDto();
		dto.setX(position.getX());
		dto.setY(position.getY());
		dto.setZ(position.getZ());
		dto.setPhi(position.getPhi());
		dto.setTheta(position.getTheta());
		return dto;
	}

	public static ChannelHandlerConfig toChannelHandlerConfig(final ChannelHandlerConfigDto dto) {
		HashMultimap<String, String> properties = null;
		if (dto.getConfiguration() != null) {
			properties = HashMultimap.create();
			for (KeyValueDto keyValueDto : dto.getConfiguration()) {
				properties.put(keyValueDto.getKey(), keyValueDto.getValue());
			}
		}
		return new ChannelHandlerConfig(
				dto.getHandlerName(),
				dto.getInstanceName() == null ? dto.getHandlerName() : dto.getInstanceName(),
				properties
		);
	}

	public static ChannelHandlerConfigDto fromChannelHandlerConfig(final ChannelHandlerConfig config) {

		final List<KeyValueDto> configList = newArrayList();
		final Multimap<String,String> properties = config.getProperties();

		if (properties != null) {
			for (String key : properties.keySet()) {
				for (String value : properties.get(key)) {
					configList.add(new KeyValueDto(key, value));
				}
			}
		}

		return new ChannelHandlerConfigDto(
				config.getHandlerName(),
				config.getInstanceName(),
				configList
		);
	}

	public static Capability toCapability(final CapabilityDto dto) {
		Capability cap = new Capability();
		cap.setDatatype(dto.getDatatype());
		cap.setDefault(dto.getDefaultValue());
		cap.setName(dto.getName());
		cap.setUnit(dto.getUnit());
		return cap;
	}

	public static CapabilityDto fromCapability(Capability capability) {
		return new CapabilityDto(
				capability.getName(),
				capability.getDefault(),
				capability.getDatatype(),
				capability.getUnit()
		);
	}
}
