package de.uniluebeck.itm.tr.common.dto;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Set;

@XmlRootElement
public class DeviceConfigDto {

	private String nodeUrn;

	private String nodeType;

	private boolean gatewayNode;

	@Nullable
	private String nodePort;

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
	private Set<CapabilityDto> capabilities;

	@Nullable
	private Long timeoutNodeApiMillis;

	@Nullable
	private Long timeoutResetMillis;

	@Nullable
	private Long timeoutFlashMillis;

	@Nullable
	private Long timeoutCheckAliveMillis;

	@Nullable
	public String getNodePort() {
		return nodePort;
	}

	public void setNodePort(@Nullable final String nodePort) {
		this.nodePort = nodePort;
	}

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

	@Nullable
	public Set<CapabilityDto> getCapabilities() {
		return capabilities;
	}

	public void setCapabilities(@Nullable final Set<CapabilityDto> capabilities) {
		this.capabilities = capabilities;
	}
}
