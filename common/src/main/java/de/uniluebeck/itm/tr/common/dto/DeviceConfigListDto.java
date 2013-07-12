package de.uniluebeck.itm.tr.common.dto;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class DeviceConfigListDto {

	private List<DeviceConfigDto> configs;

	@SuppressWarnings("unused")
	public DeviceConfigListDto() {
	}

	public DeviceConfigListDto(final List<DeviceConfigDto> configs) {
		this.configs = configs;
	}

	public List<DeviceConfigDto> getConfigs() {
		return configs;
	}

	public void setConfigs(final List<DeviceConfigDto> configs) {
		this.configs = configs;
	}
}
