package de.uniluebeck.itm.tr.common.json;

import eu.wisebed.api.v3.common.NodeUrn;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import java.io.IOException;

public class NodeUrnDeserializer extends JsonDeserializer<NodeUrn> {

	@Override
	public NodeUrn deserialize(final JsonParser jp, final DeserializationContext ctxt)
			throws IOException {
		return new NodeUrn(jp.getText());
	}
}
