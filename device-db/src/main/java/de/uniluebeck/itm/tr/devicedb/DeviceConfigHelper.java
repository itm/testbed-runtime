package de.uniluebeck.itm.tr.devicedb;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfig;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfigList;
import de.uniluebeck.itm.tr.common.dto.*;
import de.uniluebeck.itm.tr.devicedb.entity.*;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.wiseml.*;

import javax.annotation.Nullable;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Maps.newHashMap;
import static de.uniluebeck.itm.tr.devicedb.entity.CapabilityEntity.fromCapabilitySet;
import static de.uniluebeck.itm.tr.devicedb.entity.ChannelHandlerConfigEntity.fromChannelhandlerConfig;

public abstract class DeviceConfigHelper {

	@Transient
	private static final Function<ChannelHandlerConfigEntity, ChannelHandlerConfig> ENTITY_TO_CHC_FUNCTION =
			new Function<ChannelHandlerConfigEntity, ChannelHandlerConfig>() {
				@Override
				public ChannelHandlerConfig apply(ChannelHandlerConfigEntity config) {
					return config.toChannelHandlerConfig();
				}
			};

	public static DeviceConfigEntity toEntity(final DeviceConfig config) {
		final DeviceConfigEntity entity = new DeviceConfigEntity();
		entity.setNodeUrn(config.getNodeUrn().toString());
		entity.setNodeType(config.getNodeType());
		entity.setGatewayNode(config.isGatewayNode());
		entity.setNodePort(config.getNodePort());
		entity.setDescription(config.getDescription());
		entity.setNodeUSBChipID(config.getNodeUSBChipID());
		entity.setPosition(toEntity(config.getPosition()));
		entity.setNodeConfiguration(config.getNodeConfiguration());
		entity.setDefaultChannelPipeline(fromChannelhandlerConfig(config.getDefaultChannelPipeline()));
		entity.setCapabilities(fromCapabilitySet(config.getCapabilities()));
		entity.setTimeoutCheckAliveMillis(config.getTimeoutCheckAliveMillis());
		entity.setTimeoutResetMillis(config.getTimeoutResetMillis());
		entity.setTimeoutFlashMillis(config.getTimeoutFlashMillis());
		entity.setTimeoutNodeApiMillis(config.getTimeoutNodeApiMillis());
		return entity;
	}

	@Nullable
	public static CoordinateEntity toEntity(@Nullable final Coordinate coordinate) {
		if (coordinate == null) {
			return null;
		}
		final CoordinateEntity entity = new CoordinateEntity();
		entity.setIndoorCoordinates(toEntity(coordinate.getIndoorCoordinates()));
		entity.setOutdoorCoordinates(toEntity(coordinate.getOutdoorCoordinates()));
		return entity;
	}

	@Nullable
	public static OutdoorCoordinatesEntity toEntity(@Nullable final OutdoorCoordinatesType outdoorCoordinates) {
		if (outdoorCoordinates == null) {
			return null;
		}
		final OutdoorCoordinatesEntity entity = new OutdoorCoordinatesEntity();
		entity.setLatitude(outdoorCoordinates.getLatitude());
		entity.setLongitude(outdoorCoordinates.getLongitude());
		entity.setPhi(outdoorCoordinates.getPhi());
		entity.setRho(outdoorCoordinates.getRho());
		entity.setTheta(outdoorCoordinates.getTheta());
		entity.setX(outdoorCoordinates.getX());
		entity.setY(outdoorCoordinates.getY());
		entity.setZ(outdoorCoordinates.getZ());
		return entity;
	}

	@Nullable
	private static IndoorCoordinatesEntity toEntity(@Nullable final IndoorCoordinatesType indoorCoordinates) {
		if (indoorCoordinates == null) {
			return null;
		}
		final IndoorCoordinatesEntity entity = new IndoorCoordinatesEntity();
		entity.setBackgroundImage(indoorCoordinates.getBackgroundimage());
		entity.setBuilding(indoorCoordinates.getBuilding());
		entity.setFloor(indoorCoordinates.getFloor());
		entity.setRoom(indoorCoordinates.getRoom());
		entity.setX(indoorCoordinates.getX());
		entity.setY(indoorCoordinates.getY());
		entity.setZ(indoorCoordinates.getZ());
		return entity;
	}

	public static Set<Capability> fromEntity(final Set<CapabilityEntity> capabilities) {
		if (capabilities == null || capabilities.size() == 0) {
			return null;
		}
		Set<Capability> caps = new HashSet<Capability>();
		for (CapabilityEntity cap : capabilities) {
			caps.add(cap.toCapability());
		}
		return caps;
	}

	public static DeviceConfig fromEntity(final DeviceConfigEntity entity) {
		return new DeviceConfig(
				new NodeUrn(entity.getNodeUrn()),
				entity.getNodeType(),
				entity.isGatewayNode(),
				entity.getNodePort(),
				entity.getDescription(),
				entity.getNodeUSBChipID(),
				entity.getNodeConfiguration(),
				entity.getDefaultChannelPipeline() == null ? null : fromEntity(entity.getDefaultChannelPipeline()),
				entity.getTimeoutCheckAliveMillis(),
				entity.getTimeoutFlashMillis(),
				entity.getTimeoutNodeApiMillis(),
				entity.getTimeoutResetMillis(),
				entity.getPosition() == null ? null : fromEntity(entity.getPosition()),
				fromEntity(entity.getCapabilities())
		);
	}

	@Nullable
	public static Coordinate fromEntity(@Nullable final CoordinateEntity entity) {
		if (entity == null) {
			return null;
		}
		Coordinate coordinate = null;
		if (entity.getIndoorCoordinates() != null && entity.getOutdoorCoordinates() != null) {
			throw new IllegalArgumentException("Coordinates can either be indoor or outdoor, not both");
		} else if (entity.getIndoorCoordinates() != null) {
			coordinate = new Coordinate();
			coordinate.setType(CoordinateType.INDOOR);
			coordinate.setIndoorCoordinates(fromEntity(entity.getIndoorCoordinates()));
		} else if (entity.getOutdoorCoordinates() != null) {
			coordinate = new Coordinate();
			coordinate.setType(CoordinateType.OUTDOOR);
			coordinate.setOutdoorCoordinates(fromEntity(entity.getOutdoorCoordinates()));
		}
		return coordinate;
	}

	@Nullable
	public static OutdoorCoordinatesType fromEntity(@Nullable final OutdoorCoordinatesEntity entity) {
		if (entity == null) {
			return null;
		}
		final OutdoorCoordinatesType coordinates = new OutdoorCoordinatesType();
		coordinates.setLatitude(entity.getLatitude());
		coordinates.setLongitude(entity.getLongitude());
		coordinates.setPhi(entity.getPhi());
		coordinates.setRho(entity.getRho());
		coordinates.setTheta(entity.getTheta());
		coordinates.setX(entity.getX());
		coordinates.setY(entity.getY());
		coordinates.setZ(entity.getZ());
		return coordinates;
	}

	@Nullable
	public static IndoorCoordinatesType fromEntity(@Nullable final IndoorCoordinatesEntity entity) {
		if (entity == null) {
			return null;
		}
		final IndoorCoordinatesType coordinates = new IndoorCoordinatesType();
		coordinates.setBackgroundimage(entity.getBackgroundImage());
		coordinates.setBuilding(entity.getBuilding());
		coordinates.setFloor(entity.getFloor());
		coordinates.setRoom(entity.getRoom());
		coordinates.setX(entity.getX());
		coordinates.setY(entity.getY());
		coordinates.setZ(entity.getZ());
		return coordinates;
	}

	@Nullable
	public static ChannelHandlerConfigList fromEntity(@Nullable final List<ChannelHandlerConfigEntity> pipeline) {
		if (pipeline == null || pipeline.size() == 0) {
			return null;
		}
		return new ChannelHandlerConfigList(transform(pipeline, ENTITY_TO_CHC_FUNCTION));
	}

	public static DeviceConfig fromDto(final DeviceConfigDto dto) {

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
				channelHandlerConfigs.add(fromDto(channelHandlerConfigDto));
			}
		}

		Set<Capability> capabilitiesSet = null;
		if (dto.getCapabilities() != null) {
			capabilitiesSet = new HashSet<Capability>();
			for (CapabilityDto cap : dto.getCapabilities()) {
				capabilitiesSet.add(fromDto(cap));
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
				dto.getPosition() == null ? null : fromDto(dto.getPosition()),
				capabilitiesSet
		);
	}

	public static DeviceConfigDto toDto(DeviceConfig deviceConfig) {

		final DeviceConfigDto dto = new DeviceConfigDto();

		final ChannelHandlerConfigList defaultChannelPipeline = deviceConfig.getDefaultChannelPipeline();
		if (defaultChannelPipeline != null) {
			List<ChannelHandlerConfigDto> dtoList = newArrayList();
			for (ChannelHandlerConfig config : defaultChannelPipeline) {
				dtoList.add(toDto(config));
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
				caps.add(toDto(cap));
			}
			dto.setCapabilities(caps);
		}

		dto.setNodeType(deviceConfig.getNodeType());
		dto.setNodeUrn(deviceConfig.getNodeUrn().toString());
		dto.setNodeUSBChipID(deviceConfig.getNodeUSBChipID());
		dto.setPosition(deviceConfig.getPosition() == null ?
				null :
				toDto(deviceConfig.getPosition())
		);
		dto.setTimeoutCheckAliveMillis(deviceConfig.getTimeoutCheckAliveMillis());
		dto.setTimeoutFlashMillis(deviceConfig.getTimeoutFlashMillis());
		dto.setTimeoutNodeApiMillis(deviceConfig.getTimeoutNodeApiMillis());
		dto.setTimeoutResetMillis(deviceConfig.getTimeoutResetMillis());

		return dto;
	}

	public static Coordinate fromDto(final CoordinateDto dto) {
		Coordinate coordinate = null;
		if (dto.getIndoorCoordinates() != null && dto.getOutdoorCoordinates() != null) {
			throw new IllegalArgumentException("CoordinateDto must either have indoor or outdoor coordinates!");
		} else if (dto.getIndoorCoordinates() != null) {
			coordinate = new Coordinate();
			coordinate.setType(CoordinateType.INDOOR);
			coordinate.setIndoorCoordinates(fromDto(dto.getIndoorCoordinates()));
		} else if (dto.getOutdoorCoordinates() != null) {
			coordinate = new Coordinate();
			coordinate.setType(CoordinateType.OUTDOOR);
			coordinate.setOutdoorCoordinates(fromDto(dto.getOutdoorCoordinates()));
		}
		return coordinate;
	}

	@Nullable
	private static OutdoorCoordinatesType fromDto(@Nullable final OutdoorCoordinatesDto dto) {
		if (dto == null) {
			return null;
		}
		final OutdoorCoordinatesType coordinates = new OutdoorCoordinatesType();
		coordinates.setLatitude(dto.getLatitude());
		coordinates.setLongitude(dto.getLongitude());
		coordinates.setPhi(dto.getPhi());
		coordinates.setRho(dto.getRho());
		coordinates.setTheta(dto.getTheta());
		coordinates.setX(dto.getX());
		coordinates.setY(dto.getY());
		coordinates.setZ(dto.getZ());
		return coordinates;
	}

	@Nullable
	private static IndoorCoordinatesType fromDto(@Nullable final IndoorCoordinatesDto dto) {
		if (dto == null) {
			return null;
		}
		final IndoorCoordinatesType coordinates = new IndoorCoordinatesType();
		coordinates.setBackgroundimage(dto.getBackgroundImage());
		coordinates.setBuilding(dto.getBuilding());
		coordinates.setFloor(dto.getFloor());
		coordinates.setRoom(dto.getRoom());
		coordinates.setX(dto.getX());
		coordinates.setY(dto.getY());
		coordinates.setZ(dto.getZ());
		return coordinates;
	}

	@Nullable
	public static CoordinateDto toDto(@Nullable final Coordinate coordinate) {
		if (coordinate == null) {
			return null;
		}
		final CoordinateDto dto = new CoordinateDto();
		switch (coordinate.getType()) {
			case INDOOR:
				dto.setIndoorCoordinates(toDto(coordinate.getIndoorCoordinates()));
				break;
			case OUTDOOR:
				dto.setOutdoorCoordinates(toDto(coordinate.getOutdoorCoordinates()));
				break;
		}
		return dto;
	}

	@Nullable
	private static OutdoorCoordinatesDto toDto(@Nullable final OutdoorCoordinatesType coordinates) {
		if (coordinates == null) {
			return null;
		}
		final OutdoorCoordinatesDto dto = new OutdoorCoordinatesDto();
		dto.setLatitude(coordinates.getLatitude());
		dto.setLongitude(coordinates.getLongitude());
		dto.setPhi(coordinates.getPhi());
		dto.setRho(coordinates.getRho());
		dto.setTheta(coordinates.getTheta());
		dto.setX(coordinates.getX());
		dto.setY(coordinates.getY());
		dto.setZ(coordinates.getZ());
		return dto;
	}

	@Nullable
	private static IndoorCoordinatesDto toDto(@Nullable final IndoorCoordinatesType coordinates) {
		if (coordinates == null) {
			return null;
		}
		final IndoorCoordinatesDto dto = new IndoorCoordinatesDto();
		dto.setBackgroundImage(coordinates.getBackgroundimage());
		dto.setBuilding(coordinates.getBuilding());
		dto.setFloor(coordinates.getFloor());
		dto.setRoom(coordinates.getRoom());
		dto.setX(coordinates.getX());
		dto.setY(coordinates.getY());
		dto.setZ(coordinates.getZ());
		return dto;
	}

	public static ChannelHandlerConfig fromDto(final ChannelHandlerConfigDto dto) {
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

	public static ChannelHandlerConfigDto toDto(final ChannelHandlerConfig config) {

		final List<KeyValueDto> configList = newArrayList();
		final Multimap<String, String> properties = config.getProperties();

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

	public static Capability fromDto(final CapabilityDto dto) {
		Capability cap = new Capability();
		cap.setDatatype(dto.getDatatype());
		cap.setDefault(dto.getDefaultValue());
		cap.setName(dto.getName());
		cap.setUnit(dto.getUnit());
		return cap;
	}

	public static CapabilityDto toDto(Capability capability) {
		return new CapabilityDto(
				capability.getName(),
				capability.getDefault(),
				capability.getDatatype(),
				capability.getUnit()
		);
	}
}
