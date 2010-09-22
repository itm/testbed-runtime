package de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.ParserHelper;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.ParserCallback;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.WiseMLElementParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.Capability;
import de.itm.uniluebeck.tr.wiseml.merger.structures.LinkProperties;
import de.itm.uniluebeck.tr.wiseml.merger.structures.RSSI;

public class LinkPropertiesParser extends WiseMLElementParser<LinkProperties> {

	public LinkPropertiesParser(WiseMLTreeReader reader) {
		super(reader);
	}

	@Override
	protected void parseStructure() {
		structure = new LinkProperties();
		ParserHelper.parseStructures(reader, new ParserCallback(){
			@Override
			public void nextStructure(WiseMLTag tag, Object obj) {
				switch (tag) {
				case encrypted:
					structure.setEncrypted((Boolean)obj);
					break;
				case virtual:
					structure.setVirtual((Boolean)obj);
					break;
				case rssi:
					structure.setRssi((RSSI)obj);
					break;
				case capability:
					structure.addCapability((Capability)obj);
					break;
				}
			}
		}); 
	}

}
