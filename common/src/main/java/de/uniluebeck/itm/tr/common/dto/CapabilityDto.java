package de.uniluebeck.itm.tr.common.dto;

public class CapabilityDto {

	private String name;

	private String defaultValue;

	private String datatype;

	private String unit;

	@SuppressWarnings("unused")
	public CapabilityDto() {
	}

	public CapabilityDto(String name, String defaultValue, String datatype, String unit) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.datatype = datatype;
		this.unit = unit;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getDatatype() {
		return datatype;
	}

	public void setDatatype(String datatype) {
		this.datatype = datatype;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}
}
