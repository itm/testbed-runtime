package de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements;

import de.itm.uniluebeck.tr.wiseml.merger.enums.DataType;
import de.itm.uniluebeck.tr.wiseml.merger.enums.Unit;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.ParserHelper;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.ParserCallback;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.WiseMLElementParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.Capability;

public class CapabilityParser extends WiseMLElementParser<Capability> {

	public CapabilityParser(WiseMLTreeReader reader) {
		super(reader);
	}

	@Override
	protected void parseStructure() {
		structure = new Capability();
		ParserHelper.parseStructures(reader, new ParserCallback(){
			@Override
			public void nextStructure(WiseMLTag tag, Object obj) {
				switch (tag) {
				case name:
					structure.setName((String)obj);
					break;
				case dataType:
					structure.setDataType((DataType)obj);
					break;
				case unit:
					structure.setUnit((Unit)obj);
					break;
				case capabilityDefaultValue:
					structure.setDefaultValue((String)obj);
					break;
				}
			}
		});
	}

}
