package de.uniluebeck.itm.tr.devicedb.dto;

import eu.wisebed.wiseml.Capability;
import eu.wisebed.wiseml.Dtypes;
import eu.wisebed.wiseml.Units;

public class CapabilityDto {

	private String name;

	private String defaultValue;

	private Dtypes datatype;

	private Units unit;

	@SuppressWarnings("unused")
	public CapabilityDto() {
	}

	public CapabilityDto(String name, String defaultValue, Dtypes datatype, Units unit) {
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

	public Dtypes getDatatype() {
		return datatype;
	}

	public void setDatatype(Dtypes datatype) {
		this.datatype = datatype;
	}

	public Units getUnit() {
		return unit;
	}

	public void setUnit(Units unit) {
		this.unit = unit;
	}

	public Capability toCapability() {
		Capability cap = new Capability();
		cap.setDatatype(getDatatype());
		cap.setDefault(getDefaultValue());
		cap.setName(getName());
		cap.setUnit(getUnit());
		return cap;
	}

	public static CapabilityDto fromCapability(Capability capability) {
		return new CapabilityDto(
				capability.getName(),
				capability.getDefault(),
				capability.getDatatype(),
				capability.getUnit()
		);
	}

}
