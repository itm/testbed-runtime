package de.uniluebeck.itm.tr.common.json;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.joda.time.DateTime;

import java.io.IOException;

public class DateTimeSerializer extends JsonSerializer<DateTime> {

	@Override
	public void serialize(final DateTime value, final JsonGenerator jgen, final SerializerProvider provider)
			throws IOException {
		jgen.writeString(value.toString());
	}
}
