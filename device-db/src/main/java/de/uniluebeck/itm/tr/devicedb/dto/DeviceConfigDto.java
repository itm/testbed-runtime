package de.uniluebeck.itm.tr.devicedb.dto;

import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfig;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfigList;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import eu.wisebed.api.v3.common.NodeUrn;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

@XmlRootElement
public class DeviceConfigDto {

	private String nodeUrn;

	private String nodeType;

	private boolean gatewayNode;

	@Nullable
	private String description;

	@Nullable
	private String nodeUSBChipID;

	@Nullable
	private CoordinateDto position;

	@Nullable
	private List<KeyValueDto> nodeConfiguration;

	@Nullable
	private List<ChannelHandlerConfigDto> defaultChannelPipeline;

	@Nullable
	private Long timeoutNodeApiMillis;

	@Nullable
	private Long timeoutResetMillis;

	@Nullable
	private Long timeoutFlashMillis;

	@Nullable
	private Long timeoutCheckAliveMillis;

	@Nullable
	public List<ChannelHandlerConfigDto> getDefaultChannelPipeline() {
		return defaultChannelPipeline;
	}

	public void setDefaultChannelPipeline(
			@Nullable final List<ChannelHandlerConfigDto> defaultChannelPipeline) {
		this.defaultChannelPipeline = defaultChannelPipeline;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable final String description) {
		this.description = description;
	}

	public boolean isGatewayNode() {
		return gatewayNode;
	}

	public void setGatewayNode(final boolean gatewayNode) {
		this.gatewayNode = gatewayNode;
	}

	@Nullable
	public List<KeyValueDto> getNodeConfiguration() {
		return nodeConfiguration;
	}

	public void setNodeConfiguration(@Nullable final List<KeyValueDto> nodeConfiguration) {
		this.nodeConfiguration = nodeConfiguration;
	}

	public String getNodeType() {
		return nodeType;
	}

	public void setNodeType(final String nodeType) {
		this.nodeType = nodeType;
	}

	public String getNodeUrn() {
		return nodeUrn;
	}

	public void setNodeUrn(final String nodeUrn) {
		this.nodeUrn = nodeUrn;
	}

	@Nullable
	public String getNodeUSBChipID() {
		return nodeUSBChipID;
	}

	public void setNodeUSBChipID(@Nullable final String nodeUSBChipID) {
		this.nodeUSBChipID = nodeUSBChipID;
	}

	@Nullable
	public CoordinateDto getPosition() {
		return position;
	}

	public void setPosition(@Nullable final CoordinateDto position) {
		this.position = position;
	}

	@Nullable
	public Long getTimeoutCheckAliveMillis() {
		return timeoutCheckAliveMillis;
	}

	public void setTimeoutCheckAliveMillis(@Nullable final Long timeoutCheckAliveMillis) {
		this.timeoutCheckAliveMillis = timeoutCheckAliveMillis;
	}

	@Nullable
	public Long getTimeoutFlashMillis() {
		return timeoutFlashMillis;
	}

	public void setTimeoutFlashMillis(@Nullable final Long timeoutFlashMillis) {
		this.timeoutFlashMillis = timeoutFlashMillis;
	}

	@Nullable
	public Long getTimeoutNodeApiMillis() {
		return timeoutNodeApiMillis;
	}

	public void setTimeoutNodeApiMillis(@Nullable final Long timeoutNodeApiMillis) {
		this.timeoutNodeApiMillis = timeoutNodeApiMillis;
	}

	@Nullable
	public Long getTimeoutResetMillis() {
		return timeoutResetMillis;
	}

	public void setTimeoutResetMillis(@Nullable final Long timeoutResetMillis) {
		this.timeoutResetMillis = timeoutResetMillis;
	}

	public static DeviceConfigDto fromDeviceConfig(DeviceConfig deviceConfig) {

		final DeviceConfigDto dto = new DeviceConfigDto();

		final ChannelHandlerConfigList defaultChannelPipeline = deviceConfig.getDefaultChannelPipeline();
		if (defaultChannelPipeline != null) {
			List<ChannelHandlerConfigDto> dtoList = newArrayList();
			for (ChannelHandlerConfig config : defaultChannelPipeline) {
				dtoList.add(ChannelHandlerConfigDto.fromChannelHandlerConfig(config));
			}
			dto.defaultChannelPipeline = dtoList;
		}

		dto.description = deviceConfig.getDescription();
		dto.gatewayNode = deviceConfig.isGatewayNode();

		final Map<String, String> nodeConfiguration = deviceConfig.getNodeConfiguration();
		if (nodeConfiguration != null) {
			final List<KeyValueDto> nodeConfigs = newArrayList();
			for (Map.Entry<String, String> entry : nodeConfiguration.entrySet()) {
				nodeConfigs.add(new KeyValueDto(entry.getKey(), entry.getValue()));
			}
			dto.nodeConfiguration = nodeConfigs;
		}

		dto.nodeType = deviceConfig.getNodeType();
		dto.nodeUrn = deviceConfig.getNodeUrn().toString();
		dto.nodeUSBChipID = deviceConfig.getNodeUSBChipID();
		dto.position = deviceConfig.getPosition() == null ?
				null :
				CoordinateDto.fromCoordinate(deviceConfig.getPosition());
		dto.timeoutCheckAliveMillis = deviceConfig.getTimeoutCheckAliveMillis();
		dto.timeoutFlashMillis = deviceConfig.getTimeoutFlashMillis();
		dto.timeoutNodeApiMillis = deviceConfig.getTimeoutNodeApiMillis();
		dto.timeoutResetMillis = deviceConfig.getTimeoutResetMillis();

		return dto;
	}

	public DeviceConfig toDeviceConfig() {

		Map<String, String> nodeConfigurationMap = null;
		if (nodeConfiguration != null) {
			nodeConfigurationMap = newHashMap();
			for (KeyValueDto keyValueDto : nodeConfiguration) {
				nodeConfigurationMap.put(keyValueDto.getKey(), keyValueDto.getValue());
			}
		}

		ChannelHandlerConfigList channelHandlerConfigs = null;
		if (defaultChannelPipeline != null) {
			channelHandlerConfigs = new ChannelHandlerConfigList();
			for (ChannelHandlerConfigDto channelHandlerConfigDto : defaultChannelPipeline) {
				channelHandlerConfigs.add(channelHandlerConfigDto.toChannelHandlerConfig());
			}
		}

		return new DeviceConfig(
				new NodeUrn(nodeUrn),
				nodeType,
				gatewayNode,
				description,
				nodeUSBChipID,
				nodeConfigurationMap,
				channelHandlerConfigs,
				timeoutCheckAliveMillis,
				timeoutFlashMillis,
				timeoutNodeApiMillis,
				timeoutResetMillis,
				position == null ? null : position.toCoordinate()
		);
	}
}
