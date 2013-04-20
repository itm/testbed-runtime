package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.serializers;

import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

import java.io.IOException;

public class NodeUrnPrefixSerializer extends SerializerBase<NodeUrnPrefix> {

	public NodeUrnPrefixSerializer() {
		super(NodeUrnPrefix.class);
	}

	@Override
	public void serialize(final NodeUrnPrefix nodeUrnPrefix, final JsonGenerator jsonGenerator,
						  final SerializerProvider serializerProvider) throws IOException, JsonGenerationException {
		jsonGenerator.writeString(nodeUrnPrefix.getNodeUrnPrefix());
	}
}
