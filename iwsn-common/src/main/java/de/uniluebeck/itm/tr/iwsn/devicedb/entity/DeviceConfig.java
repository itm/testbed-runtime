package de.uniluebeck.itm.tr.iwsn.devicedb.entity;

import de.uniluebeck.itm.tr.util.Tuple;
import eu.wisebed.api.v3.common.NodeUrn;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.jboss.netty.channel.ChannelHandler;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.Transient;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Entity
public class DeviceConfig {

	private static final int DEFAULT_TIMEOUT_FLASH_MILLIS = 120000;

	private static final int DEFAULT_TIMEOUT_NODE_API_MILLIS = 1000;

	private static final int DEFAULT_TIMEOUT_RESET_MILLIS = 3000;

	private static final int DEFAULT_TIMEOUT_CHECK_ALIVE_MILLIS = 3000;

	@Id
	private String nodeUrn;
	
	@Column(nullable=false)
	private  String nodeType;

	private  boolean gatewayNode;

	@Nullable 
	private  String nodeUSBChipID;

	@Nullable
	@ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "key", length = 1024)
    @Column(name = "value", length = 4069)
	private  Map<String, String> nodeConfiguration;

	@Nullable
	// TODO how to persist?
	@Transient
	private  List<Tuple<String, ChannelHandler>> defaultChannelPipeline;

	@Nullable
	private  Long timeoutNodeApiMillis;

	@Nullable
	private  Long timeoutResetMillis;

	@Nullable
	private  Long timeoutFlashMillis;

	@Nullable
	private  Long timeoutCheckAliveMillis;
	
	public DeviceConfig() {
		
	}

	public DeviceConfig(
			final NodeUrn nodeUrn,
			final String nodeType,
			final boolean gatewayNode,
			@Nullable final String nodeUSBChipID,
			@Nullable final Map<String, String> nodeConfiguration,
			@Nullable final List<Tuple<String, ChannelHandler>> defaultChannelPipeline,
			@Nullable final Long timeoutCheckAliveMillis,
			@Nullable final Long timeoutFlashMillis,
			@Nullable final Long timeoutNodeApiMillis,
			@Nullable final Long timeoutResetMillis) {

		this.nodeUrn = checkNotNull(nodeUrn).toString();
		this.nodeType = checkNotNull(nodeType);
		this.gatewayNode = gatewayNode;
		this.nodeUSBChipID = nodeUSBChipID;
		this.nodeConfiguration = nodeConfiguration;
		this.defaultChannelPipeline = defaultChannelPipeline;

		checkArgument((timeoutCheckAliveMillis == null || timeoutCheckAliveMillis > 0),
				"The timeout value for the checkAlive operation must either be omitted (null) to use the default value "
						+ "of " + DEFAULT_TIMEOUT_CHECK_ALIVE_MILLIS + " ms or be larger than 0 (zero). Configured "
						+ "value: " + timeoutCheckAliveMillis
		);
		this.timeoutCheckAliveMillis = timeoutCheckAliveMillis;

		checkArgument((timeoutFlashMillis == null || timeoutFlashMillis > 0),
				"The timeout value for the flash operation must either be omitted (null) to use the default "
						+ "value of " + DEFAULT_TIMEOUT_FLASH_MILLIS + " ms or be larger than 0 (zero). Configured "
						+ "value: " + timeoutFlashMillis
		);
		this.timeoutFlashMillis = timeoutFlashMillis;

		checkArgument((timeoutNodeApiMillis == null || timeoutNodeApiMillis > 0),
				"The timeout value for the Node API must either be omitted (null) to use the default value of " +
						DEFAULT_TIMEOUT_NODE_API_MILLIS + " ms or be larger than 0 (zero). Configured value: " +
						timeoutNodeApiMillis
		);
		this.timeoutNodeApiMillis = timeoutNodeApiMillis;

		checkArgument((timeoutResetMillis == null || timeoutResetMillis > 0),
				"The timeout value for the reset operation must either be omitted (null) to use the default value "
						+ "of " + DEFAULT_TIMEOUT_RESET_MILLIS + " ms or be larger than 0 (zero). Configured "
						+ "value: " + timeoutResetMillis
		);
		this.timeoutResetMillis = timeoutResetMillis;
	}

	public String getNodeType() {
		return nodeType;
	}

	public NodeUrn getNodeUrn() {
		return new NodeUrn(nodeUrn);
	}

	@Nullable
	public String getNodeUSBChipID() {
		return nodeUSBChipID;
	}

	public long getTimeoutFlashMillis() {
		return timeoutFlashMillis != null ? timeoutFlashMillis : DEFAULT_TIMEOUT_FLASH_MILLIS;
	}

	public long getTimeoutNodeApiMillis() {
		return timeoutNodeApiMillis != null ? timeoutNodeApiMillis : DEFAULT_TIMEOUT_NODE_API_MILLIS;
	}

	public long getTimeoutResetMillis() {
		return timeoutResetMillis != null ? timeoutResetMillis : DEFAULT_TIMEOUT_RESET_MILLIS;
	}

	public long getTimeoutCheckAliveMillis() {
		return timeoutCheckAliveMillis != null ? timeoutCheckAliveMillis : DEFAULT_TIMEOUT_CHECK_ALIVE_MILLIS;
	}

	@Nullable
	public Map<String, String> getNodeConfiguration() {
		return nodeConfiguration;
	}

	@Nullable
	public List<Tuple<String, ChannelHandler>> getDefaultChannelPipeline() {
		return defaultChannelPipeline;
	}

	public boolean isGatewayNode() {
		return gatewayNode;
	}
}
