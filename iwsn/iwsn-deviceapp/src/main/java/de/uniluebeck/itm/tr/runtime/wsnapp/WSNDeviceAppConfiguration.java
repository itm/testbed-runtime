package de.uniluebeck.itm.tr.runtime.wsnapp;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniluebeck.itm.tr.util.StringUtils.assertHexOrDecLongUrnSuffix;

public class WSNDeviceAppConfiguration {

	public static final int DEFAULT_TIMEOUT_FLASH_MILLIS = 120000;

	public static final int DEFAULT_TIMEOUT_NODE_API_MILLIS = 1000;

	public static final int DEFAULT_TIMEOUT_RESET_MILLIS = 3000;

	public static final int DEFAULT_MAXIMUM_MESSAGE_RATE = Integer.MAX_VALUE;

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

			// set defaults
			this.configuration.maximumMessageRate = DEFAULT_MAXIMUM_MESSAGE_RATE;
			this.configuration.timeoutFlash = DEFAULT_TIMEOUT_FLASH_MILLIS;
			this.configuration.timeoutNodeAPI = DEFAULT_TIMEOUT_NODE_API_MILLIS;
			this.configuration.timeoutReset = DEFAULT_TIMEOUT_RESET_MILLIS;
		}

		public Builder setMaximumMessageRate(@Nullable final Integer maximumMessageRate) {
			checkArgument((maximumMessageRate == null || maximumMessageRate > 0),
					"The maximum number of messages per second must either be omitted (null) to use the default value "
							+ "of " + DEFAULT_MAXIMUM_MESSAGE_RATE + " or be larger than 0 (zero). Configured value: "
							+ maximumMessageRate
			);
			assertNotBuiltYet();
			if (maximumMessageRate != null) {
				this.configuration.maximumMessageRate = maximumMessageRate;
			}
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

		public Builder setTimeoutFlash(@Nullable final Integer timeoutFlash) {
			checkArgument((timeoutFlash == null || timeoutFlash > 0),
					"The timeout value for the flash operation must either be omitted (null) to use the default "
							+ "value of " + DEFAULT_TIMEOUT_FLASH_MILLIS + " ms or be larger than 0 (zero). Configured "
							+ "value: " + timeoutFlash
			);
			assertNotBuiltYet();
			if (timeoutFlash != null) {
				this.configuration.timeoutFlash = timeoutFlash;
			}
			return this;
		}

		public Builder setTimeoutNodeAPI(@Nullable final Integer timeoutNodeAPI) {
			checkArgument((timeoutNodeAPI == null || timeoutNodeAPI > 0),
					"The timeout value for the Node API must either be omitted (null) to use the default value of " +
							DEFAULT_TIMEOUT_NODE_API_MILLIS + " ms or be larger than 0 (zero). Configured value: " +
							timeoutNodeAPI
			);
			assertNotBuiltYet();
			if (timeoutNodeAPI != null) {
				this.configuration.timeoutNodeAPI = timeoutNodeAPI;
			}
			return this;
		}

		public Builder setTimeoutReset(@Nullable final Integer timeoutReset) {
			checkArgument((timeoutReset == null || timeoutReset > 0),
					"The timeout value for the reset operation must either be omitted (null) to use the default value "
							+ "of " + DEFAULT_MAXIMUM_MESSAGE_RATE + " ms or be larger than 0 (zero). Configured "
							+ "value: " + timeoutReset
			);
			assertNotBuiltYet();
			if (timeoutReset != null) {
				this.configuration.timeoutReset = timeoutReset;
			}
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

	private String nodeUrn;

	private String nodeType;

	private String nodeSerialInterface;

	private String nodeUSBChipID;

	private int timeoutNodeAPI;

	private int maximumMessageRate;

	private int timeoutReset;

	private int timeoutFlash;

	public static Builder builder(final String nodeUrn, final String nodeType) {
		return new Builder(nodeUrn, nodeType);
	}

	public Integer getMaximumMessageRate() {
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

	public Integer getTimeoutFlash() {
		return timeoutFlash;
	}

	public Integer getTimeoutNodeAPI() {
		return timeoutNodeAPI;
	}

	public Integer getTimeoutReset() {
		return timeoutReset;
	}
}
