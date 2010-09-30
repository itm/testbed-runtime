/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.util;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class StringUtils {

	// -------------------------------------------------------------------------

	/**
	 * @param jaxbObject
	 * @return
	 */
	public static String jaxbMarshal(Object jaxbObject) {
		StringWriter writer = new StringWriter();
		if (jaxbObject instanceof Collection) {
			for (Object o : (Collection) jaxbObject) {
				JAXB.marshal(o, writer);
				writer.append("\n");
			}
		} else {
			JAXB.marshal(jaxbObject, writer);
		}
		return writer.toString();
	}

	// -------------------------------------------------------------------------

	/**
	 * @param jaxbObject
	 * @return
	 * @throws JAXBException
	 * @throws Exception
	 */
	public static String jaxbMarshalFragment(Object jaxbObject) throws JAXBException {
		JAXBContext jc = JAXBContext.newInstance(jaxbObject.getClass().getPackage().getName());
		Marshaller marshaller = jc.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
		StringWriter writer = new StringWriter();
		marshaller.marshal(jaxbObject, writer);
		return writer.toString();
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public static String toHexString(char tmp) {
		return toHexString((byte) tmp);
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public static String toHexString(byte[] tmp) {
		return toHexString(tmp, 0, tmp.length);
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public static String toHexString(byte tmp) {
		return "0x" + Integer.toHexString(tmp & 0xFF);
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public static String toHexString(byte[] tmp, int offset) {
		return toHexString(tmp, offset, tmp.length - offset);
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public static String toHexString(byte[] tmp, int offset, int length) {
		StringBuffer s = new StringBuffer();
		for (int i = offset; i < offset + length; ++i) {
			if (s.length() > 0)
				s.append(' ');
			s.append("0x");
			s.append(Integer.toHexString(tmp[i] & 0xFF));
		}
		return s.toString();
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public static String toHexStringReverseDirection(byte[] tmp) {
		return toHexStringReverseDirection(tmp, 0, tmp.length);
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public static String toHexStringReverseDirection(byte[] tmp, int offset, int length) {
		byte reverse[] = new byte[length];

		for (int i = 0; i < length; ++i)
			reverse[i] = tmp[offset + length - i - 1];

		return toHexString(reverse);
	}

	public static List<String> parseLines(String str) {
		return Arrays.asList(str.split("[\\r\\n]+"));
	}

	public static String toString(Collection<? extends Object> list) {
		if (list == null)
			return "null";

		return Arrays.toString(list.toArray());
	}


	public static Long parseHexOrDecLong(String value) {
		return value.startsWith("0x") ? Long.parseLong(value.substring(2), 16) : Long.parseLong(value, 10);
	}

	public static Long parseHexOrDecLongFromUrn(String urn) {
		String[] arr = urn.split(":");
		String suffix = arr[arr.length - 1];
		return parseHexOrDecLong(suffix);		
	}

    public static String parseHexOrDecLongUrnSuffix(String value) {
        String[] valueAsArray = value.split(":");
        String suffix = valueAsArray[valueAsArray.length - 1];
        return getPrefixAsStringFromStringArray(valueAsArray) + ":" + (suffix.startsWith("0x") ? Long.parseLong(suffix.substring(2), 16) : Long.parseLong(suffix, 10));
    }

    private static String getPrefixAsStringFromStringArray(String[] value){
        StringBuffer result = new StringBuffer();
        if (value.length > 0) {
            result.append(value[0]);
            for (int i = 1; i < value.length - 1; i++) {
                result.append(":");
                result.append(value[i]);
            }
        }
        return result.toString();
    }

    public static boolean assertHexOrDecLongValue(String value){
        try {
            parseHexOrDecLong(value);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

	public static boolean hasHexOrDecLongUrnSuffix(String value) {
		String[] arr = value.split(":");
		String suffix = arr[arr.length - 1];
		return assertHexOrDecLongValue(suffix);
	}

	/**
	 * Asserts that the given String {@code value} is a URN that has a suffix which can be parsed as a long value,
	 * either hex-encoded (starting with 0x) or decimal-encoded.
	 *
	 * @param value
	 * @throws RuntimeException if suffix can not be parse as long value
	 */
	public static void assertHexOrDecLongUrnSuffix(String value) throws RuntimeException {
		if (!StringUtils.hasHexOrDecLongUrnSuffix(value))
			throw new RuntimeException("Suffix of {" + value + "} has to be an integer-value!");
	}

    public static String getUrnSuffix(String urn) {
        String[] arr = urn.split(":");
        return arr[arr.length - 1];  
    }
}
