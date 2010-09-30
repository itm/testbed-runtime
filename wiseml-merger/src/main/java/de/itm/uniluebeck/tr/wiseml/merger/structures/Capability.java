package de.itm.uniluebeck.tr.wiseml.merger.structures;

import de.itm.uniluebeck.tr.wiseml.merger.enums.DataType;
import de.itm.uniluebeck.tr.wiseml.merger.enums.Unit;

/**
 * Created by IntelliJ IDEA.
 * User: jacob
 * Date: Aug 23, 2010
 * Time: 7:04:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class Capability {

    private String name;

    private DataType dataType;

    private Unit unit;

    private String defaultValue;


	public String getName() {
        return name;
    }

    public DataType getDataType() {
        return dataType;
    }

    public Unit getUnit() {
        return unit;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

	public void setName(String name) {
		this.name = name;
	}

	public void setDataType(DataType dataType) {
		this.dataType = dataType;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Capability)) {
			return false;
		}
		Capability other = (Capability)obj;
		return equals(this.name, other.name)
			&& equals(this.dataType, other.dataType)
			&& equals(this.unit, other.unit)
			&& equals(this.defaultValue, other.defaultValue);
	}
	
	private static <T> boolean equals(T a, T b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}
}
