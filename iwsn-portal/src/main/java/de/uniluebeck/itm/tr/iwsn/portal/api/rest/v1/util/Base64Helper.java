package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util;


import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;

import static com.google.common.base.Throwables.propagate;

public class Base64Helper {

	public static String decode(String input) throws Base64Exception {
		return new String(Base64Utility.decode(input));
	}

	public static byte[] decodeBytes(String input) throws Base64Exception{
		return Base64Utility.decode(input);
	}

	public static String encode(String input) {
		return Base64Utility.encode(input.getBytes());
	}

	public static String encode(byte[] input) {
		return Base64Utility.encode(input);
	}

}
