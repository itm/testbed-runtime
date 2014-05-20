package de.uniluebeck.itm.tr.iwsn.common.json;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.joda.time.DateTime;

import java.io.IOException;

public class DateTimeDeserializer extends JsonDeserializer<DateTime> {

	@Override
	public DateTime deserialize(final JsonParser jp, final DeserializationContext ctxt)
			throws IOException {
		return new DateTime(jp.getText());
	}
}
