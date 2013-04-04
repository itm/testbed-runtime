package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util;


public class Base64Helper {

	public static String decode(String input) {
		return new String(Base64.decode(input));
	}

	public static String encode(String input) {
		return new String(Base64.encode(input));
	}

	public static String encode(byte[] input) {
		return new String(Base64.encode(input));
	}

}
