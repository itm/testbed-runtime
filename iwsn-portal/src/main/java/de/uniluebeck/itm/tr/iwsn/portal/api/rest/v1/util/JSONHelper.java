package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util;

import com.google.common.base.Throwables;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.type.TypeReference;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import java.io.IOException;
import java.io.StringWriter;

@SuppressWarnings("deprecation")
public class JSONHelper {

	private static ObjectMapper mapper = new ObjectMapper();

	private static AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();

	private static ObjectWriter writer;

	static {
		// make deserializer use JAXB annotations (only)
		mapper.getDeserializationConfig().setAnnotationIntrospector(introspector);
		// make serializer use JAXB annotations (only)
		mapper.getSerializationConfig().setAnnotationIntrospector(introspector);

		writer = mapper.writerWithDefaultPrettyPrinter();
	}

	public static String toXML(Object o) {
		StringWriter writer = new StringWriter();
		javax.xml.bind.JAXB.marshal(o, writer);
		return writer.toString();
	}

	public static String toJSON(Object o) {
		StringWriter stringWriter = new StringWriter();
		try {
			writer.writeValue(stringWriter, o);
		} catch (IOException e) {
			// should not happen because of StringWriter
		} catch (ArrayIndexOutOfBoundsException e) {
			try {
				writer = mapper.writerWithDefaultPrettyPrinter();
				stringWriter = new StringWriter();
				writer.writeValue(stringWriter, o);
			} catch (Exception e1) {
				throw new RuntimeException(e1);
			}
		}
		return stringWriter.toString();
	}

	public static <T> T fromJSON(String json, Class<T> type) throws Exception {
		try {
			return mapper.<T>readValue(json, type);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public static <T> T fromJSON(String json, TypeReference<T> type) throws Exception {
		try {
			return mapper.<T>readValue(json, type);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}
}
