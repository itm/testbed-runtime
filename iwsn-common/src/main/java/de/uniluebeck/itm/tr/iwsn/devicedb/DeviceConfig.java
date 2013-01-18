package de.uniluebeck.itm.tr.iwsn.devicedb;

import de.uniluebeck.itm.tr.util.Tuple;
import eu.wisebed.api.v3.common.NodeUrn;
import org.jboss.netty.channel.ChannelHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class DeviceConfig {

	private static final int DEFAULT_TIMEOUT_FLASH_MILLIS = 120000;

	private static final int DEFAULT_TIMEOUT_NODE_API_MILLIS = 1000;

	private static final int DEFAULT_TIMEOUT_RESET_MILLIS = 3000;

	private static final int DEFAULT_TIMEOUT_CHECK_ALIVE_MILLIS = 3000;

	private static final int DEFAULT_MAXIMUM_MESSAGE_RATE = Integer.MAX_VALUE;

	private final NodeUrn nodeUrn;

	private final String nodeType;

	@Nullable
	private final String nodeSerialInterface;

	@Nullable
	private final String nodeUSBChipID;

	@Nullable
	private final Map<String, String> nodeConfiguration;

	@Nullable
	private final List<Tuple<String, ChannelHandler>> defaultChannelPipeline;

	@Nullable
	private final Integer maximumMessageRate;

	@Nullable
	private final Long timeoutNodeApiMillis;

	@Nullable
	private final Long timeoutResetMillis;

	@Nullable
	private final Long timeoutFlashMillis;

	@Nullable
	private final Long timeoutCheckAliveMillis;

	public DeviceConfig(
			final NodeUrn nodeUrn,
			final String nodeType,
			@Nullable final String nodeSerialInterface,
			@Nullable final String nodeUSBChipID,
			@Nullable final Map<String, String> nodeConfiguration,
			@Nullable final List<Tuple<String, ChannelHandler>> defaultChannelPipeline,
			@Nullable final Integer maximumMessageRate,
			@Nullable final Long timeoutCheckAliveMillis,
			@Nullable final Long timeoutFlashMillis,
			@Nullable final Long timeoutNodeApiMillis,
			@Nullable final Long timeoutResetMillis) {

		this.nodeUrn = checkNotNull(nodeUrn);
		this.nodeType = checkNotNull(nodeType);
		this.nodeSerialInterface = nodeSerialInterface;
		this.nodeUSBChipID = nodeUSBChipID;
		this.nodeConfiguration = nodeConfiguration;
		this.defaultChannelPipeline = defaultChannelPipeline;

		// maximumMessageRate
		checkArgument((maximumMessageRate == null || maximumMessageRate > 0),
				"The maximum number of messages per second must either be omitted (null) to use the default value "
						+ "of " + DEFAULT_MAXIMUM_MESSAGE_RATE + " or be larger than 0 (zero). Configured value: "
						+ maximumMessageRate
		);
		this.maximumMessageRate = maximumMessageRate;

		// timeoutCheckAliveMillis
		checkArgument((timeoutCheckAliveMillis == null || timeoutCheckAliveMillis > 0),
				"The timeout value for the checkAlive operation must either be omitted (null) to use the default value "
						+ "of " + DEFAULT_TIMEOUT_CHECK_ALIVE_MILLIS + " ms or be larger than 0 (zero). Configured "
						+ "value: " + timeoutCheckAliveMillis
		);
		this.timeoutCheckAliveMillis = timeoutCheckAliveMillis;

		// timeoutFlashMillis
		checkArgument((timeoutFlashMillis == null || timeoutFlashMillis > 0),
				"The timeout value for the flash operation must either be omitted (null) to use the default "
						+ "value of " + DEFAULT_TIMEOUT_FLASH_MILLIS + " ms or be larger than 0 (zero). Configured "
						+ "value: " + timeoutFlashMillis
		);
		this.timeoutFlashMillis = timeoutFlashMillis;

		// timeoutNodeApiMillis
		checkArgument((timeoutNodeApiMillis == null || timeoutNodeApiMillis > 0),
				"The timeout value for the Node API must either be omitted (null) to use the default value of " +
						DEFAULT_TIMEOUT_NODE_API_MILLIS + " ms or be larger than 0 (zero). Configured value: " +
						timeoutNodeApiMillis
		);
		this.timeoutNodeApiMillis = timeoutNodeApiMillis;

		// timeoutResetMillis
		checkArgument((timeoutResetMillis == null || timeoutResetMillis > 0),
				"The timeout value for the reset operation must either be omitted (null) to use the default value "
						+ "of " + DEFAULT_TIMEOUT_RESET_MILLIS + " ms or be larger than 0 (zero). Configured "
						+ "value: " + timeoutResetMillis
		);
		this.timeoutResetMillis = timeoutResetMillis;
	}

	public int getMaximumMessageRate() {
		return maximumMessageRate != null ? maximumMessageRate : DEFAULT_MAXIMUM_MESSAGE_RATE;
	}

	@Nullable
	public String getNodeSerialInterface() {
		return nodeSerialInterface;
	}

	public String getNodeType() {
		return nodeType;
	}

	public NodeUrn getNodeUrn() {
		return nodeUrn;
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

}
