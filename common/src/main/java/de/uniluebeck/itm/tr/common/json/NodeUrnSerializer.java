package de.uniluebeck.itm.tr.common.json;

import eu.wisebed.api.v3.common.NodeUrn;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

import java.io.IOException;

public class NodeUrnSerializer extends SerializerBase<NodeUrn> {

	public NodeUrnSerializer() {
		super(NodeUrn.class);
	}

	@Override
	public void serialize(final NodeUrn nodeUrn, final JsonGenerator jsonGenerator,
						  final SerializerProvider serializerProvider) throws IOException, JsonGenerationException {
		jsonGenerator.writeString(nodeUrn.getNodeUrn());
	}
}
