package de.uniluebeck.itm.tr.common.dto;

import javax.annotation.Nullable;
import java.util.List;

public class ChannelHandlerConfigDto {

	private String handlerName;

	private String instanceName;

	private List<KeyValueDto> configuration;

	@SuppressWarnings("unused")
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

	@Nullable
	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(@Nullable final String instanceName) {
		this.instanceName = instanceName;
	}

	public List<KeyValueDto> getConfiguration() {
		return configuration;
	}

	public void setConfiguration(final List<KeyValueDto> configuration) {
		this.configuration = configuration;
	}
}
