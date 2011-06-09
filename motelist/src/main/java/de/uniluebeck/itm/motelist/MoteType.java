package de.uniluebeck.itm.motelist;

public enum MoteType {

	ISENSE("isense"),
	TELOSB("telosb"),
	PACEMATE("pacemate");

	private String typeStr;

	private MoteType(String typeStr) {
		this.typeStr = typeStr;
	}

	public static MoteType fromString(String typeStr) {
		if (ISENSE.typeStr.equals(typeStr)) {
			return ISENSE;
		} else if (TELOSB.typeStr.equals(typeStr)) {
			return TELOSB;
		} else if (PACEMATE.typeStr.equals(typeStr)) {
			return PACEMATE;
		} else {
			return null;
		}
	}

	public static String getTypesString() {
		return ISENSE.typeStr + ", " + TELOSB.typeStr + ", " + PACEMATE.typeStr;
	}

	@Override
	public String toString() {
		return typeStr;
	}

}
