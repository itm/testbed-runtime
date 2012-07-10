package de.uniluebeck.itm.tr.runtime.wsnapp;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

public class WSNDeviceAppConnectorConfiguration {

	private static final int DEFAULT_TIMEOUT_FLASH_MILLIS = 120000;

	private static final int DEFAULT_TIMEOUT_NODE_API_MILLIS = 1000;

	private static final int DEFAULT_TIMEOUT_RESET_MILLIS = 3000;

	private static final int DEFAULT_TIMEOUT_CHECK_ALIVE_MILLIS = 3000;

	private static final int DEFAULT_MAXIMUM_MESSAGE_RATE = Integer.MAX_VALUE;

	private final String nodeUrn;

	private final String nodeType;

	@Nullable
	private final String nodeSerialInterface;

	@Nullable
	private final String nodeUSBChipID;

	@Nullable
	private final Map<String, String> nodeConfiguration;

	@Nullable
	private final File defaultChannelPipelineConfigurationFile;

	@Nullable
	private final Integer maximumMessageRate;

	@Nullable
	private final Integer timeoutNodeApiMillis;

	@Nullable
	private final Integer timeoutResetMillis;

	@Nullable
	private final Integer timeoutFlashMillis;

	@Nullable
	private final Integer timeoutCheckAliveMillis;

	public WSNDeviceAppConnectorConfiguration(
			final String nodeUrn,
			final String nodeType,
			@Nullable final String nodeSerialInterface,
			@Nullable final String nodeUSBChipID,
			@Nullable final Map<String, String> nodeConfiguration,
			@Nullable final File defaultChannelPipelineConfigurationFile,
			@Nullable final Integer maximumMessageRate,
			@Nullable final Integer timeoutCheckAliveMillis,
			@Nullable final Integer timeoutFlashMillis,
			@Nullable final Integer timeoutNodeApiMillis,
			@Nullable final Integer timeoutResetMillis) {

		this.nodeUrn = nodeUrn;
		this.nodeType = nodeType;
		this.nodeSerialInterface = nodeSerialInterface;
		this.nodeUSBChipID = nodeUSBChipID;
		this.nodeConfiguration = nodeConfiguration;

		// defaultChannelPipelineConfigurationFile
		final boolean defaultChannelPipelineConfigurationFileNullOrReadableFile =
				defaultChannelPipelineConfigurationFile == null ||
						(
								defaultChannelPipelineConfigurationFile.exists() &&
										defaultChannelPipelineConfigurationFile.isFile() &&
										defaultChannelPipelineConfigurationFile.canRead()
						);
		checkArgument(defaultChannelPipelineConfigurationFileNullOrReadableFile,
				"The default channel pipeline configuration file for " + nodeUrn +
						" (\"" + (defaultChannelPipelineConfigurationFile != null ?
						defaultChannelPipelineConfigurationFile.getAbsolutePath() : "") + "\") "
						+ "either does not exists, is not a file or is not readable!"
		);
		this.defaultChannelPipelineConfigurationFile = defaultChannelPipelineConfigurationFile;

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

		// timeoutFlashMilis
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

	public String getNodeUrn() {
		return nodeUrn;
	}

	@Nullable
	public String getNodeUSBChipID() {
		return nodeUSBChipID;
	}

	public int getTimeoutFlashMillis() {
		return timeoutFlashMillis != null ? timeoutFlashMillis : DEFAULT_TIMEOUT_FLASH_MILLIS;
	}

	public int getTimeoutNodeApiMillis() {
		return timeoutNodeApiMillis != null ? timeoutNodeApiMillis : DEFAULT_TIMEOUT_NODE_API_MILLIS;
	}

	public int getTimeoutResetMillis() {
		return timeoutResetMillis != null ? timeoutResetMillis : DEFAULT_TIMEOUT_RESET_MILLIS;
	}

	public int getTimeoutCheckAliveMillis() {
		return timeoutCheckAliveMillis != null ? timeoutCheckAliveMillis : DEFAULT_TIMEOUT_CHECK_ALIVE_MILLIS;
	}

	@Nullable
	public Map<String, String> getNodeConfiguration() {
		return nodeConfiguration;
	}

	@Nullable
	public File getDefaultChannelPipelineConfigurationFile() {
		return defaultChannelPipelineConfigurationFile;
	}

}
