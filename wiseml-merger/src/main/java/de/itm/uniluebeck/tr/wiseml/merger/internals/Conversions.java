package de.itm.uniluebeck.tr.wiseml.merger.internals;

public class Conversions {
	
	public static Boolean readBoolean(String string) {
		if (string.equals("1")) {
			return Boolean.TRUE;
		}
		if (string.equals("0")) {
			return Boolean.FALSE;
		}
		string = string.toLowerCase();
		if (string.equals("true")) {
			return Boolean.TRUE;
		}
		if (string.equals("false")) {
			return Boolean.FALSE;
		}
		throw new RuntimeException("could not parse '"+string+"' as a boolean");
	}
	
	public static String writeBoolean(Boolean value) {
		return value.toString();
	}

}
