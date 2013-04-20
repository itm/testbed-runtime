package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.serializers;

import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import java.io.IOException;

public class NodeUrnPrefixDeserializer extends JsonDeserializer<NodeUrnPrefix> {

	@Override
	public NodeUrnPrefix deserialize(final JsonParser jp, final DeserializationContext ctxt)
			throws IOException {
		return new NodeUrnPrefix(jp.getText());
	}
}
