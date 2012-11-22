package de.uniluebeck.itm.tr.iwsn.devicedb;

import com.google.common.collect.Multimap;
import de.uniluebeck.itm.tr.util.Tuple;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceType;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.List;

public class DeviceData {

	private NodeUrn nodeUrn;

	private MacAddress macAddress;

	private DeviceType deviceType;

	private String serialInterface;

	private String usbChipId;

	private int maximumMessageRate;

	private int timeoutReset;

	private int timeoutFlash;

	private int timeoutNodeApi;

	private int timeoutCheckAlive;

	private Multimap<String, String> configuration;

	private byte[] defaultImage;

	private List<Tuple<String, Multimap<String, String>>> defaultChannelPipeline;

	public Multimap<String, String> getConfiguration() {
		return configuration;
	}

	public void setConfiguration(final Multimap<String, String> configuration) {
		this.configuration = configuration;
	}

	public List<Tuple<String, Multimap<String, String>>> getDefaultChannelPipeline() {
		return defaultChannelPipeline;
	}

	public void setDefaultChannelPipeline(final List<Tuple<String, Multimap<String, String>>> defaultChannelPipeline) {
		this.defaultChannelPipeline = defaultChannelPipeline;
	}

	public byte[] getDefaultImage() {
		return defaultImage;
	}

	public void setDefaultImage(final byte[] defaultImage) {
		this.defaultImage = defaultImage;
	}

	public DeviceType getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(final DeviceType deviceType) {
		this.deviceType = deviceType;
	}

	public MacAddress getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(final MacAddress macAddress) {
		this.macAddress = macAddress;
	}

	public int getMaximumMessageRate() {
		return maximumMessageRate;
	}

	public void setMaximumMessageRate(final int maximumMessageRate) {
		this.maximumMessageRate = maximumMessageRate;
	}

	public NodeUrn getNodeUrn() {
		return nodeUrn;
	}

	public void setNodeUrn(final NodeUrn nodeUrn) {
		this.nodeUrn = nodeUrn;
	}

	public String getSerialInterface() {
		return serialInterface;
	}

	public void setSerialInterface(final String serialInterface) {
		this.serialInterface = serialInterface;
	}

	public int getTimeoutCheckAlive() {
		return timeoutCheckAlive;
	}

	public void setTimeoutCheckAlive(final int timeoutCheckAlive) {
		this.timeoutCheckAlive = timeoutCheckAlive;
	}

	public int getTimeoutFlash() {
		return timeoutFlash;
	}

	public void setTimeoutFlash(final int timeoutFlash) {
		this.timeoutFlash = timeoutFlash;
	}

	public int getTimeoutNodeApi() {
		return timeoutNodeApi;
	}

	public void setTimeoutNodeApi(final int timeoutNodeApi) {
		this.timeoutNodeApi = timeoutNodeApi;
	}

	public int getTimeoutReset() {
		return timeoutReset;
	}

	public void setTimeoutReset(final int timeoutReset) {
		this.timeoutReset = timeoutReset;
	}

	public String getUsbChipId() {
		return usbChipId;
	}

	public void setUsbChipId(final String usbChipId) {
		this.usbChipId = usbChipId;
	}
}
