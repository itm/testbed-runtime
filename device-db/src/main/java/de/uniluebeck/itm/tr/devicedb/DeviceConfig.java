package de.uniluebeck.itm.tr.devicedb;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Map;

import javax.annotation.Nullable;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfigList;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.wiseml.Coordinate;

public class DeviceConfig implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final int DEFAULT_TIMEOUT_FLASH_MILLIS = 120000;

	private static final int DEFAULT_TIMEOUT_NODE_API_MILLIS = 1000;

	private static final int DEFAULT_TIMEOUT_RESET_MILLIS = 3000;

	private static final int DEFAULT_TIMEOUT_CHECK_ALIVE_MILLIS = 3000;


	private String nodeUrn;

	private String nodeType;

	private boolean gatewayNode;

	@Nullable
	private String description;

	@Nullable
	private String nodeUSBChipID;

	@Nullable
	private Coordinate position;

	private Map<String, String> nodeConfiguration;

	@Nullable
	private ChannelHandlerConfigList defaultChannelPipeline;

	@Nullable
	private Long timeoutNodeApiMillis;

	@Nullable
	private Long timeoutResetMillis;

	@Nullable
	private Long timeoutFlashMillis;

	@Nullable
	private Long timeoutCheckAliveMillis;

	public DeviceConfig() {

	}

	public DeviceConfig(
			final NodeUrn nodeUrn,
			final String nodeType,
			final boolean gatewayNode,
			@Nullable final String description,
			@Nullable final String nodeUSBChipID,
			@Nullable final Map<String, String> nodeConfiguration,
			@Nullable final ChannelHandlerConfigList defaultChannelPipeline,
			@Nullable final Long timeoutCheckAliveMillis,
			@Nullable final Long timeoutFlashMillis,
			@Nullable final Long timeoutNodeApiMillis,
			@Nullable final Long timeoutResetMillis,
			@Nullable final Coordinate position) {

		this.nodeUrn = checkNotNull(nodeUrn).toString();
		this.nodeType = checkNotNull(nodeType);
		this.gatewayNode = gatewayNode;
		this.description = description;
		this.nodeUSBChipID = nodeUSBChipID;
		this.nodeConfiguration = nodeConfiguration == null ? Maps.<String, String>newHashMap() : nodeConfiguration;
		this.defaultChannelPipeline = defaultChannelPipeline;
		this.position = position;

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
	public String getDescription() {
		return description;
	}

	@Nullable
	public Coordinate getPosition() {
		return position;
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

	public Map<String, String> getNodeConfiguration() {
		return nodeConfiguration;
	}

	@Nullable
	public ChannelHandlerConfigList getDefaultChannelPipeline() {
		return defaultChannelPipeline;
	}

	public boolean isGatewayNode() {
		return gatewayNode;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final DeviceConfig that = (DeviceConfig) o;

		if (gatewayNode != that.gatewayNode) {
			return false;
		}
		if (defaultChannelPipeline != null ? !defaultChannelPipeline.equals(that.defaultChannelPipeline) :
				that.defaultChannelPipeline != null) {
			return false;
		}
		if (description != null ? !description.equals(that.description) : that.description != null) {
			return false;
		}
		if (nodeConfiguration != null ? !nodeConfiguration.equals(that.nodeConfiguration) :
				that.nodeConfiguration != null) {
			return false;
		}
		if (nodeType != null ? !nodeType.equals(that.nodeType) : that.nodeType != null) {
			return false;
		}
		if (nodeUSBChipID != null ? !nodeUSBChipID.equals(that.nodeUSBChipID) : that.nodeUSBChipID != null) {
			return false;
		}
		if (nodeUrn != null ? !nodeUrn.equals(that.nodeUrn) : that.nodeUrn != null) {
			return false;
		}
		if (position != null ? !position.equals(that.position) : that.position != null) {
			return false;
		}
		if (timeoutCheckAliveMillis != null ? !timeoutCheckAliveMillis.equals(that.timeoutCheckAliveMillis) :
				that.timeoutCheckAliveMillis != null) {
			return false;
		}
		if (timeoutFlashMillis != null ? !timeoutFlashMillis.equals(that.timeoutFlashMillis) :
				that.timeoutFlashMillis != null) {
			return false;
		}
		if (timeoutNodeApiMillis != null ? !timeoutNodeApiMillis.equals(that.timeoutNodeApiMillis) :
				that.timeoutNodeApiMillis != null) {
			return false;
		}
		if (timeoutResetMillis != null ? !timeoutResetMillis.equals(that.timeoutResetMillis) :
				that.timeoutResetMillis != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = nodeUrn != null ? nodeUrn.hashCode() : 0;
		result = 31 * result + (nodeType != null ? nodeType.hashCode() : 0);
		result = 31 * result + (gatewayNode ? 1 : 0);
		result = 31 * result + (description != null ? description.hashCode() : 0);
		result = 31 * result + (nodeUSBChipID != null ? nodeUSBChipID.hashCode() : 0);
		result = 31 * result + (position != null ? position.hashCode() : 0);
		result = 31 * result + (nodeConfiguration != null ? nodeConfiguration.hashCode() : 0);
		result = 31 * result + (defaultChannelPipeline != null ? defaultChannelPipeline.hashCode() : 0);
		result = 31 * result + (timeoutNodeApiMillis != null ? timeoutNodeApiMillis.hashCode() : 0);
		result = 31 * result + (timeoutResetMillis != null ? timeoutResetMillis.hashCode() : 0);
		result = 31 * result + (timeoutFlashMillis != null ? timeoutFlashMillis.hashCode() : 0);
		result = 31 * result + (timeoutCheckAliveMillis != null ? timeoutCheckAliveMillis.hashCode() : 0);
		return result;
	}
	
	@Override
	public String toString() {
		Joiner joiner = Joiner.on(", ").useForNull("null");
		return "{"+ joiner.join(
					nodeUrn,
					nodeType,
					gatewayNode,
					description,
					nodeUSBChipID,
					position,
					nodeConfiguration,
					timeoutCheckAliveMillis,
					timeoutFlashMillis,
					timeoutNodeApiMillis,
					timeoutResetMillis,
					defaultChannelPipeline
				) +"}";
	}

}
