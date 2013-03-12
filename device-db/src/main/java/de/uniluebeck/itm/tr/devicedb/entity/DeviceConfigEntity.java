package de.uniluebeck.itm.tr.devicedb.entity;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfig;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfigList;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import eu.wisebed.api.v3.common.NodeUrn;

@Entity(name="DeviceConfig")
public class DeviceConfigEntity {
	
	@Id
	private String nodeUrn;

	@Column(nullable = false)
	private String nodeType;

	private boolean gatewayNode;

	@Nullable
	private String description;

	@Nullable
	private String nodeUSBChipID;

	@Nullable
	@OneToOne(cascade=CascadeType.ALL)
	private CoordinateEntity position;

	@ElementCollection(fetch = FetchType.EAGER)
	private Map<String, String> nodeConfiguration;

	@Nullable
	@Transient
	// TODO persist this
	private ChannelHandlerConfigList defaultChannelPipeline;

	@Nullable
	private Long timeoutNodeApiMillis;

	@Nullable
	private Long timeoutResetMillis;

	@Nullable
	private Long timeoutFlashMillis;

	@Nullable
	private Long timeoutCheckAliveMillis;
	
	public DeviceConfigEntity() {	}
	
	public DeviceConfigEntity(DeviceConfig config) {
		this.nodeUrn = config.getNodeUrn().toString();
		this.nodeType = config.getNodeType();
		this.gatewayNode = config.isGatewayNode();
		this.description = config.getDescription();
		this.nodeUSBChipID = config.getNodeUSBChipID();
		this.position = CoordinateEntity.fromCoordinate(config.getPosition());
		this.nodeConfiguration = config.getNodeConfiguration();
		this.defaultChannelPipeline = config.getDefaultChannelPipeline();
		this.timeoutNodeApiMillis = config.getTimeoutNodeApiMillis();
		this.timeoutResetMillis = config.getTimeoutResetMillis();
		this.timeoutFlashMillis = config.getTimeoutFlashMillis();
		this.timeoutCheckAliveMillis = config.getTimeoutCheckAliveMillis();
	}

	public String getNodeUrn() {
		return nodeUrn;
	}

	public void setNodeUrn(String nodeUrn) {
		this.nodeUrn = nodeUrn;
	}

	public String getNodeType() {
		return nodeType;
	}

	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}

	public boolean isGatewayNode() {
		return gatewayNode;
	}

	public void setGatewayNode(boolean gatewayNode) {
		this.gatewayNode = gatewayNode;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getNodeUSBChipID() {
		return nodeUSBChipID;
	}

	public void setNodeUSBChipID(String nodeUSBChipID) {
		this.nodeUSBChipID = nodeUSBChipID;
	}

	public CoordinateEntity getPosition() {
		return position;
	}

	public void setPosition(CoordinateEntity position) {
		this.position = position;
	}

	public Map<String, String> getNodeConfiguration() {
		return nodeConfiguration;
	}

	public void setNodeConfiguration(Map<String, String> nodeConfiguration) {
		this.nodeConfiguration = nodeConfiguration;
	}

	public List<ChannelHandlerConfig> getDefaultChannelPipeline() {
		return defaultChannelPipeline;
	}

	public void setDefaultChannelPipeline(
			ChannelHandlerConfigList defaultChannelPipeline) {
		this.defaultChannelPipeline = defaultChannelPipeline;
	}

	public Long getTimeoutNodeApiMillis() {
		return timeoutNodeApiMillis;
	}

	public void setTimeoutNodeApiMillis(Long timeoutNodeApiMillis) {
		this.timeoutNodeApiMillis = timeoutNodeApiMillis;
	}

	public Long getTimeoutResetMillis() {
		return timeoutResetMillis;
	}

	public void setTimeoutResetMillis(Long timeoutResetMillis) {
		this.timeoutResetMillis = timeoutResetMillis;
	}

	public Long getTimeoutFlashMillis() {
		return timeoutFlashMillis;
	}

	public void setTimeoutFlashMillis(Long timeoutFlashMillis) {
		this.timeoutFlashMillis = timeoutFlashMillis;
	}

	public Long getTimeoutCheckAliveMillis() {
		return timeoutCheckAliveMillis;
	}

	public void setTimeoutCheckAliveMillis(Long timeoutCheckAliveMillis) {
		this.timeoutCheckAliveMillis = timeoutCheckAliveMillis;
	}

	public DeviceConfig toDeviceConfig() {
		return new DeviceConfig(
				new NodeUrn(nodeUrn),
				nodeType,
				gatewayNode,
				description,
				nodeUSBChipID,
				nodeConfiguration,
				defaultChannelPipeline,
				timeoutCheckAliveMillis,
				timeoutFlashMillis,
				timeoutNodeApiMillis,
				timeoutResetMillis,
				position == null ? null : position.toCoordinate());
	}
	
}
