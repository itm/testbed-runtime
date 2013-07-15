package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util;

import com.google.common.base.Throwables;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.io.StringWriter;

@SuppressWarnings("deprecation")
public class JSONHelper {

	private static ObjectMapper mapper = new ObjectMapper();

	public static String toXML(Object o) {
		StringWriter writer = new StringWriter();
		javax.xml.bind.JAXB.marshal(o, writer);
		return writer.toString();
	}

	public static String toJSON(Object o) {
		StringWriter stringWriter = new StringWriter();
		try {
			mapper.typedWriter(o.getClass()).writeValue(stringWriter, o);
		} catch (IOException e) {
			// should not happen because of StringWriter
		} catch (ArrayIndexOutOfBoundsException e) {
			try {
				stringWriter = new StringWriter();
				mapper.typedWriter(o.getClass()).writeValue(stringWriter, o);
			} catch (Exception e1) {
				throw new RuntimeException(e1);
			}
		}
		return stringWriter.toString();
	}

	public static <T> T fromJSON(String json, Class<T> type) throws Exception {
		try {
			return mapper.readValue(json, type);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public static <T> T fromJSON(String json, TypeReference<T> type) throws Exception {
		try {
			return mapper.readValue(json, type);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}
}
