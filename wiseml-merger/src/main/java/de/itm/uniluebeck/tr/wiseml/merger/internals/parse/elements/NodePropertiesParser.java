package de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.ParserHelper;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.ParserCallback;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.WiseMLElementParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.Capability;
import de.itm.uniluebeck.tr.wiseml.merger.structures.Coordinate;
import de.itm.uniluebeck.tr.wiseml.merger.structures.NodeProperties;

public class NodePropertiesParser extends WiseMLElementParser<NodeProperties> {

	public NodePropertiesParser(WiseMLTreeReader reader) {
		super(reader);
	}

	@Override
	protected void parseStructure() {
		structure = new NodeProperties();
		ParserHelper.parseStructures(reader, new ParserCallback(){
			@Override
			public void nextStructure(WiseMLTag tag, Object obj) {
				switch (tag) {
				case position:
					structure.setPosition((Coordinate)obj);
					break;
				case gateway:
					structure.setGateway((Boolean)obj);
					break;
				case programDetails:
					structure.setProgramDetails((String)obj);
					break;
				case nodeType:
					structure.setNodeType((String)obj);
					break;
				case description:
					structure.setDescription((String)obj);
					break;
				case capability:
					structure.addCapability((Capability)obj);
					break;
				}
			}
		});
	}

}
