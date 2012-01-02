package de.uniluebeck.itm.tr.runtime.wsnapp;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniluebeck.itm.tr.util.StringUtils.assertHexOrDecLongUrnSuffix;

public class WSNDeviceAppConfiguration {

	private static final int DEFAULT_TIMEOUT_FLASH_MILLIS = 120000;

	private static final int DEFAULT_TIMEOUT_NODE_API_MILLIS = 1000;

	private static final int DEFAULT_TIMEOUT_RESET_MILLIS = 3000;

	private static final int DEFAULT_TIMEOUT_CHECK_ALIVE_MILLIS = 3000;

	private static final int DEFAULT_MAXIMUM_MESSAGE_RATE = Integer.MAX_VALUE;

	public static class Builder {

		private WSNDeviceAppConfiguration configuration;

		private volatile boolean built = false;

		private Builder(final String nodeUrn, final String nodeType) {

			checkNotNull(nodeUrn);
			assertHexOrDecLongUrnSuffix(nodeUrn);

			checkNotNull(nodeType);

			this.configuration = new WSNDeviceAppConfiguration();
			this.configuration.nodeUrn = nodeUrn;
			this.configuration.nodeType = nodeType;
		}

		public Builder setMaximumMessageRate(@Nullable final Integer maximumMessageRate) {

			checkArgument((maximumMessageRate == null || maximumMessageRate > 0),
					"The maximum number of messages per second must either be omitted (null) to use the default value "
							+ "of " + DEFAULT_MAXIMUM_MESSAGE_RATE + " or be larger than 0 (zero). Configured value: "
							+ maximumMessageRate
			);

			assertNotBuiltYet();

			this.configuration.maximumMessageRate = maximumMessageRate != null ?
					maximumMessageRate :
					DEFAULT_MAXIMUM_MESSAGE_RATE;

			return this;
		}

		public Builder setNodeSerialInterface(@Nullable final String nodeSerialInterface) {
			assertNotBuiltYet();
			this.configuration.nodeSerialInterface = nodeSerialInterface;
			return this;
		}

		public Builder setNodeUSBChipID(@Nullable final String nodeUSBChipID) {
			assertNotBuiltYet();
			this.configuration.nodeUSBChipID = nodeUSBChipID;
			return this;
		}

		public Builder setConfiguration(@Nullable final Map<String, String> configuration) {
			assertNotBuiltYet();
			this.configuration.deviceConfiguration = configuration;
			return this;
		}

		public Builder setDefaultChannelPipelineConfigurationFile(
				@Nullable final File defaultChannelPipelineConfigurationFile) {
			assertNotBuiltYet();
			this.configuration.defaultChannelPipelineConfigurationFile = defaultChannelPipelineConfigurationFile;
			return this;
		}

		public Builder setTimeoutFlashMillis(@Nullable final Integer timeoutFlash) {

			checkArgument((timeoutFlash == null || timeoutFlash > 0),
					"The timeout value for the flash operation must either be omitted (null) to use the default "
							+ "value of " + DEFAULT_TIMEOUT_FLASH_MILLIS + " ms or be larger than 0 (zero). Configured "
							+ "value: " + timeoutFlash
			);

			assertNotBuiltYet();

			this.configuration.timeoutFlashMillis = timeoutFlash != null ?
					timeoutFlash :
					DEFAULT_TIMEOUT_FLASH_MILLIS;

			return this;
		}

		public Builder setTimeoutNodeApiMillis(@Nullable final Integer timeoutNodeAPI) {

			checkArgument((timeoutNodeAPI == null || timeoutNodeAPI > 0),
					"The timeout value for the Node API must either be omitted (null) to use the default value of " +
							DEFAULT_TIMEOUT_NODE_API_MILLIS + " ms or be larger than 0 (zero). Configured value: " +
							timeoutNodeAPI
			);

			assertNotBuiltYet();

			this.configuration.timeoutNodeApiMillis = timeoutNodeAPI != null ?
					timeoutNodeAPI :
					DEFAULT_TIMEOUT_NODE_API_MILLIS;

			return this;
		}

		public Builder setTimeoutResetMillis(@Nullable final Integer timeoutReset) {

			checkArgument((timeoutReset == null || timeoutReset > 0),
					"The timeout value for the reset operation must either be omitted (null) to use the default value "
							+ "of " + DEFAULT_TIMEOUT_RESET_MILLIS + " ms or be larger than 0 (zero). Configured "
							+ "value: " + timeoutReset
			);

			assertNotBuiltYet();

			this.configuration.timeoutResetMillis = timeoutReset != null ?
					timeoutReset :
					DEFAULT_TIMEOUT_RESET_MILLIS;

			return this;
		}

		public Builder setTimeoutCheckAliveMillis(@Nullable final Integer timeoutCheckAliveMillis) {

			checkArgument((timeoutCheckAliveMillis == null || timeoutCheckAliveMillis > 0),
					"The timeout value for the checkAlive operation must either be omitted (null) to use the default value "
							+ "of " + DEFAULT_TIMEOUT_CHECK_ALIVE_MILLIS + " ms or be larger than 0 (zero). Configured "
							+ "value: " + timeoutCheckAliveMillis
			);

			assertNotBuiltYet();

			this.configuration.timeoutCheckAliveMillis = timeoutCheckAliveMillis != null ?
					timeoutCheckAliveMillis :
					DEFAULT_TIMEOUT_CHECK_ALIVE_MILLIS;

			return this;
		}

		public WSNDeviceAppConfiguration build() {
			assertNotBuiltYet();
			built = true;
			return configuration;
		}

		private void assertNotBuiltYet() {
			if (built) {
				throw new IllegalArgumentException("The configuration has already been built!");
			}
		}
	}

	private File defaultChannelPipelineConfigurationFile;

	private Map<String, String> deviceConfiguration;

	private String nodeUrn;

	private String nodeType;

	private String nodeSerialInterface;

	private String nodeUSBChipID;

	private int timeoutNodeApiMillis = DEFAULT_TIMEOUT_NODE_API_MILLIS;

	private int maximumMessageRate = DEFAULT_MAXIMUM_MESSAGE_RATE;

	private int timeoutResetMillis = DEFAULT_TIMEOUT_RESET_MILLIS;

	private int timeoutFlashMillis = DEFAULT_TIMEOUT_FLASH_MILLIS;

	private int timeoutCheckAliveMillis = DEFAULT_TIMEOUT_CHECK_ALIVE_MILLIS;

	public static Builder builder(final String nodeUrn, final String nodeType) {
		return new Builder(nodeUrn, nodeType);
	}

	public int getMaximumMessageRate() {
		return maximumMessageRate;
	}

	public String getNodeSerialInterface() {
		return nodeSerialInterface;
	}

	public String getNodeType() {
		return nodeType;
	}

	public String getNodeUrn() {
		return nodeUrn;
	}

	public String getNodeUSBChipID() {
		return nodeUSBChipID;
	}

	public int getTimeoutFlashMillis() {
		return timeoutFlashMillis;
	}

	public int getTimeoutNodeApiMillis() {
		return timeoutNodeApiMillis;
	}

	public int getTimeoutResetMillis() {
		return timeoutResetMillis;
	}

	public int getTimeoutCheckAliveMillis() {
		return timeoutCheckAliveMillis;
	}

	@Nullable
	public Map<String, String> getDeviceConfiguration() {
		return deviceConfiguration;
	}

	@Nullable
	public File getDefaultChannelPipelineConfigurationFile() {
		return defaultChannelPipelineConfigurationFile;
	}
}
