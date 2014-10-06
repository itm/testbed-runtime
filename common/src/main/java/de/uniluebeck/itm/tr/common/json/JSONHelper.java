package de.uniluebeck.itm.tr.common.json;

import com.google.common.base.Throwables;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.StringWriter;

@SuppressWarnings("deprecation")
public class JSONHelper {

	private static ObjectMapper mapper = new ObjectMapper();

	static {

		final SimpleModule module = new SimpleModule(
				"TR REST API Custom Serialization Module",
				new Version(1, 0, 0, null)
		);

		module.addSerializer(NodeUrnPrefix.class, new NodeUrnPrefixSerializer());
		module.addDeserializer(NodeUrnPrefix.class, new NodeUrnPrefixDeserializer());

		module.addSerializer(NodeUrn.class, new NodeUrnSerializer());
		module.addDeserializer(NodeUrn.class, new NodeUrnDeserializer());

		module.addSerializer(DateTime.class, new DateTimeSerializer());
		module.addDeserializer(DateTime.class, new DateTimeDeserializer());

		mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
		mapper.registerModule(module);
	}

	public static String toJSON(Object o) {
		return toJSON(o, false);
	}

	public static String toJSON(Object o, boolean formatJson) {
		StringWriter stringWriter = new StringWriter();
		try {
			final ObjectWriter writer = formatJson ?
					mapper.typedWriter(o.getClass()).withDefaultPrettyPrinter() :
					mapper.typedWriter(o.getClass());
			writer.writeValue(stringWriter, o);
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
