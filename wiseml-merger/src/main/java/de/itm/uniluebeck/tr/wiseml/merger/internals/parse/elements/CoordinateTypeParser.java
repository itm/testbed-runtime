package de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.WiseMLElementParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class CoordinateTypeParser extends WiseMLElementParser<String> {

	public CoordinateTypeParser(WiseMLTreeReader reader) {
		super(reader);
	}

	@Override
	protected void parseStructure() {
		this.structure = reader.getText();
	}
	
}
