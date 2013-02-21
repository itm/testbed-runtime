package de.uniluebeck.itm.tr.devicedb.dto;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class DeviceConfigListDto {

	private List<DeviceConfigDto> deviceConfigDtos;

	public DeviceConfigListDto() {
	}

	public DeviceConfigListDto(final List<DeviceConfigDto> deviceConfigDtos) {
		this.deviceConfigDtos = deviceConfigDtos;
	}

	public List<DeviceConfigDto> getDeviceConfigDtos() {
		return deviceConfigDtos;
	}

	public void setDeviceConfigDtos(final List<DeviceConfigDto> deviceConfigDtos) {
		this.deviceConfigDtos = deviceConfigDtos;
	}
}
