package de.uniluebeck.itm.tr.devicedb.entity;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Cacheable
@Entity(name="DeviceConfig")
public class DeviceConfigEntity {
	
	@Id
	private String nodeUrn;

	@Column(nullable = false)
	private String nodeType;

	private boolean gatewayNode;

	@Nullable
	private String nodePort;

	@Nullable
	@Column(length = 1024)
	private String description;

	@Nullable
	private String nodeUSBChipID;

	@Nullable
	@OneToOne(cascade=CascadeType.ALL)
	private CoordinateEntity position;

	@ElementCollection(fetch = FetchType.EAGER)
	private Map<String, String> nodeConfiguration;

	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
	@Nullable
	private List<ChannelHandlerConfigEntity> defaultChannelPipeline;

	@Nullable
	private Long timeoutNodeApiMillis;

	@Nullable
	private Long timeoutResetMillis;

	@Nullable
	private Long timeoutFlashMillis;

	@Nullable
	private Long timeoutCheckAliveMillis;
	
	@Nullable
	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
	private Set<CapabilityEntity> capabilities;

	@Nullable
	public List<ChannelHandlerConfigEntity> getDefaultChannelPipeline() {
		return defaultChannelPipeline;
	}

	public void setDefaultChannelPipeline(
			@Nullable final List<ChannelHandlerConfigEntity> defaultChannelPipeline) {
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

	public Map<String, String> getNodeConfiguration() {
		return nodeConfiguration;
	}

	public void setNodeConfiguration(final Map<String, String> nodeConfiguration) {
		this.nodeConfiguration = nodeConfiguration;
	}

	@Nullable
	public String getNodePort() {
		return nodePort;
	}

	public void setNodePort(@Nullable final String nodePort) {
		this.nodePort = nodePort;
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
	public CoordinateEntity getPosition() {
		return position;
	}

	public void setPosition(@Nullable final CoordinateEntity position) {
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
	public Set<CapabilityEntity> getCapabilities() {
		return capabilities;
	}

	public void setCapabilities(@Nullable Set<CapabilityEntity> capabilities) {
		this.capabilities = capabilities;
	}
}
