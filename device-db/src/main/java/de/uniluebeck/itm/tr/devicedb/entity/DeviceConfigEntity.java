package de.uniluebeck.itm.tr.devicedb.entity;

import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfig;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfigList;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import eu.wisebed.api.v3.common.NodeUrn;

import javax.annotation.Nullable;
import javax.persistence.*;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Entity(name="DeviceConfig")
public class DeviceConfigEntity {
	
	@Transient
	private static final Function<ChannelHandlerConfigEntity, ChannelHandlerConfig> ENTITY_TO_CHC_FUNCTION =
			new Function<ChannelHandlerConfigEntity, ChannelHandlerConfig>() {
				@Override
				public ChannelHandlerConfig apply(ChannelHandlerConfigEntity config) {
					return config.toChannelHandlerConfig();
				}
			};
	
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
	
	public DeviceConfigEntity() {	}
	
	public DeviceConfigEntity(DeviceConfig config) {
		this.nodeUrn = config.getNodeUrn().toString();
		this.nodeType = config.getNodeType();
		this.gatewayNode = config.isGatewayNode();
		this.nodePort = config.getNodePort();
		this.description = config.getDescription();
		this.nodeUSBChipID = config.getNodeUSBChipID();
		this.position = CoordinateEntity.fromCoordinate(config.getPosition());
		this.nodeConfiguration = config.getNodeConfiguration();
		this.defaultChannelPipeline = ChannelHandlerConfigEntity.fromChannelhandlerConfig(config.getDefaultChannelPipeline());
		this.timeoutNodeApiMillis = config.getTimeoutNodeApiMillis();
		this.timeoutResetMillis = config.getTimeoutResetMillis();
		this.timeoutFlashMillis = config.getTimeoutFlashMillis();
		this.timeoutCheckAliveMillis = config.getTimeoutCheckAliveMillis();
	}

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

	public DeviceConfig toDeviceConfig() {
		return new DeviceConfig(
				new NodeUrn(nodeUrn),
				nodeType,
				gatewayNode,
				nodePort,
				description,
				nodeUSBChipID,
				nodeConfiguration,
				defaultChannelPipeline == null ? null : convertDefaultPipeline(),
				timeoutCheckAliveMillis,
				timeoutFlashMillis,
				timeoutNodeApiMillis,
				timeoutResetMillis,
				position == null ? null : position.toCoordinate());
	}

	private ChannelHandlerConfigList convertDefaultPipeline() {
		Collection<ChannelHandlerConfig> configList = Lists.transform(defaultChannelPipeline, ENTITY_TO_CHC_FUNCTION);
		return new ChannelHandlerConfigList(configList);
	}
	
}
