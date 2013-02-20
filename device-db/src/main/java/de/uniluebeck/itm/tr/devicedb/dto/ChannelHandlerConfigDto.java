package de.uniluebeck.itm.tr.devicedb.dto;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfig;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class ChannelHandlerConfigDto {

	private String handlerName;

	private String instanceName;

	private List<KeyValueDto> configuration;

	public ChannelHandlerConfigDto() {
	}

	public ChannelHandlerConfigDto(final String handlerName, final String instanceName,
								   final List<KeyValueDto> configuration) {
		this.handlerName = handlerName;
		this.instanceName = instanceName;
		this.configuration = configuration;
	}

	public String getHandlerName() {
		return handlerName;
	}

	public void setHandlerName(final String handlerName) {
		this.handlerName = handlerName;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(final String instanceName) {
		this.instanceName = instanceName;
	}

	public List<KeyValueDto> getConfiguration() {
		return configuration;
	}

	public void setConfiguration(final List<KeyValueDto> configuration) {
		this.configuration = configuration;
	}

	public ChannelHandlerConfig toChannelHandlerConfig() {
		HashMultimap<String, String> properties = null;
		if (configuration != null) {
			properties = HashMultimap.create();
			for (KeyValueDto keyValueDto : configuration) {
				properties.put(keyValueDto.getKey(), keyValueDto.getValue());
			}
		}
		return new ChannelHandlerConfig(handlerName, instanceName, properties);
	}

	public static ChannelHandlerConfigDto fromChannelHandlerConfig(final ChannelHandlerConfig config) {

		final List<KeyValueDto> configList = newArrayList();
		final Multimap<String,String> properties = config.getProperties();

		for (String key : properties.keySet()) {
			for (String value : properties.get(key)) {
				configList.add(new KeyValueDto(key, value));
			}
		}

		return new ChannelHandlerConfigDto(
				config.getHandlerName(),
				config.getInstanceName(),
				configList
		);
	}
}
