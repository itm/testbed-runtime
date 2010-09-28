package de.itm.uniluebeck.tr.wiseml.merger.structures;

import de.itm.uniluebeck.tr.wiseml.merger.enums.DataType;
import de.itm.uniluebeck.tr.wiseml.merger.enums.Unit;

public class RSSI {
	
	private DataType dataType;
    private Unit unit;
    private String defaultValue;
    
	public DataType getDataType() {
		return dataType;
	}
	
	public void setDataType(DataType dataType) {
		this.dataType = dataType;
	}
	
	public Unit getUnit() {
		return unit;
	}
	
	public void setUnit(Unit unit) {
		this.unit = unit;
	}
	
	public String getDefaultValue() {
		return defaultValue;
	}
	
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof RSSI)) {
			return false;
		}
		RSSI other = (RSSI)obj;
		return equals(this.dataType, other.dataType)
			&& equals(this.unit, other.unit)
			&& equals(this.defaultValue, other.defaultValue);
	}
	
	private static boolean equals(Object a, Object b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}

}
